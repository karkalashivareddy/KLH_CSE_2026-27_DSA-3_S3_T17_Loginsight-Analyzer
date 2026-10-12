package com.loginsight.dsa.dp.editdistance;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: Damerau-Levenshtein edit distance — <strong>Optimal String Alignment (OSA)</strong>
 * variant, sometimes called the "restricted" Damerau-Levenshtein distance.
 * <p>
 * <strong>Exact variant implemented (do not confuse with unrestricted DL):</strong> OSA adds the
 * adjacent-transposition edit to Levenshtein but allows each substring to be edited at most once.
 * The O(n·m) recurrence below is the textbook OSA recurrence. The <em>unrestricted</em>
 * Damerau-Levenshtein distance (true metric, where a substring may be edited more than once) is a
 * different algorithm; e.g. {@code "CA" -> "ABC"} is 3 under OSA but 2 under unrestricted DL. This
 * class implements OSA only and never mixes the two.
 * <p>
 * Purpose: typo tolerance that also recognises swapped adjacent letters (a very common log typo,
 * e.g. "erorr" vs "error").
 * <p>
 * Input: source {@code a} (length n), target {@code b} (length m).
 * <p>
 * Output: minimum number of insertions, deletions, substitutions and adjacent transpositions.
 * <p>
 * State: {@code dp[i][j]} = OSA distance between prefixes {@code a[0..i)} and {@code b[0..j)}.
 * <p>
 * Base case: {@code dp[i][0] = i}, {@code dp[0][j] = j}.
 * <p>
 * Recurrence / transition:
 * <pre>
 *   dp[i][j] = min( dp[i-1][j] + 1,
 *                   dp[i][j-1] + 1,
 *                   dp[i-1][j-1] + (a[i-1] == b[j-1] ? 0 : 1),
 *                   dp[i-2][j-2] + 1   when a[i-1] == b[j-2] && a[i-2] == b[j-1] )   // transpose
 * </pre>
 * <p>
 * The transposition term is only valid for {@code i > 1 && j > 1}; it costs 1 (a single adjacent
 * swap), which is why {@code "CA" -> "AC"} is 1 here versus 2 under plain Levenshtein.
 * <p>
 * Final answer: {@code dp[n][m]}.
 * <p>
 * Why subproblems overlap: same prefix-overlap argument as Levenshtein, now with an extra
 * length-2 lookback; tabulation still visits each {@code (i, j)} once.
 * <p>
 * Time Complexity: O(n·m). Space Complexity: O(n·m).
 * <p>
 * Deterministic tie-breaking: prefer MATCH, then TRANSPOSE, then SUBSTITUTE, then DELETE, then
 * INSERT.
 * <p>
 * Character model: UTF-16 {@code char} units.
 */
public final class DamerauLevenshteinDistance {

    private static final String ALGORITHM = "Damerau-Levenshtein (OSA)";

    /** Human-readable identification of the implemented variant, for the viva and the docs. */
    public static String variant() {
        return "Optimal String Alignment (restricted Damerau-Levenshtein), O(n*m)";
    }

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
                long best = Math.min(substitute, Math.min(delete, insert));
                if (i > 1 && j > 1 && x[i - 1] == y[j - 2] && x[i - 2] == y[j - 1]) {
                    best = Math.min(best, dp[i - 2][j - 2] + 1);
                }
                dp[i][j] = best;
            }
        }
        return dp;
    }

    public long distance(String a, String b) {
        return buildMatrix(a, b)[a.length()][b.length()];
    }

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
            } else if (i > 1 && j > 1 && x[i - 1] == y[j - 2] && x[i - 2] == y[j - 1]
                    && dp[i][j] == dp[i - 2][j - 2] + 1) {
                reversed[count++] = EditDistanceResult.TRANSPOSE;
                i -= 2;
                j -= 2;
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
