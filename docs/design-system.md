# LogInsight Visual System

The interface uses one dark graphite visual system for the operational workspace and the algorithm lab. Operational data stays visually primary; blur, lighting and motion are restrained and never supply metrics that are absent from the API.

## Color and surfaces

The canonical base tokens live in `frontend/src/styles/global.css`; component-level material and responsive rules live in `frontend/src/styles/experience.css`.

| Token | Meaning |
| --- | --- |
| `--bg` / `--bg-elevated` / `--bg-soft` | graphite page, shell and nested surfaces |
| `--text` / `--text-muted` / `--text-faint` | primary content, secondary context and metadata |
| `--accent` | active navigation, keyboard focus and selected state |
| `--ok` / `--warn` / `--danger` | healthy, watch/degraded and elevated/critical state |
| `--info` / `--purple` | informational and algorithmic/academic context |
| `--surface-0` … `--surface-3` | stepped glass opacity |
| `--glass-blur-1` … `--glass-blur-modal` | card, elevated surface and overlay blur levels |
| `--motion-fast` / `--motion-normal` / `--motion-slow` | 120 / 220 / 420 ms transition rhythm |

Status is paired with text and shape as well as color. Severity badges and health bands preserve their labels for users who do not distinguish color. The page background has a low contrast grid and fixed radial light; neither animates.

Glass appears on the shell, command palette, topology hero and selected cards. Dense tables remain opaque enough for readability. Every focusable control retains a visible focus ring. Mobile widths reduce the number of simultaneous columns and visual effects.

## Navigation and hierarchy

The persistent shell prioritizes Operate, Investigate, Analyze and Algorithm Lab. Supporting catalogue, benchmark, run, source, ingestion, system and documentation routes sit under a collapsible More group and remain available in the command palette. The overview leads with selected-window metrics and the observed dependency view, then the current detector result and supporting analysis.

## Topology encodings

Both renderers consume the same nodes and edges from the existing dependency API:

- A node is a service returned by observed `requestId` request-trail grouping.
- Node size represents event volume. It does not represent service importance or request rate.
- Node color and health label use the existing heuristic error-rate band.
- Edge geometry and width represent the observed adjacency relationship and its returned weight.
- By default, both renderers show the 18 strongest returned edges by weight to keep dense request-trail datasets readable. `Show all` reveals every returned edge. The accessible adjacency list always retains the full backend result.
- The SVG view is the default and draws bounded particles from normalized displayed-edge weight.
- The lazy Three.js/WebGL view positions the same displayed nodes and edges in perspective. A capped `InstancedMesh` animates small markers along observed edges; density and speed are normalized visual encodings, not exact request counts, measured latency or live external requests.
- The 3D canvas is supplementary and `aria-hidden`. The paired service and edge lists remain the accessible source for keyboard and screen-reader users.
- WebGL setup failure offers a clear fallback message; selecting 2D returns to the same graph. Orbit, fit and selected-service focus controls are provided in 3D.

The graph represents observed log relationships, not verified infrastructure and not proven causality. Its data is not scoped to the Command Center's selected time window.

## Motion and resource lifecycle

CSS state changes use short transitions. SVG particles and 3D traffic motion follow `prefers-reduced-motion`; the WebGL scene stops its animation loop when its panel is outside the viewport or the browser tab is hidden. The canvas uses a capped pixel ratio, observes container resize, and disposes renderer, controls, geometries, materials, observers and event listeners when it unmounts. There is no idle camera rotation or full-screen flashing.

## Support and limits

2D SVG remains the supported fallback for devices without WebGL. The Three.js chunk is loaded only after choosing the 3D mode. No chart framework, motion framework, external telemetry source, AI service or WebSocket client was added for this visual pass. A Chrome browser with WebGL rendered the scene successfully; component tests cover the unavailability fallback and the production build verifies the lazy bundle.
