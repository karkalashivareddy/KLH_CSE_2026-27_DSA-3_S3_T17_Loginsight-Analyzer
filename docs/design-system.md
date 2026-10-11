# LogInsight Frontend Design System â€” Atmospheric Signal

The frontend implements **Atmospheric Signal**: warm mineral surfaces, deep-ink typography, a single atmospheric teal accent, an editorial serif display face, and a layered light field that reads as weather rather than as decoration. Data remains the visual priority â€” color, depth and motion describe values returned by the application APIs, and nothing else.

This document describes the system as it exists in `frontend/src/styles/signal-atlas.css`. Every token name and value below is quoted from that file.

## Source of truth and the layering model

```css
/* frontend/src/styles/signal-atlas.css */
@import './global.css'  layer(loginsight-structure);
@import './product.css' layer(loginsight-structure);
/* signal-atlas.css itself is deliberately UNLAYERED */
```

Three files, one cascade:

| File | Role | Cascade position |
|---|---|---|
| `signal-atlas.css` | **Authoritative.** Tokens, atmosphere, base, shell, primitives, Command Center, data surfaces, states, motion, responsive. 1,312 lines. | Unlayered â€” **wins over everything** |
| `global.css` | Structural scaffolding: layout, landmarks, component skeletons. | Named layer `loginsight-structure` |
| `product.css` | Product-page component structure. | Named layer `loginsight-structure` |

Unlayered declarations outrank every named layer regardless of source order. So `signal-atlas.css` overrides the two inherited sheets **without editing them**, while those sheets keep supplying layout every route depends on. This is why many legacy selectors remain visible in `global.css` and `product.css` while being inactive at runtime.

`frontend/src/main.tsx` imports only `signal-atlas.css`. The entry stylesheet is the single source of truth.

**`signal-in-motion.css` was removed.** A repository-wide grep found no importer, so the retired visual revision and its last copy of the old cobalt accent were deleted rather than left in the tree.

## Surfaces

Warm mineral surfaces, low chroma, paper-like, chosen for long log reads.

| Token | Value | Use |
|---|---|---|
| `--bg` | `#edeae2` | Base page wash |
| `--bg-elevated` | `#fbfaf6` | Elevated background |
| `--bg-soft` | `#e5e1d7` | Recessed background |
| `--bg-hover` | `#e4e7e2` | Hover state |
| `--bg-active` | `#dde7e5` | Active/pressed state |
| `--surface-glass` | `rgba(251, 250, 246, .93)` | Translucent navigation and overlays |
| `--surface-0` | `#fbfaf6` | Card surface; the reference background for every contrast assertion |
| `--surface-1` | `#f5f2ea` | Secondary surface, dashed empty states |
| `--surface-2` | `#ece8de` | Skeleton gradient track |
| `--surface-3` | `#e0dbcd` | Deepest surface |
| `--surface-inset` | `#f2efe6` | Inset wells |
| `--surface-ink` | `#16211f` | Inked surface |
| `--surface-ink-2` | `#1e2c29` | Secondary ink surface |

## Borders

| Token | Value |
|---|---|
| `--border` | `#dbd5c7` |
| `--border-strong` | `#bdb5a3` |
| `--border-hairline` | `rgba(22, 33, 31, .09)` |

## Typography

Deep ink. Every text value clears WCAG AA on `--surface-0`.

| Token | Value | Use |
|---|---|---|
| `--text` | `#16211f` | Body |
| `--text-soft` | `#333f3b` | Secondary body |
| `--text-muted` | `#55625c` | Labels, metadata |
| `--text-faint` | `#66736d` | De-emphasised; still AA |
| `--text-disabled` | `#8d968f` | Disabled only |
| `--text-on-ink` | `#eef2ee` | Text on ink surfaces |

### Type scale

| Token | Value | Token | Value |
|---|---|---|---|
| `--text-2xs` | `10px` | `--text-xl` | `18px` |
| `--text-xs` | `11px` | `--text-2xl` | `22px` |
| `--text-sm` | `12px` | `--text-3xl` | `30px` |
| `--text-md` | `13px` | | |
| `--text-lg` | `15px` | | |

Base: `font-size: var(--text-md)`, `line-height: 1.55`, with `font-synthesis: none`, `text-rendering: optimizeLegibility` and `-webkit-font-smoothing: antialiased`.

### Families

| Token | Stack | Role |
|---|---|---|
| `--font-display` | `'Iowan Old Style', 'Palatino Linotype', Palatino, 'Book Antiqua', 'Source Serif 4', Georgia, 'Times New Roman', serif` | **Editorial serif** for display: `.page-title`, hero `h2`, `.crash-card h1`, guided-demo headings |
| `--font-ui` | `'Inter', 'Segoe UI', ui-sans-serif, system-ui, â€¦` | Interface and body text |
| `--mono` | `'Cascadia Code', 'SFMono-Regular', Consolas, 'Liberation Mono', 'Courier New', monospace` | **All data.** `code`, `pre`, `.mono`, `.ts`, and `font-variant-numeric: tabular-nums` on every numeric readout |

The serif/mono split is the system's central typographic idea: editorial voice for what the product *is*, monospace for what it *measured*.

## Accent and semantic color

Atmospheric teal is the single accent. Cyan is reserved for live signal.

| Token | Value | Use |
|---|---|---|
| `--accent` | `#0f6e7a` | Active / live / selected |
| `--accent-strong` | `#0a5560` | Primary actions, links, eyebrow labels, focus outline |
| `--accent-bright` | `#17a2ae` | Signal highlight, active nav rail, pulse stroke |
| `--accent-dim` | `#dcebec` | Tinted fill behind an accent element |
| `--accent-border` | `#a4c6c9` | Accent-tinted border |
| `--accent-glow` | `rgba(23, 162, 174, .16)` | Signal pulse glow |

| Meaning | Token | Value |
|---|---|---|
| Healthy / resolved | `--ok` / `--ok-dim` | `#0e6e4e` / `#dceee5` |
| Warning / degraded | `--warn` / `--warn-dim` | `#8a5a08` / `#f7e9cd` |
| Critical / error | `--danger` / `--danger-dim` | `#a62b3e` / `#f7dfe2` |
| Informational | `--info` / `--info-dim` | `#1f5e7e` / `#ddeaf1` |
| Algorithmic / provenance | `--purple` | `#6b4ba8` |

Severity scale, used by badges and log rows: `--severity-fatal: #8e1b2e`, `--severity-error: #a62b3e`, `--severity-warn: #8a5a08`, `--severity-info: #1f5e7e`, `--severity-debug: #4c5a55`, `--severity-trace: #6b4ba8`, `--severity-unknown: #6a7873`.

Algorithm-module families, each with a matching `--glow-*` at `.12` alpha: `--mod-strings: #0f6e7a`, `--mod-dp: #6b4ba8`, `--mod-flow: #8a5a08`, `--mod-approx: #0e6e4e`, `--mod-random: #a03462`, `--mod-parallel: #2f5aa8`.

Status is never communicated by hue alone: badges, topology nodes and health bands always carry a label or an accessible description.

## Geometry and elevation

| Token | Value |
|---|---|
| `--sidebar-width` / `--sidebar-collapsed` | `252px` / `72px` |
| `--header-height` | `62px` |
| `--shell-max` | `1760px` |
| `--space-1` â€¦ `--space-9` | `4, 8, 12, 16, 20, 24, 32, 40, 48px` |
| `--radius-xs` / `-sm` / `--radius` / `-card` / `-hero` | `5px` / `8px` / `12px` / `14px` / `20px` |
| `--shadow-panel` | `0 1px 1px rgba(31,44,39,.035), 0 4px 14px rgba(31,44,39,.05)` |
| `--shadow-raised` | `0 2px 4px rgba(31,44,39,.05), 0 12px 30px rgba(31,44,39,.075)` |
| `--shadow-float` | `0 30px 90px rgba(21,33,30,.26), 0 4px 14px rgba(21,33,30,.1)` |
| `--focus-ring` | `0 0 0 2px var(--surface-0), 0 0 0 5px var(--accent-bright)` |

`:focus-visible` is `outline: 2px solid var(--accent-strong)` with `outline-offset: 2px`. **This corrects an earlier claim:** the previous revision documented the focus ring as "3 px solid cobalt". It is now 2 px atmospheric teal.

## Motion tokens

One curve, three speeds, used by every animated surface.

| CSS token | Value | JS counterpart in `src/motion/motion.ts` |
|---|---|---|
| `--motion-instant` | `120ms` | â€” |
| `--motion-fast` | `160ms` | `MOTION.fast` = `0.16` |
| `--motion-normal` | `260ms` | `MOTION.normal` = `0.26` |
| `--motion-slow` | `420ms` | `MOTION.slow` = `0.42` |
| `--motion-scene` | `620ms` | `MOTION.scene` = `0.62` |
| `--ease-out` | `cubic-bezier(.22, .78, .28, 1)` | `EASE.out` = `[0.22, 0.78, 0.28, 1]` |
| `--ease-in-out` | `cubic-bezier(.6, .02, .3, 1)` | `EASE.inOut` = `[0.6, 0.02, 0.3, 1]` |
| `--ease-drift` | `linear` | â€” |

The JavaScript values mirror the CSS custom properties exactly, so a timing change is made once per medium rather than per component. This is the defining property of the motion layer.

CSS keyframes: `atlas-stage-in`, `atlas-page-in`, `atlas-section-in`, `atlas-value-in`, `atlas-pulse-travel`, `atlas-spin`, `tour-orbit`. Everything under this list is decorative and carries no information on its own.

Scroll reveals use a two-class contract: `Layout` adds `.reveal-section` (opacity 0, `translateY(22px)`) from an `IntersectionObserver`, then `.is-revealed` on entry. Because the hidden state only ever arrives via JavaScript, a failed or unavailable observer leaves content **visible**.

## The backdrop

A decorative, scroll-linked light field was built and then removed during review: it competed with dense log
data for attention and added motion that carried no information. The remaining backdrop is deliberately
inert - a faint lattice plus film grain painted on html/ody, neither of which animates.


## Composition

The hero, the signal field and the source strip form the Command Center's opening composition.

| Element | Notes |
|---|---|
| `.cc-hero` | Two-column grid, `min-height: 340px`, `border-radius: var(--radius-hero)`, `--shadow-raised`, with a 2px spectrum hairline along the base (`:after`) that reads as a pressure gradient |
| `.signal-field` | An isobar chart â€” four concentric `<ellipse>` isobars, a node halo and a travelling `.pulse` path â€” with four `.signal-stage` links carrying real backend values |
| `.cc-source` | The runtime source strip; `.source-pulse` is `--ok`, or `--warn` with `source-pulse--pending` while a request is in flight |

`.overview-window-bar` is a sticky, blurred, thin bar that always states what the numbers describe.

## Responsive breakpoints

The breakpoints actually present in `signal-atlas.css`:

| Breakpoint | What changes |
|---|---|
| `1240px` | Overview grid rebalances to `1.45fr / .8fr`; topology surface min-heights reduce |
| `1100px` | Header status chip label hides; hero becomes `240px / 1fr` at `min-height: 330px`; topology stage shortens |
| `960px` | Hero collapses to one column; overview / service grids go single-column; metrics become 2Ã—2; `source-empty-state` reflows |
| `680px` | Header compacts, status group hides, command trigger label hides; signal field becomes 2Ã—2; window bar goes static and full-width; metric tiles compress; topology stage is `284px`; guided demo becomes `min(78dvh, 640px)` and single-column |
| `400px` | Signal field stages collapse to a single column; tighter padding; page-action buttons compact |

The layered `global.css` and `product.css` additionally carry structural rules at 1120, 860, 960, 760, 680, 1150, 1180, 1000 and 640 px. Where the two overlap, the unlayered `signal-atlas.css` wins.

**Correction to an earlier claim.** Previous revisions of this document stated "responsive rules at 1120 px, 860 px and 680 px". Those are the *inherited* structural breakpoints; the authoritative system uses 1240 / 1100 / 960 / 680 / 400.

## Reduced motion

`prefers-reduced-motion: reduce` in `signal-atlas.css`:

- `html { scroll-behavior: auto }`.
- Every animation and transition collapses to `0.01ms` with a single iteration and zero delay, via `!important`.
- `opacity: 1` and `transform: none` are forced on `.route-view` and its children, `.overview-metric strong`, `.guided-demo`, `.signal-stage`, `.cc-hero h2`, `.cc-hero__lede`, `.cc-hero__actions`, `.cc-source`, `.signal-field`, `.pulse` and both tour orbits.
- `.reveal-section` is forced visible â€” **scroll-driven reveals must never hide content for reduced-motion users**.

The defence is layered: JavaScript checks `useReducedMotion()` and skips the observer entirely, and CSS is the final backstop. A user with the preference set gets a static, fully visible workspace.

## States

| State | Treatment |
|---|---|
| Loading | `.skeleton*` shimmer on a `--surface-2 â†’ --surface-1 â†’ --surface-2` gradient track; `.route-fallback` for a lazily loaded route keeps the page title visible and is `aria-hidden` |
| Empty | `.empty-state` centered, max 58ch. `.investigation-empty` uses a dashed `--border-strong` border so an empty investigation explains itself rather than floating adrift. **Empty is never rendered as `0`.** |
| Error | `.error-box` on `--danger-dim` with a `#dfaeb5` border and a mono `.error-meta` line |
| Source absent | `.source-empty-state` is a full hero-width panel with a real next step, not a small inline message |
| Budget exceeded | `.topology-budget-note` on `--warn-dim`, so omitted data is reported rather than silently dropped |
| Crash | `.crash-screen` / `.crash-card` â€” `role="alert"`, the error *message* only, and Try again / Reload workspace actions |

## Topology encodings

Both renderers consume the same nodes and edges returned by the dependency API:

- A node is a service returned by observed request-trail grouping.
- Node size represents dataset event volume. It does **not** represent service importance or request rate.
- Node color and health label use the heuristic error-rate band (`healthy` < 5%, `watch` 5â€“<10%, `elevated` â‰¥ 10%, `unknown` when non-finite).
- Edge geometry and width represent returned observed adjacency and weight.
- The default view shows the strongest returned edges; "Show all" reveals every returned edge. The accessible adjacency list always retains the full API result.
- The SVG renderer uses **static, bounded markers** from normalized displayed-edge weight, so a static dataset graph cannot be mistaken for ongoing traffic.
- The lazy Three.js/WebGL renderer uses the same nodes and edges with restrained highlights and no generated particle stream.
- Render caps: 250 services and 2,500 valid edge records, with omitted records reported. The 3D scene renders at most 500 strongest edges.
- The canvas is supplementary. The paired service and edge lists are the accessible source for keyboard and screen-reader users.
- WebGL setup failure leaves the 2D view available.

The graph represents relationships observed in log records â€” not verified infrastructure, and not proven causality. Its source API is not scoped to the Command Center's selected time window.

## Product data modes and limits

Two clearly separated data modes. **Dataset mode** analyses the loaded log dataset and supports a bounded SSE dataset replay. **Simulation mode** runs a deterministic generated microservice scenario over SSE, labelled as generated simulation, never as observed telemetry.

Because the modes have different provenance, topology surfaces state their source explicitly: dataset pages render `OBSERVED DEPENDENCIES` with dataset event counts; simulation pages render `DECLARED DEPENDENCIES`, because that graph is declared by the scenario definition rather than inferred from request identifiers.

Patterns are heuristic normalization, not ML. Incidents are elevated-error windows with no confidence score, because none is computed. Blast radius is reachability over declared dependencies, never a confirmed root cause.

## Readability and accessibility floor

These are interaction goals, not a conformance certificate.

| Rule | Value | Enforcement |
|---|---|---|
| Minimum text size | `--text-2xs` = 10px floor | Some dense annotations still sit at 9px (`.signal-stage__index`, `.signal-stage small`); a known tradeoff, not audited route by route. |
| Minimum target size | 24 Ã— 24px target | Compact inline controls and dense data rows need further touch-target auditing; Playwright verifies overflow, not every hit target. |
| Focus visibility | `2px solid var(--accent-strong)`, plus `--focus-ring` on `.signal-stage` | Global `:focus-visible`; command palette and mobile navigation keyboard paths are covered. |
| Contrast | 4.5:1 body | Enforced in Playwright for **20 token pairs** â€” the four text tokens, `--accent` and `--accent-strong`, all four status/dim pairs, `--purple`, four severity colors and all six algorithm-module colors â€” each asserted â‰¥ 4.5 against its intended surface. This is a focused check, not a whole-application certification. |
| Colour independence | required | Health, severity and provenance are always carried by a label or accessible description, never by hue alone. |
| Horizontal overflow | 0px | Playwright asserts `documentElement.scrollWidth â‰¤ viewport + 1` on Command Center and the Service Map at 1440Ã—900, 1280Ã—800, 1024Ã—768, 768Ã—1024, 390Ã—844 and 360Ã—800. |

## Verification and capture

`frontend/scripts/capture-visuals.mjs` captures **19 screenshots** into `docs/images/signal-atlas/`, every one taken against the running application with a real dataset loaded through the API â€” including the no-dataset state, a forced WebGL failure and a reduced-motion run.

Browser E2E holds **18 workflows** covering direct routing, semantic token contrast, scroll reveal and reduced motion, dataset search and event details, topology selection and forced WebGL fallback, guided-presentation evidence and exit, mobile navigation, the signal field's no-fabrication contract, the empty-dataset state, a failing backend, command-palette focus restoration, documented route aliases and the not-found route, plus the six viewport targets.

No full axe scan, no screen-reader audit and no pixel-diff visual-regression suite are configured. Successful hardware-accelerated WebGL rendering has not been verified â€” the fallback path has been.

## Bundle impact

The design system's cost is one 191.81 kB stylesheet (35.61 kB gzip), served as `index.css`. See [ARCHITECTURE.md](ARCHITECTURE.md) for the code-splitting and vendor-chunking model.
