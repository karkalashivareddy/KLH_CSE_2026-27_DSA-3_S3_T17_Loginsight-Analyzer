# LogInsight — Current Implementation Audit

Audit snapshot: frontend redesign and guided-demo revision. The exact commit carrying this snapshot is the repository HEAD that contains this file.

This is the canonical description of the checked-out implementation. It is based on the current source, tests, frontend routes, build files and API controllers. Historical rebuild reports remain in [`docs/archive/`](archive/); they are not the current contract.

**Terminology.** These documents use **Dataset Replay** for the bounded replay of the loaded dataset served by `GET /api/live`. Its frontend route is `/replay`, its sidebar entry and page heading read `Dataset Replay`, the backend reports `source: "demo-replay"` and the UI shows a "not real-time" disclosure. The `/live` route is a different surface: **Live Monitor**, the deterministic server-generated simulation served by `GET /api/simulation/stream` and reporting `source: "live-simulation"`. The two are never interchangeable.

## Verification snapshot

| Gate | Command | Result |
|---|---|---|
| Backend | `cd backend; .\mvnw.cmd -o verify` | 878 tests, 0 failures, 0 errors; Spring Boot jar packaged |
| Frontend tests | `cd frontend; npm.cmd test -- --reporter=dot` | 56 tests across 18 files passed |
| Frontend build | `cd frontend; npm run build` | TypeScript and Vite production build passed |
| Compose syntax | `docker compose config --quiet` | Passed |
| Container runtime | `docker compose up --build` | Not run: Docker daemon unavailable in this environment |

CI mirrors these gates: the backend job runs `mvn -B verify` and the frontend job runs `npm ci`, `npm test` and `npm run build`.

Backend verify completed locally: 878 tests, 0 failures, 0 errors, 0 skipped, and the Spring Boot jar was packaged. The frontend test result includes the guided-demo regression. The TypeScript and production build passed; the production build emits a 576.56 kB minified lazy topology chunk (145.44 kB gzip) and warns about chunks over 500 kB. Docker is unavailable (`docker info` cannot reach `docker_engine`), so image builds and container smoke tests remain unexecuted and unclaimed. Browser visual verification and viewport screenshots were not run; no current screenshots are represented as evidence.

GitHub Actions run [38042812637](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38042812637), for commit `e09c49fda898c2edb28ff4109bd61403096d29ca`, completed successfully. Its Backend tests and Frontend tests and build jobs both passed.

The Maven build targets Java 21 and uses Spring Boot 3.5.16. The frontend uses React 18, TypeScript, Vite, React Router and Vitest 5; the Vitest suite requires Node 22.12 or newer. The frontend has no configured browser-test or lint command.

## Runtime shape

```text
Browser
  └─ React SPA
       ├─ REST: /api/*
       └─ SSE: /api/simulation/stream, /api/live and /api/runs/{id}/events
              │
              ├─ Vite dev proxy: /api -> localhost:8080
              └─ Nginx deployment proxy: /api/ -> backend:8080
                                      │
                              Spring Boot API
                                      │
                         one in-memory current Dataset
                                      │
              parsers, index, search, analytics, heuristics,
              catalogue, engines, traces and run history
```

The backend is a single Spring Boot process. `DatasetService` owns one current dataset. `LogIndex` provides sorted field position lists, token positions and half-open timestamp windows. Controllers are thin transport adapters; services and query engines perform the work.

The production frontend is static Vite output served by Nginx. Nginx and Vite are both configured for `/api`; there is no frontend-side API mock and no browser-generated replacement dataset.

## Shell audit

The current `frontend/src/App.tsx` routes are:

- `/` and `/command-center`: Command Center.
- `/logs` and `/logs/:id`: explorer and event detail.
- `/search`: product search and fuzzy suggestion.
- `/analytics` and `/analyze`: analytics.
- `/patterns`, `/incidents`, `/incidents/:id`, `/investigate/:id`, `/services` and `/services/:id`. `/investigate/:id` is an alias of the same `IncidentsPage` detail view and is registered in the command palette under `/incidents`.
- `/scenario-lab` and `/simulation`: Scenario Lab (scenario catalogue, run controls, generated telemetry).
- `/live`: Live Monitor (the deterministic generated simulation streamed by `GET /api/simulation/stream`).
- `/replay`: Dataset Replay (bounded replay of the loaded dataset streamed by `GET /api/live`).
- `/incidents/workbench`: Incident Workbench (simulation incident investigation, lifecycle, blast radius, algorithm evidence).
- `/datasets`, `/data` and `/ingestion`.
- `/analysis`, `/lab`, `/algorithm-lab`, `/algorithms`, `/analysis/algorithms`, `/benchmarks` and `/analysis/benchmarks`.
- `/runs` and `/runs/:id`.
- `/system`, `/system/status`, `/docs` and a not-found route.

`Layout.tsx` provides grouped navigation, breadcrumbs, runtime/dataset status, a `Ctrl+K` command palette, a skip link and a mobile navigation drawer. `api/client.ts` uses the `/api` base path, normalizes the backend error envelope and parses the three SSE streams: `/api/simulation/stream` (generated simulation), `/api/live` (dataset replay) and `/api/runs/{id}/events` (recorded trace replay). No WebSocket client exists.

`replay/ReplayContext.tsx` adds a `ReplayProvider` around the router in `App.tsx`. It owns the single Demo Replay SSE subscription, the recent-event buffer and the dataset-invalidation reset, so Command Center and the replay screen read the same state instead of opening competing streams.

## Command Center audit

Full detail is in [COMMAND_CENTER.md](COMMAND_CENTER.md). The audited facts:

- **Selected window.** `OverviewService.snapshot` anchors `windowEnd` on the newest event timestamp in the loaded dataset and sets `windowStart = windowEnd - range`, filtering inclusively at both ends. `scope` is the literal `selected-window`. `events`, `errors`, `warnings`, `services`, `hosts`, `severity`, `statusCodes`, `topServices`, `topPatterns`, `recentCritical`, `timeline` and `heatmap` are window-scoped. `datasetEvents` is the unfiltered dataset size and drives the coverage percentage. `activeIncidents` is the count of heuristic windows the detector returns for the window, capped at 200.
- **eventsPerMinute.** `window events / (range width in minutes)`. The denominator is the nominal range width, not the observed in-window span, so a clustered window reads low. It is a normalized window rate, not an inter-arrival measurement.
- **Health vs compatibility field.** `OverviewDto.systemStatus` is hardcoded to `"Operational"` by the service and carries no runtime information. Real status is `GET /api/health/status` (`uptimeMillis`, `datasetLoaded`, `datasetName`, `datasetSize`, `engines`). `OverviewPage` renders `health.data?.status ?? data.systemStatus`, so the compatibility string appears only as a pre-resolution fallback.
- **Observed request-trail topology.** `TopologyPanel` renders `GET /api/analytics/dependencies` over the **full current dataset**, not the selected window. `ServiceGraphBuilder` groups by `requestId`, sorts each group by `(timestamp, id)` and links consecutive distinct services; edge `weight` is the observed pair count. This is co-occurrence inside logs, not verified infrastructure, and the panel says so in its card subtitle, its screen-reader description and its accessible edge list.
- **Deterministic particles.** Particle count is `min(8, max(1, ceil(normalized * 7)))`; positions, stroke width, opacity, curvature and animation duration are pure functions of the returned weight, and `animateMotion` start offsets are staggered by a fixed per-particle fraction. Nothing is random. `prefers-reduced-motion` removes `animateMotion` and marks particles static.
- **Display modes.** `2d` is the default 760×440 SVG view. `3d` lazy-loads a Three.js/WebGL scene over the same API-derived nodes and edges, with OrbitControls, normalized illustrative traffic markers, hover details, focus/fit camera actions, reduced-motion handling and a clean SVG fallback. The canvas is supplementary; a semantic service list and observed-edge list remain present. It does not imply a live external request stream.
- **Accessibility.** Nodes are `role="button"` with `tabIndex=0`, respond to `Enter`/`Space` and carry an `aria-label` of name, event count and health band. An accessible service list and an accessible edge list mirror the SVG; "Focus selected" narrows the `viewBox`, "Reset view" restores it.
- **Heuristic health bands.** `healthy` < 5% error rate, `watch` 5–<10%, `elevated` ≥ 10%, `unknown` when the rate is not finite. Fixed thresholds over a top-N rollup, not a health model or SLI evaluation.
- **Pipeline story.** A Load → Observe → Detect → Investigate strip links each displayed number to `/ingestion`, `/analytics`, `/incidents` and `/logs`. It is navigation over already-returned data, not a process model.

## Frontend test inventory

`npm test` runs Vitest over 55 tests in 17 files:

| File | Tests | Covers |
|---|---:|---|
| `src/api/client.test.ts` | 3 | SSE frame parsing (chunking, comments, ids, multiline data, unterminated final frame) and multipart boundary handling |
| `src/components/format.test.ts` | 8 | Millisecond vs nanosecond duration formatting (including fractional values) and `eventKey` stability: dataset ids, deterministic generated ids, provenance namespacing, key stability and the malformed-payload fallback |
| `src/components/Layout.test.tsx` | 4 | Skip navigation, grouped navigation entries and active navigation |
| `src/components/TopologyPanel.test.tsx` | 7 | Accessible node/edge lists, data-driven radius and particle counts, controlled depth mode, reset view, reduced-motion static particles |
| `src/pages/AlgorithmsPage.test.tsx` | 3 | Algorithm Lab runs a catalogue `defaultInput` through the runs API, shows the run input, and surfaces a rejected run without navigating |
| `src/pages/AnalyticsPage.test.tsx` | 1 | Analytics tablist association: `aria-selected`, `aria-controls` and tabpanel labelling follow the selected tab |
| `src/pages/IncidentWorkbench.test.tsx` | 4 | Related-event rendering, evidence sections, and stable keys for generated event rows |
| `src/pages/LivePage.test.tsx` | 2 | Demo replay disclosure, shared `Replay state:` label, and start/replay-again honouring the selected batch size and pace |
| `src/pages/LogsPage.test.tsx` | 1 | Log row rendering and dataset-backed empty/loading states |
| `src/pages/MonitorPage.test.tsx` | 4 | Simulation event stream rendering, tick counter, and stable keys for generated event rows |
| `src/pages/OverviewPage.test.tsx` | 3 | Command Center selected-window metrics, coverage, investigation state and the explicit no-dataset state |
| `src/pages/PatternsPage.test.tsx` | 1 | Pattern example links search the returned example message rather than the wildcard template |
| `src/pages/ScenarioLabPage.test.tsx` | 4 | Scenario catalogue, selection, start/pause/resume, and stable keys for generated event rows |
| `src/pages/ServicesPage.test.tsx` | 2 | Service table ordering and health-band presentation |
| `src/replay/ReplayContext.test.tsx` | 2 | Single SSE subscription, progress, state transitions and stop/restart behavior |
| `src/telemetry/adapters.test.ts` | 4 | Dataset vs generated-event normalization, including identity fields used for React keys |
| `src/telemetry/SimulationBand.test.tsx` | 2 | Simulation control band labelling and connection-status exposure |

This is focused component coverage. It is not browser QA.

## Dataset audit

- `DemoDatasetGenerator` produces 14,000 events from seed `20260913L`. Content is deterministic; the time anchor is the current hour. The generated source label is `demo-stream` and the dataset is explicitly demo data.
- The committed `sample-data/` files provide canonical text, JSONL, malformed and multi-service examples. The catalog and parser tests are the source of truth for their line counts and parse outcomes.
- `LogParserFactory` selects JSONL when the first non-blank line starts with `{`; otherwise it selects canonical text. Text parsing expects 11 fields separated by ` | `. JSONL requires `timestamp` and `message`; unknown JSON fields are retained as attributes.
- Parsing is line-oriented. Malformed lines are recorded as failures and do not abort the rest of the import. A successful load reports `size`, `totalLines`, `failedLines` and `loadedAt`.
- `DELETE /api/datasets` clears the in-memory state. A restart clears it as well.
- The Demo Replay service reads the currently loaded event list and emits it oldest-first in bounded batches. Every payload identifies `demo-replay`; it does not connect to an external source.

## API and error audit

The active endpoint list and payload details are in [API.md](API.md). `GlobalExceptionHandler` emits this ordinary error envelope:

```json
{
  "status": 400,
  "error": "InvalidQueryException",
  "message": "Pattern must not be blank",
  "timestamp": "2026-09-25T00:00:00Z",
  "path": "/api/search/kmp"
}
```

The handler returns sanitized messages and does not expose stack traces. The current mappings are:

- `400`: invalid query/log input, illegal arguments, malformed or missing request data, invalid dates, bounds failures and upload-size rejection.
- `404`: missing dataset state, unknown event/service/incident/run, unknown sample and unknown route/resource.
- `405`, `406` and `415`: unsupported HTTP method or media type.
- `500`: algorithm execution failure or an unexpected exception, with a safe message.

There is one dedicated `409` mapping: `IllegalLifecycleTransitionException` returns `409 Conflict`, so an operator lifecycle action the current state does not allow is visibly rejected rather than silently accepted. Dataset presence endpoints have intentional special 404 bodies: `/api/health/dataset` and `/api/datasets/current` return `{ "loaded": false }`, while loading an unknown sample returns `{ "loaded": false, "error": "..." }`. `/api/live/status` remains `200` with `enabled: false` when no dataset is loaded.

Explicit limits are documented in [API.md](API.md). Important ones include the 64 MB upload ceiling, product-search `size=1..200`, log-list `limit=1..1000`, suggestion cap 50, incident detection cap 200, evidence cap 1,000, replay batch `1..200` with `intervalMs=100..60000`, run history cap 64 and recorder cap 400 steps. Algorithm validators add separate text, DP-cell, pattern-set, reservoir, benchmark and parallelism ceilings.

## Algorithm audit

`AlgorithmCatalog` contains 42 entries in six modules:

| Module | Entries | Reachable entries | Traceable |
|---|---:|---:|---:|
| Strings | 9 | 8 | 4 |
| Dynamic Programming | 12 | 12 | 2 |
| Graph and Flow | 6 | 6 | 3 |
| Approximation | 7 | 3 | 1 |
| Randomized | 5 | 4 | 3 |
| Parallel | 3 | 3 | 0 |
| **Total** | **42** | **36** | **13** |

`QueryDispatcher` registers 35 dispatch keys for the 36 reachable entries. `suffix_array` and `suffix_search` are two catalogue entries served by one engine (build and query phases). The 6 entries not counted as reachable at all — `kasai_lcp`, `bounded_vertex_cover`, `vertex_cover_kernelization`, `knapsack_fptas`, `vc_is_reduction`, `perfect_hash` — are library-only, with `exposed == false` enforced by `AlgorithmCatalogTest`.

Product use is narrower than catalogue exposure. KMP is the default product search matcher; Levenshtein is used for zero-hit suggestions; pattern and dataset-incident screens use deterministic heuristics; analytics and benchmarks compute from the active dataset. The Command Center topology folds the dataset by `requestId` in `ServiceGraphBuilder`, which is a deterministic adjacency pass rather than a catalogue algorithm driving an independent panel.

The generated-simulation path is a second genuine product use, and it is where four more catalogue algorithms do real work in `SimulationDetector`: **Aho-Corasick** scans the rolling window once for every signature the scenario can emit, **KMP** re-counts the dominant signature to confirm the multi-pattern result, the fixed **sliding-window** aggregate in `RollingWindow` produces the per-tick and rolling baselines the thresholds compare against, and **BFS** over `SimulationTopology` computes the blast radius across the declared dependency graph. Those four, plus product search and the suggestion path, are the algorithms with a product caller. Every other catalogue entry is an honest algorithm-engine, catalogue, trace or run-session capability.

The product does claim Three.js/WebGL rendering: the optional 3D topology is a lazy-loaded WebGL scene over the same backend nodes and edges as the 2D SVG map, with an accessible fallback. It does not claim WebSocket transport, research-service integration, trained ML or external live ingestion.

### Algorithm Lab surfaces

- **Algorithms** (`/algorithms`, aliased `/analysis/algorithms`) calls `api.algorithmGroups` → `GET /api/analysis/algorithms` and renders only that response: module grouping, per-module and total counts, a client-side text filter over returned name/key/problem/description fields, a per-row query type and complexity readout, `traceable` and `exposed` badges derived from the returned flags, and a detail card with the problem, algorithm type, complexities and the canonical/trace endpoint paths. It links to `/benchmarks`.
- **Benchmarks** (`/benchmarks`) issues one `GET /api/analysis/benchmarks/search?pattern=` request per submitted pattern and renders the returned per-matcher measurements, winner, haystack size and methodology note. No timing is computed in the browser.
- **Run Sessions** (`/runs`, `/runs/:id`) lists `GET /api/runs`, opens one record and streams its recorded ledger through `GET /api/runs/{id}/events` into `TracePlayer`. Run creation is a backend capability of `POST /api/runs`; the pages render recorded server events and never synthesize a computation.

### Academic `java.util` scope guard

`EngineScopeGuardTest` enforces the course rule against `java.util` delegation inside `dsa/**`. It holds a frozen manifest of the exact `java.util` import suffixes per `dsa` source file, walks every `.java` file under `backend/src/main/java/com/loginsight/dsa` (asserting a minimum scanned-file count so a wrong source root cannot pass silently), and fails the build on any import outside the manifest. Deleting recorded usage is always allowed, so the ledger can only shrink. `java.util.concurrent` under `dsa/parallel` and `java.util.Random` under `dsa/randomized` are course-licensed and are listed. Packages outside `dsa` are unrestricted.

## Accessibility and responsive audit

The current source includes a skip link, semantic `header`/`nav`/`main` landmarks, labelled navigation and icon buttons, live status regions, `aria-current`, focus-visible styling, keyboard navigation for the command palette and trace player, dialog Escape handling, reduced-motion CSS, table captions/labels and textual chart labels. The layout has responsive rules at 1120 px, 860 px and 680 px, with a mobile navigation drawer and single-column small-screen grids.

`TopologyPanel` adds `role="button"` nodes with `tabIndex=0`, `Enter`/`Space` activation, per-node `aria-label`s including the health band, `aria-pressed` selection state, and paired accessible service and edge lists that mirror the SVG. It honours `prefers-reduced-motion` by removing edge animation. It does not implement arrow-key roving focus between nodes; `Tab` order follows document order.

This is a source-level audit supported by the focused Layout and TopologyPanel component tests. No automated axe run, screen-reader test or fresh browser QA is configured in this checkout, so those broader claims are not made.

## Current limitations

- One process, one in-memory dataset, no persistence or durable upload storage.
- No authentication, authorization, multi-tenancy or production secret management.
- No external collector, true live ingestion, WebSocket transport or telemetry retention. Dataset Replay is a finite, oldest-first replay of the loaded dataset and is labelled `not real-time` wherever it appears; Live Monitor is server-generated simulation, not captured telemetry.
- Service topology edges are observed `requestId` adjacency over the loaded logs, not verified infrastructure. Edge absence is not proof of an absent call, and the graph is not scoped to the Command Center's selected window.
- Service health bands are fixed error-rate thresholds, not a health model, SLI evaluation or learned score. `eventsPerMinute` is a normalized window rate, not a measured inter-arrival rate.
- `OverviewDto.systemStatus` is a hardcoded compatibility string. Consumers needing runtime status must use `/api/health/status`.
- Run history and trace steps are bounded and disappear on restart.
- The `java.util` scope guard freezes the `dsa` usage that already existed instead of removing it, so recorded `ArrayList`/`List`/`Map` imports remain in the manifest until someone deletes them. New usage fails the build; existing usage is a visible, shrinkable ledger.
- Some legacy laboratory controllers have different request shapes and validator paths; clients should use the endpoint-specific contract rather than assuming one universal body. Product analytics, pattern, service and replay controls are explicitly bounded.
- Benchmarks are measured on the current host and input; they are not universal performance claims.
- The optional topology WebGL renderer requires browser/device WebGL support. The default SVG topology remains usable when it is unavailable. Particle movement visualizes normalized observed adjacency weight and is not an exact or live request count.
- Docker deployment has not been runtime-smoke-tested in the audit environment because no Docker daemon was available. `docker compose config` validated the Compose model only.
