package com.loginsight.dsa.dp.editdistance;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: Weighted edit distance (Wagner-Fischer with configurable operation costs).
 * <p>
 * Purpose: show that edit distance is a family parameterised by costs, not tied to unit costs —
 * e.g. a deletion may be cheap while a substitution is expensive (Playground demo).
 * <p>
 * Input: source {@code a} (length n), target {@code b} (length m), and non-negative integer costs
 * {@code insertCost}, {@code deleteCost}, {@code substituteCost}. A match always costs 0.
 * <p>
 * Output: the minimum total cost to transform {@code a} into {@code b}.
 * <p>
 * State: {@code dp[i][j]} = minimum weighted cost between prefixes {@code a[0..i)} and
 * {@code b[0..j)}.
 * <p>
 * Base case: {@code dp[i][0] = i * deleteCost} (delete the i source chars);
 * {@code dp[0][j] = j * insertCost} (insert the j target chars).
 * <p>
 * Recurrence / transition:
 * <pre>
 *   dp[i][j] = min( dp[i-1][j]   + deleteCost,                         // delete a[i-1]
 *                   dp[i][j-1]   + insertCost,                         // insert b[j-1]
 *                   dp[i-1][j-1] + (a[i-1] == b[j-1] ? 0 : substituteCost) )
 * </pre>
 * <p>
 * Final answer: {@code dp[n][m]}.
 * <p>
 * Why subproblems overlap: prefix-pair states are shared by many edit sequences.
 * <p>
 * Time Complexity: O(n·m). Space Complexity: O(n·m).
 * <p>
 * Overflow safety: all costs are {@code long} and all DP values are {@code long}; costs are required
 * to be non-negative so no {@code Integer.MAX_VALUE + x} sentinel is ever needed.
 * <p>
 * Cost contract: {@code insertCost >= 0}, {@code deleteCost >= 0}, {@code substituteCost >= 0};
 * rejected otherwise. A substitution may be more expensive than a delete+insert pair — the recurrence
 * naturally prefers whichever is cheaper. Transposition is intentionally not part of this model
 * (that is {@link DamerauLevenshteinDistance}).
 * <p>
 * Deterministic tie-breaking: prefer MATCH, then SUBSTITUTE, then DELETE, then INSERT.
 * <p>
 * Character model: UTF-16 {@code char} units.
 */
public final class WeightedEditDistance {

    private static final String ALGORITHM = "Weighted edit distance";

    private final long insertCost;
    private final long deleteCost;
    private final long substituteCost;

    public WeightedEditDistance(int insertCost, int deleteCost, int substituteCost) {
        DpValidator.requireNonNegative(insertCost, "insertCost");
        DpValidator.requireNonNegative(deleteCost, "deleteCost");
        DpValidator.requireNonNegative(substituteCost, "substituteCost");
        this.insertCost = insertCost;
        this.deleteCost = deleteCost;
        this.substituteCost = substituteCost;
    }

    public long getInsertCost() {
        return insertCost;
    }

    public long getDeleteCost() {
        return deleteCost;
    }

    public long getSubstituteCost() {
        return substituteCost;
    }

    public long[][] buildMatrix(String a, String b) {
        DpValidator.requireNonNull(a, b);
        char[] x = a.toCharArray();
        char[] y = b.toCharArray();
        int n = x.length;
        int m = y.length;
        long[][] dp = new long[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            dp[i][0] = i * deleteCost;
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j * insertCost;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                long substitute = dp[i - 1][j - 1] + (x[i - 1] == y[j - 1] ? 0 : substituteCost);
                long delete = dp[i - 1][j] + deleteCost;
                long insert = dp[i][j - 1] + insertCost;
                dp[i][j] = Math.min(substitute, Math.min(delete, insert));
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
            } else if (i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + substituteCost) {
                reversed[count++] = EditDistanceResult.SUBSTITUTE;
                i--;
                j--;
            } else if (i > 0 && dp[i][j] == dp[i - 1][j] + deleteCost) {
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
