package com.loginsight.dsa.randomized;

import java.util.Random;

/**
 * Seeded random-number abstraction for the randomized algorithms module.
 *
 * <p>Production callers should use {@link #unseeded()} (time-seeded, non-reproducible).
 * Tests must use {@link #seeded(long)} to guarantee deterministic, reproducible runs.</p>
 *
 * <p>No hidden global mutable RNG state exists anywhere in the DSA engine: each algorithm
 * call receives its own {@code RandomSource} instance by dependency injection.</p>
 */
public final class RandomSource {

    private final Random random;
    private final Long seed;

    private RandomSource(Random random, Long seed) {
        this.random = random;
        this.seed = seed;
    }

    /** Fixed-seed instance for tests and reproducible demos. */
    public static RandomSource seeded(long seed) {
        return new RandomSource(new Random(seed), seed);
    }

    /** Time-seeded instance for production use (non-reproducible). */
    public static RandomSource unseeded() {
        return new RandomSource(new Random(), null);
    }

    /**
     * Returns a pseudorandom {@code int} uniformly distributed in {@code [0, bound)}.
     *
     * @throws IllegalArgumentException if {@code bound <= 0}
     */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be > 0, got " + bound);
        }
        return random.nextInt(bound);
    }

    /** Returns a pseudorandom {@code long}. */
    public long nextLong() {
        return random.nextLong();
    }

    /**
     * Returns a pseudorandom {@code long} uniformly distributed in {@code [0, bound)}.
     *
     * @throws IllegalArgumentException if {@code bound <= 0}
     */
    public long nextLong(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be > 0, got " + bound);
        }
        return random.nextLong(bound);
    }

    /** {@code true} when this instance was created via {@link #seeded(long)}. */
    public boolean isSeeded() {
        return seed != null;
    }

    /** The seed value, or {@code null} if this instance is unseeded. */
    public Long seed() {
        return seed;
    }
}
