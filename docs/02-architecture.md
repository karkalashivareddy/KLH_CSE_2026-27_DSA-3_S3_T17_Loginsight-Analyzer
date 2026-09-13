# 02 — System Architecture

## LogInsight Analyzer — Technical Architecture

---

## 1. System Context

Single-run topology:

```
┌─────────────────────────────┐        ┌──────────────────────────────┐
│  React + Vite + TypeScript  │  HTTP  │  Spring Boot (embedded)      │
│  frontend (dev server)      │ ─────► │  REST API + DSA engine       │
└─────────────────────────────┘        └──────────────────────────────┘
        UI / visualization                    algorithms + in-memory dataset
```

- The backend is a self-contained Spring Boot application, no external database.
- In production-style packaging the frontend build is served as static resources by Spring Boot;
  during development the Vite dev server proxies `/api` to the backend.

---

## 2. Data Transformation Pipeline

```
RAW LOGS
   ↓                    LogController.upload / DatasetService
LOG INGESTION
   ↓                    LogParserFactory (by content/header detection)
LOG PARSING
   ↓                    TextLogParser | JsonLogParser  →  ParsedLog
NORMALIZED LOG EVENTS   LogEvent (model)
   ↓
INDEX / ANALYTICS       FrequencyAnalyzer, TopKAnalyzer, TimeWindowAnalyzer,
                        ServiceGraphBuilder, ErrorAnalyzer
   ↓
QUERY ENGINE            QueryDispatcher → strategy → AlgorithmRequest
   ↓
ADVANCED DSA ENGINE     dsa/** hand-written implementations
   ↓
RESULT + METRICS        AlgorithmResult (result + intermediateData + complexity + time)
   ↓
REST API                controllers (DTOs, validation, error handling)
   ↓
REACT DASHBOARD         pages/visualizations
```

Every algorithm execution documents: **Problem → Input → Algorithm → Execution → Intermediate
Structure → Result → Complexity → Performance**.

---

## 3. Backend Layer Map (`com.loginsight`)

| Layer | Package | Responsibility |
|---|---|---|
| Application | `LogInsightApplication` | Boot entry point |
| Config | `config` | CORS, Web, property wiring |
| Web / API | `controller` | REST endpoints, thin mapping to services |
| DTO | `dto` | Request/response contracts (never expose algorithm internals) |
| Exception | `exception` | Domain exceptions + `GlobalExceptionHandler` (clean JSON errors) |
| Model | `model` | `LogEvent`, `LogLevel`, `HttpMethod`, `ServiceNode`, `LogQuery`, `QueryType`, `AlgorithmType`, `AlgorithmMetrics`, `QueryResult` |
| Parser | `parser` | `LogParser` SPI, `TextLogParser`, `JsonLogParser`, `LogParserFactory`, `ParserException` |
| Service | `service` | `DatasetService`, `LogService`, `SearchService`, `BenchmarkService`, … |
| Analytics | `analytics` | Statistical facades that *coordinate* algorithms |
| Query | `query` | `QueryEngine`, `QueryDispatcher`, strategy objects, validation, context |
| DSA | `dsa` | Hand-written algorithms (see §8 rules) |
| Metrics | derived | `AlgorithmMetrics` collects `executionTimeNanos`, `inputSize`, result size |

Layering rule: **controller → service → query/analytics → dsa**. `dsa` classes have no Spring
dependencies, no web dependencies, and no knowledge of DTOs. `service`/`query` layers depend on `dsa`.

---

## 4. Query Engine Design (Strategy Pattern)

```
HTTP request  →  DTO  →  QueryValidator
                              │
                              ▼
                     QueryDispatcher.swtch(queryType)
                              │
        ┌──────────────┬──────┴───────┬──────────────┬─────────────┐
        ▼              ▼              ▼              ▼             ▼
   KMPStrategy   RabinKarpStrategy FuzzySearchStrat AhoStrategy  DinicStrategy
        │              │              │              │             │
        ▼              ▼              ▼              ▼             ▼
   KMPMatcher    RabinKarpMatcher  Levenshtein…  AhoCorasick   Dinic
        │              │              │              │             │
        └──────────────┴──────┬──────┴──────────────┴─────────────┘
                             ▼
                   AlgorithmMetrics assembler
                             ▼
                AlgorithmResult envelope → DTO → JSON
```

- `QueryType` is an enum (e.g. `PATTERN_SEARCH`, `FUZZY_SEARCH`, `MULTI_PATTERN_SEARCH`,
  `DOCUMENT_SIMILARITY`, `SEQUENCE_ALIGNMENT`, `SUFFIX_ANALYSIS`, `SERVICE_FLOW`, `MIN_CUT`,
  `APPROXIMATE_COVER`, `PRIMALITY_TEST`, `STREAM_SAMPLE`, `BENCHMARK`, …). The dispatcher routes by
  this enum; every strategy is wrapped in an `AlgorithmRequest`-style DTO.
- **PlaygroundController** is deliberately thin: it translates explicit user input into the same DTO
  used by the dataset-facing endpoints and delegates to the same dispatcher, so no logic is
  duplicated between "search the dataset" and "run on my own input".

---

## 5. AlgorithmResult Envelope (canonical)

Every algorithmic endpoint returns the same shape:

```json
{
  "algorithm": "KMP",
  "queryType": "PATTERN_SEARCH",
  "inputSize": 250000,
  "pattern": "database connection failed",
  "result": { "matches": [125, 982, 1542], "matchCount": 3 },
  "intermediateData": { "lps": [0, 0, 0, 1], "lpsStringSteps": 5 },
  "executionTimeNanos": 4215000,
  "memoryEstimateBytes": 12288,
  "timeComplexity": "O(n + m)",
  "spaceComplexity": "O(m)",
  "notes": "LPS array computed from pattern length 23"
}
```

- `executionTimeNanos`, `result`, `intermediateData` come from the actual run — never fabricated.
- `memoryEstimateBytes` is an approximation (e.g. `sizeof`-style count of primary arrays) eligible
  for the "memory where measurable" requirement; the docs never claim JVM-accurate heap numbers.

---

## 6. Dataset / Session Model

- The dataset lives **in memory** in a Spring singleton (`DatasetService`).
- Operations: `upload` (streaming read), `importSample(name)`, `clear`, `stats`, `events` (paged).
- Persistence is intentionally excluded; `DatasetService` hides storage behind methods only, so a
  future store can be added behind the same interface **without** changing controllers.

Rationale documented in `docs/17-limitations.md`: the academic problem is algorithmic analysis, not
database engineering.

---

## 7. Analytics ↔ DSA Separation

- `analytics/*Analyzer` classes compute **statistics** and **coordinate** algorithm calls.
- The algorithmic logic (matching, alignment, flow, sampling) lives entirely in `dsa/**`.
- Example: `ServiceGraphBuilder` parses log events into a `FlowGraph` (vertex=service,
  edge=dependency relationship, capacity=request throughput observed in the data). The graph is then
  handed to `Dinic`, `FordFulkerson`, `EdmondsKarp`, `MinCut`, or `BipartiteMatching`.
  The graph is **built from imported logs**, never hard-coded.

---

## 8. DSA Engine Rules (java.util Restriction)

Scope: every class under `com.loginsight.dsa.**`.

### 8.1 Prohibited for core algorithm logic
| Prohibited | Why |
|---|---|
| `String.indexOf`, `contains`, `matches`, `replaceAll`-as-search | would be the algorithm itself |
| `java.util.Collections` (sort/search) | hides sorting algorithm |
| `Arrays.sort`, `Arrays.binarySearch` | hides sorting/ranking logic |
| `java.util.PriorityQueue`, `TreeSet`, `TreeMap` | hides heap/BST logic |
| `HashMap`/`HashSet` as the algorithm's core structure | e.g. the Aho trie is a real trie, not a map |
| Stream / `filter` / `map` pipelines | opaque iteration |
| Third-party algorithm/string libs | defeats the purpose |

### 8.2 Allowed
| Allowed | Reason |
|---|---|
| `charAt`, `length`, `substring`, `StringBuilder` | primitive string access, not search |
| primitive arrays (`char[]`, `int[]`, `long[]`, `boolean[]`) | the substrate of all algorithms |
| `java.util.Random` / `ThreadLocalRandom` | randomness *source* (sampling/hashing) |
| `java.util.concurrent.*` | **parallel layer only** (`dsa/parallel`), per syllabus |
| own `dsa/common` structures | CustomQueue/CustomStack/CustomArrayList/CustomTrie/… |

### 8.3 Custom data structures (`dsa/common`)
Created only where the syllabus cares about the structure:

| Structure | Used by | Why |
|---|---|---|
| `CustomQueue<T>` | Edmonds-Karp BFS, Dinic BFS, Matching | FIFO augmenting searches |
| `CustomStack<T>` | Ford-Fulkerson iterative DFS, HP/TSP backtracking | explicit DFS |
| `CustomArrayList<T>` | collecting matches/spans | transparent growth policy |
| `CustomPriorityQueue`* | (only if a Dijkstra-style min-cost step needs it) | see min-cost §below |
| `CustomTrie` | Aho-Corasick | the syllabus requires a real trie |
| `CustomHashMap`/`CustomHashSet`* | (only if a viva explicitly demands it) | not needed today |

*\* documented and used **only if** an algorithm genuinely requires it; the default is to prefer
primitive arrays and avoid structure over-engineering. Min-cost max-flow is implemented with
Bellman-Ford/SPFA shortest augmenting paths, so no priority queue is required.*

Each structure carries a javadoc note explaining why it exists (`docs/03` lists them).

### 8.4 Complexity honesty
- Claimed complexity must match the implementation actually present (e.g. doubling suffix-array is
  `O(n log² n)`, SA-IS documented as conceptual `O(n)`).
- Miller-Rabin only claims deterministic ranges the implementation actually verifies, otherwise it
  is documented as **probabilistic**.
- Parallel layers report measured time plus **work/span** derived from the implemented schedule.

---

## 9. Benchmarking Strategy

- All numbers measured live in `BenchmarkService` / ParallelBenchmark: `executionTimeNanos`,
  throughput (`inputSize / seconds`), speedup (`sequential / parallel`), result size, memory estimate.
- Warm-up runs, several repetitions, median reported; environment (JVM, cores, OS) recorded in the
  payload. Full methodology: `docs/14-benchmarking.md`.
- Datasets 10k/50k/100k/500k/1,000k where practical; online caps keep demos interactive (per NFR-8).

---

## 10. Error Handling

- Domain exceptions: `InvalidQueryException`, `InvalidLogException`,
  `AlgorithmExecutionException`, `DatasetException`.
- `GlobalExceptionHandler` maps them to clean JSON:

```json
{
  "status": 400,
  "error": "InvalidQueryException",
  "message": "Pattern must not be empty",
  "timestamp": "2026-09-13T10:00:00Z",
  "path": "/api/search/kmp"
}
```

- Stack traces never reach the client; validation errors return HTTP 400 (`docs/12` catalog).

---

## 11. Frontend Architecture

- Vite + React 18 + TypeScript; `react-router-dom` routing; `recharts` for charts; plain CSS for a
  technical/research look; custom SVG components for graph/trie/alignment rendering.
- Page composition (`pages/`): Dashboard, LogExplorer, StringAlgorithms, SimilarityLab, SuffixLab,
  NetworkFlow, Approximation, RandomizedLab, ParallelLab, BenchmarkLab, DSAPlayground.
- State: small typed stores per page; datasets held by the backend session; API layer in `api/`.
- Visual hierarchy (mandatory order): Project title → Current dataset → Query/algorithm →
  Visualization → Result → Complexity → Performance.

---

## 12. Design Decisions and Alternatives

| Decision | Chosen | Rejected | Rationale |
|---|---|---|---|
| Storage | in-memory session | PostgreSQL/MySQL/Mongo | problem is algorithmic, not persistence |
| Backend | Spring Boot 3.5.x (Java 21 target) | Jakarta EE / Play | syllabus-familiar, embeddable, wide support |
| Build | Maven Wrapper committed | global maven | reproducible from clean machine |
| Frontend | Vite + React + TS | CRA | modern, fast, no CRA limitations |
| Charts | recharts | bespoke canvas lib | adequate + small |
| UI kit | hand-rolled CSS | Tailwind/Component libs | full control of research aesthetic, no bloat |
| Flow impl | manual BFS/DFS/Dinic | libraries (JGraphT…) | syllabus requires hand implementation |
| Min-cost flow | SSP via Bellman-Ford | cycle cancelling (impl.) | educational, bounded; cycle-cancelling documented conceptually |
| Suffix array | doubling + counting sort | SA-IS (impl.), java sort | transparency + O(n log² n); SA-IS conceptual doc |
| Matching | Dinic-based bipartite matching | Hungarian-only | reuses flow engine, matches syllabus context |
| Parallel | ExecutorService/ForkJoin | Spring async/WebFlux | explicit schedules, measurable speedup, work/span |

---

## 13. Runtime Notes

- Dev: `mvnw spring-boot:run` (port 8080) + `npm run dev` (Vite proxy `/api` → 8080).
- Prod-style: `npm run build` emits `frontend/dist` copied into backend static resources at package
  time (Phase 11 detail).
- CORS enabled for the dev origin only; no auth (explicitly out of scope).

---

## 14. Guarantees VIVA-Facing

The architecture is designed so that any evaluator can:
1. Find the algorithm class by name (mapping in `docs/04`), 
2. Read the implementation in `dsa/**` without delegation to `java.util` shortcuts,
3. Trace the endpoint → service → strategy → algorithm call path,
4. Read the intermediate structure in the JSON envelope,
5. See the measured time and the stated complexity side by side,
6. Verify the result against independently implemented algorithms in the cross-check tests.