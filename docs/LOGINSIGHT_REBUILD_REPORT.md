# LogInsight Rebuild Report

> Historical phase report. The route, count, QA and verification figures below describe the earlier rebuild snapshot. For the current branch, use [IMPLEMENTATION_AUDIT.md](IMPLEMENTATION_AUDIT.md) and [13-testing.md](13-testing.md).

> The 732 backend count and the "no test runner configured in `frontend/`" note are historical. The current branch runs 827 backend tests and 24 frontend tests across 10 files, and `npm test` is a CI gate.

Date: 2026-09-24. This report records the conversion of the "TextHack — Advanced Algorithms
Laboratory" site into **LogInsight Analyzer**: a log-intelligence product whose internal search,
patterns, incidents and analytics are powered by the same DSA-3 engines, re-framed as the product's
backend rather than its face.

## Goal

Deliver a 73-section LogInsight Analyzer product spec on top of the existing DSA-3 engines,
**without** breaking a single existing test, and without fabricating any metric, benchmark, stream
or incident. Everything the UI shows must trace to the loaded dataset or to a measured run.

## What was kept

- The full DSA-3 engine layer (`com.loginsight.dsa`) untouched — 42 algorithms, trace
  instrumentation, cross-verification tests.
- Run sessions (`/api/runs`) and SSE replay, the Algorithm Catalogue (`/api/modules`,
  `/api/algorithms`), and the classic laboratory endpoints (`/api/search/kmp`, `/api/dp/…`,
  `/api/flow/…`, `/api/approx/…`, `/api/random/…`, `/api/parallel/…`, `/api/benchmark/run`).
- All pre-existing tests (incl. `SearchControllerTest`'s reliance on `/api/logs/first`,
  `/api/logs?limit&offset`, `/api/logs/stats`).
- The dark control-room palette and hand-rolled SVG chart components.

## What was added (backend)

- **Demo & datasets** — `DemoDatasetGenerator` (deterministic 14k-event corpus, 8 services,
  error bursts), `DatasetService.loadDemo()`, `ingest()` with auto-detecting `TextLogParser`/
  `JsonLogParser`, `DatasetSummaryDto`.
- **Index layer** — `LogIndex` (per-field sorted position lists, half-open `[from,to)` time-window
  lookups), `LogIndexService` (per-dataset-instance cache).
- **Product search** — `SearchQuery`/`SearchQueryParser` (structured query grammar),
  `LogSearchService` (index filters + DSA matcher over the rendered haystack, hit/snippet mapping,
  Levenshtein "did you mean", `suggest()` typeahead), `LogSearchRequest/Response`.
- **Patterns** — `PatternExtractor.normalizeMessage` heuristic token templates, `PatternDto`.
- **Incidents** — `IncidentDetector` (5-minute windows vs dataset-baseline threshold, merge logic),
  `IncidentsService`, `IncidentDto`/`IncidentDetail`.
- **Analytics** — `TimelineAnalyzer` (range presets), `SeverityAnalyzer`, `HeatmapAnalyzer`
  (7×24), `FleetAnalyzer` (service/http/host rollups + percentile), `OverviewService`/`OverviewDto`.
- **Services** — `ServicesController` fleet rollups + drill-down detail.
- **Live** — `LiveStreamService` (SSE `start`/`batch`/`replay-complete`, labelled `demo-replay`).
- **Ingestion** — `IngestionController` status/demo endpoints.
- **Analysis** — `InsightsController` (`/api/analysis/algorithms` grouped catalogue,
  `/api/analysis/benchmarks/search` measured benchmark via `SearchBenchmarkService`).
- **Controllers** — wired `Overview`, `Patterns`, `Incidents`, `Services`, `Live`, `Ingestion`,
  `Insights`, plus explorer/log-detail endpoints and multipart upload.
- **Tests** — 36 new tests across `datasets/`, `index/`, `pattern/`, `incident/`, `search/`,
  `analytics/`, `service/`.

## What was added (frontend)

- `api/types.ts` extended with every product DTO; `api/client.ts` gained one method per endpoint
  plus SSE parsers for live replay.
- New shell: `Layout` (LogInsight sidebar + dataset-aware header) and `App` routes.
- New/rewritten pages: Overview, Logs (explorer + detail), Search (typeahead + methodology +
  "did you mean"), Analytics (5 tabs), Patterns, Incidents, Services (+ detail), Live, Datasets,
  Ingestion, Analysis hub, Algorithms, Search Benchmarks; Run Sessions/System/Docs kept.
- CSS additions for search typeahead, heatmap grid, pattern/incident/service cards, live progress,
  benchmark winner emphasis, explorer table, detail grids.
- Removed TextHack-branded pages (`TextHackPage`, `LabPage`, `CourseMapPage`).

## Verification

| Check | Result |
| --- | --- |
| Backend: `.\mvnw.cmd -o test` | **732 tests, 0 failures, 0 errors** — historical count, see [13-testing.md](13-testing.md) |
| Backend: `.\mvnw.cmd -o verify` | BUILD SUCCESS, jar `loginsight-analyzer-0.1.0-SNAPSHOT.jar` |
| Frontend: `npm run build` (tsc + vite) | clean production build (no frontend test runner at that point) |

## Honesty rules enforced during the rebuild

- `java.util.*` remains forbidden in the `com.loginsight.dsa` package (scope-guard tested).
- Product endpoints 404 when no dataset is loaded so the UI cannot show invented numbers.
- Demo and the live stream are labelled (`demo-replay`, "not real-time"); patterns are labelled
  heuristic, not ML; incidents expose their method and supporting logs.
- Benchmark numbers are single measured runs, winner = fastest measured, with an explicit note.
- All 732 pre-existing + new tests stay green (types/format kept compatible with old pages).

## Open items / notes

- Frontend port 5173 proxies `/api` → 8080; no test runner configured in `frontend/` (backend
  test suite is the gate).
- `docs/API.md` supersedes the older `docs/12-api-documentation.md` for the product surface.
- `RunsPage` replays runs in-page (no `/analysis/trace/:runId` route) — documented in the
  browser QA section below.

## Final browser QA (Playwright headless Chromium)

| Check | Result |
| --- | --- |
| Browser available | YES (Playwright 1.63 / chromium-1243) |
| Routes checked | `/`, `/logs`, `/logs/:id`, `/search`, `/analytics` (all 5 tabs: timeline/severity/heatmap/http/hosts), `/patterns` (+ example), `/incidents` (+ evidence), `/services` + `/services/api-gateway`, `/live` (start/stop replay), `/datasets` (clear + load demo via UI), `/ingestion`, `/analysis`, `/analysis/algorithms`, `/analysis/benchmarks` (run), `/runs` (create via API, open, TracePlayer play), `/system`, `/docs`, 404 page |
| Pages | 59 viewport/page loads, **0 blank routes**, 0 page (JS) errors |
| Console errors | 0 after fixing favicon 404 (added `frontend/public/favicon.svg` + `<link rel="icon">`) |
| Network errors | 1 intentional SSE `ERR_ABORTED` when the Live Stop button cancels the `demo-replay` stream |
| Desktop sizes | 1440 / 1280 / 1024 / 768 / 480 / 375 on `/`, `/search`, `/logs`, `/analytics` — no horizontal overflow |
| Responsive issues | FIXED: `/runs` run-detail card overflowed the viewport by 1863px (40-step `TracePlayer` timeline forced a `minmax(auto,1fr)` grid track wide) → `.run-layout` now uses `320px minmax(0,1fr)` + `min-width:0` |
| Accessibility issues | Trace controls are `button` elements (keyboard focusable); timeline run-detail steps are buttons + breadcrumb links; no blocking contrast/a11y regressions observed this pass |
| Legacy UI references | 0 visible occurrences of TextHack / Advanced Algorithms Laboratory / Course Map / DSA Laboratory in the rendered product UI (docs/System/Runs/Layout/Overview checked) |
| Search & fuzzy | `connection refused` → "N matching events in Demo Dataset" + methodology badges (strategy/algorithm/pattern/measured); typo `paymentd proccessd` surfaced "Did you mean…" with Levenshtein similarity |
| Demo workflow | First run shows "No dataset loaded" with working Datasets/Ingestion links; "Load Demo Dataset" (UI button) loads 14,000 events and the Overview populates |
| Live | "▶ Start replay" streams `demo-replay` batches with "not real-time" disclosure; Stop clean |
| Known limitation | Trace replay uses in-page selection (`RunsPage` + `TracePlayer`); there is no `:runId` deep link — kept to avoid churn before release |