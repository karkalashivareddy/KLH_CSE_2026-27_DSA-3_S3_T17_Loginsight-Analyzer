# 01 — Requirements Specification

## LogInsight Analyzer — DSA-3 Advanced Algorithmic Log Intelligence and Text Analytics System

| Field | Value |
|---|---|
| Project | LogInsight Analyzer |
| Type | DSA-3 Advanced Algorithmic Log Intelligence and Text Analytics System |
| Repository | `KLR_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer` |
| Academic context | University DSA-3 (Text Analytics / Advanced Algorithms) project |
| Student badge | Must be explainable under source-code inspection and viva questioning |

---

## 1. Problem Statement

Operators inspect raw application/system logs manually or with simple substring searches. Manual
inspection does not scale and plain filtering does not expose the algorithmic structure of the data
— repeated error templates, fuzzy duplicates, service dependency bottlenecks, minimum cuts, or
subset-optimization problems.

LogInsight Analyzer treats logs as **algorithmic input**. It provides a query-driven engine that maps
DSA-3 syllabus algorithms onto real log-analysis problems, executes **hand-written** implementations,
and reports the result, the intermediate data structure, the complexity, and the empirically measured
performance for every query.

> **Identity statement:** *LogInsight Analyzer is not just a log viewer. It is an algorithmic query
> engine that applies advanced DSA-3 algorithms to real text, sequence, graph, optimization,
> randomized, and streaming problems.*

---

## 2. Objectives

1. Provide log ingestion that parses multiple formats into a normalized `LogEvent` model.
2. Expose a query engine where the **algorithm is the unit of work**, not the database row.
3. Implement the DSA-3 syllabus algorithms **manually** (no library delegation for algorithm logic).
4. For every query return: problem → input → algorithm → execution → intermediate structure → result
   → time/space complexity → measured performance.
5. Build a React dashboard that visualizes intermediate structures (LPS, Z-array, DP matrices,
   residual graphs, suffix arrays, matchings, samples).
6. Test algorithmic correctness — including **cross-checks between independent implementations** —
   and measure real benchmarks without fabricated numbers.
7. Produce documentation that fully answers viva questions (see `docs/16-viva-questions.md`).

---

## 3. User Personas

| Persona | Need |
|---|---|
| Evaluator / examiner | Inspect source, run the app, verify algorithms are genuine and explainable |
| Student (author) | Demonstrate mastery of every DSA-3 module, defend design in viva |
| Operator (demo fictional role) | Upload logs, run algorithmic queries, read results and complexity |

---

## 4. Functional Requirements

### FR-1 Log ingestion and parsing
- Upload raw log files (multi-line text / JSON Lines) via REST.
- Parse a canonical text log format and a structured JSON Lines format.
- Normalize every record into `LogEvent`; optional fields remain nullable; malformed records are
  rejected with useful parse errors without aborting the whole import.
- Load bundled sample datasets from `sample-data/`.
- Maintain the current dataset in memory (session model) with clear/statistics operations.

### FR-2 Query engine
- A single query abstraction dispatches to a per-algorithm strategy.
- Every query result carries: `algorithm`, `queryType`, `inputSize`, result, `intermediateData`,
  `executionTimeNanos`, `timeComplexity`, `spaceComplexity`.

### FR-3 String algorithms (Module 2)
- Naive, KMP, Z, Rabin-Karp (rolling hash + double-hash option), Aho-Corasick (multi-pattern),
  Suffix Array (O(n log² n) doubling + counting sort, SA-IS documented conceptually), Kasai LCP.
- Searchable against the current dataset and against explicit user text.
- Visualize LPS, Z-array, hash windows, trie/failure links, suffix array, LCP.

### FR-4 Dynamic programming (Module 3)
- Levenshtein, Damerau-Levenshtein (with transposition), Weighted edit distance (configurable costs).
- Needleman-Wunsch (global) and Smith-Waterman (local) sequence alignment over event sequences.
- Matrix-chain multiplication (interval DP), Bitmask TSP, Hamiltonian path, tree diameter /
  rerooting DP, SOS DP — exposed as DSA Playground demonstrations.
- Visualize DP matrices, backtrace paths, alignments, picked orderings.

### FR-5 Network flow (Module 4)
- A service dependency graph built from parsed logs (vertex = service, edge = dependency/request
  relationship, capacity = request throughput) — **not hard-coded**.
- Ford-Fulkerson, Edmonds-Karp, Dinic, Min-cut, Bipartite matching, Min-cost max flow (SSP).
- Visualize residual graphs, augmenting paths, level graphs, blocking flows, the cut.

### FR-6 Approximation (Module 5)
- Maximal matching → vertex cover 2-approximation.
- `Incident Coverage Planner` application feature mapping incident relationships to coverage.
- Clear statement that exact optimization is NP-hard; approximation is the practical tool.
- Set-cover greedy demonstration.

### FR-7 Randomised algorithms (Module 6)
- Miller-Rabin primality (manual modular exponentiation, d·2^s decomposition, witnesses, rounds).
- Reservoir sampling over a log stream (size K, uniform, no full-stream storage).
- Universal/randomized hashing education demo. Randomised quicksort.

### FR-8 Parallel algorithms (Module 6)
- Parallel reduce (sum/count/max/error-count), parallel prefix scan (Blelloch), parallel sort.
- Work/span reporting and sequential-vs-parallel comparison in the Performance Lab.

### FR-9 REST API
- Endpoints per `docs/12-api-documentation.md`, DTOs used, clean JSON errors, HTTP 400 on
  invalid input, no stack traces exposed.

### FR-10 React dashboard
- Dashboard, Log Explorer, String Algorithm Lab, Similarity Lab, Suffix Lab, Network Flow Lab,
  Approximation page, Randomized Lab, Parallel Lab, Benchmark Lab, DSA Playground (see
  `docs/15-user-guide.md`).
- Technical/research aesthetic; rigorous result + complexity + performance hierarchy.

---

## 5. Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | **Correctness > feature count.** Every claim must be backed by an actual implementation and passing test. |
| NFR-2 | **No fabricated numbers.** All benchmark and execution-time values come from real runs. |
| NFR-3 | **Transparent algorithms.** No `java.util` delegation for core algorithm logic in `dsa/**`. |
| NFR-4 | Buildability — backend (`mvnw`) and frontend (`npm run build`) must build from a clean state. |
| NFR-5 | Academic scope — no external algorithm libraries, no unnecessary database, no fake authentication, no AI/ML marketing. |
| NFR-6 | Ability to run with a single command from the repo root using the checked-in Maven Wrapper. |
| NFR-7 | Graceful handling of malformed logs, oversize queries, and user errors. |
| NFR-8 | Benchmarks bounded (string search for n up to ~1,000,000 where practical; flow/DP/randomized with size caps that keep a demo responsive). |

---

## 6. Constraints

1. **DSA-authenticity rule** — inside `com.loginsight.dsa.**` the following are **prohibited** for
   core algorithm logic: `String.indexOf`, `String.contains`, `String.matches`,
   `java.util.Collections`, `java.util.PriorityQueue`, `Treeset/TreeMap`, `HashMap/HashSet` (when
   the structure itself is the algorithm's subject, e.g. the Aho trie), `Arrays.sort`, stream/filter
   pipelines, and any third-party algorithm/string library. See `docs/02-architecture.md` §8.
2. All string-search, sequence-alignment, flow, approximation, randomised and parallel algorithm
   logic must be hand-implemented.
3. Persistence is out of scope for the academic version; an in-memory session is acceptable if
   documented (isolation behind an interface for a possible future store).
4. Work is developed in explicit phases; each phase ends in review + a single logical commit; the
   repository must remain buildable after every commit.

---

## 7. Acceptance Criteria

| Area | Criterion |
|---|---|
| Build | `mvnw clean verify` passes; `npm run build` passes. |
| Tests | All core algorithm tests pass, including cross-check suites (Naive==KMP==Z==Rabin-Karp;
  Ford-Fulkerson==Edmonds-Karp==Dinic on shared valid graphs). |
| String | Naive, KMP, Z, Rabin-Karp, Aho-Corasick, Suffix Array, Kasai LCP implemented + tested. |
| DP | Practical DP algorithms (edit-distance family, alignments, matrix chain, bitmask TSP)
  implemented + tested; playground demos for tree/SOS. |
| Flow | All max-flow implementations agree on test graphs; min-cut and matching implemented + tested. |
| Approximation | Vertex-cover 2-approximation returns its bound and reports ratio. |
| Randomised | Miller-Rabin and reservoir sampling implemented + tested. |
| Parallel | Parallel results equal sequential results on shared datasets. |
| API | Endpoints return valid envelopes; validation returns HTTP 400. |
| UI | Every major feature reachable from the React app. |
| Documentation | Every algorithm has idea, example, implementation, complexity, feature mapping
  (`docs/04-dsa-mapping.md`) and a viva answer (`docs/16-viva-questions.md`). |
| No fake features | Nothing listed as implemented may be absent from source. |

---

## 8. Out of Scope (explicitly)

- Persistent storage / databases (PostgreSQL/MongoDB/MySQL).
- Authentication / authorization / multi-tenant isolation.
- Real-time streaming at scale (beyond in-memory reservoir sampling demo).
- O(n) SA-IS construction (documented conceptually only; doubling + counting sort implemented).
- Cycle-cancelling min-cost flow (SSP implemented, cycle-cancelling documented conceptually).
- Machine learning / anomaly detection models.
- Running log consumers/agents on production infrastructure.

---

## 9. Requirements Traceability

Each requirement above maps to:
- `docs/02-architecture.md` — design satisfaction
- `docs/04-dsa-mapping.md` — syllabus→algorithm→feature trace
- `docs/12-api-documentation.md` — endpoint satisfaction
- `docs/13-testing.md` — test satisfaction
- `docs/14-benchmarking.md` — performance satisfaction
- `docs/15-user-guide.md` — usability satisfaction

Traceability is recorded in the DSA mapping table (class + endpoint + UI page + test columns), which
is the single source of truth that every claimed feature is implemented.