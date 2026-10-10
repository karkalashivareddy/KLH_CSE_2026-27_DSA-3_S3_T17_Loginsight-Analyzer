# Testing and Verification

## Current gates

The Atmospheric Signal revision was verified locally with:

```powershell
cd backend
.\mvnw.cmd -o verify

cd ..\frontend
npx.cmd vitest run
npm.cmd run build
```

Results, measured on this revision:

- Backend: **910 tests, 0 failures, 0 errors, 0 skipped** across 108 surefire classes; BUILD SUCCESS.
- Frontend unit: **64 tests passed across 19 test files**, 0 failures, 34.09 s in a single aggregate run. The Vitest worker-start timeout recorded by earlier revisions did not reproduce.
- Frontend build: `tsc` and the Vite production build passed; 2357 modules transformed. `index.css` is 191.81 kB (35.61 kB gzip). **No bundle-size warning is emitted** — `chunkSizeWarningLimit` is 600 and the largest chunk (`vendor-three`) measures 562.23 kB.
- Bundle: first-load JavaScript is **~493 kB raw / ~154 kB gzip** across `index`, `vendor-react`, `vendor-motion` and `vendor-icons`, down from ~1061 kB / ~281 kB before route splitting and vendor chunking. `vendor-three` (562.23 kB, 140.66 kB gzip) is deferred and fetched only when the 3D topology is opened.
- Browser E2E: **15 Playwright workflows** are defined in `frontend/e2e/product.spec.ts`, up from 9. **They were not re-executed on this revision**, so no pass result is claimed for them here.
- Visual capture: `npm run capture:visuals` writes **19 screenshots** into `docs/images/signal-atlas/`.
- Dependency audit: `npm audit` reported 0 vulnerabilities after the React Router 7.18.4 update. **Not re-run on this revision.**
- JaCoCo: report generated under `backend/target/site/jacoco/`; no numeric coverage threshold is configured in the POM, so no coverage percentage is claimed.
- Compose syntax: `docker compose config --quiet` passed on an earlier pass. **Not re-run on this revision.**
- Container image build/runtime: not executed, because no Docker daemon was reachable in the development environment. Image builds and container smoke tests remain unexecuted and unclaimed.

The checked-in CI workflow runs `mvn -B verify`, frontend tests/build, a full npm audit and a browser E2E job that provisions Chromium and starts both services. GitHub Actions [run 38052830711](https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer/actions/runs/38052830711) passed all three jobs for the **previous** commit `c6349c77f3f755c231e1172efec14b01009bf1ef` and reports that commit's counts. **No CI run exists for the current revision.** The frontend job pins Node 22 because Vitest 5 declares `engines: ^22.12.0 || ^24.0.0 || >=26.0.0`.

### Backend regression tests added this revision

| File | Covers |
|---|---|
| `MillerRabinValidationRegressionTest` | Shared `validateArguments`: null mode, null `rng` and non-positive rounds are rejected by both `test` and `testTracked`, closing the path where `testTracked(n, PROBABILISTIC, rng, 0)` reported composites as `PRIME` |
| `DatasetServiceIngestRegressionTest` | `ingest(null, …)` no longer NPEs; the dataset name is normalized before use; an empty stream raises `UnsupportedLogFormatException` whether or not a name was supplied; a null stream raises `IllegalArgumentException` |
| `ParserExceptionMappingTest` | `ParserException` maps to HTTP 400, not 500 |
| `DatasetUploadErrorContractTest` | End-to-end MockMvc contract for `POST /api/datasets`: zero-byte and whitespace-only bodies answer 400 with the parser's own message, an unparsable body answers the standard envelope with no stack trace, and a valid upload still succeeds |
| `RunServiceSseLifecycleTest` | The run-replay emitter registers completion/timeout/error callbacks and stops promptly when the client goes away |

### Guided-demo backend integration smoke

The packaged backend jar was started locally on port `18080` because port `8080` was already occupied. Against this isolated process, the guided-demo sequence successfully loaded the deterministic demo dataset and called the real APIs: 14,000 parsed events; 6,127 `level:ERROR` search matches (the response's algorithm is `null` because this structured filter uses indexes); 12 analytics buckets; 8 services and 56 observed request-trail edges; 6 detector windows; 13 trace-catalogue operations; and 615 events in the returned `1h` overview. These are observations from one local smoke run on an earlier pass, not expected constants or performance claims.

**Local verification and GitHub Actions verification are separate records.** The local commands above run the same goals the workflow runs, but the authoritative gate for a merge is the workflow run on `main`, which reports its own per-job counts. A local green run is not a substitute for a green workflow run, and this document records only what was actually executed.

## Frontend test inventory

| File | Tests | Coverage |
|---|---:|---|
| `src/api/client.test.ts` | 3 | SSE frame parsing — chunked frames, comment lines, event ids, multiline `data:`, and flushing an unterminated final frame. Plus multipart boundary handling. |
| `src/components/AppErrorBoundary.test.tsx` | 3 | **(new)** Children render while nothing throws; a render crash becomes a `role="alert"` panel with "Try again" and "Reload workspace"; no stack trace and no component name reach the DOM. |
| `src/components/format.test.ts` | 8 | Duration, nanosecond and fractional-millisecond formatting, plus the `eventKey` identity contract: dataset ids, deterministic generated ids, provenance namespacing, key stability and the malformed-payload fallback. |
| `src/components/Layout.test.tsx` | 4 | Skip link, grouped navigation entries with active route state, the `Ctrl+K` command palette with focus restoration, and no false "no dataset" claim while status is still loading. |
| `src/components/TopologyPanel.test.tsx` | 11 | Accessible service and edge lists; data-driven node radius and static edge-weight markers; declared-graph wording; observed-vs-window count separation; incident-window marking without causal claims; dense-graph legibility; WebGL fallback; reduced-motion handling; bounded zoom/reset; malformed and excessive graph input; keyboard selection/clear. |
| `src/pages/AlgorithmsPage.test.tsx` | 3 | Algorithm Lab posts the catalogue `defaultInput` to the runs API and navigates to the created session; no run control without an input; a rejected run surfaces an error instead of navigating. |
| `src/pages/AnalyticsPage.test.tsx` | 1 | Analytics tablist semantics: `aria-selected`, `aria-controls`, and a tabpanel whose `id`/`aria-labelledby` follow the selected tab. |
| `src/pages/IncidentWorkbench.test.tsx` | 4 | The detected incident with its measured evidence and blast radius; lifecycle advance follows the server contract rather than a local guess; automatic advancement offered only while open; session-scoped lifecycle stated in the copy. |
| `src/pages/LivePage.test.tsx` | 2 | Dataset replay disclosure and shared `Replay state:` label; start and "Replay again" both open one SSE stream with the selected batch size and pace. |
| `src/pages/LogsPage.test.tsx` | 1 | The explorer applies backend severity syntax and opens the selected event drawer. |
| `src/pages/MonitorPage.test.tsx` | 4 | The stream is labelled generated rather than captured telemetry; measured frame metrics and server evidence are rendered; a deterministic run starts on demand; operators are pointed at Scenario Lab instead of being offered a traffic control that does not exist. |
| `src/pages/OverviewPage.test.tsx` | 3 | Command Center selected-window metrics, dataset coverage, detected-investigation state, the bounded-dataset-replay disclosure with working pattern log links, and the explicit source-selection state. |
| `src/pages/PatternsPage.test.tsx` | 1 | Pattern evidence links search the returned example message, not the wildcard template. |
| `src/pages/ScenarioLabPage.test.tsx` | 4 | Scenario catalogue with the persisted selection; a real server frame previewed before any stream starts; a deterministic run opened through the client and its session reported; the two data sources kept separate in the copy. |
| `src/pages/ServicesPage.test.tsx` | 2 | Generated fleet health renders with no dataset loaded, and stays separate from the dataset service map. |
| `src/presentation/GuidedDemo.test.tsx` | 2 | Guided demo reads the active dataset, issues the structured-search request, shows backend evidence and exits with Escape; failed operation reports the error and supports retry. |
| `src/replay/ReplayContext.test.tsx` | 2 | One shared SSE subscription, replay state transitions, progress counters, and restart/stop behaviour. |
| `src/telemetry/SimulationBand.test.tsx` | 2 | Measured simulation metrics come from the shared stream, and the open incident is separated from the dataset sections with a workbench link. |
| `src/telemetry/adapters.test.ts` | 4 | The measured band reads the current tick rather than the lagging rolling window; reports elevated while the current tick is failing; counts `FATAL` as failing to match the server detector rule; and is explicit about having no measurement. |
| **Total** | **64** | **19 test files.** |

These are focused component tests over client helpers, the shell, the topology renderer, the Command Center, Algorithm Lab, analytics tabs, patterns, the Dataset Replay screen, the replay provider and the error boundary. They do not exercise production network conditions or hardware accelerated WebGL; Playwright covers the assembled local app and backend separately.

## Browser E2E inventory

`frontend/e2e/product.spec.ts` holds **15 workflows**. Six were added this revision:

| Test | Asserts |
|---|---|
| Atmospheric Signal semantic text colours | Relative luminance ≥ 4.5 for 20 token pairs: four text tokens, `--accent` and `--accent-strong`, four status/dim pairs, `--purple`, four severity colours and all six algorithm-module colours |
| Signal field reports real values and never invents them | The four stages carry values the API actually returned, and em dashes rather than zeros with no dataset |
| Empty dataset shows onboarding | The explicit no-dataset state renders with no fabricated metrics |
| Failing backend surfaces a retryable error | A 500 envelope on `/api/*` produces an alert region with a retry path |
| Command palette navigates and restores focus | Open, keyboard navigation, selection, focus restoration |
| Documented route aliases resolve | Every documented alias lands on its canonical route |
| A document route does not resolve | The catch-all not-found route renders |

The viewport matrix is unchanged: 1440×900, 1280×800, 1024×768, 768×1024, 390×844 and 360×800, asserting `document.documentElement.scrollWidth` fits on both the Command Center and the Service Map.

**These 15 workflows have not been executed on this revision.** The nine pre-existing workflows were previously green against a live local backend; the six new ones are present in source and are named above, but no run result is claimed for them.

## Backend coverage map

The Maven suite covers:

- String matchers, Aho-Corasick, suffix structures and cross-implementation match-set checks.
- DP edit distances, alignment, interval, bitmask, tree and SOS algorithms.
- Flow algorithms, min-cut, matching, min-cost flow and shared flow cross-checks.
- Approximation, FPT, kernelization, reductions, FPTAS and their independent test oracles.
- Randomized sorting, Miller-Rabin, hashing, reservoir sampling and modular arithmetic.
- Parallel reduce, scan, sort, work/span analysis and sequential/parallel witnesses.
- Parsers, malformed-line handling, sample data, datasets, indexing, search, analytics, patterns and incidents.
- Catalogue metadata, query validation, request conversion, trace/run endpoints and MockMvc error contracts.
- The academic `java.util` scope guard (`EngineScopeGuardTest`), described below.

Cross-checks compare independent implementations or explicit sequential or brute-force oracles where applicable. Live timing is kept out of correctness assertions; benchmark endpoints measure at runtime.

### Academic `java.util` scope guard

DSA-3 forbids delegating core algorithm logic in `dsa/**` to `java.util` collections. The pre-rebuild code already imported a bounded set of `java.util` types, so the guard is a frozen ledger rather than a refactor: `EngineScopeGuardTest` records today's exact `java.util` import suffixes per `dsa` source file, walks every `.java` file under `backend/src/main/java/com/loginsight/dsa` (and asserts it scanned at least 30 files), and fails the build if any file imports a `java.util` type that is not in the manifest. Removing recorded usage is always allowed, so the ledger can only shrink. `java.util.concurrent` under `dsa/parallel` and `java.util.Random` under `dsa/randomized` are course-licensed and are therefore listed in the manifest. Non-`dsa` packages are unrestricted.

## Frontend verification boundary

The production build validates TypeScript and Vite bundling. Playwright runs the product through Vite against the actual local Spring Boot backend; deterministic demo data is loaded through its real API. Fifteen workflows check the documented routes, browser console and unexpected failed requests, 20 semantic token contrast pairs, real scroll reveal and reduced motion, dataset search and event details, topology selection and forced WebGL fallback, guided-presentation evidence/exit, mobile navigation, the signal field's no-fabrication contract, the empty-dataset state, a failing backend, command-palette focus restoration, route aliases and the not-found route, plus overflow at six viewport targets. The visual capture script records 19 current screenshots. These checks do not prove GPU-backed WebGL rendering, complete screen-reader usability or production network behavior under load.

**Executed on this revision:** backend `mvn verify`, frontend `vitest run`, frontend `npm run build`. **Not executed on this revision:** Playwright, `npm audit`, `docker compose config`.

No axe-based accessibility scan or pixel-diff visual-regression suite is configured. A deployment smoke test should additionally verify:

1. `GET /healthz` on the frontend container.
2. `GET /api/health` and `GET /api/health/status` through the frontend origin.
3. Demo load, dataset-backed search and a replay stream through Nginx.
4. Direct navigation to a client-side route such as `/logs`, `/investigate/{id}` and `/runs/{id}`.
5. Multipart upload size and error rendering — in particular that an empty upload now answers **400** rather than 500.
6. Command Center range switching, SVG/WebGL mode switching, node selection and camera controls. On a device without WebGL, verify the explicit fallback and confirm the accessible service list remains available.
7. That the `vendor-three` chunk is fetched only when the 3D view is opened, and is not requested on first paint.


## Performance claims

The search benchmark runs one measured execution per matcher over the same loaded haystack. Parallel benchmark endpoints report actual sequential/parallel timings and work/span values for the executed schedule. Results depend on the host, JVM, input and concurrent load; no universal speedup is claimed.

## Reproducibility notes

- Demo content uses a fixed seed; only its time anchor follows the current hour.
- Run history and trace steps are bounded and process-local.
- The sample-data files are committed and parser-tested.
- The visual evidence set is reproducible with `npm run capture:visuals` against a running backend and frontend.
- A clean checkout must provide Java 21+ and Node.js 22.12+ for the local test command. The checked-in CI jobs use Temurin 21 and Node 22, so the same commands run unchanged in CI.
