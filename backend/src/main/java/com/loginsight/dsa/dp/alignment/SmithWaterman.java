package com.loginsight.dsa.dp.alignment;

import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: Smith-Waterman — <strong>local</strong> sequence alignment via dynamic programming.
 * <p>
 * Purpose: find the best-matching <em>region</em> shared by two sequences (e.g. the common motif
 * {@code USER-DB-PAYMENT}) without being penalised for unrelated leading/trailing material.
 * <p>
 * Input: sequence A (length n), sequence B (length m) as token arrays, plus scores
 * {@code match >= 0}, {@code mismatch <= 0} and {@code gap <= 0}. Convenience overloads accept plain
 * {@code String}s (one token per UTF-16 char).
 * <p>
 * Output: the highest local alignment score plus the two aligned core token arrays (gaps = "-") and
 * the half-open intervals inside A and B that the core covers.
 * <p>
 * State: {@code dp[i][j]} = best local alignment score ending at {@code A[i-1]} / {@code B[j-1]}.
 * <p>
 * Base case: the entire first row and column are {@code 0} — a local alignment may start anywhere.
 * <p>
 * Recurrence / transition:
 * <pre>
 *   dp[i][j] = max( 0,                                  // RESET: start a fresh local alignment here
 *                   dp[i-1][j-1] + s(A[i-1], B[j-1]),   // extend the diagonal
 *                   dp[i-1][j] + gap,                   // extend with a gap in B
 *                   dp[i][j-1] + gap )                  // extend with a gap in A
 * </pre>
 * <p>
 * The {@code max(0, ...)} clamp is the defining difference from global alignment: a negative-running
 * score is abandoned rather than carried.
 * <p>
 * Final answer: the maximum value over the whole matrix; traceback runs from that cell until it
 * reaches a zero cell (the start of the local alignment).
 * <p>
 * Global vs local: Needleman-Wunsch aligns the full sequences and can be dragged down by unrelated
 * ends; Smith-Waterman isolates the best local region and is the right tool for motif discovery.
 * <p>
 * Why subproblems overlap: same prefix-pair reuse as Needleman-Wunsch.
 * <p>
 * Time Complexity: O(n·m). Space Complexity: O(n·m).
 * <p>
 * Deterministic tie-breaking: the best cell is the first maximum found scanning rows then columns
 * (smallest i, then smallest j); during traceback prefer the diagonal, then "up", then "left".
 * <p>
 * Overflow safety: scores and the matrix are {@code long}.
 */
public final class SmithWaterman {

    private static final String ALGORITHM = "Smith-Waterman";

    public AlignmentResult align(String[] a, String[] b, int match, int mismatch, int gap) {
        DpValidator.requireNonNull(a, b);
        DpValidator.requireNonNegative(match, "match");
        if (mismatch > 0) {
            throw new IllegalArgumentException("mismatch must be <= 0 but was " + mismatch);
        }
        if (gap > 0) {
            throw new IllegalArgumentException("gap must be <= 0 but was " + gap);
        }
        int n = a.length;
        int m = b.length;
        long[][] dp = new long[n + 1][m + 1];
        long bestScore = 0;
        int bestI = 0;
        int bestJ = 0;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                long diagonal = dp[i - 1][j - 1] + (a[i - 1].equals(b[j - 1]) ? match : mismatch);
                long up = dp[i - 1][j] + gap;
                long left = dp[i][j - 1] + gap;
                long value = Math.max(0, Math.max(diagonal, Math.max(up, left)));
                dp[i][j] = value;
                if (value > bestScore) {
                    bestScore = value;
                    bestI = i;
                    bestJ = j;
                }
            }
        }

        String[] reversedA = new String[n + m];
        String[] reversedB = new String[n + m];
        int count = 0;
        int i = bestI;
        int j = bestJ;
        while (i > 0 && j > 0 && dp[i][j] > 0) {
            if (dp[i][j] == dp[i - 1][j - 1] + (a[i - 1].equals(b[j - 1]) ? match : mismatch)) {
                reversedA[count] = a[i - 1];
                reversedB[count] = b[j - 1];
                count++;
                i--;
                j--;
            } else if (dp[i][j] == dp[i - 1][j] + gap) {
                reversedA[count] = a[i - 1];
                reversedB[count] = AlignmentResult.GAP;
                count++;
                i--;
            } else {
                reversedA[count] = AlignmentResult.GAP;
                reversedB[count] = b[j - 1];
                count++;
                j--;
            }
        }
        String[] alignedA = new String[count];
        String[] alignedB = new String[count];
        for (int k = 0; k < count; k++) {
            alignedA[k] = reversedA[count - 1 - k];
            alignedB[k] = reversedB[count - 1 - k];
        }
        return new AlignmentResult(ALGORITHM, bestScore, alignedA, alignedB, dp, i, j, bestI, bestJ);
    }

    /** Convenience: character sequences with default scores match = 2, mismatch = -1, gap = -2. */
    public AlignmentResult align(String a, String b) {
        return align(a, b, 2, -1, -2);
    }

    /** Convenience: character sequences with explicit scores. */
    public AlignmentResult align(String a, String b, int match, int mismatch, int gap) {
        DpValidator.requireNonNull(a, b);
        return align(NeedlemanWunsch.toTokens(a), NeedlemanWunsch.toTokens(b), match, mismatch, gap);
    }
}
