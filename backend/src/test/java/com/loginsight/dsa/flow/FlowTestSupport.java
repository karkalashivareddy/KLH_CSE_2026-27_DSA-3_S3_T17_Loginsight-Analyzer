package com.loginsight.dsa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared, deterministic fixtures and structural checks for the flow tests.
 *
 * <p>Two jobs: (1) generate small random graphs with a fixed seed via a local xorshift so runs are
 * reproducible and tests add no {@code java.util} dependency for data, and (2) assert the defining
 * invariants of a flow result — the properties any correct algorithm must satisfy regardless of
 * implementation. The independent optimal values themselves come from {@link FlowOracle}, never from a
 * second copy of the same algorithm.</p>
 */
final class FlowTestSupport {

    /** Maximum flow of {@link #classicGraph()} (the standard CLRS network). */
    static final long CLASSIC_MAX_FLOW = 23L;

    /**
     * The textbook flow network (CLRS): source 0, sink 5, with the classic cross edges that force
     * augmenting-path algorithms to cancel earlier choices. Maximum flow is {@code 23}.
     */
    static FlowGraph classicGraph() {
        FlowGraph graph = new FlowGraph(6);
        graph.addEdge(0, 1, 16);
        graph.addEdge(0, 2, 13);
        graph.addEdge(1, 2, 10);
        graph.addEdge(2, 1, 4);
        graph.addEdge(1, 3, 12);
        graph.addEdge(3, 2, 9);
        graph.addEdge(2, 4, 14);
        graph.addEdge(4, 3, 7);
        graph.addEdge(3, 5, 20);
        graph.addEdge(4, 5, 4);
        return graph;
    }

    private long state;

    FlowTestSupport(long seed) {
        this.state = seed == 0L ? 0x9E3779B97F4A7C15L : seed;
    }

    private long nextLong() {
        state ^= state << 13;
        state ^= state >>> 7;
        state ^= state << 17;
        return state >>> 1;
    }

    int nextInt(int bound) {
        return (int) (nextLong() % bound);
    }

    /**
     * Builds a random graph on {@code n} vertices with at most {@code maxEdges} non-loop edges,
     * capacities in {@code [0, maxCap]} and costs in {@code [-maxCost, maxCost]} (or {@code [0,
     * maxCost]} when {@code allowNegativeCost} is false).
     */
    FlowGraph randomGraph(int n, int maxEdges, int maxCap, int maxCost, boolean allowNegativeCost) {
        FlowGraph graph = new FlowGraph(n);
        int edges = maxEdges == 0 ? 0 : nextInt(maxEdges + 1);
        for (int i = 0; i < edges; i++) {
            int from = nextInt(n);
            int to = nextInt(n);
            if (from == to) {
                continue;
            }
            long capacity = nextInt(maxCap + 1);
            long cost;
            if (allowNegativeCost && maxCost > 0) {
                cost = nextInt(2 * maxCost + 1) - maxCost;
            } else {
                cost = maxCost == 0 ? 0 : nextInt(maxCost + 1);
            }
            graph.addEdge(from, to, capacity, cost);
        }
        return graph;
    }

    /** Checks capacity bounds, the residual relationship, conservation and the reported value. */
    static void assertValidMaxFlow(FlowGraph graph, FlowResult result) {
        int source = result.getSource();
        int sink = result.getSink();
        assertEquals(graph.vertexCount(), result.getVertexCount());
        assertEquals(graph.edgeCount(), result.getEdgeCount(), "edge count");
        long[] balance = new long[graph.vertexCount()];
        for (int id = 0; id < graph.edgeCount(); id++) {
            long flow = result.flowOf(id);
            long capacity = graph.edgeCapacity(id);
            assertTrue(flow >= 0 && flow <= capacity, "edge " + id + " flow " + flow
                    + " within [0, " + capacity + "]");
            assertEquals(capacity - flow, result.residualOf(id), "residual = c - f on edge " + id);
            balance[graph.edgeFrom(id)] -= flow;
            balance[graph.edgeTo(id)] += flow;
        }
        assertEquals(-result.getMaxFlow(), balance[source], "net outflow at source");
        assertEquals(result.getMaxFlow(), balance[sink], "net inflow at sink");
        for (int v = 0; v < graph.vertexCount(); v++) {
            if (v != source && v != sink) {
                assertEquals(0L, balance[v], "conservation at vertex " + v);
            }
        }
    }

    /** Checks capacity bounds, conservation and that the reported total cost matches the flows. */
    static void assertValidMinCostFlow(FlowGraph graph, MinCostFlowResult result,
                                       long expectedFlow, long expectedCost) {
        int source = result.getSource();
        int sink = result.getSink();
        long[] balance = new long[graph.vertexCount()];
        long cost = 0;
        for (int id = 0; id < graph.edgeCount(); id++) {
            long flow = result.flowOf(id);
            assertTrue(flow >= 0 && flow <= graph.edgeCapacity(id), "edge " + id + " flow");
            balance[graph.edgeFrom(id)] -= flow;
            balance[graph.edgeTo(id)] += flow;
            cost += flow * graph.edgeCost(id);
        }
        assertEquals(expectedFlow, result.getTotalFlow(), "total flow");
        assertEquals(expectedCost, result.getTotalCost(), "total cost");
        assertEquals(cost, result.getTotalCost(), "cost equals sum f(e)*cost(e)");
        assertEquals(-result.getTotalFlow(), balance[source], "net outflow at source");
        assertEquals(result.getTotalFlow(), balance[sink], "net inflow at sink");
        for (int v = 0; v < graph.vertexCount(); v++) {
            if (v != source && v != sink) {
                assertEquals(0L, balance[v], "conservation at vertex " + v);
            }
        }
    }
}
