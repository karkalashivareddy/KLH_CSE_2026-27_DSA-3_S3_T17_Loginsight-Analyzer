package com.loginsight.dsa.dp.bitmask;

/**
 * Result of a Hamiltonian-path bitmask-DP solve: whether a path visiting every vertex exactly once
 * exists (from the requested start, or from any start for the "find any" variant) and, if so, one
 * such path.
 */
public final class HamiltonianPathResult {

    private final boolean exists;
    private final int[] path;

    public HamiltonianPathResult(boolean exists, int[] path) {
        this.exists = exists;
        this.path = path == null ? new int[0] : path;
    }

    public boolean exists() {
        return exists;
    }

    /** A path covering all vertices exactly once, or an empty array when {@link #exists()} is false. */
    public int[] getPath() {
        return path;
    }
}
