package com.loginsight.query.engine.randomized;

import java.util.Arrays;
import java.util.Map;

import com.loginsight.dsa.randomized.RandomSource;
import com.loginsight.dsa.randomized.RandomizedQuickSort;
import com.loginsight.dto.request.QuicksortRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Randomized quicksort (docs/12 §5): the pivot is drawn per partition from the seeded source, making
 * the result exactly reproducible for the same seed while avoiding the deterministic worst-case
 * pattern. The sorted array is the payload; the input size and verified-sorted flag are reported.
 */
public final class QuicksortEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.RANDOMIZED_SORT;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.RANDOMIZED_QUICKSORT;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        QuicksortRequest request = (QuicksortRequest) context.getRequest();
        long[] values = request.values();
        if (values == null || values.length == 0) {
            throw new com.loginsight.exception.InvalidQueryException("values must not be empty");
        }
        QueryValidator.requireBounds(1, values.length, 200_000, "array size");
        long seed = request.seed();

        long[] input = values.clone();
        long start = System.nanoTime();
        long[] sorted = new RandomizedQuickSort().sort(input, RandomSource.seeded(seed));
        long elapsed = System.nanoTime() - start;
        boolean verified = isSorted(sorted);

        Map<String, Object> payload = Map.of("sorted", sorted, "inputSize", values.length,
                "verifiedSorted", verified);
        return Results.measured(type(), algorithm(), values.length, start, payload,
                Map.of("seed", seed), 8L * values.length, "O(n log n) expected",
                "O(log n) recursion stack", "randomized pivot selection with seed " + seed);
    }

    private static boolean isSorted(long[] array) {
        for (int i = 1; i < array.length; i++) {
            if (array[i - 1] > array[i]) {
                return false;
            }
        }
        return true;
    }
}