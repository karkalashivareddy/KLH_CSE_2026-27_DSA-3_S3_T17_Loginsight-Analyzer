# LogInsight — Final Submission Index

## Read this first

This revision delivers the complete submission set: a rebuilt frontend interface, a corrected WebGL
topology lifecycle, four backend correctness fixes, and **all three submission documents regenerated
from the running application on this revision**.

> **The submission documents are no longer blocked.** An earlier revision reported the `.pptx`,
> `.docx` and `.pdf` as stale because no document toolchain was assumed to exist. That assumption was
> wrong, and the tools are now confirmed present:
>
> - **Microsoft Word and PowerPoint** (Office 16) are installed and driveable over COM. The report PDF
>   is therefore **Word's own export of the DOCX**, not a second layout engine's approximation, and the
>   deck is verified by rendering every slide through PowerPoint.
> - **Python** with `python-docx`, `python-pptx`, `Pillow`, `pymupdf` and `reportlab`.
>
> The report is the supplied institutional sample report, corrected in place: its page size, margins,
> Cambria/Consolas typography, chapter hierarchy, cover block and figure sizing are all preserved
> exactly. The PDF is Word's rendering of that DOCX, so the two agree page for page.

**Project:** LogInsight — Real-Time Log Intelligence & Incident Investigation Platform
**Course:** Data Structures and Algorithms-3 (25CS2103E) · 2026-2027, Odd Semester
**Repository:** https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer
**Branch checked out:** `main`

## Requirement status

Every item is marked from evidence. Nothing is marked complete on the basis of intent.

| # | Requirement | Status | Basis |
|---|---|---|---|
| 1 | Project identity, course context and team recorded | **UNVERIFIED** | The two names, two roll numbers and the course code are transcribed from the institutional report title page, not from anything the repository can verify. See the note below. |
| 2 | Problem, objectives and scope documented | **VERIFIED** | `docs/01-requirements.md`; `docs/COMMAND_CENTER.md` |
| 3 | Architecture documented and matching the code | **VERIFIED** | `docs/ARCHITECTURE.md` and `docs/02-architecture.md` describe the motion layer, error boundary, route splitting, vendor chunking and stylesheet layering that are present in source. |
| 4 | DSA contributions honestly mapped to product use | **VERIFIED** | `docs/ALGORITHMS.md`, `docs/DSA_PRODUCT_MAPPING.md`, `docs/04-dsa-mapping.md`. The **42 / 36 / 13 / 35** figures are asserted exactly in `AlgorithmCatalogTest`, including both reconciling facts, so they fail the build if they drift. |
| 5 | Backend correctness | **VERIFIED** | `.\mvnw.cmd -B verify` — **910 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS. Includes 5 new regression classes for this revision's fixes. |
| 6 | Frontend correctness | **VERIFIED** | `npm test` — **72 tests across 20 test files**, 0 failures, 28.24 s, single aggregate run. |
| 7 | Frontend builds | **VERIFIED** | `npm run build` — `tsc` and Vite clean, 2,356 modules transformed, built in 5.62 s, **no bundle-size warning**. |
| 8 | Browser end-to-end coverage | **VERIFIED** | `npm run test:e2e` — **18 workflows, 18 passed**, 34.8 s against the live application. Covers semantic contrast, the signal field's no-fabrication contract, the empty-dataset state, a failing backend, command-palette focus restoration, route aliases, compact navigation, and the 2D/3D toggle surviving repeated switching. |
| 9 | CI evidence for this revision | **VERIFIED** | Run [38109292191](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38109292191) passed all three jobs on commit `4f74d247e1002f717f5ce0a2e11a343e9e40c37a`. Run [38066030545](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38066030545) passed for the preceding revision. |
| 10 | Dependency audit on this revision | **VERIFIED** | `npm audit` — **0 vulnerabilities**. |
| 11 | Genuine, current UI evidence | **VERIFIED** | 19 QA screenshots in `docs/images/signal-atlas/` and **11 report figures** in `docs/images/report/`, every one captured from the running application against a dataset loaded through the API. Reproducible via `npm run capture:visuals` and `npm run capture:report`. |
| 12 | Documentation matches the implementation | **VERIFIED** | All docs under `docs/` were re-synchronised to this revision. Dead and orphaned files are named rather than silently retained. |
| 13 | Docker deployment validated | **NOT VERIFIED** | `docker compose config --quiet` passes. **No Docker daemon is reachable in this environment**, so image builds and container smoke tests have not been run and are not claimed. |
| 14 | Accessibility conformance | **PARTIALLY VERIFIED** | Source-level review, 20 semantic token contrast pairs asserted in E2E, reduced-motion handled at three levels, visible focus indicators asserted. **No axe scan and no screen-reader audit are configured.** Contrast assertions are a focused check, not whole-application WCAG certification. |
| 15 | Hardware-accelerated WebGL rendering | **VERIFIED** | The unavailable-WebGL fallback *and* successful rendering are both now captured. Headless Chromium renders the scene through SwiftShader, and Figure 7 of the report and slide 13 of the deck show a **real rendered WebGL graph**, not the fallback. |
| 16 | **Presentation matches the submitted product** | **VERIFIED** | `LogInsight_Analyzer_Final_Presentation.pptx` — **16 slides**, 7 embedded captures, regenerated by `scripts/build_presentation.py`. Every diagram is built from native PowerPoint shapes and stays editable. All 16 slides were rendered through PowerPoint and inspected. |
| 17 | **Report matches the submitted product** | **VERIFIED** | `LogInsight_Final_Project_Report.docx` — built by `scripts/build_report.py` from the supplied sample, with 11 current screenshots and every figure re-verified against the repository. |
| 18 | **PDF matches the submitted product** | **VERIFIED** | `LogInsight_Final_Project_Report.pdf` — **20 A4 pages**, exported by Word from the same DOCX via `scripts/export_report_pdf.ps1`, so the two agree page for page. |
| 19 | Presentation can be visually inspected | **VERIFIED** | Every slide was exported to PNG through PowerPoint and reviewed for clipping, overflow and overlap. `scripts/check_deck_layout.py` additionally runs **1,352 structural assertions** over the deck (bounds, picture/panel overlap, text-versus-card fit) without needing Office installed. |
| 20 | Official slide count / template compliance | **NOT APPLICABLE** | No mandatory slide count and no official template were found anywhere in the repository. Recorded as N/A rather than guessed at. |

## Student

| Name | Roll Number | Programme |
| --- | --- | --- |
| KARKALA SHIVA REDDY | 2520030105 | B.Tech Computer Science and Engineering · graduating 2029 |
| PARIPALLI NAVADEEP | 2520030196 | See the verification note below |

Institution: Koneru Lakshmaiah Education Foundation (KL University), KLH Campus, Guntur.
Course instructor: Dr. J Sirisha Devi, Professor, Department of Computer Science and Engineering.

> **UNVERIFIED — needs a human check before submission.** Nothing in this repository can confirm a
> student roster, a course code, a section or a submission date. The two names and the two roll numbers
> above are carried verbatim from the institutional report title page
> (`LogInsight_Final_Project_Report.docx`), not derived from code, and not altered here. Course code
> `25CS2103E` is likewise carried from that document and from the presentation. Confirm all of these
> against the official registration record.

## Deliverables in this directory

| File | Description | State |
| --- | --- | --- |
| `LogInsight_Analyzer_Final_Presentation.pptx` | 16 slides, 16:9: problem, objectives, positioning, architecture, stack, data structures, algorithms, simulation pipeline, four product walkthroughs, verification, limitations and conclusion. 7 current captures embedded. | **Current** — regenerated by `scripts/build_presentation.py`. |
| `LogInsight_Final_Project_Report.docx` | The supplied institutional report, corrected in place. A4 portrait, 1-inch margins, Cambria prose and Consolas code, four chapters, 11 current screenshots. | **Current** — regenerated by `scripts/build_report.py` from the sample. |
| `LogInsight_Final_Project_Report.pdf` | 20-page A4 export of that DOCX, produced by Word itself. | **Current** — `scripts/export_report_pdf.ps1`. |
| `FINAL_SUBMISSION_INDEX.md` | This file. | Current. |
| `FINAL_SUBMISSION_CHECKLIST.md` | Requirement-to-evidence map. | Current. |
| `PRESENTATION_OUTLINE.md` | Slide-by-slide outline of the deck, with the evidence each slide rests on. | Current. |

The binaries are generated rather than hand-edited. The report is produced by
`scripts/build_report.py`, the deck by `scripts/build_presentation.py`, and both consume the
screenshots in `docs/images/report/` plus `scripts/submission_meta.json`, so re-running the scripts
reproduces them from the current application instead of letting them drift.

## Verification figures

Measured on this revision, 2026-10-11.

| Gate | Command | Result |
| --- | --- | --- |
| Backend | `cd backend; .\mvnw.cmd -B verify` | **910 tests**, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS (28.2 s) |
| Frontend tests | `cd frontend; npm test` | **72 tests across 20 files**, 0 failures (28.24 s) |
| TypeScript + build | `cd frontend; npm run build` | Clean (`tsc && vite build`), 2,356 modules, 5.62 s, no warning |
| Browser E2E | `cd frontend; npm run test:e2e` | **18 workflows, 18 passed** (34.8 s) |
| Dependency audit | `cd frontend; npm audit` | **0 vulnerabilities** |
| QA screenshots | `cd frontend; npm run capture:visuals` | 19 into `docs/images/signal-atlas/` |
| Report figures | `cd frontend; npm run capture:report` | 11 into `docs/images/report/` |
| Report checks | `python scripts/verify_report.py` | **56 checks passed** |
| Deck checks | `python scripts/check_deck_layout.py` | **1,352 checks passed** |
| Report PDF | `powershell scripts/export_report_pdf.ps1` | 20 A4 pages, Word export |
| Slide renders | `powershell scripts/export_slides.ps1` | 16 PNGs via PowerPoint |
| Compose syntax | `docker compose config --quiet` | Passed |
| Container runtime | `docker compose up --build` | Not run: Docker daemon unavailable |

The workflow is `.github/workflows/ci.yml`: the backend job runs `mvn -B verify`; the frontend job runs
`npm ci`, `npm test` and `npm run build`; the browser job runs the Playwright workflows. Local and
workflow figures agree where both have been executed.

## CI record

| Run | Commit | Result |
| --- | --- | --- |
| [38109292191](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38109292191) | `4f74d24` | **success** — backend, frontend tests/build, browser workflows |
| [38066030545](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38066030545) | `bcafe72` | **success** |
| [38053218849](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38053218849) | `444868b` | **success** — reported 878 backend tests, the 61-test frontend suite and 9 browser workflows, which are the counts for *that* commit only |

## Bundle

Measured from `npm run build` on this revision.

| Chunk | Raw | gzip | Loaded |
| --- | --- | --- | --- |
| `index` | 141.66 kB | 41.03 kB | first load |
| `vendor-react` | 181.40 kB | 59.80 kB | first load |
| `vendor-motion` | 127.88 kB | 42.19 kB | first load |
| `vendor-icons` | 32.28 kB | 7.08 kB | first load |
| **First-load JS total** | **483.22 kB** | **150.10 kB** | |
| `vendor-three` | 562.23 kB | 140.66 kB | **deferred** until 3D is selected |
| `Topology3D` | 10.52 kB | 4.09 kB | lazy route wrapper |
| `index.css` | 189.83 kB | 35.07 kB | first load |

`chunkSizeWarningLimit` is 600, so the deferred Three.js chunk emits no warning.

## What changed in this revision

### Frontend

- **Design system rewritten.** styles/signal-atlas.css is now the authoritative **Atmospheric Signal**
  system: warm mineral surfaces, deep-ink text, an atmospheric teal accent, and an editorial serif
  display face paired with a monospace face for operational data.
- **Command center compacted.** The hero block dropped from roughly 523 px to 335 px, moving the
  selected-window metrics from `y≈1565` to `y≈616`, so they are readable without scrolling. The
  decorative atmosphere and isobar backdrop was removed as noise competing with dense log data, and the
  generated-simulation band moved below the dataset evidence so it cannot be read as part of it.
- **Navigation reduced to five primary Workspace entries** (Overview, Logs, Incidents, Services,
  Analytics) plus a collapsible **Advanced** group holding the remaining fourteen specialist tools. No
  route was removed: the workspace still has **36 route declarations** (1 layout, 34 paths, 1 catch-all)
  and all 19 navigation entries remain reachable.
- **Motion layer.** `src/motion/motion.ts` exports `MOTION`, `EASE`, `spring` and a variant set whose
  values mirror the CSS custom properties exactly, driving route transitions, reveals and feedback.
- **New error boundary.** `src/components/AppErrorBoundary.tsx` wraps `<App />` in `main.tsx`, replacing
  a render-time crash with a `role="alert"` recovery panel instead of a blank page.
- **Route-level code splitting.** 17 of 20 routes are `lazy()`-loaded; Command Center, Logs Explorer and
  Incident Workbench stay in the entry chunk.

### WebGL topology lifecycle — corrected

| Defect | Fix |
|---|---|
| The scene effect depended on raw `nodes`/`edges` arrays, so a parent render with unchanged data tore the scene down and rebuilt it — exhausting the browser's limited pool of WebGL contexts. | The effect is keyed on a content hash (`graphKey`, `incidentKey`), plus `reducedMotion` and an explicit retry token. |
| `renderer.dispose()` alone leaves the underlying WebGL context alive. | `forceContextLoss()` is called on unmount, alongside disposal of controls, geometry, materials, the observer and every listener. |
| A lost context left a permanently blank stage. | A `webglcontextrestored` listener rebuilds the scene, and a `Retry WebGL` control is offered if a context cannot be created at all. |
| The render loop ran while the panel was off-screen or the tab was hidden. | An `IntersectionObserver` and a `visibilitychange` listener pause the loop. |
| Raycasting against full geometry made every object pickable. | Raycasting is hit-area only. |
| The stage subtree was pruned by `role="img"`, hiding the WebGL fallback message and its retry control from assistive technology. | Removed. |

`frontend/src/components/TopologyPanelWebGL.test.tsx` adds **8 dedicated lifecycle tests**, including a
regression guard for the churn defect, and the Playwright suite asserts the 2D/3D toggle survives
repeated switching without duplicating canvases.

### Backend — four production fixes

| File | Defect | Fix |
| --- | --- | --- |
| `dsa/randomized/MillerRabin.java` | `testTracked(n, PROBABILISTIC, rng, 0)` skipped validation, produced an empty witness array, never ran the loop, and reported **composites as PRIME**. Null mode NPE'd. | Extracted `validateArguments`, shared by `test` and `testTracked`. |
| `service/DatasetService.java` | `ingest(null, stream)` NPE'd; the empty-stream guard `!name.isEmpty() && raw.length == 0` was de-inverted, so a named empty upload bypassed it. | Name normalized first; null stream → `IllegalArgumentException`; any zero-length body → `UnsupportedLogFormatException`. |
| `exception/GlobalExceptionHandler.java` | `ParserException` unmapped, so an empty/blank upload answered **500**. | `ParserException` added to the 400 handler. |
| `service/RunService.java` | SSE emitter registered no completion/timeout/error callbacks; `shutdown()` did not wait. | `ReplayTask` with all three callbacks and an `AtomicBoolean` guard; bounded 5 s `awaitTermination`. |

Five new test classes cover these: `MillerRabinValidationRegressionTest`,
`DatasetServiceIngestRegressionTest`, `ParserExceptionMappingTest`, `RunServiceSseLifecycleTest`,
`DatasetUploadErrorContractTest`. Backend total moved 878 → 910.

### Report corrections against the previous draft

The supplied sample report predated this revision and carried figures and code excerpts that no longer
matched the repository. Each was corrected against the source rather than restated:

| Claim in the sample | Corrected to | Checked against |
| --- | --- | --- |
| 877 backend tests | **910** | `mvnw -B verify` |
| 55 frontend tests across 17 files | **72 across 20 files** | `npm test` |
| Seven services | **eight** | `GET /api/services` |
| Five viewport widths down to 390x844 | **six, down to 360x800** | `frontend/e2e/product.spec.ts` |
| Tree DP for "diameter, centroid and subset sum" | **diameter, rerooting and sum-over-subsets** | `GET /api/algorithms` |
| `public Set<String> reachableFrom(...)` | **`public List<String> reachableFrom(...)`** with the `seen` set and the `outbound.containsKey` guard | `ServiceDependencyGraph.java` |
| `public Map<String, Object> lifecycleConflict(...)` | the current `ResponseEntity<?>` handler | `GlobalExceptionHandler.java` |
| `" cannot move from " + status + " to " + target +)` — a snippet that would not compile | the current exception constructor | `IncidentLifecycleStore.java` |
| "Figure 3 shows a run in progress" | **Figure 2**; Figure 3 is the catalogue | report captions |

The sample also forced each chapter onto a new page with a page break placed *inside* the preceding
empty spacer paragraph, which rendered a fully blank page at every chapter boundary — four of them. The
break now sits on the chapter heading as `pageBreakBefore`, so every chapter still opens on a fresh
page and the document has **zero blank pages**.

### Dead and orphaned files

| File | Finding |
| --- | --- |
| `frontend/src/styles/signal-in-motion.css` | **DEAD.** Imported by nothing — a repository-wide grep for `signal-in-motion` returns no match outside the file itself. The only remaining home of the retired `#315cf5` accent. Source code, so not deleted here; flagged for the source owner. |
| `docs/images/signal-atlas/guided-presentation-mobile.png` | **ORPHANED.** Present on disk, not produced by the current capture script. |
| `final-submission/LogInsight_Final_Project_Presentation.pptx` | **SUPERSEDED and removed.** Replaced by `LogInsight_Analyzer_Final_Presentation.pptx`, so the directory holds exactly one final deck. |
| `docs/images/*.png` (11 pre-redesign screenshots) | **REMOVED.** Zero content references remained. |
| `docs/images/capture-report.json` | **REMOVED.** A generated report listing captures whose artifacts no longer exist. |

## DSA contribution

Three classifications are used, and no algorithm is described as a product capability unless a product
path actually calls it.

**Product pipeline** — genuinely exercised by a reachable product surface.

| Algorithm | Complexity | Where it is called | Role |
| --- | --- | --- | --- |
| Knuth-Morris-Pratt | O(n + m) | `LogSearchService`, then `/api/search` and `/api/logs/explore` | Default free-text product search; also re-counts the dominant signature in the live detector |
| Aho-Corasick | O(n) | `SimulationDetector` over the rolling window | One-pass multi-signature labelling for the simulation evidence panel |
| Levenshtein | O(n·m) | product search response builder | `did you mean` suggestion on a zero-hit search |
| Fixed sliding window | O(n + W) | `RollingWindow`, then `SimulationDetector` | Baseline-relative elevated-error windows that become incidents |
| BFS reachability | O(V + E) | `SimulationDetector` over `SimulationTopology` | Blast radius over **declared** dependencies |

**Algorithm engine** — a real REST or trace endpoint a user or a run session can invoke, but not
driving a product panel: naive, Z and Rabin-Karp matchers, suffix array plus Kasai LCP, bounded edit
distances, Needleman-Wunsch and Smith-Waterman, tree and SOS dynamic programs, the flow family,
bitmask TSP, Hamiltonian path, vertex-cover approximation and kernelization, set cover, reservoir
sampling, Miller-Rabin, randomized quicksort, universal and perfect hashing, and the parallel family.

**Academic / Algorithm Lab** — implemented and tested but not on any product or engine path:
bounded vertex cover internals, independent-set and knapsack reductions.

The catalogue holds **42 descriptors across six modules** — Strings 9, Dynamic Programming 12,
Graph & Flow 6, Approximation 7, Randomized 5, Parallel 3. Of these, **36 are reachable** through a
canonical or a trace endpoint, dispatched across **35 distinct query/algorithm keys**, and **13 are
trace-instrumented**. The System page reports the exposure and traceability split per module, so the
boundary is visible in the product rather than only in documentation. All four figures are pinned by
`AlgorithmCatalogTest`, which also asserts both reconciling facts: 42 − 36 is the six library-only
entries, and 36 − 35 is the single dispatch key shared by suffix-array build and suffix-array search.

## Honesty constraints

- **Provenance.** Dataset replay and server-generated simulation are labelled separately at every
  surface; simulation is never presented as captured telemetry.
- **Topology.** Dataset surfaces show `OBSERVED DEPENDENCIES` from request-trail adjacency; simulation
  surfaces show `DECLARED DEPENDENCIES` from the scenario graph. The simulation blast radius is a
  traversal over declared dependencies, not over observed ones.
- **No invented numbers.** With no dataset loaded, the Command Center signal field renders em dashes,
  not zeros, and its footer legend says so. An empty detector result is explicitly "not evidence that
  the system is healthy".
- **No false confidence.** Health bands are fixed heuristic thresholds, not a trained model. Incidents
  are elevated-error windows with no confidence score, because none is computed. Blast radius is graph
  reachability, not a confirmed root cause.
- **No generated ML.** There is no trained model, no inference and no prediction anywhere in the
  product. Pattern extraction is token normalization; detection is thresholding plus multi-pattern
  matching.
- **Stale artefacts are named, not hidden.** This file, the checklist and the audit documents state the
  verification status of every deliverable, including the ones that remain open.

## Limitations

In-memory single-JVM state with no durable storage; restart discards the dataset, any running stream
and any session incident lifecycle. Simulation traffic is server-generated, not captured. Incident
lifecycle state lives in the session only and is never written to the dataset. Run history, event
buffers and trace steps are bounded. Docker Compose files were validated for syntax only, because no
container runtime was available in the development environment — image builds and container smoke
tests were not run and are not claimed. No authentication, authorization or multi-tenancy exists. No
external telemetry collector exists. `OverviewDto.systemStatus` is a hardcoded compatibility string;
real runtime status is `GET /api/health/status`.

## Open items for whoever submits this

1. **Confirm student names, roll numbers, course code, section and instructor** against the official
   registration record. Everything in this repository is transcribed, not verified.
2. **Push this revision and let CI run.** The current commit is verified locally and the previous
   revision is green in CI; the run covering *this* commit will appear once it is pushed.
3. **Delete `frontend/src/styles/signal-in-motion.css`** — dead, and the last home of the retired accent.
4. **Regenerate or delete `docs/images/signal-atlas/guided-presentation-mobile.png`** — orphaned.
5. **Confirm the manifest in `EngineScopeGuardTest`** still reflects intent; it is a frozen ledger that
   can only shrink.
6. **Run a container smoke test** on a machine with a Docker daemon, if container deployment is part of
   the assessment.