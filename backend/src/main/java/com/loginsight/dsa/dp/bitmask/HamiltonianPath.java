package com.loginsight.dsa.dp.bitmask;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: Hamiltonian path existence via subset (bitmask) dynamic programming.
 * <p>
 * Purpose: decide whether a path exists that visits every vertex of a graph exactly once, and return
 * one if so (a dependency/route feasibility question in the Playground).
 * <p>
 * Input: number of vertices {@code n} (1..{@link #MAX_VERTICES}) and undirected edges added through
 * {@link #addEdge(int, int)}. Optionally a required start vertex.
 * <p>
 * Output: {@link HamiltonianPathResult} — existence plus a reconstructed path when one exists.
 * <p>
 * State: {@code dp[mask][last]} = true iff there is a path that starts at the fixed start vertex,
 * visits exactly the vertices in {@code mask}, and currently ends at {@code last}.
 * <p>
 * Base case: {@code dp[1 << start][start] = true}.
 * <p>
 * Recurrence / transition:
 * <pre>
 *   dp[mask | (1 << next)][next] = true   for any state (mask, last) true,
 *                                          any next not in mask with an edge last-next
 * </pre>
 * <p>
 * Final answer: {@code dp[(1 << n) - 1][last]} true for some {@code last}. {@link #findAny()} tries
 * each start vertex in ascending order and returns the first success.
 * <p>
 * Why subproblems overlap: the same {@code (mask, last)} is reached by many orderings; the table
 * records it once instead of exploring all permutations.
 * <p>
 * Time Complexity: O(2^n · n^2) worst case (each state scans its neighbours). Space Complexity:
 * O(2^n · n).
 * <p>
 * Deterministic tie-breaking: vertices are scanned in ascending index order and states are only set
 * once, so the reconstructed path is stable.
 * <p>
 * Empty-graph contract: {@code n} must be {@code >= 1}; a graph with zero vertices is rejected with
 * {@link IllegalArgumentException}. A single vertex trivially has the path {@code [0]}.
 * <p>
 * Size cap: {@link #MAX_VERTICES} bounds the {@code 2^n} table.
 */
public final class HamiltonianPath {

    /** Largest supported vertex count for the {@code 2^n} table. */
    public static final int MAX_VERTICES = 18;
    private static final String ALGORITHM = "Hamiltonian path";
    private static final int NO_PARENT = -2;

    private final int n;
    private final boolean[][] adjacency;

    public HamiltonianPath(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("vertex count must be >= 1 but was " + n);
        }
        if (n > MAX_VERTICES) {
            throw new IllegalArgumentException(
                    "at most " + MAX_VERTICES + " vertices are supported but got " + n);
        }
        this.n = n;
        this.adjacency = new boolean[n][n];
    }

    /** Adds an undirected edge; rejects self loops and invalid endpoints. */
    public void addEdge(int u, int v) {
        DpValidator.requireInRange(u, 0, n - 1, "u");
        DpValidator.requireInRange(v, 0, n - 1, "v");
        if (u == v) {
            throw new IllegalArgumentException("self loops are not allowed: " + u);
        }
        adjacency[u][v] = true;
        adjacency[v][u] = true;
    }

    public void addEdge(int[] from, int[] to) {
        DpValidator.requireNonNull((Object) from, (Object) to);
        if (from.length != to.length) {
            throw new IllegalArgumentException("from and to must have the same length");
        }
        for (int i = 0; i < from.length; i++) {
            addEdge(from[i], to[i]);
        }
    }

    public int vertexCount() {
        return n;
    }

    public boolean hasEdge(int u, int v) {
        return adjacency[u][v];
    }

    /** Determine existence from a fixed start vertex. */
    public HamiltonianPathResult find(int start) {
        DpValidator.requireInRange(start, 0, n - 1, "start");
        int full = (1 << n) - 1;
        boolean[][] dp = new boolean[1 << n][n];
        int[][] parent = new int[1 << n][n];
        for (int mask = 0; mask <= full; mask++) {
            for (int v = 0; v < n; v++) {
                parent[mask][v] = NO_PARENT;
            }
        }
        dp[1 << start][start] = true;

        for (int mask = 1; mask <= full; mask++) {
            if ((mask & (1 << start)) == 0) {
                continue;
            }
            for (int last = 0; last < n; last++) {
                if ((mask & (1 << last)) == 0 || !dp[mask][last]) {
                    continue;
                }
                for (int next = 0; next < n; next++) {
                    if ((mask & (1 << next)) != 0 || !adjacency[last][next]) {
                        continue;
                    }
                    int nextMask = mask | (1 << next);
                    if (!dp[nextMask][next]) {
                        dp[nextMask][next] = true;
                        parent[nextMask][next] = last;
                    }
                }
            }
        }

        int end = -1;
        for (int v = 0; v < n; v++) {
            if (dp[full][v]) {
                end = v;
                break;
            }
        }
        if (end == -1) {
            return new HamiltonianPathResult(false, new int[0]);
        }
        int[] path = new int[n];
        int mask = full;
        int current = end;
        int index = n - 1;
        while (true) {
            path[index--] = current;
            if (index < 0) {
                break;
            }
            int previous = parent[mask][current];
            mask ^= (1 << current);
            current = previous;
        }
        return new HamiltonianPathResult(true, path);
    }

    /** Existence from any start vertex; tries starts in ascending order for determinism. */
    public HamiltonianPathResult findAny() {
        for (int start = 0; start < n; start++) {
            HamiltonianPathResult result = find(start);
            if (result.exists()) {
                return result;
            }
        }
        return new HamiltonianPathResult(false, new int[0]);
    }

    /** Envelope view; {@code result} = existence (Boolean), {@code intermediateData} = the path. */
    public DpResult match() {
        long begin = System.nanoTime();
        HamiltonianPathResult result = findAny();
        long elapsed = System.nanoTime() - begin;
        return new DpResult(ALGORITHM, n, result.exists(), elapsed,
                "O(2^n * n^2)", "O(2^n * n)", result.exists() ? result.getPath() : null);
    }
}
