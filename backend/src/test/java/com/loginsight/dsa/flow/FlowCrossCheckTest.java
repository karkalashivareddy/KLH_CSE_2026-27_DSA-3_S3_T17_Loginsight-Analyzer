package com.loginsight.dsa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Cross-algorithm consistency: Ford-Fulkerson, Edmonds-Karp and Dinic must all report the same maximum
 * flow on every graph, that value must equal the minimum cut, and solving must not mutate the shared
 * {@link FlowGraph}. This is the strongest check that no single implementation is accidentally tuned to
 * the oracle.
 */
class FlowCrossCheckTest {

    @Test
    void allMaxFlowAlgorithmsAgreeOnClassicNetwork() {
        FlowGraph graph = FlowTestSupport.classicGraph();
        long ff = new FordFulkerson().maxFlow(graph, 0, 5).getMaxFlow();
        long ek = new EdmondsKarp().maxFlow(graph, 0, 5).getMaxFlow();
        long dinic = new Dinic().maxFlow(graph, 0, 5).getMaxFlow();
        long cut = new MinCut().minCut(graph, 0, 5).getCutCapacity();
        assertEquals(FlowTestSupport.CLASSIC_MAX_FLOW, ff);
        assertEquals(ff, ek);
        assertEquals(ff, dinic);
        assertEquals(ff, cut);
    }

    @Test
    void allAlgorithmsAgreeOnRandomGraphs() {
        for (long seed = 1301; seed <= 1600; seed++) {
            FlowTestSupport support = new FlowTestSupport(seed);
            int n = 2 + support.nextInt(5);
            FlowGraph graph = support.randomGraph(n, 6, 4, 0, false);
            int source = 0;
            int sink = n - 1;
            long oracle = FlowOracle.maxFlow(graph, source, sink);
            FlowResult ff = new FordFulkerson().maxFlow(graph, source, sink);
            FlowResult ek = new EdmondsKarp().maxFlow(graph, source, sink);
            FlowResult dinic = new Dinic().maxFlow(graph, source, sink);
            assertEquals(oracle, ff.getMaxFlow(), "Ford-Fulkerson, seed " + seed);
            assertEquals(oracle, ek.getMaxFlow(), "Edmonds-Karp, seed " + seed);
            assertEquals(oracle, dinic.getMaxFlow(), "Dinic, seed " + seed);
            FlowTestSupport.assertValidMaxFlow(graph, ff);
            FlowTestSupport.assertValidMaxFlow(graph, ek);
            FlowTestSupport.assertValidMaxFlow(graph, dinic);
            assertEquals(oracle, new MinCut().minCut(graph, source, sink).getCutCapacity(),
                    "min cut, seed " + seed);
        }
    }

    @Test
    void solvingDoesNotMutateTheGraphDefinition() {
        FlowGraph graph = FlowTestSupport.classicGraph();
        Edge[] before = graph.edges();
        new FordFulkerson().maxFlow(graph, 0, 5);
        new EdmondsKarp().maxFlow(graph, 0, 5);
        new Dinic().maxFlow(graph, 0, 5);
        new MinCut().minCut(graph, 0, 5);
        assertEquals(before.length, graph.edges().length);
        for (int id = 0; id < before.length; id++) {
            assertEquals(before[id].getCapacity(), graph.edgeCapacity(id), "capacity unchanged");
            assertEquals(before[id].getFrom(), graph.edgeFrom(id), "from unchanged");
            assertEquals(before[id].getTo(), graph.edgeTo(id), "to unchanged");
        }
    }

    @Test
    void algorithmsReportTheirOwnIdentity() {
        FlowGraph graph = FlowTestSupport.classicGraph();
        String ff = new FordFulkerson().maxFlow(graph, 0, 5).getAlgorithm();
        String ek = new EdmondsKarp().maxFlow(graph, 0, 5).getAlgorithm();
        String dinic = new Dinic().maxFlow(graph, 0, 5).getAlgorithm();
        assertNotEquals(ff, ek);
        assertNotEquals(ek, dinic);
        assertNotEquals(ff, dinic);
        assertTrue(ff.contains("Ford"));
        assertTrue(ek.contains("Edmonds"));
        assertTrue(dinic.contains("Dinic"));
    }
}
