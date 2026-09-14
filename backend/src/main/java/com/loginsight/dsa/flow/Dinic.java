package com.loginsight.dsa.flow;

import com.loginsight.dsa.common.CustomQueue;

/**
 * Algorithm: <strong>Dinic</strong> maximum flow using a level graph and blocking flows.
 *
 * <h2>Problem</h2>
 * Maximum {@code s}-{@code t} flow in a directed capacitated graph; also the workhorse for the
 * bipartite-matching and min-cut reductions in this package.
 *
 * <h2>Core idea</h2>
 * Two phases per round:
 * <ol>
 *   <li><strong>BFS level graph</strong> over positive-residual edges: {@code level[v]} is the shortest
 *       residual distance from {@code source}. If {@code sink} is unreachable, we are done.</li>
 *   <li><strong>Blocking flow</strong> by DFS that only follows edges with
 *       {@code level[v] == level[u] + 1}, saturating as it goes until no such {@code s -> t} path
 *       remains in the level graph.</li>
 * </ol>
 * Repeat: each round strictly increases the sink's level, which is at most {@code V - 1}.
 *
 * <h2>State</h2>
 * Shared {@link ResidualNetwork}; {@code level[]} BFS distances; {@code nextEdge[]} a <em>current-arc
 * pointer</em> per vertex, so once a residual edge is exhausted it is never rescanned within the round
 * — this is what makes Dinic faster than repeatedly re-running BFS/DFS from scratch.
 *
 * <h2>Transition</h2>
 * Augment along a level-graph path, updating residual capacities forward {@code -= delta}, reverse
 * {@code += delta}; a saturated arc advances the current-arc pointer.
 *
 * <h2>Termination / correctness</h2>
 * When BFS can no longer reach the sink, the source-reachable set is a minimum cut and the flow is
 * maximum. Shortest-level distances are monotone across rounds, so there are at most {@code V} rounds.
 *
 * <h2>Complexity</h2>
 * Time {@code O(V^2 E)} for general capacities. On <strong>unit-capacity</strong> networks (the
 * bipartite-matching reduction) the tighter bound {@code O(E sqrt(V))} holds; the result string
 * reports the general bound since that is the safe claim for arbitrary capacities. Space
 * {@code O(V + E)}.
 *
 * <h2>Why this is not Edmonds-Karp</h2>
 * Edmonds-Karp augments one shortest path at a time and re-runs BFS after each; Dinic computes one BFS
 * level graph and drains it with a blocking flow before rebuilding, using current-arc pruning. The
 * asymptotics and the search structure differ.
 *
 * <h2>Edge cases</h2>
 * Invalid/null graph, out-of-range endpoints and {@code source == sink} are rejected; unreachable sink
 * yields flow 0.
 */
public final class Dinic {

    static final String ALGORITHM = "Dinic";

    public FlowResult maxFlow(FlowGraph graph, int source, int sink) {
        FlowValidator.requireFlowEndpoint(graph, source, sink);
        long start = System.nanoTime();
        ResidualNetwork residual = new ResidualNetwork(graph);
        int n = graph.vertexCount();
        int[] level = new int[n];
        int[] nextEdge = new int[n];
        long maxFlow = 0;
        int augmentations = 0;
        while (buildLevelGraph(residual, source, sink, level)) {
            for (int v = 0; v < n; v++) {
                nextEdge[v] = residual.firstEdge(v);
            }
            long pushed;
            while ((pushed = pushBlockingFlow(residual, source, sink, Long.MAX_VALUE, level,
                    nextEdge)) > 0) {
                maxFlow += pushed;
                augmentations++;
            }
        }
        long elapsed = System.nanoTime() - start;
        return FlowResult.of(ALGORITHM, graph, residual, maxFlow, source, sink, augmentations,
                elapsed, "O(V^2 * E)", "O(V + E)");
    }

    /** BFS over positive-residual edges; fills {@code level} and reports sink reachability. */
    static boolean buildLevelGraph(ResidualNetwork residual, int source, int sink, int[] level) {
        int n = residual.vertexCount();
        boolean[] visited = new boolean[n];
        for (int v = 0; v < n; v++) {
            level[v] = -1;
        }
        CustomQueue<Integer> queue = new CustomQueue<>();
        level[source] = 0;
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
                level[v] = level[u] + 1;
                queue.enqueue(v);
            }
        }
        return visited[sink];
    }

    /**
     * DFS in the level graph with current-arc pruning; pushes up to {@code limit} units from {@code u}
     * toward the sink and returns the amount pushed (0 when none).
     */
    static long pushBlockingFlow(ResidualNetwork residual, int u, int sink, long limit, int[] level,
                                 int[] nextEdge) {
        if (u == sink) {
            return limit;
        }
        while (nextEdge[u] != -1) {
            int e = nextEdge[u];
            long capacity = residual.residualCapacity(e);
            int v = residual.edgeTo(e);
            if (capacity > 0 && level[v] == level[u] + 1) {
                long pushed = pushBlockingFlow(residual, v, sink, Math.min(limit, capacity), level,
                        nextEdge);
                if (pushed > 0) {
                    residual.push(e, pushed);
                    return pushed;
                }
            }
            nextEdge[u] = residual.nextEdge(e);
        }
        return 0;
    }
}
