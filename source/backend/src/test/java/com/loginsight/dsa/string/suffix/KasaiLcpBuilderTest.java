package com.loginsight.dsa.string.suffix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

class KasaiLcpBuilderTest {

    @Test
    void bananaLcp() {
        int[] sa = SuffixArray.build("banana").getSuffixArray();
        int[] lcp = KasaiLCP.build("banana".toCharArray(), sa);
        assertArrayEquals(new int[]{0, 1, 3, 0, 0, 2}, lcp);
    }

    @Test
    void mississippiLcp() {
        int[] sa = SuffixArray.build("mississippi").getSuffixArray();
        int[] lcp = KasaiLCP.build("mississippi".toCharArray(), sa);
        // lcp[i] = LCP(sa[i-1], sa[i]); verified by hand against the SA order below.
        assertArrayEquals(new int[]{10, 7, 4, 1, 0, 9, 8, 6, 3, 5, 2}, sa);
        assertArrayEquals(new int[]{0, 1, 1, 4, 0, 0, 1, 0, 2, 1, 3}, lcp);
    }

    @Test
    void emptyTextYieldsEmptyLcp() {
        int[] lcp = KasaiLCP.build(new char[0], new int[0]);
        assertArrayEquals(new int[0], lcp);
    }

    @Test
    void lcpAgreesWithOReferenceOnDeterministicPseudoRandom() {
        Random rng = new Random(42L);
        for (int trial = 0; trial < 20; trial++) {
            int n = 20 + rng.nextInt(60);
            char[] chars = new char[n];
            for (int i = 0; i < n; i++) {
                chars[i] = (char) ('a' + rng.nextInt(5));
            }
            SuffixArray sa = SuffixArray.build(new String(chars));
            int[] saArray = sa.getSuffixArray();
            int[] lcp = KasaiLCP.build(chars, saArray);

            assertEquals(n, lcp.length);
            assertArrayEquals(lcp, referenceLcp(chars, saArray),
                    "trial " + trial);
        }
    }

    @Test
    void lcpFirstElementIsAlwaysZero() {
        int[] sa = SuffixArray.build("loginsight").getSuffixArray();
        assertEquals(0, KasaiLCP.build("loginsight".toCharArray(), sa)[0]);
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> KasaiLCP.build(null, new int[]{}));
        assertThrows(IllegalArgumentException.class, () -> KasaiLCP.build("a".toCharArray(), null));
        assertThrows(IllegalArgumentException.class, () -> KasaiLCP.build("ab".toCharArray(), new int[]{}));
    }

    @Test
    void rejectsNonPermutationSuffixArray() {
        assertThrows(IllegalArgumentException.class,
                () -> KasaiLCP.build("abc".toCharArray(), new int[]{0, 1, 1}));
    }

    private static int[] referenceLcp(char[] text, int[] sa) {
        int n = text.length;
        int[] lcp = new int[n];
        for (int i = 1; i < n; i++) {
            lcp[i] = lcpSlow(text, sa[i - 1], sa[i]);
        }
        return lcp;
    }

    private static int lcpSlow(char[] text, int a, int b) {
        int count = 0;
        while (a + count < text.length && b + count < text.length && text[a + count] == text[b + count]) {
            count++;
        }
        return count;
    }

    private static void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}