# UI, Shell and Accessibility

The current frontend is a React 18 + TypeScript + Vite single-page application with a dark observability workspace shell.

## Navigation

`Layout.tsx` prioritizes four daily-use groups—Operate, Investigate, Analyze and Algorithm Lab. Supporting Algorithms, Benchmarks, Run Sessions, Datasets, Ingestion, System and Documentation links live under a collapsible More section. The header shows backend, active dataset and dataset-replay status from the API; it does not surface algorithm counts as global runtime metrics.

Aliases such as `/command-center`, `/analyze`, `/data`, `/lab`, `/algorithm-lab`, `/analysis/algorithms` and `/analysis/benchmarks` are routed to the same current pages. `/live` is the Live Monitor generated simulation, not a dataset replay and not an external production stream; the dataset replay lives at `/replay`.

## Current screens

- Overview (`/`, `/command-center`): selected-window metric strip, live simulation band, observed request-trail topology, detected investigation, pipeline story, shared replay context, timeline, service health matrix, pattern intelligence, HTTP class breakdown, window context and recent critical events. Range buttons switch the selected window.
- Logs (`/logs`, `/logs/:id`): indexed explorer, paging, filters and event detail.
- Algorithmic Search (`/search`): structured query fields, KMP free text, typeahead and Levenshtein suggestion.
- Analytics (`/analytics`): timeline, severity, heatmap, HTTP and hosts.
- Patterns (`/patterns`) and Detector Windows (`/incidents`, `/incidents/:id`, alias `/investigate/:id`): heuristic results with examples and evidence.
- Scenario Lab (`/scenario-lab`, alias `/simulation`): scenario catalogue, run controls, and the generated telemetry frames and algorithm evidence.
- Live Monitor (`/live`): the generated simulation stream — stream state, scenario, seed, speed, tick, phase, measured error rate, throughput, p95, signals, evidence, incident, event stream and declared topology.
- Incident Workbench (`/incidents/workbench`): the investigation surface — incident navigator, measured detail with lifecycle, timeline and algorithm evidence, and context with origin, affected services, blast radius, topology and health.
- Services (`/services`): fleet rollups and per-service activity.
- Dataset Replay (`/replay`): bounded SSE replay of the loaded dataset with source disclosure and progress, reading the same `ReplayProvider` state as the Command Center. Its page disclosure copy still reads "Demo replay of the loaded dataset".
- Datasets/Ingestion: demo, bundled sample and upload workflows.
- Algorithm Lab (`/analysis`, alias `/lab` and `/algorithm-lab`): catalogue, measured benchmark and recorded run sessions.
- System/Docs: runtime registry and concise API guidance.

Charts are hand-rolled SVG components. The dependency graph has a default SVG view and an on-demand Three.js/WebGL view. There is no WebSocket client and no browser-generated telemetry source.

## Algorithm Lab surfaces

The `Algorithm Lab` navigation group contains Lab overview (`/analysis`, aliased by `/lab` and `/algorithm-lab`), Algorithms (`/algorithms`), Benchmarks (`/benchmarks`) and Run Sessions (`/runs`).

- **Algorithms** reads `GET /api/analysis/algorithms` and renders the returned catalogue: per-module counts, a text filter over the returned fields, a per-row query type and complexity readout, `traceable` and `exposed` badges, and a detail card showing problem, algorithm type, time and space complexity and the canonical and trace endpoint paths. Every value on the page comes from that response; no catalogue entry is synthesized in the browser.
- **Benchmarks** is the measured execution surface: it posts a pattern to `GET /api/analysis/benchmarks/search` and renders one server-measured run per matcher over the same dataset haystack, with the winner, methodology note and haystack preview returned by the backend.
- **Run Sessions** lists recorded executions from `GET /api/runs`, opens one by id, streams its recorded step ledger over `GET /api/runs/{id}/events`, and renders those steps in `TracePlayer`. Runs are created server-side through `POST /api/runs`; the browser only renders recorded server events.

## Command Center topology rendering

`components/TopologyPanel.tsx` renders the observed service graph from `/api/analytics/dependencies`.

- **Layout**: nodes sit on a ring sized by node count. There is no force-directed or hierarchical layout.
- **Encoding**: node radius is `12 + sqrt(normalizedEvents) * 22`; the health dot color and CSS class follow the heuristic error-rate band; the label below the node shows observed events and error percentage.
- **Edges**: quadratic Bézier paths with an arrowhead marker. Stroke width, opacity and curvature all scale with `weight / maxEdgeWeight`, so the picture is a pure function of the returned data.
- **Particles**: `min(8, max(1, ceil(normalized * 7)))` circles per edge, positioned analytically on the path and staggered by a fixed per-particle offset. No randomness or time seeding, so the same payload renders identically each time. `prefers-reduced-motion: reduce` removes `animateMotion` and marks the particles `topology-particle--static`, with the CSS opacity drift animation disabled.
- **Modes**: `2D` is a flat 760×440 SVG. `3D WebGL` lazy-loads a Three.js scene with a perspective camera and OrbitControls. The 3D node positions are deterministically derived from the same service node list; node size follows dataset event volume, node color follows the existing heuristic health band and edges follow observed request-trail weight. Instanced moving markers show normalized edge intensity and are explicitly labelled illustrative rather than exact/live requests.
- **Lifecycle and fallback**: the renderer caps device pixel ratio, renders only while its panel is visible and the tab is active, pauses animation for reduced motion, observes resize and visibility changes, and disposes the animation loop, controls, geometries, materials and listeners on unmount. WebGL initialization/context failure displays a recovery message; the SVG view and accessible service/edge lists remain available.
- **Controls**: mode switch, `Focus selected`, `Fit graph` in 3D and `Reset view`. The panel is controlled or uncontrolled via `mode`/`onModeChange`, `selectedId`/`onSelect` and `defaultMode`.

The card subtitle and the SVG `<desc>` both state that node size follows observed events and edge weight follows observed request-trail adjacency, not verified infrastructure.

## Interaction details

- Search and explorer inputs support debounced typeahead where implemented and submit on Enter.
- Tables expose explicit labels and captions where applicable; chart SVGs have text alternatives.
- Event detail uses a labelled dialog, focuses its close control, supports Escape and links to a full route.
- The command palette opens with `Ctrl+K` or `Cmd+K`, traps Tab within the dialog, supports arrow navigation, Enter and Escape, and restores focus.
- Trace playback uses buttons, a labelled range control, a speed select, a ledger and keyboard shortcuts. Shortcuts are ignored while typing in form controls.
- Topology nodes are focusable and activate on `Enter` or `Space`; selecting one reveals a link to that service's page. There is no arrow-key roving focus between nodes.
- The replay page shows `demo-replay` and "not real-time" and supports stop/replay. Starting a replay from the Command Center affects the same shared stream.

## Accessibility audit

The current source provides:

- A skip link to `#main-content`.
- Semantic header, navigation, main and labelled section/card landmarks.
- `aria-current` for the active route, labelled icon buttons, live status regions, alert regions and dialog roles.
- Visible `:focus-visible` outlines and keyboard-operable controls.
- Text alternatives for SVG charts and heatmap cells.
- Topology nodes exposed as `role="button"` with `tabIndex=0`, `aria-pressed` selection and an `aria-label` carrying service name, observed event count and health band. An accessible service list and an accessible edge list mirror the SVG, since the graph is not readable from the shapes alone.
- `prefers-reduced-motion` handling in CSS and in the topology particle rendering.
- The 3D canvas is hidden from assistive technology because the visible accessible service list and edge list carry the same semantic data; pointer hover is supplementary and not required to inspect services.
- Responsive rules at 1120 px, 860 px and 680 px, including a mobile navigation drawer and stacked small-screen grids.

This is a source-level review supported by focused Layout and TopologyPanel component tests. The repository has no configured automated axe, screen-reader or browser-test command, so those broader validations are not claimed as completed in this audit.

## Data honesty in the UI

Empty dataset states link to Datasets/Ingestion rather than showing invented metrics. The Command Center labels its own scope: the metric strip names the rate "Selected-window observed rate", the timeline and window-context cards print `windowStart`–`windowEnd` and the `scope` string, and a separate card shows coverage against the unfiltered `datasetEvents` total. The header status pill reads the real `/api/health/status` value rather than the hardcoded `OverviewDto.systemStatus` compatibility string.

Patterns are labelled heuristic and not ML. Incidents expose their method and evidence, and the Command Center investigation card states that the returned window and pattern do not establish why the events occurred. Service health bands are printed with their thresholds in the matrix subtitle rather than presented as a verdict. Topology edges are labelled observed request-trail adjacency on dataset surfaces and declared dependencies on simulation surfaces, never as verified infrastructure. Benchmarks state that they are measured on the current machine. Dataset Replay is explicitly a bounded, oldest-first dataset replay and carries a "not real-time" disclosure; the Live Monitor simulation surfaces state that their traffic is generated rather than captured, and the Incident Workbench describes blast radius as a traversal over declared dependencies rather than a confirmed root cause.

See [API.md](API.md), [COMMAND_CENTER.md](COMMAND_CENTER.md), [DATASET.md](DATASET.md) and [13-testing.md](13-testing.md) for the runtime contract and verification limits.
