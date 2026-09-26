# DSA to Product Mapping

This table describes the current branch. “Product feature” means the algorithm directly powers a reachable product screen or API. “Algorithm engine” means the implementation is reachable through the catalogue, explicit laboratory endpoint or trace/run surface but is not claimed to drive a specific product panel.

## Product features

| Product problem | Current implementation | Endpoint or screen | Honest label |
|---|---|---|---|
| Free-text log search | `dsa/string/KMPMatcher` via `LogSearchService` | `POST /api/search`, `GET /api/logs/explore` | Product feature; KMP reports measured duration |
| Typo suggestion | `dsa/dp/editdistance/LevenshteinDistance` | Product search response `suggestion` | Product feature; bounded zero-hit path |
| Recurring message shapes | `pattern/PatternExtractor` | `GET /api/patterns`, `/examples` | Heuristic token normalization, not ML |
| Elevated-error grouping | `incident/IncidentDetector` | `GET /api/incidents`, detail/evidence | Five-minute baseline heuristic |
| Matcher comparison | `SearchBenchmarkService` | `GET /api/analysis/benchmarks/search` | One measured run per matcher |
| Recorded execution | `StepRecorder`, `RunService`, `RunStore` | `POST /api/runs`, run SSE | Real recorded steps, capped history |

## Current algorithm inventory

| Module | Entries | Reachable | Traceable | Main implementations |
|---|---:|---:|---:|---|
| Strings | 9 | 8 | 4 | Naive, KMP, Z, Rabin-Karp, Aho-Corasick, suffix array/search, Kasai, fuzzy |
| Dynamic Programming | 12 | 12 | 2 | Edit distance, alignment, matrix chain, OBST, bitmask, tree and SOS DP |
| Graph and Flow | 6 | 6 | 3 | Ford-Fulkerson, Edmonds-Karp, Dinic, min-cut, matching, min-cost flow |
| Approximation | 7 | 3 | 1 | Vertex cover, set/incident cover, bounded VC, kernelization, FPTAS, reduction |
| Randomized | 5 | 4 | 3 | Quicksort, Miller-Rabin, reservoir, universal hash, perfect hash |
| Parallel | 3 | 3 | 0 | Reduce, prefix scan, sort |
| **Total** | **42** | **36** | **13** | |

`EngineRegistry` has 35 engines because suffix build/search share one engine. Library-only entries are not presented as product endpoints: Kasai LCP, bounded vertex cover, kernelization, knapsack FPTAS, VC/IS reduction and perfect hashing.

## Current laboratory routes

- String: `POST /api/search/{naive,kmp,z,rabin-karp,multi}`, `/api/string/suffix/{build,search}`, `/api/fuzzy/search`.
- DP: `POST /api/dp/{levenshtein,damerau,weighted-edit,global,local,matrix-chain,obst,tsp,hamiltonian,tree,rerooting,sos}`.
- Flow: `POST /api/flow`, `/api/flow/{edmonds-karp,dinic,min-cut,matching,min-cost}`.
- Approximation: `POST /api/approx/{vertex-cover,incident-cover,set-cover}`.
- Randomized: `POST /api/random/{prime,sample,hash,quicksort}`.
- Parallel: `POST /api/parallel/{reduce,scan,sort}` and `POST /api/benchmark/run`.
- Trace: `/api/trace/catalog` plus the trace routes in [API.md](API.md).
- Compatibility query facade: `POST /api/text-hack/query`.

No 3D, WebSocket, external research integration or trained ML feature is implied by this mapping.
