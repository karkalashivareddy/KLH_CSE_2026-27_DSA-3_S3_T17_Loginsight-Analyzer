# Project Walkthrough

This walkthrough describes the current `rebuild/loginsight-v4` implementation rather than an earlier TextHack phase.

## 1. Start with the data

Open Datasets or Ingestion and choose one of three sources:

1. the deterministic 14,000-event Demo Dataset;
2. a committed text/JSONL sample;
3. a multipart upload.

The backend owns the one current dataset. Parser counts and failed lines are returned to the UI.

## 2. Follow the investigation surfaces

- **Command Center** (`/`, `/command-center`) is a selected-window view. `range` selects the window; `windowEnd` is the newest event timestamp in the dataset and `windowStart` is `windowEnd - range`, so the window trails the data rather than the clock. Window-scoped figures are `events`, `errors`, `warnings`, `services`, `hosts`, `severity`, `statusCodes`, `topServices`, `topPatterns`, `recentCritical`, the timeline and the heatmap. `datasetEvents` is the unfiltered total used for the coverage card. `eventsPerMinute` divides window events by the nominal range width in minutes, so it is a normalized window rate rather than a measured inter-arrival rate.
- The **observed request-trail topology** renders `GET /api/analytics/dependencies` for the *full* dataset, not the selected window. Edges are `requestId` co-occurrence observed in the logs, not verified infrastructure. It offers a flat 2D SVG mode and a 2.5D "3D / depth" mode built from a CSS `perspective` + `rotateX` transform; the panel states "not WebGL" in both, and no WebGL context exists.
- **Service health bands** are fixed error-rate thresholds printed in the UI: healthy <5%, watch 5–<10%, elevated ≥10%, unavailable when the rate is not finite.
- The **pipeline story** strip links Load → Observe → Detect → Investigate to `/ingestion`, `/analytics`, `/incidents` and `/logs`.
- **Log Explorer** applies indexed filters and opens event details.
- **Search** accepts `level:`, `service:`, `host:`, `source:`, `status:`, `trace:`, `request:` and `message:` fields. Free text is executed by KMP; a zero-hit result can produce a Levenshtein suggestion.
- **Analytics** derives time, severity, heatmap, HTTP, host, top-K, error and dependency views.
- **Patterns** groups normalized message tokens and labels the result heuristic, not ML.
- **Incidents** groups elevated ERROR/FATAL windows and exposes the method and evidence events. Evidence is reachable at `/incidents`, `/incidents/:id` and the alias `/investigate/:id`.
- **Services** provides rollups and a per-service activity view.
- **Demo Replay** (route `/live`, navigation label `Live Replay`) emits the loaded events over SSE in bounded batches, oldest-first by `(timestamp, id)`. It is explicitly a finite dataset replay, not a live collector. The Command Center and Demo Replay screens share one `ReplayProvider` subscription, so a stream started on either is visible on both.

Full Command Center semantics are in [COMMAND_CENTER.md](COMMAND_CENTER.md).

## 3. Inspect the algorithm layer

- **Algorithms** reads the backend catalogue at `GET /api/analysis/algorithms`: 42 entries across Strings, DP, Flow, Approximation, Randomized and Parallel, with query types, complexity bounds, `traceable`/`exposed` flags and canonical/trace endpoint paths.
- **Benchmarks** runs Naive, KMP, Z and Rabin-Karp once each over the same loaded haystack and reports the smallest measured time.
- **Run Sessions** lists the records created by `POST /api/runs`, opens one by id and replays its recorded steps over SSE. The recorder caps a trace at 400 steps and marks truncation.

The catalogue contains real algorithm engines, including library-only implementations. It does not imply that every algorithm drives a product panel. The course rule that keeps `dsa/**` free of `java.util` delegation is enforced by `EngineScopeGuardTest`, which freezes a per-file `java.util` manifest and fails the build on any new import; see [13-testing.md](13-testing.md).

## 4. Understand the architecture

The frontend uses typed REST/SSE calls under `/api`; Vite proxies those calls during development. The deployment adds Nginx, which serves the Vite build, proxies `/api/` to Spring Boot and falls back to `index.html` for client routes. `ReplayProvider` wraps the router so a single Demo Replay subscription backs both the Command Center and the replay page.

The backend separates controllers, services, index/search/analytics layers, query engines, DSA implementations, catalogue metadata, traces and run storage. All dataset state is in one process. Runtime status comes from `GET /api/health/status`; the `systemStatus` string on the overview DTO is a hardcoded compatibility field and carries no runtime information.

## 5. Verify honestly

```powershell
cd backend
.\mvnw.cmd -o verify

cd ..\frontend
npm test
npm run build
```

The current audit result is 827 backend tests with no failures or errors, 24 frontend tests across 10 files passed, and a clean frontend production build. CI runs the same three commands, so `npm test` is a merge gate. The frontend has no configured browser-test or lint script. Source-level accessibility features and the focused shell, topology, Command Center and replay tests are documented in [UI-UX.md](UI-UX.md), but no full automated accessibility run is claimed.

Docker image builds and container smoke tests have not been run: no Docker daemon is available in this environment, so only `docker compose config` has been validated.

## 6. Know the limits

There is no persistence, authentication, multi-tenancy, external ingestion, WebSocket transport, WebGL/3D engine, research integration or trained ML model. The topology depth mode is a 2.5D CSS transform over a flat SVG, not a 3D renderer. Topology edges are observed `requestId` adjacency over the full loaded dataset rather than verified infrastructure, and health bands are error-rate thresholds rather than a health model. Docker Compose is a practical single-node evaluation deployment, not a production platform. See [COMMAND_CENTER.md](COMMAND_CENTER.md), [DEPLOYMENT.md](DEPLOYMENT.md) and [IMPLEMENTATION_AUDIT.md](IMPLEMENTATION_AUDIT.md).
