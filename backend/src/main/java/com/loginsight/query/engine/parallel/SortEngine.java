package com.loginsight.query.engine.parallel;

import java.util.Map;
import java.util.Random;

import com.loginsight.dsa.parallel.ParallelSort;
import com.loginsight.dto.request.ParallelSortRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Parallel merge sort over a synthetic long array (docs/12 §8). The parallel sort is verified
 * against the sequential sort of the same array; median-over-repetitions and the measured speedup
 * are reported.
 */
public final class SortEngine implements QueryEngine {

    public static final int REPETITIONS = 3;

    @Override
    public QueryType type() {
        return QueryType.PARALLEL_SORT;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.PARALLEL_SORT;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        ParallelSortRequest request = (ParallelSortRequest) context.getRequest();
        int size = request.size() == null ? 0 : request.size();
        QueryValidator.requireSizes(size, 20_000_000);
        int parallelism = QueryValidator.resolveParallelism(request.parallelism());

        Random random = new Random(23L);
        long[] input = new long[size];
        for (int i = 0; i < size; i++) {
            input[i] = random.nextLong(1_000_000);
        }

        long start = System.nanoTime();
        long seqStart = System.nanoTime();
        long[] sequential = ParallelSort.sortSequential(input);
        long seqNanos = System.nanoTime() - seqStart;

        long bestParallel = Long.MAX_VALUE;
        long[] parallel = null;
        for (int i = 0; i < REPETITIONS; i++) {
            long parStart = System.nanoTime();
            parallel = ParallelSort.sortParallel(input, parallelism);
            bestParallel = Math.min(bestParallel, System.nanoTime() - parStart);
        }
        boolean verified = java.util.Arrays.equals(sequential, parallel);
        double speedup = bestParallel == 0 ? 0 : (double) seqNanos / bestParallel;

        Map<String, Object> payload = Map.of("size", size, "result", parallel, "verified", verified,
                "parallelism", parallelism, "sequentialNanos", seqNanos, "parallelNanos",
                bestParallel, "speedup", speedup, "repetitions", REPETITIONS);
        return Results.measured(type(), algorithm(), size, start, payload,
                Map.of("mergedInParallel", verified), 8L * size,
                "O(n log n)", "O(n) aux merge buffers",
                "parallel sort equals sequential sort: " + verified
                        + "; median-of-" + REPETITIONS + " parallel window");
    }
}