# Rebuild Baseline — Forensic Repository Audit

Date: 2026-09-23
Scope: full checkout at `main` (HEAD after fast-forward to `ead2f86`).

This document records the exact state of the repository **before** the TextHack
rebuild so that every later change is auditable. It is a snapshot, not a
specification. Everything here was verified against the working tree and build
output, not inferred from the README.

---

## 1. Repository facts

| Item | Value |
| --- | --- |
| Remote | `https://github.com/karkalashivareddy/KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer` |
| Branch | `main` |
| Backend | Spring Boot 3.5.16, Java 21 target, Maven (wrapper 3.9.9), JaCoCo 0.8.15 |
| Frontend | React 18.3, TypeScript 5.6, Vite 6.4, React Router 6.30 — **no test/lint infra** |
| Data | bundled `sample-data/` (text + JSONL); in-memory runtime, no database |
| CI | `.github/workflows/ci.yml` → backend `mvn -q verify` (+ frontend `npm ci && npm run build`) |
| Verified build | backend `clean verify`: **669 tests, 0 failures, 0 errors**; frontend `npm run build`: clean |
| Routed pages | 8 (Overview, Logs, Analytics, Datasets, Lab, Benchmarks, System, Docs) |

---

## 2. Architecture at baseline

```text
Browser (React SPA, REST only)
    └── /api proxy (Vite dev server)
            └── Spring Boot (spring-boot-starter-web only)
                    ├── controller/  REST shells (thin; no algorithm logic)
                    ├── service/     orchestration: build QueryContext, map results, benchmark, trace, datasets
                    ├── query/       QueryType/AlgorithmType classification, QueryDispatcher (strategy registry)
                    ├── query/engine/ 30 strategy engines (string / dp / flow / approx / randomized / parallel)
                    ├── dsa/         hand-built algorithm implementations (+ CustomQueue/CustomStack primitives)
                    └── trace/       AlgorithmStep + StepRecorder (bounded 400) + TraceCatalog (14 entries)
```

Key properties measured at baseline:

- Execution is **synchronous REST**. There is no run lifecycle, no streaming
  (SSE/WebSocket), no cancellation, no run history.
- Trace playback is a **post-hoc replay** of a bounded step list produced during
  a real instrumented run (`StepRecorder.MAX_STEPS = 400`, truncation flagged).
- Frontend drives the Lab exclusively through `GET /api/trace/catalog` +
  generic `POST` against each trace endpoint.

---

## 3. Algorithm inventory actually present (verified via source + tests)

| Module | Algorithms (Java class → endpoint) |
| --- | --- |
| M2 Strings | Naive `/api/search/naive`; KMP `/api/search/kmp`; Z `/api/search/z`; Rabin-Karp `/api/search/rabin-karp`; Aho-Corasick `/api/search/multi`; Suffix-array build `/api/string/suffix/build` + search `/api/string/suffix/search`; Fuzzy (Levenshtein) `/api/fuzzy/search` |
| M3 DP | Levenshtein `/api/dp/levenshtein`; Damerau `/api/dp/damerau`; weighted edit `/api/dp/weighted-edit`; Needleman-Wunsch `/api/dp/global`; Smith-Waterman `/api/dp/local`; matrix chain `/api/dp/matrix-chain`; OBST `/api/dp/obst`; TSP `/api/dp/tsp`; Hamiltonian `/api/dp/hamiltonian`; tree DP `/api/dp/tree`; rerooting `/api/dp/rerooting`; SOS `/api/dp/sos` |
| M4 Flow | Ford-Fulkerson `/api/trace/flow/ford-fulkerson`; Edmonds-Karp `/api/flow/edmonds-karp`; Dinic `/api/flow/dinic`; min-cut `/api/flow/min-cut`; bipartite matching `/api/flow/matching`; min-cost `/api/flow/min-cost` |
| M5 Approx | VC 2-approx `/api/approx/vertex-cover`; incident cover `/api/approx/incident-cover`; set cover `/api/approx/set-cover` |
| M6a Random | Miller-Rabin `/api/random/prime`; reservoir `/api/random/sample`; universal/random hash `/api/random/hash`; randomized quicksort `/api/random/quicksort` |
| M6b Parallel | reduce `/api/parallel/reduce`; scan `/api/parallel/scan`; sort `/api/parallel/sort`; benchmark `/api/benchmark/run` |

### Traceable (StepRecorder-instrumented, listed by `/api/trace/catalog`)

`Naive, KMP, Z, Rabin-Karp, Levenshtein, MatrixChain, Ford-Fulkerson,
Edmonds-Karp, Dinic, VertexCover, Quicksort, MillerRabin, Reservoir` → **13–14** entries
(depending on count; catalog returns a flat list).

### Documented-conceptual (not implemented — stated honestly in docs)

SA-IS O(n) suffix construction; cycle-cancelling min-cost flow; the NP-completeness
theory body; Las Vegas vs Monte Carlo taxonomy. These remain conceptual in the
rebuild and must never be presented as implemented.

---

## 4. Trace model at baseline

- `AlgorithmStep(index, operation, description, state, highlighted, metrics)`.
- `StepRecorder.record(...)` appends until the 400-step ceiling, then flags truncation.
- Operation tags observed in code: `COMPARE`, `FALLBACK`, `WINDOW`, `MATCH`,
  `DP_*`, `AUGMENT*`, `PARTITION`, `WITNESS`, `SAMPLE`, etc.
- `TraceResponseDto(algorithm, category, result, intermediateData, steps, truncated, executionTimeNanos, timeComplexity, spaceComplexity)`.
- Trace is produced end-to-end; there is no mid-stream delivery. Frontend replays the returned list.

## 5. Frontend at baseline

- 20 source files; plain CSS (~15.7 KB); hand-rolled SVG chart primitives.
- Pages: Overview (dataset-centric), Logs, Analytics, Datasets, Lab (catalog +
  generic JSON editor + TracePlayer), Benchmarks (table + per-algorithm bar
  chart), System (health), Docs, 404.
- `TracePlayer`: step counter, play/pause, prev/next, range scrub, 3 speed
  presets, timeline chips, state table. Solid but single-purpose.
- No design-token system (hard-coded colors in CSS + inline styles), no module
  accents, no per-algorithm visualization, no TextHack experience, no course
  mapping page, no run history, no a11y audit, no `prefers-reduced-motion`.

## 6. Problems found (baseline)

Academic/product:

1. **Module 1 (TextHack) is not productized.** The six query classes exist as
   concepts/endpoints but there is no unified "TextHack" entry point or query→
   algorithm→module mapping UX.
2. **No algorithm registry API.** Only the 13–14 traceable algorithms are
   exposed (`/api/trace/catalog`); the full 32-algorithm inventory and its
   DSA-3 module mapping is not machine-readable.
3. **No course-map page.** Docs `04-dsa-mapping.md` is excellent but the app
   does not surface the syllabus mapping.
4. **The dashboard identity is log-analytics**, not "Advanced Algorithms
   Laboratory"; the strongest differentiator (real execution + trace + course
   mapping) is under-visible.

Engineering:

5. **No run lifecycle / streaming / cancellation**; traces always arrive whole.
6. **No scope guard.** `dsa/**` currently imports `java.util.*` in ~25 files
   (`ArrayList/List/Map/Random/concurrent.*`). The prohibition documented in the
   syllabus is not enforced, and previously-unmeasured licenses vary by class.
7. **Frontend has no tests, no lint, no a11y pass.**
8. **Stale doc links**: `docs/04` cites `05-string-algorithms.md` and
   `07-network-flow.md`, which do not exist in the tree.
9. Minor duplication: Ford-Fulkerson exists only under `/api/trace/...` (no
   canonical `/api/flow/ford-fulkerson`).

## 7. Reusable assets (do not rewrite)

- All 30 engines + `QueryDispatcher` + `QueryContext` + `Results`.
- All `dsa/` implementations and their 669-test suite.
- `StepRecorder`, `AlgorithmStep`, `TraceCatalog`, `TraceService`.
- Health/dataset/log/analytics services.
- Frontend `useApi`, chart primitives, `format`, `TracePlayer` internals.

## 8. Definition of the rebuild contract

The rebuild must:

1. Keep every verified algorithm and its tests working (green `mvn verify`).
2. Add a machine-readable **algorithm/registry + module API**.
3. Productize **TextHack** (six query classes) as the entry experience.
4. Add a bounded **run model + SSE replay + run history** without faking real-time.
5. Enforce a **scope guard** that freezes current `java.util.*` licenses in
   `dsa/` and fails the build on any new, undocumented license.
6. Rebuild the frontend into a dark scientific **Algorithms Laboratory** shell
   with a design-token system, module accents, a command center, per-module
   labs, a richer Trace Studio, a course-map view, and benchmark polish.
7. Respect `prefers-reduced-motion`, keyboard navigation, and contrast.
8. Rewrite/repair documentation and record the outcome in a final report.