package com.loginsight.dsa.dp.alignment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NeedlemanWunschTest {

    private final NeedlemanWunsch nw = new NeedlemanWunsch();

    @Test
    void classicWorkedExample() {
        AlignmentResult result = nw.align("GATTACA", "GCATGCU");
        assertEquals(0, result.getScore());
        assertEquals(0, result.getStartA());
        assertEquals(0, result.getStartB());
        assertEquals(7, result.getEndA());
        assertEquals(7, result.getEndB());
        AlignmentVerifier.assertConsistent(result, 1, -1, -1);
    }

    @Test
    void identicalSequencesAllMatches() {
        AlignmentResult result = nw.align("LOGIN", "LOGIN");
        assertEquals(5, result.getScore());
        assertEquals(0, result.getGapCount());
        assertArrayEquals(new String[]{"L", "O", "G", "I", "N"}, result.getAlignedA());
    }

    @Test
    void emptyAgainstNonEmptyUsesGaps() {
        AlignmentResult result = nw.align("ABC", "");
        assertEquals(-3, result.getScore());
        assertEquals(3, result.getGapCount());
        assertEquals(3, result.getAlignmentLength());
        AlignmentVerifier.assertConsistent(result, 1, -1, -1);
    }

    @Test
    void bothEmpty() {
        AlignmentResult result = nw.align("", "");
        assertEquals(0, result.getScore());
        assertEquals(0, result.getAlignmentLength());
    }

    @Test
    void tokenSequenceInput() {
        String[] a = {"AUTH", "USER", "DB"};
        String[] b = {"AUTH", "DB"};
        AlignmentResult result = nw.align(a, b, 2, -1, -2);
        assertEquals(2, result.getScore());
        AlignmentVerifier.assertConsistent(result, 2, -1, -2);
    }

    @Test
    void explicitScoresRecomputation() {
        AlignmentResult result = nw.align("ACGTACGT", "ACGTACGT", 3, -2, -3);
        assertEquals(24, result.getScore());
        AlignmentVerifier.assertConsistent(result, 3, -2, -3);
    }

    @Test
    void deterministicTieBreaking() {
        AlignmentResult first = nw.align("AA", "AA");
        AlignmentResult second = nw.align("AA", "AA");
        assertArrayEquals(first.getAlignedA(), second.getAlignedA());
        assertArrayEquals(first.getAlignedB(), second.getAlignedB());
    }

    @Test
    void rejectsInvalidScores() {
        assertThrows(IllegalArgumentException.class, () -> nw.align("a", "b", -1, -1, -1));
        assertThrows(IllegalArgumentException.class, () -> nw.align("a", "b", 1, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> nw.align("a", "b", 1, -1, 1));
    }

    @Test
    void rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> nw.align((String) null, "b"));
        assertThrows(IllegalArgumentException.class, () -> nw.align("a", (String) null));
        assertThrows(IllegalArgumentException.class,
                () -> nw.align((String[]) null, new String[]{"b"}, 1, -1, -1));
    }

    @Test
    void alignmentResultAccessors() {
        AlignmentResult result = nw.align("AB", "A");
        assertEquals("Needleman-Wunsch", result.getAlgorithm());
        assertEquals(1, result.getGapCount());
        assertEquals(0, result.getStartB());
        assertEquals(1, result.getEndB());
        assertTrue(result.getMatrix().length == 3);
        assertTrue(result.toString().contains("score="));
    }
}
