package com.loginsight.dsa.dp.bitmask;

/**
 * Result of a bitmask TSP solve: whether a closed tour exists, its minimum cost, and a reconstructed
 * visit order (starting at the configured start city and returning to it).
 */
public final class TspResult {

    private final boolean hasTour;
    private final long minCost;
    private final int[] path;

    public TspResult(boolean hasTour, long minCost, int[] path) {
        this.hasTour = hasTour;
        this.minCost = minCost;
        this.path = path == null ? new int[0] : path;
    }

    /** A tour visiting every city exactly once and returning to the start exists. */
    public boolean hasTour() {
        return hasTour;
    }

    /** Minimum tour cost when {@link #hasTour()} is true; the sentinel INF otherwise. */
    public long getMinCost() {
        return minCost;
    }

    /** Visit order; first element is the start city and the array covers all cities. */
    public int[] getPath() {
        return path;
    }
}
