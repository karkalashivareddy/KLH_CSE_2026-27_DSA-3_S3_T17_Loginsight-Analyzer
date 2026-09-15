package com.loginsight.dsa.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Parallel reduction over a {@code long[]}.
 *
 * <p>Reduction applies an associative binary operation to every element, e.g. the sum,
 * maximum, count of marked entries, or error-count of a numeric-encoded log column.  The input
 * array is never mutated; a fresh result value is returned.</p>
 *
 * <h2>Parallel schedule</h2>
 * <p>The array is split into {@code min(parallelism, n)} contiguous chunks; every chunk is
 * reduced <b>sequentially</b> by one worker, then the <b>chunk partials</b> are combined by a
 * balanced binary tree.  All workers write only to their own chunk and the partials-only tree
 * combine is race-free.  For {@code p} workers and {@code n} elements:
 * <b>work = O(n)</b>, <b>span = O(n/p + log p)</b> — with {@code p = n} (binary-tree reduce)
 * the span is O(log n), the classic PRAM reduction claim.  Inputs of at most
 * {@link ParallelSupport#SEQUENTIAL_THRESHOLD} elements run sequentially.</p>
 *
 * <p>Because every operator here is associative (including the natural modulo-2^64 wrap-around
 * of {@code long} addition), the parallel result equals the sequential left-to-right fold
 * for <b>any</b> chunking and thread count.</p>
 */
public final class ParallelReduce {

    /** Associative reduction operators. */
    public enum ReduceOp {
        /** Sum of all elements (natural {@code long} arithmetic; overflow wraps like +). */
        SUM,
        /** Count of elements strictly greater than zero. */
        COUNT,
        /** Maximum element. */
        MAX,
        /** Count of elements equal to a given error marker (the error-count primitive). */
        ERROR_COUNT
    }

    private ParallelReduce() {
    }

    /**
     * Sequential left-to-right fold.
     *
     * @param input input array (never mutated)
     * @param op    reduction operator
     * @param marker marker value used only by {@link ReduceOp#ERROR_COUNT}, ignored otherwise
     * @return the reduction value
     * @throws IllegalArgumentException if {@code input} is null, or {@code op} is
     *                                  {@link ReduceOp#MAX} on an empty array
     */
    public static long reduceSequential(long[] input, ReduceOp op, long marker) {
        requireInput(input, op);
        if (input.length == 0) {
            return identity(op);
        }
        long acc = transform(op, input[0], marker);
        for (int i = 1; i < input.length; i++) {
            acc = combine(op, acc, transform(op, input[i], marker), marker);
        }
        return acc;
    }

    /** Sequential fold for the marker-free operators. */
    public static long reduceSequential(long[] input, ReduceOp op) {
        return reduceSequential(input, op, 0L);
    }

    /** Parallel reduction over {@code parallelism} worker chunks. */
    public static long reduce(long[] input, ReduceOp op, long marker, int parallelism) {
        requireInput(input, op);
        ParallelSupport.requireParallelism(parallelism);
        if (input.length <= ParallelSupport.SEQUENTIAL_THRESHOLD) {
            return reduceSequential(input, op, marker);
        }
        int workers = Math.min(parallelism, input.length);
        int chunk = ParallelSupport.ceilDiv(input.length, workers);
        ExecutorService pool = ParallelSupport.newPool(parallelism);
        try {
            List<Callable<Long>> tasks = new ArrayList<>(workers);
            for (int i = 0; i < workers; i++) {
                final int start = i * chunk;
                final int end = Math.min(start + chunk, input.length);
                tasks.add(() -> foldRange(input, start, end, op, marker));
            }
            List<Future<Long>> futures = pool.invokeAll(tasks);
            long[] partials = new long[futures.size()];
            for (int i = 0; i < partials.length; i++) {
                partials[i] = futures.get(i).get();
            }
            return treeCombine(partials, op, marker);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("parallel reduction task failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("parallel reduction interrupted", e);
        } finally {
            pool.shutdownNow();
        }
    }

    /** Parallel reduction for the marker-free operators. */
    public static long reduce(long[] input, ReduceOp op, int parallelism) {
        return reduce(input, op, 0L, parallelism);
    }

    private static long foldRange(long[] input, int from, int to, ReduceOp op, long marker) {
        if (from == to) {
            return identity(op);
        }
        long acc = transform(op, input[from], marker);
        for (int i = from + 1; i < to; i++) {
            acc = combine(op, acc, transform(op, input[i], marker), marker);
        }
        return acc;
    }

    private static long treeCombine(long[] partials, ReduceOp op, long marker) {
        while (partials.length > 1) {
            int next = ParallelSupport.ceilDiv(partials.length, 2);
            long[] upper = new long[next];
            for (int i = 0; i < next; i++) {
                long left = partials[2 * i];
                long right = (2 * i + 1 < partials.length) ? partials[2 * i + 1] : 0;
                upper[i] = (2 * i + 1 < partials.length)
                        ? combine(op, left, right, marker)
                        : left;
            }
            partials = upper;
        }
        return partials.length == 0 ? identity(op) : partials[0];
    }

    /** Maps a single input element onto the value its operator folds. */
    private static long transform(ReduceOp op, long value, long marker) {
        switch (op) {
            case SUM:
            case MAX:
                return value;
            case COUNT:
                return value > 0 ? 1L : 0L;
            case ERROR_COUNT:
                return value == marker ? 1L : 0L;
            default:
                throw new IllegalArgumentException("unknown reduce op " + op);
        }
    }

    private static long combine(ReduceOp op, long a, long b, long marker) {
        switch (op) {
            case SUM:
                return a + b;
            case COUNT:
                return a + b;
            case MAX:
                return Math.max(a, b);
            case ERROR_COUNT:
                return a + b;
            default:
                throw new IllegalArgumentException("unknown reduce op " + op);
        }
    }

    private static long identity(ReduceOp op) {
        switch (op) {
            case SUM:
            case COUNT:
            case ERROR_COUNT:
                return 0L;
            case MAX:
                return Long.MIN_VALUE;
            default:
                throw new IllegalArgumentException("unknown reduce op " + op);
        }
    }

    private static void requireInput(long[] input, ReduceOp op) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (input.length == 0 && op == ReduceOp.MAX) {
            throw new IllegalArgumentException("MAX is undefined on an empty array");
        }
    }
}