package com.loginsight.dsa.dp.alignment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SmithWatermanTest {

    private final SmithWaterman sw = new SmithWaterman();

    @Test
    void findsContainedMotif() {
        AlignmentResult result = sw.align("HELLOWORLD", "WORLD");
        assertEquals(10, result.getScore());
        assertEquals(5, result.getStartA());
        assertEquals(10, result.getEndA());
        assertEquals(0, result.getStartB());
        assertEquals(5, result.getEndB());
        assertArrayEquals(new String[]{"W", "O", "R", "L", "D"}, result.getAlignedA());
        AlignmentVerifier.assertConsistent(result, 2, -1, -2);
    }

    @Test
    void ignoresUnrelatedEnds() {
        AlignmentResult result = sw.align("XXXXABCXXXX", "YYYYABCYYYY");
        assertEquals(6, result.getScore());
        assertEquals(4, result.getStartA());
        assertEquals(7, result.getEndA());
        assertEquals(4, result.getStartB());
        assertEquals(7, result.getEndB());
        AlignmentVerifier.assertConsistent(result, 2, -1, -2);
    }

    @Test
    void explicitScores() {
        AlignmentResult result = sw.align("ACGTACGT", "CGTAC", 2, -1, -2);
        assertEquals(10, result.getScore());
        assertEquals(1, result.getStartA());
        assertEquals(6, result.getEndA());
        AlignmentVerifier.assertConsistent(result, 2, -1, -2);
    }

    @Test
    void noLocalMatchYieldsZeroAndEmptyAlignment() {
        AlignmentResult result = sw.align("AAA", "BBB");
        assertEquals(0, result.getScore());
        assertEquals(0, result.getAlignmentLength());
        assertEquals(0, result.getStartA());
        assertEquals(0, result.getEndA());
    }

    @Test
    void emptySequences() {
        assertEquals(0, sw.align("", "").getScore());
        assertEquals(0, sw.align("abc", "").getScore());
    }

    @Test
    void emptyTokenSequences() {
        AlignmentResult result = sw.align(new String[0], new String[0], 2, -1, -2);
        assertEquals(0, result.getScore());
        assertEquals(0, result.getAlignmentLength());
    }

    @Test
    void accessorsAndMatrix() {
        AlignmentResult result = sw.align("WORLD", "WORLD");
        assertTrue(result.getMatrix().length == 6);
        assertTrue(result.toString().contains("score="));
    }

    @Test
    void identicalSequences() {
        AlignmentResult result = sw.align("PAYMENT", "PAYMENT");
        assertEquals(14, result.getScore());
        assertEquals(0, result.getGapCount());
        AlignmentVerifier.assertConsistent(result, 2, -1, -2);
    }

    @Test
    void tokenSequenceInput() {
        String[] a = {"AUTH", "USER", "DB"};
        String[] b = {"X", "USER", "DB", "Y"};
        AlignmentResult result = sw.align(a, b, 3, -2, -2);
        assertEquals(6, result.getScore());
        AlignmentVerifier.assertConsistent(result, 3, -2, -2);
    }

    @Test
    void rejectsInvalidScores() {
        assertThrows(IllegalArgumentException.class, () -> sw.align("a", "b", -1, -1, -1));
        assertThrows(IllegalArgumentException.class, () -> sw.align("a", "b", 1, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> sw.align("a", "b", 1, -1, 1));
    }

    @Test
    void rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> sw.align((String) null, "b"));
        assertThrows(IllegalArgumentException.class, () -> sw.align("a", (String) null));
        assertThrows(IllegalArgumentException.class,
                () -> sw.align((String[]) null, new String[]{"b"}, 1, -1, -1));
    }

    @Test
    void localBeatsGlobalWhenEndsAreUnrelated() {
        AlignmentResult local = sw.align("XXXXABCXXXX", "YYYYABCYYYY");
        NeedlemanWunsch nw = new NeedlemanWunsch();
        long global = nw.align("XXXXABCXXXX", "YYYYABCYYYY", 2, -1, -2).getScore();
        assertTrue(local.getScore() > global,
                "local (" + local.getScore() + ") should beat global (" + global + ") here");
    }
}
