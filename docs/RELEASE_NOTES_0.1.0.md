# LogInsight 0.1.0 — Real-Time Log Intelligence & Incident Investigation Platform

## Status

This is the current single-node release shape for the academic/portfolio project. It is not a public or production deployment. The backend, frontend and Compose artifacts are present in the repository; the audit environment did not have a running Docker daemon, so image build/runtime smoke tests remain a local follow-up.

## Current capabilities

- Rebuilt React/TypeScript observability shell with Command Center, Logs, Algorithmic Search, analytics, patterns, Detector Windows, Incident Workbench, services, datasets, ingestion, Algorithm Lab, run replay, system and docs routes. Dataset incident evidence is also reachable at `/investigate/:id`.
- Deterministic generated simulation with a scenario catalogue, run controls and a Server-Sent Events frame stream: Scenario Lab (`/scenario-lab`), Live Monitor (`/live`), and the Incident Workbench (`/incidents/workbench`) with an operator lifecycle, timeline, blast radius and algorithm evidence.
- Scenario Lab and Live Monitor share one `TelemetryProvider` stream. The backend owns the session; the browser renders measured frames and never substitutes its own telemetry.
- Command Center selected-window view: `range` selects the window, `windowStart`/`windowEnd` anchor it on the newest event timestamp, `scope` is `selected-window`, and window-scoped counts are reported alongside the unfiltered `datasetEvents` total. `eventsPerMinute` divides by the nominal range width, so it is a normalized window rate rather than a measured inter-arrival rate.
- Observed request-trail topology from `requestId` co-occurrence, rendered as a 2D SVG map by default with an on-demand **Three.js/WebGL** scene. The WebGL view is lazy-loaded (`frontend/src/components/Topology3D.tsx` via `React.lazy` in `TopologyPanel.tsx`), so its code is split out of the main bundle. Both modes read the same backend nodes and edges, and an accessible service list plus the 2D view remain available as fallbacks when WebGL is unsupported.
- Deterministic edge-weight encoding: stroke width, opacity, curvature and a bounded particle count per edge are pure functions of the observed weight, with `prefers-reduced-motion` support.
- Heuristic service health bands (healthy <5%, watch 5–<10%, elevated ≥10%, unavailable when the rate is not finite), with the thresholds printed in the UI.
- Pipeline story strip linking each Command Center figure to the page that produced it.
- One shared `ReplayProvider` subscription backing both the Command Center replay card and the Dataset Replay screen (route `/replay`, navigation label `Dataset Replay`).
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
| `cd backend; .\mvnw.cmd -o verify` | 877 tests, 0 failures, 0 errors |
| `cd frontend; npm test` | 55 tests across 17 files passed |
| `cd frontend; npm run build` | Clean TypeScript/Vite production build |
| `docker compose config --quiet` | Passed |
| Docker image build/runtime | Not run: Docker daemon unavailable in the audit environment |

All three build commands are also the CI gates: the backend job runs `mvn -B verify`, and the frontend job runs `npm ci`, `npm test` and `npm run build`.

The Docker daemon limitation still holds on this branch; `docker info` cannot reach the engine, so no image build or container smoke test has been run.

The old browser-QA figures and the smaller backend test counts in earlier rebuild reports are historical and are not claims for this branch; see [13-testing.md](13-testing.md).

## Data honesty

Demo data is synthetic. **Dataset Replay** (`/replay`) is a bounded, oldest-first replay of the loaded dataset labelled `demo-replay` and “not real-time”; its own page copy still reads “Demo replay of the loaded dataset”. **Live Monitor** (`/live`) is the separate, server-generated deterministic simulation reported as `live-simulation`. The two are never interchangeable.

Topology edges are observed log co-occurrence over the full loaded dataset on dataset surfaces (`OBSERVED DEPENDENCIES`), and the scenario's declared graph on simulation surfaces (`DECLARED DEPENDENCIES`); neither is verified infrastructure. Health bands are error-rate thresholds, not a health model. There is no external collector, WebSocket transport, trained ML model or research integration.

## Known limitations

- One backend process and one in-memory dataset; no persistence, authentication, authorization or multi-tenancy.
- Run history is capped at 64; recorded traces are capped at 400 steps.
- Uploads are capped at 64 MB and algorithm requests have endpoint-specific bounds.
- The optional 3D topology depends on device and browser WebGL support; it is lazy-loaded, cleans up its renderer, controls, geometry, materials and animation loop on unmount, and falls back to the 2D view.
- Benchmarks are host- and input-dependent measurements.
- Compose is a practical evaluation deployment, not a durable or horizontally scalable platform. Image builds were never executed because no Docker daemon was available.

See [IMPLEMENTATION_AUDIT.md](IMPLEMENTATION_AUDIT.md), [COMMAND_CENTER.md](COMMAND_CENTER.md), [API.md](API.md) and [DEPLOYMENT.md](DEPLOYMENT.md) for the current contract.
