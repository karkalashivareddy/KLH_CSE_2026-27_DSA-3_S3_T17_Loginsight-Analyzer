package com.loginsight.dsa.dp.bitmask;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: Travelling Salesman Problem via bitmask (Held-Karp) dynamic programming.
 * <p>
 * Purpose: find the cheapest closed tour that visits every city exactly once, returning to the start
 * (Playground demo with the {@code 2^n} state-count warning).
 * <p>
 * Input: an {@code n x n} cost matrix {@code dist}, where {@code dist[u][v] >= 0} is the cost of the
 * edge {@code u -> v} and {@code dist[u][v] == -1} means "no edge" (unreachable). A start city index.
 * <p>
 * Output: {@link TspResult} — whether a complete tour exists, its minimum cost, and the visit order.
 * <p>
 * State: {@code dp[mask][last]} = minimum cost of a path that starts at {@code start}, visits exactly
 * the cities in {@code mask} and currently ends at {@code last}, with {@code start} and {@code last}
 * both in {@code mask}.
 * <p>
 * Base case: {@code dp[1 << start][start] = 0} (only the start city visited). All other states start
 * at {@link #INF}.
 * <p>
 * Recurrence / transition:
 * <pre>
 *   for every state (mask, last) with a finite value,
 *   for every next not in mask with dist[last][next] != -1:
 *       dp[mask | (1 << next)][next] = min( dp[mask | (1 << next)][next],
 *                                           dp[mask][last] + dist[last][next] )
 * </pre>
 * <p>
 * Final answer: {@code min over last != start of ( dp[full][last] + dist[last][start] )}, where
 * {@code full = (1 << n) - 1}. If no such finite closing edge exists, there is no tour.
 * <p>
 * Why subproblems overlap: the same {@code (mask, last)} is reachable through many visit orders, so
 * enumerating permutations recomputes it exponentially; the table stores it once.
 * <p>
 * Time Complexity: O(2^n · n^2). Space Complexity: O(2^n · n).
 * <p>
 * Deterministic tie-breaking: cities are scanned in ascending index order and a relaxation only
 * replaces on a strictly smaller cost, so both the parent pointers and the final path are stable.
 * The closing city is the smallest index achieving the minimum.
 * <p>
 * Overflow safety: all distances and DP values are {@code long}; unreachable states use the sentinel
 * {@link #INF} and are skipped before any addition, so no {@code Long.MAX_VALUE + x} ever occurs.
 * <p>
 * Size cap: {@link #MAX_CITIES} bounds the {@code 2^n} table (a demo must stay interactive); larger
 * inputs are rejected rather than silently exhausting memory.
 */
public final class BitmaskTSP {

    /** Input sentinel meaning "no edge between these two cities". */
    public static final long UNREACHABLE = -1L;
    /** Internal "no path yet" sentinel; small enough that additions cannot overflow. */
    public static final long INF = Long.MAX_VALUE / 4;
    /** Largest supported city count, driven by the O(2^n · n) table. */
    public static final int MAX_CITIES = 18;

    private static final String ALGORITHM = "Bitmask TSP";
    private static final int NO_PARENT = -2;

    private final long[][] dist;
    private final int start;

    public BitmaskTSP(long[][] dist, int start) {
        DpValidator.requireNonNull((Object) dist);
        int n = dist.length;
        if (n == 0) {
            throw new IllegalArgumentException("distance matrix must contain at least one city");
        }
        if (n > MAX_CITIES) {
            throw new IllegalArgumentException(
                    "at most " + MAX_CITIES + " cities are supported but got " + n);
        }
        for (int i = 0; i < n; i++) {
            DpValidator.requireNonNull((Object) dist[i]);
            if (dist[i].length != n) {
                throw new IllegalArgumentException("distance matrix must be square; row " + i
                        + " has length " + dist[i].length + " but expected " + n);
            }
            for (int j = 0; j < n; j++) {
                if (dist[i][j] < UNREACHABLE) {
                    throw new IllegalArgumentException(
                            "dist[" + i + "][" + j + "] must be >= -1 but was " + dist[i][j]);
                }
            }
        }
        DpValidator.requireInRange(start, 0, n - 1, "start");
        this.dist = dist;
        this.start = start;
    }

    public TspResult solve() {
        int n = dist.length;
        if (n == 1) {
            return new TspResult(true, 0L, new int[]{start});
        }
        int full = (1 << n) - 1;
        long[][] dp = new long[1 << n][n];
        int[][] parent = new int[1 << n][n];
        for (int mask = 0; mask <= full; mask++) {
            for (int last = 0; last < n; last++) {
                dp[mask][last] = INF;
                parent[mask][last] = NO_PARENT;
            }
        }
        dp[1 << start][start] = 0L;

        for (int mask = 0; mask <= full; mask++) {
            if ((mask & (1 << start)) == 0) {
                continue;
            }
            for (int last = 0; last < n; last++) {
                if ((mask & (1 << last)) == 0 || dp[mask][last] == INF) {
                    continue;
                }
                for (int next = 0; next < n; next++) {
                    if ((mask & (1 << next)) != 0 || dist[last][next] == UNREACHABLE) {
                        continue;
                    }
                    int nextMask = mask | (1 << next);
                    long candidate = dp[mask][last] + dist[last][next];
                    if (candidate < dp[nextMask][next]) {
                        dp[nextMask][next] = candidate;
                        parent[nextMask][next] = last;
                    }
                }
            }
        }

        long best = INF;
        int bestLast = -1;
        for (int last = 0; last < n; last++) {
            if (last == start || dp[full][last] == INF || dist[last][start] == UNREACHABLE) {
                continue;
            }
            long candidate = dp[full][last] + dist[last][start];
            if (candidate < best) {
                best = candidate;
                bestLast = last;
            }
        }
        if (bestLast == -1) {
            return new TspResult(false, INF, new int[0]);
        }

        int[] path = new int[n];
        int mask = full;
        int current = bestLast;
        int index = n - 1;
        while (true) {
            path[index--] = current;
            if (current == start) {
                break;
            }
            int previous = parent[mask][current];
            mask ^= (1 << current);
            current = previous;
        }
        return new TspResult(true, best, path);
    }

    /** Envelope view; {@code result} = minimum cost, {@code intermediateData} = the reconstructed path. */
    public DpResult match() {
        long begin = System.nanoTime();
        TspResult result = solve();
        long elapsed = System.nanoTime() - begin;
        int n = dist.length;
        Object intermediate = result.hasTour() ? result.getPath() : null;
        return new DpResult(ALGORITHM, n, result.hasTour() ? result.getMinCost() : INF, elapsed,
                "O(2^n * n^2)", "O(2^n * n)", intermediate);
    }

    public int getStart() {
        return start;
    }

    public int cityCount() {
        return dist.length;
    }
}
