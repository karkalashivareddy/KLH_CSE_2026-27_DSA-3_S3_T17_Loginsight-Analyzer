package com.loginsight.dsa.string.suffix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

class SuffixArrayBuilderTest {

    @Test
    void bananaSuffixArray() {
        SuffixArray sa = SuffixArray.build("banana");
        assertArrayEquals(new int[]{5, 3, 1, 0, 4, 2}, sa.getSuffixArray());
    }

    @Test
    void mississippiSuffixArray() {
        SuffixArray sa = SuffixArray.build("mississippi");
        // standard SA: i(10) ippi(7) issippi(4) ississippi(1) mississippi(0) pi(9) ppi(8) sippi(6) sissippi(3) ssippi(5) ssissippi(2)
        assertArrayEquals(new int[]{10, 7, 4, 1, 0, 9, 8, 6, 3, 5, 2}, sa.getSuffixArray());
    }

    @Test
    void singleCharacterText() {
        SuffixArray sa = SuffixArray.build("x");
        assertArrayEquals(new int[]{0}, sa.getSuffixArray());
    }

    @Test
    void emptyText() {
        assertArrayEquals(new int[0], SuffixArray.build("").getSuffixArray());
    }

    @Test
    void allSameCharactersIsSorted() {
        SuffixArray sa = SuffixArray.build("aaaaa");
        int[] arr = sa.getSuffixArray();
        assertEquals(5, arr.length);
        for (int i = 0; i < 4; i++) {
            assertTrue(arr[i] > arr[i + 1], "suffix at " + arr[i] + " should be lex after " + arr[i + 1]);
        }
    }

    @Test
    void lexicographicOrderIsSortedForDeterministicRandom() {
        Random rng = new Random(42L);
        for (int trial = 0; trial < 10; trial++) {
            int n = 50 + rng.nextInt(50);
            char[] chars = new char[n];
            for (int i = 0; i < n; i++) {
                chars[i] = (char) ('a' + rng.nextInt(4));
            }
            String text = new String(chars);
            SuffixArray sa = SuffixArray.build(text);
            int[] arr = sa.getSuffixArray();
            for (int i = 0; i < n - 1; i++) {
                String a = text.substring(arr[i]);
                String b = text.substring(arr[i + 1]);
                assertTrue(a.compareTo(b) < 0,
                        "non-decreasing at index " + i + "; " + arr[i] + " should be lex before " + arr[i + 1]);
            }
        }
    }

    @Test
    void searchFindsAllMatches() {
        // Results come back in suffix-array (lexicographic) order, not position order.
        SuffixArray sa = SuffixArray.build("abracadabra");
        assertArrayEquals(new int[]{7, 0}, sa.search("abra"));
        assertArrayEquals(new int[]{10, 7, 0, 3, 5}, sa.search("a"));
        assertArrayEquals(new int[0], sa.search("xyz"));
        assertArrayEquals(new int[]{0}, sa.search("abracadabra"));
    }

    @Test
    void searchAgreesWithNaiveOnDeterministicRandom() {
        Random rng = new Random(99L);
        String[] patterns = {"ab", "abc", "a", "cde", "xyz", "banana"};
        for (int trial = 0; trial < 20; trial++) {
            int n = 30 + rng.nextInt(70);
            char[] chars = new char[n];
            for (int i = 0; i < n; i++) {
                chars[i] = (char) ('a' + rng.nextInt(6));
            }
            String text = new String(chars);
            SuffixArray sa = SuffixArray.build(text);
            for (String p : patterns) {
                int[] expected = naive(text, p);
                int[] actual = sa.search(p);
                Arrays.sort(expected);
                Arrays.sort(actual);
                assertArrayEquals(expected, actual, "trial " + trial + " pattern=" + p);
            }
        }
    }

    @Test
    void searchRejectsNullAndEmptyPattern() {
        SuffixArray sa = SuffixArray.build("text");
        assertThrows(IllegalArgumentException.class, () -> sa.search(null));
        assertThrows(IllegalArgumentException.class, () -> sa.search(""));
    }

    @Test
    void buildRejectsNullText() {
        assertThrows(IllegalArgumentException.class, () -> SuffixArray.build(null));
    }

    @Test
    void suffixAtReturnsCorrectSubstring() {
        SuffixArray sa = SuffixArray.build("banana");
        assertEquals("a", sa.suffixAt(0));
        assertEquals("ana", sa.suffixAt(1));
        assertEquals("banana", sa.suffixAt(3));
    }

    private static int[] naive(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        int[] positions = new int[n - m + 1 > 0 ? n - m + 1 : 0];
        int count = 0;
        for (int i = 0; i <= n - m; i++) {
            boolean match = true;
            for (int j = 0; j < m; j++) {
                if (text.charAt(i + j) != pattern.charAt(j)) {
                    match = false;
                    break;
                }
            }
            if (match) positions[count++] = i;
        }
        return Arrays.copyOf(positions, count);
    }
}