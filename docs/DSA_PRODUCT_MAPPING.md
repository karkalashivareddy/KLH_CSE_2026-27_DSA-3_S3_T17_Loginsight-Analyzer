# LogInsight Analyzer — DSA → Product Mapping

How the implemented algorithm engine (`backend/src/main/java/com/loginsight/dsa/**`) connects to
product features. Paths are `backend/src/main/java/com/loginsight/...`. See
[ALGORITHMS.md](ALGORITHMS.md) for the full catalogue behaviour and [API.md](API.md) for the
endpoints.

Two honest labels are used:

- **Product feature** — the algorithm directly powers a screen or API the user can reach.
- **Algorithm engine / analytical capability** — the algorithm is real, exposed, benchmarked and
  runnable via the Algorithm Insights catalogue / run sessions, but we do not claim it drives a
  specific product panel (per the portfolio honesty rule).

## M2 — String algorithms

| Algorithm | Product use | Where implemented | Notes |
| --- | --- | --- | --- |
| KMP | **Product feature** — free-text search matching (`algorithm: "KMP"` reported in the search response) | `search/LogSearchService.java` (default matcher), engine at `dsa/string/KMPMatcher.java` | also measured in the search benchmark |
| Naive | **Product feature** — one of the four measured matchers in the search benchmark | `dsa/string/NaiveMatcher.java` | benchmark row "Naive" |
| Z Algorithm | **Product feature** — one of the four measured matchers in the search benchmark | `service/SearchBenchmarkService.java`, engine at `dsa/string/ZAlgorithm.java` | benchmark row "Z-Algorithm" |
| Rabin-Karp | **Product feature** — one of the four measured matchers in the search benchmark | `service/SearchBenchmarkService.java`, engine at `dsa/string/RabinKarpMatcher.java` | benchmark row "Rabin-Karp" |
| Aho-Corasick | Algorithm engine / analytical capability | `dsa/string/aho/AhoCorasick.java` (+`AhoNode`, `PatternMatch`) | multi-pattern engine in catalogue |
| Suffix Array + Kasai LCP | Algorithm engine / analytical capability | `dsa/string/suffix/SuffixArray.java`, `dsa/string/suffix/KasaiLCP.java` | library-only (`kasai_lcp` not exposed) |
| String matcher contract | shared abstraction | `dsa/string/StringMatcher.java`, `StringSearchResult.java`, `TextValidator.java` | — |

## M3 — Dynamic programming

| Algorithm | Product use | Where implemented | Notes |
| --- | --- | --- | --- |
| Levenshtein edit distance | **Product feature** — "did you mean?" fuzzy suggestion on zero-match search | `search/LogSearchService.java` (`dsa/dp/editdistance/LevenshteinDistance.java`) | also the `fuzzy_search` catalogue entry; script-driven `FuzzySearchEngine` in the query facade |
| Damerau-Levenshtein (OSA) | Algorithm engine / analytical capability | `dsa/dp/editdistance/DamerauLevenshteinDistance.java` | exposed via the DP endpoints |
| Weighted edit distance | Algorithm engine / analytical capability | `dsa/dp/editdistance/WeightedEditDistance.java` | — |
| Needleman-Wunsch / Smith-Waterman alignment | Algorithm engine / analytical capability | `dsa/dp/alignment/NeedlemanWunsch.java`, `dsa/dp/alignment/SmithWaterman.java` | — |
| Tree DP (diameter, centroid, subset-sum, rerooting) | Algorithm engine / analytical capability | `dsa/dp/tree/*.java` | — |
| Interval DP (matrix chain, optimal BST) | Algorithm engine / analytical capability | `dsa/dp/interval/MatrixChainMultiplication.java`, `OptimalBinarySearchTree.java` | matrix-chain is traceable |
| Bitmask DP (TSP, Hamiltonian path) | Algorithm engine / analytical capability | `dsa/dp/bitmask/BitmaskTSP.java`, `HamiltonianPath.java` | — |
| SOS DP | Algorithm engine / analytical capability | `dsa/dp/sos/SOSDP.java` | — |

## M4 — Network flow

| Algorithm | Product use | Where implemented | Notes |
| --- | --- | --- | --- |
| Ford-Fulkerson | Algorithm engine / analytical capability | `dsa/flow/FordFulkerson.java` | traceable |
| Edmonds-Karp | Algorithm engine / analytical capability | `dsa/flow/EdmondsKarp.java` | traceable |
| Dinic | Algorithm engine / analytical capability | `dsa/flow/Dinic.java` | traceable; legacy TextHack "Dependency Flow" facade routes here (compatibility, not in the product UI) |
| Min-Cost Max-Flow / Min-Cut / Bipartite Matching | Algorithm engine / analytical capability | `dsa/flow/MinCostMaxFlow.java`, `MinCut.java`, `BipartiteMatching.java` | supporting `FlowGraph`, `ResidualNetwork`, validators |

## M5 — Approximation & NP-completeness

| Algorithm | Product use | Where implemented | Notes |
| --- | --- | --- | --- |
| Vertex cover approximation (2-approx) | Algorithm engine / analytical capability (traceable) | `dsa/approximation/VertexCoverApproximation.java` | one of the 13 traceable algorithms |
| Kernelization / bounded (FPT) / Set Cover / Maximal Matching / Knapsack FPTAS / Independent-Set reduction | Algorithm engine / analytical capability | `dsa/approximation/VertexCoverKernelization.java`, `BoundedVertexCover.java`, `SetCoverDemo.java`, `MaximalMatching.java`, `KnapsackFPTAS.java`, `IndependentSetReduction.java`, `ComplementGraph.java` | — |

## M6 — Randomized

| Algorithm | Product use | Where implemented | Notes |
| --- | --- | --- | --- |
| Randomized QuickSort | Algorithm engine / analytical capability (traceable) | `dsa/randomized/RandomizedQuickSort.java` | traceable |
| Miller-Rabin | Algorithm engine / analytical capability (traceable) | `dsa/randomized/MillerRabin.java` | traceable; legacy TextHack "Prime Testing" facade routes here (compatibility) |
| Reservoir sampling | Algorithm engine / analytical capability (traceable) | `dsa/randomized/ReservoirSampling.java` | traceable |
| Perfect / randomized hashing, universal hash family, modular arithmetic | Algorithm engine / analytical capability | `dsa/randomized/PerfectHash.java`, `RandomizedHash.java`, `UniversalHashFamily.java`, `ModularArithmetic.java` | `perfect_hash` is library-only (not exposed) |

## M6 — Parallel

| Algorithm | Product use | Where implemented | Notes |
| --- | --- | --- | --- |
| Parallel sort / reduce / prefix scan | Algorithm engine / analytical capability (benchmarked) | `dsa/parallel/ParallelSort.java`, `ParallelReduce.java`, `ParallelPrefixScan.java` | work-span analysis via `WorkSpanAnalyzer`, benchmarks via `BenchmarkService`; no traceable algorithms in this module (0 of 3) |

## Product-side algorithms (outside `dsa`, but algorithmic)

| Component | Role | Where implemented | Classification |
| --- | --- | --- | --- |
| SearchQueryParser | parses `level:/service:/host:/source:/status:/trace:` and ranges | `search/SearchQueryParser.java` | product core |
| LogIndex | sorted position lists, time-window lookups, intersections | `index/LogIndex.java` | product core |
| PatternExtractor | heuristic token normalization → message templates | `pattern/PatternExtractor.java` | product core (explicitly **not ML**) |
| IncidentDetector | 5-min windowed baseline (`≥ max(3, 3×baseline)`) | `incident/IncidentDetector.java` | product core (rule-based heuristics) |
| Analytics analyzers | timeline/severity/heatmap/fleet/frequency variants | `analytics/*.java` | product core |
| Service dependency graph | builds graph + top-k from events | `graph/ServiceDependencyGraph.java`, `graph/TopKFrequentAnalyzer.java` | product core (analytics/dependencies) |

## Catalogue summary (live, from `catalog/AlgorithmCatalog` + `service/CatalogService`)

| Module | id | Algorithms | Exposed | Traceable |
| --- | --- | --- | --- | --- |
| M2 String Algorithms | strings | 9 | 8 | 4 (naive, kmp, z, rabinkarp) |
| M3 Advanced Dynamic Programming | dp | 12 | 12 | 2 (levenshtein, matrixchain) |
| M4 Network Flow | flow | 6 | 6 | 3 (fordfulkerson, edmondskarp, dinic) |
| M5 Approximation & NP-Completeness | approximation | 7 | 3 | 1 (vertexcover) |
| M6 Randomized Algorithms | randomized | 5 | 4 | 3 (quicksort, millerrabin, reservoir) |
| M6 Parallel Algorithms | parallel | 3 | 3 | 0 |
| **Total** | — | **42** | **36** | **13** |

> Honesty note: the M1 slot is unused and both M6 randomised and M6 parallel carry the "M6" label in
> the catalogue subtitles — this is how the modules are represented in `CatalogService`, not a typo.