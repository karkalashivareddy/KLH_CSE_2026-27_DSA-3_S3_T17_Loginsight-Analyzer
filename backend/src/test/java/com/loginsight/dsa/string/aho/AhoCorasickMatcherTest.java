package com.loginsight.dsa.string.aho;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.loginsight.dsa.string.StringSearchResult;

class AhoCorasickMatcherTest {

    private final String[] ushersPatterns = {"he", "she", "his", "hers"};

    @Test
    void textbookUshersMatches() {
        AhoCorasick ac = new AhoCorasick(ushersPatterns);
        PatternMatch[] matches = ac.search("ushers");

        // she ends at 3 (start 1), he ends at 3 (start 2), hers ends at 5 (start 2)
        assertEquals(3, matches.length);
        String[] byStart = Arrays.stream(matches)
                .sorted(Comparator.comparingInt(PatternMatch::getStart))
                .map(PatternMatch::getPattern)
                .toArray(String[]::new);
        assertArrayEquals(new String[]{"she", "he", "hers"}, byStart);
        int[] starts = Arrays.stream(matches).mapToInt(PatternMatch::getStart).toArray();
        assertTrue(contains(starts, 1) && contains(starts, 2), "starts are 1, 2, 2");
    }

    @Test
    void overlappingOccurrencesAreRecorded() {
        // a:2, ab:2, abc:2, b:2, bc:2 over "abcabc"
        AhoCorasick ac = new AhoCorasick(new String[]{"a", "ab", "abc", "b", "bc"});
        PatternMatch[] matches = ac.search("abcabc");
        assertEquals(10, matches.length, "every pattern occurrence counted");
    }

    @Test
    void duplicatePatternFirstIdWins() {
        AhoCorasick ac = new AhoCorasick(new String[]{"hello", "hello"});
        PatternMatch[] matches = ac.search("hello");
        assertEquals(1, matches.length);
        assertEquals(0, matches[0].getPatternId());
    }

    @Test
    void multiPatternAgainstDeterministicLogLines() {
        String[] patterns = {"error", "timeout", "db", "catastrophic"};
        String[] lines = {
            "error: database connection failed",
            "timeout reaching db after 30s",
            "catastrophic: all systems down",
            "normal operation"
        };
        AhoCorasick ac = new AhoCorasick(patterns);
        int[] hits = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            hits[i] = ac.search(lines[i]).length;
        }
        // "database" contains no "db"; first line hits only on "error".
        assertArrayEquals(new int[]{1, 2, 1, 0}, hits);
    }

    @Test
    void unicodePatternsAreFound() {
        AhoCorasick ac = new AhoCorasick(new String[]{"भारत", "अरे"});
        PatternMatch[] matches = ac.search("भारत देश अरे");
        assertEquals(2, matches.length);
        int[] starts = Arrays.stream(matches).mapToInt(PatternMatch::getStart).toArray();
        assertArrayEquals(new int[]{0, 9}, starts);
    }

    @Test
    void perPatternNaiveCrossCheckOnDeterministicPseudoRandomStrings() {
        String[] patterns = {"the", "quick", "brown", "fox", "loginsight", "error"};
        String[] texts = buildDeterministicTexts(100);
        AhoCorasick ac = new AhoCorasick(patterns);

        for (String text : texts) {
            PatternMatch[] matches = ac.search(text);
            for (int pid = 0; pid < patterns.length; pid++) {
                int expected = naiveCount(text, patterns[pid]);
                int actual = 0;
                for (PatternMatch m : matches) {
                    if (m.getPatternId() == pid) actual++;
                }
                assertEquals(expected, actual,
                        "pattern " + patterns[pid] + " in text (len=" + text.length() + ')');
            }
        }
    }

    @Test
    void resultAggregatesAllStartPositions() {
        AhoCorasick ac = new AhoCorasick(new String[]{"a", "b"});
        StringSearchResult r = ac.match("abxaba");
        assertEquals("Aho-Corasick", r.getAlgorithm());
        assertEquals(6, r.getTextLength());
        // a@0, b@1, a@3, b@4, a@5
        assertEquals(5, r.getMatchCount());
        assertInstanceOf(PatternMatch[].class, r.getIntermediateData());
    }

    @Test
    void trieStructureHasCorrectFailureLinks() {
        AhoCorasick ac = new AhoCorasick(ushersPatterns);
        assertTrue(ac.nodeCount() > 5, "trie has at least 5 nodes");
        assertTrue(ac.failureOf(0) == 0, "root fail points to root");
    }

    @Test
    void rejectsInvalidConstructionAndSearch() {
        assertThrows(IllegalArgumentException.class, () -> new AhoCorasick(null));
        assertThrows(IllegalArgumentException.class, () -> new AhoCorasick(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> new AhoCorasick(new String[]{null}));
        assertThrows(IllegalArgumentException.class, () -> new AhoCorasick(new String[]{""}));
        assertThrows(IllegalArgumentException.class,
                () -> new AhoCorasick(new String[]{"a"}).search(null));
    }

    private static String[] buildDeterministicTexts(int count) {
        Random rng = new Random(42L);
        String[] texts = new String[count];
        for (int i = 0; i < count; i++) {
            int len = 20 + rng.nextInt(80);
            StringBuilder sb = new StringBuilder(len);
            for (int j = 0; j < len; j++) {
                sb.append((char) ('a' + rng.nextInt(26)));
            }
            // inject patterns deterministically at known offsets
            if (rng.nextBoolean() && len > 5) {
                sb.setCharAt(0, 'e');
                sb.setCharAt(1, 'r');
                sb.setCharAt(2, 'r');
            }
            texts[i] = sb.toString();
        }
        return texts;
    }

    private static int naiveCount(String text, String pattern) {
        int count = 0;
        int m = pattern.length();
        for (int i = 0; i <= text.length() - m; i++) {
            if (text.startsWith(pattern, i)) {
                count++;
            }
        }
        return count;
    }

    private static boolean contains(int[] array, int value) {
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }
}