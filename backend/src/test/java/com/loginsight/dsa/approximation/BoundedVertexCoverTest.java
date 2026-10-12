package com.loginsight.dsa.approximation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the FPT decision {@link BoundedVertexCover}: the decision must agree with the exact
 * subset oracle, any returned certificate must be a valid cover of size at most {@code k}, batches of
 * randomized instances (fixed seeds) and the small deterministic family must all pass, and the
 * branching must be deterministic.
 */
class BoundedVertexCoverTest {

    private final BoundedVertexCover fpt = new BoundedVertexCover();

    @Test
    void edgelessGraphTriviallySolvable() {
        UndirectedGraph graph = new UndirectedGraph(4);
        assertTrue(fpt.hasVertexCover(graph, 0));
        assertEquals(0, fpt.findVertexCover(graph, 0).length);
    }

    @Test
    void singleEdgeNeedsOneOfItsEndpoints() {
        UndirectedGraph graph = new UndirectedGraph(2);
        graph.addEdge(0, 1);
        assertTrue(fpt.hasVertexCover(graph, 1));
        assertFalse(fpt.hasVertexCover(graph, 0));
        assertEquals(1, fpt.findVertexCover(graph, 1).length);
    }

    @Test
    void pathWithThreeVertices() {
        UndirectedGraph graph = new UndirectedGraph(3);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        assertEquals(1, ApproxTestSupport.exactMinimumVertexCover(graph));
        assertTrue(fpt.hasVertexCover(graph, 1));
        assertFalse(fpt.hasVertexCover(graph, 0));
        assertCertificateValid(graph, fpt.findVertexCover(graph, 1), 1);
    }

    @Test
    void triangleNeedsTwoVertices() {
        UndirectedGraph triangle = new UndirectedGraph(3);
        triangle.addEdge(0, 1);
        triangle.addEdge(1, 2);
        triangle.addEdge(2, 0);
        assertEquals(2, ApproxTestSupport.exactMinimumVertexCover(triangle));
        assertTrue(fpt.hasVertexCover(triangle, 2));
        assertFalse(fpt.hasVertexCover(triangle, 1));
        assertCertificateValid(triangle, fpt.findVertexCover(triangle, 2), 2);
    }

    @Test
    void starCenterIsOptimalSingleVertex() {
        UndirectedGraph star = new UndirectedGraph(5);
        star.addEdge(0, 1);
        star.addEdge(0, 2);
        star.addEdge(0, 3);
        star.addEdge(0, 4);
        assertTrue(fpt.hasVertexCover(star, 1));
        assertFalse(fpt.hasVertexCover(star, 0));
        int[] cover = fpt.findVertexCover(star, 1);
        assertEquals(0, cover[0]);
    }

    @Test
    void disconnectedGraphDecisionAgrees() {
        UndirectedGraph graph = new UndirectedGraph(8);
        graph.addEdge(0, 1);
        graph.addEdge(2, 3);
        graph.addEdge(4, 5);
        graph.addEdge(6, 7);
        assertEquals(4, ApproxTestSupport.exactMinimumVertexCover(graph));
        assertTrue(fpt.hasVertexCover(graph, 4));
        assertFalse(fpt.hasVertexCover(graph, 3));
    }

    @Test
    void emptyGraphOverSeveralVerticesWithBigK() {
        UndirectedGraph graph = new UndirectedGraph(6);
        assertTrue(fpt.hasVertexCover(graph, 3));
        assertEquals(0, fpt.findVertexCover(graph, 3).length);
    }

    @Test
    void randomInstancesDecisionMatchesOracle() {
        for (int seed = 1; seed <= 300; seed++) {
            ApproxTestSupport support = new ApproxTestSupport(seed);
            int n = 1 + support.nextInt(7);
            UndirectedGraph graph = support.randomGraph(n, 8);
            int optimum = ApproxTestSupport.exactMinimumVertexCover(graph);
            for (int k = 0; k <= Math.min(optimum + 2, n); k++) {
                boolean expected = k >= optimum;
                boolean actual = fpt.hasVertexCover(graph, k);
                assertEquals(expected, actual, "decision mismatch seed " + seed + " k=" + k);
                if (expected) {
                    assertCertificateValid(graph, fpt.findVertexCover(graph, k), k);
                } else {
                    assertNull(fpt.findVertexCover(graph, k), "no cover should be found, seed " + seed);
                }
            }
        }
    }

    @Test
    void deterministicBranching() {
        UndirectedGraph graph = new UndirectedGraph(6);
        graph.addEdge(0, 3);
        graph.addEdge(1, 3);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);
        graph.addEdge(4, 5);
        int[] first = fpt.findVertexCover(graph, 3);
        int[] second = fpt.findVertexCover(graph, 3);
        assertEquals(first.length, second.length);
        for (int i = 0; i < first.length; i++) {
            assertEquals(first[i], second[i]);
        }
        assertNotNull(first);
    }

    @Test
    void selfLoopsRequireTheirVertex() {
        UndirectedGraph graph = new UndirectedGraph(3);
        graph.addEdge(1, 1);
        assertTrue(fpt.hasVertexCover(graph, 1));
        assertFalse(fpt.hasVertexCover(graph, 0));
        assertCertificateValid(graph, fpt.findVertexCover(graph, 1), 1);
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> fpt.findVertexCover(null, 1));
        assertThrows(IllegalArgumentException.class, () -> fpt.findVertexCover(new UndirectedGraph(2), -1));
    }

    private static void assertCertificateValid(UndirectedGraph graph, int[] cover, int maxSize) {
        assertNotNull(cover);
        assertTrue(cover.length <= maxSize, "certificate must respect the budget");
        boolean[] mask = new boolean[graph.vertexCount()];
        for (int v : cover) {
            assertTrue(v >= 0 && v < graph.vertexCount());
            mask[v] = true;
        }
        assertTrue(IndependentSetReduction.isVertexCover(graph, mask), "certificate must cover every edge");
    }
}