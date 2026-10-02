# LogInsight

**Real-Time Log Intelligence & Incident Investigation Platform**

LogInsight is a full-stack, in-memory log investigation workspace. It combines a React 18 + TypeScript + Vite shell with a Java 21 Spring Boot REST/SSE API, classical data-structure and algorithm engines, and a backend-owned dataset lifecycle.

The platform ingests a dataset or runs a deterministic generated scenario, streams it live, detects elevated-error windows with measured evidence, and presents an investigation surface where every number on screen can be traced back to the algorithm that produced it.

> Academic/portfolio software. The Docker artifacts are a single-node deployment shape, not a claim of production scale or public deployment.

## Screenshots

Captured from the running application at 1440x900 unless noted. The Overview and Scenario Lab shots
are taken during a live deterministic run, so the figures on screen are measured, not staged.

### 1. Overview — command center

![Overview during a live run: stream state, KPI strip, detected signals and algorithm evidence](docs/images/overview.png)

### 2. Scenario Lab — controlled failure lab

![Scenario catalogue, selected scenario, run controls and emerging telemetry](docs/images/scenario-lab.png)

### 3. Live Monitor — system state to measurement to detection

![Hero telemetry state, KPI row, chart, topology and event stream](docs/images/live-monitor.png)

### 4. Incident Workbench — investigation desk

![Three-zone workbench: incident navigator, measured detail with lifecycle and evidence, and blast radius with topology](docs/images/incident-workbench.png)

### 5. Service topology — 2D and 3D

The same backend nodes and edges, rendered two ways. 2D is the accessible SVG map; 3D is a lazy-loaded WebGL scene. Both encode node size from measured event volume and node state from measured health.

| 2D — accessible SVG map | 3D — lazy-loaded WebGL scene |
|---|---|
| ![2D service topology](docs/images/topology-2d.png) | ![3D service topology](docs/images/topology-3d.png) |

### 6. Algorithm evidence

![Algorithm catalogue with complexity, exposure and runnable evidence](docs/images/algorithm-evidence.png)

### 7. Dataset analysis

![Analytics over the loaded dataset](docs/images/dataset-analysis.png)

### Responsive layouts

The dashboard is restructured rather than scaled down. Topology, tables and KPI strips collapse to a single column, and hit areas grow on touch viewports.

| Tablet — 1024x768 | Mobile — 390x844 |
|---|---|
| ![Overview at 1024x768](docs/images/tablet-overview.png) | ![Overview at 390x844](docs/images/mobile-overview.png) |

## The problem

During an outage, the log lines needed to explain what happened are spread across services, arrive interleaved and out of order, and are queried far faster than a human can read them. LogInsight applies classical algorithms to that work — multi-pattern and exact-substring search, bounded edit distance, streaming window aggregation, and dependency-graph traversal — and presents the result as an investigation surface where every number on screen can be traced back to the algorithm that produced it.

The intended workflow is:

**Ingest → Observe → Detect → Investigate → Explain → Act → Verify**

Ingest a dataset or start a generated scenario, observe service health and live metrics, let the detector raise a window, investigate the evidence behind it, explain it with the algorithm evidence and dependency graph, act through an explicit operator lifecycle, then verify that measured metrics returned to a healthy level. The application does not claim to identify a root cause on its own; it reports measured evidence and leaves attribution to the operator.

## Technology stack

| Layer | Technology |
|---|---|
| Frontend | React 18, TypeScript, Vite, React Router, hand-rolled SVG charts, Three.js (lazy-loaded 3D only), Vitest + Testing Library |
| Backend | Java 21, Spring Boot (REST + Server-Sent Events), in-memory dataset store, JUnit 5 |
| Data | Backend-held in-memory dataset, JSONL and pipe-delimited uploads, bundled `sample-data/` samples |
| Transport | HTTP REST plus SSE. No WebSocket, no external log collector, no cloud integration |


## Current implementation

The current shell is a dark observability workspace with:

- Command Center, Log Explorer, Search, Analytics, Patterns, Detector Windows, Incident Workbench, Services, Datasets and Ingestion views.
- A **Scenario Lab** that configures and runs the deterministic microservice simulation, a **Live Monitor** that streams it, and a separate **Dataset Replay** for the loaded dataset.
- An Algorithm Lab with the backend catalogue, measured search benchmark and recorded run sessions.
- System and Documentation views.
- A responsive navigation shell with a command palette, breadcrumbs, runtime status and dataset status.
- Hand-rolled SVG charts and an interactive service topology with a 2D SVG default plus an on-demand Three.js/WebGL 3D view, an accessible service-list and 2D fallback. Topology provenance is labelled: dataset pages show `OBSERVED DEPENDENCIES`, simulation pages show `DECLARED DEPENDENCIES`. There is no WebSocket client.

The canonical route and implementation audit is [docs/IMPLEMENTATION_AUDIT.md](docs/IMPLEMENTATION_AUDIT.md). The API contract is [docs/API.md](docs/API.md). See [docs/COMMAND_CENTER.md](docs/COMMAND_CENTER.md) for the Command Center semantics.

## Command Center

`/` and `/command-center` are a selected-window operations view built only from returned dataset and runtime data.

- **Selected window**: `range=5m|15m|1h|6h|24h` (default `1h`). `windowEnd` is the newest event timestamp in the loaded dataset and `windowStart` is `windowEnd - range`, so the window is a trailing slice of the data, not of the wall clock. `scope` is the literal `selected-window`.
- **Window-scoped counts**: `events`, `errors`, `warnings`, `services`, `hosts`, `severity`, `statusCodes`, `topServices`, `topPatterns`, `recentCritical`, the timeline and the heatmap are all computed over that window only.
- **`datasetEvents`** is the unfiltered size of the loaded dataset. The UI uses it to show selected-window coverage as a percentage of the whole dataset.
- **`eventsPerMinute`** is `window events / (range width in minutes)`. It is a rate over the nominal range width, not a measured inter-arrival rate over the observed span, so it reads low when events cluster near `windowEnd`.
- **Real health vs compatibility status**: `OverviewDto.systemStatus` is a compatibility field the backend always sets to `"Operational"`. Runtime health is `GET /api/health/status`, which reports `status`, `uptimeMillis`, `datasetLoaded`, `datasetName`, `datasetSize` and the registered engine count. The Command Center header prefers the real health value and falls back to the compatibility string only when the health request has not resolved.
- **Observed request-trail topology**: nodes and edges come from `GET /api/analytics/dependencies`, which groups events by `requestId` and links consecutive distinct services. Node size follows observed event counts and edge width, opacity and particle count follow the observed adjacency weight. This is co-occurrence inside log data, not verified infrastructure topology.
- **Deterministic edge particles**: each edge renders `min(8, max(1, ceil(normalizedWeight * 7)))` particles, positions them deterministically along the edge path, and staggers `animateMotion` by a fixed per-particle offset derived from the measured weight. `prefers-reduced-motion` replaces the animation with static particles.
- **Pipeline story**: a Load → Observe → Detect → Investigate strip links each Command Center number to the page that produced it.
- **Shared replay context**: Command Center and Demo Replay read one `ReplayProvider` subscription, so a replay started on either screen is visible on both.

The topology panel renders a 2D SVG by default. Its optional Three.js/WebGL view is lazy-loaded, uses bounded instanced traffic markers and OrbitControls, and cleans up its renderer, controls, geometry, listeners and animation loop on unmount. If WebGL is unavailable, it reports that and the user can switch back to the same 2D data view. Moving markers communicate normalized observed request-trail intensity, not exact request counts or a live production stream. See [docs/design-system.md](docs/design-system.md) for visual encodings and constraints.

## Run locally

Prerequisites: Java 21 or newer, Node.js 22.12 or newer for the Vitest suite, and npm.

### Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`.

### Frontend

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The Vite development server preserves the local proxy: `/api` is forwarded to `http://localhost:8080`, so the browser uses same-origin API paths in development.

Load a source from **Datasets** or **Ingestion**. The application starts with no dataset; dataset-backed product endpoints do not invent first-run metrics.

Incident evidence is reachable at `/incidents`, `/incidents/:id` and the alias `/investigate/:id`; all three render the same `IncidentsPage` detail view.

## Run with Docker Compose

Docker Compose uses two images:

- `backend/Dockerfile` is a multi-stage Maven build with a non-root Java 21 runtime. The repository `sample-data/` directory is copied into `/app/sample-data` and selected with `LOGINSIGHT_SAMPLE_DATA_DIR`.
- `frontend/Dockerfile` is a multi-stage Node build with a non-root Nginx runtime. `frontend/nginx.conf` serves the SPA, proxies `/api/` to the backend service, disables proxy buffering for SSE, and falls back to `index.html` for client routes.

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose ps
```

Open `http://localhost:8088`. The default host mappings are backend `8080` and frontend `8088`; change them in `.env` when needed. See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for healthchecks, operations and limitations.

The Compose file was syntax-validated with `docker compose config`. The Docker daemon was not available in the audit environment, so image builds and live container smoke tests were not run here.

## Data and replay honesty

- **Demo Dataset**: 14,000 deterministic synthetic events generated by `DemoDatasetGenerator` with seed `20260913L`, re-anchored to the current hour. It is not production telemetry.
- **Bundled samples**: committed text and JSONL files under `sample-data/`, selectable from the Datasets screen.
- **Upload**: multipart JSONL or canonical pipe-delimited text. The parser reports successful and failed line counts.
- **Current state**: one dataset is held in backend memory at a time. Restarting the backend clears it.
- **Demo Replay**: a bounded Server-Sent Events replay of the loaded dataset, emitted oldest-first by `(timestamp, id)`. It is labelled `demo-replay` and "not real-time" in the API and UI. It is not an external log collector, WebSocket feed, or live ingestion path.
- **Shared replay state**: `ReplayProvider` in `frontend/src/replay/ReplayContext.tsx` owns the single subscription, the recent-event buffer (capped at 300) and the dataset-invalidation reset. Command Center and Demo Replay are two views of that one subscription, not two streams.

### Terminology

These documents distinguish two separate data modes and never use one name for the other:

- **Dataset Replay** — a bounded replay of the *loaded dataset*. Route `/replay`, sidebar label `Dataset Replay`, backend reports `source: demo-replay`, and the UI shows a "not real-time" disclosure.
- **Live Monitor** — the *deterministic generated simulation*. Route `/live`, sidebar label `Live Monitor`, backend reports `source: live-simulation`. It is generated from a scenario definition plus a seed, not captured from any external system.

`/live` is **not** the dataset replay route. Dataset replay lives at `/replay`. Scenario configuration and run control live at `/scenario-lab` (alias `/simulation`).

## API behavior

All browser API calls use `/api`. Dataset-backed requests return a safe JSON error when no dataset is loaded. The normal error shape is:

```json
{
  "status": 404,
  "error": "DatasetException",
  "message": "No dataset loaded",
  "timestamp": "2026-09-25T00:00:00Z",
  "path": "/api/overview"
}
```

The current handler maps validation and malformed requests to `400`, missing dataset state to `404`, unsupported methods/media to `405`/`415`, and unexpected failures to a sanitized `500`. Upload size is 64 MB, product-search pages use `size=1..200`, log slices use `limit=1..1000`, replay controls are validated at `batchSize=1..200` and `intervalMs=100..60000`, and run history is capped at 64 records. The complete contract and limit table are in [docs/API.md](docs/API.md).

## Algorithms

### DSA-3 relationship

The six catalogue modules map directly onto the DSA-3 subject groupings: **Strings**, **Dynamic Programming**, **Graph & Flow**, **Approximation**, **Randomized**, and **Parallel**. Each descriptor is classified as a **product feature** (genuinely exercised by a product path), an **algorithm engine** (a real REST or trace endpoint that a user or a run session can invoke), or an **academic lab** entry (implemented and tested, but not wired into a product panel). The per-algorithm classification is in [docs/DSA_PRODUCT_MAPPING.md](docs/DSA_PRODUCT_MAPPING.md), and the subject-by-subject breakdown is in [docs/04-dsa-mapping.md](docs/04-dsa-mapping.md). No algorithm is presented as a product capability unless a product path actually calls it.

The backend catalogue currently contains 42 descriptors across six modules, 35 registered query engines, 36 catalogue entries reachable through a REST or trace endpoint, and 13 trace-instrumented algorithms. Product-facing algorithm use is intentionally narrower:

- KMP powers free-text product search.
- Levenshtein powers the zero-result “Did you mean?” suggestion.
- Heuristic token normalization powers Patterns; it is not ML.
- Five-minute baseline thresholding powers Incidents; it is rule-based and exposes evidence.
- Group-by-`requestId` adjacency folding builds the Command Center topology; it is a deterministic pass over the log, not a service registry.
- Aho-Corasick multi-pattern search and a rolling sliding-window aggregate power the generated simulation's evidence panel, and declared-graph BFS computes the incident blast radius. These are simulation-path algorithms, not claims about observed infrastructure.
- Naive, KMP, Z and Rabin-Karp are compared by one measured run per matcher over the loaded dataset.
- The remaining catalogue entries are exposed algorithm engines, library-only implementations, or trace/run-session capabilities; they are not all claimed to drive a product panel.
- The course rule against `java.util` delegation in `dsa/**` is enforced by `EngineScopeGuardTest`: it freezes today's exact `java.util` imports per `dsa` source file in a manifest, fails the build on any new `java.util` import under `dsa/`, and allows the ledger to shrink. `java.util.concurrent` (parallel) and `java.util.Random` (randomized) are course-licensed and recorded in that manifest.

The implementation inventory and exposure rules are in [docs/ALGORITHMS.md](docs/ALGORITHMS.md). The frontend includes a Three.js/WebGL topology view; there is no WebSocket transport, external research integration, trained ML model, or production telemetry integration.

## Verification

**Local verification** — the current checkout was verified with:

```powershell
cd backend
.\mvnw.cmd -o verify

cd ..\frontend
npm test
npm run build
```

Results: backend **877 tests**, 0 failures, 0 errors, 0 skipped, jar packaged; frontend Vitest **55 tests** across **17 files** passed; TypeScript and the Vite production build clean. The backend produces JaCoCo reports under `backend/target/site/jacoco/`. The 17 frontend test files are `api/client.test.ts`, `components/format.test.ts`, `components/Layout.test.tsx`, `components/TopologyPanel.test.tsx`, `pages/AlgorithmsPage.test.tsx`, `pages/AnalyticsPage.test.tsx`, `pages/IncidentWorkbench.test.tsx`, `pages/LivePage.test.tsx`, `pages/LogsPage.test.tsx`, `pages/MonitorPage.test.tsx`, `pages/OverviewPage.test.tsx`, `pages/PatternsPage.test.tsx`, `pages/ScenarioLabPage.test.tsx`, `pages/ServicesPage.test.tsx`, `replay/ReplayContext.test.tsx`, `telemetry/adapters.test.ts` and `telemetry/SimulationBand.test.tsx`; `docs/13-testing.md` tables every one of them.

**GitHub Actions verification** — `.github/workflows/ci.yml` runs on every push to `main`: the backend job runs `mvn -B verify`, and the frontend job runs `npm ci`, `npm test` and `npm run build`. The authoritative merge gate is the workflow run, not the local run above; the two are recorded separately. Earlier phase reports in `docs/` carry smaller backend counts from their own snapshots and are labelled as such.

## Limitations

- In-memory, single-current-dataset state; no database, durable uploads, or restart persistence.
- Run history is bounded at 64 and trace steps at 400; both reset with the process.
- No authentication, authorization, multi-tenant isolation, or production observability-scale ingestion.
- The Dataset Replay screen is a labelled bounded replay of the loaded dataset, not a live collector. The Live Monitor screen is server-generated simulation, not captured telemetry.
- Service topology edges are observed request-trail adjacency inferred from `requestId` co-occurrence. They are not verified infrastructure, and a missing edge is not proof of a missing call.
- Service health bands are error-rate thresholds (healthy <5%, watch 5–<10%, elevated ≥10%, unavailable when the rate is not finite). They are heuristics, not a health model.
- Benchmarks are host- and input-dependent measurements, not universal performance claims.
- No WebSocket transport, research integration, ML training or inference, or external log transport. The optional WebGL topology depends on device/browser support and has an SVG fallback.
- Docker deployment is a practical single-node evaluation setup; TLS, secrets, durable storage and horizontal scaling belong in a production platform layer.
- Docker image builds and container smoke tests were still not run: the Docker daemon is not available in this environment, so only `docker compose config` has been validated.

## Documentation

- [Current implementation audit](docs/IMPLEMENTATION_AUDIT.md)
- [Command Center semantics](docs/COMMAND_CENTER.md)
- [Deployment guide](docs/DEPLOYMENT.md)
- [API reference](docs/API.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Datasets and replay](docs/DATASET.md)
- [Algorithms](docs/ALGORITHMS.md)
- [UI and accessibility](docs/UI-UX.md)
- [Testing](docs/13-testing.md)
- [Demo guide](docs/DEMO_GUIDE.md)
- [Project walkthrough](docs/PROJECT_WALKTHROUGH.md)
- [Historical reports](docs/REBUILD_BASELINE.md), retained as snapshots rather than current specifications
