package com.loginsight.dsa.approximation;

/**
 * Immutable, undirected, simple graph — the shared problem definition for every Module 5 example
 * (vertex cover, independent set, clique, matching, FPT).
 *
 * <h2>Model</h2>
 * Vertices are {@code 0..n-1}. {@link #addEdge(int, int)} stores an undirected edge with endpoints in
 * canonical order. The graph is kept <em>simple</em>:
 * <ul>
 *   <li><strong>duplicate-edge policy:</strong> adding a pair that already exists is a silent no-op
 *       (returns the existing edge id) — a simple graph has no parallel edges, and duplicates carry no
 *       information for covers/matchings;</li>
 *   <li><strong>self-loop policy:</strong> self-loops are allowed and stored; a self-loop at
 *       {@code v} forces {@code v} into every vertex cover, and it is never usable inside a matching.</li>
 * </ul>
 *
 * <h2>Determinism</h2>
 * Edges are kept in insertion order and all iteration (matching, branching, kernelization) scans in
 * that order, so results are reproducible.
 *
 * <h2>Removal</h2>
 * {@link #withoutVertex(int)} returns a copy with one vertex and all its incident edges removed while
 * preserving the <em>original vertex label space</em> — the backbone of the FPT branching and
 * kernelization reductions, which need to keep track of which original vertices were chosen.
 *
 * <h2>Validation</h2>
 * Out-of-range endpoints and a negative vertex count are rejected with {@link IllegalArgumentException}.
 */
public final class UndirectedGraph {

    private static final int INITIAL_EDGE_CAPACITY = 16;

    private final int vertexCount;
    private int[] edgeU;
    private int[] edgeV;
    private int edgeCount;

    public UndirectedGraph(int vertexCount) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("vertexCount must be >= 0 but was " + vertexCount);
        }
        this.vertexCount = vertexCount;
        this.edgeU = new int[INITIAL_EDGE_CAPACITY];
        this.edgeV = new int[INITIAL_EDGE_CAPACITY];
    }

    /**
     * Adds an undirected edge, or returns the id of the existing edge when the pair is already
     * present (simple-graph contract: duplicates are ignored).
     */
    public int addEdge(int u, int v) {
        checkVertex(u);
        checkVertex(v);
        int low = Math.min(u, v);
        int high = Math.max(u, v);
        for (int i = 0; i < edgeCount; i++) {
            if (edgeU[i] == low && edgeV[i] == high) {
                return i;
            }
        }
        if (edgeCount == edgeU.length) {
            int expanded = edgeU.length * 2;
            edgeU = copyOf(edgeU, expanded);
            edgeV = copyOf(edgeV, expanded);
        }
        int id = edgeCount++;
        edgeU[id] = low;
        edgeV[id] = high;
        return id;
    }

    public int vertexCount() {
        return vertexCount;
    }

    /** Number of distinct undirected edges (self-loops counted once each). */
    public int edgeCount() {
        return edgeCount;
    }

    /**
     * Number of distinct edges incident to {@code vertex}. A self-loop is one incident edge; isolated
     * vertices have degree 0. This is the degree used by the kernelization high-degree rule.
     */
    public int degree(int vertex) {
        checkVertex(vertex);
        int count = 0;
        for (int i = 0; i < edgeCount; i++) {
            if (edgeU[i] == vertex || edgeV[i] == vertex) {
                count++;
            }
        }
        return count;
    }

    public boolean hasVertex(int vertex) {
        return degree(vertex) > 0;
    }

    public boolean hasEdge(int u, int v) {
        checkVertex(u);
        checkVertex(v);
        int low = Math.min(u, v);
        int high = Math.max(u, v);
        for (int i = 0; i < edgeCount; i++) {
            if (edgeU[i] == low && edgeV[i] == high) {
                return true;
            }
        }
        return false;
    }

    public UndirectedEdge edge(int id) {
        checkEdge(id);
        return new UndirectedEdge(edgeU[id], edgeV[id]);
    }

    /** Snapshot of all edges in insertion order. */
    public UndirectedEdge[] edges() {
        UndirectedEdge[] result = new UndirectedEdge[edgeCount];
        for (int i = 0; i < edgeCount; i++) {
            result[i] = new UndirectedEdge(edgeU[i], edgeV[i]);
        }
        return result;
    }

    /**
     * Returns a copy of this graph with {@code vertex} and all its incident edges removed. Vertex ids
     * of the survivors are unchanged, so callers can reason about the original labels.
     */
    public UndirectedGraph withoutVertex(int vertex) {
        checkVertex(vertex);
        UndirectedGraph copy = new UndirectedGraph(vertexCount);
        for (int i = 0; i < edgeCount; i++) {
            if (edgeU[i] != vertex && edgeV[i] != vertex) {
                copy.addEdge(edgeU[i], edgeV[i]);
            }
        }
        return copy;
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount) {
            throw new IllegalArgumentException("vertex must be in [0, " + (vertexCount - 1)
                    + "] but was " + vertex);
        }
    }

    private void checkEdge(int id) {
        if (id < 0 || id >= edgeCount) {
            throw new IllegalArgumentException("edgeId must be in [0, " + (edgeCount - 1)
                    + "] but was " + id);
        }
    }

    private static int[] copyOf(int[] source, int newLength) {
        int[] target = new int[newLength];
        System.arraycopy(source, 0, target, 0, source.length);
        return target;
    }

    @Override
    public String toString() {
        return "UndirectedGraph(" + vertexCount + " vertices, " + edgeCount + " edges)";
    }
}