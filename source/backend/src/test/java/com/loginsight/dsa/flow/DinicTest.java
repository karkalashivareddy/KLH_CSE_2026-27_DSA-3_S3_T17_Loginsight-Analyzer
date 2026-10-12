package com.loginsight.dsa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Behavioural tests for {@link Dinic}: known values, invariants, random-oracle agreement and validation.
 * Dinic also powers {@link BipartiteMatching} and {@link MinCut}, so those tests exercise it along
 * different reductions.
 */
class DinicTest {

    private final Dinic dinic = new Dinic();

    @Test
    void solvesClassicNetwork() {
        FlowResult result = dinic.maxFlow(FlowTestSupport.classicGraph(), 0, 5);
        assertEquals(FlowTestSupport.CLASSIC_MAX_FLOW, result.getMaxFlow());
        assertEquals("Dinic", result.getAlgorithm());
        FlowTestSupport.assertValidMaxFlow(FlowTestSupport.classicGraph(), result);
    }

    @Test
    void blockingFlowDrainsLayeredNetworkInOneRound() {
        FlowGraph graph = new FlowGraph(4);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 2, 5);
        graph.addEdge(1, 3, 3);
        graph.addEdge(2, 3, 7);
        FlowResult result = dinic.maxFlow(graph, 0, 3);
        assertEquals(8, result.getMaxFlow());
        FlowTestSupport.assertValidMaxFlow(graph, result);
    }

    @Test
    void unitCapacityNetworkMatchesEdgeDisjointPaths() {
        FlowGraph graph = new FlowGraph(5);
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 2, 1);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(1, 4, 1);
        graph.addEdge(2, 4, 1);
        FlowResult result = dinic.maxFlow(graph, 0, 4);
        assertEquals(2, result.getMaxFlow());
        FlowTestSupport.assertValidMaxFlow(graph, result);
    }

    @Test
    void matchesOracleOnRandomGraphs() {
        for (long seed = 401; seed <= 650; seed++) {
            FlowTestSupport support = new FlowTestSupport(seed);
            int n = 2 + support.nextInt(4);
            FlowGraph graph = support.randomGraph(n, 5, 3, 0, false);
            int source = 0;
            int sink = n - 1;
            long expected = FlowOracle.maxFlow(graph, source, sink);
            FlowResult result = dinic.maxFlow(graph, source, sink);
            assertEquals(expected, result.getMaxFlow(), "seed " + seed);
            FlowTestSupport.assertValidMaxFlow(graph, result);
        }
    }

    @Test
    void rejectsInvalidArguments() {
        FlowGraph graph = new FlowGraph(3);
        assertThrows(IllegalArgumentException.class, () -> dinic.maxFlow(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> dinic.maxFlow(graph, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> dinic.maxFlow(graph, -1, 1));
    }
}
