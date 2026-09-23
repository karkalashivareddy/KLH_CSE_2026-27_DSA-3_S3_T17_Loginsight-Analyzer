# Post-Rebuild Hardening Audit

> Scope: verify the complete TextHack rebuild (phases 1-13, commits `f014e5a` → `cf76458` → `b711d94`)
> on branch `main`, then fix anything that fails the bar, and re-verify. No feature creep - this pass
> hardens and proves the existing baseline.

## Baseline (as of `b711d94`, verified during this pass)

| Check | Result |
| --- | --- |
| `git status` | Clean; only untracked `docs/screenshots/` (8 stale pre-rebuild PNGs, unreferenced in any markdown) |
| Branch | `main` (8 commits ahead of `origin/main`) |
| Backend tests | **696 tests / 0 failures / 0 errors** across 85 surefire files (`mvn -q verify`, exit 0) |
| Frontend build | Clean (`npm run build`; JS ~233 kB / gzip ~72 kB, CSS ~22 kB) |
| Live smoke | Boot jar on `:8099`: health UP, catalog, TextHack, runs, SSE replay, failure path all exercised |

## End-to-end API verification (live, all 42 catalogue entries)

### Canonical endpoints (36 of 36 reachable non-library endpoints)

- **strings**: naive, kmp, z, rabinkarp, aho_corasick(multi), suffix_array, suffix_search, fuzzy_search — all 200 with valid result envelopes. `kasai_lcp` is library-only (no endpoint, as designed).
- **dp**: levenshtein, damerau, weighted_edit, global, local, matrixchain, optimal_bst, bitmask_tsp, hamiltonian, tree_diameter, rerooting, sos — all 200.
- **flow**: edmondskarp, dinic, min_cut, bipartite_matching, min_cost_max_flow — 200. `fordfulkerson` has **no canonical endpoint by design** (trace-only; catalogue text states "No direct endpoint (lab + traces only)").
- **approximation**: vertexcover, set_cover, incident_cover — 200. bounded vertex cover / kernelization / fptas / vc-is-reduction are library-only (no endpoint).
- **randomized**: quicksort, millerrabin, reservoir, universal_hash — 200. `perfect_hash` library-only.
- **parallel**: reduce, scan, sort — 200.

### Trace endpoints (13 of 13)

`naive(38 steps) kmp(24) z(36) rabinkarp(11) levenshtein(51) matrixchain(16) fordfulkerson(4)
edmondskarp(4) dinic(6) vertexcover(4) quicksort(15) millerrabin(7) reservoir(10)`

All validated: steps are `index`-ordered 1..N, `operation` non-empty, `result` equals the canonical
result payload semantics (e.g. KMP `positions:[8],matchCount:1` on the same input; flow family all
report `maxFlow:15` on the same 5-node graph; levenshtein `distance:3` kitten→sitting).

### TextHack console (6 of 6 query classes)

| Query class | Engine | Result asserted |
| --- | --- | --- |
| PATTERN_SEARCH | KMP | `positions:[4,49], matchCount:2` |
| FUZZY_MATCH* | LEVENSHTEIN | per-line distance, `totalMatches:0` on explicit lines (honest) |
| DOCUMENT_SIMILARITY | NEEDLEMAN_WUNSCH | `similarityPercent:70`, `identityMatches:16`, aligned A/B with gaps |
| CITATION_FLOW | DINIC | `maxFlow:9` with residual edges + augmenting-paths count |
| PROJECT_SCHEDULING | VERTEX_COVER | `coverSize:4, lowerBound:2, ratio:2.0` (path A-B-C-D) |
| PRIME_TESTING | MILLER_RABIN | `isPrime:true` for 2147483647 with 20 witnesses |

\* FUZZY_MATCH without an explicit `text` resolves against the loaded dataset; with no dataset loaded
it returns HTTP 404 "No dataset loaded" (honest, but needs friendlier UI guidance → P2-02).

### Run sessions + SSE replay

- All 13 trackable algorithms create `COMPLETED` runs with recorded `stepCount` matching the trace endpoint.
- SSE replay emits `meta → step×N → complete` in order (`Invoke-WebRequest` verified verbatim bodies).
- Invalid-input runs produce a `FAILED` RunRecord with the recorded error message (`steps=0`).
- RunStore bounded at 64 (FIFO), documented as in-memory/diagnostic.

## Findings and classification

Severity scale: **P0** broken path or incorrect result · **P1** major correctness/UX/accessibility
issue · **P2** material polish · **P3** nice-to-have (documented, not fixed).

### P0 — none
No incorrect algorithm result or 500/exception path was found in any exercised endpoint. The blank
`POST /` 500 noticed in the smoke harness is an artifact of posting to the root path (not an
application route); not reachable from the UI.

### P1

| # | Area | Finding | Fix |
| --- | --- | --- | --- |
| P1-01 | `SearchService` | Canonical `/api/search/*` and `/api/search/multi` treat a **missing `scope`** as `DATASET` → "No dataset loaded" 404, while `RequestFactory.search` defaults a missing scope to `EXPLICIT`. The Lab sends no scope, so running a search algorithm with no loaded dataset fails confusingly even though the JSON text is provided. | Default missing scope to `EXPLICIT` in `SearchService.dispatchPattern`/`multi` so explicit-text runs work with no dataset. |
| P1-02 | `TracePlayer` | Global `window` keydown handler binds **Space/←/→/Home/End** even while the user types in the Lab JSON textarea, TextHack textarea or Run input — hijacks caret navigation and page scroll. | Skip the player shortcut when the event target is an editable element (`INPUT`/`TEXTAREA`/`SELECT`/`contentEditable`). |
| P1-03 | `RunsPage` | Streaming a **FAILED** run (or a run that errors mid-stream) leaves the UI stuck on "Waiting for streamed steps…" forever; SSE errors are only `console.error`. | Track a stream-failure state; render `ErrorBox` with the recorded `error`/`meta.error` or "connection lost" message; surface network errors visibly. |
| P1-04 | `LabPage` | `pick()` navigates to `/labs/:key`, dropping the `:module` segment — breaks the `/labs/:module/:key` deep link and abandons the module filter the user arrived with. | Preserve the module segment when present. |

### P2

| # | Area | Finding | Fix |
| --- | --- | --- | --- |
| P2-01 | `AlgorithmCatalog` | All non-tracked entries expose `defaultInput=null` → Lab shows `{}` and every library algorithm needs hand-typed JSON before its first run. | Add sensible `defaultInput` for the exposed non-tracked entries (search family, alignment, interval/bitmask/tree DP, flow, approx, randomized, parallel) so the Lab opens with a working example. |
| P2-02 | `TextHackPage` | Dataset-scoped queries (Fuzzy Match) show a raw 404 "No dataset loaded" with no guidance. | Detect `ApiError.status===404` and show a friendly, actionable error message + link to Datasets. |
| P2-03 | `RunsPage` | `accentId={run.category.toLowerCase()}` never resolves to a module color (falls back to global accent). Also an `await` race can apply a stale record to a newer selection. | Derive accent from the algorithm key; guard `open()` so a late response cannot clobber a newer selection. |
| P2-04 | `BenchmarksPage` | Speedup chart is correctly labelled "Sequential ÷ Parallel wall time" but the page does not state that measurements are environment-dependent ("measured on this machine"). | Add an honest methodology footnote; speedup baseline already documented in the chart label. |
| P2-05 | Responsive | Only 980px / 860px breakpoints exist; 768px / 480px "mobile" is not refined (header + trace toolbar + form grid slightly cramped). | Add a `≤640px` refinement block (header type scale, page padding, trace toolbar wrap, bench form stacking). |
| P2-06 | perf (trace) | Timeline + ledger render up to `StepRecorder.MAX_STEPS` (400) DOM buttons; bounded, but the ledger re-renders each step while playing. | Keep the slider/ledger; document the 400 ceiling and the aggregation decision. No windowing needed at the current cap — noted as a known limitation, revisited if the cap rises. |

### P3 (documented, not fixed)

| # | Finding |
| --- | --- |
| P3-01 | `CourseMapPage` intro line renders `<strong> — </strong> = exposed endpoint only` oddly as literal fragments. |
| P3-02 | `RunService` SSE emitter pool is fixed at 2 threads; adequate for demo traffic, could starve under many simultaneous large replays. |
| P3-03 | Duplicate `@media (max-width: 860px)` blocks in `global.css` (`.run-layout` defined twice). Harmless; consolidating is optional. |
| P3-04 | `docs/screenshots/*.png` are stale, uncommitted and unreferenced — removed (README links to the live app instead). Regeneration needs a browser, which this pass cannot run; documented in README. |

## Performance / security / accessibility notes

- **Bundle**: single-page build, no code-split; JS ~72 kB gzip is small. Vite `build.chunkSizeWarningLimit` relevant only if exceeded (it is not).
- **Backend**: `RequestFactory` caps `MAX_ARRAY_ELEMENTS=4096`, `MAX_TEXT_LENGTH=1_000_000`, `MAX_INTERVAL_DIMS=40`; `StepRecorder.MAX_STEPS=400`; RunStore ≤ 64. No unbounded input path.
- **Security**: REST-only, no auth/AI/DB surface; CORS not enabled (same-origin via Vite proxy); no secrets in the repo; error handler returns sanitised messages.
- **A11y**: `:focus-visible` rings, `prefers-reduced-motion` respected, buttons/labels/aria-expanded present, status conveyed by icon+label+color (not colour alone), trace keyboard shortcuts exposed to screen-reader users via the visible kbd hint bar.

## Verification plan (this pass)

1. Fix P1-01..04 + material P2 (listed above).
2. Re-run the full smoke matrix + TextHack + runs/SSE.
3. `mvn -q verify` (must stay ≥696/0) and `npm run build`.
4. Source-level responsive/accessibility review at 1440/1280/1024/768/480 widths (no browser automation available in this environment; media-query + grid audit done by construction).
5. Refresh docs: `POST_REBUILD_AUDIT.md` (this file), `FINAL_REBUILD_REPORT.md`, `README.md`, API §1 (search scope semantics).
6. Commit as logical units.

## Outcome (this pass)

| Fix | Status | Verification |
| --- | --- | --- |
| P1-01 search scope | Done | Default moved to `EXPLICIT` in `SearchRequest`/`MultiPatternRequest` DTOs (the compact constructors normalised a missing scope to `DATASET`, which is what produced the 404 — `SearchService` now resolves EXPLICIT too). Live: `/api/search/{kmp,z}` + `/api/search/multi` with `{pattern,text}` and no scope → **200**; `DatasetException` 404 retained for explicit `scope:DATASET` with no dataset (`ErrorContractControllerTest` unchanged). |
| P1-02 TracePlayer | Done | Global keydown handler skips `INPUT`/`TEXTAREA`/`SELECT`/`contentEditable` targets; Lab JSON editor, TextHack console and Run input no longer hijacked. |
| P1-03 RunsPage failures | Done | Streamed FAILED runs now render an `ErrorBox` with the recorded error (no more infinite "Waiting for streamed steps…"); SSE transport errors surface visibly with a retry that re-streams the same run. |
| P1-04 LabPage deep link | Done | `pick()` navigates to `/labs/:module/:key`, preserving the module segment. |
| P2-01 default inputs | Done | 22 exposed non-tracked algorithms gained `defaultInput` starters matching their request DTOs → 36/42 catalogue entries open with a runnable example (`{}` only for the 6 library-only classes). |
| P2-02 TextHack dataset hint | Done | 404 `DatasetException` shows an actionable hint (load a dataset / add a `text` field) instead of the raw error. |
| P2-03 Runs accent + race | Done | Accent derived from algorithm→module mapping; stale `runGet` response cannot clobber a newer selection. |
| P2-04 Benchmark honesty | Done | "measured on this machine (JVM 21 · N logical cores)" footnote added under the page title. |
| P2-05 Responsive | Done | `≤640px` block: header/type scale, page padding, hero/trace-toolbar wrap, form stacking, action buttons. |
| P3-01 CourseMap copy | Done | Intro legend rewritten without stray `<strong>` fragments. |
| P3-04 stale screenshots | Done | `docs/screenshots/` (8 unreferenced pre-rebuild PNGs) removed; README links to the live app instead of stale images. |

Final state: `mvn -q verify` **696 tests / 0 failures / 0 errors** (85 surefire files), `npm run build` clean
(JS 235.8 kB / gzip 73.5 kB, CSS 22.8 kB). All live smoke checks green against the rebuilt jar.