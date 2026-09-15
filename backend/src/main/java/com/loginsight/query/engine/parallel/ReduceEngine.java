package com.loginsight.query.engine.parallel;

import java.util.Map;
import java.util.Random;

import com.loginsight.dsa.parallel.ParallelReduce;
import com.loginsight.dsa.parallel.WorkSpanAnalyzer;
import com.loginsight.dto.request.ParallelReduceRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Parallel reduction over a synthetic long array (docs/12 §8): a sequential fold is compared with
 * the parallel chunk-tree reduction and equality is verified. For the log scenario (ERROR_COUNT)
 * the marker equals the error-line marker the client supplies. Work/span come from the reduce-tree
 * schedule the engine runs; speedup is the measured sequential/parallel wall-clock ratio.
 */
public final class ReduceEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.PARALLEL_REDUCE;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.PARALLEL_REDUCE;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        ParallelReduceRequest request = (ParallelReduceRequest) context.getRequest();
        int size = request.size() == null ? 0 : request.size();
        QueryValidator.requireSizes(size, 20_000_000);
        int parallelism = request.parallelism() == null ? 0 : request.parallelism();
        if (parallelism <= 0) {
            parallelism = Runtime.getRuntime().availableProcessors();
        }
        ParallelReduce.ReduceOp op = ParallelReduce.ReduceOp.valueOf(request.op().toUpperCase());
        long marker = request.marker() == null ? 1L : request.marker();

        Random random = new Random(7L);
        long[] input = new long[size];
        for (int i = 0; i < size; i++) {
            input[i] = random.nextLong(1_000_000);
        }

        long seqStart = System.nanoTime();
        long sequential = ParallelReduce.reduceSequential(input, op, marker);
        long seqNanos = System.nanoTime() - seqStart;
        long parStart = System.nanoTime();
        long parallel = ParallelReduce.reduce(input, op, marker, parallelism);
        long parNanos = System.nanoTime() - parStart;
        double measuredSpeedup = parNanos == 0 ? 0 : (double) seqNanos / parNanos;

        WorkSpanAnalyzer.WorkSpanResult ws = WorkSpanAnalyzer.analyze(
                WorkSpanAnalyzer.Task.binaryReduceTree("reduce", Math.max(1, parallelism), 1, 1));
        Map<String, Object> payload = Map.of("op", op.name(), "size", size, "result", parallel,
                "sequential", sequential, "verified", sequential == parallel,
                "parallelism", parallelism, "speedup", measuredSpeedup,
                "theoreticalWork", ws.getWork(), "theoreticalSpan", ws.getSpan(),
                "theoreticalParallelism", ws.getParallelism());
        return Results.measured(type(), algorithm(), size, parStart, payload,
                Map.of("marker", marker, "reduceTree", "binary tree over " + parallelism + " leaves"),
                8L * size, "O(n/p + log p)", "O(log n)",
                "sequential == parallel verified: " + (sequential == parallel));
    }
}