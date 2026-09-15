package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/random/quicksort} (docs/12 §5). Sorts {@code values} with
 * randomized-quicksort using a seeded source (default seed {@code 42}) so results are
 * reproducible for the same input; the engine reports comparisons and the pivot recursion evidence.
 */
public record QuicksortRequest(long[] values, Long seed) {

    public QuicksortRequest {
        seed = seed == null ? 42L : seed;
    }
}