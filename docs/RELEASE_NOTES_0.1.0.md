# LogInsight Analyzer 0.1.0 — rebuild/loginsight-v4

## Status

This is the current single-node release shape for the academic/portfolio project. It is not a public or production deployment. The backend, frontend and Compose artifacts are present in the repository; the audit environment did not have a running Docker daemon, so image build/runtime smoke tests remain a local follow-up.

## Current capabilities

- Rebuilt React/TypeScript observability shell with Command Center, explorer, search, analytics, patterns, incidents, services, datasets, ingestion, Algorithm Lab, run replay, system and docs routes. Incident evidence is also reachable at `/investigate/:id`.
- Command Center selected-window view: `range` selects the window, `windowStart`/`windowEnd` anchor it on the newest event timestamp, `scope` is `selected-window`, and window-scoped counts are reported alongside the unfiltered `datasetEvents` total. `eventsPerMinute` divides by the nominal range width, so it is a normalized window rate rather than a measured inter-arrival rate.
- Observed request-trail topology from `requestId` co-occurrence, with a 2D SVG mode and a 2.5D "3D / depth" mode implemented as a CSS `perspective` + `rotateX` transform over the same SVG. The panel states "not WebGL" in both modes; no WebGL context, shader or 3D engine exists.
- Deterministic edge-weight encoding: stroke width, opacity, curvature and a bounded particle count per edge are pure functions of the observed weight, with `prefers-reduced-motion` support.
- Heuristic service health bands (healthy <5%, watch 5–<10%, elevated ≥10%, unavailable when the rate is not finite), with the thresholds printed in the UI.
- Pipeline story strip linking each Command Center figure to the page that produced it.
- One shared `ReplayProvider` subscription backing both the Command Center replay card and the Demo Replay screen (route `/live`, navigation label `Live Replay`).
- Runtime health read from the real `GET /api/health/status`; the overview DTO's `systemStatus` is a hardcoded compatibility field used only as a pre-resolution fallback.
- Spring Boot REST/SSE API over one in-memory current dataset.
- Deterministic 14,000-event demo generator, bundled text/JSONL samples and multipart upload parsing.
- 42-entry, six-module algorithm catalogue; 35 registered query engines; 13 trace-instrumented algorithms.
- An academic `java.util` scope guard (`EngineScopeGuardTest`) that freezes a per-file manifest over `dsa/**` and fails the build on any new `java.util` import.
- KMP product search, Levenshtein zero-hit suggestions, heuristic patterns, five-minute incident evidence, measured matcher comparison and recorded run sessions.
- Multi-stage, non-root Docker images, Nginx `/api/` proxy, SSE buffering settings, SPA fallback, healthchecks and Compose service ordering.
- Vite development proxy preserved at `/api` → `localhost:8080`.

## Verification

| Check | Result |
|---|---|
| `cd backend; .\mvnw.cmd -o verify` | 827 tests, 0 failures, 0 errors |
| `cd frontend; npm test` | 24 tests across 10 files passed |
| `cd frontend; npm run build` | Clean TypeScript/Vite production build |
| `docker compose config --quiet` | Passed |
| Docker image build/runtime | Not run: Docker daemon unavailable in the audit environment |

All three build commands are also the CI gates: the backend job runs `mvn -q verify`, and the frontend job runs `npm ci`, `npm test` and `npm run build`.

The Docker daemon limitation still holds on this branch; `docker info` cannot reach the engine, so no image build or container smoke test has been run.

The old browser-QA figures and the smaller backend test counts in earlier rebuild reports are historical and are not claims for this branch; see [13-testing.md](13-testing.md).

## Data honesty

Demo data is synthetic. Demo Replay is a bounded, oldest-first replay labelled `demo-replay` / “not real-time”; the route is `/live` and the navigation and page label is `Live Replay`, which is a navigation label, not a real-time capture claim. Topology edges are observed log co-occurrence over the full loaded dataset, not verified infrastructure. Health bands are error-rate thresholds, not a health model. There is no external collector, WebSocket transport, trained ML model, WebGL or 3D engine, or research integration.

## Known limitations

- One backend process and one in-memory dataset; no persistence, authentication, authorization or multi-tenancy.
- Run history is capped at 64; recorded traces are capped at 400 steps.
- Uploads are capped at 64 MB and algorithm requests have endpoint-specific bounds.
- The topology depth mode is a 2.5D CSS transform over a flat SVG, not a 3D renderer.
- Benchmarks are host- and input-dependent measurements.
- Compose is a practical evaluation deployment, not a durable or horizontally scalable platform. Image builds were never executed because no Docker daemon was available.

See [IMPLEMENTATION_AUDIT.md](IMPLEMENTATION_AUDIT.md), [COMMAND_CENTER.md](COMMAND_CENTER.md), [API.md](API.md) and [DEPLOYMENT.md](DEPLOYMENT.md) for the current contract.
