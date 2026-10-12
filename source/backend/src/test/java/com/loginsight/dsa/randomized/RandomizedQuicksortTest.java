package com.loginsight.dsa.randomized;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class RandomizedQuicksortTest {

    private final RandomizedQuickSort sorter = new RandomizedQuickSort();

    @Test
    void emptyArray() {
        long[] result = sorter.sort(new long[0], RandomSource.seeded(1L));
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void singleElement() {
        assertArrayEquals(new long[]{42}, sorter.sort(new long[]{42}, RandomSource.seeded(1L)));
    }

    @Test
    void twoElementsSorted() {
        assertArrayEquals(new long[]{1, 2}, sorter.sort(new long[]{1, 2}, RandomSource.seeded(1L)));
    }

    @Test
    void twoElementsReverse() {
        assertArrayEquals(new long[]{1, 2}, sorter.sort(new long[]{2, 1}, RandomSource.seeded(1L)));
    }

    @Test
    void alreadySorted() {
        long[] input = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        long[] expected = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertArrayEquals(expected, sorter.sort(input, RandomSource.seeded(1L)));
    }

    @Test
    void reverseSorted() {
        long[] input = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        long[] expected = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertArrayEquals(expected, sorter.sort(input, RandomSource.seeded(1L)));
    }

    @Test
    void allEqual() {
        long[] input = {7, 7, 7, 7, 7};
        long[] expected = {7, 7, 7, 7, 7};
        assertArrayEquals(expected, sorter.sort(input, RandomSource.seeded(1L)));
    }

    @Test
    void duplicatesScattered() {
        long[] input = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5};
        long[] expected = input.clone();
        java.util.Arrays.sort(expected);
        assertArrayEquals(expected, sorter.sort(input, RandomSource.seeded(1L)));
    }

    @Test
    void randomArray() {
        RandomSource rng = RandomSource.seeded(42L);
        long[] input = new long[200];
        for (int i = 0; i < input.length; i++) {
            input[i] = rng.nextLong(1000);
        }
        long[] expected = input.clone();
        java.util.Arrays.sort(expected);
        assertArrayEquals(expected, sorter.sort(input, RandomSource.seeded(1L)));
    }

    @Test
    void doesNotMutateInput() {
        long[] input = {5, 3, 1, 4, 2};
        long[] copy = input.clone();
        sorter.sort(input, RandomSource.seeded(1L));
        assertArrayEquals(copy, input, "Input array must not be mutated");
    }

    @Test
    void deterministicSameSeed() {
        long[] input = {10, 7, 3, 8, 1, 9, 2, 6, 4, 5};
        long[] r1 = sorter.sort(input, RandomSource.seeded(42L));
        long[] r2 = sorter.sort(input, RandomSource.seeded(42L));
        assertArrayEquals(r1, r2);
    }

    @Test
    void sortedOutputMatchesOracle() {
        // Reference sort via test-only oracle (java.util.Arrays.sort in test code only)
        RandomSource inputRng = RandomSource.seeded(123L);
        long[] input = new long[500];
        for (int i = 0; i < input.length; i++) {
            input[i] = inputRng.nextLong(10000);
        }
        long[] oracle = input.clone();
        java.util.Arrays.sort(oracle);
        assertArrayEquals(oracle, sorter.sort(input, RandomSource.seeded(7L)));
    }

    @Test
    void negativeValues() {
        long[] input = {-5, 3, -1, 0, -7, 2};
        long[] expected = {-7, -5, -1, 0, 2, 3};
        assertArrayEquals(expected, sorter.sort(input, RandomSource.seeded(1L)));
    }

    @Test
    void nullInputThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> sorter.sort(null, RandomSource.seeded(1L)));
    }

    @Test
    void largeRandomArraySortedCorrectly() {
        RandomSource inputRng = RandomSource.seeded(999L);
        long[] input = new long[5000];
        for (int i = 0; i < input.length; i++) {
            input[i] = inputRng.nextLong(50000);
        }
        long[] oracle = input.clone();
        java.util.Arrays.sort(oracle);
        assertArrayEquals(oracle, sorter.sort(input, RandomSource.seeded(7L)));
    }
}
