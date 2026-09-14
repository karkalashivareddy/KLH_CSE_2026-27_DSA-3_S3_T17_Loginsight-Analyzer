package com.loginsight.dsa.dp.interval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.SmallRandom;
import org.junit.jupiter.api.Test;

class MatrixChainTest {

    private final MatrixChainMultiplication chain = new MatrixChainMultiplication();

    @Test
    void classicFourMatrixExample() {
        int[] p = {10, 30, 5, 60};
        assertEquals(4500, chain.minCost(p));
        assertEquals("((A1 x A2) x A3)", chain.parenthesization(p));
    }

    @Test
    void smallExample() {
        assertEquals(18, chain.minCost(new int[]{1, 2, 3, 4}));
    }

    @Test
    void singleMatrixNeedsNoMultiplication() {
        assertEquals(0, chain.minCost(new int[]{5, 10}));
        assertEquals("A1", chain.parenthesization(new int[]{5, 10}));
    }

    @Test
    void twoMatrices() {
        assertEquals(10 * 30 * 5, chain.minCost(new int[]{10, 30, 5}));
        assertEquals("(A1 x A2)", chain.parenthesization(new int[]{10, 30, 5}));
    }

    @Test
    void costTableDiagonalIsZero() {
        long[][] m = chain.computeCost(new int[]{10, 30, 5, 60});
        for (int i = 1; i <= 3; i++) {
            assertEquals(0, m[i][i]);
        }
        assertEquals(4500, m[1][3]);
    }

    @Test
    void splitTableMatchesKnownSplit() {
        int[][] split = chain.splitTable(new int[]{10, 30, 5, 60});
        assertEquals(1, split[1][2]);
        assertEquals(2, split[2][3]);
        assertEquals(2, split[1][3]);
    }

    @Test
    void matchesExhaustiveParenthesization() {
        SmallRandom random = new SmallRandom(20260914L);
        for (int trial = 0; trial < 40; trial++) {
            int matrices = 1 + random.nextInt(6);
            int[] p = new int[matrices + 1];
            for (int i = 0; i < p.length; i++) {
                p[i] = 1 + random.nextInt(50);
            }
            assertEquals(bruteForce(p, 1, matrices), chain.minCost(p),
                    "mismatch for trial " + trial);
        }
    }

    @Test
    void envelopeView() {
        DpResult result = chain.solve(new int[]{10, 30, 5, 60});
        assertEquals("Matrix-chain multiplication", result.getAlgorithm());
        assertEquals(3, result.getInputSize());
        assertEquals(4500, result.resultAsLong());
        assertEquals("O(n^3)", result.getTimeComplexity());
        assertTrue(result.getIntermediateData() instanceof int[][]);
    }

    @Test
    void rejectsInvalidDimensions() {
        assertThrows(IllegalArgumentException.class, () -> chain.minCost(null));
        assertThrows(IllegalArgumentException.class, () -> chain.minCost(new int[]{5}));
        assertThrows(IllegalArgumentException.class, () -> chain.minCost(new int[]{10, 0, 5}));
        assertThrows(IllegalArgumentException.class, () -> chain.minCost(new int[]{10, -3, 5}));
        assertThrows(IllegalArgumentException.class, () -> chain.parenthesization(new int[]{5}));
    }

    private static long bruteForce(int[] p, int i, int j) {
        if (i == j) {
            return 0;
        }
        long best = Long.MAX_VALUE;
        for (int k = i; k < j; k++) {
            long candidate = bruteForce(p, i, k) + bruteForce(p, k + 1, j)
                    + (long) p[i - 1] * p[k] * p[j];
            if (candidate < best) {
                best = candidate;
            }
        }
        return best;
    }
}
