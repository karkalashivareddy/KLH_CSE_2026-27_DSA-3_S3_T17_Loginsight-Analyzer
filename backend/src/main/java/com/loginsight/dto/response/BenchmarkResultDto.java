package com.loginsight.dto.response;

/**
 * One row of a {@code POST /api/benchmark/run} response (docs/12 §10). All timings are measured live
 * (median over repetitions); throughput is {@code inputSize / seconds}. Parallel rows additionally
 * carry the engine's work/span evidence.
 */
public record BenchmarkResultDto(String algorithm, long inputSize, long executionTimeNanos,
                                 double throughputPerSec, int resultSize, Long sequentialNanos,
                                 Long parallelNanos, Double speedup, Long work, Long span,
                                 Integer parallelism) {

    public static BenchmarkResultDto of(String algorithm, long inputSize, long nanos,
                                        double throughput, int resultSize) {
        return new BenchmarkResultDto(algorithm, inputSize, nanos, throughput, resultSize,
                null, null, null, null, null, null);
    }

    public static BenchmarkResultDto parallel(String algorithm, long inputSize, long sequentialNanos,
                                              long parallelNanos, double speedup, int resultSize,
                                              long work, long span, int parallelism) {
        double seconds = parallelNanos == 0 ? 0 : parallelNanos / 1_000_000_000.0;
        double throughput = parallelNanos == 0 ? 0 : inputSize / seconds;
        return new BenchmarkResultDto(algorithm, inputSize, parallelNanos, throughput, resultSize,
                sequentialNanos, parallelNanos, speedup, work, span, parallelism);
    }
}