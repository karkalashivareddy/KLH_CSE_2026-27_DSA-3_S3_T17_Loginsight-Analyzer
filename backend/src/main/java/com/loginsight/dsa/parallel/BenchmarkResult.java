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
        return "BenchmarkResult{algorithm='" + algorithm + "', inputSize=" + inputSize
                + ", seq=" + sequentialNanos + " ns, par=" + parallelNanos + " ns, speedup="
                + fixed(speedup, 2) + ", throughput=" + fixed(throughputPerSecond, 2)
                + "/s, work=" + work + ", span=" + span + ", parallelism=" + parallelism + "}";
    }

    /**
     * Locale-independent fixed-point rendering built from {@link Math} and {@link String} only, so
     * the benchmark row never depends on a {@code java.util} formatter that would hide the
     * measurement it prints.
     */
    private static String fixed(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return Double.toString(value);
        }
        long scale = 1L;
        for (int i = 0; i < decimals; i++) {
            scale *= 10L;
        }
        long scaled = Math.round(Math.abs(value) * scale);
        StringBuilder fraction = new StringBuilder(Long.toString(scaled % scale));
        while (fraction.length() < decimals) {
            fraction.insert(0, '0');
        }
        return (value < 0 ? "-" : "") + (scaled / scale) + "." + fraction;
    }
}