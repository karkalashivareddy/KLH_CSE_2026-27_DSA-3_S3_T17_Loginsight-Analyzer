package com.loginsight.trace;

import java.util.List;

/**
 * Result of a trace-capable algorithm run.
 *
 * <p>Pairs the same qualitative result the untraced variant produces (algorithm name, result
 * payload, teaching structure) with the ordered list of {@link AlgorithmStep}s recorded while the
 * algorithm executed. The step list is always bounded in production to keep the trace payload
 * sane; callers that need a full trace should pass a bounded demonstration input.</p>
 *
 * @param algorithm        canonical algorithm name (matches the untraced variant)
 * @param result           the algorithm result payload (positions, distance, max-flow, …)
 * @param finalIntermediate the final teaching structure (LPS, DP matrix, residual edges, …)
 * @param steps            ordered operations observed during the run
 * @param executionTimeNanos measured wall-clock time of the traced run
 * @param timeComplexity   the complexity statement the implementation honours
 * @param spaceComplexity  the space statement the implementation honours
 */
public record TracedResult(String algorithm, Object result, Object finalIntermediate,
                           List<AlgorithmStep> steps, long executionTimeNanos,
                           String timeComplexity, String spaceComplexity) {
}