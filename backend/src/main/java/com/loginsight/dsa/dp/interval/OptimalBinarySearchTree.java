package com.loginsight.dsa.dp.interval;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: Optimal Binary Search Tree (interval DP).
 * <p>
 * Purpose: build the BST over a sorted key set that minimises the expected number of comparisons for
 * a known access distribution — a classic interval-DP problem.
 * <p>
 * <strong>Cost model (exact definition):</strong> keys {@code 1..n} are given in sorted order with
 * non-negative integer search frequencies {@code freq[0..n-1]} (successful searches only; unsuccessful
 * searches are not modelled). If key {@code i} sits at depth {@code depth(i)} (root depth 0), the
 * weighted search cost is {@code sum over i of freq[i] * (depth(i) + 1)} — the depth plus the node
 * itself, matching "number of nodes examined on a successful search". The DP minimises this quantity.
 * This is the standard CLRS-style OBST restricted to successful searches.
 * <p>
 * Input: sorted frequencies {@code freq[0..n-1]}, each {@code >= 0}.
 * <p>
 * Output: minimum weighted search cost and the root table (from which the tree can be rebuilt).
 * <p>
 * State: {@code e[i][j]} = minimum cost of a BST containing keys {@code i..j} (1-based);
 * {@code root[i][j]} = index of the optimal root of that subtree.
 * <p>
 * Base case: {@code e[i][i-1] = 0} for the empty key range.
 * <p>
 * Recurrence / transition:
 * <pre>
 *   w(i, j) = sum of freq[i..j]                        (prefix sums, O(1))
 *   e[i][j] = w(i, j) + min over r in [i, j] of ( e[i][r-1] + e[r+1][j] )
 *   root[i][j] = the r achieving the minimum (smallest r on ties)
 * </pre>
 * <p>
 * Final answer: {@code e[1][n]}.
 * <p>
 * Why subproblems overlap: every key range {@code i..j} is a subproblem of many larger ranges, so its
 * optimal cost is recomputed by naive recursion but tabulated once here.
 * <p>
 * Time Complexity: O(n^3). Space Complexity: O(n^2). (The Knuth O(n^2) optimisation is intentionally
 * not used; the educational cubic form is the documented one.)
 * <p>
 * Deterministic tie-breaking: iterate {@code r} ascending and keep the strictly smaller cost, so the
 * smallest optimal root wins and the root table is reproducible.
 * <p>
 * Overflow safety: costs and frequencies are {@code long}.
 */
public final class OptimalBinarySearchTree {

    private static final String ALGORITHM = "Optimal binary search tree";

    /** Minimum weighted search cost. */
    public long optimalCost(long[] freq) {
        return compute(freq).cost[1][freq.length];
    }

    /** Root table {@code root[i][j]} (1-based) for keys {@code i..j}; row 0 unused. */
    public int[][] rootTable(long[] freq) {
        return compute(freq).root;
    }

    /** Envelope view; {@code intermediateData} = the root table. */
    public DpResult solve(long[] freq) {
        DpValidator.requireNonNull((Object) freq);
        long start = System.nanoTime();
        Tables tables = compute(freq);
        long elapsed = System.nanoTime() - start;
        return new DpResult(ALGORITHM, freq.length, tables.cost[1][freq.length], elapsed,
                "O(n^3)", "O(n^2)", tables.root);
    }

    private static Tables compute(long[] freq) {
        DpValidator.requireNonNull((Object) freq);
        int n = freq.length;
        if (n == 0) {
            throw new IllegalArgumentException("frequencies must contain at least one key");
        }
        for (int i = 0; i < n; i++) {
            DpValidator.requireNonNegative(freq[i], "freq[" + i + "]");
        }
        long[] prefix = new long[n + 1];
        for (int i = 1; i <= n; i++) {
            prefix[i] = prefix[i - 1] + freq[i - 1];
        }
        long[][] e = new long[n + 2][n + 1];
        int[][] root = new int[n + 1][n + 1];
        for (int i = 1; i <= n + 1; i++) {
            e[i][i - 1] = 0;
        }
        for (int length = 1; length <= n; length++) {
            for (int i = 1; i + length - 1 <= n; i++) {
                int j = i + length - 1;
                long weight = prefix[j] - prefix[i - 1];
                long best = Long.MAX_VALUE;
                int bestRoot = i;
                for (int r = i; r <= j; r++) {
                    long candidate = e[i][r - 1] + e[r + 1][j];
                    if (candidate < best) {
                        best = candidate;
                        bestRoot = r;
                    }
                }
                e[i][j] = weight + best;
                root[i][j] = bestRoot;
            }
        }
        return new Tables(e, root);
    }

    private static final class Tables {
        final long[][] cost;
        final int[][] root;

        Tables(long[][] cost, int[][] root) {
            this.cost = cost;
            this.root = root;
        }
    }
}
