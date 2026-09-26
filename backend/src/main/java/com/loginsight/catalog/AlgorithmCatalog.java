package com.loginsight.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.loginsight.trace.TraceCatalog;

/**
 * Static catalogue of every implemented algorithm in the laboratory (docs/REBUILD_BASELINE
 * Phase-2). Six DSA-3 modules: String Algorithms, Advanced DP, Network Flow, Approximation /
 * NP-Completeness, Randomized, Parallel.
 *
 * <p>Integrity rules (enforced by {@code AlgorithmCatalogTest}): keys are unique, module ids
 * belong to the module set, {@code tracked} holds exactly when a trace endpoint exists, complexity
 * strings always come from the class-level claims of the owning DSA class, and library-only
 * entries (FPT / kernalisation / FPTAS / reductions / perfect hashing) are marked
 * {@code exposed=false} so the UI never presents them as reachable.</p>
 */
public final class AlgorithmCatalog {

    private AlgorithmCatalog() {
    }

    private static final Map<String, Map<String, Object>> DEFAULT_INPUTS = new LinkedHashMap<>();

    /**
     * Default inputs for exposed algorithms that are not trace-instrumented (so they are not in
     * {@link TraceCatalog}). They match the request DTO of the canonical endpoint exactly, so the
     * Laboratory opens with a runnable example instead of an empty {@code {}} editor.
     */
    private static final Map<String, Map<String, Object>> EXTRA_DEFAULT_INPUTS = new LinkedHashMap<>();

    static {
        for (TraceCatalog.Entry entry : TraceCatalog.ENTRIES) {
            DEFAULT_INPUTS.put(entry.key(), entry.defaultInput());
        }
        extra("aho_corasick", input(
                "patterns", List.of("error", "timeout"),
                "text", "gateway error at 03:14, cache timeout at 03:15",
                "scope", "EXPLICIT"));
        extra("suffix_array", input("text", "banana bandana papaya"));
        extra("suffix_search", input("text", "banana bandana papaya", "pattern", "ana"));
        extra("fuzzy_search", input("query", "error", "maxDistance", 2,
                "text", "an error occurred while loading config\na warning was logged"));
        extra("damerau", input("a", "alex", "b", "axel"));
        extra("weighted_edit", input("a", "kitten", "b", "sitting",
                "insertCost", 1, "deleteCost", 1, "substituteCost", 2));
        extra("global_alignment", input("a", "ATTACA", "b", "ATGCTA"));
        extra("local_alignment", input("a", "ATCGT", "b", "ACGGT"));
        extra("optimal_bst", input("freqs", List.of(2, 3, 1, 4)));
        extra("bitmask_tsp", input("costs", List.of(
                        List.of(0L, 10L, 15L, 20L),
                        List.of(10L, 0L, 35L, 25L),
                        List.of(15L, 35L, 0L, 30L),
                        List.of(20L, 25L, 30L, 0L)),
                "start", 0));
        extra("hamiltonian", input(
                "from", List.of(0, 1, 2, 3), "to", List.of(1, 2, 3, 0), "start", 0));
        extra("tree_diameter", input(
                "from", List.of(0, 0, 1), "to", List.of(1, 2, 3), "vertexCount", 4));
        extra("rerooting", input(
                "from", List.of(0, 0, 1), "to", List.of(1, 2, 3), "vertexCount", 4));
        extra("sos", input("values", List.of(1L, 2L, 4L, 8L, 3L, 1L, 9L, 2L), "bits", 3));
        extra("min_cut", flowExample());
        extra("bipartite_matching", input(
                "incidents", List.of("i1", "i2", "i3"),
                "resources", List.of("r1", "r2"),
                "edges", List.of(
                        List.of("i1", "r1"), List.of("i1", "r2"),
                        List.of("i2", "r2"), List.of("i3", "r1"))));
        extra("min_cost_max_flow", input(
                "suppliers", List.of("s1", "s2"),
                "demand", List.of("c1", "c2"),
                "costEdges", List.of(
                        List.of("s1", "c1", "5"), List.of("s1", "c2", "2"),
                        List.of("s2", "c1", "1"), List.of("s2", "c2", "6"))));
        extra("set_cover", input(
                "universe", List.of("1", "2", "3", "4", "5"),
                "sets", new LinkedHashMap<>(Map.of(
                        "s1", new String[]{"1", "2"},
                        "s2", new String[]{"2", "3", "4"},
                        "s3", new String[]{"4", "5"},
                        "s4", new String[]{"1", "5"}))));
        extra("incident_cover", input(
                "services", List.of("S1", "S2", "S3"),
                "relationships", List.of(List.of("S1", "S2"), List.of("S2", "S3"))));
        extra("universal_hash", input("text", "hello world", "seed", 12345L, "m", 101));
        extra("parallel_reduce", input("op", "ERROR_COUNT", "size", 256, "parallelism", 4, "marker", 1));
        extra("parallel_scan", input("size", 64, "parallelism", 4));
        extra("parallel_sort", input("size", 2048, "parallelism", 4));
        DEFAULT_INPUTS.putAll(EXTRA_DEFAULT_INPUTS);
    }

    private static void extra(String key, Map<String, Object> body) {
        EXTRA_DEFAULT_INPUTS.put(key, body);
    }

    private static Map<String, Object> input(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return out;
    }

    private static Map<String, Object> flowExample() {
        return input(
                "source", "S", "sink", "T",
                "nodes", List.of("S", "A", "B", "C", "T"),
                "edges", List.of(
                        edge("S", "A", 10L), edge("S", "B", 5L), edge("A", "B", 5L),
                        edge("A", "C", 5L), edge("B", "C", 5L), edge("B", "T", 10L),
                        edge("C", "T", 10L)));
    }

    private static Map<String, Object> edge(String from, String to, long capacity) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", from);
        out.put("to", to);
        out.put("capacity", capacity);
        return out;
    }

    private static final List<AlgorithmInfo> ALGORITHMS = build();

    public static List<AlgorithmInfo> algorithms() {
        return ALGORITHMS;
    }

    public static Optional<AlgorithmInfo> byKey(String key) {
        for (AlgorithmInfo info : ALGORITHMS) {
            if (info.key().equals(key)) {
                return Optional.of(info);
            }
        }
        return Optional.empty();
    }

    private static AlgorithmInfo entry(String key, String name, String moduleId,
                                       String moduleLabel, String problem, String queryType,
                                       String algorithmType, String canonicalEndpoint,
                                       String traceEndpoint, String time, String space,
                                       boolean tracked, String description) {
        return new AlgorithmInfo(key, name, moduleId, moduleLabel, problem, queryType,
                algorithmType, canonicalEndpoint, traceEndpoint, time, space, tracked,
                canonicalEndpoint != null || traceEndpoint != null,
                DEFAULT_INPUTS.get(key), description);
    }

    private static AlgorithmInfo library(String key, String name, String moduleId,
                                         String moduleLabel, String problem, String queryType,
                                         String algorithmType, String time, String space,
                                         String description) {
        return new AlgorithmInfo(key, name, moduleId, moduleLabel, problem, queryType,
                algorithmType, null, null, time, space, false, false, null, description);
    }

    private static List<AlgorithmInfo> build() {
        return List.of(
                entry("naive", "Naive Pattern Search", "strings", "Strings",
                        "Baseline pattern search over the log corpus",
                        "PATTERN_SEARCH", "NAIVE", "/api/search/naive", "/api/trace/search/naive",
                        "O(n·m)", "O(1)", true,
                        "Brute-force search: every start position is compared from scratch; "
                                + "the baseline every advanced matcher must beat."),
                entry("kmp", "KMP Pattern Search", "strings", "Strings",
                        "Linear-time pattern search with a failure function",
                        "PATTERN_SEARCH", "KMP", "/api/search/kmp", "/api/trace/search/kmp",
                        "O(n+m)", "O(m) LPS", true,
                        "Knuth-Morris-Pratt: a hand-built LPS table lets the text cursor never move "
                                + "backwards."),
                entry("z", "Z-Algorithm Search", "strings", "Strings",
                        "Linear-time pattern search via the Z-function",
                        "PATTERN_SEARCH", "Z", "/api/search/z", "/api/trace/search/z",
                        "O(n+m)", "O(n+m)", true,
                        "Z-function over the pattern + separator + text with a maintained [l, r] "
                                + "window."),
                entry("rabinkarp", "Rabin-Karp Search", "strings", "Strings",
                        "Rolling-hash pattern search with collision verification",
                        "PATTERN_SEARCH", "RABIN_KARP", "/api/search/rabin-karp",
                        "/api/trace/search/rabin-karp", "O(n+m) avg", "O(1)", true,
                        "Polynomial rolling hash; every candidate is character-verified so "
                                + "collisions are detected, not ignored."),
                entry("aho_corasick", "Aho-Corasick Multi-Pattern Search", "strings", "Strings",
                        "All nefarious patterns found in one pass",
                        "MULTI_PATTERN_SEARCH", "AHO_CORASICK", "/api/search/multi", null,
                        "O(n + Σ|P|)", "O(Σ|P|) trie", false,
                        "Dictionary trie with failure links and output links - a single stream pass "
                                + "reports every pattern of a batch."),
                entry("suffix_array", "Suffix Array Build", "strings", "Strings",
                        "Prefix-doubling suffix ordering for the corpus",
                        "SUFFIX_ANALYSIS", "SUFFIX_ARRAY", "/api/string/suffix/build", null,
                        "O(n log n)", "O(n)", false,
                        "Prefix-doubling construction; its LCP table (Kasai) powers phrase search."),
                entry("suffix_search", "Suffix-Array Phrase Search", "strings", "Strings",
                        "Every occurrence of a phrase in log lines",
                        "SUFFIX_ANALYSIS", "SUFFIX_ARRAY", "/api/string/suffix/search", null,
                        "O(m log n)", "O(n) + suffix array", false,
                        "Binary search over the suffix array to locate every phrase occurrence."),
                entry("kasai_lcp", "Kasai LCP", "strings", "Strings",
                        "Longest-common-prefix lifting of the suffix array",
                        "SUFFIX_ANALYSIS", "KASAI_LCP", null, null, "O(n)", "O(n)", false,
                        "Ranks the suffixes and fills LCP in a single amortized pass; no standalone "
                                + "endpoint."),
                entry("fuzzy_search", "Fuzzy Search (Levenshtein)", "strings", "Strings",
                        "Forgiving search inside log messages",
                        "FUZZY_SEARCH", "LEVENSHTEIN", "/api/fuzzy/search", null,
                        "O(lines · |q|)", "O(line · |q|) DP table", false,
                        "Per-line bounded edit distance; every result reports its real distance, "
                                + "never a fabricated score."),

                entry("levenshtein", "Levenshtein Edit Distance", "dp", "Dynamic Programming",
                        "Edit-script distance between two messages",
                        "EDIT_DISTANCE", "LEVENSHTEIN", "/api/dp/levenshtein",
                        "/api/trace/dp/levenshtein", "O(n·m)", "O(n·m) DP table", true,
                        "Wagner-Fischer DP that records the traceback edit script, not just the "
                                + "final number."),
                entry("damerau", "Damerau-Levenshtein Distance", "dp", "Dynamic Programming",
                        "Edit distance that also transposes adjacent characters",
                        "EDIT_DISTANCE", "DAMERAU_LEVENSHTEIN", "/api/dp/damerau", null,
                        "O(n·m)", "O(n·m)", false,
                        "Extends Levenshtein with the adjacent-transposition operation."),
                entry("weighted_edit", "Weighted Edit Distance", "dp", "Dynamic Programming",
                        "Edit distance with per-operation costs",
                        "EDIT_DISTANCE", "WEIGHTED_EDIT_DISTANCE", "/api/dp/weighted-edit", null,
                        "O(n·m)", "O(n·m)", false,
                        "Insert / delete / substitute costs are inputs, so the DP honours domain "
                                + "semantics."),
                entry("global_alignment", "Needleman-Wunsch Global Alignment", "dp",
                        "Dynamic Programming", "Aligned similarity of two sequences",
                        "GLOBAL_ALIGNMENT", "NEEDLEMAN_WUNSCH", "/api/dp/global", null, "O(n·m)",
                        "O(n·m)", false,
                        "Global DP alignment used here for document similarity; traces the optimal "
                                + "matching of tokens."),
                entry("local_alignment", "Smith-Waterman Local Alignment", "dp",
                        "Dynamic Programming", "Best common substring alignment",
                        "LOCAL_ALIGNMENT", "SMITH_WATERMAN", "/api/dp/local", null, "O(n·m)",
                        "O(n·m)", false,
                        "Semi-global alignment that locates the highest-scoring local fragment."),
                entry("matrixchain", "Matrix Chain Ordering", "dp", "Dynamic Programming",
                        "Cheapest parenthesisation of a chain product",
                        "INTERVAL_DP", "MATRIX_CHAIN", "/api/dp/matrix-chain",
                        "/api/trace/dp/matrix-chain", "O(n³)", "O(n²)", true,
                        "Interval DP that records every candidate split to justify the choice."),
                entry("optimal_bst", "Optimal Binary Search Tree", "dp",
                        "Dynamic Programming", "Cheapest weighted search tree",
                        "INTERVAL_DP", "OPTIMAL_BINARY_SEARCH_TREE", "/api/dp/obst", null, "O(n³)",
                        "O(n²)", false,
                        "Interval DP over weighted keys; Knuth's quadrangle optimisation is noted "
                                + "(not applied)."),
                entry("bitmask_tsp", "Held-Karp TSP", "dp", "Dynamic Programming",
                        "Exact TSP over small graphs",
                        "BITMASK_DP", "BITMASK_TSP", "/api/dp/tsp", null, "O(n²·2^n)", "O(n·2^n)",
                        false, "Subset-DP over bitmasks of visited cities - exact for small n, "
                                + "exponential by nature."),
                entry("hamiltonian", "Hamiltonian Path via Bitmask DP", "dp",
                        "Dynamic Programming", "Existence of a Hamiltonian path",
                        "BITMASK_DP", "HAMILTONIAN_PATH", "/api/dp/hamiltonian", null,
                        "O(2^n·n²)", "O(2^n·n)", false,
                        "Bitmask DP over the vertex subset; reports the visited order."),
                entry("tree_diameter", "Tree Diameter DP", "dp", "Dynamic Programming",
                        "Longest dependency chain in the service tree",
                        "TREE_DP", "TREE_DIAMETER", "/api/dp/tree", null, "O(n)", "O(n)", false,
                        "Two-pass tree DP (heights) that also reports the diameter path."),
                entry("rerooting", "Rerooting DP", "dp", "Dynamic Programming",
                        "All-root tree statistics in one orientation pass",
                        "TREE_DP", "REROOTING_DP", "/api/dp/rerooting", null, "O(n)", "O(n)", false,
                        "Computes sums for every possible root by re-using the parent orientation."),
                entry("sos", "Sum-over-Subsets DP", "dp", "Dynamic Programming",
                        "Subset lattice aggregation",
                        "SOS_DP", "SOS_DP", "/api/dp/sos", null, "O(n·2^n)", "O(2^n)", false,
                        "The standard SOS forward loop over a 2^n domain."),

                entry("fordfulkerson", "Ford-Fulkerson Max Flow", "flow", "Graph & Flow",
                        "Maximum service flow with DFS augmentation",
                        "SERVICE_FLOW", "FORD_FULKERSON", null, "/api/trace/flow/ford-fulkerson",
                        "O(E·fmax)", "O(V+E)", true,
                        "Residual-graph cascade augmenting along DFS paths; records each path and "
                                + "bottleneck. No direct endpoint (lab + traces only)."),
                entry("edmondskarp", "Edmonds-Karp Max Flow", "flow", "Graph & Flow",
                        "Maximum service flow with shortest augmentation",
                        "SERVICE_FLOW", "EDMONDS_KARP", "/api/flow/edmonds-karp",
                        "/api/trace/flow/edmonds-karp", "O(V·E²)", "O(V+E)", true,
                        "BFS-residual augmentation so every augmenting path is a shortest one."),
                entry("dinic", "Dinic Max Flow", "flow", "Graph & Flow",
                        "Maximum service flow with blocking flows",
                        "SERVICE_FLOW", "DINIC", "/api/flow/dinic", "/api/trace/flow/dinic",
                        "O(V²·E)", "O(V+E)", true,
                        "Level-graph BFS rounds with current-arc pruning and a blocking-flow DFS."),
                entry("min_cut", "Min-Cut Computation", "flow", "Graph & Flow",
                        "Bottleneck partition of the service graph",
                        "MIN_CUT", "MIN_CUT", "/api/flow/min-cut", null, "O(V²·E) with Dinic",
                        "O(V+E)", false,
                        "Reachability from the source on the residual graph of a max-flow value "
                                + "equalling the max flow (Dinic)."),
                entry("bipartite_matching", "Bipartite Matching", "flow", "Graph & Flow",
                        "One-to-one pairing through a unit-capacity flow",
                        "MATCHING", "BIPARTITE_MATCHING", "/api/flow/matching", null,
                        "O(E√V) unit Dinic", "O(V+E)", false,
                        "Unit-capacity flow reduction to Dinic on the bipartite decomposition."),
                entry("min_cost_max_flow", "Min-Cost Max Flow", "flow", "Graph & Flow",
                        "Cheapest feasible routing",
                        "MIN_COST_FLOW", "MIN_COST_MAX_FLOW", "/api/flow/min-cost", null,
                        "O(f·V·E) SSP", "O(V+E)", false,
                        "Successive shortest paths with potentials; reports cost and flow together."),

                entry("vertexcover", "Vertex Cover 2-Approximation", "approximation",
                        "Approximation", "Cover every dependency edge with few services",
                        "APPROXIMATE_COVER", "VERTEX_COVER", "/api/approx/vertex-cover",
                        "/api/trace/approx/vertex-cover", "O(E)", "O(V)", true,
                        "Greedy maximal matching 2-approximation. Vertex Cover is NP-hard; the "
                                + "ratio is guaranteed, the minimum is not."),
                entry("set_cover", "Set-Cover Greedy", "approximation", "Approximation",
                        "Cover requirements with the fewest sets",
                        "SET_COVER", "SET_COVER", "/api/approx/set-cover", null,
                        "greedy O(|U|·|S|)/round", "O(|U|+|S|)", false,
                        "Logarithmic-ratio greedy; demonstrates the NP-hard set-cover framing."),
                entry("incident_cover", "Incidence Cover Greedy", "approximation",
                        "Approximation", "Cover violation edges via incident services",
                        "APPROXIMATE_COVER", "MAXIMAL_MATCHING", "/api/approx/incident-cover", null,
                        "greedy O(|U|·|S|)/round", "O(|U|+|S|)", false,
                        "Set-cover dressing over incidence relationships; the greedy theme repeats."),
                entry("bounded_vertex_cover", "Bounded Vertex Cover (FPT)", "approximation",
                        "Approximation", "Exact vertex cover bounded by parameter k",
                        "APPROXIMATE_COVER", "VERTEX_COVER", null, null, "O(2^k·(V+E))",
                        "O(k·(V+E))", false,
                        "Branch on an edge till k removals; correctness is exact, hence FPT rather "
                                + "than an approximation."),
                entry("vertex_cover_kernelization", "Vertex Cover Kernelization", "approximation",
                        "Approximation", "FPT kernel for vertex cover",
                        "APPROXIMATE_COVER", "VERTEX_COVER", null, null, "O(V·E)", "O(V+E)", false,
                        "High-degree and matching-based kernel rules to shrink the instance."),
                entry("knapsack_fptas", "Knapsack FPTAS", "approximation", "Approximation",
                        "1+ε approximation of knapsack",
                        "APPROXIMATE_COVER", "SET_COVER", null, null, "O(n³/ε)", "O(n²/ε)", false,
                        "Value-scaling FPTAS giving the 1+ε trade-off over the classic pseudo-"
                                + "polynomial DP."),
                entry("vc_is_reduction", "Vertex-Cover / Independent-Set Reduction",
                        "approximation", "Approximation",
                        "Reduce VC to IS for hardness teaching",
                        "APPROXIMATE_COVER", "VERTEX_COVER", null, null, "O(V+E)", "O(V+E)", false,
                        "Complement-graph and reduction used to show NP-completeness directions."),

                entry("quicksort", "Randomized QuickSort", "randomized", "Randomized",
                        "Expected-optimal pivot-sort of service metrics",
                        "RANDOMIZED_SORT", "RANDOMIZED_QUICKSORT", "/api/random/quicksort",
                        "/api/trace/random/quicksort", "O(n log n) expected", "O(log n) expected",
                        true, "Random pivot with a seeded source; records pivot choices and "
                                + "partitioning."),
                entry("millerrabin", "Miller-Rabin Primality Test", "randomized", "Randomized",
                        "Probable-primality gate for huge numbers",
                        "PRIMALITY_TEST", "MILLER_RABIN", "/api/random/prime",
                        "/api/trace/random/prime", "O(k·log²n)", "O(1)", true,
                        "Monte Carlo: n-1 = d·2^s decomposition, base-witness squarings. "
                                + "Deterministic mode runs the fixed witness set when rounds≤0."),
                entry("reservoir", "Reservoir Sampling (Algorithm R)", "randomized",
                        "Randomized", "Uniform sample from a stream",
                        "STREAM_SAMPLE", "RESERVOIR_SAMPLING", "/api/random/sample",
                        "/api/trace/random/sample", "O(n)", "O(k)", true,
                        "One streaming pass with acceptance decisions that keep the sample uniform."),
                entry("universal_hash", "Universal Hashing", "randomized", "Randomized",
                        "Hashing with a random family",
                        "HASH", "UNIVERSAL_HASH", "/api/random/hash", null, "O(1) per op", "O(1)",
                        false, "A random coefficient family bounds collision probability across "
                                + "keys."),
                entry("perfect_hash", "Two-Level Perfect Hashing", "randomized", "Randomized",
                        "Worst-case O(1) membership",
                        "HASH", "UNIVERSAL_HASH", null, null, "O(1) worst case", "O(n)", false,
                        "FKS-style two-level scheme with expected-linear build; library class with "
                                + "tests."),

                entry("parallel_reduce", "Parallel Reduce", "parallel", "Parallel",
                        "Aggregate service metrics across workers",
                        "PARALLEL_REDUCE", "PARALLEL_REDUCE", "/api/parallel/reduce", null, "O(n) "
                                + "work, O(log n) span", "O(n/workers)", false,
                        "Work-stealing reduce reporting speedup, parallelism and efficiency "
                                + "(span analysed)."),
                entry("parallel_scan", "Parallel Prefix Scan", "parallel", "Parallel",
                        "Inclusive prefix over a partitioned array",
                        "PARALLEL_SCAN", "PARALLEL_PREFIX_SCAN", "/api/parallel/scan", null,
                        "O(n) work, O(log n) span", "O(n/workers)", false,
                        "Blelloch-style scan with an explicit work-span analysis."),
                entry("parallel_sort", "Parallel Sample Sort", "parallel", "Parallel",
                        "Sort metrics with parallel partitioning",
                        "PARALLEL_SORT", "PARALLEL_SORT", "/api/parallel/sort", null,
                        "O(n log n) work", "O(n/workers)", false,
                        "Sample-sort across workers; determinism is preserved by chunked merging."));
    }
}