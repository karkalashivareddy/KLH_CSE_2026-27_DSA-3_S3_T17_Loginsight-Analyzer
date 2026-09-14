# 13 - Testing Strategy

## 1. Objectives

- Prove every implemented algorithm is **correct**, not just executable.
- Prove **independent implementations agree** (cross-checks) - the strongest evidence of correctness.
- Cover edge cases from the syllabus and adversarial inputs.
- Keep the suite fast and deterministic; live measurements stay out of unit tests (they belong to
  the benchmark suite in `docs/14-benchmarking.md`).

## 2. Stack

- JUnit 5 (Jupiter) via `spring-boot-starter-test`; AssertJ for fluent assertions.
- MockMvc for controller tests.
- Tests run with `mvnw test` (and `mvnw verify` runs checkstyle/spotbugs if configured in Phase 1).

## 3. Test Pyramid

| Layer | Location | What |
|---|---|---|
| Algorithm unit tests | `src/test/java/com/loginsight/dsa/**` | correctness, edge cases, intermediate structures |
| Cross-check tests | same tree (`*CrossCheckTest`) | independent-implementation agreement |
| Parser tests | `.../parser/` | text, JSONL, malformed inputs, partial failures |
| Service tests | `.../service/` | dataset lifecycle, graph building |
| Query tests | `.../query/` | dispatcher routing, validation |
| Controller tests | `.../controller/` | MockMvc: endpoint contract, 400s, envelope shape |

## 4. Algorithm Unit-Test Catalogue (normal + edge cases)

### String (`dsa/string`)
- `NaiveMatcherTest` - basic matches, no match, pattern == text, pattern longer than text,
  repeated-char text, overlapping matches.
- `KMPMatcherTest` - LPS correctness for `ABABC`, `AAAAB`, single-char; match sets; empty pattern
  rejected; boundary n=1/m=1.
- `ZAlgorithmTest` - Z-array correctness; search equivalence with KMP.
- `RabinKarpTest` - rolling hash updates; collision candidates produce wrong-hash but verify step
  rejects; double-hash path; hash equality edge cases.
- `AhoCorasickTest` - single/multiple patterns, overlapping patterns, duplicates, patterns that are
  prefixes of each other, empty text, trie failure links on `he/she/his/hers`, output links.
- `SuffixArrayTest` - naive cross-check of suffix array on small random strings (rank property),
  single-char strings, empty string.
- `KasaiLCPTest` - LCP correctness on known strings, longest common substring extraction.
- `StringCrossCheckTest` - for property strings (hand-built + small random): Naive == KMP == Z ==
  Rabin-Karp match sets.

### DP (`dsa/dp`)
- `LevenshteinTest` - 0/1 distances, empty strings, transpose case (=2 without Damerau), matrix
  reconstruction, weighted variants.
- `DamerauTest` - transposition gives 1 where Levenshtein gives 2 (`authentication` typos),
  mixed operations.
- `NeedlemanWunschTest` - global alignment score, gap penalties, traceback paths, one-track tests.
- `SmithWatermanTest` - local alignment zero-clamping, the USER-DB-PAYMENT example, empty overlap.
- `MatrixChainTest` - classic `A(10x30) B(30x5) C(5x60)` = 4500, n=1/2, descending sizes.
- `BitmaskTSPTest` - brute-force cross-check on n<=8 distance matrices, asymmetry, unreachable.
- `HamiltonianPathTest` - existence/non-existence, cycle variant, single node.
- `TreeDpTest` - diameter on path/star/interior-weighted trees, rerooting consistency (values
  independent of root).
- `SosDpTest` - F for all masks vs naive subset loop, n=1..10.

### Flow (`dsa/flow`)
- `FordFulkersonTest`, `EdmondsKarpTest`, `DinicTest` - the shared **standard test graph**
  (S-A-B-T, S-C-D-T, cross edges, zero-capacity edges), disconnected graphs, cycles, sink
  unreachable (flow 0), multiple parallel edges forbidden at parse layer, big-ish chain graph.
- `MinCutTest` - source side + cut capacity == max flow (validated by all three engines).
- `BipartiteMatchingTest` - incidents-to-resources, empty side, perfect vs imperfect matching.
- `MinCostMaxFlowTest` - min-cost vs brute-force on small networks, negative-cost avoidance,
  infeasible demand.
- `FlowCrossCheckTest` - **FF == EK == Dinic** on a battery of graphs (including random small
  graphs, forced bad FF cases on integers).

### Approximation (`dsa/approximation`)
- `MaximalMatchingTest` - greedy matching is valid (endpoint-disjoint) and maximal (no strictly larger
  matching extends it on small graphs).
- `VertexCoverApproximationTest` - cover is valid (every edge covered), size <= 2*OPT on small graphs
  solved by subset-enumeration oracle, determinism, self-loops force their vertex, duplicates per the
  simple-graph contract, isolated vertices, empty graph, disconnected graphs.
- `SetCoverDemoTest` - greedy covers universe, empty family/universe, duplicates inside a set,
  H(n) bound reporting.
- `ReductionsTest` - S is a cover iff V-S is independent; clique in G iff independent set in complement(G),
  on tiny graphs by direct definition.
- `BoundedVertexCoverTest` - FPT decision/certificate matches the brute-force oracle on seeded small
  graphs, k=0, edgeless, disconnected, k too small.
- `KernelizationTest` - reduction preserves tau(G) <= k (oracle on original vs reduced + forced),
  high-degree correctness, infeasible cases.
- `KnapsackFPTASTest` - vs independent exact DP on seeded random instances; A >= (1-eps)*OPT; empty/one
  item/capacity edge cases; varying eps.
- `ApproximationExperimentTest` - deterministic exact-vs-approx-vs-FPT comparison demo (OPT, approximate
  cover, ratio, FPT decision for selected k) on fixed graphs.

### Randomized (`dsa/randomized`)
- `MillerRabinTest` - small primes/composites, Carmichael number (561), safe ranges, deterministic
  verdicts for what the implementation actually supports, rounds behavior.
- `ReservoirSamplingTest` - size N <= K (whole stream), uniform distribution sanity over many runs
  (statistical, tolerance-based), K=1.
- `RandomHashTest` - same key + same seed reproducible, different seeds spread, collision
  probability bounded, universal family property (pairwise independence check on small universe).
- `RandomizedQuicksortTest` - sorted output == sorted reference, unstable-sort immaterial, empty
  arrays, duplicates; comparison-count sanity.

### Parallel (`dsa/parallel`)
- `ParallelReduceTest` - sum/count/max/error-count results equal sequential across sizes incl.
  small n (threshold falls back to sequential).
- `ParallelPrefixScanTest` - inclusive/exclusive scan equals sequential scan; work/span sanity.
- `ParallelSortTest` - output sorted and equals sequential sort results.
- `ParallelCrossCheckTest` - reduce/scan/sort parallel == sequential on shared datasets, including
  n below parallelism threshold.

## 5. Explicit Edge-Case Catalogue (required coverage)

- Empty input (text, pattern, arrays, stream).
- Single-element input (1-char patterns, n=1 matrices, 1-node graphs).
- Repeated characters / repeated patterns / overlapping matches.
- Pattern longer than text (0 matches, no crash).
- Collision candidates in Rabin-Karp (constructed collisions where feasible; verified-step rejects).
- Duplicate patterns in Aho-Corasick (reported once per occurrence set, no double/lost matches).
- Disconnected graphs, zero-capacity edges, cycles, unreachable sink (flow = 0).
- DP boundaries: strings of length 1 and 2, alignment of empty sequence, matrix chain of size 2,
  TSP on 2 and 3 vertices, SOS with n=1.
- Invalid parameters: empty pattern, negative capacities/costs, k out of range, invalid source/sink,
  n out of range - all `InvalidQueryException` -> HTTP 400 at controller level.
- Large inputs: string search on ~1M chars (bounded check), stream of 100k for reservoir, parallel
  n = 1M (best-effort, machine-dependent).

## 6. Property / Cross-Check Testing (the core evidence)

Independent implementations MUST agree:

| Property | Check |
|---|---|
| Match sets | Naive == KMP == Z == Rabin-Karp (locations identical) |
| Hash integrity | RK verified matches == naive matches; collisions counted when measurable |
| LCP vs brute force | LCP array cross-verified against naive suffix comparison on small strings |
| Max flow | Ford-Fulkerson == Edmonds-Karp == Dinic values on the same graphs |
| Min cut | cut capacity == max flow for every engine |
| Matching | bipartite matching size == max-flow value in the constructed network |
| DP vs brute force | TSP and Hamiltonian cross-checked against exhaustive search (n <= 8) |
| Edit distance | Levenshtein brute force (recursive memoised, n <= 6) == DP result |
| Sorted results | randomized quicksort and parallel sort == gold-standard sorted array |
| Sampling | reservoir sample membership subset-of-stream and uniformity tolerance |

## 7. Controller Tests (MockMvc)

- Health returns 200 + expected body.
- Upload valid text + malformed file -> mixed result with per-line errors, import continues.
- `POST /api/search/kmp` with empty pattern -> 400; unknown dataset id -> 404.
- Envelope contract test: every algorithm endpoint returns `algorithm`, `queryType`, `inputSize`,
  `result`, `intermediateData`, `executionTimeNanos`, `timeComplexity`, `spaceComplexity`.
- Flow invalid source/sink -> 400; negative capacity -> 400.
- CORS preflight from dev origin returns allowed headers.

## 8. Coverage Targets and Tracking

- Target: >= 80% line coverage on `dsa/**` (measured by jacoco plugin configured in Phase 1).
- Every class in the `docs/04` mapping table is covered by at least one test class.
- CI-less but reproducible: `mvnw clean verify` from a clean checkout must pass (documented run
  order and JDK requirement in README).

## 9. Where Results Are Recorded

- Test report: `backend/target/surefire-reports`.
- Cross-check results logged at INFO in test output for review.
- Long-running/large-parameter tests are tagged `@Tag("slow")` and excluded from the default run;
  they run explicitly in a `verify` profile or Perf phase to keep the demo loop fast.