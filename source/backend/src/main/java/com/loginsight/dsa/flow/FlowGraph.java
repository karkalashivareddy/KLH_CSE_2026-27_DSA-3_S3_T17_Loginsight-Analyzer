package com.loginsight.dsa.flow;

/**
 * Immutable directed capacitated graph — the single shared definition consumed by every algorithm in
 * this package. It stores only the <em>original</em> edges; each algorithm builds its own
 * {@link ResidualNetwork} from it, so running one solver never corrupts another's state and callers
 * cannot reach mutable residual internals.
 *
 * <p><strong>Model.</strong> Vertices are {@code 0..n-1}. Each {@link #addEdge(int, int, long)} adds
 * one directed edge {@code from -> to} with a non-negative capacity. Parallel edges are allowed (they
 * are genuinely different edges and their flows add); zero-capacity edges are allowed (residual-only);
 * an optional per-unit {@code cost} supports {@link MinCostMaxFlow}. Where a reverse edge is needed,
 * it is the residual edge created internally by the solver, not a second original edge.</p>
 *
 * <p><strong>Validation.</strong> Vertex indices must be in range and capacities non-negative; a
 * negative capacity is rejected rather than clamped. Capacities and costs are {@code long} to keep
 * arithmetic well clear of {@code int} overflow.</p>
 *
 * <p><strong>Determinism.</strong> Edges are kept in insertion order and solvers iterate adjacency in
 * that order, so results (and, for matching, the chosen pairs) are reproducible.</p>
 */
public final class FlowGraph {

    private static final int INITIAL_EDGE_CAPACITY = 16;

    private final int vertexCount;
    private int[] edgeFrom;
    private int[] edgeTo;
    private long[] edgeCapacity;
    private long[] edgeCost;
    private int edgeCount;

    public FlowGraph(int vertexCount) {
        FlowValidator.requireNonNegative(vertexCount, "vertexCount");
        this.vertexCount = vertexCount;
        this.edgeFrom = new int[INITIAL_EDGE_CAPACITY];
        this.edgeTo = new int[INITIAL_EDGE_CAPACITY];
        this.edgeCapacity = new long[INITIAL_EDGE_CAPACITY];
        this.edgeCost = new long[INITIAL_EDGE_CAPACITY];
    }

    /** Adds a directed edge with zero cost. Returns its id. */
    public int addEdge(int from, int to, long capacity) {
        return addEdge(from, to, capacity, 0L);
    }

    /** Adds a directed edge with an explicit per-unit cost. Returns its id. */
    public int addEdge(int from, int to, long capacity, long cost) {
        FlowValidator.requireInRange(from, 0, vertexCount - 1, "from");
        FlowValidator.requireInRange(to, 0, vertexCount - 1, "to");
        FlowValidator.requireNonNegative(capacity, "capacity");
        if (edgeCount == edgeFrom.length) {
            grow();
        }
        int id = edgeCount++;
        edgeFrom[id] = from;
        edgeTo[id] = to;
        edgeCapacity[id] = capacity;
        edgeCost[id] = cost;
        return id;
    }

    public int vertexCount() {
        return vertexCount;
    }

    /** Number of original directed edges (parallel edges counted separately). */
    public int edgeCount() {
        return edgeCount;
    }

    public int edgeFrom(int id) {
        checkEdgeId(id);
        return edgeFrom[id];
    }

    public int edgeTo(int id) {
        checkEdgeId(id);
        return edgeTo[id];
    }

    public long edgeCapacity(int id) {
        checkEdgeId(id);
        return edgeCapacity[id];
    }

    public long edgeCost(int id) {
        checkEdgeId(id);
        return edgeCost[id];
    }

    public Edge edge(int id) {
        checkEdgeId(id);
        return new Edge(id, edgeFrom[id], edgeTo[id], edgeCapacity[id], edgeCost[id]);
    }

    /** Snapshot of the original edges in insertion order. */
    public Edge[] edges() {
        Edge[] result = new Edge[edgeCount];
        for (int i = 0; i < edgeCount; i++) {
            result[i] = new Edge(i, edgeFrom[i], edgeTo[i], edgeCapacity[i], edgeCost[i]);
        }
        return result;
    }

    private void checkEdgeId(int id) {
        FlowValidator.requireInRange(id, 0, edgeCount - 1, "edgeId");
    }

    private void grow() {
        int expanded = edgeFrom.length * 2;
        edgeFrom = copyOf(edgeFrom, expanded);
        edgeTo = copyOf(edgeTo, expanded);
        edgeCapacity = copyOf(edgeCapacity, expanded);
        edgeCost = copyOf(edgeCost, expanded);
    }

    private static int[] copyOf(int[] source, int newLength) {
        int[] target = new int[newLength];
        System.arraycopy(source, 0, target, 0, source.length);
        return target;
    }

    private static long[] copyOf(long[] source, int newLength) {
        long[] target = new long[newLength];
        System.arraycopy(source, 0, target, 0, source.length);
        return target;
    }
}
