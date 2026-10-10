# LogInsight Frontend Design System

The frontend uses the **Signal Atlas** light-first system: warm porcelain and white surfaces, cobalt actions, cyan data accents, restrained violet analysis, and semantic severity colors. The optional 3D topology retains a dark scene surface for node and edge contrast. Data remains the visual priority: color, depth, and motion describe values returned by the application APIs.

## Source of truth

- `frontend/src/styles/signal-atlas.css` owns the active palette, shared visual component states, responsive overrides, motion tokens and the Presentation Mode surface.
- `frontend/src/styles/global.css` and `frontend/src/styles/product.css` remain layered beneath Signal Atlas for shared layout and legacy component structure; Signal Atlas owns the active visual tokens and route styling.
- `frontend/src/main.tsx` imports only `signal-atlas.css`; old skin files are not entry-point styles.

## Tokens and semantics

The `:root` block in `signal-atlas.css` defines porcelain surfaces, text, semantic colors, shadows and motion timing over the shared component structure. Status is not communicated by color alone: badges and graph nodes include labels or accessible descriptions.

| Meaning | Token family | Use |
| --- | --- | --- |
| Active / live / selected | `--accent*` | Primary actions, focus, active navigation |
| Healthy / resolved | `--ok*` | Healthy services and completed work |
| Warning / degraded | `--warn*` | Elevated but noncritical signals |
| Critical / error | `--danger*`, `--severity-*` | Error evidence and critical state |
| Informational | `--info*` | Dataset or runtime context |
| Algorithmic | `--purple`, `--mod-*`, `--glow-*` | DSA modules and analysis workspaces |

The background uses restrained blue/cyan illumination with opaque white data panels. Translucency is reserved for navigation and the instructor presentation backdrop; dense event content remains opaque for readability.

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
- The SVG renderer uses static, bounded markers from normalized displayed-edge weight; it does not imply ongoing traffic in a static dataset.
- The lazy Three.js/WebGL renderer uses the same nodes and edges with restrained highlights and no generated particle stream.
- The renderer caps its view at 250 services and 2,500 valid edge records, and reports when additional input records are omitted. The accessible lists match the rendered subset.
- The canvas is supplementary. The paired service and edge lists remain the accessible source for keyboard and screen-reader users.
- WebGL setup failure leaves the 2D view available. The 3D renderer supports orbit, fit, and selected-service focus.

The graph represents relationships observed in log records, not verified infrastructure or proven causality. Its source API is not scoped to the Overview's selected time window.

## Motion and lifecycle

Motion uses shared fast, normal, and slow timing tokens. Route entry, in-view section reveals, hover/focus/selection, chart and topology state changes, and Presentation Mode transitions use short, purposeful movement. Scroll reveals never hide content before intersection; reduced-motion preferences suppress decorative movement. The WebGL renderer pauses while its panel is outside the viewport or the browser tab is hidden, caps pixel ratio, observes container resize, and disposes renderer resources, geometries, materials, controls, observers, and listeners on unmount.

The guided presentation executes its evidence request through the shared API client, aborts superseded requests, exposes failures with a retry action, and supports Escape, arrow-key navigation, focus trapping and focus restoration. Nine screenshots were captured from the running app, including Command Center at 1440×900 and 390×844, plus analytics, logs, incidents, topology and guided presentation. Browser E2E also checks all six viewport targets, direct routing, search, node selection, WebGL fallback, keyboard navigation, reduced motion and presentation exit. This does not establish WCAG conformance or successful hardware-accelerated WebGL rendering.

## Product data modes and limits

The current backend provides two clearly separated data modes. **Dataset mode** analyses the loaded log dataset and supports a bounded SSE dataset replay. **Simulation mode** runs a deterministic generated microservice scenario over SSE; it is labelled as generated simulation, not as observed telemetry. The interface labels replay as replay; it does not represent it as a live production source. Patterns are heuristic normalization, and incidents are elevated-error windows. Neither implies ML confidence or confirmed root cause. There is no external telemetry integration in this frontend.

Because the two modes have different provenance, topology surfaces state their source explicitly: dataset pages render `OBSERVED DEPENDENCIES` with dataset event counts, while simulation pages render `DECLARED DEPENDENCIES` because the service graph is declared by the scenario definition rather than inferred from request identifiers. Dense operational metadata is kept at or above 10 CSS pixels on the targeted Signal Atlas surfaces, and browser checks enforce WCAG AA contrast for eleven semantic token pairs; this focused check is not a whole-application WCAG certification.

The Three.js view is lazy-loaded and the 2D SVG topology is the fallback. The production build reports a 571.82 kB minified (144.15 kB gzip) lazy 3D chunk; it is downloaded only when the 3D view is selected.

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

These are interaction goals, not a conformance certificate. Browser checks cover selected viewports and workflows but do not verify every color pair, assistive technology, or all target sizes.

| Rule | Value | Enforcement |
| --- | --- | --- |
| Minimum text size | 10 px target | Some dense technical annotations use 8–9 px; this is a known legibility tradeoff and has not been audited route-by-route for contrast. |
| Minimum target size | 24 x 24 px target | Compact inline controls and dense data rows need further touch-target auditing; responsive browser tests verify overflow, not every hit target. |
| Focus visibility | 3 px solid cobalt | Global `:focus-visible` outline; mobile navigation and command palette keyboard paths are covered, but not every route/control combination is manually audited. |
| Contrast | 4.5:1 body, 3:1 large text | Target; not yet measured across every component and route. |
| Colour independence | required | Health, severity and provenance are always carried by a label or accessible description, never by hue alone. |
| Horizontal overflow | 0 px | Playwright asserts no document overflow on Command Center and Services at all six target sizes: 1440×900, 1280×800, 1024×768, 768×1024, 390×844 and 360×800. |

### Reduced motion

`prefers-reduced-motion: reduce` collapses `page-in` and every transition to `0.01ms`, and in the WebGL scene it disables camera damping, skips the animation loop in favour of a single render, and still leaves orbit, zoom and selection fully usable.

### Historical runtime stability checks

The following browser observations are from an earlier visual revision and have not been rerun after the light-theme update. They are not current acceptance evidence:

- Five 2D-to-3D-to-2D cycles held exactly one `<canvas>` in 3D and zero in 2D, with identical DOM node counts and a flat JS heap, so no renderer or WebGL context is duplicated.
- Three start/stop cycles kept the event buffer bounded at 40 rows with no DOM or heap growth.
- Twelve route changes across four routes returned to an identical DOM node count with zero leaked canvases.
- Console errors: 0.
