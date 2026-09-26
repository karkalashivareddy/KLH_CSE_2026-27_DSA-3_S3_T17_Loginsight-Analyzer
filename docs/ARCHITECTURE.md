# Architecture

## Runtime topology

```text
Browser
  │
  ▼
React 18 + TypeScript + Vite SPA
  │  relative /api REST and SSE
  ├── development: Vite proxy ──► Spring Boot :8080
  └── deployment: Nginx :8080 ──► backend:8080
                                      │
                         Spring Boot REST/SSE API
                                      │
                 DatasetService ── one current in-memory Dataset
                                      │
       ┌──────────────┬──────────────┼──────────────┐
       ▼              ▼              ▼              ▼
     parsers       LogIndex      analytics     algorithms
                                      │
                             controllers → services
```

## Frontend

`frontend/src/App.tsx` defines the current route tree and wraps it in `ReplayProvider`. `Layout.tsx` supplies grouped navigation, breadcrumbs, status indicators, a command palette, a mobile drawer and the skip link. Pages fetch through `api/client.ts`, which uses the `/api` base path, request timeouts, cancellation, error-envelope normalization and manual SSE parsing. `useApi.ts` handles loading, refresh, cancellation and dataset-change invalidation.

`replay/ReplayContext.tsx` owns the one Demo Replay SSE subscription for the whole app. Because the provider sits above the router, the Command Center and the Demo Replay page render the same stream state, progress and event buffer rather than each opening a connection. A generation counter invalidates callbacks from a superseded subscription, and `subscribeDatasetInvalidation` resets the stream when the dataset changes. The backend snapshot is sorted by `(timestamp, id)` and emitted oldest-first, so the "live" route name is a label on a finite ordered replay.

Charts and graph views are local SVG components in `components/ui.tsx`. The Command Center service topology in `components/TopologyPanel.tsx` is also SVG, with an optional 2.5D CSS `perspective` + `rotateX` transform for its "3D / depth" mode; it is not a 3D renderer and no WebGL context exists. There is no chart library, no WebGL or 3D engine, no WebSocket client and no browser-side data generator.

## Backend

The backend is Java 21 with Spring Boot 3.5.16 and Spring Web. Controllers are thin and map HTTP requests to services. `GlobalExceptionHandler` provides the JSON error contract. `QueryDispatcher` routes resolved `QueryContext` objects to the registered `QueryEngine` implementations.

The main layers are:

- `controller`: REST, multipart and SSE transport.
- `service`: dataset lifecycle, product analysis, benchmark orchestration, run lifecycle and trace mapping.
- `search`, `index`, `parser`, `analytics`, `pattern`, `incident`, `graph`: product analysis over the active dataset.
- `query`, `catalog`, `trace`, `run`: algorithm dispatch, catalogue metadata, recorded execution and replay.
- `dsa`: hand-written algorithm implementations and supporting data structures.

`DatasetService` holds one volatile current dataset. `LogIndex` keeps sorted field and timestamp position lists for that dataset. There is no database, JPA layer or external cache.

## Deployment topology

The Compose deployment adds an Nginx runtime in front of the API. Nginx serves the Vite build, preserves `/api` paths, disables proxy buffering for SSE and uses `try_files` for client-side routes. The backend image includes the bundled sample directory and runs as a non-root Java user. Both containers have healthchecks.

See [DEPLOYMENT.md](DEPLOYMENT.md) for commands and [nginx.conf](../frontend/nginx.conf) for the routing details.

## Data flow

1. The user chooses a bundled sample, generated demo or uploaded file.
2. The parser detects canonical text or JSONL and reports per-line failures.
3. The service assigns event IDs and installs one current dataset.
4. The index and analyzers derive counts, rollups, search results, patterns and incidents from that snapshot.
5. The frontend receives DTOs and renders them; it does not synthesize missing dataset values.
6. A replay request reads the same in-memory event list, sorts it oldest-first, and emits it over SSE with a `demo-replay` disclosure.

### Command Center aggregation

The Command Center issues four independent requests and composes them without cross-checking them:

| Request | Scope | Used for |
|---|---|---|
| `GET /api/overview?range=` | Selected window | Metric strip, timeline, severity, heatmap, patterns, critical events, window context |
| `GET /api/analytics/dependencies` | Full current dataset | Observed request-trail topology |
| `GET /api/incidents?limit=20` | Full current dataset | Detected investigation card, filtered client-side against the overview window |
| `GET /api/health/status` | Process | Real runtime status pill |

Because the two scopes differ, topology node/edge counts do not reconcile with the window-scoped metric strip by construction, and the incident card filters the returned list against `windowStart`/`windowEnd` in the browser. The page surfaces `windowStart`, `windowEnd` and `scope` on the timeline and window-context cards so the selected scope is visible rather than assumed.

Two honesty details are load-bearing here. `OverviewDto.systemStatus` is a hardcoded compatibility string, so the header prefers the real `/api/health/status` value. And the topology edges are `requestId` co-occurrence, not verified infrastructure, so the panel labels them as observed request-trail adjacency.

See [COMMAND_CENTER.md](COMMAND_CENTER.md) for the field-level semantics.

## Trust boundaries and limitations

The API has no authentication or authorization. The local Vite proxy and Nginx same-origin route avoid browser cross-origin requests, but they are not security controls. Runtime state is not durable, and the design is not a multi-instance architecture.

The dependency graph is derived from log content, so it inherits the dataset's coverage gaps and naming inconsistencies. The CSS depth mode is a presentation transform over a flat SVG, not a projection. Neither should be read as an authoritative view of the deployment.
