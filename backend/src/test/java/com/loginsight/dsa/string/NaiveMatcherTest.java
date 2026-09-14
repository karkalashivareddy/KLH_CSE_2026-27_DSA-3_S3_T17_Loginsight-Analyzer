package com.loginsight.dsa.string;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NaiveMatcherTest {

    private final NaiveMatcher matcher = new NaiveMatcher();

    @Test
    void singleMatchAtStart() {
        StringSearchResult r = matcher.match("loginsight analyzer project", "loginsight");
        assertArrayEquals(new int[]{0}, r.getMatchPositions());
        assertEquals(1, r.getMatchCount());
    }

@Test
    void matchesIncludingOverlaps() {
        assertArrayEquals(new int[]{0, 1}, matcher.match("aaa", "aa").getMatchPositions());
        assertArrayEquals(new int[]{0, 3, 6}, matcher.match("abcabcabc", "abc").getMatchPositions());
        assertArrayEquals(new int[]{0, 2, 4}, matcher.match("ababab", "ab").getMatchPositions());
    }

    @Test
    void noMatchYieldsEmptyResult() {
        StringSearchResult r = matcher.match("log entry", "xyz-not-there");
        assertEquals(0, r.getMatchCount());
        assertArrayEquals(new int[0], r.getMatchPositions());
    }

    @Test
    void patternEqualToTextMatchesExactlyOnce() {
        assertArrayEquals(new int[]{0}, matcher.match("error", "error").getMatchPositions());
    }

    @Test
    void patternLongerThanTextYieldsEmptyResult() {
        StringSearchResult r = matcher.match("tiny", "longpattern");
        assertEquals(0, r.getMatchCount());
    }

    @Test
    void isCaseSensitive() {
        assertEquals(0, matcher.match("Auth FAILED", "failed").getMatchCount());
        assertArrayEquals(new int[]{5}, matcher.match("Auth failed", "failed").getMatchPositions());
    }

    @Test
    void logStyleLine() {
        String line = "2026-09-14T10:00:00.000Z ERROR database connection failed for order 42";
        assertArrayEquals(new int[]{31}, matcher.match(line, "database connection failed").getMatchPositions());
        assertArrayEquals(new int[]{line.indexOf("connection")},
                matcher.match(line, "connection").getMatchPositions());
        assertArrayEquals(new int[]{0}, matcher.match(line, "2026").getMatchPositions());
    }

    @Test
    void unicodeDevanagariText() {
        String text = "राम रामाय नमः रामः";
        assertArrayEquals(new int[]{0, 4, 14}, matcher.match(text, "राम").getMatchPositions());
        assertArrayEquals(new int[]{10}, matcher.match(text, "नमः").getMatchPositions());
    }

    @Test
    void allRepeatedCharacters() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append('a');
        }
        int[] positions = matcher.match(sb.toString(), "aa").getMatchPositions();
        assertEquals(999, positions.length);
        assertEquals(998, positions[998]);
    }

    @Test
    void resultMetadataIsFilled() {
        StringSearchResult r = matcher.match("hello world", "world");
        assertEquals("Naive", r.getAlgorithm());
        assertEquals(11, r.getTextLength());
        assertEquals("world", r.getPattern());
        assertEquals("O(n * m)", r.getTimeComplexity());
        assertEquals("O(1) auxiliary", r.getSpaceComplexity());
        assertTrue(r.getExecutionTimeNanos() >= 0);
        assertNull(r.getIntermediateData());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> matcher.match(null, "ab"));
        assertThrows(IllegalArgumentException.class, () -> matcher.match("ab", (String) null));
        assertThrows(IllegalArgumentException.class, () -> matcher.match("ab", ""));
    }
}