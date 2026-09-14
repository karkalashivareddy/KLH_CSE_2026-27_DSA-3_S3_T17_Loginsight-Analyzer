package com.loginsight.dsa.dp.tree;

import com.loginsight.dsa.dp.DpResult;

/**
 * Algorithm: tree diameter via tree DP (two largest downward depths per node).
 * <p>
 * Purpose: find the longest path (in edge-weight terms) between any two nodes of a tree — the classic
 * tree-DP demonstration (Playground).
 * <p>
 * Input: a validated {@link Tree} (unweighted or non-negatively weighted).
 * <p>
 * Output: the diameter length and its two endpoints.
 * <p>
 * State (per node {@code u}, computed bottom-up): {@code down[u]} = the maximum distance from {@code u}
 * down to a leaf of its rooted subtree, and {@code deepestLeaf[u]} = a leaf achieving it. During the
 * same pass we keep the best two child branches through {@code u}.
 * <p>
 * Base case: a leaf has {@code down = 0} and its deepest "leaf" is itself.
 * <p>
 * Recurrence / transition: root the tree anywhere (node 0); process nodes in reverse BFS order so
 * every child is done before its parent. For each child {@code c} of {@code u}, the branch length is
 * {@code down[c] + w(u, c)}; keep the two largest branches, {@code top1 + top2}. Then
 * <pre>
 *   down[u] = top1
 *   diameter through u = top1 + top2   (or top1 alone when u has a single branch)
 * </pre>
 * <p>
 * Final answer: the maximum "diameter through u" over all nodes; the endpoints are the deepest leaves
 * of the two branches (or {@code u} and the deepest leaf when only one branch exists).
 * <p>
 * Why subproblems overlap: a node's deepest leaf is reused by its parent's computation, so computing
 * "longest path" independently for every node would redundantly explore whole subtrees.
 * <p>
 * Time Complexity: O(n) — each edge is examined a constant number of times. Space Complexity: O(n).
 * <p>
 * Deterministic tie-breaking: children are scanned in adjacency order and the strictly larger branch
 * wins, so equal-length diameters resolve to the first branch encountered.
 * <p>
 * Iterative implementation (no recursion) so long chains cannot overflow the call stack.
 */
public final class TreeDiameterDP {

    private static final String ALGORITHM = "Tree diameter DP";

    /** Diameter length only. */
    public long diameter(Tree tree) {
        return compute(tree).length;
    }

    /** The two endpoints of a longest path (both equal for a single-node tree). */
    public int[] endpoints(Tree tree) {
        Computation c = compute(tree);
        return new int[]{c.u, c.v};
    }

    /** Envelope view; {@code result} = diameter, {@code intermediateData} = {@code int[]{u, v}}. */
    public DpResult solve(Tree tree) {
        long start = System.nanoTime();
        Computation c = compute(tree);
        long elapsed = System.nanoTime() - start;
        return new DpResult(ALGORITHM, tree.size(), c.length, elapsed, "O(n)", "O(n)",
                new int[]{c.u, c.v});
    }

    private static Computation compute(Tree tree) {
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

        long[] down = new long[n];
        int[] deepestLeaf = new int[n];
        long bestLength = 0;
        int bestU = 0;
        int bestV = 0;
        for (int idx = n - 1; idx >= 0; idx--) {
            int u = order[idx];
            long top1 = Long.MIN_VALUE;
            long top2 = Long.MIN_VALUE;
            int leaf1 = -1;
            int leaf2 = -1;
            for (int k = 0; k < tree.degree(u); k++) {
                int v = tree.neighbor(u, k);
                if (parent[v] != u) {
                    continue;
                }
                long branch = down[v] + tree.weight(u, k);
                if (branch > top1) {
                    top2 = top1;
                    leaf2 = leaf1;
                    top1 = branch;
                    leaf1 = deepestLeaf[v];
                } else if (branch > top2) {
                    top2 = branch;
                    leaf2 = deepestLeaf[v];
                }
            }
            int u1;
            int u2;
            long through;
            if (leaf1 == -1) {
                down[u] = 0;
                deepestLeaf[u] = u;
                through = 0;
                u1 = u;
                u2 = u;
            } else {
                down[u] = top1;
                deepestLeaf[u] = leaf1;
                through = top1 + (leaf2 != -1 ? top2 : 0);
                u1 = leaf1;
                u2 = leaf2 != -1 ? leaf2 : u;
            }
            if (through > bestLength) {
                bestLength = through;
                bestU = u1;
                bestV = u2;
            }
        }
        return new Computation(bestLength, bestU, bestV);
    }

    private static final class Computation {
        final long length;
        final int u;
        final int v;

        Computation(long length, int u, int v) {
            this.length = length;
            this.u = u;
            this.v = v;
        }
    }
}
