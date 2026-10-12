package com.loginsight.dsa.approximation;

/**
 * Immutable result of {@link VertexCoverKernelization}: the forced vertices and reduced problem state.
 *
 * <p>The reduction is answer-preserving in the sense of the viva contract: {@code tau(G) <= k}
 * <em>iff</em> the reduced graph has a vertex cover of size at most {@code remainingK}. Whenever the
 * original instance is a YES instance ({@code tau(G) <= k}) every forced vertex belongs to every such
 * cover, so the stronger identity {@code tau(G) = tau(reducedGraph) + |forced|} holds; on NO
 * instances the identity is not guaranteed (a poor-ratio cover may avoid a forced vertex) but the
 * decision equivalence always is. When {@link #isInfeasible()} is set the original instance is
 * necessarily a NO instance and the reduced bound is 0.</p>
 */
public final class KernelizationResult {

    private final int[] forcedVertices;
    private final UndirectedGraph reducedGraph;
    private final int remainingK;
    private final boolean infeasible;
    private final long executionTimeNanos;
    private final String notes;

    KernelizationResult(int[] forcedVertices, UndirectedGraph reducedGraph, int remainingK,
                        boolean infeasible, long executionTimeNanos, String notes) {
        this.forcedVertices = forcedVertices;
        this.reducedGraph = reducedGraph;
        this.remainingK = remainingK;
        this.infeasible = infeasible;
        this.executionTimeNanos = executionTimeNanos;
        this.notes = notes;
    }

    /** Vertices provably belonging to every cover of size {@code <= k}, in scan order. */
    public int[] getForcedVertices() {
        return forcedVertices;
    }

    public int getForcedCount() {
        return forcedVertices.length;
    }

    /** The reduced graph (labels preserved); degrees are now {@code <= remainingK} unless infeasible. */
    public UndirectedGraph getReducedGraph() {
        return reducedGraph;
    }

    public int getRemainingK() {
        return remainingK;
    }

    /**
     * True when the reduction alone already proves that no cover of size {@code <=} the original
     * {@code k} exists (budget exhausted while edges remain).
     */
    public boolean isInfeasible() {
        return infeasible;
    }

    /**
     * Restores the optimum of the original graph from an optimum of the reduced graph when the
     * reduction is on a YES instance: {@code tau(G) = reducedTau + forcedCount}.
     */
    public int originalTau(int reducedTau) {
        return reducedTau + forcedVertices.length;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "VertexCoverKernelization: forced=" + forcedVertices.length + ", k=" + remainingK
                + (infeasible ? " (infeasible)" : "");
    }
}