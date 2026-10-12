package com.loginsight.dsa.flow;

import com.loginsight.dsa.common.CustomQueue;

/**
 * Algorithm: minimum {@code s}-{@code t} cut via the <strong>max-flow/min-cut theorem</strong>.
 *
 * <h2>Problem</h2>
 * Partition the vertices into a set {@code S} containing {@code source} and a set {@code T} containing
 * {@code sink} so that the total capacity of edges from {@code S} to {@code T} is minimum.
 *
 * <h2>Core idea</h2>
 * Compute a maximum flow with {@link Dinic}, then take for {@code S} every vertex reachable from the
 * source in the <em>final residual graph</em>. No residual edge leaves {@code S}, so every original
 * edge crossing {@code S -> T} is saturated and no reverse flow crosses back; hence the crossing
 * capacity equals the flow value and, by max-flow/min-cut, is minimum.
 *
 * <h2>Residual reconstruction</h2>
 * The flow/reverse residuals are recovered from the {@link FlowResult} (never from internal state):
 * forward residual of original edge {@code e} is {@code result.residualOf(e)}, reverse residual is
 * {@code result.flowOf(e)}. BFS then walks forward residual edges and reverse flow edges.
 *
 * <h2>Termination</h2>
 * BFS over the finite residual graph visits each vertex once.
 *
 * <h2>Correctness intuition</h2>
 * A residual path from the source to the sink would be an augmenting path, contradicting maximality,
 * so the sink is not in {@code S}. The cut's capacity is therefore a valid upper bound on every flow
 * and is attained — it is a minimum cut.
 *
 * <h2>Complexity</h2>
 * Dominated by the max-flow phase: time {@code O(V^2 E)} with Dinic, space {@code O(V + E)}.
 *
 * <h2>Edge cases</h2>
 * Endpoints are validated as everywhere else; an empty crossing set means cut capacity 0.
 */
public final class MinCut {

    /**
     * Computes a minimum cut, reusing a fresh max flow internally so cuts are always consistent with
     * their flow value.
     */
    public MinCutResult minCut(FlowGraph graph, int source, int sink) {
        FlowValidator.requireFlowEndpoint(graph, source, sink);
        long start = System.nanoTime();
        FlowResult flow = new Dinic().maxFlow(graph, source, sink);
        int n = graph.vertexCount();
        boolean[] onSourceSide = residualReachable(graph, flow, source);
        boolean[] onSinkSide = new boolean[n];
        for (int v = 0; v < n; v++) {
            onSinkSide[v] = !onSourceSide[v];
        }
        int m = graph.edgeCount();
        Edge[] crossing = new Edge[m];
        int crossingCount = 0;
        long cutCapacity = 0;
        for (int id = 0; id < m; id++) {
            if (onSourceSide[graph.edgeFrom(id)] && !onSourceSide[graph.edgeTo(id)]) {
                crossing[crossingCount++] = graph.edge(id);
                cutCapacity += graph.edgeCapacity(id);
            }
        }
        Edge[] cutEdges = new Edge[crossingCount];
        System.arraycopy(crossing, 0, cutEdges, 0, crossingCount);
        long elapsed = System.nanoTime() - start;
        return new MinCutResult(flow.getMaxFlow(), source, sink, n, onSourceSide, onSinkSide,
                cutEdges, cutCapacity, elapsed, flow.getTimeComplexity());
    }

    /** BFS over the residual graph reconstructed from a max-flow result, starting at the source. */
    private static boolean[] residualReachable(FlowGraph graph, FlowResult flow, int source) {
        int n = graph.vertexCount();
        int m = graph.edgeCount();
        boolean[] reachable = new boolean[n];
        CustomQueue<Integer> queue = new CustomQueue<>();
        reachable[source] = true;
        queue.enqueue(source);
        while (!queue.isEmpty()) {
            int u = queue.dequeue();
            for (int id = 0; id < m; id++) {
                int from = graph.edgeFrom(id);
                int to = graph.edgeTo(id);
                if (from == u && flow.residualOf(id) > 0 && !reachable[to]) {
                    reachable[to] = true;
                    queue.enqueue(to);
                } else if (to == u && flow.flowOf(id) > 0 && !reachable[from]) {
                    reachable[from] = true;
                    queue.enqueue(from);
                }
            }
        }
        return reachable;
    }
}
