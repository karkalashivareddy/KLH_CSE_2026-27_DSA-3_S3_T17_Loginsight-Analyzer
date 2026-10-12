package com.loginsight.query.engine.parallel;

import java.util.Map;
import java.util.Random;

import com.loginsight.dsa.parallel.ParallelPrefixScan;
import com.loginsight.dto.request.ParallelScanRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Inclusive prefix sum with the parallel scan (docs/12 §8). The parallel result is verified against
 * the sequential scan and reference witness values (first and last element) are reported so
 * consumers can assert the semantics of the result.
 */
public final class ScanEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.PARALLEL_SCAN;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.PARALLEL_PREFIX_SCAN;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        ParallelScanRequest request = (ParallelScanRequest) context.getRequest();
        int size = request.size() == null ? 0 : request.size();
        QueryValidator.requireSizes(size, 20_000_000);
        int parallelism = QueryValidator.resolveParallelism(request.parallelism());

        Random random = new Random(11L);
        long[] input = new long[size];
        for (int i = 0; i < size; i++) {
            input[i] = random.nextLong(1_000);
        }

        long[] sequential = ParallelPrefixScan.inclusiveSequential(input);
        long start = System.nanoTime();
        long[] scan = ParallelPrefixScan.inclusive(input, parallelism);
        long elapsed = System.nanoTime() - start;
        boolean verified = java.util.Arrays.equals(sequential, scan);

        Map<String, Object> payload = Map.of("size", size, "result", scan, "verified", verified,
                "first", scan.length == 0 ? 0 : scan[0],
                "last", scan.length == 0 ? 0 : scan[scan.length - 1],
                "serialFirst", sequential.length == 0 ? 0 : sequential[0],
                "serialLast", sequential.length == 0 ? 0 : sequential[sequential.length - 1]);
        return Results.measured(type(), algorithm(), size, start, payload,
                Map.of("parallelism", parallelism, "scanPolicy", "inclusive"), 8L * size,
                "O(n) work / O(n) serial-space", "O(log n) span",
                "parallel scan equals serial scan: " + verified);
    }
}