package com.loginsight.dsa.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Parallel comparison sort by round-based bottom-up merge sort.
 *
 * <p>The array is sorted by repeatedly merging adjacent sorted runs.  In round {@code r} the
 * runs have length {@code run} and are merged pairwise into an output buffer; the buffer roles
 * are then swapped (ping-pong), so no auxiliary copy pass per round is needed.  Each round is
 * executed with the merge <em>blocks</em> partitioned across up to {@code parallelism} workers;
 * workers write to disjoint output ranges, so the schedule is race-free.  The merge step is
 * hand-written and stable ({@code src[i] <= src[j]}), so elements that compare equal keep their
 * order.</p>
 *
 * <p>Because the round/block iteration order is identical for the parallel and sequential
 * variants, the two produce <b>byte-identical</b> output arrays — including arrays full of
 * duplicates.  The input array is never mutated; a sorted copy is returned.</p>
 *
 * <h2>Complexity</h2>
 * <ul>
 *   <li><b>Work</b>: O(n log n) (each of the log₂ n rounds scans the whole array once).</li>
 *   <li><b>Span</b>: O(n) (each merge task is sequential; the final round merges the two
 *       largest halves into one block).</li>
 * </ul>
 */
public final class ParallelSort {

    private ParallelSort() {
    }

    /** Computes a sorted copy of {@code input} using the same round-based merge schedule,
     *  but executing every merge on the calling thread. */
    public static long[] sortSequential(long[] input) {
        requireInput(input);
        int n = input.length;
        if (n <= 1) {
            return input.clone();
        }
        long[] src = input.clone();
        long[] dst = new long[n];
        for (int run = 1; run < n; run <<= 1) {
            int step = run << 1;
            for (int start = 0; start < n; start += step) {
                int mid = Math.min(start + run, n);
                int end = Math.min(start + step, n);
                mergeRun(src, dst, start, mid, end);
            }
            long[] tmp = src;
            src = dst;
            dst = tmp;
        }
        return src;
    }

    /** Computes a sorted copy of {@code input}, merging blocks in parallel each round. */
    public static long[] sortParallel(long[] input, int parallelism) {
        requireInput(input);
        ParallelSupport.requireParallelism(parallelism);
        int n = input.length;
        if (n <= ParallelSupport.SEQUENTIAL_THRESHOLD) {
            return sortSequential(input);
        }
        long[] src = input.clone();
        long[] dst = new long[n];
        ExecutorService pool = ParallelSupport.newPool(parallelism);
        try {
            for (int run = 1; run < n; run <<= 1) {
                final int r = run;
                int step = r << 1;
                int blocks = ParallelSupport.ceilDiv(n, step);
                final int fStep = step;
                final long[] fSrc = src;
                final long[] fDst = dst;
                mergeBlocks(pool, blocks, parallelism,
                        block -> {
                            int start = block * fStep;
                            int mid = Math.min(start + r, n);
                            int end = Math.min(start + fStep, n);
                            mergeRun(fSrc, fDst, start, mid, end);
                        });
                long[] tmp = src;
                src = dst;
                dst = tmp;
            }
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("parallel sort task failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("parallel sort interrupted", e);
        } finally {
            pool.shutdownNow();
        }
        return src;
    }

    private interface MergeAction {
        void run(int blockIndex);
    }

    /** Executes {@code action} for each merge block, partitioning contiguous block ranges
     *  across up to {@code parallelism} tasks and waiting for all (an implicit barrier). */
    private static void mergeBlocks(ExecutorService pool, int blocks, int parallelism, MergeAction action)
            throws InterruptedException, java.util.concurrent.ExecutionException {
        if (blocks <= 1) {
            action.run(0);
            return;
        }
        int workers = Math.min(parallelism, blocks);
        int perWorker = ParallelSupport.ceilDiv(blocks, workers);
        List<Callable<Void>> tasks = new ArrayList<>(workers);
        for (int w = 0; w < workers; w++) {
            final int from = w * perWorker;
            final int to = Math.min(from + perWorker, blocks);
            tasks.add(() -> {
                for (int b = from; b < to; b++) {
                    action.run(b);
                }
                return null;
            });
        }
        List<Future<Void>> futures = pool.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
    }

    /** Merges the two sorted runs {@code [start, mid)} and {@code [mid, end)} from {@code src}
     *  into the same positions of {@code dst}.  Stable: {@code src[i] <= src[j]}. */
    private static void mergeRun(long[] src, long[] dst, int start, int mid, int end) {
        if (start >= end) {
            return;
        }
        if (mid <= start || mid >= end) {
            System.arraycopy(src, start, dst, start, end - start);
            return;
        }
        int i = start;
        int j = mid;
        int k = start;
        while (i < mid && j < end) {
            dst[k++] = (src[i] <= src[j]) ? src[i++] : src[j++];
        }
        if (i < mid) {
            System.arraycopy(src, i, dst, k, mid - i);
        } else {
            System.arraycopy(src, j, dst, k, end - j);
        }
    }

    private static void requireInput(long[] input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
    }
}