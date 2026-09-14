package com.loginsight.dsa.dp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Documents <em>when DP is the right tool and when it is not</em> (docs/02 §8, Phase 4 STEP 23).
 *
 * <p>These tests use small, self-contained reference implementations rather than the engine classes:
 * they exist to make the applicability boundary explicit and verifiable, not to add more DP API.</p>
 */
class DpApplicabilityTest {

    @Test
    void overlappingSubproblemsJustifyMemoization() {
        // Naive recursion is exponential; memoisation collapses it to O(n). Both must agree.
        for (int n = 0; n <= 25; n++) {
            assertEquals(naiveFib(n), memoFib(n), "fib(" + n + ")");
        }
    }

    @Test
    void greedyFailsWhereDpSucceedsForCoinChange() {
        int[] coins = {4, 3, 1};
        int amount = 6;
        int greedy = greedyCoins(coins, amount);
        int optimal = dpCoins(coins, amount);
        assertEquals(3, greedy, "greedy picks 4+1+1");
        assertEquals(2, optimal, "dp picks 3+3");
        assertTrue(greedy > optimal, "greedy is sub-optimal here, so DP is required");
    }

    @Test
    void knapsackDpMatchesBruteForce() {
        int[] weights = {2, 3, 4, 5, 9};
        int[] values = {3, 4, 5, 8, 10};
        int capacity = 20;
        assertEquals(bruteKnapsack(weights, values, capacity), dpKnapsack(weights, values, capacity));
    }

    private static long naiveFib(int n) {
        return n < 2 ? n : naiveFib(n - 1) + naiveFib(n - 2);
    }

    private static long memoFib(int n) {
        long[] memo = new long[n + 2];
        for (int i = 0; i < memo.length; i++) {
            memo[i] = -1;
        }
        return memoFib(n, memo);
    }

    private static long memoFib(int n, long[] memo) {
        if (n < 2) {
            return n;
        }
        if (memo[n] != -1) {
            return memo[n];
        }
        long value = memoFib(n - 1, memo) + memoFib(n - 2, memo);
        memo[n] = value;
        return value;
    }

    private static int greedyCoins(int[] coins, int amount) {
        int remaining = amount;
        int count = 0;
        for (int coin : coins) {
            while (remaining >= coin) {
                remaining -= coin;
                count++;
            }
        }
        return count;
    }

    private static int dpCoins(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        for (int i = 1; i <= amount; i++) {
            dp[i] = Integer.MAX_VALUE / 2;
            for (int coin : coins) {
                if (coin <= i) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }
        return dp[amount];
    }

    private static int dpKnapsack(int[] weights, int[] values, int capacity) {
        int[] dp = new int[capacity + 1];
        for (int i = 0; i < weights.length; i++) {
            for (int c = capacity; c >= weights[i]; c--) {
                dp[c] = Math.max(dp[c], dp[c - weights[i]] + values[i]);
            }
        }
        return dp[capacity];
    }

    private static int bruteKnapsack(int[] weights, int[] values, int capacity) {
        int best = 0;
        int subsets = 1 << weights.length;
        for (int mask = 0; mask < subsets; mask++) {
            int weight = 0;
            int value = 0;
            for (int i = 0; i < weights.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    weight += weights[i];
                    value += values[i];
                }
            }
            if (weight <= capacity && value > best) {
                best = value;
            }
        }
        return best;
    }
}
