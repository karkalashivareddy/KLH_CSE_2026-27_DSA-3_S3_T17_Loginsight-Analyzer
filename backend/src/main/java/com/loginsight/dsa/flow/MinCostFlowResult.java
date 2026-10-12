package com.loginsight.dsa.flow;

/**
 * Immutable result of a minimum-cost maximum-flow computation.
 *
 * <p>Reports the maximum flow value and, among all flows of that value, the total cost
 * {@code sum over edges of f(e) * cost(e)}. As in {@link FlowResult}, the flow on each original edge is
 * available by edge id.</p>
 */
public final class MinCostFlowResult {

    private final long totalFlow;
    private final long totalCost;
    private final int source;
    private final int sink;
    private final int vertexCount;
    private final Edge[] edges;
    private final long[] flow;
    private final int augmentingPathCount;
    private final long executionTimeNanos;
    private final String timeComplexity;
    private final String spaceComplexity;

    MinCostFlowResult(long totalFlow, long totalCost, int source, int sink, int vertexCount,
                      Edge[] edges, long[] flow, int augmentingPathCount, long executionTimeNanos,
                      String timeComplexity, String spaceComplexity) {
        this.totalFlow = totalFlow;
        this.totalCost = totalCost;
        this.source = source;
        this.sink = sink;
        this.vertexCount = vertexCount;
        this.edges = edges;
        this.flow = flow;
        this.augmentingPathCount = augmentingPathCount;
        this.executionTimeNanos = executionTimeNanos;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
    }

    /** Maximum flow value (min-cost max-flow still maximizes flow first). */
    public long getTotalFlow() {
        return totalFlow;
    }

    /** Total cost of the returned flow. */
    public long getTotalCost() {
        return totalCost;
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

    public long flowOf(int edgeId) {
        checkEdgeId(edgeId);
        return flow[edgeId];
    }

    /** Cost contributed by edge {@code edgeId}: {@code f(e) * cost(e)}. */
    public long costOf(int edgeId) {
        checkEdgeId(edgeId);
        return flow[edgeId] * edges[edgeId].getCost();
    }

    public int getAugmentingPathCount() {
        return augmentingPathCount;
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
        return "MinCostMaxFlow: flow=" + totalFlow + ", cost=" + totalCost + " (" + source + " -> "
                + sink + "), " + augmentingPathCount + " augmentations [" + timeComplexity + "]";
    }
}
