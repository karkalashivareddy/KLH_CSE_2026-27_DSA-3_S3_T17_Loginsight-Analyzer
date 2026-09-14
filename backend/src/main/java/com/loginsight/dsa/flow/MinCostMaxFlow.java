package com.loginsight.dsa.flow;

/**
 * Algorithm: <strong>minimum-cost maximum flow</strong> by successive shortest augmenting paths
 * (SSP) with Bellman-Ford, the workhorse named in docs/02 §7 for this module.
 *
 * <h2>Problem</h2>
 * Among all maximum flows from {@code source} to {@code sink}, find one of minimum total cost, where
 * edge {@code e} charges {@code cost(e)} per unit of flow.
 *
 * <h2>Core idea</h2>
 * Repeatedly find the <em>cheapest</em> residual augmenting path (fewest cost, not fewest edges) and
 * saturate it. Sending an augmenting path's bottleneck along a globally shortest residual path keeps
 * the flow min-cost for its current value, so when no residual path remains the flow is both maximum
 * and min-cost.
 *
 * <h2>Why Bellman-Ford</h2>
 * Residual graphs contain <em>negative-cost reverse edges</em> (the reverse of an original edge has
 * cost {@code -cost}), so Dijkstra is not valid without reduced-cost potentials. Bellman-Ford handles
 * arbitrary edge costs directly; that is exactly why it is used here.
 *
 * <h2>State</h2>
 * Shared {@link ResidualNetwork}; {@code dist[v]} = cheapest residual cost from source to {@code v};
 * {@code parentEdge[v]} = the residual edge used to reach it.
 *
 * <h2>Negative-cycle guard</h2>
 * Bellman-Ford shortest paths are undefined if a negative-cost cycle is reachable from the source, so
 * after {@code V-1} relaxation rounds one extra round checks for a still-relaxable reachable edge and
 * rejects the input with {@link IllegalArgumentException}. (This is a deliberate, documented guard;
 * valid min-cost-flow inputs contain no such cycle.)
 *
 * <h2>Termination</h2>
 * Each augmentation increases the integral flow by at least 1, so at most {@code f_max} augmentations
 * are performed; each augmentation is one Bellman-Ford relaxation pass.
 *
 * <h2>Complexity</h2>
 * Time {@code O(f_max * V * E)}, space {@code O(V + E)}.
 *
 * <h2>Edge cases</h2>
 * Invalid endpoints and {@code source == sink} are rejected; no path to the sink yields flow 0, cost 0;
 * zero-cost edges are handled uniformly.
 */
public final class MinCostMaxFlow {

    private static final long INFINITY = Long.MAX_VALUE / 4;

    /**
     * Computes a minimum-cost maximum flow.
     *
     * @throws IllegalArgumentException if the graph is null, endpoints are invalid,
     *                                  {@code source == sink}, or a negative-cost cycle is reachable
     *                                  from the source
     */
    public MinCostFlowResult minCostMaxFlow(FlowGraph graph, int source, int sink) {
        FlowValidator.requireFlowEndpoint(graph, source, sink);
        long start = System.nanoTime();
        ResidualNetwork residual = new ResidualNetwork(graph);
        int n = graph.vertexCount();
        long[] dist = new long[n];
        int[] parentEdge = new int[n];
        long totalFlow = 0;
        long totalCost = 0;
        int augmentations = 0;
        while (true) {
            if (bellmanFord(residual, source, dist, parentEdge)) {
                throw new IllegalArgumentException(
                        "negative-cost cycle reachable from source " + source);
            }
            if (dist[sink] >= INFINITY) {
                break;
            }
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
            totalFlow += bottleneck;
            totalCost += bottleneck * dist[sink];
            augmentations++;
        }
        int m = graph.edgeCount();
        long[] flow = new long[m];
        for (int id = 0; id < m; id++) {
            flow[id] = residual.flowOfOriginal(id);
        }
        long elapsed = System.nanoTime() - start;
        return new MinCostFlowResult(totalFlow, totalCost, source, sink, n, graph.edges(), flow,
                augmentations, elapsed, "O(f_max * V * E)", "O(V + E)");
    }

    /**
     * Bellman-Ford shortest residual path from {@code source}. Returns {@code true} if the extra
     * relaxation round finds a negative-cost cycle reachable from the source.
     */
    private static boolean bellmanFord(ResidualNetwork residual, int source, long[] dist,
                                       int[] parentEdge) {
        int n = residual.vertexCount();
        for (int v = 0; v < n; v++) {
            dist[v] = INFINITY;
            parentEdge[v] = -1;
        }
        dist[source] = 0;
        int edgeCount = residual.residualEdgeCount();
        for (int round = 0; round < n - 1; round++) {
            boolean changed = false;
            for (int e = 0; e < edgeCount; e++) {
                if (residual.residualCapacity(e) <= 0) {
                    continue;
                }
                int u = residual.edgeFrom(e);
                if (dist[u] >= INFINITY) {
                    continue;
                }
                int v = residual.edgeTo(e);
                long candidate = dist[u] + residual.edgeCost(e);
                if (candidate < dist[v]) {
                    dist[v] = candidate;
                    parentEdge[v] = e;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        for (int e = 0; e < edgeCount; e++) {
            if (residual.residualCapacity(e) <= 0) {
                continue;
            }
            int u = residual.edgeFrom(e);
            if (dist[u] >= INFINITY) {
                continue;
            }
            if (dist[u] + residual.edgeCost(e) < dist[residual.edgeTo(e)]) {
                return true;
            }
        }
        return false;
    }
}
