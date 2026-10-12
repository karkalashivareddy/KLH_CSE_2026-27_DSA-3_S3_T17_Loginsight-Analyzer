package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/benchmark/run} (docs/12 §10). {@code scenario} selects the dataset family
 * to drive (default {@code dataset}); {@code sizes} lists the input sizes to sweep (default
 * {@code 1k/10k/100k}); {@code repetitions} repeats each size (median reported). Parallel rows name
 * their {@code parallelism}.
 */
public record BenchmarkRequest(String scenario, int[] sizes, Integer repetitions,
                               Integer parallelism) {

    public static final int[] DEFAULT_SIZES = {1_000, 10_000, 100_000};

    public BenchmarkRequest {
        scenario = scenario == null ? "dataset" : scenario;
        repetitions = repetitions == null ? 3 : repetitions;
        if (sizes == null || sizes.length == 0) {
            sizes = DEFAULT_SIZES;
        }
    }
}