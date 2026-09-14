package com.loginsight.dsa.approximation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VertexCoverKernelization}. The decision equivalence {@code tau(G) <= k} must be
 * preserved exactly (verified with the subset oracle on both the original and the reduced graph), and
 * on YES instances the stronger identity {@code tau(G) = tau(reduced) + |forced|} must hold. The
 * degree, self-loop and isolated-vertex rules are exercised on deterministic families and seeded
 * random graphs.
 */
class KernelizationTest {

    private final VertexCoverKernelization kernel = new VertexCoverKernelization();

    @Test
    void edgelessGraphReducesTrivially() {
        UndirectedGraph graph = new UndirectedGraph(5);
        KernelizationResult result = kernel.kernelize(graph, 3);
        assertEquals(0, result.getForcedCount());
        assertFalse(result.isInfeasible());
        assertEquals(3, result.getRemainingK());
        assertEquals(0, ApproxTestSupport.exactMinimumVertexCover(result.getReducedGraph()));
    }

    @Test
    void highDegreeStarForcesCenter() {
        UndirectedGraph star = new UndirectedGraph(5);
        star.addEdge(0, 1);
        star.addEdge(0, 2);
        star.addEdge(0, 3);
        star.addEdge(0, 4);
        KernelizationResult result = kernel.kernelize(star, 1);
        assertEquals(1, result.getForcedCount());
        assertEquals(0, result.getForcedVertices()[0]);
        assertEquals(0, result.getRemainingK());
        assertFalse(result.isInfeasible());
        assertEquals(0, ApproxTestSupport.exactMinimumVertexCover(result.getReducedGraph()));
    }

    @Test
    void selfLoopForcesItsVertexAndReducesGraph() {
        UndirectedGraph graph = new UndirectedGraph(3);
        graph.addEdge(1, 1);
        KernelizationResult result = kernel.kernelize(graph, 2);
        assertEquals(1, result.getForcedCount());
        assertEquals(1, result.getForcedVertices()[0]);
    }

    @Test
    void budgetExhaustedBecomesInfeasible() {
        UndirectedGraph graph = new UndirectedGraph(2);
        graph.addEdge(0, 1);
        KernelizationResult result = kernel.kernelize(graph, 0);
        assertTrue(result.isInfeasible());
        // decision must be NO for k=0 when an edge remains
        assertFalse(decision(result));
    }

    @Test
    void highDegreeWithExactBudgetIsSolvable() {
        UndirectedGraph complete = new UndirectedGraph(4);
        for (int u = 0; u < 4; u++) {
            for (int v = u + 1; v < 4; v++) {
                complete.addEdge(u, v);
            }
        }
        // K4: tau = 3 > k = 2 -> the degree rule forces vertices until the budget is exhausted on a
        // remaining edge: the instance is (correctly) declared infeasible.
        KernelizationResult result = kernel.kernelize(complete, 2);
        assertTrue(result.isInfeasible());
        assertEquals(2, result.getForcedCount());
    }

    @Test
    void truthTablePreservedOnDeterministicFamilies() {
        // P3
        UndirectedGraph path = new UndirectedGraph(3);
        path.addEdge(0, 1);
        path.addEdge(1, 2);
        assertDecisionMatch(path, new int[]{0, 1, 2});
        // C5
        UndirectedGraph cycle = new UndirectedGraph(5);
        cycle.addEdge(0, 1);
        cycle.addEdge(1, 2);
        cycle.addEdge(2, 3);
        cycle.addEdge(3, 4);
        cycle.addEdge(4, 0);
        assertDecisionMatch(cycle, new int[]{1, 2, 3});
        // two disjoint edges
        UndirectedGraph two = new UndirectedGraph(4);
        two.addEdge(0, 1);
        two.addEdge(2, 3);
        assertDecisionMatch(two, new int[]{0, 1, 2});
    }

    @Test
    void highDegreeRuleMatchesOracleOnManyRandomGraphs() {
        for (int seed = 1; seed <= 400; seed++) {
            ApproxTestSupport support = new ApproxTestSupport(seed);
            int n = 2 + support.nextInt(6);
            UndirectedGraph graph = support.randomGraph(n, 8);
            int optimum = ApproxTestSupport.exactMinimumVertexCover(graph);
            for (int k = 0; k <= Math.min(optimum + 1, n); k++) {
                KernelizationResult result = kernel.kernelize(graph, k);
                assertEquals(k >= optimum, decision(result),
                        "decision equivalence, seed " + seed + " k=" + k);
                if (k >= optimum) {
                    // YES instance: exact identity must hold
                    int reducedTau = ApproxTestSupport.exactMinimumVertexCover(result.getReducedGraph());
                    assertEquals(optimum, reducedTau + result.getForcedCount(),
                            "identity tau(G)=tau(reduced)+forced, seed " + seed + " k=" + k);
                    assertFalse(result.isInfeasible(), "YES instance cannot be infeasible");
                }
            }
        }
    }

    @Test
    void reducedGraphDegreeBoundedByRemainingKUnlessInfeasible() {
        for (int seed = 1; seed <= 200; seed++) {
            ApproxTestSupport support = new ApproxTestSupport(seed);
            int n = 2 + support.nextInt(6);
            UndirectedGraph graph = support.randomGraph(n, 8);
            KernelizationResult result = kernel.kernelize(graph, 2);
            if (!result.isInfeasible()) {
                UndirectedGraph reduced = result.getReducedGraph();
                for (int v = 0; v < reduced.vertexCount(); v++) {
                    if (reduced.degree(v) > 0) {
                        assertTrue(reduced.degree(v) <= result.getRemainingK(),
                                "max degree <= remaining k, seed " + seed);
                    }
                }
            }
        }
    }

    private static boolean decision(KernelizationResult result) {
        if (result.isInfeasible()) {
            return false;
        }
        return ApproxTestSupport.exactMinimumVertexCover(result.getReducedGraph()) <= result.getRemainingK();
    }

    private void assertDecisionMatch(UndirectedGraph graph, int[] ks) {
        int optimum = ApproxTestSupport.exactMinimumVertexCover(graph);
        for (int k : ks) {
            KernelizationResult result = kernel.kernelize(graph, k);
            assertEquals(k >= optimum, decision(result), "family instance k=" + k);
            int reducedTau = ApproxTestSupport.exactMinimumVertexCover(result.getReducedGraph());
            assertEquals(optimum, reducedTau + result.getForcedCount(), "family identity k=" + k);
        }
    }
}