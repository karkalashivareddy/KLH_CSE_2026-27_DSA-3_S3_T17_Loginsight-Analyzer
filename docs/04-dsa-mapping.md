# 04 - DSA Syllabus to Implementation Mapping

Single source of truth: every curriculum topic implemented in LogInsight Analyzer, with its Java
class, application feature, REST endpoint, UI page, complexity, and test.

Place legend: StrLab = String Algorithm Lab, SimLab = Similarity Lab, SufLab = Suffix Lab,
Flow = Network Flow page, Approx = Approximation page, Rand = Randomized Lab,
Par = Parallel Lab, Play = DSA Playground.

---

## 1. Mapping Matrix by Module

### Module 1 - Text Analytics / Algorithmic Query System

| Syllabus | Algorithm / component | Java class | Feature | Endpoint | UI | Complexity | Test |
|---|---|---|---|---|---|---|---|
| Query-driven text analytics engine | Query engine (strategy pattern) | `QueryEngine`, `QueryDispatcher`, `QueryContext`, `QueryValidator` | Dispatches every query to its strategy; uniform result envelope | transversal | all pages | - | `QueryDispatcherTest`, `QueryValidatorTest` |
| Algorithm-aware query catalog | `QueryType`, `AlgorithmType` | `model/QueryType`, `model/AlgorithmType` | query classification + validation | transversal | algorithm selector | - | `QueryValidatorTest` |
| Metrics and complexity reporting | `AlgorithmMetrics` | `model/AlgorithmMetrics` | input size, nanos, memory estimate, throughput, speedup | transversal | MetricsPanel | - | - |
| Algorithm benchmarking | `BenchmarkService` | `service/BenchmarkService` | live measurements | `POST /api/benchmark/run` | Benchmark Lab | measured | `BenchmarkControllerTest` |

### Module 2 - String Algorithms

| Syllabus | Algorithm / component | Java class | Feature | Endpoint | UI | Complexity | Test |
|---|---|---|---|---|---|---|---|
| String matching | Naive pattern matching | `NaiveMatcher` | baseline; comparison vs KMP | `POST /api/search/naive` | StrLab | O(nm) time, O(1) space | `NaiveMatcherTest` |
| String matching | KMP (`buildLPS`, `search`) | `KMPMatcher` | exact search; LPS visualized | `POST /api/search/kmp` | StrLab | O(n+m); LPS O(m) space | `KMPMatcherTest` |
| String matching | Z-algorithm (`buildZArray`, `search`) | `ZAlgorithm` | exact search; Z-array visualized | `POST /api/search/z` | StrLab | O(n+m), O(n+m) space | `ZAlgorithmTest` |
| Rolling hash | Rabin-Karp | `RabinKarpMatcher` | rolling polynomial hash, modular arithmetic, collision verify, optional double hash | `POST /api/search/rabin-karp` | StrLab | O(n+m) avg, O(nm) worst; O(1) extra space | `RabinKarpTest` |
| Multi-pattern search | Aho-Corasick | `aho/AhoCorasick`, `aho/AhoNode`, `aho/PatternMatch` | multi-pattern over logs (ERROR/FAILED/TIMEOUT/...) | `POST /api/search/aho-corasick` | StrLab | O(n + sum|P| + matches); trie space O(sum|P|) | `AhoCorasickTest` |
| Suffix structures | Suffix array (doubling + counting sort) | `suffix/SuffixArray` | repeated log-pattern discovery, repeated error templates | `POST /api/suffix/build`, `POST /api/suffix/search` | SufLab | build O(n log^2 n), search O(m log n) | `SuffixArrayTest` |
| Suffix structures | Kasai LCP | `suffix/KasaiLCP` | repeated template discovery, longest common substring | `POST /api/suffix/lcp` | SufLab | O(n) | `KasaiLCPTest` |
| Suffix structures (conceptual) | SA-IS | documented only | O(n) construction concept | - | SufLab doc link | O(n) conceptual | - |

### Module 3 - Advanced Dynamic Programming

| Syllabus | Algorithm / component | Java class | Feature | Endpoint | UI | Complexity | Test |
|---|---|---|---|---|---|---|---|
| Edit distance | Levenshtein (`distance`, `buildMatrix`, `reconstructOperations`) | `dp/editdistance/LevenshteinDistance` | fuzzy log search, typo-tolerant search, DP matrix shown | `POST /api/search/fuzzy/levenshtein`, `POST /api/dp/levenshtein` | SimLab / Play | O(nm) time, O(nm) matrix (O(m) distance-only) | `LevenshteinTest` |
| Edit distance | Damerau-Levenshtein | `dp/editdistance/DamerauLevenshteinDistance` | typo correction incl. transposition | `POST /api/search/fuzzy/damerau` | SimLab | O(nm) | `DamerauTest` |
| Edit distance | Weighted edit distance | `dp/editdistance/WeightedEditDistance` | configurable insert/delete/substitute costs | `POST /api/dp/weighted-edit-distance` | Play | O(nm) | `LevenshteinTest` (coverage) |
| Sequence alignment | Needleman-Wunsch (global) | `dp/alignment/NeedlemanWunsch`, `dp/alignment/AlignmentResult` | compare service traces (AUTH-USER-DB-...) | `POST /api/similarity/needleman-wunsch` | SimLab | O(nm) time, O(nm) matrix | `NeedlemanWunschTest` |
| Sequence alignment | Smith-Waterman (local) | `dp/alignment/SmithWaterman`, `dp/alignment/AlignmentResult` | local motif discovery (USER-DB-PAYMENT) | `POST /api/similarity/smith-waterman` | SimLab | O(nm) | `SmithWatermanTest` |
| Interval DP | Matrix-chain multiplication | `dp/interval/MatrixChainMultiplication` | playground learning demo | `POST /api/dp/matrix-chain` | Play | O(n^3) time, O(n^2) space | `MatrixChainTest` |
| Bitmask DP | Bitmask TSP | `dp/bitmask/BitmaskTSP` | min-cost service route; 2^n state-count warning | `POST /api/dp/tsp` | Play | O(n^2 * 2^n), O(n * 2^n) space | `BitmaskTSPTest` |
| Bitmask DP | Hamiltonian path | `dp/bitmask/HamiltonianPath` | Hamiltonian route existence over dependency graph | `POST /api/dp/hamiltonian` | Play | O(n * 2^n) | `HamiltonianPathTest` |
| Tree DP | Tree diameter DP | `dp/tree/TreeDiameterDP` | playground tree demo | `POST /api/dp/tree-diameter` | Play | O(n) | `TreeDpTest` |
| Tree DP | Rerooting DP | `dp/tree/RerootingDP` | per-root values over all roots | `POST /api/dp/rerooting` | Play | O(n) | `TreeDpTest` |
| Subset DP | SOS DP | `dp/sos/SOSDP` | subset-based service/incident categories | `POST /api/dp/sos` | Play | O(n * 2^n) | `SosDpTest` |

### Module 4 - Graph / Network Flow

| Syllabus | Algorithm / component | Java class | Feature | Endpoint | UI | Complexity | Test |
|---|---|---|---|---|---|---|---|
| Graph modelling | Service dependency graph | `graph/ServiceDependencyGraph`, `analytics/ServiceGraphBuilder` | graph built from imported logs (vertex=service, edge=dependency, capacity=throughput) | `POST /api/flow/graph` | Flow | build O(E) | `ServiceGraphBuilderTest` |
| Max flow | Ford-Fulkerson | `flow/FordFulkerson` | residual update + augmenting path steps | `POST /api/flow/ford-fulkerson` | Flow | O(E * f_max) | `FordFulkersonTest` |
| Max flow | Edmonds-Karp (BFS) | `flow/EdmondsKarp` | BFS augmenting paths, per-step log | `POST /api/flow/edmonds-karp` | Flow | O(V * E^2) | `EdmondsKarpTest` |
| Max flow | Dinic | `flow/Dinic` | BFS level graph + DFS blocking flow | `POST /api/flow/dinic` | Flow | O(V^2 * E); O(sqrt(V) * E) unit caps | `DinicTest` |
| Min cut | Max-flow min-cut | `flow/MinCut` | reachable set, source/sink side, cut edges, capacity | `POST /api/flow/min-cut` | Flow | as chosen max-flow | `MinCutTest` |
| Matching | Bipartite matching as max flow | `flow/BipartiteMatching` | incidents to response resources | `POST /api/flow/matching` | Flow | O(V * E) via Dinic unit caps | `BipartiteMatchingTest` |
| Min-cost flow | SSP (shortest augmenting path, Bellman-Ford) | `flow/MinCostMaxFlow` | incident-response resource assignment | `POST /api/flow/min-cost-flow` | Flow | O(f * V * E) SSP | `MinCostMaxFlowTest` |
| Min-cost flow (conceptual) | Cycle cancelling | documented only | concept | - | Flow doc | - | - |
| Flow verification | cross-check | `FlowCrossCheckTest` | FF == EK == Dinic on shared graphs | - | - | - | `FlowCrossCheckTest` |

### Module 5 - NP-Completeness / Approximation

| Syllabus | Algorithm / component | Java class | Feature | Endpoint | UI | Complexity | Test |
|---|---|---|---|---|---|---|---|
| Matching | Maximal matching | `approximation/MaximalMatching` | greedy maximal matching feeding the cover | (via vertex cover) | Approx | O(E) | `MaximalMatchingTest`, `VertexCoverApproximationTest` |
| Approximation | Vertex cover 2-approx | `approximation/VertexCoverApproximation`, `approximation/ApproximationResult` | cover + matching + lower bound + ratio reporting; ratio <= 2 | `POST /api/approximation/vertex-cover` | Approx | O(E) | `VertexCoverApproximationTest` |
| Approximation app | Incident Coverage Planner | `service/ApproximationService` + `SetCoverDemo` | services covering required incident relationships; NP-hard caveat explained | `POST /api/approximation/incident-coverage`, `POST /api/approximation/set-cover` | Approx | greedy ln(n) | `SetCoverDemoTest` |
| Approximation app | Set cover (APX, H(n) bound) | `approximation/SetCoverDemo`, `approximation/SetCoverResult` | greedy set-cover demonstration; NP-hard caveat explained | (via tests/docs) | Approx | greedy | `SetCoverDemoTest` |
| FPT | Vertex cover parameterized by k | `approximation/BoundedVertexCover` | exact decision/certificate via bounded branching when k is small | (via tests/docs) | Docs | O(2^k (V+E)) | `BoundedVertexCoverTest` |
| Kernelization | VC kernelization rules | `approximation/VertexCoverKernelization`, `approximation/KernelizationResult` | self-loop + high-degree rules preserving tau(G) <= k | (via tests/docs) | Docs | O(nE) | `KernelizationTest` |
| FPTAS | 0/1 knapsack value-scaling FPTAS | `approximation/KnapsackFPTAS`, `approximation/KnapsackResult` | A >= (1-eps) OPT with polynomial n, 1/eps runtime | (via tests/docs) | Docs | O(n^3/eps) | `KnapsackFPTASTest` |
| Reduction | Vertex cover ↔ Independent set | `approximation/IndependentSetReduction` | V-S complement transformation + verifiers | (via tests/docs) | Docs | O(n+E) | `ReductionsTest` |
| Reduction | Clique ↔ IS in complement | `approximation/ComplementGraph` | complement transformation + clique verifier | (via tests/docs) | Docs | O(V^2) | `ReductionsTest` |
| Tractable-tool road map | Shared undirected graph model | `approximation/UndirectedGraph`, `approximation/UndirectedEdge` | simple graph with duplicate policy, self-loop policy, FPT-friendly removal | (transversal) | Approx | O(E) scans | all Phase 6 tests |
| Complexity theory | P/NP/co-NP/NP-hard/NP-complete, Cook-Levin, reduction zoo, PTAS/FPTAS/APX, FPT, kernelization | documented | `docs/np-completeness.md` | - | Docs | - | - |

### Module 6A - Randomized Algorithms

| Syllabus | Algorithm / component | Java class | Feature | Endpoint | UI | Complexity | Test |
|---|---|---|---|---|---|---|---|
| Randomized sort | Randomized quicksort | `randomized/RandomizedQuickSort` | random pivot comparison | `POST /api/randomized/quicksort` | Rand | O(n log n) expected | `RandomizedQuicksortTest` |
| Primality | Miller-Rabin | `randomized/MillerRabin` | manual modular exponentiation, d*2^s, witnesses, rounds | `POST /api/randomized/miller-rabin` | Rand | O(k * log^3 n) | `MillerRabinTest` |
| Randomized hashing | Universal hashing family | `randomized/UniversalHashFamily` | random seed, collision probability taught | `POST /api/randomized/hash` | Rand | O(1) op | `RandomHashTest` |
| Randomized hashing (demo) | Randomized hash | `randomized/RandomizedHash` | educational hash demo (not disguised HashMap) | `POST /api/randomized/hash` | Rand | O(1) op | `RandomHashTest` |
| Randomized hashing (demo) | Randomized result | `randomized/RandomizedResult` | wraps randomization evidence | transversal | Rand | - | `RandomHashTest` |
| Streaming | Reservoir sampling | `randomized/ReservoirSampling` | uniform K-sample of N logs, O(K) storage | `POST /api/randomized/reservoir` | Rand | O(N) time, O(K) space | `ReservoirSamplingTest` |

### Module 6B - Parallel Algorithms

| Syllabus | Algorithm / component | Java class | Feature | Endpoint | UI | Complexity | Test |
|---|---|---|---|---|---|---|---|
| Parallel reduction | `ParallelReduce` | `parallel/ParallelReduce` | sum/count/max/error-count, seq vs par | `POST /api/parallel/reduce` | Par | work O(n), span O(log n) | `ParallelReduceTest` |
| Parallel scan | Prefix scan (Blelloch) | `parallel/ParallelPrefixScan` | work/span explained + measured | `POST /api/parallel/scan` | Par | work O(n), span O(log n) | `ParallelPrefixScanTest` |
| Parallel sort | `ParallelSort` | `parallel/ParallelSort` | seq vs parallel comparison | `POST /api/parallel/sort` | Par | work O(n log n) | `ParallelSortTest` |
| Performance analysis | Work/span analyzer | `parallel/WorkSpanAnalyzer` | derives work/span from schedule | part of parallel results | Par | - | `ParallelCrossCheckTest` |
| Performance lab | Parallel benchmark | `parallel/ParallelBenchmark`, `parallel/BenchmarkResult` | measured speedup, sizes 10k..1M where practical | `POST /api/benchmark/run` | Benchmark Lab | measured | `BenchmarkControllerTest` |
| Verification | parallel == sequential | `ParallelCrossCheckTest` | reduces/scans/sorts equal sequential | - | - | - | `ParallelCrossCheckTest` |

---

## 2. Enumeration Contracts

`QueryType`: PATTERN_SEARCH, FUZZY_SEARCH, MULTI_PATTERN_SEARCH, DOCUMENT_SIMILARITY,
SEQUENCE_ALIGNMENT, SUFFIX_ANALYSIS, SERVICE_FLOW, MIN_CUT, APPROXIMATE_COVER, PRIMALITY_TEST,
STREAM_SAMPLE, BENCHMARK, EDIT_DISTANCE, GLOBAL_ALIGNMENT, LOCAL_ALIGNMENT, INTERVAL_DP,
BITMASK_DP, TREE_DP, SOS_DP, MATCHING, MIN_COST_FLOW, SET_COVER, RANDOMIZED_SORT, PARALLEL_REDUCE,
PARALLEL_SCAN, PARALLEL_SORT.

`AlgorithmType`: NAIVE, KMP, Z, RABIN_KARP, AHO_CORASICK, SUFFIX_ARRAY, KASAI_LCP, LEVENSHTEIN,
DAMERAU_LEVENSHTEIN, WEIGHTED_EDIT_DISTANCE, NEEDLEMAN_WUNSCH, SMITH_WATERMAN, MATRIX_CHAIN,
BITMASK_TSP, HAMILTONIAN_PATH, TREE_DIAMETER, REROOTING_DP, SOS_DP, FORD_FULKERSON, EDMONDS_KARP,
DINIC, MIN_CUT, BIPARTITE_MATCHING, MIN_COST_MAX_FLOW, VERTEX_COVER, MAXIMAL_MATCHING, SET_COVER,
MILLER_RABIN, RESERVOIR_SAMPLING, UNIVERSAL_HASH, RANDOMIZED_QUICKSORT, PARALLEL_REDUCE,
PARALLEL_PREFIX_SCAN, PARALLEL_SORT, BENCHMARK.

---

## 3. Coverage Registers

### 3.1 Every syllabus topic has at least one implementation
String matching: naive, KMP, Z, Rabin-Karp, multi-pattern Aho-Corasick, suffix array + LCP.
DP: 3 edit-distance variants, global + local alignment, interval (matrix chain), bitmask (TSP +
Hamiltonian), tree (diameter + rerooting), SOS subset DP.
Flow: Ford-Fulkerson, Edmonds-Karp, Dinic, min-cut, bipartite matching, min-cost max-flow (SSP).
Approximation: maximal matching, 2-approx vertex cover, FPT bounded-branching vertex cover, vertex cover
kernelization, knapsack FPTAS, greedy set cover, VC<->independent-set and clique<->complement reductions.
Randomized: Miller-Rabin, reservoir sampling, universal hashing, randomized quicksort.
Parallel: reduce, prefix scan, sort, work/span analysis, benchmark.

### 3.2 Explicitly conceptual (documented, not implemented - stated honestly)
- SA-IS O(n) suffix construction - `docs/05-string-algorithms.md`.
- Cycle-cancelling min-cost flow - `docs/07-network-flow.md`.
- NP-completeness theory body - `docs/np-completeness.md`.

### 3.3 Cross-evaluation guarantees
- `FlowCrossCheckTest`: Ford-Fulkerson == Edmonds-Karp == Dinic on the same graphs.
- String cross-checks (in `docs/13-testing.md`): Naive == KMP == Z == Rabin-Karp match sets.
- `ParallelCrossCheckTest`: parallel reduce/scan/sort == sequential results.

---

## 4. How to Read This Table

For a given syllabus topic the row gives you: the class to open in source, the feature that exposes
it, the endpoint to call, the page to open in the UI, the complexity reported, and the test that
proves it. If a row exists here, the implementation must exist; the final acceptance review (Phase
15/16) verifies the mapping is 100% satisfied.