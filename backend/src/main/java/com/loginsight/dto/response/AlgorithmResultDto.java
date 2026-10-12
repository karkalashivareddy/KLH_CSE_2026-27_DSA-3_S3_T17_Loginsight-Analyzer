package com.loginsight.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.loginsight.model.QueryResult;

/**
 * The canonical AlgorithmResult envelope (docs/02 §5, docs/12). This is the <em>only</em> object
 * serialised for an algorithmic query; it is a flat serialisable view of {@link QueryResult} and no
 * {@code dsa} object ever crosses the wire directly.
 */
@JsonInclude(Include.NON_NULL)
public record AlgorithmResultDto(String algorithm, String queryType, long inputSize, String pattern,
                                 Object result, Object intermediateData, long executionTimeNanos,
                                 long memoryEstimateBytes, String timeComplexity,
                                 String spaceComplexity, String notes) {

    public static AlgorithmResultDto from(QueryResult qr, String pattern) {
        return new AlgorithmResultDto(qr.getAlgorithm().name(), qr.getQueryType().name(),
                qr.getInputSize(), pattern, qr.getResult(), qr.getIntermediateData(),
                qr.getExecutionTimeNanos(), qr.getMemoryEstimateBytes(), qr.getTimeComplexity(),
                qr.getSpaceComplexity(), qr.getNotes());
    }

    public static AlgorithmResultDto from(QueryResult qr) {
        return new AlgorithmResultDto(qr.getAlgorithm().name(), qr.getQueryType().name(),
                qr.getInputSize(), null, qr.getResult(), qr.getIntermediateData(),
                qr.getExecutionTimeNanos(), qr.getMemoryEstimateBytes(), qr.getTimeComplexity(),
                qr.getSpaceComplexity(), qr.getNotes());
    }
}