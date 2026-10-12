package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/parallel/scan} (docs/12 §8). Computes the inclusive prefix sum of a
 * {@code size}-length random array with the parallel Hillis-Steele scan and reports deterministic
 * witness values alongside work/span theory.
 */
public record ParallelScanRequest(Integer size, Integer parallelism) {
}