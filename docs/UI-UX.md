# UI, Shell and Accessibility

The current frontend is a React 18.3 + TypeScript 5.6 + Vite 6 single-page application implementing the **Atmospheric Signal** design system: warm mineral surfaces, deep-ink typography, atmospheric teal accent, a layered scroll-linked light field, and a single editorial serif display face over monospaced data. The full token set, the atmosphere layers and their paint order, the breakpoints and the reduced-motion contract are in [design-system.md](design-system.md).

`signal-atlas.css` is the authoritative stylesheet. It layers `global.css` and `product.css` underneath in the named cascade layer `loginsight-structure` and is itself unlayered, so it wins the cascade without editing the inherited structural sheets.

## Shell composition

```text
skip link
.app-shell
  ├── <aside class="sidebar">    brand, workspace block, grouped nav, footer status
  └── .main-area
       ├── <header class="app-header">  breadcrumbs, status chips, command trigger, actions
       └── <main id="main-content">
            └── <motion.div class="route-view">   ← <Outlet/>
```

`AppErrorBoundary` wraps the whole of `<App />` in `main.tsx` with `scope="LogInsight"`. On a render-time failure it replaces the workspace with a `role="alert"` recovery panel — the error *message* only, no stack trace — offering **Try again** and **Reload workspace**. Without it, any render-time throw unmounts the tree and leaves a blank page with no message and no recovery path, which a reachable-but-failing backend can cause.

## Navigation

`Layout.tsx` prioritizes four daily-use groups — Operate, Investigate, Analyze and Algorithm Lab. Supporting Algorithms, Benchmarks, Run Sessions, Datasets, Ingestion, System and Documentation links live under a collapsible More section. The header shows backend, active dataset and dataset-replay status from the API; it does not surface algorithm counts as global runtime metrics.

Aliases such as `/command-center`, `/overview`, `/analyze`, `/data`, `/lab`, `/algorithm-lab`, `/analysis/algorithms` and `/analysis/benchmarks` are routed to the same current pages. `/live` is the Live Monitor generated simulation, not a dataset replay and not an external production stream; the dataset replay lives at `/replay`.

## Loading and route transitions

Three of the twenty routes stay in the entry chunk — Command Center, Logs Explorer and Incident Workbench — because they are the primary operational surfaces and a cold navigation to any of them should not pay a network round trip. The other seventeen are `lazy()`-loaded.

The `Suspense` fallback (`.route-fallback`) deliberately keeps the page title visible as a skeleton line and three skeleton tiles, marked `aria-hidden`, rather than replacing the route with a spinner. Navigating therefore never leaves the workspace without context.

`Layout` wraps the outlet in a `motion.div.route-view` keyed on `location.pathname`, so a route change replays the entrance animation while `Layout` itself never unmounts and shell state (collapsed sidebar, open palette) survives navigation. `Layout` itself never unmounts; the old motion subtree is disposed with the subtree.

## Current screens

- **Command Center** (`/`, `/command-center`, `/overview`): hero, signal field, source strip, live simulation band, window scope bar, selected-window metric strip, observed request-trail topology, detected investigation, timeline, recurring patterns and recent critical events. Range buttons switch the selected window. Detail in [COMMAND_CENTER.md](COMMAND_CENTER.md).
- **Logs** (`/logs`, `/logs/:id`): indexed explorer, paging, filters and event detail.
- **Algorithmic Search** (`/search`): structured query fields, KMP free text, typeahead and Levenshtein suggestion.
- **Analytics** (`/analytics`): timeline, severity, heatmap, HTTP and hosts.
- **Patterns** (`/patterns`) and **Incidents** (`/incidents`, `/incidents/:id`, alias `/investigate/:id`): heuristic results with examples and evidence.
- **Scenario Lab** (`/scenario-lab`, alias `/simulation`): scenario catalogue, run controls, and the generated telemetry frames and algorithm evidence.
- **Live Monitor** (`/live`): the generated simulation stream — stream state, scenario, seed, speed, tick, phase, measured error rate, throughput, p95, signals, evidence, incident, event stream and declared topology.
- **Incident Workbench** (`/incidents/workbench`): the investigation surface — incident navigator, measured detail with lifecycle, timeline and algorithm evidence, and context with origin, affected services, blast radius, topology and health.
- **Services** (`/services`, `/services/:id`): fleet rollups and per-service activity.
- **Dataset Replay** (`/replay`): bounded SSE replay of the loaded dataset with source disclosure and progress, reading the same `ReplayProvider` state as the Command Center. Its page disclosure copy still reads "Demo replay of the loaded dataset".
- **Datasets / Ingestion**: demo, bundled sample and upload workflows.
- **Algorithm Lab** (`/analysis`, alias `/lab` and `/algorithm-lab`): catalogue, measured benchmark and recorded run sessions.
- **System / Docs**: runtime registry and concise API guidance.

Charts are hand-rolled SVG components. The dependency graph has a default SVG view and an on-demand Three.js/WebGL view. There is no WebSocket client and no browser-generated telemetry source.

## Command Center topology rendering

`components/TopologyPanel.tsx` renders the observed service graph from `/api/analytics/dependencies`.

- **Layout**: nodes sit on a ring sized by node count. There is no force-directed or hierarchical layout.
- **Encoding**: node radius is `12 + sqrt(normalizedEvents) * 22`; the health dot color and CSS class follow the heuristic error-rate band; the label below the node shows observed events and error percentage.
- **Edges**: quadratic Bézier paths with an arrowhead marker. Stroke width, opacity and curvature all scale with `weight / maxEdgeWeight`, so the picture is a pure function of the returned data.
- **Static weight encoding.** Markers are positioned deterministically on the curve and do not move, so a dataset graph cannot be mistaken for ongoing traffic. Motion remains available for selection, camera transitions and graph entry, and honours `prefers-reduced-motion`.
- **Modes**: `2D` is an interactive SVG. `3D WebGL` lazy-loads a Three.js scene with a perspective camera and OrbitControls from the `vendor-three` chunk. Node positions are deterministically derived from the same service node list; node size follows dataset event volume and edges follow observed request-trail weight.
- **Lifecycle and fallback**: the renderer caps device pixel ratio, renders only while its panel is visible and the tab is active, pauses animation for reduced motion, observes resize and visibility changes, and disposes the animation loop, controls, geometries, materials and listeners on unmount. WebGL initialization/context failure displays a recovery message; the SVG view and accessible service/edge lists remain available.
- **Controls**: mode switch, `Focus selected`, `Fit graph` in 3D and `Reset view`. The panel is controlled or uncontrolled via `mode`/`onModeChange`, `selectedId`/`onSelect` and `defaultMode`.

The card subtitle and the SVG `<desc>` both state that node size follows observed events and edge weight follows observed request-trail adjacency, not verified infrastructure.

## Algorithm Lab surfaces

The `Algorithm Lab` navigation group contains Lab overview (`/analysis`, aliased by `/lab` and `/algorithm-lab`), Algorithms (`/algorithms`), Benchmarks (`/benchmarks`) and Run Sessions (`/runs`).

- **Algorithms** reads `GET /api/analysis/algorithms` and renders the returned catalogue: per-module counts, a text filter over the returned fields, a per-row query type and complexity readout, `traceable` and `exposed` badges, and a detail card showing problem, algorithm type, time and space complexity and the canonical and trace endpoint paths. Every value on the page comes from that response; no catalogue entry is synthesized in the browser.
- **Benchmarks** is the measured execution surface: it posts a pattern to `GET /api/analysis/benchmarks/search` and renders one server-measured run per matcher over the same dataset haystack, with the winner, methodology note and haystack preview returned by the backend.
- **Run Sessions** lists recorded executions from `GET /api/runs`, opens one by id, streams its recorded step ledger over `GET /api/runs/{id}/events`, and renders those steps in `TracePlayer`. Runs are created server-side through `POST /api/runs`; the browser only renders recorded server events.

## Interaction details

- Search and explorer inputs support debounced typeahead where implemented and submit on Enter.
- Tables expose explicit labels and captions where applicable; chart SVGs have text alternatives.
- Event detail uses a labelled dialog, focuses its close control, supports Escape and links to a full route.
- The command palette opens with `Ctrl+K` or `Cmd+K`, traps Tab within the dialog, supports arrow navigation, Enter and Escape, and restores focus. The input is `type="search"` with `autocomplete="off"` and `spellcheck="false"` so the browser does not decorate or autocorrect query syntax.
- Trace playback uses buttons, a labelled range control, a speed select, a ledger and keyboard shortcuts. Shortcuts are ignored while typing in form controls.
- Topology nodes are focusable and activate on `Enter` or `Space`; selecting one reveals a link to that service's page. There is no arrow-key roving focus between nodes.
- The replay page shows `demo-replay` and "not real-time" and supports stop/replay. Starting a replay from the Command Center affects the same shared stream.

## Accessibility audit

The current source provides:

- A skip link to `#main-content`.
- Semantic header, navigation, main and labelled section/card landmarks.
- `aria-current` for the active route, labelled icon buttons, live status regions, alert regions and dialog roles.
- Visible `:focus-visible` outlines (`2px solid var(--accent-strong)`, offset 2px) and keyboard-operable controls.
- Text alternatives for SVG charts and heatmap cells.
- Topology nodes exposed as `role="button"` with `tabIndex=0`, `aria-pressed` selection and an `aria-label` carrying service name, observed event count and health band. An accessible service list and an accessible edge list mirror the SVG, since the graph is not readable from the shapes alone.
- An application error boundary that produces a labelled `role="alert"` recovery surface instead of an unmounted tree.
- A scroll-reveal implementation whose hidden state is applied **from JavaScript only**, so a failed `IntersectionObserver` leaves content visible rather than blank.
- `prefers-reduced-motion` handling across page, topology, guided-demo and atmospheric layers, enforced at three independent levels (JavaScript component logic, `useReducedMotion()` guards in `Layout`, and CSS `!important` rules).
- Responsive rules at 1240 px, 1100 px, 960 px, 680 px and 400 px, including a mobile navigation drawer and stacked small-screen grids.

This is a source-level review supported by focused component tests (including 3 dedicated `AppErrorBoundary` tests) and 18 Playwright workflows. The repository has no configured automated axe, screen-reader or pixel-diff visual-regression command, so those broader validations are not claimed as completed in this audit. Contrast is verified for 20 semantic token pairs by Playwright; that is a focused check, not whole-application WCAG conformance.

## Data honesty in the UI

Empty dataset states link to Datasets/Ingestion rather than showing invented metrics. The Command Center labels its own scope: the metric strip names the rate "Observed rate", the timeline and window-context cards print `windowStart`–`windowEnd` and the `scope` string, and a separate card shows coverage against the unfiltered `datasetEvents` total. The header status pill reads the real `/api/health/status` value rather than the hardcoded `OverviewDto.systemStatus` compatibility string.

The signal field is the sharpest instance of this rule: it renders an em dash, never a `0`, whenever the backend has not returned a value, and its footer legend switches from "No telemetry is shown until a source is loaded" to "Backend-derived source and selected-window values" only once a dataset resolves. A Playwright workflow asserts this contract directly.

Patterns are labelled heuristic and not ML. Incidents expose their method and evidence, and the Command Center investigation card states that the returned window and pattern do not establish why the events occurred — an empty result is explicitly "not evidence that the system is healthy". Service health bands are printed with their thresholds rather than presented as a verdict. Topology edges are labelled observed request-trail adjacency on dataset surfaces and declared dependencies on simulation surfaces, never as verified infrastructure. Benchmarks state that they are measured on the current machine. Dataset Replay is explicitly a bounded, oldest-first dataset replay and carries a "not real-time" disclosure; the Live Monitor simulation surfaces state that their traffic is generated rather than captured, and the Incident Workbench describes blast radius as a traversal over declared dependencies rather than a confirmed root cause.

See [API.md](API.md), [COMMAND_CENTER.md](COMMAND_CENTER.md), [DATASET.md](DATASET.md), [design-system.md](design-system.md) and [13-testing.md](13-testing.md) for the runtime contract and verification limits.
