package com.loginsight.dsa.dp;

/**
 * Tiny deterministic xorshift generator for reproducible test data (docs/13: randomised data must use
 * fixed seeds). Implemented locally so tests introduce no {@code java.util} dependency either.
 */
public final class SmallRandom {

    private long state;

    public SmallRandom(long seed) {
        this.state = seed == 0L ? 0x9E3779B97F4A7C15L : seed;
    }

    public int nextInt(int bound) {
        state ^= state << 13;
        state ^= state >>> 7;
        state ^= state << 17;
        long value = state >>> 1;
        return (int) (value % bound);
    }
}
