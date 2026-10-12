package com.loginsight.dsa.dp.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.SmallRandom;
import org.junit.jupiter.api.Test;

class TreeDpTest {

    private final TreeDiameterDP diameter = new TreeDiameterDP();
    private final RerootingDP rerooting = new RerootingDP();

    @Test
    void diameterOfPath() {
        Tree tree = Tree.of(5, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}});
        assertEquals(4, diameter.diameter(tree));
        assertEndpoints(diameter.endpoints(tree), 0, 4);
    }

    @Test
    void diameterOfStar() {
        Tree tree = Tree.of(4, new int[][]{{0, 1}, {0, 2}, {0, 3}});
        assertEquals(2, diameter.diameter(tree));
        assertEndpoints(diameter.endpoints(tree), 1, 2);
    }

    @Test
    void diameterOfWeightedTree() {
        Tree tree = Tree.weighted(4, new int[]{0, 1, 1}, new int[]{1, 2, 3},
                new long[]{5, 3, 4});
        assertEquals(9, diameter.diameter(tree));
        assertEndpoints(diameter.endpoints(tree), 0, 3);
    }

    @Test
    void singleNodeDiameter() {
        Tree tree = Tree.of(1, new int[0][]);
        assertEquals(0, diameter.diameter(tree));
        assertEndpoints(diameter.endpoints(tree), 0, 0);
    }

    @Test
    void diameterEnvelopeView() {
        Tree tree = Tree.of(3, new int[][]{{0, 1}, {1, 2}});
        DpResult result = diameter.solve(tree);
        assertEquals("Tree diameter DP", result.getAlgorithm());
        assertEquals(3, result.getInputSize());
        assertEquals(2, result.resultAsLong());
        assertTrue(result.getIntermediateData() instanceof int[]);
    }

    @Test
    void sumDistancesOnPath() {
        Tree tree = Tree.of(4, new int[][]{{0, 1}, {1, 2}, {2, 3}});
        assertArrayEqualsLong(new long[]{6, 4, 4, 6}, rerooting.sumDistances(tree));
    }

    @Test
    void sumDistancesOnStar() {
        Tree tree = Tree.of(4, new int[][]{{0, 1}, {0, 2}, {0, 3}});
        assertArrayEqualsLong(new long[]{3, 5, 5, 5}, rerooting.sumDistances(tree));
    }

    @Test
    void sumDistancesWeightedPath() {
        Tree tree = Tree.weighted(3, new int[]{0, 1}, new int[]{1, 2}, new long[]{2, 3});
        assertArrayEqualsLong(new long[]{7, 5, 8}, rerooting.sumDistances(tree));
    }

    @Test
    void singleNodeSumIsZero() {
        assertArrayEqualsLong(new long[]{0}, rerooting.sumDistances(Tree.of(1, new int[0][])));
    }

    @Test
    void rerootingMatchesNaiveOnRandomTrees() {
        SmallRandom random = new SmallRandom(555666L);
        for (int trial = 0; trial < 30; trial++) {
            int n = 1 + random.nextInt(12);
            int[] from = new int[n - 1];
            int[] to = new int[n - 1];
            long[] weights = new long[n - 1];
            for (int i = 1; i < n; i++) {
                from[i - 1] = i;
                to[i - 1] = random.nextInt(i);
                weights[i - 1] = 1 + random.nextInt(10);
            }
            Tree tree = Tree.weighted(n, from, to, weights);
            assertArrayEqualsLong(naiveSumDistances(tree), rerooting.sumDistances(tree));
        }
    }

    @Test
    void rerootingEnvelopeView() {
        Tree tree = Tree.of(4, new int[][]{{0, 1}, {1, 2}, {2, 3}});
        DpResult result = rerooting.solve(tree);
        assertEquals("Rerooting DP", result.getAlgorithm());
        assertEquals(4, result.getInputSize());
        assertEquals(6 + 4 + 4 + 6, result.resultAsLong());
    }

    @Test
    void treeBuildersAndAccessors() {
        Tree tree = Tree.of(4, new int[]{0, 1, 1}, new int[]{1, 2, 3});
        assertEquals(4, tree.size());
        assertEquals(1, tree.degree(0));
        assertEquals(3, tree.degree(1));
        assertEquals(1, tree.neighbor(0, 0));
        assertEquals(1L, tree.weight(1, 0));
        assertEquals(3, tree.neighbors(1).length);
    }

    @Test
    void rejectsInvalidTrees() {
        assertThrows(IllegalArgumentException.class, () -> Tree.of(0, new int[0][]));
        assertThrows(IllegalArgumentException.class, () -> Tree.of(3, new int[][]{{0, 1}}));
        assertThrows(IllegalArgumentException.class, () -> Tree.of(3, new int[][]{{0, 0}, {0, 1}}));
        assertThrows(IllegalArgumentException.class,
                () -> Tree.of(3, new int[][]{{0, 1}, {0, 1}}));
        assertThrows(IllegalArgumentException.class,
                () -> Tree.of(4, new int[][]{{0, 1}, {1, 2}, {0, 2}}));
        assertThrows(IllegalArgumentException.class,
                () -> Tree.of(3, new int[][]{{0, 1}, {1, 5}}));
        assertThrows(IllegalArgumentException.class,
                () -> Tree.weighted(3, new int[]{0, 1}, new int[]{1, 2}, new long[]{1, -1}));
        assertThrows(IllegalArgumentException.class, () -> Tree.of(2, (int[][]) null));
        assertThrows(IllegalArgumentException.class, () -> Tree.of(2, new int[]{0}, new int[]{1, 2}));
    }

    private static void assertEndpoints(int[] actual, int a, int b) {
        boolean direct = actual[0] == a && actual[1] == b;
        boolean swapped = actual[0] == b && actual[1] == a;
        assertTrue(direct || swapped, "expected endpoints {" + a + ", " + b + "} but got {"
                + actual[0] + ", " + actual[1] + "}");
    }

    private static void assertArrayEqualsLong(long[] expected, long[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "index " + i);
        }
    }

    private static long[] naiveSumDistances(Tree tree) {
        int n = tree.size();
        long[] result = new long[n];
        for (int source = 0; source < n; source++) {
            long[] dist = new long[n];
            boolean[] visited = new boolean[n];
            int[] queue = new int[n];
            int head = 0;
            int tail = 0;
            queue[tail++] = source;
            visited[source] = true;
            while (head < tail) {
                int u = queue[head++];
                int d = tree.degree(u);
                for (int k = 0; k < d; k++) {
                    int v = tree.neighbor(u, k);
                    if (!visited[v]) {
                        visited[v] = true;
                        dist[v] = dist[u] + tree.weight(u, k);
                        queue[tail++] = v;
                    }
                }
            }
            for (long value : dist) {
                result[source] += value;
            }
        }
        return result;
    }
}
