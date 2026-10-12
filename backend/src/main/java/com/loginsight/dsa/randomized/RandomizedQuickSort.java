package com.loginsight.dsa.randomized;

import java.util.List;
import java.util.Map;

import com.loginsight.trace.StepRecorder;
import com.loginsight.trace.TracedResult;

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

    /**
     * Trace-capable path. Runs the identical pivot/partition recursion while recording one step per
     * chosen pivot (with pivot index and value) and per partition completion (subarray bounds, pivot
     * landing position, current partition view).
     */
    public TracedResult sortTracked(long[] input, RandomSource rng) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        StepRecorder recorder = new StepRecorder();
        long start = System.nanoTime();
        if (input.length <= 1) {
            return new TracedResult("RandomizedQuickSort",
                    Map.of("sorted", box(input), "comparisons", 0), Map.of("steps",
                            recorder.collect(), "truncated", recorder.isTruncated()),
                    recorder.collect(), System.nanoTime() - start, "O(n log n) expected",
                    "O(log n) expected stack");
        }
        long[] arr = input.clone();
        int[] comparisonCounter = new int[1];
        quickSortTracked(arr, 0, arr.length - 1, rng, recorder, comparisonCounter);
        long elapsed = System.nanoTime() - start;
        return new TracedResult("RandomizedQuickSort",
                Map.of("sorted", box(arr), "comparisons", comparisonCounter[0]),
                Map.of("steps", recorder.collect(), "truncated", recorder.isTruncated()),
                recorder.collect(), elapsed, "O(n log n) expected", "O(log n) expected stack");
    }

    private void quickSortTracked(long[] arr, int lo, int hi, RandomSource rng,
                                  StepRecorder recorder, int[] comparisons) {
        while (lo < hi) {
            int pivotIdx = lo + rng.nextInt(hi - lo + 1);
            long pivotValue = arr[pivotIdx];
            recorder.record("PIVOT", "Choose pivot index " + pivotIdx + " in [" + lo + "," + hi
                    + "], value " + pivotValue + ".",
                    StepRecorder.state("phase", "partition", "lo", lo, "hi", hi, "pivot",
                            pivotIdx, "pivotValue", pivotValue, "arr", box(arr)),
                    List.of(pivotIdx), Map.of());
            swap(arr, lo, pivotIdx);
            int p = hoarePartitionTracked(arr, lo, hi, recorder, comparisons);
            recorder.record("PARTITION", "Hoare partition around pivot " + arr[p] + " landed at "
                    + p + "; left <= pivot, right >= pivot.",
                    StepRecorder.state("phase", "partition", "lo", lo, "hi", hi, "pivot", p,
                            "pivotValue", arr[p], "arr", box(arr)),
                    List.of(p), Map.of());
            if (p - lo < hi - p) {
                quickSortTracked(arr, lo, p - 1, rng, recorder, comparisons);
                lo = p + 1;
            } else {
                quickSortTracked(arr, p + 1, hi, rng, recorder, comparisons);
                hi = p - 1;
            }
        }
    }

    private int hoarePartitionTracked(long[] arr, int lo, int hi, StepRecorder recorder,
                                      int[] comparisons) {
        long pivot = arr[lo];
        int i = lo;
        int j = hi + 1;
        while (true) {
            while (i < hi && arr[++i] < pivot) {
                comparisons[0]++;
            }
            while (j > lo && arr[--j] > pivot) {
                comparisons[0]++;
            }
            comparisons[0]++;
            if (i >= j) {
                break;
            }
            swap(arr, i, j);
        }
        swap(arr, lo, j);
        recorder.record("SWEEP", "Scan i=" + i + ", j=" + j + "; swap pivot into final slot.",
                StepRecorder.state("phase", "partition", "i", i, "j", j, "arr", box(arr)),
                List.of(i, j), Map.of());
        return j;
    }

    private static List<Long> box(long[] values) {
        List<Long> out = new java.util.ArrayList<>(values.length);
        for (long v : values) {
            out.add(v);
        }
        return out;
    }
}
