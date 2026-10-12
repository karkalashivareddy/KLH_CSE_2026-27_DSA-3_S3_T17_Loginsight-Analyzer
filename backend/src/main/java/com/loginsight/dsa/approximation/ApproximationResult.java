package com.loginsight.dsa.approximation;

/**
 * Immutable result of {@link VertexCoverApproximation}: the chosen cover, its size, the matching that
 * produced it, the forced vertices (self-loop vertices), a proven <em>lower bound</em> on the optimum,
 * and the reported ratio.
 *
 * <h2>What the ratio means</h2>
 * Every vertex cover must contain an endpoint of each edge of the greedy maximal matching {@code M}
 * (and every self-loop vertex), so {@code OPT >= lowerBound}. The algorithm returns
 * {@code |C| = |forced| + 2|M|}, hence {@code |C| <= 2 * lowerBound <= 2 * OPT}:
 * the ratio {@code |C| / lowerBound} is an <em>observed upper bound</em> on the true approximation
 * ratio {@code |C| / OPT}, guaranteed to be at most 2.
 *
 * <p>The guarantee is a worst-case bound, not a claim that every instance produces ratio 2. For the
 * empty graph both {@code |C|} and {@code lowerBound} are 0; the ratio is reported as exactly 1.0 by
 * convention (a trivial, optimal cover).</p>
 *
 * <p>No optimum is ever computed here — the algorithm must not know OPT. An optimum can only appear
 * through an independent test oracle (see the approximation package tests).</p>
 */
public final class ApproximationResult {

    private final int[] cover;
    private final int coverSize;
    private final int matchingSize;
    private final int forcedSize;
    private final int lowerBound;
    private final double ratio;
    private final long executionTimeNanos;
    private final String timeComplexity;
    private final String spaceComplexity;
    private final String notes;

    ApproximationResult(int[] cover, int matchingSize, int forcedSize, long executionTimeNanos,
                        String timeComplexity, String spaceComplexity, String notes) {
        this.cover = copyAndSort(cover);
        this.coverSize = this.cover.length;
        this.matchingSize = matchingSize;
        this.forcedSize = forcedSize;
        this.lowerBound = matchingSize + forcedSize;
        this.ratio = coverSize == 0 ? 1.0 : (double) coverSize / lowerBound;
        this.executionTimeNanos = executionTimeNanos;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.notes = notes;
    }

    /** Sorted copy so callers cannot mutate the result. */
    private static int[] copyAndSort(int[] source) {
        int[] copy = new int[source.length];
        System.arraycopy(source, 0, copy, 0, source.length);
        for (int i = 1; i < copy.length; i++) {
            int value = copy[i];
            int j = i - 1;
            while (j >= 0 && copy[j] > value) {
                copy[j + 1] = copy[j];
                j--;
            }
            copy[j + 1] = value;
        }
        return copy;
    }

    /** The selected cover vertices, sorted. */
    public int[] getCover() {
        int[] result = new int[cover.length];
        System.arraycopy(cover, 0, result, 0, cover.length);
        return result;
    }

    public int getCoverSize() {
        return coverSize;
    }

    /** Number of edges in the greedy maximal matching used by the algorithm. */
    public int getMatchingSize() {
        return matchingSize;
    }

    /** Number of vertices forced in because they carry a self-loop. */
    public int getForcedSize() {
        return forcedSize;
    }

    /**
     * Proven lower bound on {@code OPT}: {@code matchingSize + forcedSize}. Always {@code <= OPT}.
     */
    public int getLowerBound() {
        return lowerBound;
    }

    /**
     * {@code coverSize / lowerBound}, the observed upper bound on the approximation ratio
     * {@code |C| / OPT}. Guaranteed {@code <= 2.0}; 1.0 for the empty graph by convention.
     */
    public double getApproximationRatio() {
        return ratio;
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

    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "VertexCoverApproximation: size=" + coverSize + " (lower bound " + lowerBound
                + ", ratio " + ratio + ", matching " + matchingSize + ", forced " + forcedSize + ")";
    }
}