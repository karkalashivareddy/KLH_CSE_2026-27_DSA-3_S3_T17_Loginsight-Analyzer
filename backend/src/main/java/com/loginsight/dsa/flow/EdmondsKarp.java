package com.loginsight.dsa.flow;

import com.loginsight.dsa.common.CustomQueue;

/**
 * Algorithm: <strong>Edmonds-Karp</strong> maximum flow — Ford-Fulkerson with a breadth-first
 * augmenting-path search.
 *
 * <h2>Problem</h2>
 * Maximum {@code s}-{@code t} flow in a directed capacitated graph.
 *
 * <h2>Core idea</h2>
 * Same residual augmentation as Ford-Fulkerson, but each augmenting path is a
 * <strong>shortest path in number of edges</strong>, found by BFS over positive-residual edges. This
 * choice is what turns the generic method into a polynomial algorithm: the shortest augmenting-path
 * distance strictly increases over time.
 *
 * <h2>State</h2>
 * The shared {@link ResidualNetwork} plus BFS artefacts: a FIFO {@link CustomQueue} of frontier
 * vertices, a {@code visited} marker, and {@code parentEdge[v]} = the residual edge by which BFS first
 * reached {@code v} (the BFS tree).
 *
 * <h2>Transition</h2>
 * BFS from {@code source}; if {@code sink} is reached, the BFS tree gives a shortest path; augment its
 * bottleneck and update residual capacities (forward {@code -= delta}, reverse {@code += delta}).
 * Repeat until BFS cannot reach the sink.
 *
 * <h2>Termination</h2>
 * Each augmentation strictly increases the BFS distance from source to sink (a classic Edmonds-Karp
 * lemma), and that distance is bounded by {@code V - 1}, so there are {@code O(V E)} augmentations at
 * most; the algorithm terminates with integral or real capacities alike (the increasing-distance
 * argument, not an integrality argument, guarantees progress).
 *
 * <h2>Correctness intuition</h2>
 * A BFS that fails to reach the sink proves no augmenting path exists; the source-reachable set in the
 * final residual graph is a minimum cut, so the flow is maximum.
 *
 * <h2>Complexity</h2>
 * Time {@code O(V E^2)}: {@code O(V E)} augmentations, each BFS {@code O(E)}. Space {@code O(V + E)}.
 *
 * <h2>Why this is not Ford-Fulkerson</h2>
 * {@link FordFulkerson} searches with DFS and may reuse long paths; here the search is BFS and always
 * picks a shortest path. The complexity guarantees and the augmenting sequences differ — this class
 * genuinely builds a BFS tree, it is not an alias.
 *
 * <h2>Edge cases</h2>
 * Invalid/null graphs, out-of-range endpoints and {@code source == sink} are rejected; unreachable sink
 * yields flow 0.
 */
public final class EdmondsKarp {

    static final String ALGORITHM = "Edmonds-Karp (BFS)";

    public FlowResult maxFlow(FlowGraph graph, int source, int sink) {
        FlowValidator.requireFlowEndpoint(graph, source, sink);
        long start = System.nanoTime();
        ResidualNetwork residual = new ResidualNetwork(graph);
        int n = graph.vertexCount();
        int[] parentEdge = new int[n];
        long maxFlow = 0;
        int augmentations = 0;
        while (bfs(residual, source, sink, parentEdge)) {
            long bottleneck = Long.MAX_VALUE;
            for (int v = sink; v != source; ) {
                int e = parentEdge[v];
                long capacity = residual.residualCapacity(e);
                if (capacity < bottleneck) {
                    bottleneck = capacity;
                }
                v = residual.edgeFrom(e);
            }
            for (int v = sink; v != source; ) {
                int e = parentEdge[v];
                residual.push(e, bottleneck);
                v = residual.edgeFrom(e);
            }
            maxFlow += bottleneck;
            augmentations++;
        }
        long elapsed = System.nanoTime() - start;
        return FlowResult.of(ALGORITHM, graph, residual, maxFlow, source, sink, augmentations,
                elapsed, "O(V * E^2)", "O(V + E)");
    }

    /** Breadth-first shortest augmenting-path search; true when the sink was reached. */
    static boolean bfs(ResidualNetwork residual, int source, int sink, int[] parentEdge) {
        int n = residual.vertexCount();
        boolean[] visited = new boolean[n];
        CustomQueue<Integer> queue = new CustomQueue<>();
        visited[source] = true;
        queue.enqueue(source);
        while (!queue.isEmpty()) {
            int u = queue.dequeue();
            for (int e = residual.firstEdge(u); e != -1; e = residual.nextEdge(e)) {
                if (residual.residualCapacity(e) <= 0) {
                    continue;
                }
                int v = residual.edgeTo(e);
                if (visited[v]) {
                    continue;
                }
                visited[v] = true;
                parentEdge[v] = e;
                if (v == sink) {
                    return true;
                }
                queue.enqueue(v);
            }
        }
        return false;
    }
}
