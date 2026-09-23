# TextHack Course Map — DSA-3 Modules 1–6

The frontend **Course Map** page renders this matrix live from `GET /api/algorithms`. This file is
the written contract: every syllabus topic in `docs/04-dsa-mapping.md` maps to a row here, and every
row has a real implementation, latency, and (for traceable keys) a step recorder.

## Modules and accents

| Module | Title | Accent | Traceable | Exposed | Library-only |
|---|---|---|---|---|---|
| strings | String Algorithms | `#22d3ee` | 4 | 8 | suffix array / Kasai LCP |
| dp | Dynamic Programming | `#a78bfa` | 2 | 12 | — |
| flow | Graph & Flow | `#fbbf24` | 3 | 6 | — |
| approximation | Approximation | `#34d399` | 1 | 3 | bounded vertex cover, kernelization, knapsack FPTAS, VC⇄IS reduction |
| randomized | Randomized | `#f472b6` | 3 | 4 | FKS perfect hash |
| parallel | Parallel | `#60a5fa` | 0 | 3 | — |

Totals: **42 catalogue entries** · 9 trace-instrumented algorithms used by runs · 0 fabricated data.

## Reading the status marks

- **trace** — `tracked=true`: the engine records every operation through `StepRecorder`; replayable
  in the Laboratory and as SSE run sessions.
- **api** — `exposed=true, tracked=false`: canonical REST endpoint, no recorder.
- **lib** — `exposed=false`: implemented library algorithm surfaced only through the catalogue (and
  via TextHack recommendations where relevant).

## Key chains / cross-verification

- String-search chain: `naive == kmp == z == rabinkarp` (same match sets).
- Flow chain: `fordfulkerson == edmondskarp == dinic` (same max-flow).
- Parallel chain: parallel reduce/scan/sort == sequential results.
- Approximation honesty: vertex cover declares its 2-approximation ratio + matching lower bound;
  library-only FPT/kernelization/FPTAS algorithms are labeled with their exact parameterized/EPS
  complexity, never advertised as polynomial.

## TextHack query-class routing

Each of the six query classes resolves to exactly one executed engine and a recommended lab journey:

| Query class | Executed engine | Lab jumps (`recommended`) |
|---|---|---|
| PATTERN_SEARCH → KMP | `kmp` | kmp · z · rabinkarp · naive |
| FUZZY_MATCH → Levenshtein | `levenshtein` | fuzzy_search · levenshtein |
| DOCUMENT_SIMILARITY → Needleman-Wunsch | `global_alignment` | global_alignment · levenshtein |
| CITATION_FLOW → Dinic | `dinic` | dinic · edmondskarp · fordfulkerson · min_cut |
| PROJECT_SCHEDULING → Vertex Cover | `vertexcover` | vertexcover · bounded_vertex_cover · vertex_cover_kernelization |
| PRIME_TESTING → Miller-Rabin | `millerrabin` | millerrabin |

## NP-completeness & parallel notes

- Module 5 exercises (Bounded Vertex Cover, kernelization, Knapsack FPTAS, VC⇄Independent Set) are
  implemented and catalogued; see `docs/np-completeness.md` for the theory body.
- Module 6 exercises measure real speedups; benchmark rows carry `work`, `span`, and
  `parallelism = work / span` from actual runs (`docs/14-benchmarking.md`).