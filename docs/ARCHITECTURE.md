# Architecture

## Runtime topology

```text
Browser
  â”‚
  â–¼
React 18.3 + TypeScript 5.6 + Vite 6 SPA
  â”‚  relative /api REST and SSE
  â”œâ”€â”€ development: Vite proxy â”€â”€â–º Spring Boot :8080
  â””â”€â”€ deployment: Nginx :8080 â”€â”€â–º backend:8080
                                      â”‚
                         Spring Boot REST/SSE API
                                      â”‚
                 DatasetService â”€â”€ one current in-memory Dataset
                                      â”‚
       â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
       â–¼              â–¼              â–¼              â–¼
     parsers       LogIndex      analytics     algorithms
                                      â”‚
                             controllers â†’ services
```

## Frontend

### Component tree

```text
main.tsx
  â””â”€ AppErrorBoundary          class boundary, scope="LogInsight"
       â””â”€ App
            â”œâ”€ ReplayProvider        one dataset-replay SSE subscription
            â”œâ”€ TelemetryProvider     one simulation SSE subscription
            â”œâ”€ BrowserRouter
            â”‚    â””â”€ GuidedDemoProvider
            â”‚         â””â”€ Layout      nav, header, palette, skip link,
            â”‚              â”‚          <Atmosphere/>, <motion.div.route-view>
            â”‚              â””â”€ Outlet
            â”‚                   â”œâ”€ OverviewPage           entry chunk
            â”‚                   â”œâ”€ LogsPage               entry chunk
            â”‚                   â”œâ”€ IncidentWorkbenchPage  entry chunk
            â”‚                   â””â”€ 17 lazy() routes, each in Suspense
```

`AppErrorBoundary` is a class component with `getDerivedStateFromError` and `componentDidCatch`. It renders children normally and swaps in a `role="alert"` recovery panel on a render-time throw, offering "Try again" (reset state, re-render the subtree) and "Reload workspace" (`window.location.reload()`). The caught error is written to the developer console only; the DOM never receives a stack trace or a component name. It exists because without it any render-time failure unmounts the entire tree and produces a blank page with no message â€” which a reachable-but-failing backend could cause.

`Atmosphere` is rendered by `Layout` as a sibling *before* `.app-shell`. The stylesheet pins `.atmosphere` at `z-index: -2`, so the decorative backdrop can never paint above page content.

`Layout.tsx` supplies grouped navigation, breadcrumbs, status indicators, a command palette, a mobile drawer and the skip link. Pages fetch through `api/client.ts`, which uses the `/api` base path, request timeouts, cancellation, error-envelope normalization and manual SSE parsing. `useApi.ts` handles loading, refresh, cancellation and dataset-change invalidation. The guided demonstration traverses existing routes and runs requests through the same API client; it can load the deterministic demo corpus only if no dataset is currently active.

`replay/ReplayContext.tsx` owns the one Dataset Replay SSE subscription for the whole app. Because the provider sits above the router, the Command Center and the Dataset Replay page render the same stream state, progress and event buffer rather than each opening a connection. A generation counter invalidates callbacks from a superseded subscription, and `subscribeDatasetInvalidation` resets the stream when the dataset changes. The backend snapshot is sorted by `(timestamp, id)` and emitted oldest-first, so the "live" route name is a label on a finite ordered replay.

### Route-level code splitting

`App.tsx` imports three page modules directly and wraps the remaining seventeen in `lazy()`:

| Kept in the entry chunk | Why |
|---|---|
| `OverviewPage` (Command Center) | primary operational landing surface |
| `LogsPage` (Logs Explorer) | primary operational surface |
| `IncidentWorkbenchPage` (Incident Workbench) | primary operational surface |

A cold navigation to any of the three should not pay a network round trip. Everything else is split out. The most consequential of these is the WebGL topology, which is only reachable from the Services page and would otherwise drag its entire dependency graph into the first load.

Each lazy route is wrapped in `<Suspense fallback={<RouteFallback />}>`. The fallback deliberately keeps the page title visible â€” a skeleton line and three skeleton tiles, marked `aria-hidden` â€” rather than replacing the route with a spinner, so navigating never leaves the workspace without context.

### Vendor chunking

`vite.config.ts` complements route splitting with a `manualChunks` splitter, so long-lived vendor code lands in named chunks that do not change when application code changes:

| Chunk | Contents | First load? |
|---|---|---|
| `index` | application entry | yes |
| `vendor-react` | `react-dom`, `react-router`, `scheduler` | yes |
| `vendor-motion` | `motion` / `framer-motion` | yes |
| `vendor-icons` | `lucide-react` | yes |
| `vendor-three` | `three` (Three.js) | **no** â€” only when the 3D topology is opened |

`Topology3D` itself reduces to a 9.88 kB wrapper module; the 562.23 kB Three.js payload sits behind it in `vendor-three`.

Measured effect on first-load JavaScript:

| | Before | After |
|---|---|---|
| Raw | ~1061 kB | ~493 kB |
| Gzip | ~281 kB | ~154 kB |

`chunkSizeWarningLimit` is set to 600 kB. That is a deliberate threshold change, not a suppression: the measured `vendor-three` chunk is 562.23 kB and the build now emits **no** size warning. The limit was raised to reflect the real per-chunk cost of the one intentionally deferred dependency.

### Motion layer

`motion@^14.1.0` (the Framer Motion successor) is a runtime dependency, imported from `motion/react`. Two new modules sit at `src/motion/`:

**`motion/motion.ts`** â€” the token vocabulary, so a timing change is made once per medium rather than per component. The JavaScript values mirror the CSS custom properties of the same name in `signal-atlas.css`:

| Export | Values |
|---|---|
| `MOTION.fast` / `.normal` / `.slow` / `.scene` | `0.16` / `0.26` / `0.42` / `0.62` |
| `EASE.out` / `EASE.inOut` | `[0.22, 0.78, 0.28, 1]` / `[0.6, 0.02, 0.3, 1]` |
| `spring` | `{ type: 'spring', stiffness: 420, damping: 34, mass: 0.8 }` |

plus `fadeTransition`, `routeVariants`, `stagger()`, `itemVariants`, `overlayVariants` and `surfaceVariants`.

**`motion/Atmosphere.tsx`** â€” a decorative scroll-linked light field: a three-gradient veil and a six-path isobar SVG, driven by `useScroll`/`useTransform` at two different rates (`-14%` and `-30%` over the full scroll). Design constraints:

- *Performance.* Both layers write only to compositor motion values via `style`. No React component re-renders while scrolling, and this component registers no scroll listener.
- *Accessibility.* `aria-hidden`, `pointer-events: none`, and the component returns `null` entirely under `prefers-reduced-motion: reduce` â€” the backdrop is not merely hidden, it is never mounted.

`Layout` consumes `useReducedMotion()`, `MOTION` and `EASE` directly: it wraps the route outlet in a `motion.div.route-view` keyed on `location.pathname` (so a route change replays the entrance while `Layout` itself never unmounts and shell state survives), and it short-circuits the scroll-reveal `IntersectionObserver` when reduced motion is requested.

Charts remain local SVG components in `components/ui.tsx`. The Command Center service topology defaults to the accessible SVG renderer in `components/TopologyPanel.tsx`; its optional `Topology3D` chunk uses Three.js/WebGL and OrbitControls over the same observed dependency API response. The 3D scene is lazy-loaded, visibility-aware, capped to 500 rendered edges and disposable on unmount. SVG service and edge lists remain available independently of WebGL. There is no WebSocket client or browser-side telemetry generator.

### Stylesheet layering

```css
/* frontend/src/styles/signal-atlas.css */
@import './global.css'  layer(loginsight-structure);
@import './product.css' layer(loginsight-structure);
/* signal-atlas.css itself is deliberately UNLAYERED */
```

The **Atmospheric Signal** design system is defined in `frontend/src/styles/signal-atlas.css` (1,312 lines). It owns the token layer, the atmospheric backdrop, the visual identity and all route composition. `global.css` and `product.css` are imported into the named cascade layer `loginsight-structure`, where they retain the structural scaffolding every route depends on.

CSS cascade layers are ordered by declaration: unlayered rules outrank every named layer. So `signal-atlas.css` overrides the two inherited sheets **without editing them**, while the inherited sheets keep supplying layout and component structure underneath. This is why many legacy selectors remain visible in `global.css` and `product.css` while being inactive at runtime â€” they are present in source but outranked.

`main.tsx` imports only `signal-atlas.css`. The entry stylesheet is the single source of truth.

**`signal-in-motion.css` was removed.** A repository-wide grep found no importer, so the retired visual revision and its last copy of the old cobalt accent were deleted rather than left in the tree.

Local Playwright checks cover core routes, semantic token contrast, search, topology fallback, guided presentation, the no-dataset and backend-error states, and responsive overflow at all six documented viewport targets. They do not establish full accessibility conformance or hardware WebGL rendering.

## Backend

The backend is Java 21 with Spring Boot 3.5.16 and Spring Web. Controllers are thin and map HTTP requests to services. `GlobalExceptionHandler` provides the JSON error contract. `QueryDispatcher` routes resolved `QueryContext` objects to the registered `QueryEngine` implementations.

The main layers are:

- `controller`: REST, multipart and SSE transport.
- `service`: dataset lifecycle, product analysis, benchmark orchestration, run lifecycle and trace mapping.
- `search`, `index`, `parser`, `analytics`, `pattern`, `incident`, `graph`: product analysis over the active dataset.
- `query`, `catalog`, `trace`, `run`: algorithm dispatch, catalogue metadata, recorded execution and replay.
- `dsa`: hand-written algorithm implementations and supporting data structures.

`DatasetService` holds one volatile current dataset. `LogIndex` keeps sorted field and timestamp position lists for that dataset. There is no database, JPA layer or external cache.

### SSE emitter lifecycle

All three SSE endpoints are long-lived, and all three now register a full emitter lifecycle:

| Service | Stream | Lifecycle |
|---|---|---|
| `SimulationService` | `/api/simulation/stream` | `onCompletion` / `onTimeout` / `onError` |
| `LiveService` (dataset replay) | `/api/live` | `onCompletion` / `onTimeout` / `onError` |
| `RunService` | `/api/runs/{id}/events` | `onCompletion` / `onTimeout` / `onError` â€” **added this revision** |

`RunService` previously registered none of them, so an abandoned replay kept a pool thread until the 60 s emitter timeout. The replay is now an inner `ReplayTask` class with an `AtomicBoolean` closed flag, making completion, cancellation and failure mutually exclusive and idempotent, plus a bounded 5 s `awaitTermination` in `@PreDestroy shutdown()`.

## Deployment topology

The Compose deployment adds an Nginx runtime in front of the API. Nginx serves the Vite build, preserves `/api` paths, disables proxy buffering for SSE and uses `try_files` for client-side routes. The backend image includes the bundled sample directory and runs as a non-root Java user. Both containers have healthchecks.

See [DEPLOYMENT.md](DEPLOYMENT.md) for commands and [nginx.conf](../frontend/nginx.conf) for the routing details. The Compose model was syntax-validated only; no container runtime was available, so image builds and container smoke tests have not been run and are not claimed.

## Data flow

1. The user chooses a bundled sample, generated demo or uploaded file.
2. The parser detects canonical text or JSONL and reports per-line failures.
3. The service assigns event IDs and installs one current dataset.
4. The index and analyzers derive counts, rollups, search results, patterns and incidents from that snapshot.
5. The frontend receives DTOs and renders them; it does not synthesize missing dataset values.
6. A replay request reads the same in-memory event list, sorts it oldest-first, and emits it over SSE with a `demo-replay` disclosure.

### Upload failure path

`DatasetService.ingest` is the single ingestion entry point for upload and pasted content. Its contract after this revision:

- a `null` stream is rejected with `IllegalArgumentException` â†’ **400**;
- a zero-length body raises `UnsupportedLogFormatException("input stream is empty")` â†’ **400**, regardless of whether a name was supplied;
- a null or blank name is normalized (trimmed; blank/null â†’ `"imported-logs"`) *before* use, so no code path can dereference a null name;
- a parse that yields no successful events raises `DatasetException` â†’ **404**;
- an oversized upload raises `MaxUploadSizeExceededException` â†’ **400**.

`ParserException` is mapped to 400 in `GlobalExceptionHandler`, which is what makes the empty-body case a client error rather than a 500.

### Command Center aggregation

The Command Center issues four independent requests and composes them without cross-checking them:

| Request | Scope | Used for |
|---|---|---|
| `GET /api/overview?range=` | Selected window | Metric strip, timeline, severity, heatmap, patterns, critical events, window context |
| `GET /api/analytics/dependencies` | Full current dataset | Observed request-trail topology, and the Services count in the signal field |
| `GET /api/incidents?limit=20` | Full current dataset | Detected investigation card, filtered client-side against the overview window, and the Detector count in the signal field |
| `GET /api/health/status` | Process | Real runtime status pill |

Because the two scopes differ, topology node/edge counts do not reconcile with the window-scoped metric strip by construction, and the incident card filters the returned list against `windowStart`/`windowEnd` in the browser. The page surfaces `windowStart`, `windowEnd` and `scope` on the timeline and window-context cards so the selected scope is visible rather than assumed.

Three honesty details are load-bearing here. `OverviewDto.systemStatus` is a hardcoded compatibility string, so the header prefers the real `/api/health/status` value. The topology edges are `requestId` co-occurrence, not verified infrastructure, so the panel labels them as observed request-trail adjacency. And the signal field renders an em dash rather than a zero whenever the backend has returned no value, so an unpopulated workspace never looks like a healthy one.

See [COMMAND_CENTER.md](COMMAND_CENTER.md) for the field-level semantics.

## Trust boundaries and limitations

The API has no authentication or authorization. The local Vite proxy and Nginx same-origin route avoid browser cross-origin requests, but they are not security controls. Runtime state is not durable, and the design is not a multi-instance architecture.

The dependency graph is derived from log content, so it inherits the dataset's coverage gaps and naming inconsistencies. The CSS depth mode is a presentation transform over a flat SVG, not a projection. Neither should be read as an authoritative view of the deployment.
