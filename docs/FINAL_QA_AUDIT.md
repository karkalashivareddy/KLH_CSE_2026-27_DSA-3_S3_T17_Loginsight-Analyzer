# Final quality audit

Audit scope: the **Atmospheric Signal** revision — rewritten design system, new motion layer, route-level code splitting, an application error boundary, four backend correctness fixes, and a refreshed browser-evidence set. This record supersedes the Signal Atlas pass; the earlier entries are retained below for traceability, marked as closed.

## Defects found and corrected in this revision

| Severity | Finding and root cause | Correction | Evidence |
|---|---|---|---|
| **High** | `MillerRabin.testTracked` performed no argument validation. `testTracked(n, PROBABILISTIC, rng, 0)` therefore produced an empty witness array, the witness loop never executed, and the method returned `PRIME` for composite `n` — a **wrong primality verdict**, reported under a `probabilistic` label. `testTracked(n, null, …)` also threw NPE. | Extracted `validateArguments` and called it from both `test` and `testTracked`. Null mode, `PROBABILISTIC` with a null `rng`, and non-positive rounds are rejected up front with `IllegalArgumentException`. | `MillerRabinValidationRegressionTest` — new file; composites that previously read `PRIME` are now rejected at the boundary. |
| **High** | `POST /api/datasets` with an empty or whitespace-only body answered **HTTP 500**. `ParserException` had no handler, so an entirely ordinary client mistake fell through to the generic `Exception` handler. | `ParserException` added to the 400 handler; `UnsupportedLogFormatException` inherits it. | `ParserExceptionMappingTest` and `DatasetUploadErrorContractTest` — new files. The latter asserts `status: 400`, `error: "UnsupportedLogFormatException"`, `message: "input stream is empty"`, `path: "/api/datasets"`. |
| **High** | `DatasetService.ingest(null, stream)` threw `NullPointerException` (name dereferenced before null-check), and the empty-stream guard `!name.isEmpty() && raw.length == 0` was **de-inverted**: a named dataset with an empty body skipped the guard entirely and failed later with the wrong exception type. | Name normalized first (trimmed; blank/null → `"imported-logs"`), null stream rejected with `IllegalArgumentException`, and any zero-length body raises `UnsupportedLogFormatException` regardless of the name. | `DatasetServiceIngestRegressionTest` — new file. |
| **Medium** | `RunService`'s SSE emitter registered no `onCompletion` / `onTimeout` / `onError` callbacks, unlike the other two SSE services. An abandoned run replay kept occupying a pool thread until the 60 s emitter timeout. `shutdown()` called `shutdown()` on the pool and returned without waiting, so `@PreDestroy` dropped in-flight replays. | Replay is now a `ReplayTask` registering all three callbacks, guarded by an `AtomicBoolean` so completion, cancellation and failure are idempotent; `shutdown()` adds a bounded 5 s `awaitTermination` and restores the interrupt flag. | `RunServiceSseLifecycleTest` — new file. |
| **Medium** | First-load JavaScript was ~1061 kB raw / ~281 kB gzip, and the entire Three.js payload was on the critical path even though the 3D topology is reachable only from the Services page. | Route-level `lazy()` for 17 of 20 routes (Command Center, Logs and Workbench kept in the entry chunk), plus `manualChunks` splitting `vendor-react` / `vendor-motion` / `vendor-icons` / `vendor-three`. | Measured build: first load **~493 kB raw / ~154 kB gzip** — a 54% / 45% reduction. `vendor-three` (562.23 kB) is now fetched only when 3D is opened. |
| **Medium** | A render-time exception anywhere in the tree unmounted the whole workspace and left a blank page with no message and no recovery path. A reachable-but-failing backend could produce exactly that. | `AppErrorBoundary` class component wrapping `<App />` in `main.tsx`, rendering a `role="alert"` panel with "Try again" and "Reload workspace". Never renders a stack trace. | `AppErrorBoundary.test.tsx` — 3 new tests. Playwright: `a failing backend surfaces a retryable error instead of a blank workspace`. |
| **Low** | Scroll-reveal used a CSS-hidden class, so a failing or unavailable `IntersectionObserver` could leave content permanently invisible. | The hidden state now arrives from JavaScript: `Layout` adds `.reveal-section`, then `.is-revealed` on entry. A failed observer means nothing is ever hidden. | `signal-atlas.css` comment at `.reveal-section`; Playwright asserts `.is-revealed` toggles on real scrolling and that the reduced-motion run stays visible without it. |
| **Low** | `docs/images/` carried eleven pre-redesign screenshots of the previous interface plus a generated `capture-report.json`, none of them referenced by anything. | Removed, after a repository-wide grep proved zero references. See the evidence section. | Deletion evidence below. |

## Verified defects and corrections — previous revision (closed)

| Severity | Finding and root cause | Correction | Evidence |
|---|---|---|---|
| Medium | Scroll reveals were only observed at initial mount, before asynchronous overview sections existed. | Observe the app content with `IntersectionObserver` and attach newly inserted content using a scoped `MutationObserver`; disconnect both on cleanup. | Playwright scrolls to the lower overview section and observes reveal; reduced-motion run keeps it visible without adding the reveal class. |
| Medium | The faint semantic text token measured 4.44:1 against the light surface, and several dense overview/demo labels used 8–9px text. | Darkened the token and raised targeted operational metadata to a 10px floor. | Playwright computes semantic token contrast pairs and asserts AA contrast; the six viewport checks pass. This is not a full application conformance audit. |
| Low | Browser icon still used KLH branding as the app favicon while the product identity had changed. | Added the LogInsight SVG brand mark and set it as the app favicon/theme color. | Direct-route E2E confirms the asset is served and referenced. |

## Current evidence and limits

### Executed on this revision

| Gate | Command | Result |
|---|---|---|
| Backend | `cd backend; .\mvnw.cmd -o verify` | **910 tests, 0 failures, 0 errors, 0 skipped** across 108 surefire classes |
| Frontend unit | `cd frontend; npx vitest run` | **64 tests passed across 19 test files**, 0 failures, 34.09 s |
| Frontend build | `cd frontend; npm run build` | `tsc && vite build` clean; 2357 modules; `index.css` 191.81 kB (35.61 kB gzip) |
| Bundle | same build | First load ~493 kB raw / ~154 kB gzip across `index`, `vendor-react`, `vendor-motion`, `vendor-icons` |
| E2E definition | `frontend/e2e/product.spec.ts` | **15 workflows** defined (was 9) |

### Not executed on this revision — and therefore not claimed

- **Playwright was not re-run here.** The suite grew from 9 to 15 workflows; the six additions are present in source and are named in [IMPLEMENTATION_AUDIT.md](IMPLEMENTATION_AUDIT.md), but no local or CI run has executed them.
- **`npm audit` was not re-run.** The last recorded result was 0 vulnerabilities after the React Router 7.18.4 update.
- **`docker compose config --quiet` was not re-run.** No Docker daemon is reachable in this environment; image builds and container smoke tests remain unexecuted and unclaimed.
- **No GitHub Actions run exists for these changes.** Run [38052830711](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38052830711) passed all three jobs on the *previous* commit `c6349c77f3f755c231e1172efec14b01009bf1ef`, and reports that commit's counts (878 backend, 61 frontend, 9 browser workflows). It is not evidence for this revision.
- **The Vitest worker-start timeout did not reproduce.** Earlier revisions recorded a 50-test/17-file run plus an isolated 11-test topology run. The single aggregate invocation completed all 19 files and 64 tests. That workaround is retired.

### Standing limits

- Browser tests exercise the actual local Spring backend with the deterministic demo dataset, not an external service.
- Browser console/network errors are monitored across the documented product routes. This does not replace production traffic/load testing.
- Responsive assertions cover 1440×900, 1280×800, 1024×768, 768×1024, 390×844 and 360×800 on Command Center and the Service Map — not every route at every size.
- **Bundle warning.** Earlier records stated the build "warns" about a 571.82 kB chunk above a 500 kB threshold. That is no longer true. `chunkSizeWarningLimit` was raised to 600, `vendor-three` measures 562.23 kB, and the measured build **emits no size warning**. The threshold was raised to match the real per-chunk cost of the one intentionally deferred dependency — it was not silenced. GPU-backed rendering still was not verified; the fallback was.
- No full axe scan, no screen-reader audit, no pixel-diff visual-regression suite and no Docker runtime check are configured. Do not infer complete WCAG conformance or production readiness.

### Unresolved submission defect

`LogInsight_Final_Project_Presentation.pptx`, `LogInsight_Final_Project_Report.docx` and `LogInsight_Final_Project_Report.pdf` under `final-submission/` were **not regenerated**. No document-generation toolchain (Office, LibreOffice, Pandoc, python-docx, python-pptx) is available in this environment. The binaries still show the previous interface and the previous test counts, so the submitted documents do not describe the product being submitted. This is a real, open defect — see [../final-submission/FINAL_SUBMISSION_INDEX.md](../final-submission/FINAL_SUBMISSION_INDEX.md).

## Deletion evidence

The eleven pre-redesign screenshots and the generated capture report were removed. Before removal, a repository-wide content search across `*.md`, `*.tsx`, `*.ts`, `*.java`, `*.json`, `*.yml`, `*.css`, `*.html` and `*.mjs` (excluding `node_modules` and `package-lock.json`) was run for each filename. Every one returned **zero matches**:

`overview.png`, `topology-2d.png`, `topology-3d.png`, `live-monitor.png`, `tablet-overview.png`, `mobile-overview.png`, `dataset-analysis.png`, `incident-workbench.png`, `algorithm-evidence.png`, `run-summary.png`, `scenario-lab.png`, `capture-report.json`

`README.md` refers to `docs/images/` only as a directory ("older images are historical") and links to one specific file, `docs/images/signal-atlas/command-center-desktop.png`, which is untouched. `docs/13-testing.md` likewise referred to the directory generically. Both survive the deletion without a dangling link. The one literal `capture-report.json` reference in `docs/archive/POST_REBUILD_AUDIT.md` was updated.

**Kept:** `docs/images/klh-logo.png`. It is a brand asset, not a screenshot; it is outside the removal set; and the checklist references the live asset at `frontend/public/klh-logo.png`, which is a different file and still present.

## Internal scorecard (out of 10)

Scores are an internal self-assessment, not an external rubric result, and not a substitute for the submission gaps listed above. Equal weight across the 12 areas; the overall value is the arithmetic mean, rounded to one decimal. "After" reflects this revision.

| Area | Previous | After | Basis / remaining gap |
|---|---:|---:|---|
| Visual design | 9.1 | 9.0 | Atmospheric Signal is coherent and screenshot-evidenced, but route-by-route human design review was not repeated this pass. |
| Typography/readability | 8.8 | 8.9 | Deep-ink scale with an explicit 20-pair AA contrast assertion in E2E. Still no complete component-by-component contrast scan. |
| Motion/interaction | 8.8 | 9.0 | Motion tokens shared between CSS and JS, compositor-only scroll linkage, and a documented reduced-motion contract. Not every animation state manually audited. |
| Dependency Topology | 8.8 | 8.8 | Real node selection, inspector screenshot, 2D and WebGL fallback. Hardware 3D and large-graph performance still unassessed. |
| Functional correctness | 9.2 | 9.5 | Four backend defects fixed with regression tests, including a wrong primality verdict and a 500-instead-of-400. Broader failure injection is still limited. |
| Accessibility | 8.4 | 8.6 | 20-pair contrast assertion, reduced-motion handling, and a recoverable crash surface. No axe scan and no screen-reader audit. |
| Responsive quality | 9.2 | 9.1 | Five authoritative breakpoints and six E2E viewport targets, but E2E was not re-run and only two routes are covered at each size. |
| Automated testing | 9.3 | 9.5 | 910 backend, 64 frontend, 15 defined browser workflows, 5 new regression test classes. Held back from 10 because the 6 new E2E tests are unexecuted. |
| Performance | 8.4 | 9.2 | First-load JS cut from ~1061 kB to ~493 kB raw and ~281 kB to ~154 kB gzip, with a named deferred chunk and an honest, un-suppressed size threshold. |
| Academic submission | 8.7 | 7.5 | **Worse, deliberately.** The submitted `.pptx`/`.docx`/`.pdf` were not regenerated and still show the previous interface; student and course metadata still need institutional confirmation. |
| Presentation quality | 8.7 | 7.5 | **Worse, deliberately.** Same root cause: the checked-in deck is stale relative to the product and no renderer was available to inspect it. |
| Documentation/release | 9.1 | 9.2 | Audits, architecture, design system, Command Center and API documents all describe this revision; dead files and orphaned assets are now tracked rather than silently retained. Final exact-CI state is still unrecorded because no CI run exists for this revision. |

**Overall: 8.6/10.** The mean falls against the previous 8.9 because two areas — academic submission and presentation quality — were scored down to reflect an unresolved, real defect rather than a documentation gap. The implementation and verification work improved; the *submitted artefacts* did not follow it. Hardware 3D rendering, complete accessibility review, all-route responsive QA, a container runtime check and a CI run for this revision remain unverified.
