# LogInsight Frontend Design System

The frontend uses the **Signal in Motion** light-first system: porcelain and white surfaces, cobalt actions, cyan data accents, restrained indigo analysis, and semantic severity colors. The optional 3D topology retains a dark scene surface for node and edge contrast. Data remains the visual priority: color, depth, and motion describe values returned by the application APIs.

## Source of truth

- `frontend/src/styles/product.css` owns shared component compositions and responsive refinements.
- `frontend/src/styles/signal-in-motion.css` overrides shared visual tokens for the light-first identity and supplies the guided-demo surface and reduced-motion refinements.
- `frontend/src/styles/global.css` supplies semantic/base layout rules and accessible structural behavior. It consumes the tokens declared by `product.css` and does not define a second palette.
- `frontend/src/main.tsx` loads the structural rules followed by the product skin.

## Tokens and semantics

The `:root` block in `signal-in-motion.css` defines porcelain surfaces, text, semantic colors, shadows and motion timing over the shared component styles. Status is not communicated by color alone: badges and graph nodes include labels or accessible descriptions.

| Meaning | Token family | Use |
| --- | --- | --- |
| Active / live / selected | `--accent*` | Primary actions, focus, active navigation |
| Healthy / resolved | `--ok*` | Healthy services and completed work |
| Warning / degraded | `--warn*` | Elevated but noncritical signals |
| Critical / error | `--danger*`, `--severity-*` | Error evidence and critical state |
| Informational | `--info*` | Dataset or runtime context |
| Algorithmic | `--purple`, `--mod-*`, `--glow-*` | DSA modules and analysis workspaces |

The background uses restrained blue and cyan light with opaque white data panels. Translucency is reserved for navigation and the floating demo panel; dense event content remains opaque for readability.

## Navigation and page hierarchy

The sidebar keeps Overview, Scenario Lab, Live Monitor, Dataset Replay, Logs, Detector Windows, Incident Workbench, Services, Patterns, and Analytics in the primary workspace. The Algorithm Lab, algorithmic search, catalogue, benchmarks, run sessions, datasets, ingestion, system, and documentation remain available under the collapsible More group and command palette.

The Overview leads with source and selected time-window context, a backend-derived signal strip, observed service relationships, current detector output, and supporting events/patterns. Logs prioritizes a query composer and bounded result stream. Incidents pairs detector windows with the selected evidence window. Services combines the observed graph with a contextual service view.

## Topology encodings

Both renderers consume the same nodes and edges returned by the dependency API:

- A node is a service returned by observed request-trail grouping.
- Node size represents dataset event volume; it does not represent service importance or request rate.
- Node color and health label use the existing heuristic error-rate band.
- Edge geometry and width represent returned observed adjacency and weight.
- The default view shows the strongest returned edges to keep a dense graph readable. “Show all” reveals every returned edge; the accessible adjacency list retains the full API result.
- The SVG renderer draws a bounded number of particles from normalized displayed-edge weight.
- The lazy Three.js/WebGL renderer uses the same nodes and edges with capped instanced markers. Density and speed are representative encodings, not exact request counts, measured latency, or live external requests.
- The canvas is supplementary. The paired service and edge lists remain the accessible source for keyboard and screen-reader users.
- WebGL setup failure leaves the 2D view available. The 3D renderer supports orbit, fit, and selected-service focus.

The graph represents relationships observed in log records, not verified infrastructure or proven causality. Its source API is not scoped to the Overview's selected time window.

## Motion and lifecycle

Motion uses the shared fast, normal, and slow timing tokens. Reduced-motion preferences suppress decorative movement. The WebGL renderer pauses while its panel is outside the viewport or the browser tab is hidden, caps pixel ratio and particle count, observes container resize, and disposes renderer resources, geometries, materials, controls, observers, and listeners on unmount.

The guided tour keeps each underlying route interactive, executes its evidence request through the shared API client, aborts superseded requests, exposes failed requests with a retry action, and supports Escape plus arrow-key navigation. The new light-theme CSS and responsive overrides have not yet received browser visual verification in this execution environment.

## Product data modes and limits

The current backend provides two clearly separated data modes. **Dataset mode** analyses the loaded log dataset and supports a bounded SSE dataset replay. **Simulation mode** runs a deterministic generated microservice scenario over SSE; it is labelled as generated simulation, not as observed telemetry. The interface labels replay as replay; it does not represent it as a live production source. Patterns are heuristic normalization, and incidents are elevated-error windows. Neither implies ML confidence or confirmed root cause. There is no external telemetry integration in this frontend.

Because the two modes have different provenance, topology surfaces state their source explicitly: dataset pages render `OBSERVED DEPENDENCIES` with dataset event counts, while simulation pages render `DECLARED DEPENDENCIES` because the service graph is declared by the scenario definition rather than inferred from request identifiers.

The Three.js view is lazy-loaded and the 2D SVG topology is the fallback. The production build still reports a 576 kB minified lazy 3D chunk; it is downloaded only when the 3D view is selected.

## Deterministic simulation surfaces

Three surfaces read the same server stream and are documented here so their shared state is explicit:

| Surface | Route | Purpose |
| --- | --- | --- |
| Scenario Lab | `/scenario-lab` | Choose a scenario and seed, start or preview a run, inspect signals, evidence, incidents and the lifecycle. |
| Live Monitor | `/live` | Operational view of the running stream: measured error rate, p95 latency, throughput, topology health and the event buffer. |
| Incident workbench | `/incidents/workbench` | Investigate one candidate: measured evidence, blast radius, correlated events and forward-only lifecycle transitions. |
| System overview | `/` | `SimulationBand` summarises the same frame so the overview never contradicts the monitor. |

One `TelemetryProvider` owns the stream. Overview, Monitor and workbench read that single frame, so numbers cannot drift between screens. The dataset replay context is deliberately separate: `/replay` is a bounded replay of the loaded dataset and is labelled as such.

Design rules for these surfaces:

- Generated traffic is always labelled. Events and panels carry `live-simulation` or a visible `generated · not captured` note, and never share a list, chart or total with dataset-backed data.
- Phase labels are derived server-side from the measured intensity curve, so a badge cannot claim a phase the metrics contradict.
- Evidence rows show algorithm, purpose, input size, result, measured runtime and complexity. There is no confidence score, because none is computed.
- Blast radius highlighting is reachability over declared dependencies, described in the interface as a traversal and never as a probability or a confirmed root cause.
- Charts reuse `TimeChart`, stat tiles reuse `StatCard` and topology reuses the shared `TopologyPanel`, so 2D/3D switching and accessibility behaviour are identical everywhere.
- `status-strip`, `scenario-card`, `health-chip`, `signal-item`, `evidence-item`, `incident-card` and `workbench-*` extend the token set only; no new palette, radius or shadow value is introduced.
- The workbench collapses from three panes to a single column below 1180 px, and every hover or transform transition is suppressed under `prefers-reduced-motion`.

## Readability and accessibility floor

These are existing interaction rules from the previous visual audit. That browser measurement predates Signal in Motion and does not verify this theme revision; re-run browser contrast, viewport, and focus checks before claiming conformance for the new styling.

| Rule | Value | Enforcement |
| --- | --- | --- |
| Minimum text size | 10 px | The smallest values are `10px` and `0.72rem`. The root is `14px`, so `0.72rem` resolves to `10.08px`. No stylesheet declares a smaller rendered font size. |
| Minimum target size | 24 x 24 px | Dense data rows and inline links are sized to their line-height, so `.log-row-message`, `.log-row-service a`, `.log-row-context a`, `.signal-event-message`, `.signal-event-meta a`, `.text-action`, `.source-card-actions a` and `.breadcrumb-piece a` carry `min-height: 24px`. Below 680 px they grow to 32 px. |
| Focus visibility | 2 px solid `--accent-strong` | Focus rings are never removed without a replacement. Form controls use `:focus-visible`; the command palette input and topology nodes had no indicator and now do. Keyboard traversal of the shell was verified to produce a visible ring on every stop. |
| Contrast | 4.5:1 body, 3:1 large text | Target. The new palette has not yet been measured in a browser; verify every route and state before claiming conformance. |
| Colour independence | required | Health, severity and provenance are always carried by a label or accessible description, never by hue alone. |
| Horizontal overflow | 0 px | Existing responsive breakpoints remain; the six target viewports have not been rechecked for this revision. |

### Reduced motion

`prefers-reduced-motion: reduce` collapses `page-in` and every transition to `0.01ms`, and in the WebGL scene it disables camera damping, skips the animation loop in favour of a single render, and still leaves orbit, zoom and selection fully usable.

### Historical runtime stability checks

The following browser observations are from an earlier visual revision and have not been rerun after the light-theme update. They are not current acceptance evidence:

- Five 2D-to-3D-to-2D cycles held exactly one `<canvas>` in 3D and zero in 2D, with identical DOM node counts and a flat JS heap, so no renderer or WebGL context is duplicated.
- Three start/stop cycles kept the event buffer bounded at 40 rows with no DOM or heap growth.
- Twelve route changes across four routes returned to an identical DOM node count with zero leaked canvases.
- Console errors: 0.
