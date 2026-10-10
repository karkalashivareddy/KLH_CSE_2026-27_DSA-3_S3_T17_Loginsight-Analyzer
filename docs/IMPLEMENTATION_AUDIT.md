# LogInsight — Current Implementation Audit

Audit snapshot: the **Atmospheric Signal** revision — a rewritten design system, a new motion layer, route-level code splitting, an application error boundary, four backend correctness fixes and a refreshed evidence set. The exact commit carrying this snapshot is the repository HEAD that contains this file.

This is the canonical description of the checked-out implementation. It is based on the current source, tests, frontend routes, build files and API controllers. Historical rebuild reports remain in [`docs/archive/`](archive/); they are not the current contract.

**Terminology.** These documents use **Dataset Replay** for the bounded replay of the loaded dataset served by `GET /api/live`. Its frontend route is `/replay`, its sidebar entry and page heading read `Dataset Replay`, the backend reports `source: "demo-replay"` and the UI shows a "not real-time" disclosure. The `/live` route is a different surface: **Live Monitor**, the deterministic server-generated simulation served by `GET /api/simulation/stream` and reporting `source: "live-simulation"`. The two are never interchangeable.

## What this revision changed

**Frontend**

- `styles/signal-atlas.css` was rewritten as the authoritative design system — warm mineral surfaces, deep-ink text, atmospheric teal accent, editorial serif display face, mono for data. It layers `global.css` and `product.css` underneath in the named cascade layer `loginsight-structure` and is itself unlayered, so it wins the cascade without editing the inherited structural sheets. It is 1,312 lines and emits a 191.81 kB stylesheet (35.61 kB gzip).
- `src/motion/motion.ts` is a new token module exporting `MOTION`, `EASE`, `spring`, `fadeTransition`, `routeVariants`, `stagger`, `itemVariants`, `overlayVariants` and `surfaceVariants`. The JavaScript values mirror the CSS custom properties of the same name, so a timing change is made once per medium.
- `src/motion/Atmosphere.tsx` is a new decorative backdrop: a light veil and a six-line isobar SVG driven by `useScroll`/`useTransform` at two different rates. It writes only to compositor motion values, registers no scroll listener, re-renders no React component while scrolling, is `aria-hidden`, and returns `null` under `prefers-reduced-motion: reduce`.
- `src/components/AppErrorBoundary.tsx` is a new class error boundary wrapping `<App />` in `main.tsx` with `scope="LogInsight"`. It replaces a render-time crash with a `role="alert"` recovery panel offering "Try again" and "Reload workspace", and never renders a stack trace.
- `src/App.tsx` now `lazy`-loads 17 of the 20 route components. Only the Command Center (`OverviewPage`), Logs Explorer (`LogsPage`) and Incident Workbench (`IncidentWorkbenchPage`) stay in the entry chunk. The `Suspense` fallback keeps the page title visible instead of replacing the route with a spinner.
- `vite.config.ts` sets `chunkSizeWarningLimit: 600` and a `manualChunks` splitter that pins `three` to `vendor-three`, `motion`/`framer-motion` to `vendor-motion`, `react-dom`/`react-router`/`scheduler` to `vendor-react`, and `lucide-react` to `vendor-icons`.
- `motion@^14.1.0` is a new runtime dependency, imported from `motion/react`. `lucide-react` moved to `^1.48.0`.
- `Layout.tsx` scroll reveal was reworked: the hidden state now arrives from JavaScript (`.reveal-section` is added by the observer, `.is-revealed` on entry) rather than from CSS, so a failed `IntersectionObserver` leaves content visible. The old `.atlas-scroll-revealed` class no longer exists. Route content is additionally wrapped in a `motion.div.route-view` keyed on `location.pathname`.
- `scripts/capture-visuals.mjs` now captures 19 screenshots into `docs/images/signal-atlas/`.

**Dead file.** `src/styles/signal-in-motion.css` is imported by nothing — verified by a repository-wide grep for the string `signal-in-motion`, which returns no match outside the file itself. It is the only place the retired cobalt accent `#315cf5` appears. It is dead weight and a candidate for deletion, but it is source code and therefore outside documentation ownership.

**Backend — four production fixes**

| File | Defect | Correction |
|---|---|---|
| `dsa/randomized/MillerRabin.java` | `testTracked(n, PROBABILISTIC, rng, 0)` skipped the only guard `test()` had, so the witness array stayed empty, the loop never ran, and composites were reported `PRIME`. A null `mode` NPE'd. | Extracted `validateArguments` and called it from both entry points. Null mode, `PROBABILISTIC` with a null `rng`, and non-positive rounds are all rejected up front. |
| `service/DatasetService.java` | `ingest(null, stream)` threw NPE; the empty-stream check `!name.isEmpty() && raw.length == 0` was de-inverted, so an empty upload with a named dataset passed the guard and failed later with the wrong exception. | Name is normalized first (trimmed; blank/null → `"imported-logs"`), a null stream is rejected with `IllegalArgumentException`, and any zero-length body raises `UnsupportedLogFormatException` regardless of the name. |
| `exception/GlobalExceptionHandler.java` | A parse failure was not mapped, so an empty or unparsable upload fell through to the generic `Exception` handler and answered HTTP 500. | `ParserException` is now in the 400 handler, which also covers `UnsupportedLogFormatException`. |
| `service/RunService.java` | The `/api/runs/{id}/events` emitter registered no `onCompletion`/`onTimeout`/`onError` callbacks, unlike the other two SSE services, so an abandoned replay kept running to the 60 s emitter timeout; `shutdown()` returned without waiting. | The replay is a `ReplayTask` that registers all three callbacks and is guarded by an `AtomicBoolean`; `shutdown()` adds a bounded 5 s `awaitTermination` and restores the interrupt flag. |

Five new test files cover these: `MillerRabinValidationRegressionTest`, `DatasetServiceIngestRegressionTest`, `ParserExceptionMappingTest`, `RunServiceSseLifecycleTest` and `DatasetUploadErrorContractTest`.

## Verification snapshot

Measured locally on this revision.

| Gate | Command | Result |
|---|---|---|
| Backend | `cd backend; .\mvnw.cmd -o verify` | **910 tests, 0 failures, 0 errors, 0 skipped** across 108 surefire classes; BUILD SUCCESS |
| Frontend tests | `cd frontend; npx vitest run` | **64 tests passed across 19 test files**, 0 failures, 34.09 s |
| Frontend build | `cd frontend; npm run build` | `tsc` and Vite production build passed; 2357 modules transformed |
| Browser E2E | `cd frontend; npm run test:e2e` | **15 Playwright workflows** defined in `frontend/e2e/product.spec.ts` (was 9) |
| Visual capture | `cd frontend; npm run capture:visuals` | 19 screenshots into `docs/images/signal-atlas/` |
| Compose syntax | `docker compose config --quiet` | Passed (earlier pass; not re-run this revision) |
| Container runtime | `docker compose up --build` | Not run: Docker daemon unavailable in this environment |

**Not re-verified this revision, and not claimed:** Playwright was not re-executed on this revision, `npm audit` was not re-run, `docker compose config` was not re-run, and no GitHub Actions run exists for these changes. The CI run recorded below covers the *previous* commit only.

GitHub Actions run [38052830711](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38052830711), for the previous implementation commit `c6349c77f3f755c231e1172efec14b01009bf1ef`, passed its backend, frontend and browser jobs. That run reports 878 backend tests and the earlier frontend/E2E counts; it is evidence for that commit and not for this one. The Actions result for the current HEAD is reported in the push output.

The Maven build targets Java 21 and uses Spring Boot 3.5.16. The frontend is React 18.3, TypeScript 5.6, Vite 6.4, `react-router-dom` 7.18, `motion` 14.1, `lucide-react` 1.48, Vitest 5 and Playwright; the Vitest suite requires Node 22.12 or newer. There is no separate lint script and no axe audit.

## Bundle measurements

Measured on this machine with `npm run build`, before and after the code-splitting and vendor-chunking work.

| Chunk | Before (raw / gzip) | After (raw / gzip) |
|---|---|---|
| `index` (app entry) | 489.32 kB / 136.95 kB | 143.46 kB / 41.73 kB |
| `vendor-react` | — | 181.40 kB / 59.80 kB |
| `vendor-motion` | — | 136.14 kB / 45.41 kB |
| `vendor-icons` | — | 32.28 kB / 7.08 kB |
| `Topology3D` | 571.82 kB / 144.14 kB | 9.88 kB / 3.90 kB (wrapper only) |
| `vendor-three` | — | 562.23 kB / 140.66 kB (**deferred**) |
| **First-load JS total** | **~1061 kB / ~281 kB gzip** | **~493 kB / ~154 kB gzip** |
| `index.css` | — | 191.81 kB / 35.61 kB |

First-load JavaScript falls by roughly 54% raw and 45% gzip. The Three.js payload is unchanged in size; it is simply no longer on the critical path, and is fetched only when the 3D topology is opened.

**Correction to a prior claim.** Earlier revisions of this file and of `13-testing.md` stated that the build "warns" about a 571.82 kB chunk above a 500 kB threshold. That is no longer what happens: `chunkSizeWarningLimit` was raised to 600, and the current build emits **no** size warning, because `vendor-three` measures 562.23 kB. The threshold was deliberately raised to match the real per-chunk cost of the one intentionally deferred dependency, and the measured build is reported here without suppression tricks.

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

## Frontend architecture

### Module layering

```text
main.tsx
  └─ AppErrorBoundary (class boundary, scope="LogInsight")
       └─ App
            ├─ ReplayProvider          owns the one dataset-replay SSE subscription
            ├─ TelemetryProvider       owns the one simulation SSE subscription
            ├─ BrowserRouter
            │    └─ GuidedDemoProvider
            │         └─ Layout         shell: nav, header, palette, Atmosphere, route-view
            │              └─ Outlet
            │                   ├─ OverviewPage          entry chunk
            │                   ├─ LogsPage               entry chunk
            │                   ├─ IncidentWorkbenchPage  entry chunk
            │                   └─ 17 lazy() route components, each in Suspense
```

`Layout` renders `<Atmosphere />` as a sibling *before* `.app-shell`, and the stylesheet pins `.atmosphere` at `z-index: -2`, so the backdrop can never paint over content.

### Code splitting

`App.tsx` exports three pages directly and wraps the remaining seventeen in `lazy()` through a local `lazyPage` helper. The three that stay in the entry chunk are the primary operational surfaces: a cold navigation to the Command Center, Logs Explorer or Incident Workbench should not pay a network round trip. Everything else — including the WebGL topology, reachable only from the Services page — is split out.

`manualChunks` in `vite.config.ts` then moves the long-lived vendor code into named chunks that do not change when application code changes:

| Chunk | Contains |
|---|---|
| `vendor-react` | `react-dom`, `react-router`, `scheduler` |
| `vendor-motion` | `motion` / `framer-motion` |
| `vendor-icons` | `lucide-react` |
| `vendor-three` | `three` — **not loaded on first paint** |

`Topology3D` itself shrinks to a 9.88 kB wrapper; the 562.23 kB Three.js payload sits behind it in `vendor-three` and is fetched only when the 3D view is opened.

### Motion layer

| File | Exports | Purpose |
|---|---|---|
| `motion/motion.ts` | `MOTION`, `EASE`, `spring`, `fadeTransition`, `routeVariants`, `stagger`, `itemVariants`, `overlayVariants`, `surfaceVariants` | One timing vocabulary per medium. `MOTION` is `fast: 0.16`, `normal: 0.26`, `slow: 0.42`, `scene: 0.62`, mirroring the CSS `--motion-*` tokens. `EASE.out` is `[0.22, 0.78, 0.28, 1]`; `EASE.inOut` is `[0.6, 0.02, 0.3, 1]`. |
| `motion/Atmosphere.tsx` | default component | Scroll-linked decorative backdrop; returns `null` under reduced motion. |

`Layout` consumes `useReducedMotion`, `MOTION` and `EASE` for the route transition and the reveal observer. Everything else that moves is CSS keyframes; the JavaScript layer exists so JS-driven and CSS-driven motion share one set of numbers.

### Stylesheet layering

```css
@import './global.css'  layer(loginsight-structure);
@import './product.css' layer(loginsight-structure);
/* signal-atlas.css itself is unlayered */
```

Unlayered declarations outrank every named layer, so `signal-atlas.css` overrides the two inherited sheets without editing them, while the inherited sheets still supply the structural scaffolding every route depends on. This is the reason several legacy selectors in `global.css`/`product.css` remain visible in the source while being inactive.

## Shell audit

The current `frontend/src/App.tsx` routes are:

- `/`, `/command-center` and `/overview`: Command Center (`OverviewPage`).
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
- **Static edge encoding.** Weight controls visible 2D path width/opacity and a bounded set of markers; markers do not move and cannot imply incoming traffic. Node/edge input is validated and deduplicated, render sizes are capped, and omitted data is reported.
- **Display modes.** `2d` is the default interactive SVG view with pan/zoom/fit/reset, selection, bounded rendering, an inspector and accessible service/edge lists. `3d` lazy-loads Three.js over the same API-derived nodes/edges, caps edge instances, provides camera controls and cleans up renderer resources. Browser verification confirmed the unavailable-WebGL fallback and continued 2D access; GPU-backed rendering was not verified in this headless browser.
- **Accessibility.** Nodes are `role="button"` with `tabIndex=0`, respond to `Enter`/`Space` and carry an `aria-label` of name, event count and health band. An accessible service list and an accessible edge list mirror the SVG; "Focus selected" narrows the `viewBox`, "Reset view" restores it.
- **Heuristic health bands.** `healthy` < 5% error rate, `watch` 5–<10%, `elevated` ≥ 10%, `unknown` when the rate is not finite. Fixed thresholds over a top-N rollup, not a health model or SLI evaluation.
- **Hero, signal field, source strip and metrics strip.** Replaced the previous Load→Observe→Detect→Investigate strip. See [COMMAND_CENTER.md](COMMAND_CENTER.md); the signal field renders an em dash, never a zero, when no dataset is loaded.

## Frontend test inventory

`npm test` runs Vitest over **64 tests in 19 files**. Verified on this revision.

| File | Tests | Covers |
|---|---:|---|
| `src/api/client.test.ts` | 3 | SSE frame parsing (chunking, comments, ids, multiline data, unterminated final frame) and multipart boundary handling |
| `src/components/AppErrorBoundary.test.tsx` | 3 | **(new)** children render while nothing throws; a render crash becomes a `role="alert"` panel with "Try again" and "Reload workspace"; no stack trace and no component name reach the DOM |
| `src/components/format.test.ts` | 8 | Millisecond vs nanosecond duration formatting (including fractional values) and `eventKey` stability: dataset ids, deterministic generated ids, provenance namespacing, key stability and the malformed-payload fallback |
| `src/components/Layout.test.tsx` | 4 | Skip navigation, grouped navigation entries, command-palette focus restoration and no false "no dataset" claim while status is loading |
| `src/components/TopologyPanel.test.tsx` | 11 | Accessible node/edge lists, static weight markers, declared/observed semantics, graph input validation and caps, WebGL fallback, bounded zoom/reset and keyboard selection |
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
| `src/presentation/GuidedDemo.test.tsx` | 2 | Guided demo reads the active dataset, issues the structured-search request, shows evidence and exits with Escape; a failed operation reports and retries |
| `src/replay/ReplayContext.test.tsx` | 2 | Single SSE subscription, progress, state transitions and stop/restart behavior |
| `src/telemetry/adapters.test.ts` | 4 | Dataset vs generated-event normalization, including identity fields used for React keys |
| `src/telemetry/SimulationBand.test.tsx` | 2 | Simulation control band labelling and connection-status exposure |
| **Total** | **64** | **19 files** |

The aggregate invocation reported by earlier revisions — 50 tests in 17 files plus an isolated 11-test topology run, with a Vitest worker-start timeout on the topology file — **did not reproduce on this revision**. The single `npx vitest run` completed all 19 files and all 64 tests. The earlier workaround is no longer necessary.

Focused component coverage complements the Playwright suite in `frontend/e2e/product.spec.ts`. Playwright runs a real local backend and frontend; it does not mock the APIs.

## Browser E2E inventory

`frontend/e2e/product.spec.ts` holds **15 workflows** (was 9). The six added this revision:

| Test | What it asserts |
|---|---|
| `Atmospheric Signal semantic text colors meet WCAG AA against their intended surfaces` | Computes relative luminance for 20 token pairs — text, accent, status, severity and all six algorithm-module colors — and asserts every ratio ≥ 4.5 |
| `the signal field reports real backend values and never invents them` | The four signal stages carry values the API actually returned, and em dashes rather than zeros when the backend holds no dataset |
| `an empty dataset shows onboarding instead of fabricated metrics` | The explicit no-dataset state renders with no invented numbers |
| `a failing backend surfaces a retryable error instead of a blank workspace` | A 500 envelope on `/api/*` produces an alert region with a retry path |
| `the command palette navigates and restores focus` | Palette open, keyboard navigation, selection and focus restoration |
| `documented route aliases resolve to the same workspace` | Every documented alias lands on its canonical route |
| `a document route does not resolve` | The catch-all not-found route renders |

The viewport matrix is unchanged: 1440×900, 1280×800, 1024×768, 768×1024, 390×844 and 360×800, with `document.documentElement.scrollWidth` asserted to fit on both the Command Center and the Service Map.

## Visual evidence

`frontend/scripts/capture-visuals.mjs` captures **19** screenshots into `docs/images/signal-atlas/`, all taken against the running application with a real dataset loaded through the API. Ten are new this revision: `algorithmic-search-desktop`, `algorithms-desktop`, `backend-error-state`, `command-center-evidence`, `command-center-reduced-motion`, `dataset-replay-desktop`, `incident-workbench-desktop`, `live-monitor-desktop`, `mobile-navigation` and `no-dataset-state`.

`docs/images/signal-atlas/` holds **19** PNG files, all produced by the current capture script.

**Removed this revision.** The eleven pre-redesign PNGs that sat directly under `docs/images/` — `overview.png`, `topology-2d.png`, `topology-3d.png`, `live-monitor.png`, `tablet-overview.png`, `mobile-overview.png`, `dataset-analysis.png`, `incident-workbench.png`, `algorithm-evidence.png`, `run-summary.png`, `scenario-lab.png` — were removed after a repository-wide grep proved zero references. The generated `docs/images/capture-report.json`, which listed exactly those eleven captures, was removed with them. `docs/images/klh-logo.png` was **kept**: it is a brand asset rather than a screenshot, it is outside the removal set, and the checklist references the live asset at `frontend/public/klh-logo.png`.

## Dataset audit

- `DemoDatasetGenerator` produces 14,000 events from seed `20260913L`. Content is deterministic; the time anchor is the current hour. The generated source label is `demo-stream` and the dataset is explicitly demo data.
- The committed `sample-data/` files provide canonical text, JSONL, malformed and multi-service examples. The catalog and parser tests are the source of truth for their line counts and parse outcomes.
- `LogParserFactory` selects JSONL when the first non-blank line starts with `{`; otherwise it selects canonical text. Text parsing expects 11 fields separated by ` | `. JSONL requires `timestamp` and `message`; unknown JSON fields are retained as attributes.
- Parsing is line-oriented. Malformed lines are recorded as failures and do not abort the rest of the import. A successful load reports `size`, `totalLines`, `failedLines` and `loadedAt`.
- **Upload contract after this revision.** `POST /api/datasets` with a zero-byte or whitespace-only body answers `400` with `error: "UnsupportedLogFormatException"` and `message: "input stream is empty"`. The same endpoint with unparsable-but-non-empty content answers the standard envelope without a stack trace. A valid upload still succeeds. Previously the empty case answered `500`.
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

- `400`: invalid query/log input, illegal arguments, malformed or missing request data, invalid dates, bounds failures, upload-size rejection, **and — new this revision — `ParserException` and its `UnsupportedLogFormatException` subclass**.
- `404`: missing dataset state, unknown event/service/incident/run, unknown sample and unknown route/resource.
- `405`, `406` and `415`: unsupported HTTP method or media type.
- `409`: `IllegalLifecycleTransitionException` only, so an operator lifecycle action the current state does not allow is visibly rejected rather than silently accepted.
- `500`: algorithm execution failure or an unexpected exception, with a safe message.

Dataset presence endpoints have intentional special 404 bodies: `/api/health/dataset` and `/api/datasets/current` return `{ "loaded": false }`, while loading an unknown sample returns `{ "loaded": false, "error": "..." }`. `/api/live/status` remains `200` with `enabled: false` when no dataset is loaded.

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

All five figures are asserted in `AlgorithmCatalogTest` (42 total, 36 exposed, 13 tracked, 35 dispatch keys, 9 strings) and are therefore fail-on-drift rather than hand-maintained. `QueryDispatcher` registers 35 dispatch keys for the 36 reachable entries. `suffix_array` and `suffix_search` are two catalogue entries served by one engine registered under `SUFFIX_ANALYSIS|SUFFIX_ARRAY`. The 6 entries not counted as reachable at all — `kasai_lcp`, `bounded_vertex_cover`, `vertex_cover_kernelization`, `knapsack_fptas`, `vc_is_reduction`, `perfect_hash` — are library-only, with `exposed == false` enforced by the same test.

Product use is narrower than catalogue exposure. KMP is the default product search matcher; Levenshtein is used for zero-hit suggestions; pattern and dataset-incident screens use deterministic heuristics; analytics and benchmarks compute from the active dataset. The Command Center topology folds the dataset by `requestId` in `ServiceGraphBuilder`, which is a deterministic adjacency pass rather than a catalogue algorithm driving an independent panel.

The generated-simulation path is a second genuine product use, and it is where four more catalogue algorithms do real work in `SimulationDetector`: **Aho-Corasick** scans the rolling window once for every signature the scenario can emit, **KMP** re-counts the dominant signature to confirm the multi-pattern result, the fixed **sliding-window** aggregate in `RollingWindow` produces the per-tick and rolling baselines the thresholds compare against, and **BFS** over `SimulationTopology` computes the blast radius across the declared dependency graph. Those four, plus product search and the suggestion path, are the algorithms with a product caller. Every other catalogue entry is an honest algorithm-engine, catalogue, trace or run-session capability.

**Miller-Rabin after this revision.** `test` and `testTracked` now share one `validateArguments`, so both reject a null `mode`, a `PROBABILISTIC` run with a null `rng`, and non-positive rounds. The defect being fixed was a correctness one, not a defensive nicety: `testTracked(n, PROBABILISTIC, rng, 0)` previously produced an empty witness set, skipped the loop entirely, and reported composites as `PRIME` under a `probabilistic` label. The engine's canonical request path was already bounded by `QueryValidator.MAX_MILLER_RABIN_ROUNDS = 1000`; the library-level guard is what the endpoint could not enforce for every caller. See [ALGORITHMS.md](ALGORITHMS.md).

The product does claim Three.js/WebGL rendering: the optional 3D topology is a lazy-loaded WebGL scene over the same backend nodes and edges as the 2D SVG map, with an accessible fallback. It does not claim WebSocket transport, research-service integration, trained ML or external live ingestion.

### Algorithm Lab surfaces

- **Algorithms** (`/algorithms`, aliased `/analysis/algorithms`) calls `api.algorithmGroups` → `GET /api/analysis/algorithms` and renders only that response: module grouping, per-module and total counts, a client-side text filter over returned name/key/problem/description fields, a per-row query type and complexity readout, `traceable` and `exposed` badges derived from the returned flags, and a detail card with the problem, algorithm type, complexities and the canonical/trace endpoint paths. It links to `/benchmarks`.
- **Benchmarks** (`/benchmarks`) issues one `GET /api/analysis/benchmarks/search?pattern=` request per submitted pattern and renders the returned per-matcher measurements, winner, haystack size and methodology note. No timing is computed in the browser.
- **Run Sessions** (`/runs`, `/runs/:id`) lists `GET /api/runs`, opens one record and streams its recorded ledger through `GET /api/runs/{id}/events` into `TracePlayer`. Run creation is a backend capability of `POST /api/runs`; the pages render recorded server events and never synthesize a computation.

### Academic `java.util` scope guard

`EngineScopeGuardTest` enforces the course rule against `java.util` delegation inside `dsa/**`. It holds a frozen manifest of the exact `java.util` import suffixes per `dsa` source file, walks every `.java` file under `backend/src/main/java/com/loginsight/dsa` (asserting a minimum scanned-file count so a wrong source root cannot pass silently), and fails the build on any import outside the manifest. Deleting recorded usage is always allowed, so the ledger can only shrink. `java.util.concurrent` under `dsa/parallel` and `java.util.Random` under `dsa/randomized` are course-licensed and are listed. Packages outside `dsa` are unrestricted.

## Accessibility and responsive audit

The current source includes a skip link, semantic `header`/`nav`/`main` landmarks, labelled navigation and icon buttons, live status regions, `aria-current`, focus-visible styling, keyboard navigation for the command palette and trace player, dialog Escape handling, reduced-motion CSS, table captions/labels and textual chart labels.

**Responsive breakpoints in the authoritative stylesheet** (`signal-atlas.css`): 1240 px, 1100 px, 960 px, 680 px and 400 px. The layered `global.css`/`product.css` additionally carry 1120 px, 860 px, 960 px, 760 px, 680 px, 1150 px, 1180 px, 1000 px and 640 px rules for structural layout; the unlayered `signal-atlas.css` wins wherever the two overlap.

**Reduced motion.** `signal-atlas.css` sets `scroll-behavior: auto`, collapses every animation and transition to `0.01ms` with a single iteration, forces `opacity: 1` and `transform: none` on route content, the hero, the signal field, guided-demo and topology selectors, forces `.reveal-section` visible, and sets `.atmosphere { display: none }`. `Atmosphere.tsx` additionally returns `null`, so the backdrop is not merely hidden but never mounted. `Layout` reads `useReducedMotion()` and skips the `IntersectionObserver` entirely.

`TopologyPanel` adds `role="button"` nodes with `tabIndex=0`, `Enter`/`Space` activation, per-node `aria-label`s including the health band, `aria-pressed` selection state, and paired accessible service and edge lists that mirror the SVG. It honours `prefers-reduced-motion` by removing edge animation. It does not implement arrow-key roving focus between nodes; `Tab` order follows document order.

This source-level audit is supported by focused component tests, the Playwright browser suite and current screenshot captures. No automated axe run or full screen-reader test is configured, so those broader accessibility claims are not made.

## Current limitations and unresolved items

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
- The optional topology WebGL renderer requires browser/device WebGL support. The default SVG topology remains usable when it is unavailable. Static edge encoding represents observed adjacency weight and does not imply live traffic.
- **Docker deployment has not been runtime-smoke-tested**, because no Docker daemon was available in the development environment. `docker compose config` validated the Compose model only.
- **`frontend/src/styles/signal-in-motion.css` is dead** — imported by nothing, superseded by the rewritten `signal-atlas.css`, and the only remaining home of the retired `#315cf5` accent. It should be deleted by the source owner.
- `docs/images/signal-atlas/guided-presentation-mobile.png` was a leftover from an earlier capture script and has been removed.
- **The submitted `.pptx`, `.docx` and `.pdf` under `final-submission/` were not regenerated.** No document-generation toolchain is available in this environment. They therefore still show the pre-Atmospheric-Signal interface and pre-revision test counts. This is an unresolved submission defect, tracked in [../final-submission/FINAL_SUBMISSION_INDEX.md](../final-submission/FINAL_SUBMISSION_INDEX.md).
- **No CI run covers this revision.** Playwright, `npm audit` and `docker compose config` were not re-executed here.
