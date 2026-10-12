package com.loginsight.dsa.dp;

/**
 * Uniform result envelope for a single dynamic-programming computation, shaped like the string
 * engine's {@code StringSearchResult} so a student (or the later query layer) reasons about every DP
 * algorithm the same way: what was solved, the metric, the complexity, and the algorithm-specific
 * intermediate structure used for teaching/visualisation.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code algorithm} — a short human name ("Levenshtein", "Needleman-Wunsch", ...).</li>
 *   <li>{@code inputSize} — algorithm-specific: characters compared, number of keys, number of
 *       cities, number of nodes, number of bits, ...</li>
 *   <li>{@code result} — the primary answer (a distance/cost/score/metric); boxed primitive or array.</li>
 *   <li>{@code executionTimeNanos} — measured from the real run, never fabricated (docs/02 §5).</li>
 *   <li>{@code timeComplexity}/{@code spaceComplexity} — the declared complexities from the class
 *       javadoc; never reinterpreted to match an observed timing.</li>
 *   <li>{@code intermediateData} — the DP table, split table, path, root table, ... or {@code null}
 *       when the algorithm exposes no meaningful teaching structure.</li>
 * </ul>
 *
 * <p>This is intentionally a small, flat container, not a framework. Algorithms that need extra,
 * strongly-typed detail (e.g. an alignment, an edit script) use their own small result objects and
 * also expose a {@code solve}/DpResult view where useful.</p>
 */
public final class DpResult {

    private final String algorithm;
    private final int inputSize;
    private final Object result;
    private final long executionTimeNanos;
    private final String timeComplexity;
    private final String spaceComplexity;
    private final Object intermediateData;

    public DpResult(String algorithm, int inputSize, Object result, long executionTimeNanos,
                    String timeComplexity, String spaceComplexity, Object intermediateData) {
        this.algorithm = algorithm;
        this.inputSize = inputSize;
        this.result = result;
        this.executionTimeNanos = executionTimeNanos;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.intermediateData = intermediateData;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getInputSize() {
        return inputSize;
    }

    public Object getResult() {
        return result;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public String getTimeComplexity() {
        return timeComplexity;
    }

    public String getSpaceComplexity() {
        return spaceComplexity;
    }

    public Object getIntermediateData() {
        return intermediateData;
    }

    /** Convenience for the common case where the result is an integral metric (distance, cost, score). */
    public long resultAsLong() {
        if (result instanceof Number) {
            return ((Number) result).longValue();
        }
        throw new IllegalStateException("result is not numeric: " + result);
    }

    @Override
    public String toString() {
        return algorithm + "(size=" + inputSize + ") = " + result
                + " [" + timeComplexity + " time, " + spaceComplexity + " space]";
    }
}
