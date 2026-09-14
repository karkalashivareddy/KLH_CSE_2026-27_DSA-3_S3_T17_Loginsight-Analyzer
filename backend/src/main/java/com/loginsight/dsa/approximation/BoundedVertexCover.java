package com.loginsight.dsa.approximation;

/**
 * Algorithm: <strong>bounded branching vertex cover</strong> — FPT (fixed-parameter tractable)
 * decision with parameter {@code k}.
 *
 * <h2>Problem</h2>
 * Decide whether an undirected graph has a vertex cover of size at most {@code k}, and if so return
 * one. This is exact decision, <em>not</em> approximation — a completely different tool from
 * {@link VertexCoverApproximation}.
 *
 * <h2>Recurrence (bounded branching)</h2>
 * <pre>
 * VertexCover(G, k):
 *   if E(G) == 0  : return empty cover        // nothing left to cover
 *   if k == 0     : return no cover           // edges remain but budget is spent
 *   pick any edge (u, v)
 *   return (u + VertexCover(G \ u, k-1))  or  (v + VertexCover(G \ v, k-1))
 * </pre>
 *
 * <h2>Why branching on (u,v) is correct</h2>
 * Every vertex cover of size {@code <= k} must contain {@code u} or {@code v}
 * (the edge must be covered), so at least one of the branches survives with budget {@code k-1} if a
 * solution exists. Each branch removes a vertex from the graph, which is why we never re-examine the
 * same vertex twice.
 *
 * <h2>State / representation</h2>
 * Recursion over {@link UndirectedGraph} copies ({@link UndirectedGraph#withoutVertex(int)}); the
 * chosen edge is always the current graph's first edge — deterministic branching.
 *
 * <h2>Correctness intuition</h2>
 * Structural induction on {@code (|V|, k)}: both branches cover strictly smaller graphs, and the
 * "u or v" case analysis covers every possible cover.
 *
 * <h2>Complexity</h2>
 * Time {@code O(2^k * (V + E))}, space {@code O(k * (V + E))}: exponential in the parameter, polynomial
 * in the input — the defining property of an FPT algorithm. This is what makes small-{@code k} exact
 * decision practical even on huge graphs.
 *
 * <h2>Edge cases</h2>
 * Negative {@code k} rejected; {@code k = 0} with edges → no cover; edgeless graph → empty cover even
 * with {@code k = 0}; null graph rejected.
 */
public final class BoundedVertexCover {

    /** Decision form: does {@code graph} have a vertex cover of size at most {@code k}? */
    public boolean hasVertexCover(UndirectedGraph graph, int k) {
        return findVertexCover(graph, k) != null;
    }

    /**
     * Returns a vertex cover of size at most {@code k} (sorted), or {@code null} when none exists.
     *
     * @throws IllegalArgumentException if the graph is null or {@code k} is negative
     */
    public int[] findVertexCover(UndirectedGraph graph, int k) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k must be >= 0 but was " + k);
        }
        int[] cover = search(graph, k);
        return cover == null ? null : sort(cover);
    }

    private int[] search(UndirectedGraph graph, int k) {
        if (graph.edgeCount() == 0) {
            return new int[0];
        }
        if (k == 0) {
            return null;
        }
        UndirectedEdge edge = graph.edge(0);
        int[] takeU = search(graph.withoutVertex(edge.getU()), k - 1);
        if (takeU != null) {
            return prepend(edge.getU(), takeU);
        }
        int[] takeV = search(graph.withoutVertex(edge.getV()), k - 1);
        if (takeV != null) {
            return prepend(edge.getV(), takeV);
        }
        return null;
    }

    private static int[] prepend(int vertex, int[] cover) {
        int[] result = new int[cover.length + 1];
        result[0] = vertex;
        System.arraycopy(cover, 0, result, 1, cover.length);
        return result;
    }

    private static int[] sort(int[] values) {
        int[] sorted = new int[values.length];
        System.arraycopy(values, 0, sorted, 0, values.length);
        for (int i = 1; i < sorted.length; i++) {
            int value = sorted[i];
            int j = i - 1;
            while (j >= 0 && sorted[j] > value) {
                sorted[j + 1] = sorted[j];
                j--;
            }
            sorted[j + 1] = value;
        }
        return sorted;
    }
}