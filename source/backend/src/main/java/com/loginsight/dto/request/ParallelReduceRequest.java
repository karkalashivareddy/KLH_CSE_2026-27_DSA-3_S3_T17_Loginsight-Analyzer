package com.loginsight.dto.request;

/**
 * Body of the parallel reduction/scans/sort endpoints (docs/12 §8). Integer\[Threading\]-driven
 * sizes are capped by validation; {@code parallelism} is optional and falls back to a share of the
 * runtime cores. For logs ({@code ERROR_COUNT}) the backend counts error lines in the dataset.
 */
public record ParallelReduceRequest(String op, Integer size, Integer parallelism, Long marker) {

    public ParallelReduceRequest {
        op = op == null ? "SUM" : op;
        marker = marker == null ? 1L : marker;
    }
}