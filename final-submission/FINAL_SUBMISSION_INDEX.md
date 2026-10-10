# LogInsight — Final Submission Index

## Read this first

This revision delivers the **Atmospheric Signal** frontend (rewritten design system, motion layer, route-level code splitting, application error boundary), four backend correctness fixes, and a refreshed browser-evidence set. **One submission requirement is not met**, and it is stated here rather than buried:

> **BLOCKED — the three binary submission documents were NOT regenerated.**
> `LogInsight_Final_Project_Presentation.pptx`, `LogInsight_Final_Project_Report.docx` and
> `LogInsight_Final_Project_Report.pdf` in this directory still depict the *previous* interface and
> the *previous* test counts. No document-generation toolchain is available in this environment —
> no Microsoft Office, no LibreOffice, no Pandoc, no `python-docx`/`python-pptx`. They could not be
> regenerated, and they could not be edited safely either, because their screenshots are embedded
> raster images. **This is an open, unresolved submission defect.** If the submitted documents must
> match this revision, they have to be rebuilt on a machine that can open and edit `.pptx`/`.docx`.
> Nothing in this repository papers over it.

**Project:** LogInsight — Real-Time Log Intelligence & Incident Investigation Platform
**Course:** Data Structures and Algorithms-3 (25CS2103E) · 2026-2027, Odd Semester
**Repository:** https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer
**Branch checked out:** `feature/cinematic-loginsight-rebuild`

## Requirement status

Every item is marked from evidence. Nothing is marked complete on the basis of intent.

| # | Requirement | Status | Basis |
|---|---|---|---|
| 1 | Project identity, course context and team recorded | **UNVERIFIED** | The two names, two roll numbers and the course code are transcribed from the institutional report title page, not from anything the repository can verify. See the note below. |
| 2 | Problem, objectives and scope documented | **VERIFIED** | `docs/01-requirements.md`; `docs/COMMAND_CENTER.md` |
| 3 | Architecture documented and matching the code | **VERIFIED** | `docs/ARCHITECTURE.md` and `docs/02-architecture.md` describe the motion layer, error boundary, route splitting, vendor chunking and stylesheet layering that are present in source. |
| 4 | DSA contributions honestly mapped to product use | **VERIFIED** | `docs/ALGORITHMS.md`, `docs/DSA_PRODUCT_MAPPING.md`, `docs/04-dsa-mapping.md`. The 42 / 36 / 13 / 35 figures are asserted in `AlgorithmCatalogTest`, so they fail the build if they drift. |
| 5 | Backend correctness | **VERIFIED** | `./mvnw.cmd -o verify` — **910 tests, 0 failures, 0 errors, 0 skipped** across 108 surefire classes. Includes 5 new regression classes for this revision's fixes. |
| 6 | Frontend correctness | **VERIFIED** | `npx vitest run` — **64 tests across 19 test files**, 0 failures, in a single aggregate run. |
| 7 | Frontend builds | **VERIFIED** | `npm run build` — `tsc` and Vite clean; 2357 modules; no bundle-size warning. |
| 8 | Browser end-to-end coverage | **NOT VERIFIED** | 15 Playwright workflows are **defined** (was 9), including 6 new ones asserting semantic contrast, the signal field's no-fabrication contract, the empty-dataset state, a failing backend, command-palette focus restoration and route aliases. **They were not executed on this revision.** The nine pre-existing workflows were previously green against a live local backend. |
| 9 | CI evidence for this revision | **NOT VERIFIED** | No GitHub Actions run covers these changes. Run [38052830711](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38052830711) passed all three jobs on the **previous** commit `c6349c77f3f755c231e1172efec14b01009bf1ef` and reports that commit's counts. |
| 10 | Dependency audit on this revision | **NOT VERIFIED** | `npm audit` reported 0 vulnerabilities after the React Router 7.18.4 update; not re-run since. `motion@^14.1.0` and `lucide-react@^1.48.0` were added afterwards. |
| 11 | Genuine, current UI evidence | **VERIFIED** | 19 screenshots in `docs/images/signal-atlas/`, every one captured from the running application against a real dataset loaded through the API. Reproducible via `npm run capture:visuals`. |
| 12 | Documentation matches the implementation | **VERIFIED** | All docs under `docs/` were re-synchronised to this revision. Dead and orphaned files are named rather than silently retained. |
| 13 | Docker deployment validated | **NOT VERIFIED** | `docker compose config` passed on an earlier pass. No Docker daemon is reachable in this environment, so image builds and container smoke tests have not been run and are not claimed. |
| 14 | Accessibility conformance | **NOT VERIFIED** | Source-level review, 20 semantic token contrast pairs asserted in E2E, reduced-motion handled at three levels. **No axe scan and no screen-reader audit are configured.** Contrast assertions are a focused check, not whole-application WCAG certification. |
| 15 | Hardware-accelerated WebGL rendering | **NOT VERIFIED** | The unavailable-WebGL fallback *was* verified; successful GPU rendering was not. |
| 16 | **Presentation matches the submitted product** | **BLOCKED** | `LogInsight_Final_Project_Presentation.pptx` shows the previous interface. No generation toolchain available. |
| 17 | **Report matches the submitted product** | **BLOCKED** | `LogInsight_Final_Project_Report.docx` shows the previous interface and the previous test counts. Same cause. |
| 18 | **PDF matches the submitted product** | **BLOCKED** | `LogInsight_Final_Project_Report.pdf` is a rendering of that same stale report. Same cause. |
| 19 | Presentation can be visually inspected | **NOT VERIFIED** | No Office or LibreOffice renderer is available, so no slide-by-slide visual review of the `.pptx` was possible — on top of the deck being stale. |
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
| `LogInsight_Final_Project_Presentation.pptx` | Presentation covering the problem, the classical algorithms used in the product pipeline, architecture, real-time behaviour, interface, verification and limitations. | **STALE — previous interface, previous counts. Not regenerated.** |
| `LogInsight_Final_Project_Report.docx` | Full project report built on the institutional PBL template: case study, algorithm and pseudocode, code excerpts and results with captured screenshots. | **STALE — previous interface, previous counts. Not regenerated.** |
| `LogInsight_Final_Project_Report.pdf` | PDF rendering of the same report. | **STALE — a rendering of the stale report. Not regenerated.** |
| `FINAL_SUBMISSION_INDEX.md` | This file. | Current. |
| `FINAL_SUBMISSION_CHECKLIST.md` | Requirement-to-evidence map. | Current. |

The binary files were deliberately **not** modified. Editing them here would have produced documents with current text and stale embedded screenshots — a worse artifact than an honestly stale one.

## Verification figures

Measured locally on this revision.

| Gate | Command | Result |
| --- | --- | --- |
| Backend | `cd backend; .\mvnw.cmd -o verify` | **910 tests**, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS |
| Frontend tests | `cd frontend; npx vitest run` | **64 tests across 19 files**, 0 failures, 34.09 s |
| TypeScript + build | `cd frontend; npm run build` | Clean (`tsc && vite build`), no bundle-size warning |
| Browser E2E | `cd frontend; npm run test:e2e` | **15 workflows defined — not executed on this revision** |
| Visual capture | `cd frontend; npm run capture:visuals` | 19 screenshots into `docs/images/signal-atlas/` |
| Dependency audit | `cd frontend; npm audit` | Not re-run this revision |
| Compose syntax | `docker compose config --quiet` | Passed on an earlier pass; not re-run |
| Container runtime | `docker compose up --build` | Not run: Docker daemon unavailable |

The aggregate Vitest invocation passed cleanly this time. Earlier revisions recorded a 50-test/17-file run plus an isolated 11-test topology run because the aggregate run hit a worker-start timeout on this machine; **that failure did not reproduce**, and the workaround is retired.

The workflow is `.github/workflows/ci.yml`: the backend job runs `mvn -B verify`; the frontend job runs `npm ci`, `npm test` and `npm run build`. Local and workflow figures agree where both have been executed.

### Historical CI record

Run 38052830711 passed for commit `c6349c77f3f755c231e1172efec14b01009bf1ef` — backend, frontend tests/build/audit and browser workflows. It reports **878** backend tests, the **61**-test frontend suite and **9** browser workflows. Those are the counts for *that* commit. They are not the counts for the code being submitted here.

## What changed in this revision

### Frontend

- **Design system rewritten.** `styles/signal-atlas.css` (1,312 lines) is now the authoritative **Atmospheric Signal** system: warm mineral surfaces, deep-ink text, atmospheric teal accent (`--accent: #0f6e7a`), editorial serif display, mono for data. It layers `global.css` and `product.css` underneath in the named cascade layer `loginsight-structure` and is itself unlayered, so it wins the cascade without editing the inherited sheets.
- **New motion layer.** `src/motion/motion.ts` exports `MOTION`, `EASE`, `spring` and a variant set whose values mirror the CSS custom properties exactly. `src/motion/Atmosphere.tsx` renders a scroll-linked decorative backdrop via `useScroll`/`useTransform`, writing only to compositor motion values, registering no scroll listener, and returning `null` under `prefers-reduced-motion`.
- **New error boundary.** `src/components/AppErrorBoundary.tsx` wraps `<App />` in `main.tsx`, replacing a render-time crash with a `role="alert"` recovery panel instead of a blank page. Three dedicated tests.
- **Route-level code splitting.** 17 of 20 routes are `lazy()`-loaded; Command Center, Logs Explorer and Incident Workbench stay in the entry chunk. `manualChunks` pins `three`, `motion`, `react-dom`/`react-router` and `lucide-react` into `vendor-three`, `vendor-motion`, `vendor-react` and `vendor-icons`.
- **New dependency.** `motion@^14.1.0` — the Framer Motion successor, imported from `motion/react`. `lucide-react` moved to `^1.48.0`.
- **Scroll reveal reworked.** The hidden state now arrives from JavaScript (`.reveal-section` → `.is-revealed`), so a failed `IntersectionObserver` leaves content visible. The old `.atlas-scroll-revealed` class is gone.

### Bundle

| | Before | After |
|---|---|---|
| `index` | 489.32 kB / 136.95 kB gzip | 143.46 kB / 41.73 kB gzip |
| `vendor-react` | — | 181.40 kB / 59.80 kB gzip |
| `vendor-motion` | — | 136.14 kB / 45.41 kB gzip |
| `vendor-icons` | — | 32.28 kB / 7.08 kB gzip |
| `Topology3D` | 571.82 kB / 144.14 kB gzip | 9.88 kB / 3.90 kB gzip (wrapper) |
| `vendor-three` | — | 562.23 kB / 140.66 kB gzip — **deferred** |
| **First-load JS** | **~1061 kB / ~281 kB gzip** | **~493 kB / ~154 kB gzip** |

`index.css` is 191.81 kB (35.61 kB gzip). **Correction:** earlier records stated the build "warns" about a 571.82 kB chunk above a 500 kB threshold. `chunkSizeWarningLimit` was raised to 600 and the deferred chunk measures 562.23 kB, so the current build **emits no size warning**. The threshold was raised to match real per-chunk cost, not to hide a problem.

### Backend — four production fixes

| File | Defect | Fix |
|---|---|---|
| `dsa/randomized/MillerRabin.java` | `testTracked(n, PROBABILISTIC, rng, 0)` skipped validation, produced an empty witness array, never ran the loop, and reported **composites as PRIME**. Null mode NPE'd. | Extracted `validateArguments`, shared by `test` and `testTracked`. |
| `service/DatasetService.java` | `ingest(null, stream)` NPE'd; the empty-stream guard `!name.isEmpty() && raw.length == 0` was de-inverted, so a named empty upload bypassed it. | Name normalized first; null stream → `IllegalArgumentException`; any zero-length body → `UnsupportedLogFormatException`. |
| `exception/GlobalExceptionHandler.java` | `ParserException` unmapped, so an empty/blank upload answered **500**. | `ParserException` added to the 400 handler. |
| `service/RunService.java` | SSE emitter registered no completion/timeout/error callbacks; `shutdown()` did not wait. | `ReplayTask` with all three callbacks and an `AtomicBoolean` guard; bounded 5 s `awaitTermination`. |

Five new test classes cover these: `MillerRabinValidationRegressionTest`, `DatasetServiceIngestRegressionTest`, `ParserExceptionMappingTest`, `RunServiceSseLifecycleTest`, `DatasetUploadErrorContractTest`. Backend total moved 878 → 910.

### Dead and orphaned files

| File | Finding |
|---|---|
| `frontend/src/styles/signal-in-motion.css` | **DEAD.** Imported by nothing — a repository-wide grep for `signal-in-motion` returns no match outside the file itself. The only remaining home of the retired `#315cf5` accent. Source code, so not deleted here; flagged for the source owner. |
| `docs/images/signal-atlas/guided-presentation-mobile.png` | **ORPHANED.** Present on disk, not produced by the current capture script (which writes 19 files; the directory holds 20). |
| `docs/images/*.png` (11 pre-redesign screenshots) | **REMOVED.** A repository-wide content search across `*.md`, `*.tsx`, `*.ts`, `*.java`, `*.json`, `*.yml`, `*.css`, `*.html`, `*.mjs` returned zero matches for each filename. `README.md` and `docs/13-testing.md` referenced the directory generically, not the files, so no link broke. |
| `docs/images/capture-report.json` | **REMOVED.** A generated report (`"historical": true`) listing exactly those eleven captures, whose artifacts no longer exist. Its one literal reference, in `docs/archive/POST_REBUILD_AUDIT.md`, was updated. |
| `docs/images/klh-logo.png` | **KEPT.** A brand asset, not a screenshot, and outside the removal set. The checklist references the live asset at `frontend/public/klh-logo.png`, which is a different file and is present. |

## DSA contribution

Three classifications are used, and no algorithm is described as a product capability unless a product path actually calls it. The counts below are the values returned by `GET /api/modules` and `GET /api/health/status`, and are pinned by `AlgorithmCatalogTest`.

**Product pipeline** — genuinely exercised by a reachable product surface.

| Algorithm | Complexity | Where it is called | Input | Output | Role |
| --- | --- | --- | --- | --- | --- |
| Knuth-Morris-Pratt | O(n + m) | `LogSearchService`, then `/api/search` and `/api/logs/explore` | haystack plus query | match offsets and measured runtime | Default free-text product search; also re-counts the dominant signature in the live detector |
| Aho-Corasick | O(n) | `SimulationDetector` over the rolling window | concatenated searchable text plus compiled signatures | per-signature occurrence counts | One-pass multi-signature labelling for the simulation evidence panel |
| Levenshtein | O(n·m) | product search response builder | message corpus | ranked near-miss terms | `did you mean` suggestion on a zero-hit search |
| Fixed sliding window | O(n + W) | `RollingWindow`, then `SimulationDetector` | the emitted event stream | per-tick and rolling aggregates, p95 estimate | Baseline-relative elevated-error windows that become incidents |
| BFS reachability | O(V + E) | `SimulationDetector` over `SimulationTopology` | the declared dependency graph | reachable service set | Blast radius over **declared** dependencies |

**Algorithm engine** — a real REST or trace endpoint a user or a run session can invoke, but not
driving a product panel: naive, Z and Rabin-Karp matchers, suffix array plus Kasai LCP, bounded edit
distances, Needleman-Wunsch and Smith-Waterman, tree and SOS dynamic programs, the flow family
(Ford-Fulkerson, Edmonds-Karp, Dinic, min-cut, bipartite matching, min-cost flow), bitmask TSP,
Hamiltonian path, vertex-cover approximation and kernelization, set cover, reservoir sampling,
Miller-Rabin, randomized quicksort, universal and perfect hashing, and the parallel family.

**Academic / Algorithm Lab** — implemented and tested but not on any product or engine path:
bounded vertex cover internals, independent-set and knapsack reductions.

The catalogue holds **42 descriptors across six modules**, backed by **35 registered query engines**;
**36** entries are reachable through a REST or trace endpoint and **13** are trace-instrumented. The
System page reports the exposure and traceability split per module, so the boundary is visible in the
product rather than only in documentation. See [../docs/DSA_PRODUCT_MAPPING.md](../docs/DSA_PRODUCT_MAPPING.md).

## Honesty constraints

- **Provenance.** Dataset replay and server-generated simulation are labelled separately at every surface; simulation is never presented as captured telemetry.
- **Topology.** Dataset surfaces show `OBSERVED DEPENDENCIES` from request-trail adjacency; simulation surfaces show `DECLARED DEPENDENCIES` from the scenario graph. The simulation blast radius is a traversal over declared dependencies, not over observed ones.
- **No invented numbers.** With no dataset loaded, the Command Center signal field renders em dashes, not zeros, and its footer legend says so. An empty detector result is explicitly "not evidence that the system is healthy".
- **No false confidence.** Health bands are fixed heuristic thresholds, not a trained model. Incidents are elevated-error windows with no confidence score, because none is computed. Blast radius is graph reachability, not a confirmed root cause.
- **No generated ML.** There is no trained model, no inference and no prediction anywhere in the product. Pattern extraction is token normalization; detection is thresholding plus multi-pattern matching.
- **Stale artefacts are named, not hidden.** This file, the checklist and the audit documents all state plainly that the three binary documents are out of date.

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

1. **Regenerate the `.pptx`, `.docx` and `.pdf` from the current UI and current test counts** on a machine with a document toolchain. This is the single largest gap in the submission.
2. **Confirm student names, roll numbers, course code, section and instructor** against the official registration record. Everything in this repository is transcribed, not verified.
3. **Run the 15 Playwright workflows** and record the result. They are defined and unexecuted.
4. **Push and let CI run.** No Actions run covers this revision.
5. **Delete `frontend/src/styles/signal-in-motion.css`** — dead, and the last home of the retired accent.
6. **Regenerate or delete `docs/images/signal-atlas/guided-presentation-mobile.png`** — orphaned.
7. **Confirm the manifest in `EngineScopeGuardTest`** still reflects intent; it is a frozen ledger that can only shrink.
