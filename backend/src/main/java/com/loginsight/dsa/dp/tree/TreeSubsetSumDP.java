package com.loginsight.dsa.dp.tree;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: tree subset-sum via tree knapsack DP (ancestor-closed subtree selection).
 * <p>
 * <strong>Problem definition (unambiguous, viva-ready):</strong> the tree is rooted at node 0. A set
 * {@code S} of nodes is <em>valid</em> iff (i) {@code 0 in S}, and (ii) for every {@code u in S} with
 * {@code u != 0}, the parent of {@code u} is also in {@code S}. In other words, {@code S} is a
 * connected "rooted subtree": you may include a node only if its parent is included. Given
 * non-negative node weights and a target {@code T}, decide whether some valid {@code S} has total
 * weight exactly {@code T}, and reconstruct one if so.
 * <p>
 * (This is deliberately <em>not</em> an arbitrary subset: arbitrary subset-sum ignores the tree and is
 * not a tree DP. Requiring the selection to be downward-closed from the root is what makes the tree
 * structure — and the DP — meaningful.)
 * <p>
 * Input: a validated {@link Tree} (root = node 0), {@code weights[u] >= 0}, {@code target >= 0}.
 * <p>
 * Output: existence, plus a chosen node set when a solution exists.
 * <p>
 * State: {@code reach[u][s]} = true iff there is a valid selection inside {@code u}'s subtree that
 * contains {@code u} and has total weight exactly {@code s} ({@code 0 <= s <= T}).
 * <p>
 * Base case: {@code reach[u][weights[u]] = true} (select just {@code u}) when
 * {@code weights[u] <= T}; otherwise the whole row is false.
 * <p>
 * Recurrence / transition (merge children left to right):
 * <pre>
 *   start with reach[u] = { weights[u] }
 *   for each child c:
 *       next = reach[u]                                  // child c excluded (contributes 0)
 *       for every s with reach[u][s]:
 *           for every t > 0 with reach[c][t]:
 *               if s + t <= T then next[s + t] = true    // child c included with subtree weight t
 *       reach[u] = next
 * </pre>
 * <p>
 * Final answer: {@code reach[0][T]}.
 * <p>
 * Why subproblems overlap: each child's reachable-weight set is combined into its parent and reused
 * across all of the parent's own ancestor contexts; recomputing it per selection would repeat work.
 * <p>
 * Time Complexity: O(n · T^2) in the worst case (bounded by reachable sums and by capping at T); with
 * subtree-size-bounded loops it is O(n · T) in the common case. Space Complexity: O(n · T) for all
 * rows, O(T) per node row at a time if streamed.
 * <p>
 * Deterministic reconstruction: children are visited in adjacency order; during backtracking the
 * "child excluded" option is preferred whenever it still admits a completion, so the returned set is
 * reproducible (and tends to be small).
 * <p>
 * Overflow safety: weights and the running sums are {@code long}; sums never exceed {@code T}, so the
 * {@code target + 1} boolean row is a safe bound.
 */
public final class TreeSubsetSumDP {

    private static final String ALGORITHM = "Tree subset-sum DP";

    /** Whether a valid ancestor-closed selection of the given weights sums to {@code target}. */
    public boolean canAchieve(Tree tree, long[] weights, long target) {
        return buildReach(tree, weights, target)[0][(int) target];
    }

    /**
     * Reconstruct one valid ancestor-closed selection with total weight {@code target}, or {@code null}
     * when none exists. The returned array contains node indices in ascending order.
     */
    public int[] reconstruct(Tree tree, long[] weights, long target) {
        checkArguments(tree, weights, target);
        boolean[][] reach = buildReach(tree, weights, target);
        int t = (int) target;
        if (!reach[0][t]) {
            return null;
        }
        int n = tree.size();
        int[] parent = parents(tree);
        int[] selection = new int[n];
        int[] count = new int[]{0};
        reconstructNode(0, t, tree, weights, reach, parent, selection, count);
        int[] result = new int[count[0]];
        System.arraycopy(selection, 0, result, 0, count[0]);
        sortAscending(result);
        return result;
    }

    private void reconstructNode(int u, int s, Tree tree, long[] weights, boolean[][] reach,
                                 int[] parent, int[] selection, int[] count) {
        selection[count[0]++] = u;
        int remaining = s - (int) weights[u];
        int[] children = childrenOf(tree, u, parent);
        if (children.length == 0) {
            return;
        }
        boolean[][] can = new boolean[children.length + 1][remaining + 1];
        can[0][0] = true;
        for (int k = 0; k < children.length; k++) {
            int c = children[k];
            for (int v = 0; v <= remaining; v++) {
                if (!can[k][v]) {
                    continue;
                }
                can[k + 1][v] = true;
                for (int t = 1; t + v <= remaining; t++) {
                    if (reach[c][t]) {
                        can[k + 1][v + t] = true;
                    }
                }
            }
        }
        int v = remaining;
        for (int k = children.length - 1; k >= 0; k--) {
            int c = children[k];
            if (can[k][v]) {
                continue; // prefer excluding child c
            }
            for (int t = 1; t <= v; t++) {
                if (reach[c][t] && can[k][v - t]) {
                    reconstructNode(c, t, tree, weights, reach, parent, selection, count);
                    v -= t;
                    break;
                }
            }
        }
    }

    private boolean[][] buildReach(Tree tree, long[] weights, long target) {
        checkArguments(tree, weights, target);
        int n = tree.size();
        int t = (int) target;
        int[] parent = parents(tree);
        int[] order = bfsOrder(tree);
        boolean[][] reach = new boolean[n][];
        for (int idx = n - 1; idx >= 0; idx--) {
            int u = order[idx];
            boolean[] current = new boolean[t + 1];
            if (weights[u] <= target) {
                current[(int) weights[u]] = true;
            }
            for (int k = 0; k < tree.degree(u); k++) {
                int c = tree.neighbor(u, k);
                if (parent[c] != u) {
                    continue;
                }
                boolean[] next = current.clone();
                for (int s = 0; s <= t; s++) {
                    if (!current[s]) {
                        continue;
                    }
                    for (int childSum = 1; s + childSum <= t; childSum++) {
                        if (reach[c][childSum]) {
                            next[s + childSum] = true;
                        }
                    }
                }
                current = next;
            }
            reach[u] = current;
        }
        return reach;
    }

    private static void checkArguments(Tree tree, long[] weights, long target) {
        DpValidator.requireNonNull(tree, (Object) weights);
        if (weights.length != tree.size()) {
            throw new IllegalArgumentException("weights length " + weights.length
                    + " does not match tree size " + tree.size());
        }
        for (int i = 0; i < weights.length; i++) {
            DpValidator.requireNonNegative(weights[i], "weights[" + i + "]");
        }
        DpValidator.requireNonNegative(target, "target");
    }

    private static int[] parents(Tree tree) {
        int n = tree.size();
        int[] parent = new int[n];
        int[] order = new int[n];
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
        return parent;
    }

    private static int[] bfsOrder(Tree tree) {
        int n = tree.size();
        int[] order = new int[n];
        boolean[] visited = new boolean[n];
        int head = 0;
        int tail = 0;
        order[tail++] = 0;
        visited[0] = true;
        while (head < tail) {
            int u = order[head++];
            for (int k = 0; k < tree.degree(u); k++) {
                int v = tree.neighbor(u, k);
                if (!visited[v]) {
                    visited[v] = true;
                    order[tail++] = v;
                }
            }
        }
        return order;
    }

    private static int[] childrenOf(Tree tree, int u, int[] parent) {
        int count = 0;
        for (int k = 0; k < tree.degree(u); k++) {
            if (parent[tree.neighbor(u, k)] == u) {
                count++;
            }
        }
        int[] children = new int[count];
        int index = 0;
        for (int k = 0; k < tree.degree(u); k++) {
            int v = tree.neighbor(u, k);
            if (parent[v] == u) {
                children[index++] = v;
            }
        }
        return children;
    }

    private static void sortAscending(int[] values) {
        for (int i = 1; i < values.length; i++) {
            int key = values[i];
            int j = i - 1;
            while (j >= 0 && values[j] > key) {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = key;
        }
    }

    /** Envelope view; {@code result} = existence (Boolean), {@code intermediateData} = chosen set. */
    public DpResult solve(Tree tree, long[] weights, long target) {
        DpValidator.requireNonNull(tree, (Object) weights);
        long start = System.nanoTime();
        int[] selection = reconstruct(tree, weights, target);
        long elapsed = System.nanoTime() - start;
        return new DpResult(ALGORITHM, tree.size(), selection != null, elapsed,
                "O(n * T^2)", "O(n * T)", selection);
    }
}
