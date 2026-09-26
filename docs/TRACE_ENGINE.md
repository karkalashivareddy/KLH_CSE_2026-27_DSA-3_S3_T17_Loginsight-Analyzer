# Trace Engine

The current trace system records real algorithm operations and replays those records. The frontend does not invent a run.

## Recording

Trace-instrumented algorithms attach a `StepRecorder` during execution. Each `AlgorithmStep` contains:

- `index`
- `operation`
- `description`
- `state`
- `highlighted`
- `metrics`

`StepRecorder.MAX_STEPS` is 400. When the cap is reached, the recorder sets `truncated=true`; the API and UI expose that state.

The current trace catalogue has 13 entries:

- Naive, KMP, Z and Rabin-Karp string search.
- Levenshtein and matrix-chain DP.
- Ford-Fulkerson, Edmonds-Karp and Dinic flow.
- Vertex-cover approximation.
- Randomized QuickSort, Miller-Rabin and reservoir sampling.

## HTTP surfaces

- `GET /api/trace/catalog` returns trace metadata and default inputs.
- `POST /api/trace/search/{naive|kmp|z|rabin-karp}`
- `POST /api/trace/dp/{levenshtein|matrix-chain}`
- `POST /api/trace/flow/{ford-fulkerson|edmonds-karp|dinic}`
- `POST /api/trace/approx/vertex-cover`
- `POST /api/trace/random/{quicksort|prime|sample}`

Trace search endpoints require an explicit text haystack. Flow and graph payloads are remapped from numeric vertex IDs to service names before serialization.

## Run sessions

`POST /api/runs` accepts a traceable algorithm key and input map. `RunService` executes the real algorithm synchronously and stores a `RunRecord` in `RunStore`.

- History is in memory and capped at 64 records.
- A failed run is retained with `status=FAILED`, an error message and no fabricated steps.
- `GET /api/runs/{id}` returns the full record.
- `GET /api/runs/{id}/events` emits `meta`, repeated `step`, then `complete` over SSE.
- The current run replay emitter has a 60-second server timeout.

## Frontend playback

`TracePlayer` treats the recorded step list as immutable state. It supports play/pause, previous/next, first/last, a range control, speed selection, an operation ledger and keyboard shortcuts. Shortcuts are ignored in editable controls. `prefers-reduced-motion` is respected by the stylesheet.

The Nginx deployment disables proxy buffering for `/api/` so the two SSE surfaces are not hidden behind a buffered production proxy. There is no WebSocket implementation in the current trace system.
