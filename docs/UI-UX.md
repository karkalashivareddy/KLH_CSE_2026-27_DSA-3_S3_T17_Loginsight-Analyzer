# UI, Shell and Accessibility

The current frontend is a React 18 + TypeScript + Vite single-page application with a dark observability workspace shell.

## Navigation

`Layout.tsx` groups navigation into:

- Command Center.
- Investigate: Log Explorer, Search, Patterns, Incidents and Demo Replay.
- Analyze: Analytics and Services.
- Algorithm Lab: Lab overview, Algorithms, Benchmarks and Run Sessions.
- Data: Datasets and Ingestion.
- System: System and Documentation.

Aliases such as `/command-center`, `/analyze`, `/data`, `/lab`, `/algorithm-lab`, `/algorithms` and `/benchmarks` are routed to the same current pages. The header shows backend, dataset and replay status from the API. The `Investigate` entry for the replay screen is currently labelled `Live Replay` and points at `/live`; the feature is referred to as Demo Replay throughout these documents.

## Current screens

- Command Center (`/`, `/command-center`): selected-window metric strip, observed request-trail topology, detected investigation, pipeline story, shared replay context, timeline, service health matrix, pattern intelligence, HTTP class breakdown, window context and recent critical events. Range buttons switch the selected window.
- Logs: indexed explorer, paging, filters and event detail.
- Search: structured query fields, KMP free text, typeahead and Levenshtein suggestion.
- Analytics: timeline, severity, heatmap, HTTP and hosts.
- Patterns and Incidents: heuristic results with examples/evidence. Incident evidence is reachable at `/incidents`, `/incidents/:id` and the alias `/investigate/:id`.
- Services: fleet rollups and per-service activity.
- Demo Replay (`/live`): bounded SSE replay with source disclosure and progress, reading the same `ReplayProvider` state as the Command Center. The navigation entry and page heading are labelled `Live Replay`.
- Datasets/Ingestion: demo, bundled sample and upload workflows.
- Analysis: catalogue, measured benchmark and recorded run sessions.
- System/Docs: runtime registry and concise API guidance.

Charts are hand-rolled SVG components. There is no chart library, no WebGL or 3D engine, no WebSocket client and no browser-generated data source.

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
- **Modes**: `2D` is a flat 760×440 SVG. `3D / depth` applies `rotateX(38deg) rotateZ(-5deg) scale(0.88)` to the same SVG inside a CSS `perspective: 1000px` stage, which is a 2.5D presentation rather than a 3D renderer. The panel renders a visible note — "2D SVG renderer · not WebGL" or "2.5D / SVG perspective · not WebGL" — in each mode. There is no WebGL context, shader, 3D engine or `three.js` dependency; the "3D / depth" label refers to the CSS transform.
- **Controls**: mode switch, "Focus selected" (narrows the `viewBox` around the selected node) and "Reset view" (clears selection, restores the full viewBox). The panel is controlled or uncontrolled via `mode`/`onModeChange`, `selectedId`/`onSelect` and `defaultMode`.

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
- Responsive rules at 1120 px, 860 px and 680 px, including a mobile navigation drawer and stacked small-screen grids.

This is a source-level review supported by focused Layout and TopologyPanel component tests. The repository has no configured automated axe, screen-reader or browser-test command, so those broader validations are not claimed as completed in this audit.

## Data honesty in the UI

Empty dataset states link to Datasets/Ingestion rather than showing invented metrics. The Command Center labels its own scope: the metric strip names the rate "Selected-window observed rate", the timeline and window-context cards print `windowStart`–`windowEnd` and the `scope` string, and a separate card shows coverage against the unfiltered `datasetEvents` total. The header status pill reads the real `/api/health/status` value rather than the hardcoded `OverviewDto.systemStatus` compatibility string.

Patterns are labelled heuristic and not ML. Incidents expose their method and evidence, and the Command Center investigation card states that the returned window and pattern do not establish why the events occurred. Service health bands are printed with their thresholds in the matrix subtitle rather than presented as a verdict. Topology edges are labelled observed request-trail adjacency, not verified infrastructure, in the card subtitle, the screen-reader description and the accessible edge list, and the graph is built over the whole loaded dataset rather than the selected window. Benchmarks state that they are measured on the current machine. Demo Replay is explicitly a bounded, oldest-first dataset replay; the `Live Replay` navigation and page label is a navigation label rather than a claim of real-time capture.

See [API.md](API.md), [COMMAND_CENTER.md](COMMAND_CENTER.md), [DATASET.md](DATASET.md) and [13-testing.md](13-testing.md) for the runtime contract and verification limits.
