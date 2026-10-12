package com.loginsight.dsa.approximation;

/**
 * Independent, deterministic fixtures and brute-force oracles for the approximation tests (docs/13:
 * randomised data uses fixed seeds; oracles must be separate from the algorithms).
 *
 * <p>Every oracle here is deliberately tiny and exhaustive: subset enumeration for vertex
 * cover / independent set / clique, and a straightforward weight-based exact DP for 0/1 knapsack.
 * They are <em>not</em> packaged with production code and are used only by the tests.</p>
 */
final class ApproxTestSupport {

    private long state;

    ApproxTestSupport(long seed) {
        this.state = seed == 0L ? 0x9E3779B97F4A7C15L : seed;
    }

    private long nextLong() {
        state ^= state << 13;
        state ^= state >>> 7;
        state ^= state << 17;
        return state >>> 1;
    }

    int nextInt(int bound) {
        return (int) (nextLong() % bound);
    }

    /** Random simple undirected graph on {@code n} vertices with 0..maxEdges sampled edges. */
    UndirectedGraph randomGraph(int n, int maxEdges) {
        UndirectedGraph graph = new UndirectedGraph(n);
        int target = maxEdges == 0 ? 0 : nextInt(maxEdges + 1);
        for (int i = 0; i < target; i++) {
            int u = nextInt(n);
            int v = nextInt(n);
            if (u != v) {
                graph.addEdge(u, v);
            }
        }
        return graph;
    }

    /**
     * Random knapsack instance: {@code n} items, positive weights in {@code [1, maxWeight]}, values in
     * {@code [0, maxValue]}, capacity in {@code [0, 2*sumWeight/n]}.
     */
    long[][] randomKnapsack(int n, int maxWeight, int maxValue) {
        long[] weights = new long[n];
        long[] values = new long[n];
        long weightSum = 0;
        for (int i = 0; i < n; i++) {
            weights[i] = 1 + nextInt(maxWeight);
            values[i] = nextInt(maxValue + 1);
            weightSum += weights[i];
        }
        long capacity = weightSum == 0 ? 0 : nextInt((int) (2 * weightSum / n) + 1);
        return new long[][]{weights, values, {capacity}};
    }

    /** Exact minimum vertex cover size by exhaustive subset enumeration. */
    static int exactMinimumVertexCover(UndirectedGraph graph) {
        int n = graph.vertexCount();
        int best = n;
        for (long mask = 0; mask < (1L << n); mask++) {
            if (isCover(graph, mask)) {
                best = Math.min(best, Long.bitCount(mask));
            }
        }
        return best;
    }

    static int exactMaximumIndependentSet(UndirectedGraph graph) {
        int n = graph.vertexCount();
        int best = 0;
        for (long mask = 0; mask < (1L << n); mask++) {
            if (isIndependent(graph, mask)) {
                best = Math.max(best, Long.bitCount(mask));
            }
        }
        return best;
    }

    /** Exact 0/1 knapsack value by a weight-based DP (independent of the FPTAS scaling). */
    static long exactKnapsack(long[] weights, long[] values, long capacity) {
        long[] best = new long[(int) capacity + 1];
        for (int i = 0; i < weights.length; i++) {
            for (long w = capacity; w >= weights[i]; w--) {
                long candidate = best[(int) (w - weights[i])] + values[i];
                if (candidate > best[(int) w]) {
                    best[(int) w] = candidate;
                }
            }
        }
        return best[(int) capacity];
    }

    private static boolean isCover(UndirectedGraph graph, long mask) {
        for (int id = 0; id < graph.edgeCount(); id++) {
            UndirectedEdge e = graph.edge(id);
            if ((mask & (1L << e.getU())) == 0 && (mask & (1L << e.getV())) == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIndependent(UndirectedGraph graph, long mask) {
        for (int id = 0; id < graph.edgeCount(); id++) {
            UndirectedEdge e = graph.edge(id);
            if ((mask & (1L << e.getU())) != 0 && (mask & (1L << e.getV())) != 0) {
                return false;
            }
        }
        return true;
    }
}