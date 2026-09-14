package com.loginsight.dsa.dp.interval;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: Matrix-chain multiplication order optimisation (interval DP).
 * <p>
 * Purpose: choose the cheapest way to parenthesise a chain of matrices; a teaching demo for interval
 * DP (Playground). The matrices themselves are never multiplied — only the scalar-multiplication
 * count is optimised.
 * <p>
 * Input: dimensions {@code p[0..n]} where matrix {@code i} (1-based) has size
 * {@code p[i-1] x p[i]}. So there are {@code n = p.length - 1} matrices.
 * <p>
 * Output: the minimum number of scalar multiplications and an optimal parenthesisation.
 * <p>
 * State: {@code m[i][j]} = minimum cost to multiply matrices {@code i..j} (1-based).
 * <p>
 * Base case: {@code m[i][i] = 0} — a single matrix needs no multiplication.
 * <p>
 * Recurrence / transition (over interval length):
 * <pre>
 *   m[i][j] = min over k in [i, j-1] of ( m[i][k] + m[k+1][j] + p[i-1] * p[k] * p[j] )
 *   split[i][j] = the k that achieves the minimum (smallest k on ties)
 * </pre>
 * <p>
 * Final answer: {@code m[1][n]}, with {@code split} used to print the parenthesisation.
 * <p>
 * Why subproblems overlap: a sub-chain {@code i..j} is a term in the cost of every larger interval
 * that contains it, so its cost is reused many times.
 * <p>
 * Time Complexity: O(n^3). Space Complexity: O(n^2).
 * <p>
 * Deterministic tie-breaking: iterate {@code k} ascending and keep the strictly smaller cost, so the
 * smallest optimal split wins.
 * <p>
 * Overflow safety: costs and the {@code p[i-1]*p[k]*p[j]} term are computed in {@code long}.
 */
public final class MatrixChainMultiplication {

    private static final String ALGORITHM = "Matrix-chain multiplication";

    /** Minimum scalar-multiplication count. */
    public long minCost(int[] p) {
        return computeCost(p)[1][matrixCount(p)];
    }

    /** Full cost table {@code m[i][j]} (1-based; index 0 unused). */
    public long[][] computeCost(int[] p) {
        DpValidator.requireNonNull((Object) p);
        int n = matrixCount(p);
        long[][] m = new long[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            m[i][i] = 0;
        }
        for (int length = 2; length <= n; length++) {
            for (int i = 1; i + length - 1 <= n; i++) {
                int j = i + length - 1;
                long best = Long.MAX_VALUE;
                for (int k = i; k < j; k++) {
                    long candidate = m[i][k] + m[k + 1][j] + (long) p[i - 1] * p[k] * p[j];
                    if (candidate < best) {
                        best = candidate;
                    }
                }
                m[i][j] = best;
            }
        }
        return m;
    }

    /** Split table: {@code split[i][j]} is the optimal first cut of matrices {@code i..j}. */
    public int[][] splitTable(int[] p) {
        DpValidator.requireNonNull((Object) p);
        int n = matrixCount(p);
        int[][] split = new int[n + 1][n + 1];
        long[][] m = new long[n + 1][n + 1];
        for (int length = 2; length <= n; length++) {
            for (int i = 1; i + length - 1 <= n; i++) {
                int j = i + length - 1;
                long best = Long.MAX_VALUE;
                int bestSplit = i;
                for (int k = i; k < j; k++) {
                    long candidate = m[i][k] + m[k + 1][j] + (long) p[i - 1] * p[k] * p[j];
                    if (candidate < best) {
                        best = candidate;
                        bestSplit = k;
                    }
                }
                m[i][j] = best;
                split[i][j] = bestSplit;
            }
        }
        return split;
    }

    /** Optimal parenthesisation, e.g. "((A1 x A2) x A3)". */
    public String parenthesization(int[] p) {
        int n = matrixCount(p);
        if (n == 1) {
            return "A1";
        }
        int[][] split = splitTable(p);
        StringBuilder out = new StringBuilder();
        buildParenthesization(split, 1, n, out);
        return out.toString();
    }

    private static void buildParenthesization(int[][] split, int i, int j, StringBuilder out) {
        if (i == j) {
            out.append('A').append(i);
            return;
        }
        out.append('(');
        buildParenthesization(split, i, split[i][j], out);
        out.append(" x ");
        buildParenthesization(split, split[i][j] + 1, j, out);
        out.append(')');
    }

    /** Envelope view; {@code intermediateData} = the split table used to print the parenthesisation. */
    public DpResult solve(int[] p) {
        DpValidator.requireNonNull((Object) p);
        int n = matrixCount(p);
        long start = System.nanoTime();
        long cost = minCost(p);
        int[][] split = splitTable(p);
        long elapsed = System.nanoTime() - start;
        return new DpResult(ALGORITHM, n, cost, elapsed, "O(n^3)", "O(n^2)", split);
    }

    private static int matrixCount(int[] p) {
        if (p.length < 2) {
            throw new IllegalArgumentException(
                    "dimensions must contain at least two values (one matrix) but had " + p.length);
        }
        for (int i = 0; i < p.length; i++) {
            DpValidator.requirePositive(p[i], "p[" + i + "]");
        }
        return p.length - 1;
    }
}
