# LogInsight — Final Submission Index

**Project:** LogInsight — Real-Time Log Intelligence and Incident Investigation Platform
**Course:** Data Structures and Algorithms-3 (25CS2103E) · 2026-2027, Odd Semester
**Repository:** https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer
**Branch:** `main` · **Commit:** `c31cf8b`

## Team

| Name | Roll Number |
| --- | --- |
| KARKALA SHIVA REDDY | 2520030105 |
| PARIPALLI NAVADEEP | 2520030196 |

Course instructor: Dr. J Sirisha Devi, Professor, Department of Computer Science and Engineering.

## Deliverables in this directory

| File | Description |
| --- | --- |
| `LogInsight_Final_Project_Presentation.pptx` | 16-slide presentation covering the problem, the classical algorithms used in the product pipeline, architecture, real-time behaviour, interface, verification and limitations. |
| `LogInsight_Final_Project_Report.docx` | Full project report built on the institutional PBL template: case study, algorithm and pseudocode, code excerpts and results with 11 captured screenshots. |
| `LogInsight_Final_Project_Report.pdf` | PDF rendering of the same report (24 pages). |

Screenshots referenced by the report live in `docs/images/` and are reproduced in the README visual section.

## Verification figures

These are the figures produced by the last continuous-integration run on `main`, not estimates.

| Gate | Command | Result |
| --- | --- | --- |
| Backend | `cd backend; .\mvnw.cmd -B verify` | **877 tests**, 0 failures, 0 errors, 0 skipped; jar packaged; JaCoCo produced |
| Frontend tests | `cd frontend; npm test` | **55 tests** across **17 files**, 0 failures |
| TypeScript | `cd frontend; npx tsc --noEmit` | Clean |
| Production build | `cd frontend; npm run build` | Succeeds; 3D scene code-split into a lazy 576 kB chunk |

Continuous integration runs the same three commands on every push to `main`.

## Browser verification

Verified in Chromium against the running application, separately from the unit tests:

- **0** console errors on every route.
- **0** horizontal overflow at 1440x900, 1280x800, 1024x768, 768x1024 and 390x844.
- **0** contrast failures against the 4.5:1 body-text requirement, measured against the resolved effective background.
- Visible 2px keyboard focus indicator on every element reached by tabbing.
- Minimum 10px text; minimum 24x24px targets, growing to 32px on touch viewports.
- Stability: 5x 2D-to-3D switching held exactly one canvas with flat DOM and heap; 3x start/stop kept the buffer bounded at 40 rows; 12 route changes produced no DOM growth and no leaked canvases.

## DSA contribution

Algorithms running in the product pipeline, not only in a demonstration catalogue:

| Algorithm | Complexity | Role |
| --- | --- | --- |
| Knuth-Morris-Pratt | O(n + m) | Default phrase search; dominant-signature location in the live detector |
| Aho-Corasick | O(n) | Per-record multi-signature labelling from one pass |
| Levenshtein | O(nm) | `did you mean` suggestion ranking over the message corpus |
| Suffix array + Kasai LCP | O(n log n) | Traceable phrase search |
| BFS reachability | O(V + E) | Blast radius over the observed service graph |
| Fixed-window detection | O(n + W) | Baseline-relative elevated-error windows merged into incidents |
| Reservoir sampling | O(n) | Bounded stream sample |

A wider repertoire (bitmask TSP, Needleman-Wunsch, Dinic flow, vertex-cover kernelization, parallel prefix scan, Miller-Rabin, perfect hashing and more) is exposed as individually runnable, traceable implementations in the Algorithm Lab. The System page reports which algorithms are exposed through an API and which are traceable, so the boundary is explicit.

## Honesty constraints

- **Provenance.** Dataset replay and server-generated simulation are labelled separately at every surface; simulation is never presented as captured telemetry.
- **Topology.** Dataset surfaces show `OBSERVED DEPENDENCIES` from request-trail adjacency; simulation surfaces show `DECLARED DEPENDENCIES` from the scenario graph.
- **No false confidence.** Health bands are fixed heuristic thresholds, not a trained model. Incidents are elevated-error windows with no confidence score, because none is computed. Blast radius is graph reachability, not a confirmed root cause.

## Limitations

In-memory single-JVM state with no durable storage; restart discards the dataset and any running stream. Simulation traffic is generated. Run history, event buffers and trace steps are bounded. Docker Compose files were validated for syntax only, because no container runtime was available in the development environment.