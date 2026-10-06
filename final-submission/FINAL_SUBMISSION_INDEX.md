# LogInsight — Final Submission Index

**Project:** LogInsight — Real-Time Log Intelligence & Incident Investigation Platform
**Course:** Data Structures and Algorithms-3 (25CS2103E) · 2026-2027, Odd Semester
**Repository:** https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer
**Branch:** `main`

| Reference | Commit |
|---|---|
| **Content verification commit** | `53814c1cfc7f762641a6b51ab2c0ea603e1181ba` |
| **Submission-index commit** | the commit that carries this file (reported in the push output; a commit cannot contain its own hash) |

The content verification commit is the SHA the verification figures below were measured against, and
the commit GitHub Actions ran green on. Everything after it is this index file alone.

## Student

| Name | Roll Number | Programme |
| --- | --- | --- |
| KARKALA SHIVA REDDY | 2520030105 | B.Tech Computer Science and Engineering · graduating 2029 |
| PARIPALLI NAVADEEP | 2520030196 | See the verification note below |

Institution: Koneru Lakshmaiah Education Foundation (KL University), KLH Campus, Guntur.
Course instructor: Dr. J Sirisha Devi, Professor, Department of Computer Science and Engineering.

> **UNVERIFIED — needs a human check before submission.** Nothing in this repository can confirm a
> student roster, a course code, a section or a submission date. The two names and the two roll numbers
> above are carried from the institutional report title page
> (`LogInsight_Final_Project_Report.docx`), not derived from code. Course code `25CS2103E` is likewise
> carried from that document and from the presentation. Confirm all of these against the official
> registration record. A previous revision of this index removed the second name on the grounds that it
> was not repository-verifiable; that was too strict, because the institutional report is itself
> evidence, so the row has been restored and flagged instead of deleted.

## Deliverables in this directory

| File | Description |
| --- | --- |
| `LogInsight_Final_Project_Presentation.pptx` | Presentation covering the problem, the classical algorithms used in the product pipeline, architecture, real-time behaviour, interface, verification and limitations. |
| `LogInsight_Final_Project_Report.docx` | Full project report built on the institutional PBL template: case study, algorithm and pseudocode, code excerpts and results with captured screenshots. |
| `LogInsight_Final_Project_Report.pdf` | PDF rendering of the same report. |

Screenshots referenced by the report live in `docs/images/`; the ones also used in the README are
reproduced in the README visual section.

The `.pptx`, `.docx` and `.pdf` in this directory were synchronized before the documentation pass: their
figures read **877** backend tests and **55** frontend tests, the 11 embedded screenshots are the
current ones, and they carry none of the stale claims the pass corrected. That synchronization required
no regeneration by this pass, so the three files are preserved exactly as verified.

**The submitted documents therefore show 877, while the repository now runs 878.** The difference is one
test added afterwards, `AlgorithmCatalogTest.publishedCatalogueFiguresMatchTheImplementation`, which
pins the 42 / 36 / 35 / 13 catalogue figures this README publishes so they cannot drift from the code
again. No product code changed. Regenerate the three documents only if the institute requires the
report to state the live test count.

## Verification figures

Measured on the content verification commit. Local runs and the GitHub Actions run are recorded
separately because they are separate records.

| Gate | Command | Where | Result |
| --- | --- | --- | --- |
| Backend | `mvn -B verify` | GitHub Actions, run 36963789589 | **877 tests**, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS |
| Frontend tests | `npm test` | GitHub Actions, run 36963789589 | **17 test files passed** |
| Frontend build | `npm run build` | GitHub Actions, run 36963789589 | Succeeds |
| Backend | `.\mvnw.cmd -o verify` | local, pre-documentation commit | **877 tests**, 0 failures, 0 errors, 0 skipped |
| Frontend tests | `npm test` | local | **55 tests across 17 files**, 0 failures |
| TypeScript + build | `npm run build` | local | Clean (`tsc && vite build`) |
| Backend | `.\mvnw.cmd -o verify` | local, after adding the catalogue-count guard | **878 tests**, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS |

The workflow is `.github/workflows/ci.yml`: the backend job runs `mvn -B verify`; the frontend job runs
`npm ci`, `npm test` and `npm run build`. The local and workflow figures agree. Run 36963789589 is the
run for the pre-documentation commit; the documentation pass changed no code, so the test and build
results are unchanged, and the run for the final HEAD is reported in the push output.

## Browser verification

Verified in Chromium against the running application on this commit, separately from the unit tests.
The demo used the deterministic `checkout-5xx-cascade` scenario, driven through real navigation.

- **0** uncaught exceptions and **0** application console errors across all 20 routes.
- **0** React duplicate-key warnings. Generated simulation events now carry a deterministic unique id,
  so streamed tables no longer collapse generated rows onto one key.
- **0** failing API requests and **0** asset 404s on any product route. The only aborted requests are
  in-flight fetches and the SSE stream cancelled by navigation, which is the intended cleanup.
- **0** horizontal overflow at 1440x900, 1280x800, 1024x768, 768x1024 and 390x844, across Overview,
  Scenario Lab, Live Monitor, Incident Workbench, Logs, Services and Algorithm Lab.
- **3** consecutive 2D-to-3D-to-2D cycles held exactly **1** canvas each time and left **0** canvases
  after returning to 2D: no renderer duplication, no leaked renderer.
- Live demonstration confirmed: an incident opens about 6 s into the run at speed 1; the Overview shows
  the measured error rate, p95, throughput, phase and open-incident count; the Incident Workbench
  shows the DETECTED lifecycle state, the timeline, the blast radius and the algorithm evidence, and
  its lifecycle controls call the backend.
- Screenshots in `docs/images/` are captured from this build; `docs/images/capture-report.json`
  records the same run.

> Contrast ratios, focus-indicator thickness and target sizes were measured in an earlier pass and are
> carried forward from it. This pass did not re-measure them, so they are not re-asserted as fresh
> evidence.

## DSA contribution

Three classifications are used, and no algorithm is described as a product capability unless a
product path actually calls it. The counts below are the values returned by `GET /api/modules` and
`GET /api/health/status` on this commit.

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
- **No false confidence.** Health bands are fixed heuristic thresholds, not a trained model. Incidents are elevated-error windows with no confidence score, because none is computed. Blast radius is graph reachability, not a confirmed root cause.
- **No generated ML.** There is no trained model, no inference and no prediction anywhere in the product. Pattern extraction is token normalization; detection is thresholding plus multi-pattern matching.

## Limitations

In-memory single-JVM state with no durable storage; restart discards the dataset, any running stream
and any session incident lifecycle. Simulation traffic is server-generated, not captured. Incident
lifecycle state lives in the session only and is never written to the dataset. Run history, event
buffers and trace steps are bounded. Docker Compose files were validated for syntax only, because no
container runtime was available in the development environment — image builds and container smoke
tests were not run and are not claimed.