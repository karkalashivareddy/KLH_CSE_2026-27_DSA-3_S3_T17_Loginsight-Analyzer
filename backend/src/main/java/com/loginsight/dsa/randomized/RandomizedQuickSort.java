package com.loginsight.dsa.randomized;

/**
 * Randomised in-place quicksort using Hoare partition.
 *
 * <p>A random pivot is selected uniformly from the current subarray, swapped to the
 * low element, and a Hoare partition splits the array around it.  Tail-recursion is
 * eliminated by recursing on the smaller half first and looping on the larger.</p>
 *
 * <h2>Complexity</h2>
 * <ul>
 *   <li><b>Expected</b> O(n log n) comparisons and swaps.</li>
 * <li><b>Worst-case</b> O(n²) (only if the RNG consistently picks the worst pivot—
 *       probability 1/n! for a specific adversarial ordering).</li>
 *   <li><b>Stack depth</b> O(log n) expected due to the tail-call elimination.</li>
 * </ul>
 *
 * <p>No {@code java.util.Arrays.sort} or {@code Collections.sort} is used.  The algorithm
 * handles empty arrays, single elements, sorted, reverse-sorted, all-equal, and duplicate-heavy
 * inputs correctly.</p>
 */
public final class RandomizedQuickSort {

    /**
     * Returns a new sorted copy of {@code input}.
     *
     * @param input source array (not modified)
     * @param rng   random source for pivot selection
     * @return sorted copy in ascending order
     * @throws IllegalArgumentException if {@code input} is null
     */
    public long[] sort(long[] input, RandomSource rng) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (input.length <= 1) {
            return input.clone();
        }
        long[] arr = input.clone();
        quickSort(arr, 0, arr.length - 1, rng);
        return arr;
    }

    private void quickSort(long[] arr, int lo, int hi, RandomSource rng) {
        while (lo < hi) {
            int pivotIdx = lo + rng.nextInt(hi - lo + 1);
            swap(arr, lo, pivotIdx);
            int p = hoarePartition(arr, lo, hi);
            if (p - lo < hi - p) {
                quickSort(arr, lo, p - 1, rng);
                lo = p + 1;
            } else {
                quickSort(arr, p + 1, hi, rng);
                hi = p - 1;
            }
        }
    }

    /**
     * Hoare partition with pivot at {@code arr[lo]}.
     *
     * <p>Places the pivot at its final position {@code j} so that
     * {@code arr[lo..j-1] <= arr[j] <= arr[j+1..hi]}.  Elements equal to the pivot are
     * spread across both sides, giving O(n) behaviour on all-equal inputs.</p>
     */
    private int hoarePartition(long[] arr, int lo, int hi) {
        long pivot = arr[lo];
        int i = lo;
        int j = hi + 1;
        while (true) {
            while (i < hi && arr[++i] < pivot) { }
            while (j > lo && arr[--j] > pivot) { }
            if (i >= j) {
                break;
            }
            swap(arr, i, j);
        }
        swap(arr, lo, j);
        return j;
    }

    private static void swap(long[] arr, int i, int j) {
        long tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }
}
