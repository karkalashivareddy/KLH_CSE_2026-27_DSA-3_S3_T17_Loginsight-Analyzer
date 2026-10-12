# Command Center

Routes: `/`, `/command-center` and `/overview` (`frontend/src/pages/OverviewPage.tsx`).

The Command Center is a selected-window operations view. Every number on it is a value returned by the backend for the currently loaded dataset. The page does not synthesize metrics, the no-dataset state is an explicit prompt rather than zeros, and the signal field renders em dashes rather than `0` for any value the backend has not returned.

## Page composition

Top to bottom, in render order:

1. **Page header** — eyebrow `LogInsight · Observe`, title `Command center`, a scope sentence, and Refresh / Dataset replay / Live monitor actions.
2. **Hero (`.cc-hero`)** — two columns: the copy block (kicker, display headline, lede, actions, source strip) and the signal field.
3. **Simulation band** (`SimulationBand`) — the generated-simulation frame, when a scenario is running.
4. **Window scope bar** — sticky, states the observed window, offers the five range buttons, and prints the last update time.
5. **Metric strip** — four selected-window metrics.
6. **Main grid** — observed service relationships (topology) beside the current investigation.
7. **Lower grid** — recent critical events beside the activity timeline and recurring patterns.

Between 4 and 5 the page branches on request state: a skeleton while loading, an `ErrorBox` with retry on failure, and an explicit `NoDatasetState` panel when the backend holds no dataset. The metric strip, topology, investigation and lower grid render **only** when `data` is non-null.

## The hero

```tsx
<section className="cc-hero" aria-label="LogInsight overview">
  <div className="cc-hero__copy">
    <div className="cc-kicker">… LogInsight</div>
    <h2>Follow the signal.<br /><em>Read the system.</em></h2>
    <p className="cc-hero__lede">Every stage of an investigation — parsed events, the
      selected window, observed service relationships and heuristic detector windows —
      resolved from the backend and shown in context.</p>
    <div className="cc-hero__actions">
      <button className="btn btn-primary guided-demo-launch">Start guided demo</button>
      <Link className="btn" to="/services">Explore topology</Link>
    </div>
    <SourceStrip … />
  </div>
  <SignalField … />
</section>
```

- The headline is set in the editorial serif (`--font-display`), `clamp(34px, 3.9vw, 56px)`, letter-spacing `-0.03em`, line-height 1. The emphasised second line uses `--accent`.
- The lede is capped at `46ch`.
- The hero is a two-column grid, `min-height: 340px`, `--radius-hero`, `--shadow-raised`, with a 2px spectrum hairline along its base (`:after`) that reads as a pressure gradient rather than a rainbow.
- Below `1100px` the hero becomes `240px / 1fr` at `min-height: 330px`; below `960px` it collapses to one column; below `680px` the headline becomes `clamp(32px, 9.6vw, 42px)` and the action buttons become full-width.

## The signal field

An isobar chart carrying the four real pipeline values. Every number on it is a backend response.

```text
  ┌──────────────────────────────────────────────────────────┐
  │  FROM EVENT TO EVIDENCE          source · demo-stream    │  head
  │                                                           │
  │   ╭────╮   ╭────╮   ╭────╮   ╭────╮                     │
  │   │ 01 │   │ 02 │   │ 03 │   │ 04 │                     │  stages
  │   │14.0k│  │ 615│   │  8  │   │  6  │                    │
  │   ╰────╯   ╰────╯   ╰────╯   ╰────╯                     │
  │  ● Backend-derived source and selected-window values  →   │  foot
  └──────────────────────────────────────────────────────────┘
```

### The four stages

| # | Label | Value source | Detail line | Links to |
|---|---|---|---|---|
| 01 | **Dataset** | `data.datasetEvents` | `parsed events` | `/datasets` |
| 02 | **Window** | `data.events` | `{range} selected` | `/analytics` |
| 03 | **Services** | `dependencies.data.nodeCount` | `{edgeCount} observed edges` | `/services` |
| 04 | **Detector** | `data.activeIncidents` | `heuristic windows` | `/incidents` |

Two things are load-bearing:

1. **The values are real.** Stage 01 and 02 come from `GET /api/overview`, stage 03 from `GET /api/analytics/dependencies`, stage 04 from `GET /api/overview`. Stage 03 is additionally labelled with the edge count from the same response. Nothing is computed in the browser.
2. **The scope is stated, not assumed.** Stage 03 reads `dependencies` over the **full current dataset**, while stages 02 and 04 are **selected-window** values. The head row prints the source dataset name so a reader can see which snapshot is in view, and the footer legend distinguishes the two states:

   | State | Footer text | Legend dot |
   |---|---|---|
   | No dataset | `No telemetry is shown until a source is loaded` | hollow ring |
   | Dataset resolved | `Backend-derived source and selected-window values` | filled `--ok` |

### Em dashes, not zeros

**With no dataset loaded, every stage renders `—` (U+2014 em dash), never `0`.** Each stage's `detail` line also changes so the value is never read as a measurement:

| Stage | With data | Without data |
|---|---|---|
| 01 Dataset | `14,000` / `parsed events` | `—` / `awaiting source` |
| 02 Window | `615` / `1h selected` | `—` / `load data first` |
| 03 Services | `8` / `56 observed edges` | `—` / `graph unavailable` |
| 04 Detector | `6` / `heuristic windows` | `—` / `load data first` |

The head subtitle switches from `waiting for a backend dataset` to `source · {dataset}`. This is deliberate: a `0` is a measurement, and showing `0` for "the backend has no data" would assert something untrue about the system. The same rule applies to the metric strip's error-share tile, which renders `—` when `events === 0`.

This contract is asserted in the browser suite by `the signal field reports real backend values and never invents them`, which cross-checks the rendered stages against the API responses and against the cleared-dataset state.

### Decorative canvas

The isobars, node halo and the travelling `.pulse` path behind the stages are `aria-hidden` and carry no value. The comment in the source is explicit: *"Decorative only: conveys 'signal travelling', never encodes a value."* The `.pulse` stroke animates its dash offset over 6.5 s on a linear drift; it is suppressed under `prefers-reduced-motion`.

Each stage is a `<Link>` carrying `role="listitem"` inside a `role="list"`, with an `aria-label` of the form `{number} {label}: {value}, {detail}` — so a screen reader hears the same information a sighted reader reads.

### Responsive

At `960px` the field drops to `min-height: 272px`; at `680px` the four stages become a 2×2 grid at `min-height: 96px`; at `400px` they become a single column.

## The source strip

States what the workspace is actually reading. It never shows a healthy signal while the request is in flight or has failed.

| State | Head | Body | Links |
|---|---|---|---|
| Request in flight (`loading && !data`) | `Requesting source state`, amber `source-pulse--pending` | `Checking the backend…` | Choose a source, Ingest logs |
| No dataset | `Workspace ready`, green pulse | `Awaiting a data source` — *"The backend holds no dataset, so the signal field stays empty rather than showing invented values."* | Choose a source, Ingest logs |
| Dataset resolved | `Source in view`, green pulse | `{dataset}` + `{scope} · {datasetEvents} dataset events` | Change source; `Bounded dataset replay` provenance chip and replay status when a replay is active |

The prose in the empty state is the honest one: it states the consequence of the rule rather than apologising for a missing number.

## The metric strip

Four tiles, all selected-window, all backend-returned:

| Tile | Value | Note | Tone |
|---|---|---|---|
| **Events observed** | `formatNumber(data.events)` | `{scope} · {datasetEvents} in source` | cyan |
| **Observed rate** | `{eventsPerMinute.toFixed(1)} / min` | `Backend selected-window aggregate` | blue |
| **Error share** | `—` or `{errors/events*100}%` to 2dp | `{errors} ERROR/FATAL of {events} events` | amber at ≥ 5%, else green |
| **Incident windows** | `formatNumber(data.activeIncidents)` | `Heuristic windows returned for scope` | violet |

Two honesty details: the error-share tile renders `—` rather than `0.00%` when the window contains no events (an undefined share is not zero), and the incident tile is labelled *windows* rather than *incidents*, because `activeIncidents` counts detected heuristic windows, not open or ongoing incidents.

At `960px` the strip becomes 2×2; at `680px` each tile becomes an icon-plus-text cell at `min-height: 100px`.

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

The denominator is the nominal width of the selected range (5, 15, 60, 360 or 1440 minutes), not the observed span between the first and last event inside the window. A window that contains events clustered in two minutes still divides by the full range width, so the displayed rate reads low. It is a normalized window rate, not a measured inter-arrival or arrival-process rate. The UI labels the tile **Observed rate**, notes it as the *Backend selected-window aggregate*, and pairs it with the raw window event count.

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

### Static edge-weight markers

Edge stroke width (`1 + normalized * 4`), stroke opacity (`0.3 + normalized * 0.55`), curvature and static marker count are derived from the returned edge weight. Markers are positioned deterministically on the quadratic curve and do not move, so a dataset graph cannot be mistaken for ongoing traffic.

The 2D renderer caps the graph at 250 services and 2,500 edge records. It reports omitted records, keeps the accessible node/edge lists aligned with the rendered subset, and shows the strongest 18 edges by default. `Show all` expands to the full returned subset. The 3D scene renders at most 500 strongest edges from that same graph response. Reduced motion disables nonessential SVG and camera movement.

### Display modes

| Mode | Rendering |
|---|---|
| `2d` | Default flat SVG on a 760×440 viewBox. The accessible service and edge lists remain visible beneath it. |
| `3d` | Lazy-loaded Three.js/WebGL scene with perspective camera, orbit/zoom controls, hover details, selected-service focus and fit/reset camera actions. It uses the same API-derived nodes and edges as 2D. |

The 3D canvas is supplementary and lazy-loaded; if WebGL initialization or context is unavailable, the panel displays a fallback notice and the 2D view remains available. The canvas is hidden from assistive technology because the service/edge lists are the semantic view. Node size follows event volume, health reflects the existing heuristic band, and static edge geometry/markers follow observed request-trail weight. The graph is co-occurrence, not verified infrastructure or proven causality.

### Interaction and accessibility

- Nodes are laid out on a ring by node count, not by a force or graph layout.
- Nodes are keyboard-operable: `Enter` or `Space` selects, and each carries an `aria-label` with the service name, observed event count and health band.
- "Focus selected" narrows the 2D `viewBox` or eases the 3D camera toward the selected service; "Fit graph" frames the graph in 3D; "Reset view" clears selection and restores the initial camera/viewBox.
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

**Removed.** Earlier revisions documented a Load → Observe → Detect → Investigate strip linking the displayed numbers to `/ingestion`, `/analytics`, `/incidents` and `/logs`. That strip no longer exists in `OverviewPage.tsx`. It has been replaced by the **signal field** described above, which carries the same navigation intent but is anchored to real backend values rather than to narrative labels: four stages, each a link, each displaying the number it navigates away from. Playwright asserts the field directly (`the signal field reports real backend values and never invents them`).

The distinction matters. The old strip told a story with no data attached; the new field shows a number and a link. Both are navigation over already-returned data, neither is a process model of the application.

## Shared replay context

`frontend/src/replay/ReplayContext.tsx` exports `ReplayProvider` and `useReplay`. `App.tsx` wraps the router in one `ReplayProvider`, so Command Center and Dataset Replay are two views of a single subscription rather than two competing SSE streams.

- The provider fetches `GET /api/live/status` once on mount and refetches after a dataset invalidation.
- `start()` opens one `api.liveStream` subscription. A generation counter invalidates callbacks from a superseded subscription, so a stopped stream cannot update state after a restart.
- State is `idle | starting | streaming | complete | stopped | error`. A stream that closes before `replay-complete` is reported as an error rather than silently completing.
- `recentEvents` keeps the newest 300 events across batches; batches are reversed so the newest event of a batch appears first.
- `subscribeDatasetInvalidation` resets the stream and reloads status, so switching datasets cannot leave a replay pointing at the previous one.
- The Command Center card mirrors `emitted / total`, progress, the dataset name and the `demo-replay` source label, and can start, stop or restart the shared stream.

The backend side of this stream is a bounded replay of the loaded dataset, sorted by `(timestamp, id)` and emitted oldest-first. It is labelled `demo-replay` and is not real-time capture; see [API.md](API.md) and [DATASET.md](DATASET.md). These documents call the surface **Dataset Replay**; its route is `/replay` and its navigation and page label is `Dataset Replay`. It is not the `/live` route — that is the Live Monitor generated simulation.

## Route note

`/` , `/command-center` and `/overview` all render `OverviewPage`. The `/overview` alias was added so a path-shaped URL is not mistaken for a different surface; the sidebar entry registers both aliases against `/`, and the command palette resolves all three.

`/incidents`, `/incidents/:id` and the alias `/investigate/:id` all render `IncidentsPage`. The `investigate` alias is registered in the command palette alongside `/incidents`; selecting an incident from the Command Center navigates to `/incidents/{id}`.

## What this page does not do

- It does not fabricate a number. Every tile, stage and readout is a backend field; anything unresolved renders an em dash.
- It does not attribute cause. The investigation card states that the window and pattern are returned detector output.
- It does not present a healthy band as an operational verdict, and an empty investigation card states explicitly that "this is an empty result, not evidence that the system is healthy".
- It does not reconcile scopes silently. Stage 03 of the signal field is full-dataset while stages 02 and 04 are selected-window; the head row prints the dataset name and the footer legend states which values are backend-derived.
- The optional Three.js scene is a view of dataset-derived dependencies; it does not connect to an external live feed or prove the direction of causality.
