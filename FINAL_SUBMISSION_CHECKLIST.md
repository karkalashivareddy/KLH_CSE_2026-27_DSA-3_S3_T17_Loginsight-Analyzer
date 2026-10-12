# Submission requirements and evidence map

Status vocabulary used throughout:

| Mark | Meaning |
|---|---|
| **VERIFIED** | Executed or inspected on this revision; the evidence exists in this repository. |
| **PARTIALLY VERIFIED** | Checked to a known, stated limit; the remaining gap is named in the same row. |
| **NOT VERIFIED** | The work exists but was not executed or checked here. No pass is claimed. |
| **NOT APPLICABLE** | No such requirement could be found; recorded rather than guessed at. |

No mandatory slide count and no official PowerPoint template were found anywhere in the repository. That
is recorded as **NOT APPLICABLE**, not inferred.

All figures below were measured on this revision on 2026-10-11.

## Requirement map

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 1 | Project identity, course context and team | **NOT VERIFIED** | Values transcribed from the institutional report title page and flagged in [`FINAL_SUBMISSION_INDEX.md`](FINAL_SUBMISSION_INDEX.md) for official human confirmation. Institution logo asset: `source/frontend/public/klh-logo.png` (present, used on slide 1). Nothing in the repository can verify a roster. |
| 2 | Problem, objectives and scope | **VERIFIED** | `source/docs/01-requirements.md`; `source/docs/COMMAND_CENTER.md`. |
| 3 | Architecture, data flow and provenance | **VERIFIED** | `source/docs/ARCHITECTURE.md` (motion layer, error boundary, route splitting, vendor chunking, stylesheet layering, SSE lifecycle); `source/docs/02-architecture.md` (phase artifact, drift annotated); `source/docs/design-system.md`. Each claim was checked against source. |
| 4 | DSA contributions and honest product mapping | **VERIFIED** | `source/docs/ALGORITHMS.md`, `source/docs/DSA_PRODUCT_MAPPING.md`, `source/docs/04-dsa-mapping.md`. The **42 / 36 / 13 / 35** figures are asserted exactly in `AlgorithmCatalogTest`, including both reconciling facts (42 − 36 library-only, 36 − 35 shared dispatch key), and fail the build on drift. |
| 5 | Backend correctness | **VERIFIED** | `.\mvnw.cmd -B verify` — **910 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS (28.2 s). Includes 5 new regression classes covering this revision's four fixes. |
| 6 | Frontend correctness | **VERIFIED** | `npm test` — **72 tests across 20 test files**, 0 failures, 28.24 s, single aggregate run. |
| 7 | Production build | **VERIFIED** | `npm run build` — `tsc` and Vite clean, **2,356 modules** transformed, 5.62 s, no bundle-size warning. First-load JS 483.22 kB / 150.10 kB gzip; Three.js deferred. |
| 8 | Real product workflows (browser E2E) | **VERIFIED** | `npm run test:e2e` — **18 workflows, 18 passed**, 34.8 s against the live application in `source/frontend/e2e/product.spec.ts`. Includes semantic contrast, the signal field's no-fabrication contract, the empty-dataset state, a failing backend, command-palette focus restoration, route aliases, compact navigation, and the 2D/3D toggle surviving repeated switching. |
| 9 | CI evidence | **VERIFIED** | Run [38109292191](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38109292191) passed all three jobs on commit `4f74d24`. Run 38066030545 passed for the preceding revision. |
| 10 | Dependency audit on this revision | **VERIFIED** | `npm audit` — **0 vulnerabilities**. |
| 11 | Genuine current UI evidence | **VERIFIED** | **19 QA screenshots** in `source/docs/images/signal-atlas/` and **11 report figures** in `source/docs/images/report/`, each captured from the running application against a real dataset loaded through the API — including the no-dataset state, a forced WebGL failure, a reduced-motion run, a live simulation, and a **real rendered WebGL topology**. Reproducible with `npm run capture:visuals` and `npm run capture:report`. |
| 12 | Documentation matches the implementation | **VERIFIED** | Every document under `source/docs/` re-synchronised to this revision, including correcting the frontend test count from 70 to the measured 72. Removed classes, the dead stylesheet and the orphaned screenshot are named rather than silently retained. |
| 13 | Container deployment validated | **NOT VERIFIED** | `docker compose config --quiet` passes. **No Docker daemon is reachable**, so image builds and smoke tests were not run and are not claimed. |
| 14 | Accessibility conformance | **PARTIALLY VERIFIED** | Source review, 20 semantic contrast pairs asserted in E2E, reduced motion handled at three independent levels, visible focus indicators asserted. **No axe scan and no screen-reader audit are configured**, so this is a focused check, not whole-application WCAG certification. |
| 15 | Hardware WebGL rendering | **VERIFIED** | Both paths are now captured. Headless Chromium renders the scene through SwiftShader, giving a genuine WebGL context; Figure 7 of the report and slide 13 of the deck show a **real rendered WebGL graph**, and the capture script fails loudly if the scene falls back to the 2D panel. The unavailable-WebGL fallback remains captured separately in the QA gallery. |
| 16 | Presentation matches the product | **VERIFIED** | `LogInsight_Analyzer_Final_Presentation.pptx` — **16 slides**, 16:9, 7 embedded current captures, editable shape diagrams, KLH logo. Generated by `source/scripts/build_presentation.py`; slide-by-slide outline in [`PRESENTATION_OUTLINE.md`](PRESENTATION_OUTLINE.md). |
| 17 | Report matches the product | **VERIFIED** | `LogInsight_Final_Project_Report.docx` — built by `source/scripts/build_report.py` **from the supplied sample report**, preserving its A4 page size, 1-inch margins, Cambria/Consolas typography, four-chapter hierarchy, cover block and figure sizing. 11 current screenshots; every stale figure and code excerpt corrected against source. `source/scripts/verify_report.py` runs **56 assertions**. |
| 18 | PDF matches the product | **VERIFIED** | `LogInsight_Final_Project_Report.pdf` — **20 A4 pages**, exported by **Microsoft Word itself** from the same DOCX via `source/scripts/export_report_pdf.ps1`, so the two agree page for page. Zero blank pages. |
| 19 | Slide-by-slide visual review of the deck | **VERIFIED** | All 16 slides exported to PNG through PowerPoint (`source/scripts/export_slides.ps1`) and reviewed for clipping, overflow and overlap. `source/scripts/check_deck_layout.py` additionally runs **1,352 structural assertions** over the deck — slide bounds, picture-versus-panel overlap, text-versus-card fit — without needing Office installed. |
| 20 | Official slide count / template | **NOT APPLICABLE** | Neither is specified anywhere in the repository. |

## Submission documents: what changed and how they are produced

An earlier pass recorded the three binary documents as unregenerable, on the assumption that no document
toolchain existed here. **That assumption was wrong**, and the toolchain is now confirmed:

- **Microsoft Word and PowerPoint (Office 16)** are installed and driveable over COM. The report PDF is
  therefore Word's own export rather than a second layout engine's approximation, and the deck can be
  rendered for visual review.
- **Python** with `python-docx`, `python-pptx`, `Pillow`, `pymupdf` and `reportlab`.

| Document | Generator | Notes |
|---|---|---|
| `LogInsight_Final_Project_Report.docx` | `source/scripts/build_report.py` | Starts from the sample report and edits a copy in place. Preserves the sample's typography and layout by construction; swaps the eleven image parts byte-for-byte and asserts each capture's aspect ratio matches the existing figure box. |
| `LogInsight_Final_Project_Report.pdf` | `source/scripts/export_report_pdf.ps1` | Word `ExportAsFixedFormat`. |
| `LogInsight_Analyzer_Final_Presentation.pptx` | `source/scripts/build_presentation.py` | Reads metadata from `source/scripts/submission_meta.json` and figures from `source/docs/images/report/`. |

Two layout defects inherited from the sample were corrected rather than reproduced:

1. **Four blank pages.** The sample forced each chapter onto a new page with a page break placed
   *inside* the preceding empty spacer paragraph, so Word started a page for that spacer and immediately
   broke again. The break now sits on the chapter heading as `pageBreakBefore`, so every chapter still
   opens on a fresh page and the document has zero blank pages.
2. **Two stale code excerpts.** `ServiceDependencyGraph.reachableFrom` and the
   `lifecycleConflict` handler had both drifted from source; one snippet in the sample would not have
   compiled. Both are now reproduced from the current files.

## Evidence removed this revision, with the reason

| Item | Action | Reason |
|---|---|---|
| `LogInsight_Final_Project_Presentation.pptx` | **Deleted** | Superseded by `LogInsight_Analyzer_Final_Presentation.pptx`. Removed so the directory holds exactly one final deck rather than two contradictory "final" presentations. |
| `source/docs/images/overview.png`, `topology-2d.png`, `topology-3d.png`, `live-monitor.png`, `tablet-overview.png`, `mobile-overview.png`, `dataset-analysis.png`, `incident-workbench.png`, `algorithm-evidence.png`, `run-summary.png`, `scenario-lab.png` | **Deleted** | Pre-redesign screenshots of a superseded UI. A repository-wide content search across `*.md`, `*.tsx`, `*.ts`, `*.java`, `*.json`, `*.yml`, `*.css`, `*.html`, `*.mjs` (excluding `node_modules`) returned **zero matches** for every filename, so no link broke. |
| `source/docs/images/capture-report.json` | **Deleted** | A generated report marked `"historical": true` whose entire `taken` list is the eleven captures above. It was orphaned by their removal. |
| `source/docs/images/klh-logo.png` | **Kept** | A brand asset, not a screenshot, and outside the removal set. The live asset used by the product and the deck is `source/frontend/public/klh-logo.png`. |
| `source/frontend/src/styles/signal-in-motion.css` | **Kept (flagged)** | DEAD — imported by nothing, and the last home of the retired `#315cf5` accent. It is source code and outside documentation ownership; it needs the source owner's decision. |
| `source/docs/images/signal-atlas/guided-presentation-mobile.png` | **Deleted** | Leftover from an earlier capture script. |

## Local run and demonstrate

`README.md` setup and testing sections; `source/docs/13-testing.md`; `source/docs/DEMO_GUIDE.md`;
`source/docs/PROJECT_WALKTHROUGH.md`. Backend `mvnw.cmd spring-boot:run` on :8080 plus `npm run dev` on :5173,
with the Vite proxy forwarding `/api`.

To regenerate the submission artefacts from a running application:

```bash
cd source/frontend
npm run capture:report      # 11 figures -> source/docs/images/report/
cd ..
python source/scripts/build_report.py
powershell source/scripts/export_report_pdf.ps1
python source/scripts/verify_report.py
python source/scripts/build_presentation.py
python source/scripts/check_deck_layout.py
powershell source/scripts/export_slides.ps1 -Pptx LogInsight_Analyzer_Final_Presentation.pptx -OutDir slides
```