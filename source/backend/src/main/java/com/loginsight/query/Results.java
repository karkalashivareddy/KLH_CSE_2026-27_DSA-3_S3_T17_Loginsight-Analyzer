package com.loginsight.query;

import com.loginsight.model.AlgorithmMetrics;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;

/**
 * Builds the canonical {@link QueryResult} envelope for the query engines. Time is measured around
 * the real computation (either here via {@code System.nanoTime()} or passed through from an engine
 * that already measured itself); memory estimate is an approximation of the primary working arrays
 * the algorithm allocated, never a fabricated number.
 */
public final class Results {

    private Results() {
    }

    /** Engines that time their own work can pass the measured nanos through. */
    public static QueryResult timed(QueryType queryType, AlgorithmType algorithm, int inputSize,
                                    long executionTimeNanos, Object result, Object intermediate,
                                    long memoryEstimateBytes, String timeComplexity,
                                    String spaceComplexity, String notes) {
        return new QueryResult(algorithm, queryType, inputSize, result, intermediate,
                executionTimeNanos, memoryEstimateBytes, timeComplexity, spaceComplexity, notes);
    }

    /** Engines that don't self-measure pass the start mark; we measure the window here. */
    public static QueryResult measured(QueryType queryType, AlgorithmType algorithm, int inputSize,
                                       long startNanos, Object result, Object intermediate,
                                       long memoryEstimateBytes, String timeComplexity,
                                       String spaceComplexity, String notes) {
        return timed(queryType, algorithm, inputSize, System.nanoTime() - startNanos, result,
                intermediate, memoryEstimateBytes, timeComplexity, spaceComplexity, notes);
    }

    public static AlgorithmMetrics metrics(QueryResult queryResult) {
        long nanos = Math.max(queryResult.getExecutionTimeNanos(), 1);
        double seconds = nanos / 1_000_000_000.0;
        double throughput = seconds == 0 ? 0 : queryResult.getInputSize() / seconds;
        return new AlgorithmMetrics(queryResult.getInputSize(), nanos, resultSize(queryResult),
                queryResult.getMemoryEstimateBytes(), throughput, 0);
    }

    private static int resultSize(QueryResult queryResult) {
        Object result = queryResult.getResult();
        if (result instanceof int[] arr) {
            return arr.length;
        }
        if (result instanceof long[] arr) {
            return arr.length;
        }
        if (result instanceof String s) {
            return s.length();
        }
        if (result instanceof java.util.Collection<?> c) {
            return c.size();
        }
        if (result instanceof java.util.Map<?, ?> m) {
            return m.size();
        }
        return 1;
    }
}