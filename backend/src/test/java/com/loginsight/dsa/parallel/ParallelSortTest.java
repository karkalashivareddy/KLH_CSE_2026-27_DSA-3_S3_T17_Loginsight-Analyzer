package com.loginsight.dsa.parallel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loginsight.dsa.randomized.RandomSource;

import org.junit.jupiter.api.Test;

class ParallelSortTest {

    private static long[] fill(int n, long seed, long bound) {
        RandomSource rng = RandomSource.seeded(seed);
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = rng.nextLong(bound);
        }
        return a;
    }

    // --- edge cases ---

    @Test
    void emptyArray() {
        long[] r = ParallelSort.sortParallel(new long[0], 4);
        assertArrayEquals(new long[0], r);
        assertArrayEquals(new long[0], ParallelSort.sortSequential(new long[0]));
    }

    @Test
    void singleElement() {
        assertArrayEquals(new long[]{42}, ParallelSort.sortParallel(new long[]{42}, 4));
    }

    @Test
    void twoElements() {
        assertArrayEquals(new long[]{1, 2}, ParallelSort.sortParallel(new long[]{2, 1}, 4));
        assertArrayEquals(new long[]{1, 2}, ParallelSort.sortSequential(new long[]{2, 1}));
    }

    @Test
    void alreadySorted() {
        long[] a = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertArrayEquals(a, ParallelSort.sortParallel(a, 4));
    }

    @Test
    void reverseSorted() {
        long[] a = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        long[] expected = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertArrayEquals(expected, ParallelSort.sortParallel(a, 4));
        assertArrayEquals(expected, ParallelSort.sortSequential(a));
    }

    @Test
    void allEqual() {
        long[] a = {7, 7, 7, 7, 7, 7, 7};
        assertArrayEquals(a, ParallelSort.sortParallel(a, 4));
    }

    @Test
    void negativeValues() {
        long[] a = {-5, 3, -1, 0, -7, 2};
        long[] expected = {-7, -5, -1, 0, 2, 3};
        assertArrayEquals(expected, ParallelSort.sortParallel(a, 4));
        assertArrayEquals(expected, ParallelSort.sortSequential(a));
    }

    // --- parallel vs sequential and oracle ---

    @Test
    void belowThresholdFallback() {
        long[] a = fill(500, 1L, 1000);
        assertArrayEquals(ParallelSort.sortSequential(a), ParallelSort.sortParallel(a, 4));
    }

    @Test
    void largeMatchesOracle() {
        long[] a = fill(4096, 2L, 1_000_000);
        long[] oracle = a.clone();
        java.util.Arrays.sort(oracle);
        for (int p : new int[]{2, 4, 8}) {
            assertArrayEquals(oracle, ParallelSort.sortParallel(a, p), "parallelism " + p);
        }
    }

    @Test
    void parallelEqualsSequentialDuplicateHeavy() {
        RandomSource rng = RandomSource.seeded(3L);
        long[] a = new long[2000];
        for (int i = 0; i < a.length; i++) {
            a[i] = rng.nextLong(5);
        }
        for (int p : new int[]{2, 4, 8}) {
            assertArrayEquals(ParallelSort.sortSequential(a), ParallelSort.sortParallel(a, p),
                    "parallelism " + p);
        }
    }

    @Test
    void nonPowerOfTwoLarge() {
        long[] a = fill(5000, 4L, 1_000_000);
        long[] oracle = a.clone();
        java.util.Arrays.sort(oracle);
        assertArrayEquals(oracle, ParallelSort.sortParallel(a, 4));
        assertArrayEquals(oracle, ParallelSort.sortSequential(a));
    }

    @Test
    void inputNotMutated() {
        long[] a = fill(4096, 5L, 1000);
        long[] copy = a.clone();
        ParallelSort.sortParallel(a, 4);
        ParallelSort.sortSequential(a);
        assertArrayEquals(copy, a, "Input array must not be mutated");
    }

    @Test
    void deterministicAcrossRuns() {
        long[] a = fill(4096, 6L, 1000);
        assertArrayEquals(ParallelSort.sortParallel(a, 4), ParallelSort.sortParallel(a, 4));
    }

    @Test
    void nullInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> ParallelSort.sortParallel(null, 4));
        assertThrows(IllegalArgumentException.class, () -> ParallelSort.sortSequential(null));
    }

    @Test
    void invalidParallelismThrows() {
        long[] a = fill(4096, 7L, 1000);
        assertThrows(IllegalArgumentException.class, () -> ParallelSort.sortParallel(a, 0));
        assertThrows(IllegalArgumentException.class, () -> ParallelSort.sortParallel(a, -2));
    }

    @Test
    void largeRandom50k() {
        long[] a = fill(50_000, 8L, Long.MAX_VALUE / 4);
        long[] oracle = a.clone();
        java.util.Arrays.sort(oracle);
        long[] par = ParallelSort.sortParallel(a, 4);
        assertArrayEquals(oracle, par);
    }
}