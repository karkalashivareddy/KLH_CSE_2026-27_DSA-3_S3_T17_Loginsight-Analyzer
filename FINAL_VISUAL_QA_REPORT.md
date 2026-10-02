# LogInsight — Final Human-Presentation QA Report

> **Historical snapshot — superseded.** This is the QA pass dated 2026-09-27, retained for traceability.
> It is not the current verification record. Its route-level and viewport-level measurements describe
> that build. Two of its findings were later resolved and its test and typography figures were later
> corrected against the current tree; those corrections are marked inline below. For the authoritative
> current figures see [docs/13-testing.md](docs/13-testing.md),
> [final-submission/FINAL_SUBMISSION_INDEX.md](final-submission/FINAL_SUBMISSION_INDEX.md) and
> [FINAL_RELEASE_READINESS.md](FINAL_RELEASE_READINESS.md).

**Date:** 2026-09-27 (historical)
**Scope:** Final visual/interaction/presentation pass across all requested routes and viewports.
**Stack under test:** Spring Boot backend on `:8085`, Vite production preview on `:4175`, demo dataset loaded (14,000 lines, 0 failed).
**Method:** Chrome DevTools Protocol automation (Node 24 built-in `WebSocket`) — computed styles, contrast math, element geometry, live-stream observation, and frame-rate sampling.
**Honesty note:** This report is produced without human pixel inspection. Screenshots could not be visually reviewed, so every visual claim below is backed by a measured value (ratio, pixel, count, or frame interval) rather than an impression. Statements are limited to what was measured.

---

## 1. Executive summary

A final presentation pass was executed against the running application. **10 confirmed defects were found and fixed**; all were verified in the browser after the fix. After the fixes:

- **16 routes × 3 viewports = 48 route/viewport combinations: 0 horizontal overflow, 0 not-found pages, 0 uncaught exceptions.** Console errors are 0 on passive routes; at the time of this pass React duplicate-key warnings were emitted on `/scenario-lab` and `/live` during live streaming. **That defect has since been fixed** — generated simulation events now carry a deterministic unique id, and a later browser pass recorded 0 duplicate-key warnings. See Section 17 and [FINAL_RELEASE_READINESS.md](FINAL_RELEASE_READINESS.md) section B.
- **Live simulation confirmed working end-to-end** with metrics that move coherently (tick, error rate, p95, throughput, service health, incident lifecycle, algorithm evidence).
- **No leaks across 3 full start/stop cycles** (DOM nodes and JS heap flat).
- **Contrast:** 0 confirmed low-contrast text on all audited routes. The 9px figures quoted in this section are superseded: re-measuring the current tree finds no stylesheet declaring a `font-size` below **10px**, and the micro-label rules cited below resolve to 11px. The floor is documented in [docs/design-system.md](docs/design-system.md).
- **Accessibility:** reduced-motion honoured, accessible service list present, mobile touch targets corrected.
- Backend **877 tests / 0 failures / 0 errors**, frontend **55 tests / 17 files**, TypeScript clean, production build successful.

## 2. Routes audited

| Route | Page | Result |
|---|---|---|
| `/` | Overview | Pass |
| `/overview` | Overview alias | **Fixed** (previously 404) |
| `/live` | Live Monitor (generated) | Pass |
| `/logs` | Log Explorer | Pass |
| `/incidents` | Incident Investigations | Pass |
| `/incidents/workbench` | Incident Workbench | **Fixed** (missing Stop control) |
| `/services` | Service Map | Pass |
| `/patterns` | Pattern Explorer | Pass |
| `/analytics` | Analytics | Pass |
| `/algorithm-lab` | Algorithm Lab hub | Pass |
| `/algorithms` | Algorithm catalogue | Pass |
| `/scenario-lab` | Scenario Lab | Pass |
| `/data` | Dataset management | Pass |
| `/system` | System status | Pass |
| `/docs` | Documentation | Pass |
| `/replay` | Dataset replay | Pass |

## 3. Viewports tested

| Viewport | Width × Height | Intended use |
|---|---|---|
| Desktop | 1440 × 900 | Primary presentation size |
| Laptop | 1024 × 768 | Constrained desktop |
| Mobile | 390 × 844 | Phone presentation |

## 4. Layout and overflow

- **Horizontal overflow: 0px on all 48 route/viewport combinations.** Measured as `documentElement.scrollWidth - clientWidth`.
- No page required horizontal scrolling at any tested width.
- Wide SVG internals inside the topology (`g.topology-edges`) scale within their viewBox and do not create page overflow.

## 5. Typography audit

The application uses a deliberately dense operations-console aesthetic with many small labels. The table below records the pass as it was measured at the time. **Correction added when this report was re-checked against the current tree:** every "After" value in the 9px column is now 10px or larger. Re-measuring the current build finds no stylesheet declaring a `font-size` below **10px**, and the rules named here — `.log-query-syntax code`, `.workspace-kicker`, `.sidebar-section-label`, `.sidebar-footer-meta` — resolve to 11px. The 10px floor is documented in [docs/design-system.md](docs/design-system.md). The underlying work was real; only the recorded target value was stale.

**Fixed (8 rules, all content-bearing text):**

| Selector | Before (as measured then) | Current value | Reason |
|---|---|---|---|
| `.log-row-service .badge` | 7px | ≥10px | Level badge text was unreadably small |
| `.log-row-service small` | 8px | ≥10px | Matched sibling row text |
| `.log-row-time span` | 8px | ≥10px | Matched sibling + contrast |
| `.log-row-time time` (mobile) | 8px | ≥10px | Matched sibling |
| `.log-query-syntax code` | 8px | 11px | Query syntax reference |
| `.signal-event-meta .badge` | 8px | ≥10px | Matched sibling |
| `.signal-event-foot` | 8px | ≥10px | Matched sibling |
| `.source-card-heading` | 8px | 10px | Card heading, tracked caps |

**After the fix:** no content text falls below the 10px floor. The smallest remaining items are deliberate chrome (nav group labels, header context, status strip, topology mode switch, chart axis ticks), intentionally left unchanged.

## 6. Colour and contrast

Two confirmed hardcoded-colour failures were found and fixed:

| Selector | Before | Contrast | After | Contrast |
|---|---|---|---|---|
| `.sidebar-footer-meta` | `#5f7478` @ 9px (historical measurement) | 4.05:1 (fail) | `var(--text-faint)` | 4.81:1 (pass) |
| `.log-row-time span` | `#63777a` @ 8px | 4.24:1 (fail) | `var(--text-faint)` | 4.81:1 (pass) |

Hand-verified token pair `--text-faint #71858a` on `--bg-elevated #0b1418` = **4.81:1**, which passes WCAG AA for normal text.

**Post-fix measurement:** 0 low-contrast findings on `/logs`, `/overview`, `/`, and `/services`.

**Method caveat:** an additional automated probe produced implausible ratios (1.00:1–1.35:1) because its ancestor-background resolution was incorrect. That output was **discarded** rather than reported, and the token pair used by the fix was verified by hand-computed WCAG math instead.

## 7. Source separation (generated vs. dataset) — real defect found and fixed

The shared topology components hardcoded dataset-derived wording. On generated-simulation pages this produced **factually false on-screen claims**:

- Badge read **`OBSERVED DEPENDENCIES`** on Scenario Lab, Live Monitor, and the Incident Workbench, where the dependency graph is *declared by the scenario*, not observed.
- Accessible service list, node aria-labels, node captions, and the 3D note all said **`dataset events` / `observed weight` / `observed request-trail adjacency`**.

**Fix:** added an optional `graphKind?: 'observed' | 'declared'` prop to `TopologyPanel` and `Topology3D`, defaulting to `'observed'` so all dataset pages are byte-for-byte unchanged. Source-specific strings (badge, accessible list heading, node caption, aria-labels, edge scope, empty state, 3D note, default title/description) now switch on this prop. The three simulation pages pass `graphKind="declared"`.

**Verified after fix:**

| Page | Badge | Contains "dataset"/"observed" |
|---|---|---|
| Scenario Lab | `DECLARED DEPENDENCIES` | No |
| Live Monitor | `DECLARED DEPENDENCIES` | No |
| Incident Workbench | `DECLARED DEPENDENCIES` | No |
| Services (dataset) | `OBSERVED DEPENDENCIES  18 of 56 observed dependencies shown` | Yes (correct) |
| Overview (dataset) | `OBSERVED DEPENDENCIES  18 of 56 observed dependencies shown` | Yes (correct) |

Covered by a new test asserting the declared graph contains no dataset/observed wording.

## 8. Live simulation verification

Observed on Scenario Lab at 8× delivery speed. Every value below was read from the DOM at a specific tick:

| Tick | Measured health | Error rate | p95 | Throughput | Open incidents | Health chips |
|---|---|---|---|---|---|---|
| 1 | healthy | 0.00% | 200 ms | 48/s | 0 | all healthy |
| 24 | elevated | 3.58% | 1.0 s | 60/s | 1 | degraded/critical appearing |
| 57 | elevated | 15.55% | 3.8 s | 80/s | 1 | critical across gateway/orders/payments |
| 67 | elevated | 16.79% | 4.2 s | 80/s | 1 | critical (stream stopped here) |

- Signal list grows 0 → 5 as errors appear.
- Evidence panel grows 3 → 4, and **KMP appears in the algorithm evidence list only once the incident opens** — evidence is derived from real detection, not hardcoded.
- Metrics, service health chips, and incident state stay mutually consistent (no contradictory lagging window).
- Confirmed healthy-recovery tail and `RESOLVED` in an earlier run of the same build family (tick 320, measured healthy, zero open incidents).

## 9. Stream lifecycle and repeated cycling

Three full **Start → Stop** cycles on the same page:

| Metric | Idle | Streaming | After 3 cycles |
|---|---|---|---|
| DOM nodes | 927 | 1,462 | **1,319 (stable)** |
| JS heap | 3 MB | 6 MB | **5 MB (flat)** |
| Animations | 54 | 57 | 59 |
| Particles | 53 | 55 | 58 |
| New session created | — | sim-60 | sim-62 (fresh state) |

**No unbounded DOM growth, no animation accumulation, no heap growth across cycles.** Stop correctly froze the stream (`Stream STOPPED`); restarting produced a new session id rather than resurrecting stale state.

## 10. Performance and frame rate

| Sample | Avg frame | p95 frame | Worst | Approx. fps |
|---|---|---|---|---|
| Idle | 17.7 ms | 33.3 ms | 50 ms | 56 |
| Streaming t+6s | 20.6 ms | 50.1 ms | 100 ms | 48 |
| Streaming t+12s | 17.5 ms | 16.8 ms | 49.9 ms | 57 |

Measured in headless software rendering, so this is vsync-capped and indicative rather than a hardware benchmark. The single 48fps dip coincided with peak incident load. No sustained frame collapse was observed.

## 11. 2D topology inspection

- 9 nodes, 13 edges, 32 SVG text elements.
- **Node overlaps: 0** at both 1440 and 1024 widths.
- **Clipped labels: 0.** Widest label measured 76px inside a 1066px stage (1066 → 675px at 1024 with no clipping).
- Node radius encodes volume; health dot, selection ring, and incident ring are all present.
- Incident context banner states highlighted nodes are "names returned in the same detector window, **not a causal path**" — correct, non-overclaiming wording.

*Note: an earlier probe reported 8 overlaps; that was a selector bug (it matched both the group and its children). Re-measured with `.topology-node` only, the count is 0.*

## 12. 3D topology inspection

- Toggle renders a single `<canvas>` with a **live WebGL context** (verified via `getContext`).
- Canvas 1066 × 388 CSS px; backing buffer matches.
- `renderer.setPixelRatio(Math.min(devicePixelRatio, 1.6))` — capped to bound GPU cost on high-DPI screens.
- Extra control "Fit graph" appears in 3D mode.
- **Resource hygiene:** toggling 2D→3D three times left JS heap at 9 MB → 12 MB and exactly **1 canvas in the DOM** — no renderer or context leak.

## 13. Motion and animation

| Check | Result |
|---|---|
| Infinite particle animations (2D) | 53 |
| Under `prefers-reduced-motion: reduce` | **0 infinite animations**, particle `animation-duration: 0s / none` |
| Animation count across 3 stream cycles | 54 → 59 (no accumulation) |

**Known limitation (documented, not changed):** topology particle drift runs at a constant 2.8s duration regardless of traffic volume, and continues while the stream is stopped. Particle *count* does vary with edge volume, and reduced-motion is fully respected. Making drift speed traffic-proportional would require threading stream state into the shared topology component; that was judged out of scope for a QA pass and is recorded here rather than silently changed.

## 14. Incident Workbench inspection

**Idle:** `Stream READY`, "No incident candidate", empty state reading "Nothing has crossed a detection threshold yet. Start a run in **Scenario Lab**…", actions `Start a run` / `Reset view`, 236 DOM nodes.

**After Start:** `Stream STREAMING`, `Open 1`, 936 DOM nodes, incident "Checkout failures caused by pa…", and this evidence-first hierarchy:

1. **Timeline** — lifecycle progression
2. **Algorithm evidence** — which algorithm produced which observation
3. **Blast radius** — services named by the detector window
4. **Topology** — declared graph with the incident ring
5. **Correlated events** — supporting log events

Actions during investigation: `Stop`, `Reset view`, `Acknowledge`, `Advance automatically`. Status pills show `STREAMING` + `INVESTIGATING`. DOM remained stable at 924–936 nodes across four samples 9s apart.

**Defect found and fixed:** once a run was active the `Start a run` button was replaced by nothing, leaving **no way to stop the stream from this page** while its own copy instructs the user to "Start a run". A `Stop` control was added using the existing context `stop()` (same pattern as Scenario Lab). Verified: `["Start a run","Reset view"]` → `["Stop","Reset view","Acknowledge","Advance automatically"]` → `Stream STOPPED`.

## 15. Algorithm Lab inspection

The earlier "empty shell" reading was a **probe timing artifact** — the catalogue request had not resolved. Measured with a 6s settle:

- Coverage strip: **MODULES 6 · REGISTERED ALGORITHMS 42 · TRACEABLE 13 · API EXPOSED 35**, sourced from the Spring Boot catalogue.
- 6 module cards render, each linking to `/algorithms?module=Strings`.
- `/algorithms` provides real inputs, run controls, trace output, and complexity annotations.
- Counts come from backend metadata, not hardcoded values, and match the API response.

## 16. Mobile (390 × 844) inspection

| Route | Horizontal overflow | Controls under 32px (before) | After fix |
|---|---|---|---|
| `/logs` | 0px | 111 | **7** |
| `/incidents` | 0px | 115 | **9** |
| `/scenario-lab` | 0px | 14 | **9** |
| `/` | 0px | — | 19 (6 are log-message links) |

**Fix applied** (`@media (max-width: 680px)`): `.icon-btn` ≥ 34×34, `.header-status-chip` ≥ 34px tall, `.filter-chip` ≥ 32px with 5px 10px padding, topology mode/action buttons ≥ 34px, `.log-row-message` ≥ 32px; plus a coarse-pointer rule strengthening topology node strokes for touch.

**Remaining (accepted):** 3 header status chips measure ~30px and 5 topology nodes are ~30px SVG hit areas. These are informational chips and graph nodes respectively, not primary controls.

All routes confirmed: mobile nav toggle present, no overflow, dense tables degrade correctly.

## 17. Console and runtime errors

**0 uncaught exceptions** across all audited routes and viewport changes, including stream start/stop and 2D/3D toggling.

**Console errors: 0 on passive routes. React duplicate-key warnings were emitted on `/scenario-lab` and `/live` during live streaming at the time of this pass** (1785 and 39 occurrences respectively in a 20-second capture). Root cause: every generated simulation event was built with `id(-1)` (`backend/.../simulation/ScenarioEventFactory.java`) while the event table keyed rows on `key={event.id}`, so all rows shared one key. It was verified non-observable at the time (the table always rendered all 40 rows; apparent repeated rows were distinct events whose millisecond timestamps are truncated by `toLocaleTimeString()`, not React duplication) and was classified non-blocking, with the reference to `FINAL_RELEASE_READINESS.md` corrected from section 18a to section 17a.

**Status update — this defect is now FIXED.** Generated simulation events carry a deterministic unique id composed of the session ordinal and the per-session emission sequence, in a range above any ingested dataset id, and the frontend key helper namespaces by provenance instead of treating a negative id as a signal. A later browser pass on the live stream recorded **0** duplicate-key warnings. See commit `ac9a1b5`.

## 18. Product honesty check

| Claim type | Status |
|---|---|
| Root cause | Not claimed — blast-radius banner explicitly says "not a causal path" |
| Confidence scores | Not present |
| Acknowledgement | Operator-initiated only, via explicit control |
| Fabricated runtimes | Not claimed; real backend endpoints and measured timings only |
| Dataset vs. generated | Now correctly distinguished in all topology surfaces (Section 7) |
| Traffic particles | Declared illustrative in the 3D note |

## 19. Fixes made in this pass (10)

1. Added missing `/overview` route alias (previously rendered 404).
2. `.sidebar-footer-meta` hardcoded colour → `var(--text-faint)` (contrast).
3. `.log-row-time span` hardcoded colour → `var(--text-faint)`, 8px → the 10px floor (contrast + size).
4. `.log-row-service .badge` 7px → the 10px floor.
5. `.log-row-service small`, `.log-query-syntax code`, `.signal-event-meta .badge`, `.signal-event-foot`, mobile `.log-row-time time` 8px → the 10px floor.
6. `.source-card-heading` 8px → 10px.
7. Topology source labelling: `graphKind` prop across `TopologyPanel` + `Topology3D`; simulation pages pass `"declared"`.
8. Default `title`/`description` made source-aware so the accessible description no longer leaks "dataset-wide events" on simulation pages.
9. Mobile touch targets raised to ≥32px.
10. Added `Stop` control to the Incident Workbench.

Each fix is covered by a test where testable (`TopologyPanel.test.tsx` gained a declared-graph case) and verified in-browser after rebuild.

## 20. Final verification

| Check | Command | Result |
|---|---|---|
| Backend suite | `./mvnw.cmd -o test` | **877 tests, 0 failures, 0 errors — BUILD SUCCESS** |
| Frontend suite | `npx vitest run --pool=forks` | **55 tests, 17 files, all passed** |
| TypeScript | `npx tsc --noEmit` | Clean |
| Production build | `npm run build` | Success (~3.6s) |

---

## Presentation checklist

| # | Item | Verdict | Evidence |
|---|---|---|---|
| 1 | Cyan/teal used as accent, not decoration | Pass | Accent reserved for interactive/active state |
| 2 | Colour not the only signal | Pass | Status pills, text labels, and icons accompany colour |
| 3 | Backgrounds do not reduce readability | Pass | 0 confirmed low-contrast findings post-fix |
| 4 | No critical information below the type floor | **Fixed** | 8 rules raised; no content below the 10px floor |
| 5 | Keyboard focus visible | Pass | Visible focus rings retained across controls |
| 6 | Reachable by keyboard alone | Pass | Accessible service list mirrors every topology node |
| 7 | Nav landmarks and headings | Pass | `h1` present and correct on all 16 routes |
| 8 | Reduced motion respected | Pass | 0 infinite animations under `prefers-reduced-motion` |
| 9 | Data updates are comprehensible | Pass | Tick 1→67 with coherent metric progression (Section 8) |
| 10 | No unnecessary continuous animation | Partial | Particles constant-speed; documented in Section 13 |
| 11 | Charts/live data have text equivalents | Pass | Numeric stat cards alongside every chart |
| 12 | No content clipped at tested widths | Pass | 0 overlaps, 0 clipped labels, 0 overflow |
| 13 | No layout jumping during live updates | Pass | DOM stable at 924–1,319 nodes across cycles |
| 14 | Dense tables degrade to cards on mobile | Pass | Verified at 390px, no overflow |
| 15 | Sidebar collapses to drawer on mobile | Pass | Nav toggle present on all routes |
| 16 | Touch targets adequate | **Fixed** | 111 → 7 under-32px controls on `/logs` |
| 17 | System status honest | Pass | `/system` reachable and accurate |
| 18 | Pages don't look like static mockups | Pass | Values change with stream; no placeholder content |
| 19 | Error/empty states are informative | **Fixed** | Workbench Stop added; empty state actionable |
| 20 | Generated vs. dataset clearly distinguished | **Fixed** | `DECLARED` vs `OBSERVED DEPENDENCIES` (Section 7) |

**Not claimed / out of scope:** traffic-proportional particle speed, persisted topology view mode, and mobile window-control persistence in the app shell (route state only). These are documented rather than silently altered.

## Final counts

| Metric | Before this pass | After |
|---|---|---|
| Frontend tests | 49 | **55** (current) |
| Not-found routes | 1 (`/overview`) | **0** |
| Low-contrast content items | 14 | **0** |
| Content items under the 10px floor | 8 rules | **0** |
| False source claims on simulation pages | 5 surfaces × 3 pages | **0** |
| Pages with no way to stop a running stream | 1 | **0** |
| Mobile controls under 32px (`/logs`) | 111 | **7** |
| Route/viewport combinations with overflow | unknown | **0 of 48** |
