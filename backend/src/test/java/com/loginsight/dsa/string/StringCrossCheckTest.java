package com.loginsight.dsa.string;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.loginsight.dsa.string.aho.AhoCorasick;
import com.loginsight.dsa.string.aho.PatternMatch;
import com.loginsight.dsa.string.suffix.KasaiLCP;
import com.loginsight.dsa.string.suffix.SuffixArray;

/**
 * Cross-algorithm consistency tests (docs/13 §8): every fast matcher must agree with the Naive
 * baseline, Aho-Corasick must agree per pattern, and the suffix structures must agree with
 * independent reference implementations. This is the "multi-algorithm validation" gate expected of
 * the DSA engine.
 */
class StringCrossCheckTest {

    private static final NaiveMatcher NAIVE = new NaiveMatcher();
    private static final KMPMatcher KMP = new KMPMatcher();
    private static final ZAlgorithm Z = new ZAlgorithm();
    private static final RabinKarpMatcher RABIN_KARP = new RabinKarpMatcher();

    private final List<String> corpus = Arrays.asList(
            "banana",
            "abracadabra",
            "abcabcabcabc",
            "aaaa",
            "galatasaray galatasaray",
            "the quick brown fox jumps over the lazy dog",
            "2026-09-14T10:00:00.000Z ERROR database connection failed for order 42",
            "file response ok file error contact file system file",
            "भारत देश का नाम भारत है",
            "mississippi",
            "abcdefgh",
            "a",
            ""
    );

    private final List<String> patterns = Arrays.asList(
            "banana", "ana", "ab", "abc", "aa", "quick", "fox", "dog", "file",
            "भारत", "x", "a", "abcdefgh", "galatasaray", "zzzz-no-such-pattern"
    );

    @Test
    void naiveEqualsKmpEqualsZEqualsRabinKarpOnWholeCorpus() {
        for (String text : corpus) {
            for (String pattern : patterns) {
                int[] expected = positionsOf(NAIVE, text, pattern);
                assertArrayEquals(expected, positionsOf(KMP, text, pattern), "KMP on text=" + quote(text) + " pattern=" + quote(pattern));
                assertArrayEquals(expected, positionsOf(Z, text, pattern), "Z on text=" + quote(text) + " pattern=" + quote(pattern));
                assertArrayEquals(expected, positionsOf(RABIN_KARP, text, pattern), "Rabin-Karp on text=" + quote(text) + " pattern=" + quote(pattern));
            }
        }
    }

    @Test
    void naiveBaselineEqualsAhoCorasickPerPattern() {
        for (String text : corpus) {
            if (text.isEmpty()) {
                continue;
            }
            AhoCorasick ac = new AhoCorasick(patterns.toArray(new String[0]));
            for (PatternMatch match : ac.search(text)) {
                assertTrue(positionsOf(NAIVE, text, match.getPattern()).length > 0,
                        "AC reported " + match + " which naive does not see in " + quote(text));
            }
            int acTotal = ac.search(text).length;
            int naiveTotal = 0;
            for (String pattern : patterns) {
                naiveTotal += positionsOf(NAIVE, text, pattern).length;
            }
            assertTrue(acTotal == naiveTotal,
                    "AC total " + acTotal + " != naive total " + naiveTotal + " on text=" + quote(text));
        }
    }

    @Test
    void suffixArrayIsLexicographicallySortedLikeReference() {
        Random rng = new Random(7L);
        for (int trial = 0; trial < 15; trial++) {
            int n = 10 + rng.nextInt(40);
            char[] chars = new char[n];
            for (int i = 0; i < n; i++) {
                chars[i] = (char) ('a' + rng.nextInt(4));
            }
            String text = new String(chars);
            String[] suffixes = new String[n];
            for (int i = 0; i < n; i++) {
                suffixes[i] = text.substring(i);
            }
            String[] reference = suffixes.clone();
            Arrays.sort(reference);

            SuffixArray sa = SuffixArray.build(text);
            int[] array = sa.getSuffixArray();
            for (int i = 0; i < n; i++) {
                assertTrue(text.substring(array[i]).equals(reference[i]),
                        "SA order mismatch at " + i + " for text=" + quote(text));
            }
        }
    }

    @Test
    void suffixArraySearchAgreesWithMatchersAcrossCorpus() {
        for (String text : corpus) {
            SuffixArray sa = SuffixArray.build(text);
            for (String pattern : patterns) {
                if (pattern.isEmpty()) {
                    continue;
                }
                int[] expected = positionsOf(NAIVE, text, pattern);
                int[] found = sa.search(pattern);
                Arrays.sort(found);
                Arrays.sort(expected);
                assertArrayEquals(expected, found, "SA search on text=" + quote(text) + " pattern=" + quote(pattern));
            }
        }
    }

    @Test
    void kasaiLcpAgainstQuadraticReference() {
        Random rng = new Random(11L);
        String[] fixed = {"banana", "mississippi", "aaaa", "abababab", "abcabc"};
        List<String> texts = new ArrayList<>(Arrays.asList(fixed));
        for (int i = 0; i < 10; i++) {
            int n = 20 + rng.nextInt(40);
            char[] chars = new char[n];
            for (int j = 0; j < n; j++) {
                chars[j] = (char) ('a' + rng.nextInt(3));
            }
            texts.add(new String(chars));
        }
        for (String text : texts) {
            SuffixArray sa = SuffixArray.build(text);
            int[] lcp = KasaiLCP.build(text.toCharArray(), sa.getSuffixArray());
            assertArrayEquals(referenceLcp(text.toCharArray(), sa.getSuffixArray()), lcp,
                    "Kasai on " + quote(text));
            assertTrue(lcp.length == text.length());
        }
    }

    @Test
    void allMatchersReturnIdenticalResultMetadata() {
        String text = "database connection failed on database";
        String pattern = "database";
        // identical matchPositions across all single-pattern matchers
        int[] expected = NAIVE.match(text, pattern).getMatchPositions();
        assertArrayEquals(expected, KMP.match(text, pattern).getMatchPositions());
        assertArrayEquals(expected, Z.match(text, pattern).getMatchPositions());
        assertArrayEquals(expected, RABIN_KARP.match(text, pattern).getMatchPositions());
        // suffix array search reports suffix-lexicographic order, so normalize to ascending
        int[] fromSuffixArray = SuffixArray.build(text).search(pattern);
        Arrays.sort(fromSuffixArray);
        assertArrayEquals(expected, fromSuffixArray);
    }

    private static int[] positionsOf(StringMatcher matcher, String text, String pattern) {
        if (pattern.isEmpty()) {
            return new int[0];
        }
        return matcher.match(text, pattern).getMatchPositions().clone();
    }

    private static int[] referenceLcp(char[] text, int[] sa) {
        int n = text.length;
        int[] lcp = new int[n];
        for (int i = 1; i < n; i++) {
            lcp[i] = slow(sa[i - 1], sa[i], text);
        }
        return lcp;
    }

    private static int slow(int a, int b, char[] text) {
        int k = 0;
        while (a + k < text.length && b + k < text.length && text[a + k] == text[b + k]) {
            k++;
        }
        return k;
    }

    private static String quote(String s) {
        return '"' + s + '"';
    }
}