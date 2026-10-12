package com.loginsight.dsa.dp.tree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.SmallRandom;
import org.junit.jupiter.api.Test;

class TreeCentroidTest {

    private final TreeCentroidDP centroid = new TreeCentroidDP();

    @Test
    void threeNodePathCentroidIsMiddle() {
        Tree tree = Tree.of(3, new int[][]{{0, 1}, {1, 2}});
        assertArrayEquals(new int[]{1}, centroid.centroids(tree));
    }

    @Test
    void fourNodePathHasTwoCentroids() {
        Tree tree = Tree.of(4, new int[][]{{0, 1}, {1, 2}, {2, 3}});
        assertArrayEquals(new int[]{1, 2}, centroid.centroids(tree));
    }

    @Test
    void starCentroidIsCentre() {
        Tree tree = Tree.of(5, new int[][]{{0, 1}, {0, 2}, {0, 3}, {0, 4}});
        assertArrayEquals(new int[]{0}, centroid.centroids(tree));
    }

    @Test
    void twoNodeTreeHasBothCentroids() {
        Tree tree = Tree.of(2, new int[][]{{0, 1}});
        assertArrayEquals(new int[]{0, 1}, centroid.centroids(tree));
    }

    @Test
    void singleNodeIsItsOwnCentroid() {
        assertArrayEquals(new int[]{0}, centroid.centroids(Tree.of(1, new int[0][])));
    }

    @Test
    void matchesBruteForceOnRandomTrees() {
        SmallRandom random = new SmallRandom(90210L);
        for (int trial = 0; trial < 40; trial++) {
            int n = 1 + random.nextInt(12);
            int[] from = new int[n - 1];
            int[] to = new int[n - 1];
            for (int i = 1; i < n; i++) {
                from[i - 1] = i;
                to[i - 1] = random.nextInt(i);
            }
            Tree tree = Tree.of(n, from, to);
            assertArrayEquals(bruteForce(tree), centroid.centroids(tree), "trial " + trial);
        }
    }

    @Test
    void envelopeView() {
        Tree tree = Tree.of(3, new int[][]{{0, 1}, {1, 2}});
        DpResult result = centroid.solve(tree);
        assertEquals("Tree centroid DP", result.getAlgorithm());
        assertEquals(3, result.getInputSize());
        assertEquals(1, result.resultAsLong());
        assertTrue(result.getIntermediateData() instanceof int[]);
    }

    private static int[] bruteForce(Tree tree) {
        int n = tree.size();
        int[] buffer = new int[n];
        int count = 0;
        for (int removed = 0; removed < n; removed++) {
            boolean[] visited = new boolean[n];
            visited[removed] = true;
            int worst = 0;
            for (int start = 0; start < n; start++) {
                if (visited[start]) {
                    continue;
                }
                int[] queue = new int[n];
                int head = 0;
                int tail = 0;
                queue[tail++] = start;
                visited[start] = true;
                int size = 1;
                while (head < tail) {
                    int u = queue[head++];
                    int d = tree.degree(u);
                    for (int k = 0; k < d; k++) {
                        int v = tree.neighbor(u, k);
                        if (!visited[v]) {
                            visited[v] = true;
                            size++;
                            queue[tail++] = v;
                        }
                    }
                }
                if (size > worst) {
                    worst = size;
                }
            }
            if (2 * worst <= n) {
                buffer[count++] = removed;
            }
        }
        int[] result = new int[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
    }
}
