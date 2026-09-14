package com.loginsight.dsa.dp.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.SmallRandom;
import org.junit.jupiter.api.Test;

class TreeSubsetSumTest {

    private final TreeSubsetSumDP subsetSum = new TreeSubsetSumDP();

    @Test
    void reachableAndUnreachableTargets() {
        Tree tree = Tree.of(3, new int[][]{{0, 1}, {1, 2}});
        long[] weights = {1, 2, 3};
        assertTrue(subsetSum.canAchieve(tree, weights, 3));
        assertTrue(subsetSum.canAchieve(tree, weights, 6));
        assertTrue(subsetSum.canAchieve(tree, weights, 1));
        assertTrue(!subsetSum.canAchieve(tree, weights, 5));
        assertTrue(!subsetSum.canAchieve(tree, weights, 0));
    }

    @Test
    void reconstructionIsValidAndSumsCorrectly() {
        Tree tree = Tree.of(3, new int[][]{{0, 1}, {1, 2}});
        long[] weights = {1, 2, 3};
        int[] selection = subsetSum.reconstruct(tree, weights, 3);
        assertNotNull(selection);
        assertValidSelection(tree, weights, selection, 3);
        assertNull(subsetSum.reconstruct(tree, weights, 5));
    }

    @Test
    void starSelection() {
        Tree tree = Tree.of(4, new int[][]{{0, 1}, {0, 2}, {0, 3}});
        long[] weights = {1, 1, 1, 1};
        int[] selection = subsetSum.reconstruct(tree, weights, 3);
        assertNotNull(selection);
        assertEquals(3, selection.length);
        assertValidSelection(tree, weights, selection, 3);
    }

    @Test
    void zeroWeightsPreferMinimalSelection() {
        Tree tree = Tree.of(3, new int[][]{{0, 1}, {1, 2}});
        long[] weights = {0, 0, 0};
        int[] selection = subsetSum.reconstruct(tree, weights, 0);
        assertNotNull(selection);
        assertEquals(1, selection.length);
        assertEquals(0, selection[0]);
    }

    @Test
    void matchesBruteForceOnRandomTrees() {
        SmallRandom random = new SmallRandom(606060L);
        for (int trial = 0; trial < 40; trial++) {
            int n = 1 + random.nextInt(8);
            int[] from = new int[n - 1];
            int[] to = new int[n - 1];
            long[] weights = new long[n];
            for (int i = 1; i < n; i++) {
                from[i - 1] = i;
                to[i - 1] = random.nextInt(i);
            }
            int total = 0;
            for (int i = 0; i < n; i++) {
                weights[i] = random.nextInt(4);
                total += weights[i];
            }
            Tree tree = Tree.of(n, from, to);
            boolean[] expected = bruteForce(tree, weights, total);
            for (int target = 0; target <= total; target++) {
                assertEquals(expected[target], subsetSum.canAchieve(tree, weights, target),
                        "trial " + trial + " target " + target);
            }
        }
    }

    @Test
    void envelopeView() {
        Tree tree = Tree.of(3, new int[][]{{0, 1}, {1, 2}});
        DpResult result = subsetSum.solve(tree, new long[]{1, 2, 3}, 3);
        assertEquals("Tree subset-sum DP", result.getAlgorithm());
        assertEquals(3, result.getInputSize());
        assertEquals(Boolean.TRUE, result.getResult());
    }

    @Test
    void rejectsInvalidInput() {
        Tree tree = Tree.of(3, new int[][]{{0, 1}, {1, 2}});
        assertThrows(IllegalArgumentException.class, () -> subsetSum.canAchieve(null, new long[]{1}, 1));
        assertThrows(IllegalArgumentException.class, () -> subsetSum.canAchieve(tree, null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> subsetSum.canAchieve(tree, new long[]{1, 2}, 1));
        assertThrows(IllegalArgumentException.class,
                () -> subsetSum.canAchieve(tree, new long[]{1, -1, 1}, 1));
        assertThrows(IllegalArgumentException.class,
                () -> subsetSum.canAchieve(tree, new long[]{1, 1, 1}, -1));
    }

    private static void assertValidSelection(Tree tree, long[] weights, int[] selection, long target) {
        int n = tree.size();
        int[] parent = parents(tree);
        boolean[] chosen = new boolean[n];
        long sum = 0;
        for (int node : selection) {
            assertTrue(node >= 0 && node < n, "node in range");
            assertTrue(!chosen[node], "node chosen twice: " + node);
            chosen[node] = true;
            sum += weights[node];
        }
        assertTrue(chosen[0], "selection must contain the root");
        for (int u = 1; u < n; u++) {
            if (chosen[u]) {
                assertTrue(chosen[parent[u]], "ancestor-closed: parent of " + u + " must be chosen");
            }
        }
        assertEquals(target, sum, "selection weight must equal target");
    }

    private static int[] parents(Tree tree) {
        int n = tree.size();
        int[] parent = new int[n];
        boolean[] visited = new boolean[n];
        int[] queue = new int[n];
        int head = 0;
        int tail = 0;
        queue[tail++] = 0;
        visited[0] = true;
        parent[0] = -1;
        while (head < tail) {
            int u = queue[head++];
            int d = tree.degree(u);
            for (int k = 0; k < d; k++) {
                int v = tree.neighbor(u, k);
                if (!visited[v]) {
                    visited[v] = true;
                    parent[v] = u;
                    queue[tail++] = v;
                }
            }
        }
        return parent;
    }

    private static boolean[] bruteForce(Tree tree, long[] weights, int total) {
        int n = tree.size();
        int[] parent = parents(tree);
        boolean[] possible = new boolean[total + 1];
        for (int mask = 0; mask < (1 << n); mask++) {
            if ((mask & 1) == 0) {
                continue;
            }
            boolean closed = true;
            for (int u = 1; u < n; u++) {
                if ((mask & (1 << u)) != 0 && (mask & (1 << parent[u])) == 0) {
                    closed = false;
                    break;
                }
            }
            if (!closed) {
                continue;
            }
            int sum = 0;
            for (int u = 0; u < n; u++) {
                if ((mask & (1 << u)) != 0) {
                    sum += weights[u];
                }
            }
            possible[sum] = true;
        }
        return possible;
    }
}
