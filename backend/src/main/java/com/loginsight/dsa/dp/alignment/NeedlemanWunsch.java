package com.loginsight.dsa.dp.alignment;

import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: Needleman-Wunsch — <strong>global</strong> sequence alignment via dynamic programming.
 * <p>
 * Purpose: align two entire sequences (e.g. two service traces {@code AUTH-USER-DB-...}), scoring
 * matches, mismatches and gaps, so the whole of each sequence is accounted for.
 * <p>
 * Input: sequence A (length n) and sequence B (length m) as token arrays, plus scores
 * {@code match >= 0}, {@code mismatch <= 0} and {@code gap <= 0}. Convenience overloads accept plain
 * {@code String}s and treat each UTF-16 char as a one-token sequence.
 * <p>
 * Output: the optimal global alignment score plus the two aligned token arrays (gaps = "-").
 * <p>
 * State: {@code dp[i][j]} = best score aligning the prefixes {@code A[0..i)} and {@code B[0..j)}.
 * <p>
 * Base case: {@code dp[i][0] = i * gap} and {@code dp[0][j] = j * gap} — aligning an empty prefix to a
 * non-empty one forces {@code i} (resp. {@code j}) gaps.
 * <p>
 * Recurrence / transition:
 * <pre>
 *   dp[i][j] = max( dp[i-1][j-1] + s(A[i-1], B[j-1]),   // align the two tokens (match/mismatch)
 *                   dp[i-1][j] + gap,                   // token A[i-1] against a gap in B
 *                   dp[i][j-1] + gap )                  // gap in A against token B[j-1]
 *   where s(a, b) = a.equals(b) ? match : mismatch
 * </pre>
 * <p>
 * Final answer: {@code dp[n][m]}; the alignment is recovered by traceback from {@code (n, m)}.
 * <p>
 * Global vs local: this algorithm aligns the <em>entire</em> sequences (global); contrast
 * {@link SmithWaterman}, which finds the best <em>local</em> region and can ignore the rest.
 * <p>
 * Why subproblems overlap: every prefix pair is reused by many alignments, so tabulation avoids the
 * exponential recursion tree.
 * <p>
 * Time Complexity: O(n·m). Space Complexity: O(n·m).
 * <p>
 * Deterministic tie-breaking: on equal scores prefer the diagonal (match/mismatch), then the "up"
 * move (gap in B), then the "left" move (gap in A). This makes the returned alignment reproducible.
 * <p>
 * Overflow safety: scores and the matrix are {@code long}; all arithmetic is {@code long}.
 */
public final class NeedlemanWunsch {

    private static final String ALGORITHM = "Needleman-Wunsch";

    /** Align two token sequences with explicit scores. */
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
        for (int i = 0; i <= n; i++) {
            dp[i][0] = (long) i * gap;
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = (long) j * gap;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                long diagonal = dp[i - 1][j - 1] + (a[i - 1].equals(b[j - 1]) ? match : mismatch);
                long up = dp[i - 1][j] + gap;
                long left = dp[i][j - 1] + gap;
                dp[i][j] = Math.max(diagonal, Math.max(up, left));
            }
        }

        String[] reversedA = new String[n + m];
        String[] reversedB = new String[n + m];
        int count = 0;
        int i = n;
        int j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0
                    && dp[i][j] == dp[i - 1][j - 1] + (a[i - 1].equals(b[j - 1]) ? match : mismatch)) {
                reversedA[count] = a[i - 1];
                reversedB[count] = b[j - 1];
                count++;
                i--;
                j--;
            } else if (i > 0 && dp[i][j] == dp[i - 1][j] + gap) {
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
        return new AlignmentResult(ALGORITHM, dp[n][m], alignedA, alignedB, dp, 0, 0, n, m);
    }

    /** Convenience: character sequences with default scores match = 1, mismatch = -1, gap = -1. */
    public AlignmentResult align(String a, String b) {
        return align(a, b, 1, -1, -1);
    }

    /** Convenience: character sequences with explicit scores. */
    public AlignmentResult align(String a, String b, int match, int mismatch, int gap) {
        DpValidator.requireNonNull(a, b);
        return align(toTokens(a), toTokens(b), match, mismatch, gap);
    }

    static String[] toTokens(String s) {
        char[] chars = s.toCharArray();
        String[] tokens = new String[chars.length];
        for (int i = 0; i < chars.length; i++) {
            tokens[i] = String.valueOf(chars[i]);
        }
        return tokens;
    }
}
