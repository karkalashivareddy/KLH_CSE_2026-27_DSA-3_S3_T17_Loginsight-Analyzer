package com.loginsight.dsa.dp.tree;

import com.loginsight.dsa.dp.DpResult;

/**
 * Algorithm: tree centroid(s) via subtree-size tree DP.
 * <p>
 * Purpose: find a node whose removal leaves no component larger than half the tree — the natural
 * "balance point" for divide-and-conquer over trees.
 * <p>
 * <strong>Definition (do not confuse with centre/median):</strong> a <em>centroid</em> is a node
 * {@code u} such that every connected component formed by deleting {@code u} has at most
 * {@code n/2} nodes. This is not the tree <em>centre</em> (midpoint of the diameter) and not a
 * weighted <em>median</em>. A tree has either one or two centroids; when two exist they are adjacent.
 * <p>
 * Input: a validated {@link Tree}.
 * <p>
 * Output: all centroids in ascending node order (one or two nodes).
 * <p>
 * State: {@code size[u]} = number of nodes in the rooted subtree of {@code u} (root = node 0).
 * <p>
 * Base case: a leaf has {@code size = 1}.
 * <p>
 * Recurrence / transition: process nodes in reverse BFS order, adding each child's size into its
 * parent: {@code size[parent] += size[branch]}. Then for every node {@code u}, the largest component
 * after removing {@code u} is
 * <pre>
 *   worst(u) = max( n - size[u],  max over children c of size[c] )
 * </pre>
 * and {@code u} is a centroid iff {@code 2 * worst(u) <= n}.
 * <p>
 * Final answer: every node satisfying the centroid inequality.
 * <p>
 * Why subproblems overlap: each subtree size is used both by the parent's size and by the centroid
 * test at several ancestors.
 * <p>
 * Time Complexity: O(n). Space Complexity: O(n).
 * <p>
 * Deterministic output: nodes are tested in ascending index order, so the returned set is stable.
 */
public final class TreeCentroidDP {

    private static final String ALGORITHM = "Tree centroid DP";

    public int[] centroids(Tree tree) {
        int n = tree.size();
        int[] order = new int[n];
        int[] parent = new int[n];
        boolean[] visited = new boolean[n];
        int head = 0;
        int tail = 0;
        order[tail++] = 0;
        visited[0] = true;
        parent[0] = -1;
        while (head < tail) {
            int u = order[head++];
            for (int k = 0; k < tree.degree(u); k++) {
                int v = tree.neighbor(u, k);
                if (!visited[v]) {
                    visited[v] = true;
                    parent[v] = u;
                    order[tail++] = v;
                }
            }
        }

        int[] size = new int[n];
        for (int i = 0; i < n; i++) {
            size[i] = 1;
        }
        for (int idx = n - 1; idx >= 1; idx--) {
            int u = order[idx];
            size[parent[u]] += size[u];
        }

        int count = 0;
        int[] buffer = new int[n];
        for (int u = 0; u < n; u++) {
            int worst = n - size[u];
            for (int k = 0; k < tree.degree(u); k++) {
                int v = tree.neighbor(u, k);
                if (parent[v] == u && size[v] > worst) {
                    worst = size[v];
                }
            }
            if (2 * worst <= n) {
                buffer[count++] = u;
            }
        }
        int[] result = new int[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
    }

    /** Envelope view; {@code result} = number of centroids, {@code intermediateData} = the nodes. */
    public DpResult solve(Tree tree) {
        long start = System.nanoTime();
        int[] centroids = centroids(tree);
        long elapsed = System.nanoTime() - start;
        return new DpResult(ALGORITHM, tree.size(), centroids.length, elapsed, "O(n)", "O(n)",
                centroids);
    }
}
