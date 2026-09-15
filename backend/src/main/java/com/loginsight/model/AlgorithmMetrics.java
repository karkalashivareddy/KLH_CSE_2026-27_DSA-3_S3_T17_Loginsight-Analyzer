package com.loginsight.model;

/**
 * Immutable measurement record attached to every algorithmic query (docs/02 §5, docs/03 tree).
 *
 * <p>{@code executionTimeNanos} comes from the actual {@code System.nanoTime()} window around the
 * algorithm call — never fabricated. {@code throughput} is {@code inputSize / seconds} where a
 * meaningful rate exists ({@code 0} otherwise); {@code speedup} is {@code sequential / parallel}
 * for the parallel engines ({@code 0} for plain sequential algorithms).</p>
 *
 * <p>{@code memoryEstimateBytes} is documented as an <em>approximation</em> (sum of the sizes of the
 * primary working arrays), never a JVM-accurate heap number (docs/02 §5).</p>
 */
public final class AlgorithmMetrics {

    private final int inputSize;
    private final long executionTimeNanos;
    private final int resultSize;
    private final long memoryEstimateBytes;
    private final double throughput;
    private final double speedup;

    public AlgorithmMetrics(int inputSize, long executionTimeNanos, int resultSize,
                           long memoryEstimateBytes, double throughput, double speedup) {
        this.inputSize = inputSize;
        this.executionTimeNanos = executionTimeNanos;
        this.resultSize = resultSize;
        this.memoryEstimateBytes = memoryEstimateBytes;
        this.throughput = throughput;
        this.speedup = speedup;
    }

    public int getInputSize() {
        return inputSize;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public int getResultSize() {
        return resultSize;
    }

    public long getMemoryEstimateBytes() {
        return memoryEstimateBytes;
    }

    public double getThroughput() {
        return throughput;
    }

    public double getSpeedup() {
        return speedup;
    }

    @Override
    public String toString() {
        return "AlgorithmMetrics[inputSize=" + inputSize + ", nanos=" + executionTimeNanos
                + ", resultSize=" + resultSize + ", memoryBytes=" + memoryEstimateBytes
                + ", throughput=" + throughput + ", speedup=" + speedup + "]";
    }
}