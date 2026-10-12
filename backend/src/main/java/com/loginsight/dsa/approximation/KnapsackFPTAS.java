package com.loginsight.dsa.approximation;

/**
 * Algorithm: <strong>0/1 Knapsack FPTAS</strong> by value scaling.
 *
 * <h2>Problem (maximization, Module 5)</h2>
 * Choose a subset of {@code n} items (weight {@code w_i > 0}, value {@code v_i >= 0}) of total weight
 * {@code <= capacity} maximizing total value. Exact knapsack is NP-hard (weakly: pseudo-polynomial).
 *
 * <h2>Why ordinary value-DP is pseudo-polynomial</h2>
 * The classic DP is over the total value dimension: {@code O(n * sum v)} time and space. Because
 * {@code sum v} is only polynomial in the <em>numbers</em> (bit length), not in {@code n}, the DP is
 * not polynomial — it is pseudo-polynomial (polynomial in {@code n} and {@code V}).
 *
 * <h2>Scaling transformation</h2>
 * Pick scale {@code K = eps * P / n} where {@code P = max_i v_i}. Replace each value with the scaled
 * value {@code v'_i = floor(v_i / K)} and run the value-DP on {@code (w, v')}. The scaled optimal
 * value {@code OPT'} is close to {@code OPT / K}:
 * {@code OPT' >= sum_{i in OPT} floor(v_i / K) >= OPT/K - n}, and the DP's answer {@code A}
 * satisfies {@code A >= K * OPT'} because it is optimal in the scaled problem.
 *
 * <h2>Approximation reasoning (the guarantee)</h2>
 * Working through the inequalities gives {@code A >= OPT - K*n = OPT - eps*P >= OPT - eps*OPT}, i.e.
 * <strong>{@code A >= (1 - eps) * OPT}</strong>. The step {@code P <= OPT} is what forces the
 * implementation to first <em>filter out items heavier than the capacity</em> (they can never be part
 * of an optimum): {@code P} is then the maximum feasible value and scaling against it preserves the
 * guarantee instead of rounding every feasible item to zero. Runtime is bounded by the scaled value
 * budget {@code sum v'_i <= n * P / K = n^2 / eps}: polynomial in {@code n} and {@code 1/eps} — an FPTAS for
 * 0/1 knapsack. (The textbook argument uses real {@code K}; this implementation computes {@code K} in
 * {@code double} and floors with {@code (long)(v / K)}, which only rounds scaled values down, so the
 * lower bound still holds. Tests verify {@code A >= (1-eps) OPT} against an independent exact DP.)
 *
 * <h2>State</h2>
 * {@code long[] minWeight[sv]} = smallest original weight achieving scaled value {@code sv}, plus
 * predecessor arrays to reconstruct the item selection.
 *
 * <h2>Complexity</h2>
 * Time {@code O(n * sum v') <= O(n^3 / eps)}, space {@code O(sum v') <= O(n^2 / eps)}.
 *
 * <h2>Edge cases / validation</h2>
 * Empty input → empty result; capacity too small for any item → empty result (valid, since
 * {@code OPT = 0}); zero-value items carry no scaled value and are harmless; weights must be positive,
 * values non-negative, {@code 0 < eps < 1}, lengths matched — otherwise {@link IllegalArgumentException}.
 */
public final class KnapsackFPTAS {

    private static final long INFINITY = Long.MAX_VALUE / 4;

    public KnapsackResult approximate(long[] weights, long[] values, long capacity, double eps) {
        if (weights == null || values == null) {
            throw new IllegalArgumentException("weights and values must not be null");
        }
        if (weights.length != values.length) {
            throw new IllegalArgumentException("weights and values must have equal length");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0 but was " + capacity);
        }
        if (Double.isNaN(eps) || eps <= 0.0 || eps >= 1.0) {
            throw new IllegalArgumentException("eps must satisfy 0 < eps < 1 but was " + eps);
        }
        long start = System.nanoTime();
        int n = weights.length;
        for (int i = 0; i < n; i++) {
            if (weights[i] <= 0) {
                throw new IllegalArgumentException("weights must be positive but weight[" + i + "]="
                        + weights[i]);
            }
            if (values[i] < 0) {
                throw new IllegalArgumentException("values must be non-negative but value[" + i + "]="
                        + values[i]);
            }
        }
        String guarantee = "A >= (1 - eps) * OPT, with K = eps*P/n, P = max feasible value; FPTAS"
                + " (polynomial in n and 1/eps)";
        long[] feasibleWeights = new long[n];
        long[] feasibleValues = new long[n];
        int[] originalIndex = new int[n];
        int m = 0;
        for (int i = 0; i < n; i++) {
            if (weights[i] <= capacity) {
                feasibleWeights[m] = weights[i];
                feasibleValues[m] = values[i];
                originalIndex[m] = i;
                m++;
            }
        }
        if (m == 0) {
            long elapsedZero = System.nanoTime() - start;
            return new KnapsackResult(weights, values, capacity, eps, new int[0], 0, 0, 0,
                    guarantee, elapsedZero, "O(1)", "O(1)");
        }
        long maxValue = 0;
        for (int i = 0; i < m; i++) {
            if (feasibleValues[i] > maxValue) {
                maxValue = feasibleValues[i];
            }
        }
        if (maxValue == 0) {
            long elapsedZero = System.nanoTime() - start;
            return new KnapsackResult(weights, values, capacity, eps, new int[0], 0, 0, 0,
                    guarantee, elapsedZero, "O(1)", "O(1)");
        }

        // Items heavier than the capacity can never be selected (OPT never contains them), so they are
        // filtered out before scaling. This keeps P <= OPT — required for A >= OPT - eps*P to become
        // the advertised A >= (1-eps)*OPT; scaling against an oversized P would give the guarantee
        // relative to nothing (feasible items would all round to zero scaled value).
        double k = eps * maxValue / m;
        long[] scaled = new long[m];
        long scaledBudget = 0;
        for (int i = 0; i < m; i++) {
            scaled[i] = (long) (feasibleValues[i] / k);
            scaledBudget += scaled[i];
        }
        if (scaledBudget > Integer.MAX_VALUE / 2) {
            throw new IllegalArgumentException("scaled value budget " + scaledBudget
                    + " exceeds the addressable table size; reduce n or increase eps");
        }
        int budget = (int) scaledBudget;
        // The 1D predecessor chain is not safe for weight-minimization reconstruction: when a
        // later item overwrites an earlier state's weight AND predecessor, the chain from a
        // downstream state may retrace through the later item a second time.  A 2D table
        // (item × scaled value) preserves the correct per-item decision at every state and
        // lets us reconstruct in O(m) time by walking backwards through the rows.
        long[][] minW = new long[m + 1][budget + 1];
        boolean[][] reach = new boolean[m + 1][budget + 1];
        reach[0][0] = true;
        for (int sv = 1; sv <= budget; sv++) {
            minW[0][sv] = INFINITY;
        }
        for (int i = 0; i < m; i++) {
            for (int sv = 0; sv <= budget; sv++) {
                minW[i + 1][sv] = minW[i][sv];
                reach[i + 1][sv] = reach[i][sv];
            }
            if (scaled[i] == 0) {
                continue;
            }
            for (int sv = budget; sv >= scaled[i]; sv--) {
                int previous = sv - (int) scaled[i];
                if (!reach[i][previous]) {
                    continue;
                }
                long candidate = minW[i][previous] + feasibleWeights[i];
                if (candidate <= capacity && candidate < minW[i + 1][sv]) {
                    minW[i + 1][sv] = candidate;
                    reach[i + 1][sv] = true;
                }
            }
        }
        int best = budget;
        while (best > 0 && !reach[m][best]) {
            best--;
        }
        int[] selected = new int[m];
        int count = 0;
        int sv = best;
        for (int i = m - 1; i >= 0 && sv > 0; i--) {
            int previous = sv - (int) scaled[i];
            if (previous >= 0 && reach[i][previous]
                    && minW[i][previous] + feasibleWeights[i] == minW[i + 1][sv]) {
                selected[count++] = originalIndex[i];
                sv = previous;
            }
        }
        int[] items = new int[count];
        System.arraycopy(selected, 0, items, 0, count);
        long totalValue = 0;
        long totalWeight = 0;
        for (int item : items) {
            totalValue += values[item];
            totalWeight += weights[item];
        }
        long elapsed = System.nanoTime() - start;
        return new KnapsackResult(weights, values, capacity, eps, items, totalValue, totalWeight,
                best, guarantee, elapsed, "O(n^3 / eps)", "O(n^2 / eps)");
    }
}