package com.loginsight.dsa.string;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ZAlgorithmTest {

    private final ZAlgorithm matcher = new ZAlgorithm();

    @Test
    void singleAndMultipleMatches() {
        assertArrayEquals(new int[]{0, 7}, matcher.match("abracadabra", "abra").getMatchPositions());
        assertArrayEquals(new int[]{0, 3, 6}, matcher.match("abcabcabc", "abc").getMatchPositions());
        assertArrayEquals(new int[]{2}, matcher.match("zzabcxx", "abc").getMatchPositions());
    }

    @Test
    void overlappingMatches() {
        assertArrayEquals(new int[]{0, 1, 2}, matcher.match("aaaa", "aa").getMatchPositions());
        assertArrayEquals(new int[]{1, 7, 13}, matcher.match("India india India", "ndia").getMatchPositions());
    }

    @Test
    void zArrayValuesAreLinearForRepeatedChars() {
        // aaaa -> z = [4, 3, 2, 1]
        assertArrayEquals(new int[]{4, 3, 2, 1}, ZAlgorithm.buildZArray("aaaa".toCharArray()));
        assertArrayEquals(new int[]{6, 5, 4, 3, 2, 1}, ZAlgorithm.buildZArray("aaaaaa".toCharArray()));
    }

    @Test
    void zArrayForAba() {
        assertArrayEquals(new int[]{3, 0, 1}, ZAlgorithm.buildZArray("aba".toCharArray()));
    }

    @Test
    void separatorSwappedInForSharedAlphabet() {
        // Pattern and text share characters, so the separator must come from outside their union.
        String result = String.valueOf(ZAlgorithm.findSeparator("aba".toCharArray(), "b".toCharArray()));
        assertEquals(1, result.length());
    }

    @Test
    void unicodeDevanagariText() {
        String text = "कर्म कर्मणी कर्म";
        assertArrayEquals(new int[]{0, 5, 12}, matcher.match(text, "कर्म").getMatchPositions());
    }

    @Test
    void intermediateDataIsZOfConcatenation() {
        StringSearchResult r = matcher.match("xxab", "ab");
        int[] z = r.intermediateDataAsInts();
        assertEquals(7, z.length, "pattern(2) + separator(1) + text(4)");
        assertEquals("Z", r.getAlgorithm());
        assertArrayEquals(new int[]{2}, r.getMatchPositions());
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
}