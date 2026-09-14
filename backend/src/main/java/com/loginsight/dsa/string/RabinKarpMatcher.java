package com.loginsight.dsa.string;

/**
 * Algorithm: Rabin-Karp pattern matching with polynomial rolling hash (modular arithmetic), an
 * optional second independent hash (double hashing), and mandatory character-by-character
 * verification of every candidate.
 * <p>
 * Purpose: compare the pattern to each text window by a cheap constant-time hash comparison,
 * deferring the costly character comparison until hashes agree.
 * <p>
 * Input: text (length n), pattern (length m).
 * <p>
 * Output: every start index where the window genuinely equals the pattern (overlaps included).
 * <p>
 * Preprocessing: hash of the pattern under two independent (prime modulus, large base) pairs, and
 * the precomputed {@code base^(m-1)} powers. In double-hash mode both hashes must agree.
 * <p>
 * Time Complexity: O(n + m) average — the rolling update is O(1) per window and every window whose
 * hashes match is verified character-by-character (O(m) each, so worst case all windows verify:
 * O(n·m)). Space Complexity: O(1) auxiliary (two powers and two running hashes).
 * <p>
 * Hash collision vs actual match: a hash match is only a candidate. It can be a true match (chars
 * agree) or a collision (different text with the same polynomial remainder). Verification always
 * runs; collisions are counted and reported (docs/13 §6). Double hashing reduces the collision
 * probability because a false candidate must survive two independent (mod, base) pairings
 * simultaneously — but this never reaches zero, which is exactly why verification is kept.
 * <p>
 * Empty-contract: empty/null pattern rejected; pattern longer than text yields an empty result.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Rabin-Karp_algorithm">Rabin-Karp algorithm</a>
 */
public final class RabinKarpMatcher implements StringMatcher {

    private static final String ALGORITHM = "Rabin-Karp";
    private static final long MOD1 = 1_000_000_007L;
    private static final long MOD2 = 1_000_000_009L;
    /** Large odd base; both moduli are prime and neither divides it, so powers are non-periodic. */
    private static final long BASE = 911_382_323L;

    @Override
    public StringSearchResult match(String text, String pattern) {
        return match(text, pattern, true);
    }

    /**
     * Search with explicit control over double hashing (single-hash mode is used by tests that
     * construct deliberate collisions to prove the verification step).
     */
    public StringSearchResult match(String text, String pattern, boolean doubleHash) {
        TextValidator.requireNonNull(text, pattern);
        TextValidator.requireNonEmpty(pattern);
        char[] t = text.toCharArray();
        char[] p = pattern.toCharArray();
        int n = t.length;
        int m = p.length;

        if (m > n) {
            return new StringSearchResult(ALGORITHM, pattern, n, new int[0], 0,
                    "O(n + m) avg, O(n*m) worst", "O(1) auxiliary", 0);
        }

        long start = System.nanoTime();
        long hash1 = rollingHash(p, MOD1, m);
        long hash2 = doubleHash ? rollingHash(p, MOD2, m) : 0;
        long power1 = powerMod(BASE, m - 1, MOD1);
        long power2 = doubleHash ? powerMod(BASE, m - 1, MOD2) : 0;

        long window1 = m == 0 ? 0 : rollingHash(t, MOD1, m);
        long window2 = doubleHash ? (m == 0 ? 0 : rollingHash(t, MOD2, m)) : 0;

        int[] positions = new int[n - m + 1 > 0 ? n - m + 1 : 0];
        int collisions = 0;
        int count = 0;
        for (int i = 0; i <= n - m; i++) {
            boolean hashCandidate = window1 == hash1 && (!doubleHash || window2 == hash2);
            if (i + m < n) {
                window1 = roll(window1, t[i], t[i + m], power1, MOD1);
                if (doubleHash) {
                    window2 = roll(window2, t[i], t[i + m], power2, MOD2);
                }
            }
            if (hashCandidate) {
                if (matchesAt(t, p, i)) {
                    positions[count++] = i;
                } else {
                    collisions++;
                }
            }
        }
        long elapsed = System.nanoTime() - start;

        return new StringSearchResult(ALGORITHM, pattern, n, copy(positions, count), elapsed,
                "O(n + m) avg, O(n*m) worst", "O(1) auxiliary", collisions);
    }

    /** Polynomial hash: {@code sum char[i] * base^(m-1-i) mod mod}. */
    static long rollingHash(char[] s, long mod, int length) {
        long h = 0;
        for (int i = 0; i < length; i++) {
            h = (h * BASE + s[i]) % mod;
        }
        return h;
    }

    /** Slide the window: drop the outgoing char's contribution, shift, add the incoming char. */
    static long roll(long hash, char out, char in, long power, long mod) {
        long shifted = (hash - out * power) % mod;
        if (shifted < 0) {
            shifted += mod;
        }
        return (shifted * BASE + in) % mod;
    }

    static long powerMod(long base, long exp, long mod) {
        long result = 1;
        long b = base % mod;
        long e = exp;
        while (e > 0) {
            if ((e & 1L) == 1) {
                result = (result * b) % mod;
            }
            b = (b * b) % mod;
            e >>= 1;
        }
        return result;
    }

    private static boolean matchesAt(char[] text, char[] pattern, int offset) {
        for (int j = 0; j < pattern.length; j++) {
            if (text[offset + j] != pattern[j]) {
                return false;
            }
        }
        return true;
    }

    private static int[] copy(int[] positions, int size) {
        if (size == positions.length) {
            return positions;
        }
        int[] trimmed = new int[size];
        System.arraycopy(positions, 0, trimmed, 0, size);
        return trimmed;
    }
}