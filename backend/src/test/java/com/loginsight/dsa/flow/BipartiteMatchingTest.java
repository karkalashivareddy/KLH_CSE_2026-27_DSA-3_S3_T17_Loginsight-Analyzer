package com.loginsight.dsa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BipartiteMatching}: cardinality on known graphs, structural validity of the returned
 * matching (partners consistent, no vertex reused), behaviour on the classic "greedy fails" case, and
 * agreement with backtracking enumeration on random bipartite graphs.
 */
class BipartiteMatchingTest {

    private final BipartiteMatching matching = new BipartiteMatching();

    @Test
    void findsPerfectMatchingOnCompleteBipartite() {
        FlowGraph graph = new FlowGraph(4);
        for (int left = 0; left < 2; left++) {
            for (int right = 0; right < 2; right++) {
                graph.addEdge(left, right, 1);
            }
        }
        MatchingResult result = matching.maxMatching(graph, 2, 2);
        assertEquals(2, result.getMatchingSize());
        assertValid(result);
    }

    @Test
    void avoidsTheGreedyTrap() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 0, 1);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 0, 1);
        MatchingResult result = matching.maxMatching(graph, 2, 2);
        assertEquals(2, result.getMatchingSize());
        assertValid(result);
    }

    @Test
    void reportsUnmatchedVertices() {
        FlowGraph graph = new FlowGraph(5);
        graph.addEdge(0, 0, 1);
        MatchingResult result = matching.maxMatching(graph, 3, 2);
        assertEquals(1, result.getMatchingSize());
        assertEquals(2, result.getUnmatchedLeft().length);
        assertEquals(1, result.getUnmatchedRight().length);
        assertValid(result);
    }

    @Test
    void emptySideYieldsEmptyMatching() {
        FlowGraph graph = new FlowGraph(2);
        assertEquals(0, matching.maxMatching(graph, 0, 2).getMatchingSize());
        assertEquals(0, matching.maxMatching(graph, 2, 0).getMatchingSize());
    }

    @Test
    void duplicateEdgesCannotInflateMatching() {
        FlowGraph graph = new FlowGraph(2);
        graph.addEdge(0, 0, 1);
        graph.addEdge(0, 0, 1);
        graph.addEdge(0, 0, 1);
        MatchingResult result = matching.maxMatching(graph, 1, 1);
        assertEquals(1, result.getMatchingSize());
        assertValid(result);
    }

    @Test
    void matchesOracleOnRandomBipartiteGraphs() {
        for (long seed = 851; seed <= 1100; seed++) {
            FlowTestSupport support = new FlowTestSupport(seed);
            int leftCount = 1 + support.nextInt(4);
            int rightCount = 1 + support.nextInt(4);
            boolean[][] adjacency = new boolean[leftCount][rightCount];
            FlowGraph graph = new FlowGraph(leftCount + rightCount);
            int edges = support.nextInt(6);
            for (int e = 0; e < edges; e++) {
                int left = support.nextInt(leftCount);
                int right = support.nextInt(rightCount);
                adjacency[left][right] = true;
                graph.addEdge(left, right, 1);
            }
            int expected = FlowOracle.maxMatching(adjacency, leftCount, rightCount);
            MatchingResult result = matching.maxMatching(graph, leftCount, rightCount);
            assertEquals(expected, result.getMatchingSize(), "seed " + seed);
            assertValid(result);
        }
    }

    @Test
    void rejectsEdgesOnTheWrongSide() {
        FlowGraph graph = new FlowGraph(3);
        graph.addEdge(0, 2, 1);
        assertThrows(IllegalArgumentException.class, () -> matching.maxMatching(graph, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> matching.maxMatching(null, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> matching.maxMatching(new FlowGraph(1), -1, 1));
    }

    private static void assertValid(MatchingResult result) {
        int matched = 0;
        for (int left = 0; left < result.getLeftCount(); left++) {
            int right = result.partnerOfLeft(left);
            if (right != -1) {
                matched++;
                assertEquals(left, result.partnerOfRight(right), "partner maps are consistent");
            }
        }
        assertEquals(result.getMatchingSize(), matched, "size equals matched left vertices");
        assertEquals(result.getMatchingSize(), result.getMatchedPairs().length);
        assertTrue(result.getUnmatchedLeft().length
                == result.getLeftCount() - result.getMatchingSize());
        assertTrue(result.getUnmatchedRight().length
                == result.getRightCount() - result.getMatchingSize());
    }
}
