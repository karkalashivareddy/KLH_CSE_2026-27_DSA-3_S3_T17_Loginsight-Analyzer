package com.loginsight.dsa.randomized;

/**
 * Miller–Rabin probabilistic primality test.
 *
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>DETERMINISTIC</b> – uses the fixed witness set
 *       {@code {2, 325, 9375, 28178, 450775, 9780504, 1795265022}}, which is proven correct
 *       for every {@code n < 2^64}.  Since every positive {@code long} satisfies
 *       {@code n < 2^63 < 2^64}, the deterministic mode is correct for the entire
 *       {@code long} range.  The {@code rounds} and {@code rng} parameters are ignored.</li>
 *   <li><b>PROBABILISTIC</b> (Monte Carlo) – draws {@code rounds} random bases in
 *       {@code [2, n − 2]}.  The probability of a false positive on a composite is at most
 *       {@code 4^{−rounds}}.</li>
 * </ul>
 *
 * <h2>Complexity</h2>
 * <p>Each round: decompose {@code n − 1 = d·2^s}; compute {@code a^d mod n} via
 * {@link ModularArithmetic#powMod} in O(log² d) word ops; square at most {@code s} times.
 * Total per round: O(log² n) 64-bit word operations.  Full test: O(k · log² n).</p>
 *
 * <h2>Modular arithmetic</h2>
 * <p>All modular multiplication uses {@link ModularArithmetic#multiplyMod} (double-and-add),
 * which never overflows for {@code n ≤ 2^63 − 1}.  No {@code java.math.BigInteger} is used
 * in production.</p>
 */
public final class MillerRabin {

    /** Witness set proven correct for every {@code n < 2^64}. */
    private static final long[] DETERMINISTIC_WITNESSES = {
        2L, 325L, 9375L, 28178L, 450775L, 9780504L, 1795265022L
    };

    public enum Mode {
        DETERMINISTIC,
        PROBABILISTIC
    }

    /**
     * Full-featured primality test.
     *
     * @param n     the value to test
     * @param mode  deterministic or probabilistic
     * @param rng   random source (required only for {@code PROBABILISTIC} mode; ignored for
     *              {@code DETERMINISTIC})
     * @param rounds number of random rounds (required only for {@code PROBABILISTIC} mode;
     *               ignored for {@code DETERMINISTIC})
     * @return an immutable result recording the verdict and the bases used
     * @throws IllegalArgumentException if {@code PROBABILISTIC} but {@code rng} is null or
     *         {@code rounds <= 0}
     */
    public MillerRabinResult test(long n, Mode mode, RandomSource rng, int rounds) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be >= 0, got " + n);
        }
        if (mode == Mode.PROBABILISTIC) {
            if (rng == null) {
                throw new IllegalArgumentException("rng must not be null for PROBABILISTIC mode");
            }
            if (rounds <= 0) {
                throw new IllegalArgumentException("rounds must be > 0 for PROBABILISTIC mode, got " + rounds);
            }
        }
        if (n < 2) {
            return new MillerRabinResult(n, false, modeName(mode),
                    deterministicRounds(mode, 0), new long[0],
                    "n < 2 is not prime");
        }
        if (n == 2 || n == 3) {
            return new MillerRabinResult(n, true, modeName(mode),
                    deterministicRounds(mode, 0), new long[0],
                    "small prime");
        }
        if (n % 2 == 0) {
            return new MillerRabinResult(n, false, modeName(mode),
                    deterministicRounds(mode, 0), new long[0],
                    "even > 2 is composite");
        }

        long d = n - 1;
        int s = 0;
        while (d % 2 == 0) {
            d /= 2;
            s++;
        }

        long[] bases = resolveBases(n, mode, rng, rounds);

        for (long a : bases) {
            if (a % n == 0) {
                continue;
            }
            long x = ModularArithmetic.powMod(a, d, n);
            if (x == 1 || x == n - 1) {
                continue;
            }
            boolean found = false;
            for (int r = 1; r < s; r++) {
                x = ModularArithmetic.multiplyMod(x, x, n);
                if (x == n - 1) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return new MillerRabinResult(n, false, modeName(mode),
                        actualRounds(mode, bases), bases,
                        "composite (witness found)");
            }
        }
        return new MillerRabinResult(n, true, modeName(mode),
                actualRounds(mode, bases), bases,
                deterministicModeNote(mode));
    }

    /** Convenience: deterministic test for all positive {@code long} values. */
    public boolean isDefinitePrime(long n) {
        return test(n, Mode.DETERMINISTIC, null, 0).isPrime();
    }

    /** Convenience: Monte Carlo test with the given number of rounds. */
    public MillerRabinResult isProbablePrime(long n, int rounds, RandomSource rng) {
        return test(n, Mode.PROBABILISTIC, rng, rounds);
    }

    private long[] resolveBases(long n, Mode mode, RandomSource rng, int rounds) {
        if (mode == Mode.DETERMINISTIC) {
            return DETERMINISTIC_WITNESSES;
        }
        long[] bases = new long[rounds];
        for (int i = 0; i < rounds; i++) {
            bases[i] = 2 + rng.nextLong(n - 3);
        }
        return bases;
    }

    private static String modeName(Mode mode) {
        return mode == Mode.DETERMINISTIC ? "DETERMINISTIC" : "PROBABILISTIC";
    }

    private static int deterministicRounds(Mode mode, int fallback) {
        return mode == Mode.DETERMINISTIC ? DETERMINISTIC_WITNESSES.length : fallback;
    }

    private static int actualRounds(Mode mode, long[] bases) {
        return mode == Mode.DETERMINISTIC ? DETERMINISTIC_WITNESSES.length : bases.length;
    }

    private static String deterministicModeNote(Mode mode) {
        return mode == Mode.DETERMINISTIC
                ? "definitely prime (7-witness deterministic set covers all n < 2^64)"
                : "probable prime (Monte Carlo, false-positive probability <= 4^(-rounds))";
    }
}
