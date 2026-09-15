package com.loginsight.dsa.parallel;

/**
 * Immutable single-row result of a sequential-vs-parallel benchmark measurement.
 *
 * <p>Time values are wall-clock nanoseconds of the median warm measurement taken by
 * {@link ParallelBenchmark}.  {@code speedup = sequentialNanos / parallelNanos} and
 * {@code throughput} is {@code inputSize} elements per second of the parallel run.
 * {@code work} and {@code span} are schedule-derived values produced by
 * {@link WorkSpanAnalyzer}, never guessed.</p>
 */
public final class BenchmarkResult {

    private final String algorithm;
    private final int inputSize;
    private final long sequentialNanos;
    private final long parallelNanos;
    private final double speedup;
    private final double throughputPerSecond;
    private final long work;
    private final long span;
    private final int parallelism;

    public BenchmarkResult(String algorithm, int inputSize, long sequentialNanos, long parallelNanos,
                           double speedup, double throughputPerSecond, long work, long span,
                           int parallelism) {
        this.algorithm = algorithm;
        this.inputSize = inputSize;
        this.sequentialNanos = sequentialNanos;
        this.parallelNanos = parallelNanos;
        this.speedup = speedup;
        this.throughputPerSecond = throughputPerSecond;
        this.work = work;
        this.span = span;
        this.parallelism = parallelism;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getInputSize() {
        return inputSize;
    }

    public long getSequentialNanos() {
        return sequentialNanos;
    }

    public long getParallelNanos() {
        return parallelNanos;
    }

    public double getSpeedup() {
        return speedup;
    }

    public double getThroughputPerSecond() {
        return throughputPerSecond;
    }

    /** Schedule-derived work (see {@link WorkSpanAnalyzer}). */
    public long getWork() {
        return work;
    }

    /** Schedule-derived span (see {@link WorkSpanAnalyzer}). */
    public long getSpan() {
        return span;
    }

    public int getParallelism() {
        return parallelism;
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT,
                "BenchmarkResult{algorithm='%s', inputSize=%d, seq=%d ns, par=%d ns, "
                        + "speedup=%.2f, throughput=%.0f/s, work=%d, span=%d, parallelism=%d}",
                algorithm, inputSize, sequentialNanos, parallelNanos, speedup,
                throughputPerSecond, work, span, parallelism);
    }
}