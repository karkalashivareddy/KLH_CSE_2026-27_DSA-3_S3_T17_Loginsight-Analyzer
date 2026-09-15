package com.loginsight.dsa.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Parallel prefix scan (Blelloch 1990) over a {@code long[]}.
 *
 * <p>A prefix scan replaces the element at index {@code i} with the running value of an
 * associative operation over {@code input[0..i]}.  This class implements the <b>inclusive</b>
 * scan ({@code result[i] = input[0] + … + input[i]}) and the <b>exclusive</b> scan
 * ({@code result[0] = 0}, {@code result[i] = input[0] + … + input[i−1]}).  The input array is
 * never mutated and the result is a fresh array.</p>
 *
 * <h2>Parallel schedule (Blelloch)</h2>
 * <ol>
 *   <li>Copy the input into a power-of-two padded work array (pad with 0).</li>
 *   <li><b>Up-sweep</b>: for each stride level, every block of two siblings adds the left child
 *       into the right child.  After the sweep, every node holds the sum of its subtree.</li>
 *   <li><b>Down-sweep</b>: zero the root, then, walking the levels downward, each block copies
 *       the parent value into its left child and adds the left child's old value into the right
 *       child.  After the sweep, every slot holds its exclusive prefix.</li>
 * </ol>
 *
 * <p>Each level is executed by partitioning the <em>blocks</em> of that level across up to
 * {@code parallelism} workers (one {@code invokeAll} barrier per level); workers only touch
 * disjoint block ranges, so the schedule is race-free.  <b>work = O(n)</b>,
 * <b>span = O(log n)</b> levels.</p>
 *
 * <p>Addition is the natural {@code long} arithmetic (overflow wraps like Java {@code +}),
 * identical to the sequential scan, so the parallel and sequential results always agree.</p>
 */
public final class ParallelPrefixScan {

    private ParallelPrefixScan() {
    }

    /** Sequential inclusive scan: {@code result[i] = input[0] + … + input[i]}. */
    public static long[] inclusiveSequential(long[] input) {
        requireInput(input);
        long[] out = new long[input.length];
        long acc = 0;
        for (int i = 0; i < input.length; i++) {
            acc += input[i];
            out[i] = acc;
        }
        return out;
    }

    /** Sequential exclusive scan: {@code result[0] = 0}, {@code result[i] = input[0] + … + input[i−1]}. */
    public static long[] exclusiveSequential(long[] input) {
        requireInput(input);
        long[] out = new long[input.length];
        long acc = 0;
        for (int i = 1; i < input.length; i++) {
            acc += input[i - 1];
            out[i] = acc;
        }
        return out;
    }

    /** Parallel inclusive scan (Blelloch), falling back to sequential below the threshold. */
    public static long[] inclusive(long[] input, int parallelism) {
        requireInput(input);
        ParallelSupport.requireParallelism(parallelism);
        if (input.length <= ParallelSupport.SEQUENTIAL_THRESHOLD) {
            return inclusiveSequential(input);
        }
        return scan(input, parallelism, true);
    }

    /** Parallel exclusive scan (Blelloch), falling back to sequential below the threshold. */
    public static long[] exclusive(long[] input, int parallelism) {
        requireInput(input);
        ParallelSupport.requireParallelism(parallelism);
        if (input.length <= ParallelSupport.SEQUENTIAL_THRESHOLD) {
            return exclusiveSequential(input);
        }
        return scan(input, parallelism, false);
    }

    private static long[] scan(long[] input, int parallelism, boolean inclusive) {
        int n = input.length;
        int m = nextPowerOfTwo(n);
        long[] tree = new long[m];
        System.arraycopy(input, 0, tree, 0, n);

        ExecutorService pool = ParallelSupport.newPool(parallelism);
        try {
            // Up-sweep: accumulate subtree sums into the right child of every block.
            for (int offset = 1; offset < m; offset <<= 1) {
                final int o = offset;
                final int stride = o << 1;
                runLevel(pool, m / stride, parallelism,
                        block -> {
                            int base = block * stride;
                            tree[base + stride - 1] += tree[base + o - 1];
                        });
            }

            // Down-sweep: turn subtree sums into exclusive prefixes.
            tree[m - 1] = 0;
            for (int offset = m >> 1; offset > 0; offset >>= 1) {
                final int o = offset;
                final int stride = o << 1;
                runLevel(pool, m / stride, parallelism,
                        block -> {
                            int base = block * stride;
                            long t = tree[base + o - 1];
                            tree[base + o - 1] = tree[base + stride - 1];
                            tree[base + stride - 1] = t + tree[base + stride - 1];
                        });
            }

            if (inclusive) {
                // inclusive[i] = exclusive[i] + input[i], elementwise in parallel.
                final long[] src = input;
                int workers = Math.min(parallelism, n);
                runLevel(pool, workers, parallelism,
                        worker -> {
                            int chunk = ParallelSupport.ceilDiv(n, workers);
                            int start = worker * chunk;
                            int end = Math.min(start + chunk, n);
                            for (int i = start; i < end; i++) {
                                tree[i] += src[i];
                            }
                        });
            }
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("parallel scan task failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("parallel scan interrupted", e);
        } finally {
            pool.shutdownNow();
        }

        long[] result = new long[n];
        System.arraycopy(tree, 0, result, 0, n);
        return result;
    }

    private interface BlockAction {
        void run(int blockIndex);
    }

    /**
     * Executes {@code action} for every block index, partitioning contiguous block ranges across
     * up to {@code parallelism} tasks and waiting for all of them (an implicit barrier).
     */
    private static void runLevel(ExecutorService pool, int blocks, int parallelism, BlockAction action)
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

    private static int nextPowerOfTwo(int n) {
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p;
    }

    private static void requireInput(long[] input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
    }
}