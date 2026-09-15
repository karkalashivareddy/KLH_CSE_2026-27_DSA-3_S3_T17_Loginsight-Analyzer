package com.loginsight.dsa.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Measures the sequential and parallel execution time of the parallel engine and assembles
 * {@link BenchmarkResult} rows, including schedule-derived work/span.
 *
 * <p>Methodology (per docs/02 §9): one warm-up run, then {@code repetitions} timed runs whose
 * median is reported (median is robust against GC pauses and scheduling noise).  Data sets are
 * generated with a fixed-seed {@link Random}, so runs are reproducible.  Speedup is
 * {@code sequentialNanos / parallelNanos}; throughput is elements per second of the parallel
 * run.  Work/span come from {@link WorkSpanAnalyzer} models of the implemented schedules
 * (binary reduce tree, Blelloch up+down sweep, merge-sort rounds) — never guessed.</p>
 */
public final class ParallelBenchmark {

    /** Sizes benchmarked by default: 10k … 1M where practical (docs/04 Module 6B). */
    public static final int[] DEFAULT_SIZES = {10_000, 50_000, 100_000, 500_000};

    private ParallelBenchmark() {
    }

    /** Benchmarks {@link ParallelReduce} over the given sizes. */
    public static List<BenchmarkResult> benchmarkReduce(int[] sizes, ParallelReduce.ReduceOp op,
                                                        int parallelism, int repetitions) {
        requireParams(sizes, parallelism, repetitions);
        List<BenchmarkResult> results = new ArrayList<>(sizes.length);
        long[] data = fillData(sizes[sizes.length - 1]);
        for (int size : sizes) {
            long seq = medianNanos(repetitions,
                    () -> ParallelReduce.reduceSequential(sub(data, size), op));
            long par = medianNanos(repetitions,
                    () -> ParallelReduce.reduce(sub(data, size), op, parallelism));
            append(results, "ParallelReduce", size, seq, par, parallelism, reduceSchedule(size));
        }
        return results;
    }

    /** Benchmarks {@link ParallelPrefixScan} (inclusive) over the given sizes. */
    public static List<BenchmarkResult> benchmarkScan(int[] sizes, int parallelism, int repetitions) {
        requireParams(sizes, parallelism, repetitions);
        List<BenchmarkResult> results = new ArrayList<>(sizes.length);
        long[] data = fillData(sizes[sizes.length - 1]);
        for (int size : sizes) {
            long seq = medianNanos(repetitions,
                    () -> ParallelPrefixScan.inclusiveSequential(sub(data, size)));
            long par = medianNanos(repetitions,
                    () -> ParallelPrefixScan.inclusive(sub(data, size), parallelism));
            append(results, "ParallelPrefixScan", size, seq, par, parallelism, scanSchedule(size));
        }
        return results;
    }

    /** Benchmarks {@link ParallelSort} over the given sizes. */
    public static List<BenchmarkResult> benchmarkSort(int[] sizes, int parallelism, int repetitions) {
        requireParams(sizes, parallelism, repetitions);
        List<BenchmarkResult> results = new ArrayList<>(sizes.length);
        long[] data = fillData(sizes[sizes.length - 1]);
        for (int size : sizes) {
            long seq = medianNanos(repetitions,
                    () -> ParallelSort.sortSequential(sub(data, size)));
            long par = medianNanos(repetitions,
                    () -> ParallelSort.sortParallel(sub(data, size), parallelism));
            append(results, "ParallelSort", size, seq, par, parallelism, sortSchedule(size));
        }
        return results;
    }

    // --- timing helpers -------------------------------------------------------

    private interface TimedRun {
        void run();
    }

    private static long medianNanos(int repetitions, TimedRun run) {
        run.run(); // warm-up (JIT, pool spin-up)
        long[] samples = new long[repetitions];
        for (int i = 0; i < repetitions; i++) {
            long start = System.nanoTime();
            run.run();
            samples[i] = System.nanoTime() - start;
        }
        insertionSort(samples);
        return samples[repetitions / 2];
    }

    private static void insertionSort(long[] xs) {
        for (int i = 1; i < xs.length; i++) {
            long key = xs[i];
            int j = i - 1;
            while (j >= 0 && xs[j] > key) {
                xs[j + 1] = xs[j];
                j--;
            }
            xs[j + 1] = key;
        }
    }

    private static long[] fillData(int maxSize) {
        Random rng = new Random(42); // fixed seed: reproducible benchmark inputs
        long[] data = new long[maxSize];
        for (int i = 0; i < maxSize; i++) {
            data[i] = rng.nextLong(Long.MAX_VALUE);
        }
        return data;
    }

    private static long[] sub(long[] data, int size) {
        long[] copy = new long[size];
        System.arraycopy(data, 0, copy, 0, size);
        return copy;
    }

    private static void append(List<BenchmarkResult> results, String algorithm, int size,
                               long seq, long par, int parallelism, WorkSpanAnalyzer.Task schedule) {
        WorkSpanAnalyzer.WorkSpanResult ws = WorkSpanAnalyzer.analyze(schedule);
        double speedup = par == 0 ? 0 : (double) seq / par;
        double seconds = par / 1_000_000_000.0;
        double throughput = (par == 0) ? 0 : (double) size / seconds;
        results.add(new BenchmarkResult(algorithm, size, seq, par, speedup, throughput,
                ws.getWork(), ws.getSpan(), parallelism));
    }

    // --- schedule models -------------------------------------------------------

    private static WorkSpanAnalyzer.Task reduceSchedule(int n) {
        return WorkSpanAnalyzer.Task.binaryReduceTree("ParallelReduce", n, 1, 1);
    }

    private static WorkSpanAnalyzer.Task scanSchedule(int n) {
        int m = 1;
        while (m < n) {
            m <<= 1;
        }
        WorkSpanAnalyzer.Task up = WorkSpanAnalyzer.Task.binaryReduceTree("upSweep", m, 1, 1);
        WorkSpanAnalyzer.Task down = WorkSpanAnalyzer.Task.binaryReduceTree("downSweep", m, 1, 1);
        return WorkSpanAnalyzer.Task.sequentialNode("ParallelPrefixScan.blelloch", 0, up, down);
    }

    private static WorkSpanAnalyzer.Task sortSchedule(int n) {
        List<WorkSpanAnalyzer.Task> rounds = new ArrayList<>();
        for (int run = 1; run < n; run <<= 1) {
            int step = run << 1;
            int blocks = ParallelSupport.ceilDiv(n, step);
            WorkSpanAnalyzer.Task[] merges = new WorkSpanAnalyzer.Task[blocks];
            for (int b = 0; b < blocks; b++) {
                merges[b] = WorkSpanAnalyzer.Task.leaf("merge" + b, step);
            }
            rounds.add(WorkSpanAnalyzer.Task.parallelNode("round" + run, 0, merges));
        }
        WorkSpanAnalyzer.Task[] arr = rounds.toArray(new WorkSpanAnalyzer.Task[0]);
        return WorkSpanAnalyzer.Task.sequentialNode("ParallelSort", 0, arr);
    }

    private static void requireParams(int[] sizes, int parallelism, int repetitions) {
        if (sizes == null || sizes.length == 0) {
            throw new IllegalArgumentException("sizes must be a non-empty array");
        }
        for (int s : sizes) {
            if (s < 1) {
                throw new IllegalArgumentException("size must be >= 1, got " + s);
            }
        }
        ParallelSupport.requireParallelism(parallelism);
        if (repetitions < 1) {
            throw new IllegalArgumentException("repetitions must be >= 1, got " + repetitions);
        }
    }
}