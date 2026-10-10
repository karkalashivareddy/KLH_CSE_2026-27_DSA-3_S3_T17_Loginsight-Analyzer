# Final quality audit

## Verified defects and corrections

| Severity | Finding and root cause | Correction | Evidence |
|---|---|---|---|
| Medium | Scroll reveals were only observed at initial mount, before asynchronous overview sections existed. | Observe the app content with `IntersectionObserver` and attach newly inserted content using a scoped `MutationObserver`; disconnect both on cleanup. | Playwright scrolls to the lower overview section and observes reveal; reduced-motion run keeps it visible without adding the reveal class. |
| Medium | The faint semantic text token measured 4.44:1 against the light surface, and several dense overview/demo labels used 8–9px text. | Darkened the token and raised targeted operational metadata to a 10px floor. | Playwright computes eleven semantic token contrast pairs and asserts AA contrast; the six viewport checks pass. This is not a full application conformance audit. |
| Low | Browser icon still used KLH branding as the app favicon while the product identity had changed. | Added the LogInsight SVG brand mark and set it as the app favicon/theme color. | Direct-route E2E confirms the asset is served and referenced. |
| Medium | Current screenshot evidence lacked a selected topology inspector state and counts/docs still described seven E2E workflows and nine images. | Added a genuine selected-node capture; refreshed the 10-image gallery and current test descriptions. | Capture script ran against the running frontend and backend; image set is under `docs/images/signal-atlas/`. |

## Current evidence and limits

- Implementation commit `c6349c77f3f755c231e1172efec14b01009bf1ef` passed [GitHub Actions run 38052830711](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38052830711); backend, frontend test/build/audit, and browser jobs all succeeded.
- Full backend and frontend test counts and final commit/run are recorded in the submission index after release verification.
- Browser tests exercise the actual local Spring backend with the deterministic demo dataset, not an external service.
- The aggregate Vitest invocation passed 50 assertions in 17 files, then could not start the topology worker before timeout. The isolated topology file then passed all 11 assertions. Treat the complete local unit-suite invocation as environment-limited; CI remains the aggregate check.
- Browser console/network errors are monitored across all documented product routes. This does not replace production traffic/load testing.
- Responsive assertions cover 1440×900, 1280×800, 1024×768, 768×1024, 390×844 and 360×800 on Command Center and topology.
- The optional 3D chunk remains 571.82 kB minified (144.15 kB gzip); the Vite warning was not suppressed. GPU-backed rendering was not verified; fallback was.
- No full axe scan, screen-reader audit, Docker runtime check or office-rendered presentation inspection was available in this pass. Do not infer complete WCAG conformance or production readiness.

## Evidence-based scorecard (out of 10)

Scores use equal weight across the 12 requested areas; the overall value is the arithmetic mean, rounded to one decimal. These are an internal assessment, not an external rubric result.

| Area | Before | After | Basis / remaining gap |
|---|---:|---:|---|
| Visual design | 8.5 | 9.1 | Consistent light Signal Atlas, refreshed screenshots; route-by-route human design review remains limited. |
| Typography/readability | 7.8 | 8.8 | Contrast floor and dense metadata improved; no complete component contrast scan. |
| Motion/interaction | 8.0 | 8.8 | Real scroll reveal and reduced-motion tests; not every animation state was manually audited. |
| Dependency Topology | 8.5 | 8.8 | Real node selection, inspector screenshot, 2D and WebGL fallback; hardware 3D and large-graph performance not assessed. |
| Functional correctness | 9.0 | 9.2 | Existing real-API workflows and direct routes pass; broader failure injection is limited. |
| Accessibility | 7.8 | 8.4 | Contrast tokens, keyboard and reduced motion covered; no full axe/screen-reader audit. |
| Responsive quality | 8.7 | 9.2 | Six target viewports pass for Command Center and topology, not every route/control at each size. |
| Automated testing | 9.0 | 9.3 | 878 backend, 61 frontend, 9 real-backend browser workflows; current CI result is separately reported. |
| Performance | 8.2 | 8.4 | App bundle split and lazy 3D; 571.82 kB optional chunk remains above Vite's advisory limit. |
| Academic submission | 8.0 | 8.7 | Rubric mapping and current evidence documented; student/course metadata needs institutional confirmation; report/PDF remain historical. |
| Presentation quality | 7.5 | 8.7 | Editable 14-slide local deck uses KLH logo and genuine current captures; no PowerPoint renderer available for visual slide inspection. |
| Documentation/release | 8.5 | 9.1 | Counts, assets and known gaps updated; final exact-CI state recorded after release. |

**Overall: 8.9/10** by equal-weight mean. The evidence does not support 9.5+: hardware 3D, complete accessibility review, all-route responsive visual QA, office-rendered deck inspection and Docker runtime remain unverified; the bundle warning and historical report/PDF are unresolved.
