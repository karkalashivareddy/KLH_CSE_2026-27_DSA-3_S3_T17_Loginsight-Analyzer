# API Reference

Base path: `/api`.

The Vite development server proxies `/api` to `http://localhost:8080`. The Compose deployment serves the SPA and proxies `/api/` to the backend service through Nginx. API responses are JSON except the three SSE endpoints: `GET /api/simulation/stream` (generated simulation), `GET /api/live` (dataset replay) and `GET /api/runs/{id}/events` (recorded trace replay).

## Error contract

`GlobalExceptionHandler` normally returns:

```json
{
  "status": 404,
  "error": "DatasetException",
  "message": "No dataset loaded",
  "timestamp": "2026-09-25T00:00:00Z",
  "path": "/api/overview"
}
```

Stack traces are not returned. `error` is normally the exception class simple name.

| Status | Current meaning |
|---:|---|
| 400 | Invalid query/log input, illegal argument, malformed or missing body, invalid parameter/date, bounds failure, upload over 64 MB, **or a parse failure (`ParserException` and its `UnsupportedLogFormatException` subclass)** |
| 404 | Missing dataset, unknown event/service/incident/run/sample, or unknown endpoint/resource |
| 405 | HTTP method not supported |
| 406 | Request cannot accept the available representation |
| 409 | An incident lifecycle transition the current state does not allow (`IllegalLifecycleTransitionException`). Lifecycle moves forward only; an illegal target is rejected visibly rather than silently accepted |
| 415 | Unsupported request content type |
| 500 | Algorithm execution failure or unexpected server failure, with a sanitized message |

A client-side request timeout is normalized by the frontend as status `408`; it is not a backend response.

**Parse failures are 400, not 500.** `ParserException` is mapped explicitly in `GlobalExceptionHandler`. Previously it had no handler, so an empty or unparsable upload fell through to the generic `Exception` handler and answered HTTP 500 — reporting an ordinary client mistake as a server fault. The concrete observable case is a zero-byte or whitespace-only `POST /api/datasets`, which now answers:

```json
{
  "status": 400,
  "error": "UnsupportedLogFormatException",
  "message": "input stream is empty",
  "timestamp": "2026-09-25T00:00:00Z",
  "path": "/api/datasets"
}
```

`UnsupportedLogFormatException` extends `ParserException`, so the whole parse-failure family inherits the mapping.

Two presence endpoints intentionally use a small 404 body instead of the normal error envelope:

- `GET /api/health/dataset` → `{ "loaded": false }`.
- `GET /api/datasets/current` → `{ "loaded": false }`.

`POST /api/datasets/{name}` also returns `{ "loaded": false, "error": "..." }` for an unknown bundled sample. `GET /api/live/status` returns `200` with `enabled: false` when no dataset is loaded.

## Product endpoints

### Health and runtime

| Method | Endpoint | Behavior |
|---|---|---|
| GET | `/api/health` | Liveness: `status`, `service`, `timestamp` |
| GET | `/api/health/ready` | Liveness plus `ready: true` |
| GET | `/api/health/status` | Uptime, dataset presence/name/size and registered engine count |
| GET | `/api/health/dataset` | Dataset presence, or the special 404 body above |

`/api/health/status` is the real runtime status surface. Its fields are `status`, `service`, `timestamp`, `uptimeMillis`, `datasetLoaded`, `datasetName`, `datasetSize` and `engines`. The `systemStatus` string on `OverviewDto` is a separate compatibility field that the backend always sets to `"Operational"`; it is not derived from this endpoint and carries no runtime information. See [COMMAND_CENTER.md](COMMAND_CENTER.md).

### Datasets and ingestion

| Method | Endpoint | Body or parameters |
|---|---|---|
| GET | `/api/datasets` | Lists bundled sample filenames |
| POST | `/api/datasets/demo` | Generates and installs the demo dataset |
| POST | `/api/datasets` | Multipart `file`, optional `name` |
| POST | `/api/datasets/{name}` | Loads one bundled sample |
| GET | `/api/datasets/current` | Current summary or special 404 |
| DELETE | `/api/datasets` | Clears the current dataset |
| GET | `/api/ingestion/status` | Parser, replay label, available samples and current dataset |
| POST | `/api/ingestion/demo` | Alias for demo loading |

`POST /api/datasets/demo`, upload and ingestion-demo return `DatasetResultDto`; its `loaded` field is the string `"true"` in this record. The health/current presence endpoints return a boolean `loaded` field. Counts are real parse results: `size`, `totalLines` and `failedLines`.

#### Ingestion failure contract

`POST /api/datasets` routes every rejection through the standard error envelope:

| Condition | Status | `error` | Message |
|---|---:|---|---|
| Zero-byte or whitespace-only body | 400 | `UnsupportedLogFormatException` | `input stream is empty` |
| Non-empty but unparsable | 400 | `UnsupportedLogFormatException` / `ParserException` | parser message, no stack trace |
| Body over 64 MB | 400 | `MaxUploadSizeExceededException` | upload-size message |
| Parse ran but yielded no events | 404 | `DatasetException` | `Log ingestion produced no records` |
| Missing `file` part | 400 | `MissingServletRequestParameterException` | names the required part |

The `name` parameter is normalized before use: trimmed, and blank or null falls back to `imported-logs`. The empty-body check does **not** depend on whether a name was supplied — an unnamed empty upload and a named empty upload both answer 400.

### Overview and logs

| Method | Endpoint | Parameters and behavior |
|---|---|---|
| GET | `/api/overview` | `range=5m|15m|1h|6h|24h`, default `1h`; unknown values fall back to `1h` |
| GET | `/api/logs` | `limit=1..1000`, `offset>=0`; ingestion-order slice |
| GET | `/api/logs/first` | First event in the current dataset |
| GET | `/api/logs/stats` | Dataset counts, levels, status codes, top services and time buckets |
| GET | `/api/logs/{id}` | One dataset-assigned event; unknown ID is 404 |
| GET | `/api/logs/explore` | `q`, optional ISO `from`/`to`, `page`, `size`, `sort` |

`from` and `to` form a half-open `[from,to)` index window. `LogSearchRequest` defaults to page 1, size 25 and `timestamp:desc`; size must be 1–200 and page must be at least 1. `from` must not be after `to`.

#### `/api/overview` selected-window semantics

`/api/overview` is a **selected-window** snapshot, not a whole-dataset rollup.

- `windowEnd` is the newest `timestamp` in the loaded dataset, or the current instant when no event carries one. `windowStart` is `windowEnd` minus the nominal range width, so the window is a trailing slice of the data rather than of the wall clock.
- Window membership is inclusive at both ends: `timestamp >= windowStart && timestamp <= windowEnd`.
- `scope` is the literal string `selected-window`.
- `events`, `errors`, `warnings`, `services`, `hosts`, `severity`, `statusCodes`, `topServices`, `topPatterns`, `recentCritical`, `timeline` and `heatmap` are all computed over that window only. `recentCritical` is at most the 12 newest `ERROR`/`FATAL` window events.
- `activeIncidents` is the number of heuristic windows returned by `IncidentDetector` over the window's events, subject to the detector's 200-window cap. It counts detected windows, not open or ongoing incidents.
- `datasetEvents` is the **unfiltered** size of the loaded dataset. Clients should use it as the denominator for selected-window coverage, which is how the Command Center uses it.
- `eventsPerMinute` is `window events / (range width in minutes)`. The denominator is the nominal range width, not the observed span between the first and last in-window event, so a clustered window reads low. It is a normalized window rate, not a measured inter-arrival rate.
- `systemStatus` is a compatibility field that is always the string `"Operational"`. Real runtime status comes from `/api/health/status`.

`OverviewDto` also has a shorter 18-argument constructor retained for compatibility. It sets `datasetEvents` to the window `events` value and leaves `windowStart`, `windowEnd` and `scope` null. The REST path always uses the full constructor.

### Product search

`POST /api/search` accepts:

```json
{
  "query": "level:ERROR service:auth payment",
  "from": null,
  "to": null,
  "page": 1,
  "size": 25,
  "sort": "timestamp:desc"
}
```

Recognized query fields are `level:`, `service:`, `host:`, `source:`, `status:`, `trace:`, `request:` and `message:`. Other tokens are free text. Structured filters use index intersections; free text is matched by KMP over the rendered candidate text. A structured-only search can have `algorithm: null`.

A response contains `query`, `strategy`, `algorithm`, `pattern`, `patternLength`, `textSize`, measured `durationNanos`, `total`, paging fields, dataset name, `matches` and optional `suggestion`. A suggestion is computed with Levenshtein only after a free-text zero-hit result.

`GET /api/search/suggest?q=&limit=` returns dataset-derived field candidates. The backend caps the result at 50; the frontend requests a smaller bounded value.

### Patterns, incidents and services

| Method | Endpoint | Parameters |
|---|---|---|
| GET | `/api/patterns` | `level=all` by default, `limit=1..200`; level must be a valid `LogLevel` or `all` |
| GET | `/api/patterns/examples` | Required non-blank `template` up to 2,000 characters, `limit=1..200` |
| GET | `/api/incidents` | `limit>=1`, capped at 200 internally |
| GET | `/api/incidents/count` | Count from the same capped detection pass |
| GET | `/api/incidents/{id}` | Optional `logs`, evidence capped at 1,000 |
| GET | `/api/incidents/{id}/logs` | `limit>=1`, `offset>=0`, evidence capped at 1,000 |
| GET | `/api/services` | `limit=1..200` |
| GET | `/api/services/{name}` | Optional `recent=1..500`; unknown service is 404 |

Patterns use deterministic token normalization and are not ML. Incidents use the documented five-minute baseline heuristic and expose the method plus supporting events.

### Analytics

| Method | Endpoint | Response |
|---|---|---|
| GET | `/api/analytics/http` | Status/method/endpoint counts and measured latency percentiles |
| GET | `/api/analytics/hosts` | `limit=1..200`; host rows with events, errors, warnings and error rate |
| GET | `/api/analytics/heatmap` | 7×24 UTC cells, including zero cells |
| GET | `/api/analytics/windows` | `buckets=1..100`; time-bucket series |
| GET | `/api/analytics/top` | `dimension=service|endpoint|level|ip`, `limit=1..50` |
| GET | `/api/analytics/errors` | `limit=1..100`; top error and pattern counts |
| GET | `/api/analytics/dependencies` | Service nodes and request-trail edges |

All analytics require the current dataset and are derived from it.

`/api/analytics/dependencies` returns `nodes`, `edges`, `nodeCount` and `edgeCount`. `ServiceGraphBuilder` groups events by `requestId`, sorts each group by `(timestamp, id)`, and creates a directed `source -> target` edge for each consecutive pair of distinct services. `weight` is how many times that ordered pair was observed; `events` is the node's event count.

This is an observed request-trail adjacency inferred from log co-occurrence over the **full current dataset** (it is not scoped to the `/api/overview` window). It is not verified infrastructure topology, and a missing edge is not proof that a call did not happen.

### Deterministic simulation

Nine endpoints back the generated simulation. Its traffic is **server-generated from a scenario
definition and a seed**, not captured telemetry, and every payload says so. This surface is entirely
separate from the dataset replay below.

| Method | Endpoint | Behavior |
|---|---|---|
| GET | `/api/scenarios` | Scenario catalogue with title, description, origin service, affected services, expected signal, expected incident, seed and severity |
| GET | `/api/scenarios/{id}` | One scenario; unknown id is 400 |
| GET | `/api/scenarios/default` | The default scenario (`checkout-5xx-cascade`) |
| GET | `/api/simulation/status` | Capability, scenario count, active sessions, speed/interval/frame bounds and the determinism and persistence statements |
| GET | `/api/simulation/stream` | `text/event-stream`; emits `start`, then `frame` per tick, then `complete` |
| GET | `/api/simulation/sample` | Runs a scenario forward without a stream and returns the final frame |
| GET | `/api/simulation/incidents?sessionId=` | Incidents recorded for a session, so an investigation continues after the stream ends |
| POST | `/api/simulation/incidents/{id}/transition` | Operator lifecycle action; `409` if the target state is illegal from the current one |
| GET | `/api/simulation/lifecycle` | Accepted lifecycle states and the legal forward transitions |

**`/api/simulation/stream` parameters.** `scenario` (blank selects the default), `seed` (blank uses the
scenario's declared seed), `speed` `0.25..8`, `intervalMs` `40..5000`, `maxFrames` `1..20000`.
Out-of-range values return the normal 400 envelope.

- `speed` changes **delivery pace only**. It never changes which events are produced, so slowing the
  stream cannot change the outcome of a run.
- A frame is a pure function of `(scenario, seed, tick)`: the same triple reproduces the same events,
  signals, health rollup and incidents. Every event in a frame carries a deterministic unique id.
- `start` carries `sessionId`, `source`, scenario id and title, seed, tick millis, speed, pace,
  `expectedSignal`, `expectedIncident` and a generated-traffic label.
- `frame` carries the analytical snapshot: phase, intensity, cumulative and per-window error rate,
  throughput, average and p95 latency against the measured baseline, matched signatures, that tick's
  events, measured signals, algorithm evidence with runtimes, service health with blast-radius
  membership, the current incident list, and the **declared** topology.
- `complete` carries frames, ticks and a reason (`scenario-window-completed` or `frame-cap-reached`).

**Lifecycle.** `DETECTED → INVESTIGATING → ACKNEGATED → MITIGATED → RESOLVED`. Transitions are
forward-only; an illegal target is rejected with `409`. The body accepts `sessionId` with either
`status` (an explicit target) or `advance: true` (one legal step). Lifecycle state is session-scoped
and in-memory: it is never written to the dataset and does not survive a backend restart.

`GET /api/simulation/sample?frames=` is bounded `1..1000` and pins its origin to the epoch and its
identity range to the preview range, so two runs of the same `(scenario, seed, frames)` triple return
identical frames.

### Dataset Replay

`GET /api/live?batchSize=50&intervalMs=700` returns `text/event-stream`. This replays the **loaded
dataset**; the payload identifies itself as `source: "demo-replay"`. The frontend route is `/replay`
with the navigation label `Dataset Replay`; the page's own disclosure copy still reads "Demo replay of
the loaded dataset". `/live` is a different surface — the Live Monitor generated simulation above.

- `batchSize` must be 1–200.
- `intervalMs` must be 100–60,000 ms. Values outside either range return the normal 400 envelope.
- `start` contains `source: "demo-replay"`, dataset, total, batch size, pace, the ordering key `timestamp,id` and a disclosure label.
- `batch` contains sequence, source, dataset, emitted count, total and events.
- `replay-complete` contains emitted and total counts, then the stream closes.

Events are snapshotted at subscribe time and emitted **oldest-first**, ordered by `(timestamp, id)`. The stream is finite: it ends with `replay-complete` when the dataset is exhausted, and the emitter has a 6-hour ceiling. It is a replay of the current in-memory list, not external live ingestion.

### Analysis, catalogue and runs

| Method | Endpoint | Behavior |
|---|---|---|
| GET | `/api/analysis/algorithms` | Product-lab groups of catalogue metadata |
| GET | `/api/analysis/benchmarks/search?pattern=` | One measured run for Naive, KMP, Z and Rabin-Karp |
| GET | `/api/modules` | Six module descriptors with computed counts |
| GET | `/api/modules/{id}` | One module; unknown ID is 400 |
| GET | `/api/algorithms` | Flat 42-entry catalogue |
| GET | `/api/algorithms/{key}` | One catalogue entry; unknown key is 400 |
| POST | `/api/runs` | Execute a traceable algorithm and store a run record |
| GET | `/api/runs` | Newest-first summaries, maximum 64 |
| GET | `/api/runs/{id}` | Full run record; unknown ID is 400 |
| GET | `/api/runs/{id}/result` | Result/status summary |
| GET | `/api/runs/{id}/events` | SSE `meta`, repeated `step`, then `complete` |

`POST /api/runs` is the run-creation entry point used by the Algorithm Lab group: it executes the algorithm server-side, records its steps in the bounded `RunStore` and returns the record. `/api/runs` and `/api/runs/{id}/events` are what the Run Sessions page reads and streams.

**SSE emitter lifecycle.** All three streaming endpoints — `/api/simulation/stream`, `/api/live` and `/api/runs/{id}/events` — register `onCompletion`, `onTimeout` and `onError` on their emitter, so an abandoned stream releases its worker immediately rather than holding it until the emitter timeout. `RunService` additionally performs a bounded 5 s `awaitTermination` on shutdown, so in-flight replays finish or are cancelled cleanly rather than being dropped mid-flight. Each stream body is guarded so that completion, cancellation and failure are mutually exclusive and idempotent.

## Algorithm laboratory endpoints

Algorithmic endpoints return the `AlgorithmResultDto` shape: `algorithm`, `queryType`, `inputSize`, optional `pattern`, `result`, `intermediateData`, `executionTimeNanos`, `memoryEstimateBytes`, time/space complexity and `notes`.

### Search and string structures

- `POST /api/search/naive`
- `POST /api/search/kmp`
- `POST /api/search/z`
- `POST /api/search/rabin-karp`
- `POST /api/search/multi` — Aho-Corasick
- `POST /api/string/suffix/build`
- `POST /api/string/suffix/search`
- `POST /api/fuzzy/search`

### Dynamic programming

- `POST /api/dp/levenshtein`
- `POST /api/dp/damerau`
- `POST /api/dp/weighted-edit`
- `POST /api/dp/global`
- `POST /api/dp/local`
- `POST /api/dp/matrix-chain`
- `POST /api/dp/obst`
- `POST /api/dp/tsp`
- `POST /api/dp/hamiltonian`
- `POST /api/dp/tree`
- `POST /api/dp/rerooting`
- `POST /api/dp/sos`

### Flow, approximation, randomized and parallel

- `POST /api/flow` — Ford-Fulkerson implementation route.
- `POST /api/flow/edmonds-karp`
- `POST /api/flow/dinic`
- `POST /api/flow/min-cut`
- `POST /api/flow/matching`
- `POST /api/flow/min-cost`
- `POST /api/approx/vertex-cover`
- `POST /api/approx/incident-cover`
- `POST /api/approx/set-cover`
- `POST /api/random/prime`
- `POST /api/random/sample`
- `POST /api/random/hash`
- `POST /api/random/quicksort`
- `POST /api/parallel/reduce`
- `POST /api/parallel/scan`
- `POST /api/parallel/sort`
- `POST /api/benchmark/run`

### Trace and compatibility facade

Trace endpoints are under `/api/trace`:

- `GET /api/trace/catalog`
- `POST /api/trace/search/{naive|kmp|z|rabin-karp}`
- `POST /api/trace/dp/{levenshtein|matrix-chain}`
- `POST /api/trace/flow/{ford-fulkerson|edmonds-karp|dinic}`
- `POST /api/trace/approx/vertex-cover`
- `POST /api/trace/random/{quicksort|prime|sample}`

`POST /api/text-hack/query` remains a compatibility laboratory facade for six query classes. It is not a separate product data source and does not imply a research integration.

## Explicit limits

| Surface | Limit |
|---|---|
| Multipart upload and service ingestion | 64 MB |
| `GET /api/logs` | `limit` 1–1000; `offset` non-negative |
| Product search | `page>=1`; `size` 1–200 |
| Search suggestions | Backend maximum 50 |
| Incident list/detail | Detection maximum 200; evidence maximum 1,000 |
| Replay | Batch 1–200; interval 100–60,000 ms; invalid controls are 400 |
| Run history | 64 records |
| Recorded trace | 400 steps; truncation is flagged |
| Text/pattern validators | Text up to 2,000,000 characters; pattern up to 100,000 characters |
| Quadratic DP | Sequence length at most 5,000 per side and 25,000,000 cells |
| Aho-Corasick | 1,024 patterns, 200,000 total pattern characters and 200,000 reported occurrences |
| Miller-Rabin canonical request | `n>=2`, rounds 1–1,000 |
| Canonical reservoir | Stream at most 2,000,000; `k` at most 100,000 and no larger than the stream |
| Benchmark sweep | At most 12 sizes, each 1–2,000,000; repetitions 1–20 |
| RequestFactory laboratory inputs | Arrays at most 4,096 entries, text at most 1,000,000 characters, interval dimensions at most 40 |
| Parallel worker requests | Positive requests are capped at 64; `0` selects the machine-reported core count |

Some legacy laboratory endpoints have additional shape-specific checks. Clients should send the documented body for the selected endpoint and treat a `400` envelope as a validation response.
