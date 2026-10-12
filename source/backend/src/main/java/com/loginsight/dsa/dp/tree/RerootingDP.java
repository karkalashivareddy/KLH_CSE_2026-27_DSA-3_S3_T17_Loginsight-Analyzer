package com.loginsight.dsa.dp.tree;

import com.loginsight.dsa.dp.DpResult;

/**
 * Algorithm: rerooting DP — sum of distances from every node to all other nodes, in O(n).
 * <p>
 * Purpose: compute a per-node tree metric for <em>all</em> roots at once instead of rerunning an
 * O(n) traversal from each node (which would be O(n^2)). The Playground exposes the resulting array.
 * <p>
 * Input: a validated non-negatively weighted {@link Tree}.
 * <p>
 * Output: {@code answer[u]} = sum over all other nodes {@code v} of the distance {@code u..v}.
 * <p>
 * State (pass 1, rooted at node 0): {@code size[u]} = nodes in {@code u}'s subtree;
 * {@code down[u]} = sum of distances from {@code u} to every node in its subtree.
 * <p>
 * Base case: a leaf has {@code size = 1}, {@code down = 0}.
 * <p>
 * Recurrence / transition (pass 1, reverse BFS order):
 * <pre>
 *   size[parent] += size[u]
 *   down[parent] += down[u] + w(parent, u) * size[u]
 * </pre>
 * (each node in {@code u}'s subtree is one extra edge {@code w} further from the parent).
 * <p>
 * Rerooting (pass 2, forward BFS order) — move the root from {@code p} to child {@code c}:
 * <pre>
 *   answer[root] = down[root]
 *   answer[c] = answer[p] + w(p, c) * ( n - 2 * size[c] )
 * </pre>
 * Moving the root across the edge {@code p-c} decreases the distance to the {@code size[c]} nodes in
 * {@code c}'s subtree by {@code w} and increases it by {@code w} for the other {@code n - size[c]}
 * nodes. This single transition replaces recomputing the whole sum for {@code c}.
 * <p>
 * Final answer: the array {@code answer[0..n-1]}.
 * <p>
 * Why subproblems overlap: without rerooting, every node would redo a full subtree aggregation whose
 * pieces were already computed for its neighbours.
 * <p>
 * Time Complexity: O(n) — two passes. Space Complexity: O(n).
 * <p>
 * Overflow safety: sizes are {@code int}, distances and answer values are {@code long}.
 * <p>
 * Deterministic output: independent of adjacency order for the final values; node indices are fixed.
 */
public final class RerootingDP {

    private static final String ALGORITHM = "Rerooting DP";

    /** Sum of distances from each node to all other nodes. */
    public long[] sumDistances(Tree tree) {
        int n = tree.size();
        int[] order = new int[n];
        int[] parent = new int[n];
        long[] parentWeight = new long[n];
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
                    parentWeight[v] = tree.weight(u, k);
                    order[tail++] = v;
                }
            }
        }

        int[] size = new int[n];
        long[] down = new long[n];
        for (int i = 0; i < n; i++) {
            size[i] = 1;
        }
        for (int idx = n - 1; idx >= 1; idx--) {
            int u = order[idx];
            int p = parent[u];
            size[p] += size[u];
            down[p] += down[u] + parentWeight[u] * size[u];
        }

        long[] answer = new long[n];
        answer[0] = down[0];
        for (int idx = 1; idx < n; idx++) {
            int u = order[idx];
            int p = parent[u];
            answer[u] = answer[p] + parentWeight[u] * (n - 2L * size[u]);
        }
        return answer;
    }

    /** Envelope view; {@code result} = array total, {@code intermediateData} = the per-node array. */
    public DpResult solve(Tree tree) {
        long start = System.nanoTime();
        long[] answer = sumDistances(tree);
        long elapsed = System.nanoTime() - start;
        long total = 0;
        for (long value : answer) {
            total += value;
        }
        return new DpResult(ALGORITHM, tree.size(), total, elapsed, "O(n)", "O(n)", answer);
    }
}
