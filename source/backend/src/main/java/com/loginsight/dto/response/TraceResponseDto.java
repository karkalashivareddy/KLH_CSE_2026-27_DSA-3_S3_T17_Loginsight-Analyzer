package com.loginsight.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Trace API response: the outcome of a trace-capable algorithm run together with the ordered
 * {@code steps} recorded while the real algorithm executed. The laboratory front-end replays these
 * steps; it never invents animation state.
 *
 * @param algorithm        canonical algorithm name
 * @param category         academic category (Strings / Dynamic Programming / Graph &amp; Flow / …)
 * @param result           the algorithm result payload
 * @param intermediateData final teaching structure (LPS, DP matrix, residual edges, …)
 * @param steps            ordered {@code AlgorithmStep} records in wire form
 * @param truncated        true when the step list was bounded by the recorder ceiling
 * @param executionTimeNanos measured wall-clock time of the traced run
 * @param timeComplexity   complexity statement honoured by the implementation
 * @param spaceComplexity  space statement honoured by the implementation
 */
public record TraceResponseDto(String algorithm, String category, Object result,
                               Object intermediateData, List<Map<String, Object>> steps,
                               boolean truncated, long executionTimeNanos, String timeComplexity,
                               String spaceComplexity) {
}