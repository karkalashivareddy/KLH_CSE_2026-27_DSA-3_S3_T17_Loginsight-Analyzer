package com.loginsight.dsa.flow;

/**
 * Immutable result of a maximum-flow computation. It never exposes the mutable residual network;
 * instead it reports, aligned to the owning graph's original edge ids:
 *
 * <ul>
 *   <li>{@link #edges()} — the original {@link Edge} definitions (capacity, cost);</li>
 *   <li>{@link #flowOf(int)} — the final flow {@code f(e)} on each original edge;</li>
 *   <li>{@link #residualOf(int)} — the final forward residual {@code c(e) - f(e)}.</li>
 * </ul>
 *
 * <p>Together these let a caller check {@code 0 <= f(e) <= c(e)} and conservation without touching
 * algorithm state. The flow value, source, sink, augmented-path count, declared complexity and
 * measured time are also reported.</p>
 */
public final class FlowResult {

    private final String algorithm;
    private final long maxFlow;
    private final int source;
    private final int sink;
    private final int vertexCount;
    private final Edge[] edges;
    private final long[] flow;
    private final long[] residual;
    private final int augmentationCount;
    private final long executionTimeNanos;
    private final String timeComplexity;
    private final String spaceComplexity;

    private FlowResult(String algorithm, long maxFlow, int source, int sink, int vertexCount,
                       Edge[] edges, long[] flow, long[] residual, int augmentationCount,
                       long executionTimeNanos, String timeComplexity, String spaceComplexity) {
        this.algorithm = algorithm;
        this.maxFlow = maxFlow;
        this.source = source;
        this.sink = sink;
        this.vertexCount = vertexCount;
        this.edges = edges;
        this.flow = flow;
        this.residual = residual;
        this.augmentationCount = augmentationCount;
        this.executionTimeNanos = executionTimeNanos;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
    }

    /** Builds a result snapshot from a completed residual network (package-internal). */
    static FlowResult of(String algorithm, FlowGraph graph, ResidualNetwork residualNetwork,
                         long maxFlow, int source, int sink, int augmentationCount,
                         long executionTimeNanos, String timeComplexity, String spaceComplexity) {
        int m = graph.edgeCount();
        long[] flow = new long[m];
        long[] residual = new long[m];
        for (int id = 0; id < m; id++) {
            flow[id] = residualNetwork.flowOfOriginal(id);
            residual[id] = residualNetwork.residualOfOriginal(id);
        }
        return new FlowResult(algorithm, maxFlow, source, sink, graph.vertexCount(), graph.edges(),
                flow, residual, augmentationCount, executionTimeNanos, timeComplexity, spaceComplexity);
    }

    public String getAlgorithm() {
        return algorithm;
    }

    /** The maximum flow value {@code |f|}. */
    public long getMaxFlow() {
        return maxFlow;
    }

    public int getSource() {
        return source;
    }

    public int getSink() {
        return sink;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public Edge[] getEdges() {
        return edges;
    }

    public int getEdgeCount() {
        return edges.length;
    }

    /** Final flow on original edge {@code edgeId}. */
    public long flowOf(int edgeId) {
        checkEdgeId(edgeId);
        return flow[edgeId];
    }

    /** Final forward residual capacity of original edge {@code edgeId} ({@code c - f}). */
    public long residualOf(int edgeId) {
        checkEdgeId(edgeId);
        return residual[edgeId];
    }

    /** Number of augmenting steps performed (paths for FF/EK, blocking-flow totals for Dinic). */
    public int getAugmentationCount() {
        return augmentationCount;
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

    private void checkEdgeId(int edgeId) {
        FlowValidator.requireInRange(edgeId, 0, edges.length - 1, "edgeId");
    }

    @Override
    public String toString() {
        return algorithm + ": maxFlow=" + maxFlow + " (" + source + " -> " + sink + "), "
                + edges.length + " edges, " + augmentationCount + " augmentations"
                + " [" + timeComplexity + " time, " + spaceComplexity + " space]";
    }
}
