package com.loginsight.dsa.approximation;

/**
 * One undirected edge of an {@link UndirectedGraph}. Endpoints are stored in canonical order
 * ({@code u <= v}) so that {@code {u,v}} equals {@code {v,u}} and duplicate detection is a plain
 * comparison. A self-loop has {@code u == v}.
 *
 * <p>Immutable by construction; the graph owns its edges and hands out snapshots only.</p>
 */
public final class UndirectedEdge {

    private final int u;
    private final int v;

    UndirectedEdge(int u, int v) {
        if (u > v) {
            this.u = v;
            this.v = u;
        } else {
            this.u = u;
            this.v = v;
        }
    }

    public int getU() {
        return u;
    }

    public int getV() {
        return v;
    }

    public boolean isSelfLoop() {
        return u == v;
    }

    /** True if this edge touches {@code vertex}. */
    public boolean touches(int vertex) {
        return u == vertex || v == vertex;
    }

    @Override
    public String toString() {
        return "(" + u + (u == v ? ", self loop)" : " - " + v + ")");
    }
}