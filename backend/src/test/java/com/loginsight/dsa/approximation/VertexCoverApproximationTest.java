package com.loginsight.dsa.approximation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VertexCoverApproximation}. Every returned set must be a valid cover, its size must
 * lie between the optimum and twice the optimum, self-loops must force their vertex, output must be
 * deterministic, and the empty graph must return an empty cover.
 *
 * <p>The optimum used in the ratio assertions comes exclusively from {@link ApproxTestSupport}'s
 * exhaustive subset oracle — never from the production algorithm.</p>
 */
class VertexCoverApproximationTest {

    private final VertexCoverApproximation approx = new VertexCoverApproximation();

    @Test
    void emptyGraphReturnsEmptyCover() {
        ApproximationResult result = approx.approximateVertexCover(new UndirectedGraph(3));
        assertEquals(0, result.getCoverSize());
        assertEquals(0, result.getLowerBound());
        assertEquals(1.0, result.getApproximationRatio());
    }

    @Test
    void singleEdgeCoveredByBothEndpoints() {
        UndirectedGraph graph = new UndirectedGraph(2);
        graph.addEdge(0, 1);
        ApproximationResult result = approx.approximateVertexCover(graph);
        assertEquals(2, result.getCoverSize());
        assertEquals(1, result.getMatchingSize());
        assertCoverValid(graph, result);
    }

    @Test
    void pathAndCycleAreWithinFactorTwo() {
        UndirectedGraph path = new UndirectedGraph(4);
        path.addEdge(0, 1);
        path.addEdge(1, 2);
        path.addEdge(2, 3);
        checkApproximation(path, 2);

        UndirectedGraph cycle = new UndirectedGraph(4);
        cycle.addEdge(0, 1);
        cycle.addEdge(1, 2);
        cycle.addEdge(2, 3);
        cycle.addEdge(3, 0);
        checkApproximation(cycle, 2);
    }

    @Test
    void starOptimalCandidate() {
        UndirectedGraph star = new UndirectedGraph(5);
        star.addEdge(0, 1);
        star.addEdge(0, 2);
        star.addEdge(0, 3);
        star.addEdge(0, 4);
        ApproximationResult result = approx.approximateVertexCover(star);
        assertEquals(1, ApproxTestSupport.exactMinimumVertexCover(star));
        assertEquals(2, result.getCoverSize());
        assertEquals(2.0, result.getApproximationRatio());
        assertCoverValid(star, result);
    }

    @Test
    void completeGraphCovered() {
        UndirectedGraph complete = new UndirectedGraph(4);
        for (int u = 0; u < 4; u++) {
            for (int v = u + 1; v < 4; v++) {
                complete.addEdge(u, v);
            }
        }
        checkApproximation(complete, 3);
    }

    @Test
    void selfLoopForcesItsVertex() {
        UndirectedGraph graph = new UndirectedGraph(3);
        graph.addEdge(1, 1);
        ApproximationResult result = approx.approximateVertexCover(graph);
        assertTrue(result.getForcedSize() == 1);
        assertTrue(contains(result.getCover(), 1));
        assertCoverValid(graph, result);
        assertEquals(1, ApproxTestSupport.exactMinimumVertexCover(graph));
    }

    @Test
    void isolatedVerticesNeverEnterCover() {
        UndirectedGraph graph = new UndirectedGraph(4);
        graph.addEdge(0, 1);
        ApproximationResult result = approx.approximateVertexCover(graph);
        assertEquals(2, result.getCoverSize());
        for (int v : result.getCover()) {
            assertTrue(v < 2, "isolated 2 and 3 must not be selected");
        }
    }

    @Test
    void duplicateEdgesAreIgnoredBySimpleGraphContract() {
        UndirectedGraph graph = new UndirectedGraph(2);
        graph.addEdge(0, 1);
        graph.addEdge(0, 1);
        graph.addEdge(0, 1);
        ApproximationResult result = approx.approximateVertexCover(graph);
        assertEquals(2, result.getCoverSize());
        assertCoverValid(graph, result);
    }

    @Test
    void disconnectedGraphWithManyComponents() {
        UndirectedGraph graph = new UndirectedGraph(8);
        graph.addEdge(0, 1);
        graph.addEdge(2, 3);
        graph.addEdge(4, 5);
        graph.addEdge(6, 7);
        ApproximationResult result = approx.approximateVertexCover(graph);
        assertEquals(4, ApproxTestSupport.exactMinimumVertexCover(graph));
        assertEquals(8, result.getCoverSize());
        assertEquals(2.0, result.getApproximationRatio());
        assertCoverValid(graph, result);
    }

    @Test
    void deterministicAcrossRuns() {
        UndirectedGraph graph = new UndirectedGraph(6);
        graph.addEdge(0, 3);
        graph.addEdge(1, 3);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);
        graph.addEdge(4, 5);
        ApproximationResult first = approx.approximateVertexCover(graph);
        ApproximationResult second = approx.approximateVertexCover(graph);
        assertEquals(first.getCoverSize(), second.getCoverSize());
        for (int i = 0; i < first.getCover().length; i++) {
            assertEquals(first.getCover()[i], second.getCover()[i]);
        }
        assertCoverValid(graph, first);
    }

    @Test
    void approximationGuaranteeHoldsOnRandomGraphs() {
        for (int seed = 1; seed <= 400; seed++) {
            ApproxTestSupport support = new ApproxTestSupport(seed);
            int n = 2 + support.nextInt(5);
            UndirectedGraph graph = support.randomGraph(n, 8);
            int optimum = ApproxTestSupport.exactMinimumVertexCover(graph);
            ApproximationResult result = approx.approximateVertexCover(graph);
            assertCoverValid(graph, result);
            assertTrue(result.getCoverSize() >= optimum, "approx >= optimum, seed " + seed);
            assertTrue(result.getCoverSize() <= 2 * optimum, "approx <= 2*OPT, seed " + seed);
            assertTrue(result.getLowerBound() <= optimum, "lower bound <= OPT, seed " + seed);
            assertTrue(result.getApproximationRatio() <= 2.0 + 1e-9, "ratio <= 2, seed " + seed);
            assertTrue(result.getApproximationRatio() >= 1.0 - 1e-9, "ratio >= 1, seed " + seed);
        }
    }

    @Test
    void rejectNullGraph() {
        assertThrows(IllegalArgumentException.class, () -> approx.approximateVertexCover(null));
    }

    private void checkApproximation(UndirectedGraph graph, int expectedOptimum) {
        int optimum = ApproxTestSupport.exactMinimumVertexCover(graph);
        assertEquals(expectedOptimum, optimum);
        ApproximationResult result = approx.approximateVertexCover(graph);
        assertCoverValid(graph, result);
        assertTrue(result.getCoverSize() >= optimum);
        assertTrue(result.getCoverSize() <= 2 * optimum);
    }

    private static boolean contains(int[] values, int target) {
        for (int v : values) {
            if (v == target) {
                return true;
            }
        }
        return false;
    }

    private static void assertCoverValid(UndirectedGraph graph, ApproximationResult result) {
        boolean[] selected = IndependentSetReduction.selectionMask(graph.vertexCount(), result.getCover());
        assertTrue(IndependentSetReduction.isVertexCover(graph, selected),
                "returned set must cover every edge");
    }
}