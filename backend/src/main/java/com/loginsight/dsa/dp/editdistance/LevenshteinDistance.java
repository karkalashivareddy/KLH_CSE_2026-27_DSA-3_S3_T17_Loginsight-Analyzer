package com.loginsight.dsa.dp.editdistance;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: Levenshtein edit distance via the Wagner-Fischer dynamic program.
 * <p>
 * Purpose: the canonical "how different are two strings" metric, used for typo-tolerant log search
 * (SimLab / Playground).
 * <p>
 * Input: source string {@code a} (length n) and target string {@code b} (length m). Unit costs:
 * insertion = deletion = substitution = 1, match = 0.
 * <p>
 * Output: the minimum number of single-character insertions, deletions or substitutions turning
 * {@code a} into {@code b}; optionally the full matrix and a reconstructed edit script.
 * <p>
 * State: {@code dp[i][j]} = Levenshtein distance between the prefixes {@code a[0..i)} and
 * {@code b[0..j)}.
 * <p>
 * Base case: {@code dp[i][0] = i} (delete all of the first i source chars) and {@code dp[0][j] = j}
 * (insert all of the first j target chars).
 * <p>
 * Recurrence / transition:
 * <pre>
 *   dp[i][j] = min( dp[i-1][j]   + 1,                                  // delete a[i-1]
 *                   dp[i][j-1]   + 1,                                  // insert b[j-1]
 *                   dp[i-1][j-1] + (a[i-1] == b[j-1] ? 0 : 1) )       // match / substitute
 * </pre>
 * <p>
 * Final answer: {@code dp[n][m]}.
 * <p>
 * Why subproblems overlap: the prefixes {@code (i-1, j)}, {@code (i, j-1)} and {@code (i-1, j-1)} are
 * each reachable through multiple edit sequences, so plain recursion recomputes them exponentially;
 * tabulating each {@code (i, j)} once removes the overlap.
 * <p>
 * Time Complexity: O(n·m) — every cell is filled once. Space Complexity: O(n·m) for the full matrix
 * (the canonical teaching version). {@link #distanceTwoRow(String, String)} provides the O(m)
 * two-row variant separately; it does not replace this version.
 * <p>
 * Deterministic tie-breaking (for reconstruction): at cell {@code (i, j)} prefer MATCH, then
 * SUBSTITUTE, then DELETE, then INSERT. This yields the same script for equal-cost alternatives on
 * every run.
 * <p>
 * Character model: UTF-16 {@code char} units, consistent with the Phase 3 string engine.
 */
public final class LevenshteinDistance {

    private static final String ALGORITHM = "Levenshtein";

    /** Full Wagner-Fischer matrix; also used by the other edit-distance variants for structure. */
    public long[][] buildMatrix(String a, String b) {
        DpValidator.requireNonNull(a, b);
        char[] x = a.toCharArray();
        char[] y = b.toCharArray();
        int n = x.length;
        int m = y.length;
        long[][] dp = new long[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                long substitute = dp[i - 1][j - 1] + (x[i - 1] == y[j - 1] ? 0 : 1);
                long delete = dp[i - 1][j] + 1;
                long insert = dp[i][j - 1] + 1;
                dp[i][j] = Math.min(substitute, Math.min(delete, insert));
            }
        }
        return dp;
    }

    public long distance(String a, String b) {
        return buildMatrix(a, b)[a.length()][b.length()];
    }

    /**
     * Memory-optimised variant: O(m) auxiliary space using two rolling rows. It computes the same
     * distance as {@link #distance(String, String)} but cannot reconstruct the operations.
     */
    public long distanceTwoRow(String a, String b) {
        DpValidator.requireNonNull(a, b);
        char[] x = a.toCharArray();
        char[] y = b.toCharArray();
        int n = x.length;
        int m = y.length;
        long[] previous = new long[m + 1];
        long[] current = new long[m + 1];
        for (int j = 0; j <= m; j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            current[0] = i;
            for (int j = 1; j <= m; j++) {
                long substitute = previous[j - 1] + (x[i - 1] == y[j - 1] ? 0 : 1);
                long delete = previous[j] + 1;
                long insert = current[j - 1] + 1;
                current[j] = Math.min(substitute, Math.min(delete, insert));
            }
            long[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[m];
    }

    /** Reconstruct the edit script by walking the matrix from {@code (n, m)} back to {@code (0, 0)}. */
    public EditDistanceResult reconstruct(String a, String b) {
        long[][] dp = buildMatrix(a, b);
        char[] x = a.toCharArray();
        char[] y = b.toCharArray();
        int i = x.length;
        int j = y.length;
        String[] reversed = new String[x.length + y.length + 1];
        int count = 0;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && x[i - 1] == y[j - 1] && dp[i][j] == dp[i - 1][j - 1]) {
                reversed[count++] = EditDistanceResult.MATCH;
                i--;
                j--;
            } else if (i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + 1) {
                reversed[count++] = EditDistanceResult.SUBSTITUTE;
                i--;
                j--;
            } else if (i > 0 && dp[i][j] == dp[i - 1][j] + 1) {
                reversed[count++] = EditDistanceResult.DELETE;
                i--;
            } else {
                reversed[count++] = EditDistanceResult.INSERT;
                j--;
            }
        }
        String[] operations = new String[count];
        for (int k = 0; k < count; k++) {
            operations[k] = reversed[count - 1 - k];
        }
        return new EditDistanceResult(ALGORITHM, dp[x.length][y.length], dp, operations);
    }

    /** Envelope view for the query layer; the matrix is exposed as intermediate data. */
    public DpResult match(String a, String b) {
        DpValidator.requireNonNull(a, b);
        long start = System.nanoTime();
        long[][] dp = buildMatrix(a, b);
        long distance = dp[a.length()][b.length()];
        long elapsed = System.nanoTime() - start;
        return new DpResult(ALGORITHM, a.length() * b.length(), distance, elapsed,
                "O(n * m)", "O(n * m)", dp);
    }
}
