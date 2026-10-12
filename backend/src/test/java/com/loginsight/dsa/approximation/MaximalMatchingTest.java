package com.loginsight.dsa.approximation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MaximalMatching}: edges in a greedy maximal matching are vertex-disjoint, the
 * matching is genuinely maximal (no edge of the graph is untouched by it), self-loops are excluded,
 * and the greedy scan is deterministic.
 */
class MaximalMatchingTest {

    private final MaximalMatching matching = new MaximalMatching();

    @Test
    void singleEdgeMatched() {
        UndirectedGraph graph = new UndirectedGraph(2);
        graph.addEdge(0, 1);
        assertTrue(matching.matchingEdgeIds(graph).length == 1);
        assertTrue(matching.matchingSize(graph) == 1);
    }

    @Test
    void pathOfLengthThreeYieldsTwoMatchedEdges() {
        UndirectedGraph graph = new UndirectedGraph(4);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        // Greedy: (0,1) matched, (2,3) matched -> size 2 (a maximum matching!)
        assertTrue(matching.matchingSize(graph) == 2);
    }

    @Test
    void starYieldsOneMatchedEdge() {
        UndirectedGraph graph = new UndirectedGraph(4);
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(0, 3);
        assertTrue(matching.matchingSize(graph) == 1);
    }

    @Test
    void selfLoopsNeverEnterMatching() {
        UndirectedGraph graph = new UndirectedGraph(2);
        graph.addEdge(0, 0);
        graph.addEdge(0, 1);
        assertTrue(matching.matchingSize(graph) == 1);
    }

    @Test
    void matchingIsEndpointDisjointAndMaximalOnRandomGraphs() {
        for (int seed = 1; seed <= 150; seed++) {
            ApproxTestSupport support = new ApproxTestSupport(seed);
            int n = 2 + support.nextInt(6);
            UndirectedGraph graph = support.randomGraph(n, 8);
            int[] matched = matching.matchingEdgeIds(graph);
            boolean[] used = new boolean[n];
            for (int edgeId : matched) {
                UndirectedEdge edge = graph.edge(edgeId);
                assertTrue(!edge.isSelfLoop(), "loop never matched");
                assertTrue(!used[edge.getU()] && !used[edge.getV()], "endpoints disjoint");
                used[edge.getU()] = true;
                used[edge.getV()] = true;
            }
            for (int id = 0; id < graph.edgeCount(); id++) {
                UndirectedEdge edge = graph.edge(id);
                boolean untouched = !edge.isSelfLoop() && !used[edge.getU()] && !used[edge.getV()];
                assertTrue(!untouched, "maximality: every non-loop edge touches a matched vertex");
            }
        }
    }

    @Test
    void rejectNullGraph() {
        assertThrows(IllegalArgumentException.class, () -> matching.matchingEdgeIds(null));
    }
}