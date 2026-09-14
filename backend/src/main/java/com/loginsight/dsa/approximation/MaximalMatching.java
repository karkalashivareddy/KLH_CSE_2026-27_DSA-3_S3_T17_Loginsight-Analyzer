package com.loginsight.dsa.approximation;

/**
 * Algorithm: <strong>maximal (not maximum) matching</strong> by a single greedy scan.
 *
 * <h2>Problem</h2>
 * Find a set {@code M} of pairwise vertex-disjoint edges that is <em>maximal</em>: no further edge can
 * be added without sharing an endpoint. A maximal matching can be much smaller than a maximum one; the
 * two are deliberately different concepts.
 *
 * <h2>Core idea</h2>
 * Scan edges in insertion order and take every edge whose endpoints are both still free. Because the
 * scan only stops at edges blocked by an already-chosen edge, the resulting matching is maximal: every
 * edge in the graph touches a matched vertex, or it would have been chosen itself.
 *
 * <h2>State</h2>
 * A {@code boolean[] used} marker per vertex and an {@code int[]} accumulator of chosen edge ids.
 *
 * <h2>Role in Module 5</h2>
 * The primary consumer is {@link VertexCoverApproximation}: the endpoints of a maximal matching form a
 * vertex cover of size at most twice the optimum. Self-loops are never part of a matching (a loop
 * shares both endpoints with itself); {@link VertexCoverApproximation} handles them separately.
 *
 * <h2>Correctness intuition</h2>
 * Greedy maximality is immediate from the scan rule; the matching property (no shared endpoint) is
 * preserved by the {@code used} check.
 *
 * <h2>Complexity</h2>
 * Time {@code O(E)}, space {@code O(V)}. Deterministic (edges scanned in insertion order).
 */
public final class MaximalMatching {

    /**
     * Returns the ids of the matched edges (all endpoint-disjoint) or an empty array when the graph
     * has no usable edges.
     *
     * @throws IllegalArgumentException if the graph is null or contains out-of-range vertices
     */
    public int[] matchingEdgeIds(UndirectedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        boolean[] used = new boolean[graph.vertexCount()];
        int[] chosen = new int[graph.edgeCount()];
        int count = 0;
        for (int id = 0; id < graph.edgeCount(); id++) {
            UndirectedEdge edge = graph.edge(id);
            if (edge.isSelfLoop()) {
                continue;
            }
            if (used[edge.getU()] || used[edge.getV()]) {
                continue;
            }
            used[edge.getU()] = true;
            used[edge.getV()] = true;
            chosen[count++] = id;
        }
        int[] result = new int[count];
        System.arraycopy(chosen, 0, result, 0, count);
        return result;
    }

    /** Convenience: number of edges in the greedy maximal matching. */
    public int matchingSize(UndirectedGraph graph) {
        return matchingEdgeIds(graph).length;
    }
}