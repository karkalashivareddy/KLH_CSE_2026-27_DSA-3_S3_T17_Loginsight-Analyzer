# LogInsight Analyzer — Project Walkthrough

A software-engineering tour of the LogInsight Analyzer repository (commit `bbf5e45`).
Read alongside [ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md), [API.md](API.md),
[ALGORITHMS.md](ALGORITHMS.md) and [DSA_PRODUCT_MAPPING.md](DSA_PRODUCT_MAPPING.md).

---

## 1. Product overview

LogInsight Analyzer is a full-stack, in-memory **log analysis and investigation platform** whose
capabilities are implemented with classical data-structure and algorithm techniques. The product
surface (search, patterns, incidents, analytics, live replay) is a shell around a real DSA engine
layer: the same algorithms are exposed as *product features* and, transparently, as an *Algorithm
Insights catalogue* — so the engineering is inspectable, benchmarked and traceable rather than hidden.

The identity rule in this repository:

> Logs → Parsing & normalization → Indexing & search → Algorithmic matching → Analytics →
> Pattern discovery → Incident investigation → Evidence.

## 2. User journey

1. **First run** — Command Center shows "No dataset loaded" and points to Datasets/Ingestion.
2. **Load demo dataset** — one click generates a deterministic 14,000-event corpus.
3. **Explore** — Command Center, Log Explorer, Search, Analytics all show data-derived numbers.
4. **Investigate** — Patterns (heuristic templates), Incidents (windowed, evidence-based).
5. **Understand the machinery** — Algorithm Insights, benchmarks, run sessions with step replay.
6. **Stream** — Live screen replays the dataset over SSE with an honest "not real-time" label.

## 3. Frontend architecture

`frontend/` — React 18 + TypeScript + Vite, no chart/state libraries (charts are hand-rolled SVG).

- **Routing** (`src/App.tsx`): `/` Overview, `/logs`, `/search`, `/analytics`, `/patterns`,
  `/incidents`, `/services`, `/live`, `/datasets`, `/ingestion`, `/analysis`, `/analysis/algorithms`,
  `/analysis/benchmarks`, `/runs`, `/system`, `/docs`.
- **API layer** (`src/api/client.ts`, `types.ts`): typed REST client plus SSE parsers for the live
  stream and run replay. All wire types mirror backend DTOs.
- **Pages** (`src/pages/`): one component per route; each uses a `useApi` hook (loading/error/data).
- **Components** (`src/components/`): `Layout`, reusable UI primitives (`ui.tsx`: Card, StatCard,
  charts, badges), `TracePlayer` (step replay + keyboard), `format.ts` (number/time formatters).
- **State:** local React state per page; data is fetched per page from the backend. No database,
  no global store, no cache layer.
- **Styling:** `src/styles/global.css` — dark control-room theme, responsive breakpoints.

## 4. Backend architecture

`backend/` — Java 21, Spring Boot 3.5, Spring Web, in-memory runtime (no JPA/DB).

- **Package map:** `controller/` (REST), `service/` (business logic), `search/` (product search +
  query parser), `index/` (in-memory index), `pattern/`, `incident/`, `analytics/`, `graph/`,
  `datasets/`, `parser/`, `dsa/` (algorithm implementations), `trace/` (step recorder + replay),
  `run/` (run sessions), `catalog/`, `dto/`, `model/`, `query/` (legacy DSA facade), `config/`.
- **Concurrency:** the live stream uses a 2-thread scheduled pool; run replay uses SSE emitters.
- **Scope guard:** the `dsa` package cannot use general `java.util.*` collections in algorithm code
  (enforced by a test).

## 5. Log ingestion

`parser/` (`JsonLogParser`, `TextLogParser`, `LogParserFactory`) + `service/DatasetService`.

- Sources: **demo generator**, **bundled sample files**, **file upload**.
- Format auto-detection (probe → parse); parsing is **honest**: success and failed lines are both
  counted and reported (e.g. `GET /api/ingestion/status`).
- A dataset is immutable once loaded; only one is current at a time; **Clear** resets to first-run.

## 6. Log normalization

`parser/` produce `ParsedLog`/`LogParseResult`, mapped into the domain `model/LogEvent`:
severity (`LogLevel`), timestamp, service, host, source, HTTP method/status/endpoint, message,
trace/span IDs, attributes, and a `searchableText()` for substring search. Timestamps normalize to
`Instant`; missing/unknown fields are tolerated (nullable) rather than rejected.

## 7. Indexing

`index/` — `LogIndexService` + `LogIndex`.

- Per-field, sorted **position lists**: by severity, service, host, source, status, trace.
- **Time-window lookup** (`inTimeWindow`) via a two-pointer / sorted-positions approach.
- **Intersection** (`LogIndex.intersect`) combines position sets for multi-filter queries.
- `LogIndex` is built when a dataset loads and kept in memory; tests (`LogIndexTest`) verify
  correctness against brute-force.

## 8. Search

`search/` — `SearchQueryParser` (parses `level:`, `service:`, `host:`, `source:`, `status:`,
`trace:`, time ranges, sort), `LogIndex` (filter candidates), `LogSearchService` (execution),
`SearchController`/`SearchService` (HTTP).

- **Structured path:** index position lists are intersected, sorted, and paginated.
- **Free text path:** the candidate events are rendered into one lowercase haystack and matched **once**
  by `KMPMatcher` (default); positions map back to events; hit counts per event are real.
- **Response honestly reports** strategy, algorithm (`KMP`), pattern length, text size and measured
  `durationNanos`.
- **Fuzzy fallback:** zero matches → **"did you mean"** via Levenshtein over distinct messages.
- **Typeahead:** `suggest()` scans live field values and prefixes → field-filter commit chips.

## 9. Analytics

`analytics/` — pure-Java analyzers: `TimelineAnalyzer` (bucketed series), `SeverityAnalyzer`
(level counts), `HeatmapAnalyzer` (7×24 grid), `FleetAnalyzer` (host rollups), `FrequencyAnalyzer`
(top entities), `ErrorPatternAnalyzer`. `AnalyticsService` composes them; endpoints under
`/api/analytics/http|hosts|heatmap|windows|top|errors|dependencies`.

## 10. Pattern extraction

`pattern/PatternExtractor` — **heuristic** token normalization: variable tokens (numbers, UUIDs,
IPs, hex) become placeholders, producing message templates grouped by frequency with per-level
counts and sample events. Deliberately **not ML**; the UI says so.

## 11. Incident detection

`incident/IncidentDetector` — rule-based, evidence-based:

- Only ERROR/FATAL events; fixed **5-minute windows** over the dataset span.
- Baseline = average errors per window; a window is **elevated** at `count ≥ max(3, 3×baseline)`.
- Consecutive or one-window-apart elevated windows merge into incidents, labelled with top services
  and primary pattern; each incident exposes its **supporting logs** (`/api/incidents/{id}/logs`).

## 12. Services

`controller/ServicesController` + `service/…` — per-service rollups
(`GET /api/services`, `GET /api/services/{name}`) with rates, level breakdowns and a 24-hour activity
series for detail drilling.

## 13. Live replay

`service/LiveStreamService` + `controller/LiveController` — SSE emitter streaming the loaded dataset
in bounded batches with `source: demo-replay` and the visible label **"Demo replay stream — not
real-time"**. `GET /api/live/status` exposes honest stream state.

## 14. DSA engine

`dsa/` — six module packages:

- `dsa/string` — Matcher interface + Naive, KMP, Z, Rabin-Karp, Aho-Corasick, SuffixArray/Kasai-LCP.
- `dsa/dp` — edit distance (Levenshtein, Damerau, Weighted), alignment (Needleman-Wunsch,
  Smith-Waterman), tree DP, interval DP, bitmask/TSP, SOS DP.
- `dsa/flow` — Ford-Fulkerson, Edmonds-Karp, Dinic, Min-Cost Max-Flow, Min-Cut, Bipartite Matching.
- `dsa/approximation` — Vertex Cover (2-approx, kernelization, bounded/FPT), Set Cover, Maximal
  Matching, Knapsack FPTAS, Independent Set reduction.
- `dsa/randomized` — Randomized QuickSort, Miller-Rabin, Reservoir Sampling, Perfect/Randomized Hash.
- `dsa/parallel` — Parallel Sort / Reduce / Prefix Scan with work-span analysis and benchmarks.

Engines register into an **EngineRegistry**; the **Algorithm Catalogue** reports
**42 algorithms across 6 modules** (36 exposed, 13 traceable, 35 engines registered).

## 15. Trace system

`trace/` — `StepRecorder` captures `AlgorithmStep`s during execution; `AlgorithmCatalog.markTracked()`
marks the 13 traceable algorithms; `RunService` records runs to `run/RunStore` (cap 64) and replays
steps over SSE (`GET /api/runs/{id}/events`). The frontend `TracePlayer` renders steps, state tables,
a step timeline, and playback controls (play/pause/step/speed) with keyboard support.

## 16. Benchmarking

`service/SearchBenchmarkService` — runs **Naive, KMP, Z-Algorithm, Rabin-Karp** over the *same*
haystack and pattern, one measured execution each; **winner = smallest measured time** with an
explicit "single measured run" disclaimer. `BenchmarkService` also covers parallel-scenario
benchmarks. Conclusion labels stay honest: winning on one run is not a universal claim.

## 17. Dataset system

`datasets/DemoDatasetGenerator` (deterministic, seed `20260913L`, 14,000 events, ~8 services, typed
error bursts), `service/Dataset` (the in-memory model), bundled `sample-data/`, parser auto-detection,
and `dataset` endpoints (list, load demo, post/upload, clear/delete).

## 18. API layer

`controller/*` map 1:1 to `docs/API.md`: overview, logs/explore/detail, search + suggest, patterns +
examples, incidents (+count/detail/evidence), services, analytics (7 sub-endpoints), live (SSE +
status), ingestion, datasets, analysis/algorithms + benchmarks, runs (create/list + SSE replay),
catalogue/traces TextHack facade (legacy, unchanged for compatibility), module controllers
(`/api/strings`, `/api/dp`, `/api/flow`, `/api/approximation`, `/api/randomized`, `/api/parallel`,
`/api/trace/*`). `GlobalExceptionHandler` maps domain exceptions to 4xx with JSON bodies.

## 19. Testing

Backend: **93 test classes / 732 tests / 0 failures / 0 errors** via `.\mvnw.cmd -o verify`
(Maven Wrapper, JaCoCo configured). Coverage areas: every DSA algorithm (incl. cross-checks against
brute force), search parser + service, index, pattern, incident, analytics, datasets, parsers,
controllers, catalog, query facade, trace cross-check, datasets, and scope-guard. Frontend: no unit
test runner configured; verification is `npm run build` (tsc + vite) plus a headless-Chromium QA
pass (59 views, 0 blank routes, 0 JS errors, 0 console errors, responsive at
1440/1280/1024/768/480/375) recorded in `LOGINSIGHT_REBUILD_REPORT.md`.

## 20. Limitations

- In-memory, single-current-dataset design; run history capped at 64 — not multi-tenant, not durable.
- Live screen is a labelled dataset replay, not true streaming ingestion.
- Patterns and incidents are heuristics with visible evidence, not ML/trained models.
- Benchmarks are single measured runs on the host machine.
- No auth, no persistence, no observability-scale deployment claim.

## 21. Future extensions

Presented as *possible* next steps, not commitments: durable storage (DB or file persistence),
authenticated multi-user runs, scheduled/cron dataset refresh, true incremental ingestion into the
index, exporting incident/evidence bundles, an ML classifier trained on the labelled templates
(currently deliberately out of scope), and CI-scheduled browser QA alongside the existing GitHub
Actions Maven + frontend build checks.