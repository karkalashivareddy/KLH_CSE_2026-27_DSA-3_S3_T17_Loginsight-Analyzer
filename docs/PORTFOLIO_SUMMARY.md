# LogInsight — Portfolio Summary

Current branch: `main`. These summaries use only the current implementation and the latest audit results.

## One line

> LogInsight Analyzer is a full-stack, in-memory log investigation workspace that combines a React/TypeScript shell with a Spring Boot REST/SSE API, real classical algorithms, honest heuristic analysis, recorded traces and a 42-entry algorithm catalogue.

## Resume version

> **LogInsight Analyzer — Atmospheric Signal Log Intelligence Workspace** · Java 21 / Spring Boot 3.5 with React 18.3, TypeScript 5.6 and Vite 6. KMP powers product search, Levenshtein powers zero-hit suggestions, and heuristic pattern extraction and evidence-backed incident grouping remain explicitly non-ML; a six-module catalogue exposes real algorithm engines and recorded runs, and a frozen `java.util` manifest keeps the `dsa` engines inside the course scope rule. The Command Center is a selected-window operations view over an observed request-trail topology derived from `requestId` co-occurrence (not verified infrastructure), with an accessible 2D SVG default and an optional Three.js/WebGL view in a chunk deferred off the first load, error-rate health bands, and a signal field carrying four real backend values that render em dashes rather than zeros when no dataset is loaded. The runtime is single-process and in-memory; Dataset Replay is a bounded, labelled dataset replay shared by one `ReplayProvider` subscription, not production telemetry.


## Technical version

> LogInsight parses canonical text and JSONL into one backend-owned in-memory dataset, indexes fields and time windows, serves product analytics over REST, and uses SSE for a bounded, oldest-first dataset replay and recorded run-step replay. The overview endpoint returns a selected window anchored on the newest event timestamp, reporting `windowStart`/`windowEnd`/`scope` alongside window-scoped counts and the unfiltered `datasetEvents` total; `eventsPerMinute` divides by the nominal range width, so it is a normalized window rate rather than a measured inter-arrival rate. Service topology edges are counted `requestId` adjacency, and the UI labels them as observed rather than verified. Health is read from the real `/api/health/status` endpoint; the overview DTO's `systemStatus` is a hardcoded compatibility string. Search responses expose the selected matcher and measured duration; patterns are deterministic token normalization; incidents are five-minute baseline heuristics with supporting events. The topology renderer has a lazy Three.js/WebGL path with a data-equivalent SVG fallback. Docker Compose provides multi-stage non-root images, healthchecks, an Nginx `/api` proxy and SPA fallback, while the Vite development proxy remains available on port 5173. There is no database, authentication, WebSocket transport, external research integration or trained ML model.

## Evidence table

| Claim | Current evidence |
|---|---|
| Product shell | `frontend/src/App.tsx`, `Layout.tsx`, typed API client and Vitest shell test |
| Command Center selected-window semantics | `OverviewService.snapshot`, `OverviewDto`, `pages/OverviewPage.tsx`, [COMMAND_CENTER.md](COMMAND_CENTER.md) |
| Real health vs compatibility `systemStatus` | `HealthController.status()`, `api/client.ts` `systemStatus` → `/health/status` |
| Observed request-trail topology, SVG default + optional Three.js/WebGL scene and fallback | `ServiceGraphBuilder`, `/api/analytics/dependencies`, `components/TopologyPanel.tsx`, `components/Topology3D.tsx`, `TopologyPanel.test.tsx` |
| Shared replay context | `replay/ReplayContext.tsx`, `ReplayContext.test.tsx` |
| Heuristic health bands | `pages/OverviewPage.tsx` `healthBand`, printed thresholds in the matrix subtitle |
| 42 catalogue entries / 6 modules / 36 reachable / 13 traceable | `AlgorithmCatalog`, `CatalogService`, `/api/modules`, Algorithm Lab reading `/api/analysis/algorithms` |
| 35 registered engines | `EngineRegistry` and `/api/health/status` |
| KMP product search | `LogSearchService`, search response methodology |
| Levenshtein suggestion | `LogSearchService` zero-hit path |
| Heuristic patterns/incidents | `PatternExtractor`, `IncidentDetector`, evidence endpoints/UI |
| Demo versus replay | `DemoDatasetGenerator`, `LiveStreamService`, `demo-replay` labels |
| Academic `java.util` scope guard | `EngineScopeGuardTest` frozen manifest over `dsa/**` |
| Deployment | `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf`, `docker-compose.yml` |
| Verification | `.\mvnw.cmd -o verify` (910 backend tests), `npx vitest run` (64 tests across 19 files), `npm run build` (first-load JS ~493 kB raw / ~154 kB gzip), `npm audit` (0 vulnerabilities, not re-run this pass), `docker compose config`, and Playwright (15 defined browser workflows, not re-run this pass). CI also provisions Chromium for browser workflows |
| Docker limitation | daemon still unreachable in the audit environment; image/runtime smoke not claimed |
| Submission artefacts | the `.pptx`, `.docx` and `.pdf` under `final-submission/` were **not regenerated** — no document-generation toolchain is available — so they still depict the previous interface |
