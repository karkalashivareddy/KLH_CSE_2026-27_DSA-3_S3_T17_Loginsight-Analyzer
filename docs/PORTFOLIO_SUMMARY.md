# LogInsight Analyzer — Portfolio Summary

Current branch: `rebuild/loginsight-v4`. These summaries use only the current implementation and the latest audit results.

## One line

> LogInsight Analyzer is a full-stack, in-memory log investigation workspace that combines a React/TypeScript shell with a Spring Boot REST/SSE API, real classical algorithms, honest heuristic analysis, recorded traces and a 42-entry algorithm catalogue.

## Resume version

> **LogInsight Analyzer — Algorithmic Log Intelligence Workspace** · Java 21 / Spring Boot 3.5 with React 18, TypeScript and Vite. KMP powers product search, Levenshtein powers zero-hit suggestions, and heuristic pattern extraction and evidence-backed incident grouping remain explicitly non-ML; a six-module catalogue exposes real algorithm engines and recorded runs, and a frozen `java.util` manifest keeps the `dsa` engines inside the course scope rule. The Command Center is a selected-window operations view: an observed request-trail topology derived from `requestId` co-occurrence over the full dataset (not verified infrastructure), a 2D SVG view with a CSS 2.5D depth mode that is not WebGL, error-rate health bands, and a pipeline story linking each number to the page that produced it. Current verification: 827 backend tests and 24 frontend tests across 10 files pass, with a clean production build, and both suites run in CI. The runtime is single-process and in-memory; Demo Replay is a bounded, labelled dataset replay shared by one `ReplayProvider` subscription, not production telemetry.

## Technical version

> LogInsight parses canonical text and JSONL into one backend-owned in-memory dataset, indexes fields and time windows, serves product analytics over REST, and uses SSE for a bounded, oldest-first dataset replay and recorded run-step replay. The overview endpoint returns a selected window anchored on the newest event timestamp, reporting `windowStart`/`windowEnd`/`scope` alongside window-scoped counts and the unfiltered `datasetEvents` total; `eventsPerMinute` divides by the nominal range width, so it is a normalized window rate rather than a measured inter-arrival rate. Service topology edges are counted `requestId` adjacency, and the UI labels them as observed rather than verified. Health is read from the real `/api/health/status` endpoint; the overview DTO's `systemStatus` is a hardcoded compatibility string. Search responses expose the selected matcher and measured duration; patterns are deterministic token normalization; incidents are five-minute baseline heuristics with supporting events. Docker Compose provides multi-stage non-root images, healthchecks, an Nginx `/api` proxy and SPA fallback, while the Vite development proxy remains available on port 5173. There is no database, authentication, WebSocket transport, WebGL or 3D engine, external research integration or trained ML model.

## Evidence table

| Claim | Current evidence |
|---|---|
| Product shell | `frontend/src/App.tsx`, `Layout.tsx`, typed API client and Vitest shell test |
| Command Center selected-window semantics | `OverviewService.snapshot`, `OverviewDto`, `pages/OverviewPage.tsx`, [COMMAND_CENTER.md](COMMAND_CENTER.md) |
| Real health vs compatibility `systemStatus` | `HealthController.status()`, `api/client.ts` `systemStatus` → `/health/status` |
| Observed request-trail topology, 2D + CSS 2.5D depth, deterministic particles, no WebGL | `ServiceGraphBuilder`, `/api/analytics/dependencies`, `components/TopologyPanel.tsx`, `TopologyPanel.test.tsx` |
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
| Verification | `.\mvnw.cmd -o verify` (827), `npm test` (24 across 10 files), `npm run build`, `docker compose config`; CI runs all three |
| Docker limitation | daemon still unreachable in the audit environment; image/runtime smoke not claimed |
