package com.loginsight.dsa.string;

/**
 * Result of a single-pattern string search, uniformly shaped across every matcher so a student (or
 * the later query layer) can reason about an algorithm the same way: problem, positions, complexity,
 * and an algorithm-specific intermediate structure.
 *
 * <p>{@code intermediateData} is deliberately algorithm-specific and type-weak here:
 * <ul>
 *   <li>Naive → {@code null}</li>
 *   <li>KMP → the LPS array {@code int[]}</li>
 *   <li>Z → the Z-array {@code int[]} of {@code pattern + separator + text}</li>
 *   <li>Rabin-Karp → the collision count {@link Integer} (candidates whose hashes matched but whose
 *       character-by-character verification rejected them)</li>
 *   <li>Aho-Corasick → {@code PatternMatch[]} of every recorded occurrence</li>
 * </ul>
 * {@code executionTimeNanos} is measured from the actual run, never fabricated (docs/02 §5).</p>
 */
public final class StringSearchResult {

    private final String algorithm;
    private final String pattern;
    private final int textLength;
    private final int[] matchPositions;
    private final int matchCount;
    private final long executionTimeNanos;
    private final String timeComplexity;
    private final String spaceComplexity;
    private final Object intermediateData;

    public StringSearchResult(String algorithm, String pattern, int textLength, int[] matchPositions,
                              long executionTimeNanos, String timeComplexity, String spaceComplexity,
                              Object intermediateData) {
        this.algorithm = algorithm;
        this.pattern = pattern;
        this.textLength = textLength;
        this.matchPositions = matchPositions == null ? new int[0] : matchPositions;
        this.matchCount = this.matchPositions.length;
        this.executionTimeNanos = executionTimeNanos;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.intermediateData = intermediateData;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getPattern() {
        return pattern;
    }

    public int getTextLength() {
        return textLength;
    }

    public int[] getMatchPositions() {
        return matchPositions;
    }

    public int getMatchCount() {
        return matchCount;
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

    /** Convenience for algorithms whose intermediate structure is an {@code int[]} (LPS, Z). */
    public int[] intermediateDataAsInts() {
        return intermediateData instanceof int[] ? (int[]) intermediateData : new int[0];
    }

    @Override
    public String toString() {
        return algorithm + "[" + matchCount + " matches at "
                + java.util.Arrays.toString(matchPositions) + "] on text of length " + textLength;
    }
}