# Algorithms

The current backend exposes a 42-entry algorithm catalogue across six modules. The catalogue is metadata over implemented Java engines; it is not a claim that every algorithm drives a product panel.

| Module | Catalogue entries | Reachable entries | Traceable entries |
|---|---:|---:|---:|
| Strings | 9 | 8 | 4 |
| Dynamic Programming | 12 | 12 | 2 |
| Graph and Flow | 6 | 6 | 3 |
| Approximation | 7 | 3 | 1 |
| Randomized | 5 | 4 | 3 |
| Parallel | 3 | 3 | 0 |
| **Total** | **42** | **36** | **13** |

`EngineRegistry` contains 35 query engines. The count differs from reachable catalogue entries because suffix build/search share one engine and several entries are library-only.

## Product-facing algorithms

### Search

Product search first resolves structured filters through the in-memory index, then runs free text with KMP over the lowercased searchable text of the candidate events. The response reports the matcher, pattern length, text size and measured `durationNanos`.

The four single-pattern engines are also exposed directly:

- Naive: `POST /api/search/naive`
- KMP: `POST /api/search/kmp`
- Z-Algorithm: `POST /api/search/z`
- Rabin-Karp: `POST /api/search/rabin-karp`

`POST /api/search/multi` uses Aho-Corasick. Suffix build/search and fuzzy search are available at the string-lab endpoints listed in [API.md](API.md).

When free text has no result, the product path computes a bounded Levenshtein suggestion over distinct message values. The suggestion includes distance, similarity percentage and match count; it is not a canned response.

### Patterns

`PatternExtractor` normalizes message tokens. Numeric, dotted-quad, hex-like and punctuation-bearing variable tokens become `<*>`; remaining punctuation is stripped from static tokens. Templates are ranked by frequency and retain a sample event. This is deterministic token normalization, explicitly not machine learning.

### Incidents

`IncidentDetector` considers ERROR/FATAL events, places them in fixed five-minute windows, computes the dataset error baseline, marks windows at or above `max(3, ceil(3 × baseline))`, and merges consecutive or one-window-apart elevated windows. Each incident exposes its method, primary pattern, services, window and supporting logs. It is a rule-based heuristic, not a trained anomaly model.

The Command Center reuses this detector over the selected window and labels the result as heuristic. The "Detected incident windows" card counts returned windows, and the health bands shown next to it are fixed error-rate thresholds (healthy <5%, watch 5–<10%, elevated ≥10%, unavailable when the rate is not finite). Neither is a health model or an SLI evaluation.

### Analytics

Pure-Java analyzers provide timeline buckets, severity histograms, a 7×24 UTC heatmap, service/host/HTTP rollups, top-K frequency buckets, error summaries and a service dependency graph. HTTP latency percentiles are measured from response-time fields on the active dataset.

### Observed request-trail adjacency

`ServiceGraphBuilder` builds the graph behind the Command Center topology and `GET /api/analytics/dependencies`. It is one deterministic ordered pass over the dataset, not a catalogue entry:

1. Group events by `requestId` in ingestion order, preserving that order.
2. Sort each group by `(timestamp, id)` so the fold is independent of ingestion order.
3. For each event, link the previous service to the current one whenever the two differ, and increment the `a->b` trail counter.
4. `nodeCountsFrom` counts events per service for node sizing; `topNodes` returns the descending frequency order with a name tiebreak.

Edge `weight` is the observed pair count, which is what drives the topology's stroke width, opacity, curvature and deterministic particle count. The output is an observation of adjacency inside the loaded logs over the full current dataset; it is not verified infrastructure topology, and a missing edge is not evidence that a call did not occur. `ServiceGraphBuilderTest` covers the fold, the ordering and the request-trail counts.

### Search benchmark

`GET /api/analysis/benchmarks/search?pattern=...` runs Naive, KMP, Z-Algorithm and Rabin-Karp over the same rendered dataset haystack. It measures one execution per matcher and chooses the smallest measured time. The response says that no averages are fabricated; the result is host- and input-dependent.

## Laboratory and trace catalogue

The algorithm catalogue exposes six modules:

- **Strings**: Naive, KMP, Z, Rabin-Karp, Aho-Corasick, suffix array build/search, Kasai LCP and fuzzy search.
- **DP**: Levenshtein, Damerau, weighted edit, Needleman-Wunsch, Smith-Waterman, matrix chain, optimal BST, bitmask TSP, Hamiltonian, tree diameter, rerooting and SOS DP.
- **Flow**: Ford-Fulkerson, Edmonds-Karp, Dinic, min-cut, bipartite matching and min-cost max-flow.
- **Approximation**: vertex cover, greedy set cover, incident cover, bounded vertex cover, kernelization, knapsack FPTAS and VC/IS reduction.
- **Randomized**: randomized quicksort, Miller-Rabin, reservoir sampling, universal hashing and perfect hashing.
- **Parallel**: reduce, prefix scan and sort.

The canonical catalogue is available at `GET /api/algorithms` and `GET /api/modules`; the Algorithm Lab page itself reads the grouped product-lab view at `GET /api/analysis/algorithms`. Trace execution is available under `GET /api/trace/catalog` and the trace routes in [API.md](API.md). Trace responses are recorded steps, capped at 400 and marked `truncated` when the cap is reached.

Some entries are library-only: Kasai LCP, bounded vertex cover, kernelization, knapsack FPTAS, VC/IS reduction and perfect hashing do not have their own product endpoint. Ford-Fulkerson is trace-reachable; the current flow controller also exposes the implementation at `POST /api/flow`, while the catalogue intentionally records its canonical exposure as trace-only.

## Trace and run sessions

Trace-instrumented algorithms record operation, description, state, highlights and metrics during execution. `POST /api/runs` executes a real traceable algorithm synchronously, stores the record in the bounded `RunStore`, and returns it. `GET /api/runs/{id}/events` replays the recorded steps over SSE as `meta`, `step` and `complete` events. The browser renders those steps; it does not animate an invented computation.

In the shell, the Algorithm Lab group covers the catalogue (`/algorithms`, read from `GET /api/analysis/algorithms`), the measured matcher comparison (`/benchmarks`) and the recorded sessions (`/runs`), which list `GET /api/runs`, open one record and stream its ledger. Run creation happens server-side through `POST /api/runs`.

Run history is capped at 64 and resets on restart. Parallel algorithms are benchmarked but are not trace-instrumented in the current catalogue.

## Academic `java.util` scope guard

DSA-3 forbids delegating core algorithm logic in `dsa/**` to `java.util` collections. Because the pre-rebuild engines already imported a bounded set of `java.util` types, the guard is enforced as a frozen ledger by `EngineScopeGuardTest`: it records the exact allowed `java.util` import suffixes per `dsa` source file, walks every `.java` file in `backend/src/main/java/com/loginsight/dsa`, and fails the build on any import that is not in the manifest. Removing recorded usage is always allowed, so the ledger can only shrink. `java.util.concurrent` (parallel) and `java.util.Random` (randomized) are course-licensed and are listed. Packages outside `dsa` are unrestricted, and the manifest is a course-scope contract rather than a claim that no `java.util` type appears under `dsa` today.

## Explicit non-features

The current implementation does not include a WebGL or WebGPU 3D engine, WebSocket transport, external research integration, trained ML model, external live collector or production telemetry pipeline. Those are not part of the algorithm catalogue or product claims.

The Command Center topology's "3D / depth" mode is a 2.5D CSS `perspective` and `rotateX` transform over a flat SVG in `components/TopologyPanel.tsx`. The panel prints "not WebGL" in both display modes. Describing it as a 2.5D SVG/CSS presentation, rather than a 3D renderer, is the accurate claim.

See [COMMAND_CENTER.md](COMMAND_CENTER.md) for the field-level window, topology and health semantics.
