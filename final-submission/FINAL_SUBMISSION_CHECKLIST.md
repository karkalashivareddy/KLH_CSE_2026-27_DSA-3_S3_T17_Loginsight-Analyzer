# Submission requirements and evidence map

Status vocabulary used throughout:

| Mark | Meaning |
|---|---|
| **VERIFIED** | Executed or inspected on this revision; the evidence exists in this repository. |
| **NOT VERIFIED** | The work exists but was not executed or checked here. No pass is claimed. |
| **BLOCKED** | Cannot be completed in this environment. Carried as an open defect. |
| **NOT APPLICABLE** | No such requirement could be found; recorded rather than guessed at. |

No mandatory slide count and no official PowerPoint template were found anywhere in the repository. That is recorded as **NOT APPLICABLE**, not inferred.

## Requirement map

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 1 | Project identity, course context and team | **NOT VERIFIED** | Values transcribed from `LogInsight_Final_Project_Report.docx` and flagged in [`FINAL_SUBMISSION_INDEX.md`](FINAL_SUBMISSION_INDEX.md) for official human confirmation. Institution logo asset: `frontend/public/klh-logo.png` (present). Nothing in the repository can verify a roster. |
| 2 | Problem, objectives and scope | **VERIFIED** | `docs/01-requirements.md`; `docs/COMMAND_CENTER.md`. |
| 3 | Architecture, data flow and provenance | **VERIFIED** | `docs/ARCHITECTURE.md` (motion layer, error boundary, route splitting, vendor chunking, stylesheet layering, SSE lifecycle); `docs/02-architecture.md` (phase artifact, drift annotated); `docs/design-system.md`. Each claim was checked against source. |
| 4 | DSA contributions and honest product mapping | **VERIFIED** | `docs/ALGORITHMS.md`, `docs/DSA_PRODUCT_MAPPING.md`, `docs/04-dsa-mapping.md`. The 42 / 36 / 13 / 35 figures are asserted in `AlgorithmCatalogTest` and fail the build on drift. |
| 5 | Backend correctness | **VERIFIED** | `./mvnw.cmd -o verify` — **910 tests, 0 failures, 0 errors, 0 skipped**. Includes 5 new regression classes covering this revision's four fixes. |
| 6 | Frontend correctness | **VERIFIED** | `npx vitest run` — **64 tests across 19 test files**, 0 failures, single aggregate run. |
| 7 | Production build | **VERIFIED** | `npm run build` — `tsc` and Vite clean, 2357 modules, no bundle-size warning. |
| 8 | Real product workflows (browser E2E) | **NOT VERIFIED** | 15 Playwright workflows **defined** in `frontend/e2e/product.spec.ts` (was 9); **not executed on this revision**. The nine pre-existing workflows were previously green against a live local backend. |
| 9 | CI evidence | **NOT VERIFIED** | No Actions run covers this revision. Run 38052830711 passed for the previous commit `c6349c7` and reports *that* commit's counts. |
| 10 | Dependency audit on this revision | **NOT VERIFIED** | `npm audit` reported 0 vulnerabilities before `motion@^14.1.0` and `lucide-react@^1.48.0` were added. Not re-run. |
| 11 | Genuine current UI evidence | **VERIFIED** | **19 screenshots** in `docs/images/signal-atlas/`, each captured from the running application against a real dataset loaded through the API — including the no-dataset state, a forced WebGL failure and a reduced-motion run. Reproducible with `npm run capture:visuals`. |
| 12 | Documentation matches the implementation | **VERIFIED** | Every document under `docs/` re-synchronised to this revision. Removed classes, the dead stylesheet and the orphaned screenshot are named rather than silently retained. |
| 13 | Container deployment validated | **NOT VERIFIED** | `docker compose config` passed on an earlier pass. No Docker daemon is reachable; image builds and smoke tests were not run. |
| 14 | Accessibility conformance | **NOT VERIFIED** | Source review, 20 semantic contrast pairs asserted in E2E, reduced motion handled at three independent levels. No axe scan and no screen-reader audit configured. |
| 15 | Hardware WebGL rendering | **NOT VERIFIED** | The unavailable-WebGL **fallback** was verified; successful GPU rendering was not. |
| 16 | Presentation matches the product | **BLOCKED** | `LogInsight_Final_Project_Presentation.pptx` shows the previous interface. No generation toolchain available. |
| 17 | Report matches the product | **BLOCKED** | `LogInsight_Final_Project_Report.docx` shows the previous interface and counts. Same cause. |
| 18 | PDF matches the product | **BLOCKED** | `LogInsight_Final_Project_Report.pdf` renders the same stale report. Same cause. |
| 19 | Slide-by-slide visual review of the deck | **NOT VERIFIED** | No Office or LibreOffice renderer available — on top of the deck itself being stale. |
| 20 | Official slide count / template | **NOT APPLICABLE** | Neither is specified anywhere in the repository. |

## The blocked items, stated plainly

`LogInsight_Final_Project_Presentation.pptx`, `LogInsight_Final_Project_Report.docx` and
`LogInsight_Final_Project_Report.pdf` **were not regenerated and were not edited.** This environment has
no Microsoft Office, no LibreOffice, no Pandoc and no `python-docx`/`python-pptx`. Their embedded
screenshots are raster images that cannot be substituted without a renderer.

They therefore still depict the pre-Atmospheric-Signal interface and pre-revision test counts
(878 backend / 61 frontend / 9 browser workflows), while the product now measures 910 / 64 / 15-defined.
**This is an open submission defect, not a documentation gap.** It needs a machine that can open and
edit these formats.

## Evidence removed this revision, with the reason

| Item | Action | Reason |
|---|---|---|
| `docs/images/overview.png`, `topology-2d.png`, `topology-3d.png`, `live-monitor.png`, `tablet-overview.png`, `mobile-overview.png`, `dataset-analysis.png`, `incident-workbench.png`, `algorithm-evidence.png`, `run-summary.png`, `scenario-lab.png` | **Deleted** | Pre-redesign screenshots of a superseded UI. A repository-wide content search across `*.md`, `*.tsx`, `*.ts`, `*.java`, `*.json`, `*.yml`, `*.css`, `*.html`, `*.mjs` (excluding `node_modules`) returned **zero matches** for every filename. `README.md` and `docs/13-testing.md` referenced the *directory* generically; `README.md`'s only specific image link points at `docs/images/signal-atlas/command-center-desktop.png`, which is untouched. No link broke. |
| `docs/images/capture-report.json` | **Deleted** | A generated report marked `"historical": true` whose entire `taken` list is the eleven captures above. It was orphaned by their removal. Its single literal reference, in `docs/archive/POST_REBUILD_AUDIT.md`, was updated. |
| `docs/images/klh-logo.png` | **Kept** | A brand asset, not a screenshot, and outside the removal set. The live asset used by the product is `frontend/public/klh-logo.png`. |
| `frontend/src/styles/signal-in-motion.css` | **Kept (flagged)** | DEAD — imported by nothing, and the last home of the retired `#315cf5` accent. It is source code and outside documentation ownership; it needs the source owner's decision. |
| `docs/images/signal-atlas/guided-presentation-mobile.png` | **Deleted** | Leftover from an earlier capture script. |

## Local run and demonstrate

`README.md` setup and testing sections; `docs/13-testing.md`; `docs/DEMO_GUIDE.md`; `docs/PROJECT_WALKTHROUGH.md`. Backend `mvnw.cmd spring-boot:run` on :8080 plus `npm run dev` on :5173, with the Vite proxy forwarding `/api`.
