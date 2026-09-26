# Testing and Verification

## Current gates

The current checkout was verified on 2026-09-25 with:

```powershell
cd backend
.\mvnw.cmd -o verify

cd ..\frontend
npm test
npm run build
```

Results:

- Backend: **827 tests, 0 failures, 0 errors**; Spring Boot jar packaged successfully.
- Frontend: **24 tests across 10 files passed** with Vitest.
- Frontend build: TypeScript compilation and Vite production build passed.
- JaCoCo: report generated under `backend/target/site/jacoco/`; no numeric coverage threshold is configured in the POM.
- Compose syntax: `docker compose config --quiet` passed.
- Container image build/runtime: not executed because the Docker daemon was unavailable. That limitation still holds; `docker info` cannot reach the Docker engine in this environment, so no image build or container smoke test has been run on this branch.

The checked-in CI workflow runs `mvn -q verify` in the backend job, then `npm ci`, `npm test` and `npm run build` in the frontend job, so the Vitest suite is a merge gate rather than a local-only check. The frontend job pins Node 22 because Vitest 5 declares `engines: ^22.12.0 || ^24.0.0 || >=26.0.0`; the frontend still has no configured browser-test or lint command.

## Frontend test inventory

| File | Tests | Coverage |
|---|---:|---|
| `src/api/client.test.ts` | 3 | SSE frame parsing — chunked frames, comment lines, event ids, multiline `data:`, and flushing an unterminated final frame. Plus multipart boundary handling. |
| `src/components/format.test.ts` | 3 | Duration formatting: values treated as milliseconds, values treated as nanoseconds, and fractional millisecond output. |
| `src/components/Layout.test.tsx` | 2 | Skip link plus grouped navigation entries with active route state. |
| `src/components/TopologyPanel.test.tsx` | 4 | Accessible service/edge lists; data-driven node radius and edge particle count; controlled depth mode, renderer note and reset view; reduced-motion static particles. |
| `src/pages/AlgorithmsPage.test.tsx` | 3 | Algorithm Lab posts the catalogue `defaultInput` to the runs API and navigates to the created session; the run input is displayed; a rejected run surfaces an error instead of navigating. |
| `src/pages/AnalyticsPage.test.tsx` | 1 | Analytics tablist semantics: `aria-selected`, `aria-controls`, and a tabpanel whose `id`/`aria-labelledby` follow the selected tab. |
| `src/pages/LivePage.test.tsx` | 2 | Demo replay disclosure and shared `Replay state:` label; start and "Replay again" both open one SSE stream with the selected batch size and pace. |
| `src/pages/OverviewPage.test.tsx` | 3 | Command Center selected-window metrics, dataset coverage, detected-investigation state and the explicit no-dataset state. |
| `src/pages/PatternsPage.test.tsx` | 1 | Pattern evidence links search the returned example message, not the wildcard template. |
| `src/replay/ReplayContext.test.tsx` | 2 | One shared SSE subscription, replay state transitions, progress counters and stop/restart behavior. |
| **Total** | **24** | |

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

The production build validates TypeScript and Vite bundling. Source-level review covers route wiring, API paths, error rendering, SSE cancellation, responsive CSS and accessibility affordances. Focused Vitest/Testing Library coverage verifies the shell landmarks, client helpers, topology rendering, Command Center states and the shared replay provider. It does not prove browser behavior, contrast against every rendered state, screen-reader usability, WebGL/3D rendering (none exists) or network behavior under production load.

No browser automation, axe run or visual-regression suite is configured in this checkout. A deployment smoke test should additionally verify:

1. `GET /healthz` on the frontend container.
2. `GET /api/health` and `GET /api/health/status` through the frontend origin.
3. Demo load, dataset-backed search and a replay stream through Nginx.
4. Direct navigation to a client-side route such as `/logs`, `/investigate/{id}` and `/runs/{id}`.
5. Multipart upload size and error rendering.
6. Command Center range switching, topology 2D/depth mode toggle and node selection, confirming the renderer note reads "not WebGL".

## Performance claims

The search benchmark runs one measured execution per matcher over the same loaded haystack. Parallel benchmark endpoints report actual sequential/parallel timings and work/span values for the executed schedule. Results depend on the host, JVM, input and concurrent load; no universal speedup is claimed.

## Reproducibility notes

- Demo content uses a fixed seed; only its time anchor follows the current hour.
- Run history and trace steps are bounded and process-local.
- The sample-data files are committed and parser-tested.
- A clean checkout must provide Java 21+ and Node.js 22.12+ for the local test command. The checked-in CI jobs use Temurin 21 and Node 22, so the same commands run unchanged in CI.
