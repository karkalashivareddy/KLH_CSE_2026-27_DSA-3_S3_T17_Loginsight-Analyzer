package com.loginsight.dsa.approximation;

/**
 * Executable demonstration of the <strong>Clique ↔ Independent Set in the complement</strong>
 * reduction.
 *
 * <h2>Theorem (reduction zoo, Module 5)</h2>
 * {@code S} is a clique in {@code G} if and only if {@code S} is an independent set in the complement
 * graph {@code complement(G)} (the graph with an edge between {@code u} and {@code v} exactly when
 * {@code {u,v}} is <em>not</em> an edge of {@code G}, and no self-loops). Consequently clique size in
 * {@code G} equals independent-set size in {@code complement(G)} , and the two NP-complete problems
 * reduce to each other in both directions.
 *
 * <h2>Why it holds</h2>
 * A clique is a set where <em>every</em> pair is adjacent; in the complement every pair of such a set
 * is non-adjacent, which is exactly independence. The mapping is a bijection on vertex subsets.
 *
 * <h2>What is executable here</h2>
 * The complement transformation, the clique verifier, and the independence verifier shared with
 * {@link IndependentSetReduction}. Exact optimization is left to test oracles.
 */
public final class ComplementGraph {

    private ComplementGraph() {
    }

    /**
     * Builds the complement of {@code graph} on the same vertex set: for every pair {@code u < v}
     * that is <em>not</em> an edge, add {@code {u,v}}. No self-loops are introduced. Requires
     * {@code O(V^2)} time (one pass over all pairs), which is acceptable here and documents the
     * transformation itself.
     */
    public static UndirectedGraph complement(UndirectedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        UndirectedGraph result = new UndirectedGraph(graph.vertexCount());
        int n = graph.vertexCount();
        for (int u = 0; u < n; u++) {
            for (int v = u + 1; v < n; v++) {
                if (!graph.hasEdge(u, v)) {
                    result.addEdge(u, v);
                }
            }
        }
        return result;
    }

    /** True when every pair of selected vertices is joined by an edge (a single vertex is a clique). */
    public static boolean isClique(UndirectedGraph graph, boolean[] selection) {
        if (graph == null || selection == null || selection.length != graph.vertexCount()) {
            throw new IllegalArgumentException("graph or selection invalid");
        }
        int n = graph.vertexCount();
        for (int u = 0; u < n; u++) {
            if (!selection[u]) {
                continue;
            }
            for (int v = u + 1; v < n; v++) {
                if (selection[v] && !graph.hasEdge(u, v)) {
                    return false;
                }
            }
        }
        return true;
    }
}