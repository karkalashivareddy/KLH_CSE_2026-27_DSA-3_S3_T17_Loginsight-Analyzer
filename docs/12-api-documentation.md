# 12 - REST API Documentation

## Conventions

- Base URL: `http://localhost:8080/api` (dev). Vite dev server proxies `/api` to the backend.
- Content type: `application/json` for all JSON endpoints; multipart for `logs/upload`.
- Timestamps in ISO-8601 UTC (`2026-09-13T10:00:01Z`).
- Every algorithmic endpoint returns the canonical `AlgorithmResult` envelope (see `docs/02` §5).
- DTOs, never internal algorithm objects, cross the wire.
- Validation errors and domain errors return the ErrorResponse shape with HTTP status.

### Canonical AlgorithmResult envelope

```json
{
  "algorithm": "KMP",
  "queryType": "PATTERN_SEARCH",
  "inputSize": 250000,
  "pattern": "database connection failed",
  "result": {
    "matches": [125, 982, 1542],
    "matchCount": 3
  },
  "intermediateData": {
    "lps": [0, 0, 0, 1, 2, 0, 0, 1, 2, 3],
    "steps": 5
  },
  "executionTimeNanos": 4215000,
  "memoryEstimateBytes": 12288,
  "timeComplexity": "O(n + m)",
  "spaceComplexity": "O(m)",
  "notes": "pattern length 23; matched against current dataset messages"
}
```

`pattern`/`inputSize`/`notes` are present where relevant. `inputSize` = characters scanned
(string algorithms), cells computed (DP), or vertices*edges traversed (flow).

### Error response shape

```json
{
  "status": 400,
  "error": "InvalidQueryException",
  "message": "Pattern must not be empty",
  "timestamp": "2026-09-13T10:00:00Z",
  "path": "/api/search/kmp"
}
```

## Status codes

| Code | Condition |
|---|---|
| 200 | success |
| 400 | invalid input (validation, malformed body, invalid nodes/capacities, out-of-range sizes) |
| 404 | dataset not loaded, sample not found, unknown endpoint |
| 409 | conflict (algorithm constraint, e.g. TSP n too large, matching requires bipartite graph) |
| 500 | algorithmic execution failure (wrapped, no stack trace) |

---

## Endpoint Catalog

### 1. System

| Method | Path | Body | Purpose |
|---|---|---|---|
| GET | `/api/health` | - | liveness + dataset state, algorithm count |
| GET | `/api/health/ready` | - | readiness (always OK for in-memory app) |

### 2. Logs / Dataset (LogController, DatasetService)

| Method | Path | Body / params | Purpose |
|---|---|---|---|
| POST | `/api/logs/upload` | multipart `file` | upload raw logs (text or JSONL) |
| POST | `/api/logs/import` | `{ "sample": "logs-small" }` or `{ "content": "..." }` | load bundled sample or raw text |
| GET | `/api/logs` | `page`, `size`, `level`, `service`, `status`, `q`, `from`, `to` | paged, filtered events |
| GET | `/api/logs/stats` | - | dataset statistics (total, errors, warnings, services, unique IPs, top error, avg response time, requests/min) |
| POST | `/api/logs/clear` | - | clear current dataset |
| GET | `/api/logs/samples` | - | list available sample-data files |

`POST /api/logs/import` request example:

```json
{ "sample": "logs-medium" }
```

`GET /api/logs/stats` response example:

```json
{
  "totalLogs": 10000,
  "errors": 312,
  "warnings": 540,
  "services": 8,
  "uniqueIps": 1204,
  "topError": "authentication failed",
  "avgResponseTimeMs": 84.2,
  "requestsPerMinute": 45.1,
  "levels": { "INFO": 8120, "DEBUG": 620, "WARN": 540, "ERROR": 312 },
  "topServices": [ { "service": "AUTH", "count": 2200 }, { "service": "DATABASE", "count": 1800 } ],
  "statusCodes": { "200": 6800, "401": 300, "404": 120, "500": 130, "503": 82 },
  "logsOverTime": [ { "bucket": "2026-09-13T10:00", "count": 112 } ]
}
```

### 3. String algorithms (SearchController)

All take `{ "pattern": "...", "text": "...", "datasetId": "...", "scope": "DATASET|EXPLICIT" }`.
`scope=DATASET` searches over the concatenated message corpus of the current dataset;
`scope=EXPLICIT` uses the `text` field as-is. For dataset mode `text` is optional.

| Method | Path | Highlights in response |
|---|---|---|
| POST | `/api/search/naive` | matches, matchCount, comparisons (intermediate) |
| POST | `/api/search/kmp` | matches, matchCount, `intermediateData.lps` |
| POST | `/api/search/z` | matches, `intermediateData.zArray` |
| POST | `/api/search/rabin-karp` | matches + patternHash + windowHashes + candidates + verified + collisions + doubleHashUsed |
| POST | `/api/search/aho-corasick` | per-pattern `{pattern, count, locations}`, `intermediateData.trieNodeCount`, `failureLinks` |

`POST /api/search/kmp` request:

```json
{ "pattern": "database connection failed", "scope": "DATASET" }
```

response envelope:

```json
{
  "algorithm": "KMP",
  "queryType": "PATTERN_SEARCH",
  "inputSize": 312400,
  "result": { "matches": [15, 982, 1542], "matchCount": 3, "offsetsInMessages": [11, 42, 7] },
  "intermediateData": { "lps": [0,0,0,1,2,0,0,1,2,3], "patternLength": 23 },
  "executionTimeNanos": 4215000,
  "timeComplexity": "O(n + m)", "spaceComplexity": "O(m)",
  "notes": "LPS array is the KMP intermediate structure"
}
```

`POST /api/search/rabin-karp` request adds: `{ "base": 256, "doubleHash": true, "prime": 101 }`.

### 4. Fuzzy / similarity (DpController)

| Method | Path | Body (excerpt) | Purpose |
|---|---|---|---|
| POST | `/api/search/fuzzy/levenshtein` | `{ "text", "query", "maxDistance" }` | fuzzy search by Levenshtein |
| POST | `/api/search/fuzzy/damerau` | `{ "text", "query", "maxDistance" }` | fuzzy with transposition |
| POST | `/api/search/fuzzy/weighted` | `{ "a", "b", "insertCost", "deleteCost", "substituteCost" }` | weighted edit distance |
| POST | `/api/dp/levenshtein` | `{ "a", "b", "showMatrix": true }` | full DP matrix + operations |
| POST | `/api/similarity/needleman-wunsch` | `{ "seqA": ["AUTH","USER","DB"], "seqB": [...], "match", "mismatch", "gap" }` | global alignment |
| POST | `/api/similarity/smith-waterman` | same shape | local alignment |

`{ "a": "database connection failed", "b": "database conection failed" }` -> `result.distance = 1`,
`intermediateData.matrix` = 2D int array, `operationPlan` = `[...]`.

Alignment result:

```json
{
  "algorithm": "NEEDLEMAN_WUNSCH",
  "result": {
    "score": 6,
    "alignedA": ["AUTH","USER","DB","PAYMENT","-"],
    "alignedB": ["AUTH","-","DB","PAYMENT","NOTIFY"],
    "identity": 0.8
  },
  "intermediateData": { "matrix": [...], "trace": [...], "gapPenalty": -1 }
}
```

### 5. Suffix structures (StringAlgorithmController)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/suffix/build` | build suffix array + LCP over current corpus or explicit `text` |
| POST | `/api/suffix/search` | `{ "text"/"datasetId", "pattern" }` interval search via suffix array |
| GET | `/api/suffix/lcp` | cached LCP from last build (or provided `text`) |
| POST | `/api/suffix/longest-common-substring` | `{ "a", "b" }` LCS via SA+LCP |

Response: `result.suffixArray`, `result.rank`, `intermediateData.lcp`, `result.matchRange`
`[lo, hi)` over the suffix array for pattern search.

### 6. Network flow (FlowController)

Graph requests accept either `graph` inline or `sourceService`/`sinkService` names resolved from the
current dataset graph (built by ServiceGraphBuilder from real logs). Inline graph format:

```json
{
  "source": "CLIENT",
  "sink": "DATABASE",
  "nodes": ["CLIENT","API","AUTH","USER","DATABASE","PAYMENT","INVENTORY","NOTIFICATION"],
  "edges": [
    { "from": "CLIENT", "to": "API", "capacity": 120 },
    { "from": "API", "to": "AUTH", "capacity": 120 },
    { "from": "AUTH", "to": "USER", "capacity": 95 },
    { "from": "USER", "to": "DATABASE", "capacity": 200 },
    { "from": "PAYMENT", "to": "DATABASE", "capacity": 60 },
    { "from": "INVENTORY", "to": "DATABASE", "capacity": 60 }
  ]
}
```

| Method | Path | Response highlights |
|---|---|---|
| POST | `/api/flow/graph` | dataset-derived dependency graph for a given source/sink |
| POST | `/api/flow/ford-fulkerson` | maxFlow + augmentations `[{path, bottleneck, flowAfter}]` |
| POST | `/api/flow/edmonds-karp` | same shape (BFS paths) |
| POST | `/api/flow/dinic` | maxFlow + `levelGraphs[]`, `blockingFlows[]`, phase count |
| POST | `/api/flow/min-cut` | sourceSide, sinkSide, cutEdges, cutCapacity |
| POST | `/api/flow/matching` | `{ "incidents": [...], "resources": [...], "edges": [...] }` -> maximumMatching, pairs, unmatched |
| POST | `/api/flow/min-cost-flow` | `{ suppliers (incidents), demand, costPerEdge }` -> flow, totalCost, augmentations |

`POST /api/flow/dinic` response (abridged):

```json
{
  "algorithm": "DINIC",
  "queryType": "SERVICE_FLOW",
  "inputSize": 44,
  "result": { "maxFlow": 120, "valueOfCut": 120 },
  "intermediateData": {
    "phases": [
      { "levelGraph": [0,1,1,2,2,2], "blockingFlow": 95, "flowAfter": 95 },
      { "levelGraph": [0,1,1,2,2,3], "blockingFlow": 25, "flowAfter": 120 }
    ],
    "finalResidual": [ { "from": "AUTH", "to": "USER", "residual": 0, "flow": 95 } ]
  },
  "executionTimeNanos": 81200,
  "timeComplexity": "O(V^2 * E)", "spaceComplexity": "O(V + E)"
}
```

### 7. Approximation (ApproximationController)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/approximation/vertex-cover` | `{ "nodes", "edges" }` -> matching, cover, coverSize, lowerBound, ratio |
| POST | `/api/approximation/set-cover` | `{ "universe": [...], "sets": { "name": [...] } }` -> greedy cover, cardinality, ratio bound |
| POST | `/api/approximation/incident-coverage` | incident relationships + services -> candidate cover, explanation of NP-hard caveat |

Vertex-cover response:

```json
{
  "algorithm": "VERTEX_COVER",
  "result": {
    "matchingEdges": [["A","P"],["U","D"]],
    "selectedVertices": ["A","P","U","D"],
    "coverSize": 4,
    "lowerBound": 2,
    "approximationRatio": 2.0
  },
  "intermediateData": { "matchingSteps": 2, "uncoveredEdges": [] },
  "notes": "2-approximation via maximal matching; optimal size >= |matching|"
}
```

### 8. Randomized (RandomizedController)

| Method | Path | Body | Purpose |
|---|---|---|---|
| POST | `/api/randomized/miller-rabin` | `{ "n", "rounds" }` | primality test with witness log |
| POST | `/api/randomized/reservoir` | `{ "size", "k" }` or `{ "values": [...] , "k" }` | uniform K-sample (dataset = stream) |
| POST | `/api/randomized/hash` | `{ "text", "seed", "m" }` | universal hash demo: hash value, collision probability, family description |
| POST | `/api/randomized/quicksort` | `{ "values": [...] }` | randomized pivot sort + comparisons |

Miller-Rabin response:

```json
{
  "algorithm": "MILLER_RABIN",
  "queryType": "PRIMALITY_TEST",
  "result": { "isPrime": true, "verdict": "PROBABLY_PRIME" },
  "intermediateData": {
    "n": 104729, "d": 1636, "s": 6,
    "witnesses": [ { "a": 2, "verdict": "PASS" }, { "a": 3, "verdict": "PASS" } ],
    "rounds": 8
  },
  "notes": "probabilistic; deterministic only for n < 3,317,044,064,679 (verified bases)"
}
```

Reservoir response:

```json
{
  "algorithm": "RESERVOIR_SAMPLING",
  "queryType": "STREAM_SAMPLE",
  "result": { "sample": ["req-473", "req-12"], "streamSize": 10000, "reservoirSize": 2 },
  "intermediateData": { "replacements": 142, "initialFill": 2 },
  "notes": "O(N) time, O(K) memory; uniform without storing the stream"
}
```

### 9. Parallel (ParallelController)

| Method | Path | Body | Purpose |
|---|---|---|---|
| POST | `/api/parallel/reduce` | `{ "op": "SUM|COUNT|MAX|ERROR_COUNT", "size" }` | combine: sequential vs parallel |
| POST | `/api/parallel/scan` | `{ "size", "op": "ADD|MAX" }` | Blelloch prefix scan vs sequential |
| POST | `/api/parallel/sort` | `{ "size" }` | parallel merge/quick sort vs sequential |

Response includes `{ sequentialNanos, parallelNanos, speedup, work, span, parallelism }`,
`parallelism = work / span`.

### 10. Benchmark (BenchmarkController)

| Method | Path | Body | Purpose |
|---|---|---|---|
| POST | `/api/benchmark/run` | `{ "scenario": "STRING|FLOW|PARALLEL", "sizes": [10000, 50000, 100000], "repetitions": 3 }` | run live benchmarks |

Response:

```json
{
  "scenario": "STRING",
  "environment": { "javaVersion": "25", "cores": 8, "os": "Windows 11" },
  "rows": [
    { "algorithm": "NAIVE", "inputSize": 100000, "executionTimeNanos": 8821000, "throughputPerSec": 11337, "resultSize": 4 },
    { "algorithm": "KMP",    "inputSize": 100000, "executionTimeNanos": 401500,  "throughputPerSec": 249066, "resultSize": 4 }
  ]
}
```

Sizes capped so a benchmark run stays interactive (NFR-8); 1M allowed under `RUN_LARGE` flag; never
fabricated.

### 11. Playground (PlaygroundController)

All playground endpoints accept explicit inputs and return the same AlgorithmResult envelope as the
dataset-facing endpoints (shared dispatcher, no duplicated logic):

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/playground/kmp` | text + pattern, LPS |
| POST | `/api/playground/z` | text + pattern, Z-array |
| POST | `/api/playground/rabin-karp` | text + pattern, hashes |
| POST | `/api/playground/aho-corasick` | text + patterns |
| POST | `/api/playground/levenshtein` | a + b, matrix |
| POST | `/api/playground/damerau` | a + b, matrix |
| POST | `/api/playground/weighted-edit` | a + b + costs |
| POST | `/api/playground/matrix-chain` | dimensions |
| POST | `/api/playground/tsp` | distance matrix |
| POST | `/api/playground/hamiltonian` | graph + start |
| POST | `/api/playground/tree-diameter` | edges + optional weights |
| POST | `/api/playground/rerooting` | edges + root |
| POST | `/api/playground/sos` | `{ "values": [...], "n" }` |
| POST | `/api/playground/dinic` | inline graph |
| POST | `/api/playground/min-cut` | inline graph |

---

## Validation catalog (HTTP 400)

| Field | Rule |
|---|---|
| `pattern` | non-empty, length >= 1, length <= 10000 |
| `text` (explicit) | non-empty when `scope=EXPLICIT` |
| `numbers` | capacities/costs/edges must be >= 0; `k`/reservoir `1 <= k <= size` |
| `rounds` | `1 <= rounds <= 64` |
| graph | source/sink must exist; at most one edge direction pair; no self loops |
| benchmark `sizes` | each in `[1000, 300000]` default cap; ascending |
| `maxDistance` | `0 <= maxDistance <= 100` |
| matrix chain | `2 <= n <= 100` dimensions |
| TSP n | `2 <= n <= 22` with explicit warning when `2^n` exceeds `5e6` |
| SOS n | `1 <= n <= 20` |

Unknown sample names -> 404; algorithm failure -> 500 with safe message.

---

## Notes on DTO usage

Controllers accept `dto/request/*` and return `dto/response/*`. `AlgorithmResultDto` is a flat
serializable view of `query/QueryResult` + `model/AlgorithmMetrics`. No `dsa` object is ever
serialised directly, which keeps the wire contract stable as implementations evolve.

---

## 12. TextHack laboratory — catalogue, query facade, run sessions (Phases 2–4)

Added by the TextHack rebuild; these endpoints drive the frontend Command Center, TextHack console,
Laboratory and Run Sessions pages.

### 12.1 Catalog (CatalogController, CatalogService, com.loginsight.catalog)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/modules` | the six modules with per-module counts (`algorithmCount`, `exposedCount`, `trackableCount`) and the nested `algorithms` list |
| GET | `/api/modules/{id}` | single module (id ∈ `strings, dp, flow, approximation, randomized, parallel`) |
| GET | `/api/algorithms` | flat catalogue of all 42 algorithm descriptors |
| GET | `/api/algorithms/{key}` | one descriptor (keys match the trace catalogue, e.g. `naive, kmp, z, rabinkarp`) |

`AlgorithmInfo` fields: `key, name, moduleId, moduleLabel, problem, queryType, algorithmType,
canonicalEndpoint, traceEndpoint, timeComplexity, spaceComplexity, tracked, exposed, defaultInput,
description`. `exposed=false` marks library-only algorithms (no HTTP endpoint of their own);
`tracked=true` means a step recorder exists.

Module accent colours are part of the contract so the UI always matches the backend:
strings `#22d3ee`, dp `#a78bfa`, flow `#fbbf24`, approximation `#34d399`, randomized `#f472b6`,
parallel `#60a5fa`.

### 12.2 TextHack query facade (TextHackController, TextHackService, TextHackCommand)

| Method | Path | Body |
|---|---|---|
| POST | `/api/text-hack/query` | `{ "queryClass": "...", "input": { ... } }` |

`queryClass` is one of `PATTERN_SEARCH, FUZZY_MATCH, DOCUMENT_SIMILARITY, CITATION_FLOW,
PROJECT_SCHEDULING, PRIME_TESTING`. Each class routes to one real engine through the existing
QueryDispatcher; responses are `TextHackResponseDto(queryClass, label, moduleId, moduleLabel,
description, recommended[], executed, traceAlgorithmKey)`.

| queryClass | engine | module | traceAlgorithmKey |
|---|---|---|---|
| PATTERN_SEARCH | KMP | strings | `kmp` |
| FUZZY_MATCH | Levenshtein (fuzzy engine) | strings | — |
| DOCUMENT_SIMILARITY | Needleman-Wunsch (+ derived `similarityPercent`/`identityMatches`) | dp | — |
| CITATION_FLOW | Dinic | flow | `dinic` |
| PROJECT_SCHEDULING | Vertex Cover 2-approx | approximation | `vertexcover` |
| PRIME_TESTING | Miller-Rabin (seed param `0x5EED`) | randomized | `millerrabin` |

`executed` is the canonical `AlgorithmResult` envelope; `recommended` is the ordered list of catalog
descriptors the UI offers as "open in laboratory" jumps.

### 12.3 Run sessions and SSE replay (RunController, RunService, com.loginsight.run)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/runs` | `{ "algorithm": <traceable key>, "input": { ... } }` → executes the real trace-instrumented algorithm synchronously and returns the full `RunRecord` |
| GET | `/api/runs` | newest-first `RunSummaryDto` list (max 64 retained) |
| GET | `/api/runs/{id}` | full `RunRecord` (includes recorded `steps`) |
| GET | `/api/runs/{id}/result` | `{ runId, status, result, error, stepCount, executionTimeNanos, truncated }` |
| GET | `/api/runs/{id}/events` | `text/event-stream` replay: `meta` → repeated `step` → `complete` |

`RunRecord`: `runId, algorithm, algorithmName, category, status (QUEUED/RUNNING/COMPLETED/FAILED),
createdAt, completedAt, stepCount, executionTimeNanos, truncated, timeComplexity, spaceComplexity,
input, result, steps, error`. The `steps` list mirrors the trace `TraceStep` shape
(`index, operation, description, state, highlighted, metrics`) and `truncated` is set by the
recorder, never by the API.

SSE framing for `GET /api/runs/{id}/events`:

```
event: meta
data: {"runId":"...","algorithm":"kmp","stepCount":10,"truncated":false,...}

event: step
data: {"index":1,"operation":"LPS_INIT","description":"...","state":{...},"highlighted":[],"metrics":{}}

event: complete
data: {"runId":"...","stepCount":10,"executionTimeNanos":1647700,"truncated":false,"status":"COMPLETED"}
```