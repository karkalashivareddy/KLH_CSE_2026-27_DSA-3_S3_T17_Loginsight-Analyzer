package com.loginsight.dsa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MinCostMaxFlow}: it must first maximize flow and then minimize cost, use reverse
 * residual edges with negative cost, handle negative edge costs without cycles, reject reachable
 * negative-cost cycles (where Bellman-Ford is undefined), and agree with exhaustive enumeration on
 * small graphs.
 */
class MinCostMaxFlowTest {

    private final MinCostMaxFlow minCostMaxFlow = new MinCostMaxFlow();

    @Test
    void maximizesFlowThenMinimizesCost() {
        FlowGraph graph = new FlowGraph(4);
        graph.addEdge(0, 1, 2, 1);
        graph.addEdge(1, 3, 1, 5);
        graph.addEdge(0, 2, 2, 2);
        graph.addEdge(2, 3, 2, 3);
        MinCostFlowResult result = minCostMaxFlow.minCostMaxFlow(graph, 0, 3);
        assertEquals(3, result.getTotalFlow());
        assertEquals(16, result.getTotalCost());
        FlowTestSupport.assertValidMinCostFlow(graph, result, 3, 16);
    }

    @Test
    void prefersCheaperRouteWhenCapacityAllows() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 1, 1, 10);
        graph.addEdge(1, 2, 1, 10);
        graph.addEdge(0, 2, 1, 1);
        MinCostFlowResult result = minCostMaxFlow.minCostMaxFlow(graph, 0, 2);
        assertEquals(2, result.getTotalFlow());
        assertEquals(21, result.getTotalCost(), "one unit direct (1) + one unit via 1 (20)");
    }

    @Test
    void handlesNegativeEdgeCostsWithoutCycles() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 1, 1, -5);
        graph.addEdge(1, 2, 1, 1);
        MinCostFlowResult result = minCostMaxFlow.minCostMaxFlow(graph, 0, 2);
        assertEquals(1, result.getTotalFlow());
        assertEquals(-4, result.getTotalCost());
    }

    @Test
    void rejectsReachableNegativeCostCycle() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 1, 1, -1);
        graph.addEdge(1, 0, 1, -1);
        graph.addEdge(1, 2, 1, 0);
        assertThrows(IllegalArgumentException.class, () -> minCostMaxFlow.minCostMaxFlow(graph, 0, 2));
    }

    @Test
    void unreachableSinkYieldsZeroFlowAndCost() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 1, 5, 2);
        MinCostFlowResult result = minCostMaxFlow.minCostMaxFlow(graph, 0, 2);
        assertEquals(0, result.getTotalFlow());
        assertEquals(0, result.getTotalCost());
    }

    @Test
    void matchesOracleOnRandomGraphs() {
        for (long seed = 1101; seed <= 1300; seed++) {
            FlowTestSupport support = new FlowTestSupport(seed);
            int n = 2 + support.nextInt(4);
            FlowGraph graph = support.randomGraph(n, 5, 3, 3, false);
            int source = 0;
            int sink = n - 1;
            long[] expected = FlowOracle.minCostMaxFlow(graph, source, sink);
            MinCostFlowResult result = minCostMaxFlow.minCostMaxFlow(graph, source, sink);
            FlowTestSupport.assertValidMinCostFlow(graph, result, expected[0], expected[1]);
        }
    }

    @Test
    void rejectsInvalidArguments() {
        FlowGraph graph = new FlowGraph(2);
        assertThrows(IllegalArgumentException.class, () -> minCostMaxFlow.minCostMaxFlow(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> minCostMaxFlow.minCostMaxFlow(graph, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> minCostMaxFlow.minCostMaxFlow(graph, 0, 5));
    }
}
