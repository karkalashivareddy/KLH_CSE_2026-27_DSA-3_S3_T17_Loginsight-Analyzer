package com.loginsight.simulation;

/**
 * Deterministic pseudo-random source for the live simulation (SplitMix64).
 *
 * <p>The simulation must be reproducible: the same {@code (scenarioId, seed)} pair has to produce the
 * same event progression, the same severity mix and therefore the same detected incidents on every
 * run. {@link java.util.Random} would technically satisfy that, but its algorithm is not a stable
 * contract across JDKs, so the generator is pinned explicitly here.</p>
 *
 * <p>Instances are single-threaded by contract: a {@code SimulationSession} owns exactly one.</p>
 */
public final class DeterministicRandom {

    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    private static final long MIX_A = 0xBF58476D1CE4E5B9L;
    private static final long MIX_B = 0x94D049BB133111EBL;

    private long state;

    public DeterministicRandom(long seed) {
        this.state = seed == 0L ? GOLDEN_GAMMA : seed;
    }

    /** Builds a generator from a scenario id and a user-facing seed, so streams never collide. */
    public static DeterministicRandom forScenario(String scenarioId, long seed) {
        long mixed = seed;
        for (int i = 0; i < scenarioId.length(); i++) {
            mixed = mixed * GOLDEN_GAMMA + scenarioId.charAt(i);
        }
        return new DeterministicRandom(mixed);
    }

    /** Next raw 64-bit value. */
    public long nextLong() {
        state += GOLDEN_GAMMA;
        long z = state;
        z = (z ^ (z >>> 30)) * MIX_A;
        z = (z ^ (z >>> 27)) * MIX_B;
        return z ^ (z >>> 31);
    }

    /** Uniform value in {@code [0, bound)}; {@code bound <= 0} yields 0. */
    public int nextInt(int bound) {
        if (bound <= 0) {
            return 0;
        }
        return (int) Math.floorMod(nextLong(), (long) bound);
    }

    /** Uniform value in {@code [originInclusive, boundExclusive)}. */
    public int between(int originInclusive, int boundExclusive) {
        if (boundExclusive <= originInclusive) {
            return originInclusive;
        }
        return originInclusive + nextInt(boundExclusive - originInclusive);
    }

    /** Uniform double in {@code [0, 1)}. */
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    /** Uniform double in {@code [origin, bound)}. */
    public double between(double origin, double bound) {
        if (bound <= origin) {
            return origin;
        }
        return origin + nextDouble() * (bound - origin);
    }

    /** True with the given probability. */
    public boolean chance(double probability) {
        return nextDouble() < probability;
    }

    /** Uniformly picks one element; an empty collection yields {@code null}. */
    public <T> T pick(java.util.List<T> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(nextInt(values.size()));
    }
}
