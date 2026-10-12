package com.loginsight.dsa.flow;

/**
 * Immutable result of a maximum bipartite matching.
 *
 * <p>Vertices are split into a left part {@code 0..leftCount-1} and a right part
 * {@code 0..rightCount-1}. The result reports the matching size and both partner maps
 * ({@code -1} = unmatched), the chosen pairs sorted by left vertex, and the unmatched vertices on each
 * side.</p>
 */
public final class MatchingResult {

    private final int leftCount;
    private final int rightCount;
    private final int matchingSize;
    private final int[] leftPartner;
    private final int[] rightPartner;
    private final int[][] matchedPairs;
    private final int[] unmatchedLeft;
    private final int[] unmatchedRight;
    private final long executionTimeNanos;

    MatchingResult(int leftCount, int rightCount, int matchingSize, int[] leftPartner,
                   int[] rightPartner, int[][] matchedPairs, int[] unmatchedLeft,
                   int[] unmatchedRight, long executionTimeNanos) {
        this.leftCount = leftCount;
        this.rightCount = rightCount;
        this.matchingSize = matchingSize;
        this.leftPartner = leftPartner;
        this.rightPartner = rightPartner;
        this.matchedPairs = matchedPairs;
        this.unmatchedLeft = unmatchedLeft;
        this.unmatchedRight = unmatchedRight;
        this.executionTimeNanos = executionTimeNanos;
    }

    public int getLeftCount() {
        return leftCount;
    }

    public int getRightCount() {
        return rightCount;
    }

    /** Number of matched edges (the maximum matching cardinality). */
    public int getMatchingSize() {
        return matchingSize;
    }

    /** Right vertex matched to left vertex {@code left}, or {@code -1}. */
    public int partnerOfLeft(int left) {
        FlowValidator.requireInRange(left, 0, leftCount - 1, "left");
        return leftPartner[left];
    }

    /** Left vertex matched to right vertex {@code right}, or {@code -1}. */
    public int partnerOfRight(int right) {
        FlowValidator.requireInRange(right, 0, rightCount - 1, "right");
        return rightPartner[right];
    }

    /** Matched pairs as {@code {left, right}}, sorted by left vertex. */
    public int[][] getMatchedPairs() {
        return matchedPairs;
    }

    public int[] getUnmatchedLeft() {
        return unmatchedLeft;
    }

    public int[] getUnmatchedRight() {
        return unmatchedRight;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    @Override
    public String toString() {
        return "BipartiteMatching: size=" + matchingSize + " (" + leftCount + " left, " + rightCount
                + " right)";
    }
}
