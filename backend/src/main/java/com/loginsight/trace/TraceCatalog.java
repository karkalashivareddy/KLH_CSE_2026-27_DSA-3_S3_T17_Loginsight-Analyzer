package com.loginsight.trace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static catalogue describing every trace-capable algorithm exposed by the Algorithm Laboratory.
 *
 * <p>The catalogue is metadata for the front-end workspace: each entry declares the canonical
 * algorithm key, its academic category (Strings / Dynamic Programming / Graph &amp; Flow /
 * Approximation / Randomized / Parallel), a human-readable name, the REST endpoint that produces a
 * trace, the default demonstration input, and the theoretical complexity statements the
 * implementation honours. It shares no algorithm logic — the execution lives in the DSA classes.</p>
 */
public final class TraceCatalog {

    private TraceCatalog() {
    }

    public record Entry(String key, String category, String name, String endpoint,
                        Map<String, Object> defaultInput, String timeComplexity,
                        String spaceComplexity, String description) {
    }

    /** Ordered catalogue (category order matches the laboratory sidebar). */
    public static final List<Entry> ENTRIES = List.of(
            new Entry("naive", "Strings", "Naive Pattern Search", "/api/trace/search/naive",
                    Map.of("text", "ABABABABABABC", "pattern", "ABABC"), "O(n * m)", "O(1)",
                    "Baseline brute-force search: every start position compared from scratch."),
            new Entry("kmp", "Strings", "KMP Pattern Search", "/api/trace/search/kmp",
                    Map.of("text", "ABABABCABABABC", "pattern", "ABABC"), "O(n + m)", "O(m) LPS",
                    "Knuth-Morris-Pratt with hand-built LPS failure function; the text cursor never walks backwards."),
            new Entry("z", "Strings", "Z-Algorithm Search", "/api/trace/search/z",
                    Map.of("text", "ABCABABCABABC", "pattern", "ABABC"), "O(n + m)", "O(n + m)",
                    "Z-function over pattern + separator + text with the linear [l, r] window."),
            new Entry("rabinkarp", "Strings", "Rabin-Karp Search", "/api/trace/search/rabin-karp",
                    Map.of("text", "GCDGDBABCCDB", "pattern", "ABCD"), "O(n + m) avg", "O(1)",
                    "Polynomial rolling hash with mandatory character verification and collision counting."),
            new Entry("levenshtein", "Dynamic Programming", "Levenshtein Edit Distance",
                    "/api/trace/dp/levenshtein", Map.of("a", "kitten", "b", "sitting"),
                    "O(n * m)", "O(n * m) DP table",
                    "Wagner-Fischer DP with per-cell transition recording and traceback edit script."),
            new Entry("matrixchain", "Dynamic Programming", "Matrix Chain Ordering",
                    "/api/trace/dp/matrix-chain", Map.of("dims", new int[]{10, 20, 30, 40, 50}),
                    "O(n^3)", "O(n^2)",
                    "Interval DP choosing the cheapest parenthesisation; records every candidate split."),
            new Entry("fordfulkerson", "Graph & Flow", "Ford-Fulkerson Max Flow",
                    "/api/trace/flow/ford-fulkerson",
                    Map.of("source", "S", "sink", "T", "nodes", new String[]{"S", "A", "B", "T"},
                            "edges", List.of(
                                    Map.of("from", "S", "to", "A", "capacity", 10L),
                                    Map.of("from", "S", "to", "B", "capacity", 5L),
                                    Map.of("from", "A", "to", "B", "capacity", 5L),
                                    Map.of("from", "A", "to", "T", "capacity", 5L),
                                    Map.of("from", "B", "to", "T", "capacity", 10L))),
                    "O(E * f_max)", "O(V + E)",
                    "DFS-based residual augmentation; records each discovered path and its bottleneck."),
            new Entry("edmondskarp", "Graph & Flow", "Edmonds-Karp Max Flow",
                    "/api/trace/flow/edmonds-karp",
                    Map.of("source", "S", "sink", "T", "nodes", new String[]{"S", "A", "B", "T"},
                            "edges", List.of(
                                    Map.of("from", "S", "to", "A", "capacity", 10L),
                                    Map.of("from", "S", "to", "B", "capacity", 5L),
                                    Map.of("from", "A", "to", "B", "capacity", 5L),
                                    Map.of("from", "A", "to", "T", "capacity", 5L),
                                    Map.of("from", "B", "to", "T", "capacity", 10L))),
                    "O(V * E^2)", "O(V + E)",
                    "BFS shortest-path residual augmentation; each augmenting path is the shortest one."),
            new Entry("dinic", "Graph & Flow", "Dinic Max Flow",
                    "/api/trace/flow/dinic",
                    Map.of("source", "S", "sink", "T", "nodes", new String[]{"S", "A", "B", "T"},
                            "edges", List.of(
                                    Map.of("from", "S", "to", "A", "capacity", 10L),
                                    Map.of("from", "S", "to", "B", "capacity", 5L),
                                    Map.of("from", "A", "to", "B", "capacity", 5L),
                                    Map.of("from", "A", "to", "T", "capacity", 5L),
                                    Map.of("from", "B", "to", "T", "capacity", 10L))),
                    "O(V^2 * E)", "O(V + E)",
                    "Level graph + blocking flow rounds with current-arc pruning."),
            new Entry("vertexcover", "Approximation", "Vertex Cover 2-Approximation",
                    "/api/trace/approx/vertex-cover",
                    Map.of("nodes", new String[]{"A", "B", "C", "D", "E"},
                            "edges", List.of(
                                    new String[]{"A", "B"}, new String[]{"B", "C"},
                                    new String[]{"C", "D"}, new String[]{"D", "E"},
                                    new String[]{"E", "A"}, new String[]{"A", "C"})),
                    "O(E)", "O(V)",
                    "Greedy maximal matching 2-approximation; records every matched edge and the bound."),
            new Entry("quicksort", "Randomized", "Randomized QuickSort",
                    "/api/trace/random/quicksort",
                    Map.of("values", new long[]{5, 1, 4, 2, 8, 0, 9, 3}, "seed", 42L),
                    "O(n log n) expected", "O(log n) expected stack",
                    "Random pivot + Hoare partition; records pivot choices and partition completions."),
            new Entry("millerrabin", "Randomized", "Miller-Rabin Primality Test",
                    "/api/trace/random/prime",
                    Map.of("n", 121L, "rounds", 20), "O(k log^2 n)", "O(1)",
                    "Monte Carlo primality test; records n-1 = d * 2^s, every witness and squaring."),
            new Entry("reservoir", "Randomized", "Reservoir Sampling",
                    "/api/trace/random/sample",
                    Map.of("k", 3, "values", new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, "seed",
                            7L),
                    "O(n) time, O(k) space", "O(k)",
                    "Algorithm R uniform sample from a stream; records every fill/replace/skip decision.")
    );

    public static LinkedHashMap<String, Object> asMap(Entry entry) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("key", entry.key());
        out.put("category", entry.category());
        out.put("name", entry.name());
        out.put("endpoint", entry.endpoint());
        out.put("defaultInput", entry.defaultInput());
        out.put("timeComplexity", entry.timeComplexity());
        out.put("spaceComplexity", entry.spaceComplexity());
        out.put("description", entry.description());
        return out;
    }

    public static List<LinkedHashMap<String, Object>> all() {
        return ENTRIES.stream().map(TraceCatalog::asMap).toList();
    }
}