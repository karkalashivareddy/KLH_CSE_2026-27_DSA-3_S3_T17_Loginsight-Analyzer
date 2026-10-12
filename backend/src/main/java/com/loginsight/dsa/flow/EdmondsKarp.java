package com.loginsight.dsa.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.common.CustomQueue;
import com.loginsight.trace.StepRecorder;
import com.loginsight.trace.TracedResult;

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

    /**
     * Trace-capable path: identical BFS augmenting loop recording each (shortest) augmenting path,
     * its bottleneck and the running flow value. Result equals the untraced max-flow.
     */
    public TracedResult maxFlowTracked(FlowGraph graph, int source, int sink) {
        FlowValidator.requireFlowEndpoint(graph, source, sink);
        StepRecorder recorder = new StepRecorder();
        long start = System.nanoTime();
        ResidualNetwork residual = new ResidualNetwork(graph);
        int n = graph.vertexCount();
        int[] parentEdge = new int[n];
        long maxFlow = 0;
        int augmentations = 0;
        while (bfs(residual, source, sink, parentEdge)) {
            List<Integer> pathEdges = new ArrayList<>();
            long bottleneck = Long.MAX_VALUE;
            for (int v = sink; v != source; ) {
                int e = parentEdge[v];
                pathEdges.add(e);
                long capacity = residual.residualCapacity(e);
                if (capacity < bottleneck) {
                    bottleneck = capacity;
                }
                v = residual.edgeFrom(e);
            }
            List<Integer> pathVertices = new ArrayList<>();
            pathVertices.add(source);
            for (int k = pathEdges.size() - 1; k >= 0; k--) {
                pathVertices.add(residual.edgeTo(pathEdges.get(k)));
            }
            for (int v = sink; v != source; ) {
                int e = parentEdge[v];
                residual.push(e, bottleneck);
                v = residual.edgeFrom(e);
            }
            maxFlow += bottleneck;
            augmentations++;
            recorder.record("AUGMENT",
                    "BFS found shortest augmenting path " + pathVertices + ": bottleneck="
                            + bottleneck + ", flow now " + maxFlow + ".",
                    StepRecorder.state("phase", "augment", "augmentation", augmentations,
                            "pathVertices", pathVertices, "pathEdgeIds", pathEdges, "bottleneck",
                            bottleneck, "flow", maxFlow, "edges", residualEdges(graph, residual)),
                    pathEdges, Map.of("bottleneck", bottleneck, "flow", maxFlow));
        }
        recorder.record("DONE", "BFS cannot reach the sink; no augmenting path, flow is maximum.",
                StepRecorder.state("phase", "done", "maxFlow", maxFlow, "augmentations",
                        augmentations, "edges", residualEdges(graph, residual)));
        long elapsed = System.nanoTime() - start;
        return new TracedResult(ALGORITHM,
                Map.of("maxFlow", maxFlow, "source", source, "sink", sink, "augmentations",
                        augmentations, "edges", residualEdges(graph, residual)),
                Map.of("edges", residualEdges(graph, residual), "steps", recorder.collect(),
                        "truncated", recorder.isTruncated()),
                recorder.collect(), elapsed, "O(V * E^2)", "O(V + E)");
    }

    private static List<Map<String, Object>> residualEdges(FlowGraph graph,
                                                           ResidualNetwork residual) {
        List<Map<String, Object>> edges = new ArrayList<>();
        for (int id = 0; id < graph.edgeCount(); id++) {
            edges.add(Map.of("from", graph.edgeFrom(id), "to", graph.edgeTo(id),
                    "capacity", graph.edgeCapacity(id),
                    "flow", residual.flowOfOriginal(id), "residual", residual.residualOfOriginal(id)));
        }
        return edges;
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
