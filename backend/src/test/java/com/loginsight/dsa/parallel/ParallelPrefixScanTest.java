package com.loginsight.dsa.parallel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loginsight.dsa.randomized.RandomSource;

import org.junit.jupiter.api.Test;

class ParallelPrefixScanTest {

    private static long[] fill(int n, long seed, long bound) {
        RandomSource rng = RandomSource.seeded(seed);
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = rng.nextLong(bound);
        }
        return a;
    }

    // --- manual baselines ---

    @Test
    void exclusiveSequentialMatchesManual() {
        assertArrayEquals(new long[]{0, 1, 3, 6, 10},
                ParallelPrefixScan.exclusiveSequential(new long[]{1, 2, 3, 4, 5}));
    }

    @Test
    void inclusiveSequentialMatchesManual() {
        assertArrayEquals(new long[]{1, 3, 6, 10},
                ParallelPrefixScan.inclusiveSequential(new long[]{1, 2, 3, 4}));
    }

    // --- small n / below threshold ---

    @Test
    void exclusiveSmallEqualsSequential() {
        long[] a = fill(64, 1L, 10);
        assertArrayEquals(ParallelPrefixScan.exclusiveSequential(a),
                ParallelPrefixScan.exclusive(a, 4));
    }

    @Test
    void inclusiveSmallEqualsSequential() {
        long[] a = fill(64, 2L, 10);
        assertArrayEquals(ParallelPrefixScan.inclusiveSequential(a),
                ParallelPrefixScan.inclusive(a, 4));
    }

    @Test
    void belowThresholdFallsBackToSequential() {
        long[] a = fill(512, 3L, 20);
        for (int p : new int[]{2, 4, 8}) {
            assertArrayEquals(ParallelPrefixScan.exclusiveSequential(a),
                    ParallelPrefixScan.exclusive(a, p));
            assertArrayEquals(ParallelPrefixScan.inclusiveSequential(a),
                    ParallelPrefixScan.inclusive(a, p));
        }
    }

    // --- large n / above threshold ---

    @Test
    void exclusiveLargeEqualsSequential() {
        long[] a = fill(4096, 11L, 1000);
        for (int p : new int[]{2, 4, 8}) {
            assertArrayEquals(ParallelPrefixScan.exclusiveSequential(a),
                    ParallelPrefixScan.exclusive(a, p), "parallelism " + p);
        }
    }

    @Test
    void inclusiveLargeEqualsSequential() {
        long[] a = fill(4096, 12L, 1000);
        for (int p : new int[]{2, 4, 8}) {
            assertArrayEquals(ParallelPrefixScan.inclusiveSequential(a),
                    ParallelPrefixScan.inclusive(a, p), "parallelism " + p);
        }
    }

    @Test
    void nonPowerOfTwoSizes() {
        for (int n : new int[]{4097, 5000, 10000}) {
            long[] a = fill(n, 13L, 1000);
            assertArrayEquals(ParallelPrefixScan.exclusiveSequential(a),
                    ParallelPrefixScan.exclusive(a, 4), "size " + n);
            assertArrayEquals(ParallelPrefixScan.inclusiveSequential(a),
                    ParallelPrefixScan.inclusive(a, 4), "size " + n);
        }
    }

    @Test
    void powersOfTwoSizes() {
        for (int n : new int[]{1024, 2048, 8192}) {
            long[] a = fill(n, 14L, 1000);
            assertArrayEquals(ParallelPrefixScan.exclusiveSequential(a),
                    ParallelPrefixScan.exclusive(a, 4), "size " + n);
        }
    }

    @Test
    void negativesWork() {
        long[] a = {-3, 5, -10, 2};
        assertArrayEquals(new long[]{-3, 2, -8, -6},
                ParallelPrefixScan.inclusiveSequential(a));
        assertArrayEquals(ParallelPrefixScan.inclusiveSequential(a),
                ParallelPrefixScan.inclusive(a, 4));
        assertArrayEquals(new long[]{0, -3, 2, -8},
                ParallelPrefixScan.exclusiveSequential(a));
        assertArrayEquals(ParallelPrefixScan.exclusiveSequential(a),
                ParallelPrefixScan.exclusive(a, 4));
    }

    @Test
    void propertyInclusiveEqualsExclusivePlusInput() {
        long[] a = fill(4096, 21L, 1000);
        long[] incl = ParallelPrefixScan.inclusive(a, 4);
        long[] excl = ParallelPrefixScan.exclusive(a, 4);
        for (int i = 0; i < a.length; i++) {
            assertEquals(incl[i], excl[i] + a[i], "index " + i);
        }
    }

    // --- edge cases ---

    @Test
    void emptyScan() {
        assertArrayEquals(new long[0], ParallelPrefixScan.exclusiveSequential(new long[0]));
        assertArrayEquals(new long[0], ParallelPrefixScan.inclusiveSequential(new long[0]));
        assertArrayEquals(new long[0], ParallelPrefixScan.exclusive(new long[0], 4));
        assertArrayEquals(new long[0], ParallelPrefixScan.inclusive(new long[0], 4));
    }

    @Test
    void singleElementScan() {
        assertArrayEquals(new long[]{0}, ParallelPrefixScan.exclusiveSequential(new long[]{7}));
        assertArrayEquals(new long[]{7}, ParallelPrefixScan.inclusiveSequential(new long[]{7}));
        assertArrayEquals(new long[]{0}, ParallelPrefixScan.exclusive(new long[]{7}, 4));
        assertArrayEquals(new long[]{7}, ParallelPrefixScan.inclusive(new long[]{7}, 4));
    }

    @Test
    void twoElementScan() {
        long[] a = {3, 4};
        assertArrayEquals(new long[]{0, 3}, ParallelPrefixScan.exclusive(a, 4));
        assertArrayEquals(new long[]{3, 7}, ParallelPrefixScan.inclusive(a, 4));
    }

    @Test
    void scanOverflowWrapsLikeSequential() {
        long[] a = new long[2048];
        java.util.Arrays.fill(a, Long.MAX_VALUE);
        assertArrayEquals(ParallelPrefixScan.inclusiveSequential(a),
                ParallelPrefixScan.inclusive(a, 4));
        assertArrayEquals(ParallelPrefixScan.exclusiveSequential(a),
                ParallelPrefixScan.exclusive(a, 4));
    }

    @Test
    void inputNotMutated() {
        long[] a = fill(4096, 31L, 1000);
        long[] copy = a.clone();
        ParallelPrefixScan.exclusive(a, 4);
        ParallelPrefixScan.inclusive(a, 4);
        assertArrayEquals(copy, a, "Input array must not be mutated");
    }

    @Test
    void deterministicAcrossRuns() {
        long[] a = fill(4096, 41L, 1000);
        assertArrayEquals(ParallelPrefixScan.inclusive(a, 4),
                ParallelPrefixScan.inclusive(a, 4));
        assertArrayEquals(ParallelPrefixScan.exclusive(a, 8),
                ParallelPrefixScan.exclusive(a, 8));
    }

    @Test
    void nullInputThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ParallelPrefixScan.exclusive(null, 4));
        assertThrows(IllegalArgumentException.class,
                () -> ParallelPrefixScan.inclusiveSequential(null));
    }

    @Test
    void invalidParallelismThrows() {
        long[] a = fill(4096, 51L, 100);
        assertThrows(IllegalArgumentException.class,
                () -> ParallelPrefixScan.inclusive(a, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ParallelPrefixScan.exclusive(a, -1));
    }
}