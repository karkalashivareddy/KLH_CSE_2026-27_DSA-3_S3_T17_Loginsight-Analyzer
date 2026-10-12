package com.loginsight.dsa.approximation;

/**
 * Executable demonstration of the <strong>Vertex Cover ↔ Independent Set</strong> reduction.
 *
 * <h2>Theorem (reduction zoo, Module 5)</h2>
 * {@code S} is a vertex cover of {@code G} if and only if {@code V - S} is an independent set of
 * {@code G}. Consequently {@code minimum vertex cover size = |V| - maximum independent set size}.
 * The transformation flips membership, runs in {@code O(n + E)} time, and preserves the
 * answer — a polynomial reduction in the documentary sense.
 *
 * <h2>Why it holds</h2>
 * If {@code V - S} contained an edge {@code {u,v}}, then {@code S} would miss both endpoints and could
 * not cover that edge. Conversely, an edge entirely inside {@code S} is legal for a cover but would
 * make {@code V - S} fail independence, so cover validity and complement independence are the same
 * property.
 *
 * <h2>What is executable here</h2>
 * The transformation itself ({@link #coverToIndependentSet} / {@link #independentSetToCover}) plus the
 * membership verifiers used to check the equivalence on tiny graphs. Exact values must come from a
 * test oracle; this class never solves the optimization problem.
 */
public final class IndependentSetReduction {

    private IndependentSetReduction() {
    }

    /** A vertex selection, as vertex ids. */
    public static int[] selectedVertices(int n, boolean[] selection) {
        int[] values = new int[n];
        int count = 0;
        for (int v = 0; v < n; v++) {
            if (selection[v]) {
                values[count++] = v;
            }
        }
        int[] result = new int[count];
        System.arraycopy(values, 0, result, 0, count);
        return result;
    }

    /** A membership mask for a list of vertex ids. */
    public static boolean[] selectionMask(int n, int[] vertices) {
        boolean[] mask = new boolean[n];
        for (int v : vertices) {
            if (v < 0 || v >= n) {
                throw new IllegalArgumentException("vertex " + v + " out of range [0," + (n - 1) + "]");
            }
            mask[v] = true;
        }
        return mask;
    }

    /** All vertices not selected; {@code V - S} in {@code O(n)}. */
    public static int[] complement(int n, int[] selected) {
        boolean[] selectedMask = selectionMask(n, selected);
        int[] values = new int[n];
        int count = 0;
        for (int v = 0; v < n; v++) {
            if (!selectedMask[v]) {
                values[count++] = v;
            }
        }
        int[] result = new int[count];
        System.arraycopy(values, 0, result, 0, count);
        return result;
    }

    /** True when every edge of {@code g} has at least one chosen endpoint. */
    public static boolean isVertexCover(UndirectedGraph graph, boolean[] selection) {
        if (graph == null || selection == null || selection.length != graph.vertexCount()) {
            throw new IllegalArgumentException("graph or selection invalid");
        }
        for (int id = 0; id < graph.edgeCount(); id++) {
            UndirectedEdge edge = graph.edge(id);
            if (!selection[edge.getU()] && !selection[edge.getV()]) {
                return false;
            }
        }
        return true;
    }

    /** True when no edge has both endpoints selected. */
    public static boolean isIndependentSet(UndirectedGraph graph, boolean[] selection) {
        if (graph == null || selection == null || selection.length != graph.vertexCount()) {
            throw new IllegalArgumentException("graph or selection invalid");
        }
        for (int id = 0; id < graph.edgeCount(); id++) {
            UndirectedEdge edge = graph.edge(id);
            if (selection[edge.getU()] && selection[edge.getV()]) {
                return false;
            }
        }
        return true;
    }

    /** {@code V - S}: the independent set induced by a vertex cover. */
    public static int[] coverToIndependentSet(int n, int[] cover) {
        return complement(n, cover);
    }

    /** {@code V - I}: the vertex cover induced by an independent set. */
    public static int[] independentSetToCover(int n, int[] independentSet) {
        return complement(n, independentSet);
    }

    /**
     * The textbook identity: {@code tau(G) = n - alpha(G)}, where {@code tau} is the minimum vertex
     * cover size and {@code alpha} the maximum independent set size.
     */
    public static int minimumCoverSizeFromMaxIndependentSet(int n, int maxIndependentSetSize) {
        if (n < 0 || maxIndependentSetSize < 0 || maxIndependentSetSize > n) {
            throw new IllegalArgumentException("maxIndependentSetSize must be in [0, " + n + "]");
        }
        return n - maxIndependentSetSize;
    }
}