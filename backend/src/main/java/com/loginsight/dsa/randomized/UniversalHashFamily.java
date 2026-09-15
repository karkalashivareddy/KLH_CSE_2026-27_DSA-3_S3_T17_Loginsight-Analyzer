package com.loginsight.dsa.randomized;

/**
 * Universal (pairwise-independent) hashing family.
 *
 * <p>Implements the classic universal family
 * {@code h(x) = ((a * x + b) mod p) mod m} where {@code p} is prime,
 * {@code a ∈ [1, p−1]}, {@code b ∈ [0, p−1]}, and {@code m} is the table size.</p>
 *
 * <h2>Pairwise independence</h2>
 * <p>For distinct keys {@code x ≠ y}, the collision probability over a random choice of
 * {@code (a, b)} from the family is at most {@code ⌈p/m⌉ / p ≈ 1/m}.  This is a
 * <em>probability bound</em> over the family, not a guarantee for any single parameter
 * set—empirical collision rates on a finite sample do not constitute a proof of universality.</p>
 *
 * <h2>Complexity</h2>
 * <p>{@link #hash(long)} performs one modular multiplication, one modular addition, and one
 * final modulo—all O(1) on the word-RAM model (or O(log² p) bit operations using the
 * overflow-safe {@link ModularArithmetic}).</p>
 *
 * <h2>Default prime</h2>
 * <p>{@link #DEFAULT_PRIME} = 2^61 − 1 = 2305843009213693951, a Mersenne prime.</p>
 */
public final class UniversalHashFamily {

    /** 2^61 − 1, a known Mersenne prime. */
    public static final long DEFAULT_PRIME = 2305843009213693951L;

    private final long a;
    private final long b;
    private final long p;
    private final int m;

    /**
     * @param a         multiplier in {@code [1, p−1]}
     * @param b         offset in {@code [0, p−1]}
     * @param p         prime modulus (must be &gt; 2)
     * @param m         table size in {@code [1, p)}
     * @throws IllegalArgumentException if parameters are out of range
     */
    public UniversalHashFamily(long a, long b, long p, int m) {
        if (p <= 2) {
            throw new IllegalArgumentException("p must be > 2, got " + p);
        }
        if (m < 1 || m >= p) {
            throw new IllegalArgumentException("m must be in [1, p), got " + m);
        }
        if (a < 1 || a >= p) {
            throw new IllegalArgumentException("a must be in [1, p), got " + a);
        }
        if (b < 0 || b >= p) {
            throw new IllegalArgumentException("b must be in [0, p), got " + b);
        }
        this.a = a;
        this.b = b;
        this.p = p;
        this.m = m;
    }

    /**
     * Creates a random member of the universal family.
     *
     * @param p         prime modulus
     * @param tableSize table size m (must be in {@code [1, p)})
     * @param rng       random source
     * @return a fresh universal hash function
     */
    public static UniversalHashFamily random(long p, int tableSize, RandomSource rng) {
        if (p <= 2) {
            throw new IllegalArgumentException("p must be > 2");
        }
        if (tableSize < 1 || tableSize >= p) {
            throw new IllegalArgumentException("tableSize must be in [1, p)");
        }
        long a = 1 + rng.nextLong(p - 1);
        long b = rng.nextLong(p);
        return new UniversalHashFamily(a, b, p, tableSize);
    }

    /**
     * Hashes {@code key} into {@code [0, m)}.
     *
     * <p>Negative keys are normalised modulo {@code p} before hashing so the result is
     * always well-defined.</p>
     */
    public int hash(long key) {
        long x = key % p;
        if (x < 0) {
            x += p;
        }
        long ax = ModularArithmetic.multiplyMod(a, x, p);
        long result = ModularArithmetic.addMod(ax, b, p);
        return (int) (result % m);
    }

    public long getA() {
        return a;
    }

    public long getB() {
        return b;
    }

    public long getPrime() {
        return p;
    }

    public int getTableSize() {
        return m;
    }

    @Override
    public String toString() {
        return "UniversalHashFamily{a=" + a + ", b=" + b + ", p=" + p + ", m=" + m + "}";
    }
}
