package com.loginsight.dsa.dp.interval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.SmallRandom;
import org.junit.jupiter.api.Test;

class OptimalBinarySearchTreeTest {

    private final OptimalBinarySearchTree obst = new OptimalBinarySearchTree();

    @Test
    void classicThreeKeyExample() {
        long[] freq = {34, 8, 50};
        assertEquals(142, obst.optimalCost(freq));
        assertEquals(3, obst.rootTable(freq)[1][3]);
    }

    @Test
    void singleKey() {
        assertEquals(10, obst.optimalCost(new long[]{10}));
        assertEquals(1, obst.rootTable(new long[]{10})[1][1]);
    }

    @Test
    void allZeroFrequencies() {
        assertEquals(0, obst.optimalCost(new long[]{0, 0, 0}));
    }

    @Test
    void balancedFrequencies() {
        // Frequencies {1,1,1}: any shape costs 1*(depth+1)+... ; best root is middle key 2.
        long[] freq = {1, 1, 1};
        assertEquals(5, obst.optimalCost(freq));
        assertEquals(2, obst.rootTable(freq)[1][3]);
    }

    @Test
    void matchesExhaustiveSearch() {
        SmallRandom random = new SmallRandom(424242L);
        for (int trial = 0; trial < 40; trial++) {
            int n = 1 + random.nextInt(6);
            long[] freq = new long[n];
            for (int i = 0; i < n; i++) {
                freq[i] = random.nextInt(50);
            }
            assertEquals(bruteForce(freq, 0, n - 1, 0), obst.optimalCost(freq),
                    "mismatch for trial " + trial);
        }
    }

    @Test
    void envelopeView() {
        DpResult result = obst.solve(new long[]{34, 8, 50});
        assertEquals("Optimal binary search tree", result.getAlgorithm());
        assertEquals(3, result.getInputSize());
        assertEquals(142, result.resultAsLong());
        assertEquals("O(n^3)", result.getTimeComplexity());
        assertTrue(result.getIntermediateData() instanceof int[][]);
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> obst.optimalCost(null));
        assertThrows(IllegalArgumentException.class, () -> obst.optimalCost(new long[0]));
        assertThrows(IllegalArgumentException.class, () -> obst.optimalCost(new long[]{5, -1}));
    }

    private static long bruteForce(long[] freq, int lo, int hi, int depth) {
        if (lo > hi) {
            return 0;
        }
        long best = Long.MAX_VALUE;
        for (int r = lo; r <= hi; r++) {
            long candidate = freq[r] * (depth + 1)
                    + bruteForce(freq, lo, r - 1, depth + 1)
                    + bruteForce(freq, r + 1, hi, depth + 1);
            if (candidate < best) {
                best = candidate;
            }
        }
        return best;
    }
}
