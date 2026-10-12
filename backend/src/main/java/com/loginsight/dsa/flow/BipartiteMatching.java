package com.loginsight.dsa.flow;

/**
 * Algorithm: maximum <strong>bipartite matching</strong> by reduction to maximum flow.
 *
 * <h2>Problem</h2>
 * Given a bipartite graph with left vertices {@code 0..leftCount-1} and right vertices
 * {@code 0..rightCount-1}, choose as many disjoint left-right edges as possible.
 *
 * <h2>Core idea (König / flow reduction)</h2>
 * Build a unit-capacity flow network: a super-source to each left vertex, each bipartite edge directed
 * left-to-right, and each right vertex to a super-sink, all with capacity 1. A maximum integral flow in
 * a unit-capacity network decomposes into vertex-disjoint source-sink paths, i.e. a maximum matching.
 *
 * <h2>Why integrality matters</h2>
 * Capacities are integral, so {@link Dinic} returns an integral flow; each bipartite edge carries 0 or
 * 1, which is exactly the matching indicator. Unit-capacity networks also enjoy Dinic's
 * {@code O(E sqrt(V))} bound (docs/02), though the generic bound is what the shared result reports.
 *
 * <h2>Termination / correctness intuition</h2>
 * Any matching gives a flow of the same size and vice versa, so maximizing flow maximizes the matching.
 * Dinic terminates when no augmenting path remains.
 *
 * <h2>Complexity</h2>
 * Time {@code O(E sqrt(V))} for this unit-capacity reduction ({@code O(V^2 E)} worst-case general
 * bound), space {@code O(V + E)}.
 *
 * <h2>Edge cases</h2>
 * Empty left or right side yields size 0; malformed edges (wrong side) are rejected; duplicate
 * left-right edges cannot inflate the matching because the intermediate capacity is 1.
 */
public final class BipartiteMatching {

    /**
     * Computes a maximum matching. Every edge of {@code graph} must go from a left vertex
     * ({@code 0..leftCount-1}) to a right vertex ({@code 0..rightCount-1}); there are no other edges.
     *
     * @throws IllegalArgumentException if the graph is null or an edge endpoint is on the wrong side
     */
    public MatchingResult maxMatching(FlowGraph graph, int leftCount, int rightCount) {
        FlowValidator.requireNonNull(graph);
        FlowValidator.requireNonNegative(leftCount, "leftCount");
        FlowValidator.requireNonNegative(rightCount, "rightCount");
        int m = graph.edgeCount();
        for (int id = 0; id < m; id++) {
            FlowValidator.requireInRange(graph.edgeFrom(id), 0, leftCount - 1,
                    "edge " + id + " from (left side)");
            FlowValidator.requireInRange(graph.edgeTo(id), 0, rightCount - 1,
                    "edge " + id + " to (right side)");
        }
        long start = System.nanoTime();
        int source = leftCount + rightCount;
        int sink = source + 1;
        FlowGraph network = new FlowGraph(sink + 1);
        for (int left = 0; left < leftCount; left++) {
            network.addEdge(source, left, 1L);
        }
        int[] bipartiteEdgeId = new int[m];
        for (int id = 0; id < m; id++) {
            bipartiteEdgeId[id] = network.addEdge(graph.edgeFrom(id),
                    leftCount + graph.edgeTo(id), 1L);
        }
        for (int right = 0; right < rightCount; right++) {
            network.addEdge(leftCount + right, sink, 1L);
        }
        FlowResult flow = new Dinic().maxFlow(network, source, sink);

        int[] leftPartner = new int[leftCount];
        int[] rightPartner = new int[rightCount];
        for (int left = 0; left < leftCount; left++) {
            leftPartner[left] = -1;
        }
        for (int right = 0; right < rightCount; right++) {
            rightPartner[right] = -1;
        }
        int matchingSize = 0;
        for (int id = 0; id < m; id++) {
            if (flow.flowOf(bipartiteEdgeId[id]) == 1L) {
                int left = graph.edgeFrom(id);
                int right = graph.edgeTo(id);
                if (leftPartner[left] == -1 && rightPartner[right] == -1) {
                    leftPartner[left] = right;
                    rightPartner[right] = left;
                    matchingSize++;
                }
            }
        }
        int[][] pairs = new int[matchingSize][2];
        int pairIndex = 0;
        for (int left = 0; left < leftCount; left++) {
            if (leftPartner[left] != -1) {
                pairs[pairIndex][0] = left;
                pairs[pairIndex][1] = leftPartner[left];
                pairIndex++;
            }
        }
        int[] unmatchedLeft = collectUnmatched(leftPartner);
        int[] unmatchedRight = collectUnmatched(rightPartner);
        long elapsed = System.nanoTime() - start;
        return new MatchingResult(leftCount, rightCount, matchingSize, leftPartner, rightPartner,
                pairs, unmatchedLeft, unmatchedRight, elapsed);
    }

    private static int[] collectUnmatched(int[] partner) {
        int count = 0;
        for (int p : partner) {
            if (p == -1) {
                count++;
            }
        }
        int[] unmatched = new int[count];
        int index = 0;
        for (int v = 0; v < partner.length; v++) {
            if (partner[v] == -1) {
                unmatched[index++] = v;
            }
        }
        return unmatched;
    }
}
