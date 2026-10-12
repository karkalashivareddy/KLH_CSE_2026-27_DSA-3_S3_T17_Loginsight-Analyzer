# Presentation Outline

`LogInsight_Analyzer_Final_Presentation.pptx` — 16 slides, 16:9, deep-navy canvas with sky/cyan
accents. Regenerate with:

```bash
python source/scripts/build_presentation.py          # writes the .pptx
python source/scripts/check_deck_layout.py           # structural assertions, no Office needed
powershell source/scripts/export_slides.ps1 -Pptx LogInsight_Analyzer_Final_Presentation.pptx -OutDir slides
```

The deck is generated, not hand-edited. Content comes from `source/scripts/submission_meta.json` and the
screenshots in `source/docs/images/report/`, which are captured from the running application by
`npm run capture:report`. Every diagram is built from native PowerPoint shapes, so it stays editable
in PowerPoint after generation.

## Slides

| # | Title | What it establishes | Evidence |
| --- | --- | --- | --- |
| 1 | LogInsight Analyzer | Team, course, instructor, institution; KLH logo | `submission_meta.json`; `source/frontend/public/klh-logo.png` |
| 2 | An outage is never explained by one log line | The problem: the signal exists from the first moment but is diluted across ~14,000 records and 8 services, and plain search is O(nm) | `GET /api/services`, `/api/datasets/demo` |
| 3 | What the project sets out to prove | Five checkable objectives, including provenance honesty and linear-time search | Product surfaces; 910 / 72 / 18 verification gates |
| 4 | Conventional tooling vs LogInsight | The gap is traceability and provenance, not the search box | Product behaviour |
| 5 | Three layers, one deterministic stream | Presentation, services, data and algorithms; one SSE stream owned by the client provider | `source/docs/ARCHITECTURE.md`; source |
| 6 | Technology stack | Pinned versions, plus 6 viewport widths, 36 route declarations, 19 navigation entries | `source/backend/pom.xml`, `source/frontend/package.json`, `App.tsx`, `Layout.tsx` |
| 7 | The structures the algorithms are built from | LPS array, Aho-Corasick trie, edit-distance table, dependency graph, fixed windows, hand-written queue and stack | `com.loginsight.dsa` packages |
| 8 | Five production algorithms, and where the catalogue stops | The five on the production path, badged **PRODUCTION**, against one **STUDY CATALOGUE** entry; 42 / 36 / 13 | `AlgorithmCatalogTest`, `GET /api/algorithms` |
| 9 | A frame is a pure function of scenario, seed and tick | Six-step pipeline from scenario resolution to the SSE payload; delivery speed changes pacing, never content | `ScenarioLab`, `SimulationSession`, `IncidentDetector` |
| 10 | Command center: state and next action above the fold | Compact hero, backend-derived numbers, simulation reported separately from the dataset | `source/docs/images/report/report-01-command-center.png` |
| 11 | Scenario Lab: a run you can reproduce | A run in progress (health elevated, phase peak, one incident open) beside the catalogue | `report-03-scenario-running.png`, `report-02-scenario-catalogue.png` |
| 12 | Incident workbench: evidence, then conclusion | Three-pane investigation, measured evidence, blast radius by traversal, forward-only lifecycle | `report-04-incident-workbench.png` |
| 13 | One graph, two renderers | Accessible 2D SVG map and a **real rendered WebGL scene**; content-keyed effect, `forceContextLoss`, context restoration, pause when off-screen | `report-06-topology-2d.png`, `report-07-topology-3d.png` |
| 14 | Catalogue and analytics | The study catalogue stays visibly separate; aggregates are backend-computed | `report-08-algorithms.png`, `report-09-analytics.png` |
| 15 | What was actually measured | 910 / 72 / 18 / 0 vulnerabilities / build pass, then the five browser checks | `submission_meta.json`; CI run 38109292191 |
| 16 | Limits, next steps and what the project shows | Limitations, future work, and the closing statement | `submission_meta.json` |

## Design notes

- **Canvas** `#0A1623`, panels `#14263A`, accents `#38BDF8` sky and `#22D3EE` cyan, amber `#FBBF24`
  reserved for limitations and study-catalogue items. Type is Segoe UI, with Consolas for complexity
  expressions and structure names.
- **Chrome.** Every content slide carries the same eyebrow, title, accent rule, one-line subtitle and a
  footer with the slide counter. The accent rule sits between title and subtitle so it can never
  collide with body content on a dense slide.
- **The institution logo** is placed on a white chip, because `klh-logo.png` has an opaque white
  background and would otherwise read as a white box on the dark canvas.
- **Screenshots are sized by height** on the two-up slides, not width, so the note row always clears
  the images and their captions.

## Figures

| Deck slide | File | Size |
| --- | --- | --- |
| 10 | `source/docs/images/report/report-01-command-center.png` | 1440x900 |
| 11 | `source/docs/images/report/report-03-scenario-running.png` | 1440x900 |
| 11 | `source/docs/images/report/report-02-scenario-catalogue.png` | 1440x900 |
| 12 | `source/docs/images/report/report-04-incident-workbench.png` | 1440x900 |
| 13 | `source/docs/images/report/report-06-topology-2d.png` | 1440x900 |
| 13 | `source/docs/images/report/report-07-topology-3d.png` | 1440x900 |
| 14 | `source/docs/images/report/report-08-algorithms.png` | 1440x900 |
| 14 | `source/docs/images/report/report-09-analytics.png` | 1440x900 |

`report-07-topology-3d.png` is a real rendered WebGL scene. It is captured through SwiftShader, so
headless Chromium provides a genuine WebGL context; the capture script fails loudly if the scene falls
back to the 2D panel instead.

## Figures not used in the deck

`report-10-tablet-1024.png` and `report-11-mobile-390.png` are used in the written report (Figures 10
and 11) rather than the deck, which has no responsive slide. They are still captured so the report and
the deck draw from one figure set.