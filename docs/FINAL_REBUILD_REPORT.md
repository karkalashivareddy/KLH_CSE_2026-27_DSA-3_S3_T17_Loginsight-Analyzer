# FINAL REBUILD REPORT — TextHack: Advanced Algorithms Laboratory

Transforms **LogInsight Analyzer** into a DSA-3 lab where real algorithms, real traces, and real
benchmarks are surfaced through a six-module catalogue, a natural-style query facade, and recorded
run sessions. Verification: **696 backend tests / 0 failures**, `mvn -q verify` green, `npm run
build` green, live smoke test of modules/text-hack/runs/SSE passed.

## Phase log

| Phase | Deliverable | Evidence |
|---|---|---|
| 1 | Baseline audit + research (`docs/REBUILD_BASELINE.md`, `docs/RESEARCH.md`) | commits `e5dd233` |
| 2 | Algorithm catalogue + module metadata API (`/api/modules`, `/api/algorithms`) + `RequestFactory` guards | `AlgorithmCatalogTest`, `RequestFactoryTest` — commit `76ff967`, service/test refiled in `f014e5a` |
| 3 | TextHack unified query API (6 query classes → real KMP/Levenshtein/Needleman-Wunsch/Dinic/VC/Miller-Rabin) | `TextHackControllerTest` (7) — commit `67ef612` |
| 4 | Run lifecycle + SSE replay (`/api/runs`, `/api/runs/{id}/events`) with bounded in-memory `RunStore` | `RunControllerTest` (3) — commit `b613b3f` |
| 5 | `java.util` scope guard over `dsa` package | `EngineScopeGuardTest` — commit `6f5aeff` |
| 6 | Design-token system (control-room palette, module accents, focus-visible, reduced-motion) + API types/client | `frontend/src/styles/global.css`, `api/types.ts`, `api/client.ts` (incl. SSE parser) |
| 7 | Command Center (module cards from live catalog, recent runs, dataset overview) | `pages/OverviewPage.tsx` |
| 8 | TextHack console (query classes, field editor, real result + lab jumps, save-as-run) | `pages/TextHackPage.tsx` |
| 9 | Laboratory rebuilt on `/api/algorithms`, module-grouped, deep links; TraceStudio upgrades (keyboard, ledger, education) | `pages/LabPage.tsx`, `components/TracePlayer.tsx` |
| 10 | Course Map page; Run Sessions with live SSE replay; benchmarks speedup charts; system/docs updated | `pages/CourseMapPage.tsx`, `pages/RunsPage.tsx`, `pages/BenchmarksPage.tsx` |
| 11 | Accessibility polish (focus rings, ARIA on nav/query selectors, `prefers-reduced-motion`) | in shell + CSS |
| 12 | Docs: README rewrite, `COURSE_MAP.md`, `TRACE_ENGINE.md`, API §12, new theory supplements 05/07 (stale-link fix) | this report |
| 13 | Final verification | `mvn -q verify` (696 tests), `npm run build`, boot-jar smoke test |

## What changed conceptually

1. **Framing**: a "log analyzer" that happened to use algorithms became an **algorithms laboratory**
   that happens to use real log data. Six modules match DSA-3 Modules 1–6.
2. **Trace over animation**: replay is a pure function of recorded steps; run sessions persist them.
3. **Honest labels**: exposed vs library-only, exact vs approximate, probable vs proven, recorded vs
   streamed, `truncated` set by the recorder only.
4. **One source of truth**: module order/labels/accents and algorithm descriptors live in the backend
   catalogue; the UI renders them (`/api/modules`, `/api/algorithms`).

## Key files

- Backend: `catalog/AlgorithmCatalog.java`, `catalog/{AlgorithmInfo,ModuleInfo}.java`,
  `service/{CatalogService,TextHackService,RunService}.java`,
  `controller/{CatalogController,TextHackController,RunController}.java`,
  `service/text/TextHackCommand.java`, `run/RunStore.java`, `dto/RequestFactory.java`.
- Frontend: `api/{types,client}.ts`, `components/{Layout,TracePlayer}.tsx`, `styles/global.css`,
  `pages/{Overview,TextHack,Lab,Runs,CourseMap,Benchmarks}.tsx`.
- Docs: `COURSE_MAP.md`, `TRACE_ENGINE.md`, `12-api-documentation.md` §12.

## Honest limitations

- Run history is in-memory (bounded at 64); restarts lose sessions.
- Parallel algorithms have no step recorder yet (benchmarked, not traced).
- Suffix-array construction remains O(n log n) (SA-IS is a documented roadmap item, `05`).

## How to verify

```powershell
.\mvnw.cmd -q verify          # backend: 696 tests, 0 failures
cd frontend ; npm run build   # tsc + vite production build
.\mvnw.cmd -q spring-boot:run # then open http://localhost:8080 (or `npm run dev` in frontend/)
curl http://localhost:8080/api/modules
```