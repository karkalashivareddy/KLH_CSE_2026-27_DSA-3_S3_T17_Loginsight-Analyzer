package com.loginsight.dsa.flow;

import com.loginsight.dsa.common.CustomStack;

/**
 * Algorithm: <strong>Ford-Fulkerson</strong> maximum flow with an explicit <em>depth-first</em>
 * augmenting-path strategy.
 *
 * <h2>Problem</h2>
 * Given a directed capacitated graph, find a flow from {@code s} to {@code t} of maximum value.
 *
 * <h2>Core idea</h2>
 * Repeatedly find any path from source to sink whose <em>residual</em> capacities are all positive,
 * push the path's bottleneck along it, and update forward/reverse residual capacities. Stop when no
 * residual {@code s -> t} path exists.
 *
 * <h2>State</h2>
 * The residual network {@link ResidualNetwork}: for every original edge {@code u->v} a forward
 * residual {@code u->v} (initial {@code c(u,v)}) and a reverse residual {@code v->u} (initial 0).
 * {@code parentEdge[v]} records the residual edge by which {@code v} was first reached in the current
 * search.
 *
 * <h2>Augmenting path</h2>
 * Any simple {@code s -> t} path of positive residual capacity. This implementation finds it with an
 * <strong>iterative depth-first search</strong> driven by {@link CustomStack} (the syllabus wants the
 * explicit DFS visible). <em>The DFS choice is the defining property of this class</em>; it may pick
 * long or unlucky paths.
 *
 * <h2>Bottleneck and transition</h2>
 * {@code delta = min residual capacity over the path}; for every path edge {@code e}:
 * {@code residual[e] -= delta}, {@code residual[e ^ 1] += delta}. Then {@code |f| += delta}.
 *
 * <h2>Termination</h2>
 * With finite <em>integral</em> capacities each augmentation increases {@code |f|} by at least 1, so
 * the loop terminates after at most {@code f_max} augmentations. (With arbitrary real capacities the
 * generic method need not terminate — never claimed here.)
 *
 * <h2>Correctness intuition</h2>
 * At termination no residual {@code s -> t} path exists. The set reachable from {@code s} in the final
 * residual graph forms an {@code s}-{@code t} cut whose every original crossing edge is saturated, so
 * the flow equals that cut's capacity and is therefore maximum (max-flow/min-cut).
 *
 * <h2>Complexity</h2>
 * Time {@code O(E * f_max)} where {@code f_max} is the maximum flow value (each DFS is {@code O(E)}).
 * <em>This is NOT Edmonds-Karp's {@code O(V E^2)}</em> — that bound requires the shortest-path (BFS)
 * search used by {@link EdmondsKarp}. Space {@code O(V + E)}.
 *
 * <h2>Edge cases</h2>
 * {@code s == t}, out-of-range endpoints and negative capacities are rejected; a disconnected sink
 * yields flow 0.
 */
public final class FordFulkerson {

    static final String ALGORITHM = "Ford-Fulkerson (DFS)";

    /**
     * Computes a maximum flow from {@code source} to {@code sink}.
     *
     * @throws IllegalArgumentException if the graph is null, endpoints are invalid, or
     *                                  {@code source == sink}
     */
    public FlowResult maxFlow(FlowGraph graph, int source, int sink) {
        FlowValidator.requireFlowEndpoint(graph, source, sink);
        long start = System.nanoTime();
        ResidualNetwork residual = new ResidualNetwork(graph);
        int n = graph.vertexCount();
        int[] parentEdge = new int[n];
        long maxFlow = 0;
        int augmentations = 0;
        while (findAugmentingPath(residual, source, sink, parentEdge)) {
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
                elapsed, "O(E * f_max)", "O(V + E)");
    }

    /** Iterative DFS: true when a positive-residual path was found and {@code parentEdge} updated. */
    static boolean findAugmentingPath(ResidualNetwork residual, int source, int sink,
                                      int[] parentEdge) {
        int n = residual.vertexCount();
        boolean[] visited = new boolean[n];
        CustomStack<Integer> stack = new CustomStack<>();
        visited[source] = true;
        stack.push(source);
        while (!stack.isEmpty()) {
            int u = stack.pop();
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
                stack.push(v);
            }
        }
        return false;
    }
}
