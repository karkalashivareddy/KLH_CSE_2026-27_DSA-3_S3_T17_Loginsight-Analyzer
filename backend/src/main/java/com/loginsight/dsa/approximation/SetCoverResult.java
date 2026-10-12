package com.loginsight.dsa.approximation;

/**
 * Immutable result of the greedy set-cover demonstration ({@link SetCoverDemo}): which sets were
 * chosen, how much of the universe they cover, and the proven harmonic approximation-ratio bound.
 *
 * <p>As with {@link ApproximationResult}, the bound is reported, never the optimum — the algorithm
 * does not know OPT.</p>
 */
public final class SetCoverResult {

    private final int universeSize;
    private final int[] selectedSetIndices;
    private final int coveredCount;
    private final boolean allCovered;
    private final int greedySteps;
    private final double harmonicBound;
    private final String notes;
    private final long executionTimeNanos;

    SetCoverResult(int universeSize, int[] selectedSetIndices, int coveredCount,
                   boolean allCovered, int greedySteps, double harmonicBound, String notes,
                   long executionTimeNanos) {
        this.universeSize = universeSize;
        this.selectedSetIndices = selectedSetIndices;
        this.coveredCount = coveredCount;
        this.allCovered = allCovered;
        this.greedySteps = greedySteps;
        this.harmonicBound = harmonicBound;
        this.notes = notes;
        this.executionTimeNanos = executionTimeNanos;
    }

    public int getUniverseSize() {
        return universeSize;
    }

    /** Chosen set indices in the order the greedy round picked them. */
    public int[] getSelectedSetIndices() {
        return selectedSetIndices;
    }

    public int getSelectedSetCount() {
        return selectedSetIndices.length;
    }

    public int getCoveredCount() {
        return coveredCount;
    }

    /** True when every universe element is covered by the chosen sets. */
    public boolean isAllCovered() {
        return allCovered;
    }

    public int getGreedySteps() {
        return greedySteps;
    }

    /** The {@code H(n)} harmonic upper bound on the approximation ratio of greedy set cover. */
    public double getHarmonicBound() {
        return harmonicBound;
    }

    public String getNotes() {
        return notes;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    @Override
    public String toString() {
        return "SetCoverDemo: " + coveredCount + "/" + universeSize + " covered by "
                + selectedSetIndices.length + " sets, ratio bound ~ H(" + universeSize + ")="
                + harmonicBound;
    }
}