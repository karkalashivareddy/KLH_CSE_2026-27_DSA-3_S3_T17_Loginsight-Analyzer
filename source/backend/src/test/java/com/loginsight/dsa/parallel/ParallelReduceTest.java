package com.loginsight.dsa.parallel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loginsight.dsa.randomized.RandomSource;

import org.junit.jupiter.api.Test;

class ParallelReduceTest {

    private static long[] fill(int n, long seed, long bound) {
        RandomSource rng = RandomSource.seeded(seed);
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = rng.nextLong(bound);
        }
        return a;
    }

    // --- small n (below the sequential threshold) ---

    @Test
    void sumSmallEqualsSequential() {
        long[] a = fill(100, 1L, 50);
        assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.SUM),
                ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, 4));
    }

    @Test
    void maxSmallEqualsSequential() {
        long[] a = fill(100, 2L, 50);
        assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.MAX),
                ParallelReduce.reduce(a, ParallelReduce.ReduceOp.MAX, 4));
    }

    @Test
    void countSmallEqualsSequential() {
        long[] a = fill(100, 3L, 50);
        assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.COUNT),
                ParallelReduce.reduce(a, ParallelReduce.ReduceOp.COUNT, 4));
    }

    @Test
    void errorCountSmallEqualsSequential() {
        long[] a = fill(100, 4L, 20);
        a[7] = 500;
        a[42] = 500;
        long[] copy = a.clone();
        assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.ERROR_COUNT, 500),
                ParallelReduce.reduce(copy, ParallelReduce.ReduceOp.ERROR_COUNT, 500, 4));
    }

    // --- large n (above the sequential threshold), multiple parallelism counts ---

    @Test
    void sumLargeEqualsSequential() {
        long[] a = fill(4096, 11L, 1000);
        for (int p : new int[]{2, 4, 8}) {
            assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.SUM),
                    ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, p),
                    "parallelism " + p);
        }
    }

    @Test
    void maxLargeEqualsSequential() {
        long[] a = fill(4096, 12L, 1000);
        for (int p : new int[]{2, 4, 8}) {
            assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.MAX),
                    ParallelReduce.reduce(a, ParallelReduce.ReduceOp.MAX, p),
                    "parallelism " + p);
        }
    }

    @Test
    void countLargeEqualsSequential() {
        long[] a = fill(4096, 13L, 1000);
        for (int p : new int[]{2, 4, 8}) {
            assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.COUNT),
                    ParallelReduce.reduce(a, ParallelReduce.ReduceOp.COUNT, p),
                    "parallelism " + p);
        }
    }

    @Test
    void errorCountLargeEqualsSequential() {
        long[] a = fill(4096, 14L, 100);
        RandomSource rng = RandomSource.seeded(66L);
        int expected = 0;
        for (int i = 0; i < 30; i++) {
            int idx = (int) rng.nextLong(4096);
            if (a[idx] != 999) {
                a[idx] = 999;
                expected++;
            }
        }
        for (int p : new int[]{2, 4, 8}) {
            assertEquals(expected, ParallelReduce.reduce(a, ParallelReduce.ReduceOp.ERROR_COUNT, 999, p),
                    "parallelism " + p);
        }
    }

    @Test
    void resultIndependentOfWorkerCount() {
        long[] a = fill(8192, 21L, 2000);
        long base = ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, 2);
        for (int p : new int[]{1, 3, 7, 16}) {
            assertEquals(base, ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, p));
        }
    }

    // --- edge cases ---

    @Test
    void emptyInput() {
        long[] a = new long[0];
        assertEquals(0L, ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.SUM));
        assertEquals(0L, ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.COUNT));
        assertEquals(0L, ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.ERROR_COUNT, 1L));
        assertEquals(0L, ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, 4));
    }

    @Test
    void maxOnEmptyThrows() {
        long[] a = new long[0];
        assertThrows(IllegalArgumentException.class,
                () -> ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.MAX));
        assertThrows(IllegalArgumentException.class,
                () -> ParallelReduce.reduce(a, ParallelReduce.ReduceOp.MAX, 4));
    }

    @Test
    void singleElement() {
        long[] a = {42};
        assertEquals(42L, ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.SUM));
        assertEquals(1L, ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.COUNT));
        assertEquals(42L, ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.MAX));
        assertEquals(42L, ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, 4));
    }

    @Test
    void negativeValuesMax() {
        long[] a = {-5, -100, -1, -8};
        assertEquals(-1L, ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.MAX));
        assertEquals(-1L, ParallelReduce.reduce(a, ParallelReduce.ReduceOp.MAX, 4));
        assertEquals(-114L, ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.SUM));
    }

    @Test
    void sumOverflowWrapsLikeSequential() {
        long[] a = new long[4096];
        java.util.Arrays.fill(a, Long.MAX_VALUE);
        assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.SUM),
                ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, 8));
    }

    @Test
    void inputNotMutated() {
        long[] a = fill(4096, 31L, 1000);
        long[] copy = a.clone();
        ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, 4);
        ParallelReduce.reduce(a, ParallelReduce.ReduceOp.MAX, 4);
        ParallelReduce.reduce(a, ParallelReduce.ReduceOp.ERROR_COUNT, 500, 4);
        assertArrayEquals(copy, a, "Input array must not be mutated");
    }

    @Test
    void nullInputThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ParallelReduce.reduceSequential(null, ParallelReduce.ReduceOp.SUM));
        assertThrows(IllegalArgumentException.class,
                () -> ParallelReduce.reduce(null, ParallelReduce.ReduceOp.SUM, 4));
    }

    @Test
    void invalidParallelismThrows() {
        long[] a = fill(4096, 41L, 100);
        assertThrows(IllegalArgumentException.class,
                () -> ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, -2));
    }

    @Test
    void largeArray200k() {
        long[] a = fill(200_000, 51L, 1000);
        assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.SUM),
                ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, 8));
        assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.MAX),
                ParallelReduce.reduce(a, ParallelReduce.ReduceOp.MAX, 8));
    }
}