package com.loginsight.dsa.flow;

/**
 * Immutable result of a minimum {@code s}-{@code t} cut computation.
 *
 * <p>By max-flow/min-cut the cut capacity equals the maximum flow value. The partition is returned as
 * {@code sourceSide[v]}/{@code sinkSide[v]} (every vertex is on exactly one side), together with the
 * original edges crossing from the source side to the sink side and the sum of their capacities.</p>
 *
 * <p>{@code cutEdges} are exactly the saturated edges of the final residual reachability cut; the cut
 * is the unique canonical one induced by the residual graph of the accompanying max flow.</p>
 */
public final class MinCutResult {

    private final long maxFlow;
    private final int source;
    private final int sink;
    private final int vertexCount;
    private final boolean[] sourceSide;
    private final boolean[] sinkSide;
    private final Edge[] cutEdges;
    private final long cutCapacity;
    private final long executionTimeNanos;
    private final String timeComplexity;

    MinCutResult(long maxFlow, int source, int sink, int vertexCount, boolean[] sourceSide,
                 boolean[] sinkSide, Edge[] cutEdges, long cutCapacity, long executionTimeNanos,
                 String timeComplexity) {
        this.maxFlow = maxFlow;
        this.source = source;
        this.sink = sink;
        this.vertexCount = vertexCount;
        this.sourceSide = sourceSide;
        this.sinkSide = sinkSide;
        this.cutEdges = cutEdges;
        this.cutCapacity = cutCapacity;
        this.executionTimeNanos = executionTimeNanos;
        this.timeComplexity = timeComplexity;
    }

    /** The maximum flow value, which equals {@link #getCutCapacity()} by max-flow/min-cut. */
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

    public boolean isOnSourceSide(int vertex) {
        FlowValidator.requireInRange(vertex, 0, vertexCount - 1, "vertex");
        return sourceSide[vertex];
    }

    public boolean isOnSinkSide(int vertex) {
        FlowValidator.requireInRange(vertex, 0, vertexCount - 1, "vertex");
        return sinkSide[vertex];
    }

    /** A copy of the partition, so callers cannot mutate this result's state. */
    public boolean[] sourceSide() {
        boolean[] copy = new boolean[vertexCount];
        System.arraycopy(sourceSide, 0, copy, 0, vertexCount);
        return copy;
    }

    /** Original edges crossing source side -> sink side, in graph insertion order. */
    public Edge[] getCutEdges() {
        return cutEdges;
    }

    /** Sum of capacities of the crossing edges; equals the maximum flow. */
    public long getCutCapacity() {
        return cutCapacity;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public String getTimeComplexity() {
        return timeComplexity;
    }

    @Override
    public String toString() {
        return "MinCut: capacity=" + cutCapacity + " (=maxFlow " + maxFlow + "), " + cutEdges.length
                + " crossing edges [" + timeComplexity + "]";
    }
}
