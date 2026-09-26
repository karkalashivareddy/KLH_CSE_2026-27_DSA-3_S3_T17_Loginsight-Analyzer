# LogInsight Analyzer

LogInsight Analyzer is a full-stack, in-memory log investigation workspace. It combines a React 18 + TypeScript + Vite shell with a Java 21 Spring Boot REST/SSE API, classical data-structure and algorithm engines, and a backend-owned dataset lifecycle.

> Academic/portfolio software. The Docker artifacts are a single-node deployment shape, not a claim of production scale or public deployment.

## Current implementation

The current shell is a dark observability workspace with:

- Command Center, Log Explorer, Search, Analytics, Patterns, Incidents, Demo Replay, Datasets and Ingestion views.
- An Algorithm Lab with the backend catalogue, measured search benchmark and recorded run sessions.
- System and Documentation views.
- A responsive navigation shell with a command palette, breadcrumbs, runtime status and dataset status.
- Hand-rolled SVG visualizations. The Command Center service topology offers a 2D layout and a 2.5D "3D / depth" mode built from SVG plus CSS `transform` on a `perspective` stage. There is no chart library, no WebGL or WebGPU renderer, and no WebSocket client.

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

The topology panel renders in a 2D SVG layout or a 2.5D depth mode that applies `rotateX`/`rotateZ`/`scale` to the same SVG inside a CSS `perspective` stage. The renderer note in the panel states "not WebGL" in both modes. There is no WebGL context, shader, 3D engine or `three.js` dependency.

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

These documents use **Demo Replay** wherever a replay of loaded data is meant. The route is `/live`, and the sidebar entry and page heading are currently labelled `Live Replay`; that label is a navigation name for a bounded dataset replay, not a real-time capture claim. The backend reports the stream as `source: demo-replay`, and the UI shows the `demo-replay` label and a "not real-time" disclosure.

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

The backend catalogue currently contains 42 descriptors across six modules, 35 registered query engines, 36 catalogue entries reachable through a REST or trace endpoint, and 13 trace-instrumented algorithms. Product-facing algorithm use is intentionally narrower:

- KMP powers free-text product search.
- Levenshtein powers the zero-result “Did you mean?” suggestion.
- Heuristic token normalization powers Patterns; it is not ML.
- Five-minute baseline thresholding powers Incidents; it is rule-based and exposes evidence.
- Group-by-`requestId` adjacency folding builds the Command Center topology; it is a deterministic pass over the log, not a service registry.
- Naive, KMP, Z and Rabin-Karp are compared by one measured run per matcher over the loaded dataset.
- The remaining catalogue entries are exposed algorithm engines, library-only implementations, or trace/run-session capabilities; they are not all claimed to drive a product panel.
- The course rule against `java.util` delegation in `dsa/**` is enforced by `EngineScopeGuardTest`: it freezes today's exact `java.util` imports per `dsa` source file in a manifest, fails the build on any new `java.util` import under `dsa/`, and allows the ledger to shrink. `java.util.concurrent` (parallel) and `java.util.Random` (randomized) are course-licensed and recorded in that manifest.

The implementation inventory and exposure rules are in [docs/ALGORITHMS.md](docs/ALGORITHMS.md). No WebGL/3D engine, WebSocket transport, external research integration, trained ML model, or production telemetry integration is implemented. The topology "3D / depth" mode is a 2.5D CSS transform over a flat SVG, not a 3D renderer.

## Verification

The current checkout was verified with:

```powershell
cd backend
.\mvnw.cmd -o verify

cd ..\frontend
npm test
npm run build
```

Results from this audit: backend `827` tests, `0` failures, `0` errors; frontend Vitest `24` tests across `10` files passed; frontend TypeScript and Vite production build passed. The backend produces JaCoCo reports under `backend/target/site/jacoco/`. The frontend test files are `api/client.test.ts`, `components/format.test.ts`, `components/Layout.test.tsx`, `components/TopologyPanel.test.tsx`, `pages/AlgorithmsPage.test.tsx`, `pages/AnalyticsPage.test.tsx`, `pages/LivePage.test.tsx`, `pages/OverviewPage.test.tsx`, `pages/PatternsPage.test.tsx` and `replay/ReplayContext.test.tsx`. The frontend has no configured browser-test or lint script. Accessibility and responsive behavior are documented from source and component tests rather than an automated accessibility run.

Continuous integration runs the same three commands: `mvn -q verify` in the backend job, then `npm ci`, `npm test` and `npm run build` in the frontend job. Earlier phase reports in `docs/` record smaller backend counts from their own snapshot; those figures are historical and are not the current gate.

## Limitations

- In-memory, single-current-dataset state; no database, durable uploads, or restart persistence.
- Run history is bounded at 64 and trace steps at 400; both reset with the process.
- No authentication, authorization, multi-tenant isolation, or production observability-scale ingestion.
- The Demo Replay screen is a labelled dataset replay, not a live collector.
- Service topology edges are observed request-trail adjacency inferred from `requestId` co-occurrence. They are not verified infrastructure, and a missing edge is not proof of a missing call.
- Service health bands are error-rate thresholds (healthy <5%, watch 5–<10%, elevated ≥10%, unavailable when the rate is not finite). They are heuristics, not a health model.
- Benchmarks are host- and input-dependent measurements, not universal performance claims.
- No WebGL/3D engine, WebSocket transport, research integration, ML training or inference, or external log transport. The topology depth mode is 2.5D SVG plus CSS transforms.
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
- [Project walkthrough](docs/PROJECT_WALKTHROUGH.md)
- [Historical reports](docs/REBUILD_BASELINE.md), retained as snapshots rather than current specifications
