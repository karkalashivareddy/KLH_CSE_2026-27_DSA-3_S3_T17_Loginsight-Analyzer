# Command Center

Routes: `/` and `/command-center` (`frontend/src/pages/OverviewPage.tsx`).

The Command Center is a selected-window operations view. Every number on it is a value returned by the backend for the currently loaded dataset. The page does not synthesize metrics, and the no-dataset state is an explicit prompt rather than zeros.

## Selected-window overview semantics

`GET /api/overview?range=5m|15m|1h|6h|24h` (default `1h`; an unknown value falls back to `1h`) is resolved by `OverviewService.snapshot`.

| Field | Meaning |
|---|---|
| `windowEnd` | The newest `timestamp` in the loaded dataset. If no event carries a timestamp, the current instant is used instead. |
| `windowStart` | `windowEnd` minus the nominal range width. The window is a trailing slice of the data, not of the wall clock. |
| `range` | The echoed range id. |
| `scope` | The literal string `selected-window`. The UI renders it as the card subtitle and falls back to `<range> selected window` if it is absent. |
| `datasetEvents` | The unfiltered event count of the loaded dataset. This is the denominator for selected-window coverage. |
| `events` | Events inside `[windowStart, windowEnd]`, inclusive at both ends. |
| `errors` / `warnings` | Window-scoped counts: `ERROR`+`FATAL` and `WARN`. |
| `services` / `hosts` | Distinct non-null services and hosts inside the window. |
| `severity`, `statusCodes`, `topServices`, `topPatterns`, `recentCritical`, `timeline`, `heatmap` | All computed over the window only. `recentCritical` is the 12 newest `ERROR`/`FATAL` window events. |
| `activeIncidents` | Count of heuristic windows returned by `IncidentDetector` run over the window's events, with the detector's 200-window cap. It is a count of detected windows, not a count of open or ongoing incidents. |
| `eventsPerMinute` | `window events / (range width in minutes)`. |
| `systemStatus` | Compatibility field. Always the string `"Operational"`. Not a probe. |

### eventsPerMinute

The denominator is the nominal width of the selected range (5, 15, 60, 360 or 1440 minutes), not the observed span between the first and last event inside the window. A window that contains events clustered in two minutes still divides by the full range width, so the displayed rate reads low. It is a normalized window rate, not a measured inter-arrival or arrival-process rate. The UI labels it "Selected-window observed rate" and pairs it with the raw window event count.

### Real health versus the compatibility status field

`OverviewDto.systemStatus` is a legacy field that `OverviewService` hardcodes to `"Operational"`. It carries no runtime information and does not reflect the backend, the dataset or any dependency.

Real runtime status is `GET /api/health/status`, which reports `status`, `service`, `timestamp`, `uptimeMillis`, `datasetLoaded`, `datasetName`, `datasetSize` and the registered query-engine count. `GET /api/health` and `GET /api/health/ready` are liveness probes; `GET /api/health/dataset` is a dataset-presence probe that answers 404 when nothing is loaded.

The Command Center header renders `health.data?.status ?? data.systemStatus`: the real health value when the request has resolved, and the compatibility string only as a transient fallback. `SystemPage` reads the same endpoint.

## Observed request-trail topology

`GET /api/analytics/dependencies` is rendered by `components/TopologyPanel.tsx`. `ServiceGraphBuilder` groups events by `requestId` in ingestion order, sorts each group by `(timestamp, id)`, and links each consecutive pair of distinct services into a directed `a -> b` edge. The edge weight is the number of times that ordered pair was observed.

What the graph is:

- An observation of adjacency inside the loaded logs, over the **full current dataset**, not the selected window. The panel says so in its card subtitle.
- A frequency count, not a latency, an error attribution, or a routing table.

What the graph is not:

- It is not verified infrastructure topology. Nothing here is confirmed against a service registry, an orchestrator, an APM agent or a network trace.
- A missing edge is not evidence of a missing call. Services that never shared a `requestId`, or that logged under different names, will not appear.
- Node size follows observed event counts. It is not a capacity or traffic-share value.

### Deterministic edge-weight particles

Each edge draws `min(8, max(1, ceil(normalizedWeight * 7)))` particles, where `normalizedWeight = weight / maxEdgeWeight` over the currently rendered edge set. Particle count, edge stroke width (`1 + normalized * 4`), stroke opacity (`0.3 + normalized * 0.55`), curvature offset and animation duration (`max(1.2, 3.2 - normalized * 1.8)`) are all pure functions of the returned weight, so the same data always produces the same picture.

Particle positions are computed analytically on the quadratic Bézier control path at `(particleIndex + 1) / (particleCount + 1)`, and each `animateMotion` is staggered by `particleIndex * duration / particleCount`. Nothing is random or time-seeded. `prefers-reduced-motion: reduce` drops `animateMotion` and marks the particles `topology-particle--static`; opacity-only CSS animation is also disabled.

### Display modes

| Mode | Rendering |
|---|---|
| `2d` | Flat SVG on a 760×440 viewBox. Renderer note: "2D SVG renderer · not WebGL". |
| `3d` / depth | The same flat SVG inside a CSS `perspective: 1000px` stage with `transform: rotateX(38deg) rotateZ(-5deg) scale(0.88)`. Renderer note: "2.5D / SVG perspective · not WebGL". |

The depth mode is a 2.5D CSS transform over a 2D SVG. There is no WebGL context, no shader, no `three.js`, no z-buffer and no real 3D projection. The button label says "3D / depth" and the panel note says 2.5D, because the underlying geometry is still flat.

### Interaction and accessibility

- Nodes are laid out on a ring by node count, not by a force or graph layout.
- Nodes are keyboard-operable: `Enter` or `Space` selects, and each carries an `aria-label` with the service name, observed event count and health band.
- "Focus selected" narrows the `viewBox` around the selected node; "Reset view" clears the selection and restores the full viewBox.
- An accessible service list mirrors the node set, and a second list spells out every edge as `source -> target` with its observed weight. The SVG is decorative for assistive technology; the lists are the accessible representation.
- The panel is controlled or uncontrolled: `mode`/`onModeChange` and `selectedId`/`onSelect` are used by the Command Center, while a standalone `defaultMode` is available for reuse.

## Heuristic service health bands

Health is derived from the selected-window `eventRate` (error percentage) of each top service:

| Band | Condition |
|---|---|
| Healthy | rate < 5% |
| Watch | 5% ≤ rate < 10% |
| Elevated | rate ≥ 10% |
| Unavailable | rate is not finite (no matching service rollup, or a non-numeric value) |

These are fixed error-rate thresholds applied to a top-N rollup. They are not a health model, not a learned score, and not a substitute for SLI/SLO evaluation. A service absent from the selected-window top services renders with the "unknown" band rather than being dropped, and a non-finite rate is never rounded to 0%.

The same thresholds color the topology node health dot and CSS class.

## Pipeline story

The pipeline strip links each Command Center number to the surface that produced it:

| Step | Link | Value shown |
|---|---|---|
| Load | `/ingestion` | The loaded dataset name. |
| Observe | `/analytics` | Selected-window event count. |
| Detect | `/incidents` | Heuristic window count. |
| Investigate | `/logs` | Critical events returned. |

It is a navigation aid over data already on screen, not a process model of the application.

## Shared replay context

`frontend/src/replay/ReplayContext.tsx` exports `ReplayProvider` and `useReplay`. `App.tsx` wraps the router in one `ReplayProvider`, so Command Center and Demo Replay are two views of a single subscription rather than two competing SSE streams.

- The provider fetches `GET /api/live/status` once on mount and refetches after a dataset invalidation.
- `start()` opens one `api.liveStream` subscription. A generation counter invalidates callbacks from a superseded subscription, so a stopped stream cannot update state after a restart.
- State is `idle | starting | streaming | complete | stopped | error`. A stream that closes before `replay-complete` is reported as an error rather than silently completing.
- `recentEvents` keeps the newest 300 events across batches; batches are reversed so the newest event of a batch appears first.
- `subscribeDatasetInvalidation` resets the stream and reloads status, so switching datasets cannot leave a replay pointing at the previous one.
- The Command Center card mirrors `emitted / total`, progress, the dataset name and the `demo-replay` source label, and can start, stop or restart the shared stream.

The backend side of this stream is a bounded replay of the loaded dataset, sorted by `(timestamp, id)` and emitted oldest-first. It is labelled `demo-replay` and is not real-time capture; see [API.md](API.md) and [DATASET.md](DATASET.md). The screen is called Demo Replay in these documents; its route is `/live` and the current navigation and page label is `Live Replay`.

## Route note

`/incidents`, `/incidents/:id` and the alias `/investigate/:id` all render `IncidentsPage`. The `investigate` alias is registered in the command palette alongside `/incidents`; selecting an incident from the Command Center navigates to `/incidents/{id}`.

## What this page does not do

- It does not attribute cause. The investigation card states that the window and pattern are returned detector output.
- It does not present a healthy band as an operational verdict.
- It does not render a 3D scene, and it does not connect to any live external feed. The depth mode is a 2.5D CSS transform over a flat SVG.
