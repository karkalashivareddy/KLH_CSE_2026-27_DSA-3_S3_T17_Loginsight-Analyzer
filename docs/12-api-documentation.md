# 12 — Algorithm API Supplement

The product-facing REST and error contract is maintained in [API.md](API.md). This file records the algorithm-laboratory surface retained alongside the rebuilt product shell.

## Result envelope

Algorithmic controllers return `AlgorithmResultDto`:

```json
{
  "algorithm": "KMP",
  "queryType": "PATTERN_SEARCH",
  "inputSize": 1200,
  "pattern": "timeout",
  "result": {},
  "intermediateData": {},
  "executionTimeNanos": 12000,
  "memoryEstimateBytes": 64,
  "timeComplexity": "O(n + m)",
  "spaceComplexity": "O(m) LPS",
  "notes": "..."
}
```

`executionTimeNanos` is measured around the algorithm body. `memoryEstimateBytes` is an engine estimate, not a JVM heap measurement. Internal `dsa` objects are not serialized directly.

## Canonical families

| Family | Current paths |
|---|---|
| String search | `/api/search/naive`, `/kmp`, `/z`, `/rabin-karp`, `/multi`, `/api/string/suffix/build`, `/api/string/suffix/search`, `/api/fuzzy/search` |
| DP | `/api/dp/levenshtein`, `/damerau`, `/weighted-edit`, `/global`, `/local`, `/matrix-chain`, `/obst`, `/tsp`, `/hamiltonian`, `/tree`, `/rerooting`, `/sos` |
| Flow | `/api/flow`, `/edmonds-karp`, `/dinic`, `/min-cut`, `/matching`, `/min-cost` |
| Approximation | `/api/approx/vertex-cover`, `/incident-cover`, `/set-cover` |
| Randomized | `/api/random/prime`, `/sample`, `/hash`, `/quicksort` |
| Parallel | `/api/parallel/reduce`, `/scan`, `/sort`, `/api/benchmark/run` |

The product search endpoints and the catalogue are separate from these explicit laboratory calls. The product search path uses KMP by default; a benchmark compares four matchers; a run session executes a trace-instrumented algorithm.

## Catalogue and traces

- `GET /api/modules` and `GET /api/modules/{id}` return computed module counts.
- `GET /api/algorithms` and `GET /api/algorithms/{key}` return the 42-entry catalogue.
- `GET /api/trace/catalog` returns the 13 trace-capable entries.
- `POST /api/trace/...` returns recorded steps and a `truncated` flag.
- `POST /api/runs` stores a real run; `GET /api/runs/{id}/events` replays `meta → step → complete` over SSE.

The `POST /api/text-hack/query` compatibility facade routes six query classes to existing engines. It is not a research-service or external integration.

## Validation

The current validator and endpoint-specific limits are listed in [API.md](API.md). Important families include text/pattern length, quadratic DP cell budgets, Aho-Corasick pattern/occurrence caps, Miller-Rabin rounds, reservoir stream and `k`, benchmark sweeps, request-factory array/text limits, run history and recorder steps. Invalid input is returned as the standard JSON `400` envelope.

The earlier course-map and phase documents in this directory are historical context. The current product routes, counts and limitations are in [IMPLEMENTATION_AUDIT.md](IMPLEMENTATION_AUDIT.md).
