package com.loginsight.dsa.dp.tree;

import com.loginsight.dsa.dp.DpValidator;

/**
 * Lightweight, validated undirected tree used by the tree-DP algorithms.
 *
 * <p>Representation: a compressed adjacency list built from primitive arrays
 * ({@code adjacencyStart[u] .. adjacencyStart[u+1]} slice the shared {@code adjacencyTo} /
 * {@code adjacencyWeight} arrays). No {@code java.util} collection is used, so the structure a viva
 * inspects is exactly the one the algorithms traverse.</p>
 *
 * <p><strong>Contract:</strong> {@code n >= 1}; exactly {@code n - 1} edges; both endpoints in range;
 * no self loops; no duplicate edges; the graph must be connected. Together with {@code n - 1} edges,
 * connectivity implies acyclicity, so a valid instance is a tree. Any violation is rejected with
 * {@link IllegalArgumentException}.</p>
 *
 * <p>Edges may be unweighted (weight 1) or weighted with non-negative {@code long} weights.</p>
 */
public final class Tree {

    private final int n;
    private final int[] adjacencyStart;
    private final int[] adjacencyTo;
    private final long[] adjacencyWeight;

    private Tree(int n, int[] adjacencyStart, int[] adjacencyTo, long[] adjacencyWeight) {
        this.n = n;
        this.adjacencyStart = adjacencyStart;
        this.adjacencyTo = adjacencyTo;
        this.adjacencyWeight = adjacencyWeight;
    }

    /** Build an unweighted tree (every edge weight = 1) from {@code n} and undirected edge pairs. */
    public static Tree of(int n, int[][] edges) {
        DpValidator.requireNonNull((Object) edges);
        int[] from = new int[edges.length];
        int[] to = new int[edges.length];
        for (int i = 0; i < edges.length; i++) {
            if (edges[i] == null || edges[i].length != 2) {
                throw new IllegalArgumentException("edge " + i + " must be a {from, to} pair");
            }
            from[i] = edges[i][0];
            to[i] = edges[i][1];
        }
        return of(n, from, to);
    }

    /** Build an unweighted tree (every edge weight = 1). */
    public static Tree of(int n, int[] from, int[] to) {
        DpValidator.requireNonNull((Object) from, (Object) to);
        long[] weights = new long[from.length];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = 1L;
        }
        return weighted(n, from, to, weights);
    }

    /** Build a weighted tree; weights must be non-negative. */
    public static Tree weighted(int n, int[] from, int[] to, long[] weights) {
        DpValidator.requireNonNull((Object) from, (Object) to, (Object) weights);
        if (from.length != to.length || from.length != weights.length) {
            throw new IllegalArgumentException("from, to and weights must have the same length");
        }
        if (n < 1) {
            throw new IllegalArgumentException("tree must have at least one node but had " + n);
        }
        int m = from.length;
        if (m != n - 1) {
            throw new IllegalArgumentException(
                    "a tree with " + n + " nodes must have " + (n - 1) + " edges but had " + m);
        }
        for (int i = 0; i < m; i++) {
            DpValidator.requireInRange(from[i], 0, n - 1, "from[" + i + "]");
            DpValidator.requireInRange(to[i], 0, n - 1, "to[" + i + "]");
            if (from[i] == to[i]) {
                throw new IllegalArgumentException("self loop at node " + from[i]);
            }
            DpValidator.requireNonNegative(weights[i], "weights[" + i + "]");
        }

        int[] degree = new int[n];
        for (int i = 0; i < m; i++) {
            degree[from[i]]++;
            degree[to[i]]++;
        }
        int[] start = new int[n + 1];
        for (int u = 0; u < n; u++) {
            start[u + 1] = start[u] + degree[u];
        }
        int[] to2 = new int[2 * m];
        long[] weight2 = new long[2 * m];
        int[] cursor = new int[n];
        for (int u = 0; u < n; u++) {
            cursor[u] = start[u];
        }
        for (int i = 0; i < m; i++) {
            int u = from[i];
            int v = to[i];
            to2[cursor[u]] = v;
            weight2[cursor[u]] = weights[i];
            cursor[u]++;
            to2[cursor[v]] = u;
            weight2[cursor[v]] = weights[i];
            cursor[v]++;
        }
        Tree tree = new Tree(n, start, to2, weight2);
        tree.rejectDuplicateEdges();
        tree.requireConnected();
        return tree;
    }

    private void rejectDuplicateEdges() {
        for (int u = 0; u < n; u++) {
            for (int a = adjacencyStart[u]; a < adjacencyStart[u + 1]; a++) {
                for (int b = a + 1; b < adjacencyStart[u + 1]; b++) {
                    if (adjacencyTo[a] == adjacencyTo[b]) {
                        throw new IllegalArgumentException("duplicate edge between " + u + " and "
                                + adjacencyTo[a]);
                    }
                }
            }
        }
    }

    private void requireConnected() {
        boolean[] visited = new boolean[n];
        int[] queue = new int[n];
        int head = 0;
        int tail = 0;
        queue[tail++] = 0;
        visited[0] = true;
        int seen = 1;
        while (head < tail) {
            int u = queue[head++];
            for (int a = adjacencyStart[u]; a < adjacencyStart[u + 1]; a++) {
                int v = adjacencyTo[a];
                if (!visited[v]) {
                    visited[v] = true;
                    seen++;
                    queue[tail++] = v;
                }
            }
        }
        if (seen != n) {
            throw new IllegalArgumentException("edges do not form a connected tree (" + seen
                    + " of " + n + " nodes reachable)");
        }
    }

    public int size() {
        return n;
    }

    public int degree(int u) {
        DpValidator.requireInRange(u, 0, n - 1, "u");
        return adjacencyStart[u + 1] - adjacencyStart[u];
    }

    public int neighbor(int u, int index) {
        DpValidator.requireInRange(index, 0, degree(u) - 1, "index");
        return adjacencyTo[adjacencyStart[u] + index];
    }

    public long weight(int u, int index) {
        DpValidator.requireInRange(index, 0, degree(u) - 1, "index");
        return adjacencyWeight[adjacencyStart[u] + index];
    }

    /** Fresh copy of a node's neighbours. */
    public int[] neighbors(int u) {
        int d = degree(u);
        int[] result = new int[d];
        for (int i = 0; i < d; i++) {
            result[i] = adjacencyTo[adjacencyStart[u] + i];
        }
        return result;
    }
}
