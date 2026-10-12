package com.loginsight.dsa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Behavioural tests for {@link FordFulkerson}: correct values on known networks, the structural
 * invariants every max flow must satisfy, input validation, and agreement with the independent
 * brute-force oracle on small random graphs.
 */
class FordFulkersonTest {

    private final FordFulkerson fordFulkerson = new FordFulkerson();

    @Test
    void solvesClassicNetwork() {
        FlowResult result = fordFulkerson.maxFlow(FlowTestSupport.classicGraph(), 0, 5);
        assertEquals(FlowTestSupport.CLASSIC_MAX_FLOW, result.getMaxFlow());
        assertEquals("Ford-Fulkerson (DFS)", result.getAlgorithm());
        FlowTestSupport.assertValidMaxFlow(FlowTestSupport.classicGraph(), result);
    }

    @Test
    void singleEdgeCarriesItsCapacity() {
        FlowGraph graph = new FlowGraph(2);
        graph.addEdge(0, 1, 7);
        FlowResult result = fordFulkerson.maxFlow(graph, 0, 1);
        assertEquals(7, result.getMaxFlow());
        assertEquals(7, result.flowOf(0));
        assertEquals(0, result.residualOf(0));
    }

    @Test
    void unreachableSinkYieldsZeroFlow() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 1, 5);
        FlowResult result = fordFulkerson.maxFlow(graph, 0, 2);
        assertEquals(0, result.getMaxFlow());
        assertEquals(0, result.getAugmentationCount());
    }

    @Test
    void parallelEdgesAddUp() {
        FlowGraph graph = new FlowGraph(2);
        graph.addEdge(0, 1, 3);
        graph.addEdge(0, 1, 4);
        FlowResult result = fordFulkerson.maxFlow(graph, 0, 1);
        assertEquals(7, result.getMaxFlow());
        assertEquals(3, result.flowOf(0));
        assertEquals(4, result.flowOf(1));
    }

    @Test
    void zeroCapacityEdgesAreHarmless() {
        FlowGraph graph = new FlowGraph(2);
        graph.addEdge(0, 1, 0);
        FlowResult result = fordFulkerson.maxFlow(graph, 0, 1);
        assertEquals(0, result.getMaxFlow());
    }

    @Test
    void matchesOracleOnRandomGraphs() {
        for (long seed = 1; seed <= 200; seed++) {
            FlowTestSupport support = new FlowTestSupport(seed);
            int n = 2 + support.nextInt(4);
            FlowGraph graph = support.randomGraph(n, 5, 3, 0, false);
            int source = 0;
            int sink = n - 1;
            long expected = FlowOracle.maxFlow(graph, source, sink);
            FlowResult result = fordFulkerson.maxFlow(graph, source, sink);
            assertEquals(expected, result.getMaxFlow(), "seed " + seed);
            FlowTestSupport.assertValidMaxFlow(graph, result);
        }
    }

    @Test
    void rejectsInvalidArguments() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> fordFulkerson.maxFlow(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> fordFulkerson.maxFlow(graph, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> fordFulkerson.maxFlow(graph, -1, 2));
        assertThrows(IllegalArgumentException.class, () -> fordFulkerson.maxFlow(graph, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> new FlowGraph(-1));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(0, 1, -5));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(0, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> graph.edge(99));
        assertTrue(graph.edgeCount() >= 1);
    }
}
