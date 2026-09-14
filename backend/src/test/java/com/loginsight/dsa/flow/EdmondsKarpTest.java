package com.loginsight.dsa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Behavioural tests for {@link EdmondsKarp}: known maximum-flow values, flow invariants, agreement with
 * the independent oracle, and validation. The shortest-path (BFS) search is exercised indirectly via
 * the random comparisons and the classic network, whose value only a correct search can reach.
 */
class EdmondsKarpTest {

    private final EdmondsKarp edmondsKarp = new EdmondsKarp();

    @Test
    void solvesClassicNetwork() {
        FlowResult result = edmondsKarp.maxFlow(FlowTestSupport.classicGraph(), 0, 5);
        assertEquals(FlowTestSupport.CLASSIC_MAX_FLOW, result.getMaxFlow());
        assertEquals("Edmonds-Karp (BFS)", result.getAlgorithm());
        FlowTestSupport.assertValidMaxFlow(FlowTestSupport.classicGraph(), result);
    }

    @Test
    void unreachableSinkYieldsZeroFlow() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 1, 5);
        assertEquals(0, edmondsKarp.maxFlow(graph, 0, 2).getMaxFlow());
    }

    @Test
    void shortestAugmentingPathsReachOptimum() {
        FlowGraph graph = new FlowGraph(4);
        graph.addEdge(0, 1, 1000);
        graph.addEdge(0, 2, 1000);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 1);
        FlowResult result = edmondsKarp.maxFlow(graph, 0, 3);
        assertEquals(2, result.getMaxFlow());
        FlowTestSupport.assertValidMaxFlow(graph, result);
    }

    @Test
    void matchesOracleOnRandomGraphs() {
        for (long seed = 201; seed <= 400; seed++) {
            FlowTestSupport support = new FlowTestSupport(seed);
            int n = 2 + support.nextInt(4);
            FlowGraph graph = support.randomGraph(n, 5, 3, 0, false);
            int source = 0;
            int sink = n - 1;
            long expected = FlowOracle.maxFlow(graph, source, sink);
            FlowResult result = edmondsKarp.maxFlow(graph, source, sink);
            assertEquals(expected, result.getMaxFlow(), "seed " + seed);
            FlowTestSupport.assertValidMaxFlow(graph, result);
        }
    }

    @Test
    void rejectsInvalidArguments() {
        FlowGraph graph = new FlowGraph(2);
        assertThrows(IllegalArgumentException.class, () -> edmondsKarp.maxFlow(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> edmondsKarp.maxFlow(graph, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> edmondsKarp.maxFlow(graph, 0, 2));
    }
}
