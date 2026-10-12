package com.loginsight.dsa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MinCut}: max-flow/min-cut equality, a valid vertex partition, crossing edges that
 * really do cross and account for the capacity, and agreement with subset-enumeration on random graphs.
 */
class MinCutTest {

    private final MinCut minCut = new MinCut();

    @Test
    void cutCapacityEqualsMaxFlowOnClassicNetwork() {
        MinCutResult result = minCut.minCut(FlowTestSupport.classicGraph(), 0, 5);
        assertEquals(FlowTestSupport.CLASSIC_MAX_FLOW, result.getMaxFlow());
        assertEquals(FlowTestSupport.CLASSIC_MAX_FLOW, result.getCutCapacity());
        assertTrue(result.isOnSourceSide(0));
        assertFalse(result.isOnSinkSide(0));
        assertTrue(result.isOnSinkSide(5));
        assertFalse(result.isOnSourceSide(5));
    }

    @Test
    void cutEdgesCrossFromSourceToSinkSide() {
        FlowGraph graph = FlowTestSupport.classicGraph();
        MinCutResult result = minCut.minCut(graph, 0, 5);
        long summed = 0;
        for (Edge edge : result.getCutEdges()) {
            assertTrue(result.isOnSourceSide(edge.getFrom()), "cut edge starts on source side");
            assertTrue(result.isOnSinkSide(edge.getTo()), "cut edge ends on sink side");
            summed += edge.getCapacity();
        }
        assertEquals(result.getCutCapacity(), summed, "cut edges account for the capacity");
    }

    @Test
    void partitionCoversEveryVertexExactlyOnce() {
        MinCutResult result = minCut.minCut(FlowTestSupport.classicGraph(), 0, 5);
        for (int v = 0; v < result.getVertexCount(); v++) {
            assertTrue(result.isOnSourceSide(v) ^ result.isOnSinkSide(v),
                    "vertex " + v + " on exactly one side");
        }
    }

    @Test
    void unsplittableNetworkBottlenecksAtASingleEdge() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 1, 4);
        graph.addEdge(1, 2, 4);
        MinCutResult result = minCut.minCut(graph, 0, 2);
        assertEquals(4, result.getCutCapacity());
        assertEquals(1, result.getCutEdges().length);
    }

    @Test
    void matchesOracleOnRandomGraphs() {
        for (long seed = 651; seed <= 850; seed++) {
            FlowTestSupport support = new FlowTestSupport(seed);
            int n = 2 + support.nextInt(4);
            FlowGraph graph = support.randomGraph(n, 5, 3, 0, false);
            int source = 0;
            int sink = n - 1;
            long expected = FlowOracle.minCutCapacity(graph, source, sink);
            MinCutResult result = minCut.minCut(graph, source, sink);
            assertEquals(expected, result.getCutCapacity(), "cut capacity, seed " + seed);
            assertEquals(expected, result.getMaxFlow(), "max flow equals cut, seed " + seed);
        }
    }

    @Test
    void rejectsInvalidArguments() {
        FlowGraph graph = new FlowGraph(2);
        assertThrows(IllegalArgumentException.class, () -> minCut.minCut(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> minCut.minCut(graph, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> minCut.minCut(graph, 0, 9));
    }
}
