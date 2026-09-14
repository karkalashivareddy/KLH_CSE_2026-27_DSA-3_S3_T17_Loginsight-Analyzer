package com.loginsight.dsa.flow;

/**
 * The single residual-network representation shared by every max-flow algorithm in this package
 * (docs/03: one consistent residual model, not one per algorithm).
 *
 * <p><strong>Representation.</strong> Each original edge {@code u -> v} of a {@link FlowGraph} becomes
 * a <em>pair</em> of residual edges:</p>
 * <ul>
 *   <li>a <strong>forward</strong> residual edge {@code u -> v} with initial residual capacity
 *       {@code c(u,v)} and cost {@code +cost};</li>
 *   <li>a <strong>reverse</strong> residual edge {@code v -> u} with initial residual capacity
 *       {@code 0} and cost {@code -cost}.</li>
 * </ul>
 * The pair occupies consecutive indices {@code 2k} and {@code 2k+1}, so the reverse of residual edge
 * {@code e} is simply {@code e ^ 1} — no reverse pointer needs to be stored. Pushing {@code delta}
 * along {@code e} does {@code residual[e] -= delta} and {@code residual[e ^ 1] += delta}, which is
 * exactly the residual update of Ford-Fulkerson, Edmonds-Karp, Dinic and min-cost SSP alike.
 *
 * <p>Adjacency is an insertion-ordered linked list ({@code head}/{@code next}/{@code tail}) over the
 * residual edges, giving deterministic traversal without a {@code java.util} collection. Capacities and
 * costs are {@code long}.</p>
 *
 * <p>This class is package-private: it is mutable scratch state owned by a single solve and is never
 * handed to callers. Results expose only per-original-edge flow/residual, computed here.</p>
 */
final class ResidualNetwork {

    private final int vertexCount;
    private final int[] head;
    private final int[] tail;
    private final int[] next;
    private final int[] to;
    private final int[] from;
    private final long[] residual;
    private final long[] initial;
    private final long[] cost;
    private final int[] originalId;
    private final int[] forwardIndex;
    private int edgePointer;

    ResidualNetwork(FlowGraph graph) {
        this.vertexCount = graph.vertexCount();
        int originalEdges = graph.edgeCount();
        this.head = new int[vertexCount];
        this.tail = new int[vertexCount];
        for (int v = 0; v < vertexCount; v++) {
            head[v] = -1;
            tail[v] = -1;
        }
        this.next = new int[2 * originalEdges];
        this.to = new int[2 * originalEdges];
        this.from = new int[2 * originalEdges];
        this.residual = new long[2 * originalEdges];
        this.initial = new long[2 * originalEdges];
        this.cost = new long[2 * originalEdges];
        this.originalId = new int[2 * originalEdges];
        this.forwardIndex = new int[originalEdges];
        for (int id = 0; id < originalEdges; id++) {
            int u = graph.edgeFrom(id);
            int v = graph.edgeTo(id);
            long capacity = graph.edgeCapacity(id);
            long edgeCost = graph.edgeCost(id);
            int forward = addResidual(u, v, capacity, edgeCost, id);
            addResidual(v, u, 0L, -edgeCost, -1);
            forwardIndex[id] = forward;
        }
    }

    private int addResidual(int u, int v, long capacity, long edgeCost, int originId) {
        int e = edgePointer++;
        to[e] = v;
        from[e] = u;
        residual[e] = capacity;
        initial[e] = capacity;
        cost[e] = edgeCost;
        originalId[e] = originId;
        next[e] = -1;
        if (head[u] == -1) {
            head[u] = e;
        } else {
            next[tail[u]] = e;
        }
        tail[u] = e;
        return e;
    }

    int vertexCount() {
        return vertexCount;
    }

    int firstEdge(int v) {
        return head[v];
    }

    int nextEdge(int e) {
        return next[e];
    }

    int edgeTo(int e) {
        return to[e];
    }

    int edgeFrom(int e) {
        return from[e];
    }

    long residualCapacity(int e) {
        return residual[e];
    }

    long edgeCost(int e) {
        return cost[e];
    }

    int residualEdgeCount() {
        return edgePointer;
    }

    boolean isForward(int e) {
        return originalId[e] >= 0;
    }

    void push(int e, long delta) {
        residual[e] -= delta;
        residual[e ^ 1] += delta;
    }

    /** Flow currently carried by an original edge: {@code c - residual}. */
    long flowOfOriginal(int originalEdgeId) {
        int forward = forwardIndex[originalEdgeId];
        return initial[forward] - residual[forward];
    }

    /** Residual capacity of an original edge's forward residual edge: {@code c - f}. */
    long residualOfOriginal(int originalEdgeId) {
        return residual[forwardIndex[originalEdgeId]];
    }
}
