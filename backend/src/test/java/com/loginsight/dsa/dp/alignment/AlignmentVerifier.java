package com.loginsight.dsa.dp.alignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test-only verifier: independently recomputes an alignment score from the returned aligned token
 * arrays and checks structural invariants (equal lengths, no double-gap columns). This is the core
 * cross-check for both Needleman-Wunsch and Smith-Waterman (docs/13).
 */
final class AlignmentVerifier {

    private AlignmentVerifier() {
    }

    static void assertConsistent(AlignmentResult result, int match, int mismatch, int gap) {
        String[] a = result.getAlignedA();
        String[] b = result.getAlignedB();
        assertEquals(a.length, b.length, "aligned arrays must have equal length");
        long score = 0;
        for (int i = 0; i < a.length; i++) {
            boolean gapA = AlignmentResult.GAP.equals(a[i]);
            boolean gapB = AlignmentResult.GAP.equals(b[i]);
            assertFalse(gapA && gapB, "a column must not be two gaps");
            if (gapA || gapB) {
                score += gap;
            } else {
                score += a[i].equals(b[i]) ? match : mismatch;
            }
        }
        assertEquals(score, result.getScore(), "recomputed score must equal reported score");
    }
}
