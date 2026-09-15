package com.loginsight.model;

/**
 * The domain object behind the canonical AlgorithmResult envelope (docs/02 §5). Produced by the
 * query engine, consumed by the DTO mapper — no {@code dsa} object is ever serialised directly.
 *
 * <p>Fields mirror the wire contract: algorithm, query type, input size, the result payload, the
 * intermediate structure the algorithm exposed (LPS, Z-array, DP matrix, residual steps, …), and
 * the measured execution time plus the complexity statements the implementation actually honours.</p>
 */
public final class QueryResult {

    private final AlgorithmType algorithm;
    private final QueryType queryType;
    private final int inputSize;
    private final Object result;
    private final Object intermediateData;
    private final long executionTimeNanos;
    private final long memoryEstimateBytes;
    private final String timeComplexity;
    private final String spaceComplexity;
    private final String notes;

    public QueryResult(AlgorithmType algorithm, QueryType queryType, int inputSize, Object result,
                       Object intermediateData, long executionTimeNanos, long memoryEstimateBytes,
                       String timeComplexity, String spaceComplexity, String notes) {
        this.algorithm = algorithm;
        this.queryType = queryType;
        this.inputSize = inputSize;
        this.result = result;
        this.intermediateData = intermediateData;
        this.executionTimeNanos = executionTimeNanos;
        this.memoryEstimateBytes = memoryEstimateBytes;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.notes = notes;
    }

    public AlgorithmType getAlgorithm() {
        return algorithm;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public int getInputSize() {
        return inputSize;
    }

    public Object getResult() {
        return result;
    }

    public Object getIntermediateData() {
        return intermediateData;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public long getMemoryEstimateBytes() {
        return memoryEstimateBytes;
    }

    public String getTimeComplexity() {
        return timeComplexity;
    }

    public String getSpaceComplexity() {
        return spaceComplexity;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return algorithm + " [" + queryType + "]: inputSize=" + inputSize + ", nanos="
                + executionTimeNanos + " (" + timeComplexity + " time, " + spaceComplexity + " space)";
    }
}