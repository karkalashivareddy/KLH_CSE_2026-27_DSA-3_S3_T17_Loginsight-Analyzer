package com.loginsight.dsa.approximation;

/**
 * Algorithm: <strong>vertex cover 2-approximation</strong> via a greedy maximal matching.
 *
 * <h2>Problem</h2>
 * Find a set of vertices that touches every edge of an undirected graph, of as small a size as
 * possible. Exact minimum vertex cover is NP-hard; this is the standard polynomial 2-approximation.
 *
 * <h2>Core idea</h2>
 * The algorithm never computes an optimum. It builds a greedy maximal matching {@code M}
 * ({@link MaximalMatching}) and returns both endpoints of every matched edge, plus every vertex that
 * carries a self-loop.
 *
 * <h2>Implementation</h2>
 * <ol>
 *   <li>mark every self-loop vertex as <em>forced</em> (it must be in every cover);</li>
 *   <li>remove forced vertices and their incident edges;</li>
 *   <li>compute a greedy maximal matching on the survivors;</li>
 *   <li>return forced vertices {@code +} both endpoints of each matched edge.</li>
 * </ol>
 * Self-loops are excluded from the matching (a loop cannot be part of a matching) but force their
 * vertex into the cover — the documented edge case of the graph contract.
 *
 * <h2>Why it is a 2-approximation (proof, docs/02 §8 vocabulary)</h2>
 * <ul>
 *   <li>The matched edges of {@code M} are vertex-disjoint, so any vertex cover needs at least one
 *       (distinct) vertex per matched edge: {@code OPT >= |M|}. Each forced self-loop vertex is
 *       similarly mandatory and is disjoint from all matched endpoints: {@code OPT >= |M| + |F|}.</li>
 *   <li>The algorithm returns {@code |C| = |F| + 2|M|}.</li>
 *   <li>Hence {@code |C| <= 2(|M| + |F|) <= 2 OPT}: a valid cover that is never more than twice the
 *       optimum.</li>
 * </ul>
 * A maximal-matching formulation is used so the bound is visible in the result: {@code lowerBound =
 * |M| + |F|}, {@code ratio = |C| / lowerBound <= 2}.
 *
 * <h2>Correctness intuition</h2>
 * Maximality guarantees every non-loop edge touches a matched vertex, so all edges are covered. The
 * bound follows from the matching lower bound above.
 *
 * <h2>Complexity</h2>
 * Time {@code O(E)} (one scan for maximal matching plus one scan for self-loops), space
 * {@code O(V)}. Deterministic: edges are scanned in insertion order and the cover is reported sorted.
 *
 * <h2>Edge cases</h2>
 * Empty graph → empty cover, ratio 1.0 by convention; isolated vertices never enter the cover;
 * duplicate edges are irrelevant (simple-graph contract); self-loops force their vertex; a null graph
 * or invalid 'k'-free vertex indices are rejected.
 */
public final class VertexCoverApproximation {

    /**
     * Computes a vertex cover of size at most twice the optimum.
     *
     * @throws IllegalArgumentException if the graph is null
     */
    public ApproximationResult approximateVertexCover(UndirectedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        long start = System.nanoTime();
        int n = graph.vertexCount();

        boolean[] forced = new boolean[n];
        int forcedCount = 0;
        for (int id = 0; id < graph.edgeCount(); id++) {
            UndirectedEdge edge = graph.edge(id);
            if (edge.isSelfLoop() && !forced[edge.getU()]) {
                forced[edge.getU()] = true;
                forcedCount++;
            }
        }

        UndirectedGraph remaining = graph;
        int[] forcedVertices = collectSet(forced, n);
        for (int v : forcedVertices) {
            remaining = remaining.withoutVertex(v);
        }

        MaximalMatching matching = new MaximalMatching();
        int[] matchedEdges = matching.matchingEdgeIds(remaining);
        int[] cover = new int[forcedCount + 2 * matchedEdges.length];
        System.arraycopy(forcedVertices, 0, cover, 0, forcedCount);
        int index = forcedCount;
        for (int edgeId : matchedEdges) {
            UndirectedEdge edge = remaining.edge(edgeId);
            cover[index++] = edge.getU();
            cover[index++] = edge.getV();
        }
        long elapsed = System.nanoTime() - start;
        String notes = "2-approximation via maximal matching; optimal vertex cover size >= matching size"
                + " + forced self-loop vertices, so |C| <= 2*OPT";
        return new ApproximationResult(cover, matchedEdges.length, forcedCount, elapsed,
                "O(E)", "O(V)", notes);
    }

    private static int[] collectSet(boolean[] selected, int n) {
        int[] values = new int[n];
        int count = 0;
        for (int v = 0; v < n; v++) {
            if (selected[v]) {
                values[count++] = v;
            }
        }
        int[] result = new int[count];
        System.arraycopy(values, 0, result, 0, count);
        return result;
    }
}