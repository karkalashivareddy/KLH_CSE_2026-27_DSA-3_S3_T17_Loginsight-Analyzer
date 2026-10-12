package com.loginsight.dsa.approximation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the Module 5 reduction demos ({@link IndependentSetReduction}, {@link ComplementGraph}):
 * the verifiers agree with exhaustive subset enumeration, the cover/independent-set complement
 * equivalence holds, the clique/independent-set-in-complement equivalence holds, and the textbook
 * identity {@code tau(G) = n - alpha(G)} is respected by the arithmetic helper.
 */
class ReductionsTest {

    @Test
    void verifiersAgreeWithExhaustiveEnumeration() {
        for (int seed = 1; seed <= 150; seed++) {
            ApproxTestSupport support = new ApproxTestSupport(seed);
            int n = 1 + support.nextInt(6);
            UndirectedGraph graph = support.randomGraph(n, 8);
            for (long mask = 0; mask < (1L << n); mask++) {
                boolean[] selection = maskToSelection(n, mask);
                boolean exhaustiveIndependent = isIndependentByDefinition(graph, mask);
                boolean exhaustiveCover = isCoverByDefinition(graph, mask);
                assertEquals(exhaustiveIndependent,
                        IndependentSetReduction.isIndependentSet(graph, selection),
                        "isIndependentSet disagrees, seed " + seed);
                assertEquals(exhaustiveCover,
                        IndependentSetReduction.isVertexCover(graph, selection),
                        "isVertexCover disagrees, seed " + seed);
                assertEquals(isCliqueByDefinition(graph, mask),
                        ComplementGraph.isClique(graph, selection),
                        "isClique disagrees, seed " + seed);
            }
        }
    }

    @Test
    void coverToIndependentSetAndBack() {
        UndirectedGraph graph = new UndirectedGraph(5);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(3, 4);
        int[] cover = {0, 2, 3};
        int[] independent = IndependentSetReduction.coverToIndependentSet(5, cover);
        int[] back = IndependentSetReduction.independentSetToCover(5, independent);
        assertSameSet(cover, back);
    }

    @Test
    void independentSetComplementIsCoverOnRandomGraphs() {
        for (int seed = 1; seed <= 200; seed++) {
            ApproxTestSupport support = new ApproxTestSupport(seed);
            int n = 1 + support.nextInt(6);
            UndirectedGraph graph = support.randomGraph(n, 8);
            int optimumCover = ApproxTestSupport.exactMinimumVertexCover(graph);
            int maximumIndependent = ApproxTestSupport.exactMaximumIndependentSet(graph);
            assertEquals(n, optimumCover + maximumIndependent, "tau(G) + alpha(G) = n, seed " + seed);
            assertEquals(n - maximumIndependent,
                    IndependentSetReduction.minimumCoverSizeFromMaxIndependentSet(n, maximumIndependent));

            int[] minCover = minCover(graph, n);
            int[] complementOfCover = IndependentSetReduction.coverToIndependentSet(n, minCover);
            assertTrue(IndependentSetReduction.isIndependentSet(graph,
                    selectionMask(n, complementOfCover)), "V - S independent, seed " + seed);
        }
    }

    @Test
    void cliqueEquivalenceWithComplementOnRandomGraphs() {
        for (int seed = 1; seed <= 200; seed++) {
            ApproxTestSupport support = new ApproxTestSupport(seed);
            int n = 1 + support.nextInt(6);
            UndirectedGraph graph = support.randomGraph(n, 8);
            UndirectedGraph complement = ComplementGraph.complement(graph);
            UndirectedGraph reComputed = ComplementGraph.complement(complement);
            assertEquals(graph.edgeCount(), reComputed.edgeCount(),
                    "complement is an involution (same edge count), seed " + seed);
            for (int u = 0; u < n; u++) {
                for (int v = u + 1; v < n; v++) {
                    assertEquals(graph.hasEdge(u, v), reComputed.hasEdge(u, v),
                            "complement is an involution, seed " + seed);
                }
            }
            for (long mask = 0; mask < (1L << n); mask += Math.max(1, (1L << n) / 64)) {
                boolean[] selection = maskToSelection(n, mask);
                assertEquals(ComplementGraph.isClique(graph, selection),
                        IndependentSetReduction.isIndependentSet(complement, selection),
                        "S clique in G iff S independent in complement, seed " + seed);
            }
        }
    }

    @Test
    void cliqueInComplementMatchesIndependentSetInGraph() {
        UndirectedGraph graph = new UndirectedGraph(4);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        UndirectedGraph complement = ComplementGraph.complement(graph);
        // {0,2} are non-adjacent in G -> a clique of size 2 in the complement.
        assertTrue(ComplementGraph.isClique(complement, selectionMask(4, new int[]{0, 2})));
        // {0,1} are adjacent in G -> NOT a clique in the complement.
        assertTrue(!ComplementGraph.isClique(complement, selectionMask(4, new int[]{0, 1})));
    }

    private static boolean[] maskToSelection(int n, long mask) {
        boolean[] selection = new boolean[n];
        for (int v = 0; v < n; v++) {
            selection[v] = (mask & (1L << v)) != 0;
        }
        return selection;
    }

    private static boolean isCoverByDefinition(UndirectedGraph graph, long mask) {
        for (int id = 0; id < graph.edgeCount(); id++) {
            UndirectedEdge edge = graph.edge(id);
            if ((mask & (1L << edge.getU())) == 0 && (mask & (1L << edge.getV())) == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIndependentByDefinition(UndirectedGraph graph, long mask) {
        for (int id = 0; id < graph.edgeCount(); id++) {
            UndirectedEdge edge = graph.edge(id);
            if ((mask & (1L << edge.getU())) != 0 && (mask & (1L << edge.getV())) != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCliqueByDefinition(UndirectedGraph graph, long mask) {
        int n = graph.vertexCount();
        for (int u = 0; u < n; u++) {
            if ((mask & (1L << u)) == 0) {
                continue;
            }
            for (int v = u + 1; v < n; v++) {
                if ((mask & (1L << v)) != 0 && !graph.hasEdge(u, v)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean[] selectionMask(int n, int[] vertices) {
        return IndependentSetReduction.selectionMask(n, vertices);
    }

    private static int[] minCover(UndirectedGraph graph, int n) {
        int optimum = ApproxTestSupport.exactMinimumVertexCover(graph);
        for (long mask = 0; mask < (1L << n); mask++) {
            if (Long.bitCount(mask) == optimum && isCoverByDefinition(graph, mask)) {
                return maskToVertices(n, mask);
            }
        }
        throw new IllegalStateException("no minimum cover found");
    }

    private static int[] maskToVertices(int n, long mask) {
        int[] values = new int[n];
        int count = 0;
        for (int v = 0; v < n; v++) {
            if ((mask & (1L << v)) != 0) {
                values[count++] = v;
            }
        }
        int[] result = new int[count];
        System.arraycopy(values, 0, result, 0, count);
        return result;
    }

    private static void assertSameSet(int[] first, int[] second) {
        assertEquals(first.length, second.length);
        for (int i = 0; i < first.length; i++) {
            assertEquals(first[i], second[i]);
        }
    }
}