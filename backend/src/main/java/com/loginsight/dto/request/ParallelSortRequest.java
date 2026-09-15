package com.loginsight.dto.request;

/**
 * Body of the parallel engines' sort request (docs/12 §8).
 */
public record ParallelSortRequest(Integer size, Integer parallelism) {

    public ParallelSortRequest {
        size = size == null ? 100_000 : size;
    }
}