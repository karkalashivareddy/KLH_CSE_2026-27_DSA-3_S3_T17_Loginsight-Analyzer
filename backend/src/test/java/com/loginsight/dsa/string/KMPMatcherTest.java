package com.loginsight.dsa.string;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KMPMatcherTest {

    private final KMPMatcher matcher = new KMPMatcher();

@Test
    void singleAndMultipleMatches() {
        assertArrayEquals(new int[]{0, 3}, matcher.match("abcabcab", "abc").getMatchPositions());
        assertArrayEquals(new int[]{0, 3, 6}, matcher.match("abcabcabc", "abc").getMatchPositions());
        assertArrayEquals(new int[]{1, 4, 7}, matcher.match("xbcxbcxbc", "bc").getMatchPositions());
    }

    @Test
    void overlappingMatches() {
        assertArrayEquals(new int[]{0, 1, 2}, matcher.match("aaaa", "aa").getMatchPositions());
        assertArrayEquals(new int[]{0, 2, 4}, matcher.match("ababab", "ab").getMatchPositions());
    }

    @Test
    void repeatedCharacterPattern() {
        assertEquals(999, matcher.match(
                repeat('a', 1000), repeat('a', 2)).getMatchCount());
        assertEquals(1, matcher.match(
                repeat('a', 1000), repeat('a', 1000)).getMatchCount());
    }

    @Test
    void lpsBuiltCorrectly() {
        assertArrayEquals(new int[]{0, 0, 0, 0}, KMPMatcher.buildLps("ABCD".toCharArray()));
        assertArrayEquals(new int[]{0, 1, 2, 3}, KMPMatcher.buildLps("AAAA".toCharArray()));
        assertArrayEquals(new int[]{0, 1, 0, 1, 2, 3}, KMPMatcher.buildLps("AABAAB".toCharArray()));
        assertArrayEquals(new int[]{0, 1, 0, 1, 2, 0, 1, 2, 3, 4, 5},
                KMPMatcher.buildLps("AABAACAABAA".toCharArray()));
        assertArrayEquals(new int[]{0, 0, 1, 2, 3, 4}, KMPMatcher.buildLps("ABABAB".toCharArray()));
    }

    @Test
    void lpsFromIntermediateData() {
        StringSearchResult r = matcher.match("AABAACAABAA", "AABA");
        assertEquals("KMP", r.getAlgorithm());
        assertArrayEquals(new int[]{0, 1, 0, 1}, r.intermediateDataAsInts());
    }

    @Test
    void matchesReferenceValues() {
        String text = "ABABDABACDABABCABAB";
        String pattern = "ABABCABAB";
        assertArrayEquals(new int[]{10}, matcher.match(text, pattern).getMatchPositions());
        assertArrayEquals(new int[]{0, 6, 11},
                matcher.match("ABAB DABAB ABAB", "ABAB").getMatchPositions());
    }

    @Test
    void unicodeDevanagariText() {
        String text = "अरे याव दिवसा अरे";
        assertArrayEquals(new int[]{0, 14}, matcher.match(text, "अरे").getMatchPositions());
    }

    @Test
    void patternLongerThanTextIsEmpty() {
        assertEquals(0, matcher.match("short", "much longer pattern").getMatchCount());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> matcher.match(null, "ab"));
        assertThrows(IllegalArgumentException.class, () -> matcher.match("ab", (String) null));
        assertThrows(IllegalArgumentException.class, () -> matcher.match("ab", ""));
    }

    private static String repeat(char c, int n) {
        char[] chars = new char[n];
        for (int i = 0; i < n; i++) {
            chars[i] = c;
        }
        return new String(chars);
    }
}