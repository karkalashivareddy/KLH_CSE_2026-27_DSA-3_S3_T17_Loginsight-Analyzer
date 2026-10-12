package com.loginsight.dsa.string;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RabinKarpMatcherTest {

    private final RabinKarpMatcher matcher = new RabinKarpMatcher();

    @Test
    void singleAndMultipleMatches() {
        assertArrayEquals(new int[]{0}, matcher.match("sanfoundry example", "san").getMatchPositions());
        assertArrayEquals(new int[]{0, 3, 6}, matcher.match("abcabcabc", "abc").getMatchPositions());
        assertArrayEquals(new int[]{2}, matcher.match("xxabcxab", "abc").getMatchPositions());
    }

    @Test
    void overlappingMatches() {
        assertArrayEquals(new int[]{0, 1, 2}, matcher.match("aaaa", "aa").getMatchPositions());
        assertArrayEquals(new int[]{0, 2, 4}, matcher.match("ababab", "ab").getMatchPositions());
    }

    @Test
    void rollingHashIsPolynomial() {
        assertEquals(rollingHashReference("abc"),
                RabinKarpMatcher.rollingHash(new char[]{'a', 'b', 'c'}, 1_000_000_007L, 3));
    }

    @Test
    void doubleHashAgreesWithSingleHashOnOrdinaryText() {
        String text = "the needle in the haystack needle needle";
        String pattern = "needle";
        int[] doubled = matcher.match(text, pattern).getMatchPositions();
        int[] single = matcher.match(text, pattern, false).getMatchPositions();
        assertArrayEquals(doubled, single);
    }

    @Test
    void hashCollisionIsDetectedAndVerificationRejects() {
        String[] colliding = findMod1Collision();
        String pattern = colliding[0];
        String bait = colliding[1];
        String text = pattern + "x" + bait + "y";

        // Both report only the genuine match (verification always runs).
        int[] expected = new NaiveMatcher().match(text, pattern).getMatchPositions();
        assertArrayEquals(expected, matcher.match(text, pattern).getMatchPositions());
        assertArrayEquals(expected, matcher.match(text, pattern, false).getMatchPositions());

        // Single hash: the crafted window is a hash candidate but fails verification.
        int singleCollisions = (Integer) matcher.match(text, pattern, false).getIntermediateData();
        assertTrue(singleCollisions >= 1);
    }

    @Test
    void noCollisionsReportedOnNormalInput() {
        Integer collisions = (Integer) matcher.match("a somewhat longer text", "text").getIntermediateData();
        assertEquals(0, collisions);
    }

    @Test
    void intermediateDataIsCollisionCount() {
        Object data = matcher.match("log line with log repeated", "log").getIntermediateData();
        assertInstanceOf(Integer.class, data);
        assertEquals("Rabin-Karp", matcher.match("log line", "log").getAlgorithm());
    }

    @Test
    void unicodeText() {
        String text = "भारत देश भारतीय भारत";
        assertArrayEquals(new int[]{0, 9, 16}, matcher.match(text, "भारत").getMatchPositions());
        assertArrayEquals(new int[]{9}, matcher.match(text, "भारतीय").getMatchPositions());
    }

    @Test
    void edgeCasesWithRollingWindow() {
        assertArrayEquals(new int[]{0}, matcher.match("a", "a").getMatchPositions());
        assertArrayEquals(new int[0], matcher.match("ab", "abc").getMatchPositions());
        assertEquals(3, matcher.match("zzz", "z").getMatchCount());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> matcher.match(null, "ab"));
        assertThrows(IllegalArgumentException.class, () -> matcher.match("ab", (String) null));
        assertThrows(IllegalArgumentException.class, () -> matcher.match("ab", ""));
    }

    private static long rollingHashReference(String s) {
        // (c0*B + c1)*B + c2 ... == (((c0)*B+c1)*B + c2) mod M
        final long mod = 1_000_000_007L;
        long h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = h * 911_382_323L + s.charAt(i);
            h %= mod;
        }
        return h;
    }

    /**
     * Deterministically construct a collision under MOD1. For a 2-char window the hash is
     * {@code c0*B + c1 mod M}, so two distinct windows collide iff {@code Δ0*B + Δ1 ≡ 0 (mod M)}.
     * Find k = Δ0 such that {@code k*B mod M} lands within 65,000 of 0 (top or bottom petal),
     * then fix Δ1 accordingly — with k up to 65,000 there are several such hits for the fixed
     * constants, and characters are chosen inside the valid UTF-16 range.
     */
    private static String[] findMod1Collision() {
        final long mod = 1_000_000_007L;
        final long base = 911_382_323L;
        for (long k = 1; k <= 65_000; k++) {
            long x = (k * base) % mod;
            if (x == 0) {
                continue;
            }
            if (x <= 65_000) { // bottom petal: Δ1 = -x
                char c0 = (char) (500 + k);
                char d0 = 500;
                char d1 = (char) (500 + x); // 500 + 65000 = 65500 <= 65535
                String s1 = new String(new char[]{c0, 500});
                String s2 = new String(new char[]{d0, d1});
                return new String[]{s1, s2};
            }
            long y = mod - x; // top petal: Δ1 = +y
            if (y <= 65_000) {
                char c1 = (char) (500 + y); // >= 500 so c1 - y = 500 stays valid
                String s1 = new String(new char[]{(char) (500 + k), c1});
                String s2 = new String(new char[]{500, 500});
                return new String[]{s1, s2};
            }
        }
        throw new AssertionError("no collision found in the searched space");
    }
}