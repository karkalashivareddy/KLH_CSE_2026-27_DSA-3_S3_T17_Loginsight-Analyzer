# Algorithm Course Map

The frontend Algorithm Lab renders the grouped backend catalogue at `GET /api/analysis/algorithms`. The canonical catalogue and module metadata remain available at `GET /api/algorithms` and `GET /api/modules`, and those are the source of truth for the counts below.

| Module id | Module | Entries | Reachable | Traceable | Library-only examples |
|---|---|---:|---:|---:|---|
| `strings` | String Algorithms | 9 | 8 | 4 | Kasai LCP |
| `dp` | Dynamic Programming | 12 | 12 | 2 | — |
| `flow` | Graph and Flow | 6 | 6 | 3 | — |
| `approximation` | Approximation | 7 | 3 | 1 | Bounded VC, kernelization, FPTAS, VC/IS reduction |
| `randomized` | Randomized | 5 | 4 | 3 | Perfect hashing |
| `parallel` | Parallel | 3 | 3 | 0 | — |
| **Total** | | **42** | **36** | **13** | |

## Status meanings

- **Traceable**: the implementation records ordered steps and can be used through the trace/run surfaces.
- **Reachable**: the catalogue has a canonical or trace REST endpoint.
- **Library-only**: implemented and tested but no standalone endpoint is advertised.
- **Engine**: the dispatcher implementation; 35 engines back the 42 catalogue entries.

## Product mapping

KMP drives product free-text search, Levenshtein drives zero-hit suggestions, token normalization drives Patterns, and five-minute baseline thresholding drives Incidents. The other entries remain honest algorithm-engine, benchmark, trace or laboratory capabilities; they are not all product-panel claims.

## Cross-checks

- Naive, KMP, Z and Rabin-Karp match sets are compared in tests.
- Ford-Fulkerson, Edmonds-Karp and Dinic flow values are compared on shared graphs.
- Parallel reduce, scan and sort are checked against sequential results.
- Trace and run responses expose the recorded result, step count and truncation flag.

The course-lab compatibility facade is `POST /api/text-hack/query`; it routes to existing engines and is not an external research integration. The current system has no WebGL/3D engine, WebSocket transport or trained ML component. The Command Center topology's "3D / depth" mode is a 2.5D CSS transform over a flat SVG, not a 3D renderer. The course rule against `java.util` delegation in `dsa/**` is enforced by `EngineScopeGuardTest`, a frozen per-file `java.util` manifest that fails the build on any new import; see [13-testing.md](13-testing.md).

Where algorithms reach the product:

| Product surface | Algorithm behind it |
|---|---|
| Product search free text | KMP over the rendered candidate text |
| Zero-hit "Did you mean?" | Bounded Levenshtein over distinct messages |
| Patterns | Deterministic token normalization (not ML) |
| Incidents | Five-minute baseline thresholding, rule-based |
| Command Center topology | Group-by-`requestId` ordered adjacency fold over the full dataset, not a catalogue entry |
| Service health bands | Fixed error-rate thresholds |
| Search benchmark | Naive / KMP / Z / Rabin-Karp, one measured run each |
| Run sessions | Recorded execution of a trace-instrumented algorithm, created by `POST /api/runs` and streamed over `GET /api/runs/{id}/events` |

See [COMMAND_CENTER.md](COMMAND_CENTER.md) and [ALGORITHMS.md](ALGORITHMS.md).
