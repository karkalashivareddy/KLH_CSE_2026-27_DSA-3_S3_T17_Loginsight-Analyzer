# Testing and Verification

## Current gates

The Signal in Motion and guided-demo revision was verified locally with:

```powershell
cd backend
.\mvnw.cmd -o verify

cd ..\frontend
npm.cmd test -- --reporter=dot
npm.cmd run build
```

Results:

- Backend: **878 tests, 0 failures, 0 errors**; Spring Boot jar packaged successfully.
- Frontend: **56 tests across 18 files passed** with Vitest, including a guided-demo API workflow test.
- Frontend build: TypeScript compilation and Vite production build passed. Vite warns that the lazy WebGL chunk is 576.56 kB minified (145.44 kB gzip), above the 500 kB warning threshold.
- JaCoCo: report generated under `backend/target/site/jacoco/`; no numeric coverage threshold is configured in the POM.
- Compose syntax: `docker compose config --quiet` passed.
- Container image build/runtime: not executed because the Docker daemon was unavailable. That limitation still holds; `docker info` cannot reach the Docker engine in this environment, so no image build or container smoke test has been run on this branch.

The checked-in CI workflow runs `mvn -B verify` in the backend job, then `npm ci`, `npm test` and `npm run build` in the frontend job. GitHub Actions status for this revision was unavailable and is not claimed. The frontend job pins Node 22 because Vitest 5 declares `engines: ^22.12.0 || ^24.0.0 || >=26.0.0`; the frontend still has no configured browser-test or lint command.

### Guided-demo backend integration smoke

The packaged backend jar was started locally on port `18080` because port `8080` was already occupied. Against this isolated process, the guided-demo sequence successfully loaded the deterministic demo dataset and called the real APIs: 14,000 parsed events; 6,127 `level:ERROR` search matches (the response's algorithm is `null` because this structured filter uses indexes); 12 analytics buckets; 8 services and 56 observed request-trail edges; 6 detector windows; 13 trace-catalogue operations; and 615 events in the returned `1h` overview. These are observations from one local smoke run, not expected constants or performance claims.

**Local verification and GitHub Actions verification are separate records.** The local commands above run the same goals the workflow runs, but the authoritative gate for a merge is the workflow run on `main`, which reports its own per-job counts. A local green run is not a substitute for a green workflow run, and this document records only what was actually executed.

## Frontend test inventory

| File | Tests | Coverage |
|---|---:|---|
| `src/api/client.test.ts` | 3 | SSE frame parsing — chunked frames, comment lines, event ids, multiline `data:`, and flushing an unterminated final frame. Plus multipart boundary handling. |
| `src/components/format.test.ts` | 8 | Duration, nanosecond and fractional-millisecond formatting, plus the `eventKey` identity contract: dataset ids, deterministic generated ids, provenance namespacing, key stability and the malformed-payload fallback. |
| `src/components/Layout.test.tsx` | 4 | Skip link, grouped navigation entries with active route state, the `Ctrl+K` command palette with focus restoration, and no false "no dataset" claim while status is still loading. |
| `src/components/TopologyPanel.test.tsx` | 7 | Accessible service and edge lists; data-driven node radius and edge particle count; declared-graph wording; observed-vs-window count separation; incident-window marking without causal claims; dense-graph legibility; on-demand WebGL load with 2D fallback; reduced-motion static particles. |
| `src/pages/AlgorithmsPage.test.tsx` | 3 | Algorithm Lab posts the catalogue `defaultInput` to the runs API and navigates to the created session; no run control without an input; a rejected run surfaces an error instead of navigating. |
| `src/pages/AnalyticsPage.test.tsx` | 1 | Analytics tablist semantics: `aria-selected`, `aria-controls`, and a tabpanel whose `id`/`aria-labelledby` follow the selected tab. |
| `src/pages/IncidentWorkbench.test.tsx` | 4 | The detected incident with its measured evidence and blast radius; lifecycle advance follows the server contract rather than a local guess; automatic advancement offered only while open; session-scoped lifecycle stated in the copy. |
| `src/pages/LivePage.test.tsx` | 2 | Dataset replay disclosure and shared `Replay state:` label; start and "Replay again" both open one SSE stream with the selected batch size and pace. |
| `src/pages/LogsPage.test.tsx` | 1 | The explorer applies backend severity syntax and opens the selected event drawer. |
| `src/pages/MonitorPage.test.tsx` | 4 | The stream is labelled generated rather than captured telemetry; measured frame metrics and server evidence are rendered; a deterministic run starts on demand; operators are pointed at Scenario Lab instead of being offered a traffic control that does not exist. |
| `src/pages/OverviewPage.test.tsx` | 3 | Command Center selected-window metrics, dataset coverage, detected-investigation state, the bounded-dataset-replay disclosure with working pattern log links, and the explicit source-selection state. |
| `src/presentation/GuidedDemo.test.tsx` | 1 | Guided demo reads the active dataset, issues the structured-search request, shows backend evidence and exits with Escape. |
| `src/pages/PatternsPage.test.tsx` | 1 | Pattern evidence links search the returned example message, not the wildcard template. |
| `src/pages/ScenarioLabPage.test.tsx` | 4 | Scenario catalogue with the persisted selection; a real server frame previewed before any stream starts; a deterministic run opened through the client and its session reported; the two data sources kept separate in the copy. |
| `src/pages/ServicesPage.test.tsx` | 2 | Generated fleet health renders with no dataset loaded, and stays separate from the dataset service map. |
| `src/replay/ReplayContext.test.tsx` | 2 | One shared SSE subscription, replay state transitions, progress counters, and restart/stop behaviour. |
| `src/telemetry/SimulationBand.test.tsx` | 2 | Measured simulation metrics come from the shared stream, and the open incident is separated from the dataset sections with a workbench link. |
| `src/telemetry/adapters.test.ts` | 4 | The measured band reads the current tick rather than the lagging rolling window; reports elevated while the current tick is failing; counts `FATAL` as failing to match the server detector rule; and is explicit about having no measurement. |
| **Total** | **56** | 18 test files. |

These are focused component tests over client helpers, the shell, the topology renderer, the Command Center, Algorithm Lab, analytics tabs, patterns, the Demo Replay screen and the replay provider. They are not browser QA and do not exercise real network, real SSE timing or real backend responses.

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

The production build validates TypeScript and Vite bundling. Source-level review covers route wiring, API paths, error rendering, SSE cancellation, responsive CSS and accessibility affordances. Focused Vitest/Testing Library coverage verifies the shell landmarks, client helpers, topology rendering and its WebGL-unavailable fallback, Command Center states and the shared replay provider. Unit tests do not prove successful hardware-accelerated WebGL rendering, contrast against every rendered state, screen-reader usability or network behavior under production load.

No browser automation, axe run or visual-regression suite is configured in this checkout. A deployment smoke test should additionally verify:

1. `GET /healthz` on the frontend container.
2. `GET /api/health` and `GET /api/health/status` through the frontend origin.
3. Demo load, dataset-backed search and a replay stream through Nginx.
4. Direct navigation to a client-side route such as `/logs`, `/investigate/{id}` and `/runs/{id}`.
5. Multipart upload size and error rendering.
6. Command Center range switching, SVG/WebGL mode switching, node selection and camera controls. On a device without WebGL, verify the explicit fallback and confirm the accessible service list remains available.

## Performance claims

The search benchmark runs one measured execution per matcher over the same loaded haystack. Parallel benchmark endpoints report actual sequential/parallel timings and work/span values for the executed schedule. Results depend on the host, JVM, input and concurrent load; no universal speedup is claimed.

## Reproducibility notes

- Demo content uses a fixed seed; only its time anchor follows the current hour.
- Run history and trace steps are bounded and process-local.
- The sample-data files are committed and parser-tested.
- A clean checkout must provide Java 21+ and Node.js 22.12+ for the local test command. The checked-in CI jobs use Temurin 21 and Node 22, so the same commands run unchanged in CI.
