# Final Release Readiness — LogInsight

**Scope of this pass:** release hygiene and repository freeze only. No new feature work, no redesign,
no refactor of verified code, no API changes. The simulation subsystem, route surface, and design
system were treated as frozen and reviewed, not rewritten.

**Outcome:** the branch was subsequently committed and pushed to `origin/main`, and GitHub Actions ran
green on it. The "nothing committed" wording that earlier appeared here was true only of the freeze pass
that produced this document and was stale once the branch was released; see
[final-submission/FINAL_SUBMISSION_INDEX.md](../../final-submission/FINAL_SUBMISSION_INDEX.md) for the
authoritative commit and CI references.

---

## 1. Release summary

| Item | Result |
| --- | --- |
| Backend suite | 877 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS |
| Frontend suite | 17 files, 55 tests, all passed |
| TypeScript | `npx tsc --noEmit` clean, exit 0 |
| Production build | `npm run build` success |
| Tracked changes | 28 modified or deleted files |
| Untracked files | 37 files, all classified as required |
| Files staged | All required release files staged; no commit made |
| Blockers | None |
| Secrets found | None |
| Generated artifacts tracked | None |

## 2. Verification commands and results

| Check | Command | Result |
| --- | --- | --- |
| Backend full suite | `cd backend; ./mvnw.cmd -o test` | Tests run: 877, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS |
| Frontend suite | `cd frontend; npm test` | Test Files 17 passed, Tests 55 passed |
| Frontend typecheck | `cd frontend; npx tsc --noEmit` | exit 0, no diagnostics |
| Production build | `cd frontend; npm run build` | built successfully (~4.9 s) |
| Whitespace/conflict check | `git diff --check` | clean, no whitespace errors, no conflict markers |
| Route smoke | 15 SPA routes via `http://localhost:5173` | all HTTP 200 |
| API smoke | `/api/health`, `/api/health/status`, `/api/live/status`, `/api/simulation/status`, `/api/scenarios`, `/api/analysis/algorithms`, `/api/overview`, `/api/logs/explore`, `/api/analytics/dependencies`, `/api/incidents` | all 200 |
| Rendered-DOM spot check | 14 routes, 1440x900 | `h1` present on every route, no request-error text, no horizontal overflow, 0 runtime errors |

## 3. What changed in this hygiene pass

This pass deliberately made only five kinds of change, all non-behavioural:

1. **`.gitignore`** — added `coverage/` and `.vite/`. Both are real generated-output locations for
   this toolchain (Vitest coverage output, Vite dependency cache). Neither pattern can match a
   source path. Nothing else was added, and no existing rule was weakened.
2. **`README.md`** — added the problem statement, an end-to-end workflow, the technology stack, the
   two data modes with correct `DECLARED` / `OBSERVED` terminology, current-implementation scope, and
   the DSA-3 relationship with evidence-backed algorithm attribution.
3. **`docs/design-system.md`** — corrected sidebar labels to the shipped navigation and documented
   the two data modes and topology provenance rules.
4. **Report corrections** — fixed stale frontend test counts in `FINAL_IMPLEMENTATION_REPORT.md`
   (49 → 50 → 55, with per-file counts verified against Vitest JSON output) and normalised
   `FINAL_VISUAL_QA_REPORT.md` to exactly 20 numbered sections.
5. **One flaky test fixed** — see section 3a. This is the only test file touched.

No application source file was modified in this pass.

### 3a. Flaky test found and fixed

**Symptom.** The full frontend suite passed in a clean shell, then failed with
`Unable to find an accessible element with the role "link" and name "/Investigate/"` once a Spring
Boot backend was left running on port 8080 during route smoke testing.

**Root cause.** `SimulationBand.test.tsx` waited on `findAllByText('Checkout 5xx cascade')` before
asserting the workbench link. That string is ambiguous: it is rendered from `scenario?.title`
(`SimulationBand.tsx:57`, populated by `api.scenarios`) *and* from the open incident
(`SimulationBand.tsx:154`, populated by `api.simulationSample`). These are two independent async
calls, so the scenario title can resolve and satisfy the wait before the incident has rendered its
`Investigate` link (`SimulationBand.tsx:177`). The extra CPU load of a running JVM widened that
window enough to make the failure reproducible.

**Fix.** Wait for the assertion target itself (`await screen.findByRole('link', { name: /Investigate/ })`)
and resolve the accompanying text with `findByText`. This is a test-only change; no product code
was touched, and the assertions were not weakened — the same `href` and separation-copy checks still
run.

**Proof.** With a backend deliberately left running on 8080, the previously failing test passed 3
consecutive times, and the full suite passed 17 files / 55 tests. It also passes with no backend
running. The test is no longer environment-dependent.

**Why this mattered for release.** A suite that passes only when nothing else is running on the
machine is not a trustworthy release gate.

## 4. Data-source separation (product honesty)

The distinction is enforced in code, labelled in the UI, and now documented:

| Surface | Source | Label shown | Provenance |
| --- | --- | --- | --- |
| `/live`, `/scenario-lab` | Deterministic generated simulation | `GENERATED` | `DECLARED DEPENDENCIES` |
| `/replay` | Bounded dataset replay | `Demo replay: a bounded SSE replay` | `OBSERVED DEPENDENCIES` |
| `/logs`, `/services`, `/patterns`, `/analytics` | Loaded dataset | dataset-derived | `OBSERVED DEPENDENCIES` |

Simulation topology is generated from scenario definitions, so calling those edges "observed" was
a false claim. That defect was found in visual QA and fixed by adding a source-aware `graphKind`
prop to `TopologyPanel` and `Topology3D`. Five surfaces on three pages were corrected.

## 5. Algorithm claim verification

Every algorithm claim in the documentation was traced to code that is actually invoked. No claim
was added that the code does not support.

| Claim | Verification | Verdict |
| --- | --- | --- |
| KMP recounts the dominant error signature in simulation evidence | `SimulationDetector` imports and calls `com.loginsight.dsa.string.kmp.KMPMatcher` | Supported |
| Aho-Corasick performs multi-pattern error-signature matching | `SimulationDetector` imports and calls `com.loginsight.dsa.string.aho.AhoCorasick`, building a signature automaton | Supported |
| Rolling sliding-window aggregate drives burst detection | `simulation/RollingWindow.java` + `BurstDetector`, operating on consecutive log windows | Supported |
| BFS computes incident blast radius over the dependency graph | `SimulationTopology` traverses the declared graph breadth-first | Supported |
| Levenshtein / weighted edit distance | Pre-existing `dp/editdistance` implementation, already mapped before this pass | Supported |
| No AI/ML, no model inference, no LLM anywhere in the product | No ML dependency in either manifest; no inference code path | Supported |

The DSA-3 mapping in `docs/DSA_PRODUCT_MAPPING.md` and `docs/04-dsa-mapping.md` was read against the
source. Classifications used by the app remain the approved three: algorithm engine / analytical
capability, product feature, and academic lab / demonstration. No category was broadened to make a
feature look more academic.

## 6. Frontend dependency hygiene

| Dependency | Used in source | Verdict |
| --- | --- | --- |
| `react`, `react-dom` | Yes | Required |
| `react-router-dom` | Yes, `App.tsx` route table | Required |
| `clsx` | Yes | Required |
| `recharts` | Yes, chart surfaces | Required |
| `three` | Yes, `Topology3D.tsx` | Required |
| Dev: `vite`, `@vitejs/plugin-react`, `typescript`, `vitest`, testing-library, `@types/*` | Yes | Required |

No unused production dependency, no dependency added, and no version range widened by this pass.
`frontend/package-lock.json` is tracked and consistent with `frontend/package.json`.

## 7. Backend dependency hygiene

`backend/pom.xml` declares only `spring-boot-starter-web`, `spring-boot-starter-test`, and the
JaCoCo reporting plugin. All are used. No dependency was added, removed, or upgraded. Java 21 is
required and declared.

## 8. Generated-artifact hygiene

| Location | Status |
| --- | --- |
| `frontend/dist/` | Present, ignored, not tracked |
| `frontend/node_modules/` | Present, ignored, not tracked |
| `backend/target/` (classes, surefire reports, `site/jacoco/`) | Present, ignored, not tracked |
| `*.log` | Ignored; smoke-test logs created during this pass were deleted from the working tree |
| `coverage/` | Now ignored (added this pass); not currently present |
| `.vite/` | Now ignored (added this pass); not currently present |
| Heap snapshots, screenshots, QA scripts, browser profiles | None in the repository |

JaCoCo output is written to `backend/target/site/jacoco/`, which the pre-existing `target/` rule
already covered, so no additional rule was needed for it.

## 9. Secret and sensitive-data scan

All 37 untracked files and all 28 modified tracked files were scanned for AWS keys, private-key
blocks, GitHub/Slack/Google tokens, JWTs, bearer/basic auth headers, literal passwords, JDBC
connection strings with credentials, and `.npmrc` auth entries. No matches.

- `.env.example` is intentionally tracked and contains only `BACKEND_PORT=8080`,
  `FRONTEND_PORT=8088`, and `COMPOSE_PROJECT_NAME=loginsight`. No real secret.
- Real `.env` files are ignored and none are present in the working tree.
- The first scan pass emitted path errors because the deleted `frontend/src/styles/experience.css`
  was still listed; it was rerun against existing files only, which is the result reported here.

## 10. Machine-specific path and debris scan

Scanned for absolute user paths, temp directories, and QA harness residue:

- `C:\Users\...`, `AppData`, `Temp\opencode`, `qa-prof*`, `/home/`, `/Users/` — **no matches** in any
  changed or untracked file.
- `console.log`, `debugger`, `TODO`, `FIXME`, `XXX`, `HACK` — no genuine matches. The apparent hits
  were false positives: `mapToDouble` contains the substring "todo", and `textHack` /
  `TextHackResponse` are real identifiers for the existing text-similarity laboratory endpoints.
- Hardcoded `localhost:<port>` appears only in `README.md`, where documenting `8080` and `5173` is
  correct and intentional. No port is hardcoded in application source.
- Conflict markers — none.

## 11. Untracked file classification (all 37 files)

Every untracked file was opened and classified. None is disposable.

| Group | Files | Purpose |
| --- | --- | --- |
| Backend simulation service | `LiveSimulationService.java` | Session lifecycle, SSE frame production |
| Backend REST surface | `SimulationController.java`, `SimulationTransitionRequest.java` | Simulation endpoints and operator actions |
| Backend simulation DTOs | `ScenarioDto`, `SimulationFrameDto`, `SimulationIncidentDto` | Wire contract |
| Backend simulation engine | 12 files in `simulation/` | Scenarios, deterministic RNG, rolling window, burst detection, Aho-Corasick + KMP evidence, incident lifecycle, topology |
| Backend tests | 3 files in `simulation/` | Determinism, detection, lifecycle |
| Frontend operator surfaces | `MonitorPage`, `ScenarioLabPage`, `IncidentWorkbenchPage` (+ tests) | The three new operator routes |
| Frontend shared stream layer | `telemetry/` context, band, adapters (+ tests) | Single owner of the simulation stream |
| Frontend design system | `styles/product.css` | Replaces the deleted `experience.css` |
| Release documentation | `FINAL_IMPLEMENTATION_REPORT.md`, `FINAL_VISUAL_QA_REPORT.md`, this file | Evidence records |

`frontend/src/styles/experience.css` is deleted and `product.css` added as a deliberate
consolidation. No import of `experience.css` remains anywhere; `main.tsx` imports `product.css`.

## 12. Intentional deletions and replacements

| Change | Justification |
| --- | --- |
| `frontend/src/styles/experience.css` deleted | Superseded by `product.css`; zero remaining imports confirmed by repository-wide search |
| Dataset replay moved off `/live` onto `/replay` | Prevents a bounded replay from being presented as real-time traffic. No capability was removed; the capability is still reachable and better labelled |
| Extra `## 21. Final counts` heading demoted | Report specified 20 numbered sections; content preserved verbatim under an unnumbered heading matching the existing `## Presentation checklist` convention |

## 13. Documentation consistency review

| Document | Check | Result |
| --- | --- | --- |
| `README.md` | Problem, workflow, stack, run instructions, data modes, DSA-3 mapping | Complete and accurate; sidebar labels verified against the live DOM |
| `docs/design-system.md` | Navigation names, data modes, topology provenance | Consistent with shipped behaviour |
| `docs/DSA_PRODUCT_MAPPING.md`, `docs/04-dsa-mapping.md` | Algorithm-to-code traceability | Consistent with source |
| `FINAL_IMPLEMENTATION_REPORT.md` | Frontend test counts | Corrected 49 → 50 → 55 in four places; per-file counts verified against Vitest JSON |
| `FINAL_VISUAL_QA_REPORT.md` | Section count | Normalised to exactly 20 numbered sections plus the checklist |
| Encoding | UTF-8 integrity across all docs | Clean; apparent mojibake was a PowerShell console rendering artifact, verified absent from file bytes |

The README's navigation list was validated against the rendered sidebar, which returns exactly:
`Overview, Scenario Lab, Live Monitor, Dataset Replay, Logs, Detector Windows, Incident Workbench,
Services, Patterns, Analytics`.

## 14. Route surface verification

All 15 primary SPA routes return HTTP 200, and 14 routes were additionally checked in a real
rendered DOM at 1440x900. Every route exposes a `main` landmark and a unique `h1`; no route showed
request-error text and none overflowed horizontally.

One investigation is worth recording: `/replay` initially appeared to render blank with no `h1`. On
isolated re-test it rendered correctly with `h1: "Dataset replay"`. The cause was the Vite dev server
cold-compiling that route on first navigation inside a fixed wait window, not an application defect.
No source change was made as a result.

## 15. Accessibility state

Accessibility was audited in visual QA and is unchanged by this pass:

- Skip link, `main` landmark, labelled navigation, and one `h1` per route.
- Colour is never the sole signal; status pills carry text and icons.
- Content labels are at least the 10px floor after eight rules were raised.
- Focus rings retained across interactive controls.
- `prefers-reduced-motion` yields zero infinite animations.
- Mobile touch targets were corrected: 111 under-32px controls on `/logs` reduced to 7.

Known limitation, not a defect: the second automated contrast probe computed implausible background
ratios because it failed to account for stacked background layers. That probe was discarded. The
reliable probe reported zero low-contrast items on `/`, `/overview`, `/logs`, and `/services`, and
`--text-faint` on `--bg-elevated` was verified manually at 4.81:1. No screenshot-based pixel review
was available in this environment.

## 16. Deliberately out of scope

These are documented limitations of the shipped product, not release blockers, and were not
"fixed" by changing working code:

- Topology particles drift at constant speed rather than traffic-proportional speed.
- The topology view mode (2D / 3D) is not persisted across navigation.
- Mobile window-control state lives in route state only.
- `npm run preview` has no API proxy; the documented local workflow is `npm run dev`, which does.
  This is pre-existing upstream Vite behaviour, not a regression.

## 17. Remaining risks

| Risk | Severity | Assessment |
| --- | --- | --- |
| No screenshot or human visual review | Medium | All checks were programmatic (geometry, computed styles, contrast, network, console). A human should still eyeball the three operator surfaces before submission |
| Single-machine, single-dataset verification | Low | Demo dataset exercised; no second dataset or Windows/Linux differential run |
| Uncommitted work | Low | Everything is staged but uncommitted by design, so a careless `git checkout`/`reset` could lose it |
| Line-ending normalisation | Low | Git reports `LF will be replaced by CRLF` on 25 files. Content is unaffected; add a `.gitattributes` only if the team wants to pin it |

No risk rises to a release blocker.

## 17a. Release-freeze inspection findings

The final freeze pass re-exercised the running application and classified every observation. **No
release blocker was found, and no application source was modified.**

### Verified working

| Check | Evidence |
| --- | --- |
| Simulation genuinely live, not static | Error rate `0.00% → 12.61% → 17.81% → 18.84% → 19.87% → 20.57%`; tick `12 → 44 → 75 → 106 → 137 → 176`; p95 `200 ms → 5.2k ms`; incident count `0 → 1` |
| Algorithm evidence appears with detection | KMP and Aho-Corasick evidence present; `GENERATED` / `LIVE SIMULATION` labelling truthful |
| Incident lifecycle end to end | Observed live: `INVESTIGATING` (t+6s) → `MITIGATED` (t+36s) → `RESOLVED` (t+48s), with `DETECTED` retained in the timeline |
| Workbench investigation hierarchy | `H1 Incident workbench` plus `Timeline`, `Algorithm evidence`, `Blast radius`, `Topology`, `Correlated events`, `Current frame evidence` |
| No unsupported AI / root-cause claim | Only occurrence is the hedge "Heuristic signal, not a proven root cause."; blast radius is labelled as simulation-derived |
| No data loss in the event table | All 40 rows rendered on every sample; repeated-looking rows are distinct events whose millisecond timestamps collapse under `toLocaleTimeString()` |
| Start / Stop controls | `Start stream` and `Start run` begin the stream; `Stop` freezes the tick and restores the idle control set |
| Responsive integrity | 0 horizontal overflow on `/`, `/live`, `/incidents/workbench` at 1440×900, 1024×768 and 390×844; `h1` and `main` present on all three at all three widths |
| Typography floor | 0 elements below the 10px floor in the settled state across all 10 audited routes (re-measured: no stylesheet declares a font-size below 10px) |
| Uncaught exceptions | None, anywhere |

### B. Non-blocking cosmetic issues (no code changed)

1. **React duplicate-key warnings during live streaming — FIXED, no longer outstanding.** This pass
   originally recorded `ScenarioEventFactory` assigning `id(-1)` to every generated event, with the
   event tables keying rows on `key={event.id}`, which collapsed generated rows onto one key and logged
   1785 warnings on `/scenario-lab` and 39 on `/live` in a 20-second capture. The defect was real and
   has since been fixed at the source: generated events now carry a deterministic unique id composed of
   the session ordinal and the per-session emission sequence, in a range above any ingested dataset id.
   The frontend key helper namespaces by provenance rather than treating a negative id as a signal. A
   subsequent browser pass on the live stream recorded **0** duplicate-key warnings. See commit
   `ac9a1b5` and [FINAL_SUBMISSION_INDEX.md](../../final-submission/FINAL_SUBMISSION_INDEX.md).
2. **Overview mobile touch targets.** At 390×844 the Overview exposes 11 buttons under 32px tall
   (five time-range chips at 25px, six full-width event rows at 17px). The 10px+ / touch-target work
   was verified on `/logs`, which is what the visual report claims; this is a separate, lesser
   instance on a different page. No overflow or unreachable control results.

### C. Intentional design, not defects

- The generated simulation offers **Start and Stop only**. There is no pause, resume, or
  stream-reset control, and `Reset view` is the topology camera reset. No documentation claims a
  pause control, so this is an unclaimed capability rather than a regression. Adding one is
  feature work and was deliberately not done.
- `Dataset Replay` is a separate surface with its own `Start replay` control.

## 18. Recommendations before submission

1. Have a human open `/`, `/live`, `/scenario-lab`, `/replay`, and `/incidents/workbench` and confirm
   the visual result, since no image review was possible here.
2. Commit with a message that reflects the simulation subsystem, not a generic "update".
3. Decide whether to add `.gitattributes` to pin line endings; otherwise the CRLF notices will
   return on future checkouts.
4. Optionally add `preview.proxy` to `frontend/vite.config.ts` so `npm run preview` works against a
   local backend. This is a convenience change and was intentionally not made during a freeze pass.

## 19. Explicitly not done in the freeze pass

This section records the boundaries of the original freeze pass. It is retained as a historical record of
that pass's scope, not as a statement about the branch today: the branch was committed and pushed to
`origin/main` afterwards, and the duplicate-key defect that the freeze deferred has since been fixed.

- No tag, no branch creation, no history rewrite, and no force push.
- No `git reset --hard`, no `git clean`, no `git checkout` of any file.
- No deletion of legitimate untracked implementation files.
- No modification of any application source file. The single test-file change in section 3a fixes a
  race and weakens no assertion.
- No new feature, no refactor, no API change, no algorithm addition.
- No invented capability, no AI/ML framing, no broadened academic classification.

## 20. Final readiness statement

The repository is **ready for human review and an explicit commit decision**.

Every changed and untracked file is accounted for and justified. No secret, machine-specific path,
generated artifact, debug statement, or conflict marker is present in the release set. The full
backend and frontend suites, the typecheck, and the production build all pass, both in a clean shell
and with a local backend running. Documentation claims were traced to code, two stale report details
were corrected, and one environment-dependent flaky test was found and fixed.

The single genuine caveat is that no human screenshot review was possible in this environment; all
visual and accessibility evidence is programmatic. That is a recommendation for the reviewer, not a
blocker.

A subsequent release-freeze pass re-exercised the running application across the three operator
surfaces and all three target viewports (section 17a). It found **no release blocker**, modified no
application source, and confirmed that the simulation streams genuinely changing telemetry, that the
incident lifecycle advances to `RESOLVED`, and that no unsupported AI or root-cause claim is made.
Two non-blocking cosmetic issues and the absence of a pause control are documented there rather than
changed.

**This freeze pass staged files but did not commit them.** The staged work was subsequently committed and
pushed; only the wording above was stale.
