package com.loginsight.dsa.randomized;

/**
 * Overflow-safe modular arithmetic primitives for the randomized algorithms module.
 *
 * <p>All public methods accept operands of any sign and normalise them into {@code [0, mod)}
 * before computing.  {@code mod} must be positive and at most {@code 2^63 − 1}
 * ({@link Long#MAX_VALUE}).</p>
 *
 * <h2>Complexity</h2>
 * <ul>
 *   <li>{@link #addMod} – O(1) word operations (unsigned-safe comparison guard).</li>
 *   <li>{@link #multiplyMod} – O(log b) additions via the double-and-add (Russian peasant)
 *       method; each addition uses {@link #addMod}, so no intermediate value exceeds
 *       {@code 2^63 − 1}.</li>
 *   <li>{@link #powMod} – O(log e) multiplications via square-and-multiply, each
 *       using {@link #multiplyMod}; total O(log² e) 64-bit word operations.</li>
 * </ul>
 *
 * <p>No {@code java.math.BigInteger} is used in production code.</p>
 */
public final class ModularArithmetic {

    private ModularArithmetic() { }

    /**
     * Returns {@code x mod mod}, adjusted to {@code [0, mod)}.
     *
     * @throws ArithmeticException if {@code mod <= 0}
     */
    public static long normalize(long x, long mod) {
        if (mod <= 0) {
            throw new ArithmeticException("mod must be > 0, got " + mod);
        }
        long r = x % mod;
        if (r < 0) {
            r += mod;
        }
        return r;
    }

    /**
     * Returns {@code (a + b) mod mod} without intermediate overflow.
     *
     * <p>Precondition: {@code a, b ∈ [0, mod)} and {@code mod ≤ 2^63 − 1}.  The guard
     * {@code a ≥ mod − b} is evaluated using only values in {@code [0, mod)}, so no
     * signed overflow can occur.</p>
     *
     * @throws ArithmeticException if {@code mod <= 0}
     */
    public static long addMod(long a, long b, long mod) {
        a = normalize(a, mod);
        b = normalize(b, mod);
        long complement = mod - b;
        if (a >= complement) {
            return a - complement;
        }
        return a + b;
    }

    /**
     * Returns {@code (a * b) mod mod} using the double-and-add (Russian peasant) method.
     *
     * <p>The loop iterates over the bits of {@code b} (at most 63), doubling {@code a} and
     * conditionally adding to the accumulator—both operations use {@link #addMod}, so no
     * intermediate product ever exceeds {@code 2^63 − 1}.</p>
     *
     * @throws ArithmeticException if {@code mod <= 0}
     */
    public static long multiplyMod(long a, long b, long mod) {
        a = normalize(a, mod);
        b = normalize(b, mod);
        long result = 0;
        while (b > 0) {
            if ((b & 1) == 1) {
                result = addMod(result, a, mod);
            }
            a = addMod(a, a, mod);
            b >>= 1;
        }
        return result;
    }

    /**
     * Returns {@code (base^exp) mod mod} via square-and-multiply.
     *
     * <p>Each squaring and conditional multiplication uses {@link #multiplyMod}.
     * Total cost: O(log² exp) 64-bit word operations.</p>
     *
     * @throws ArithmeticException if {@code mod <= 0}
     */
    public static long powMod(long base, long exp, long mod) {
        if (mod == 1) {
            return 0;
        }
        long result = 1;
        base = normalize(base, mod);
        while (exp > 0) {
            if ((exp & 1) == 1) {
                result = multiplyMod(result, base, mod);
            }
            base = multiplyMod(base, base, mod);
            exp >>= 1;
        }
        return result;
    }
}
