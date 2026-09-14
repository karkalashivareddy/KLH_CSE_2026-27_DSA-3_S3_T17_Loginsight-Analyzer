package com.loginsight.dsa.dp.editdistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test-only verifier: replays an edit script from a source string onto a target string and checks that
 * it consumes exactly both strings and that the summed operation cost equals the reported distance.
 * This is the key independent invariant for reconstruction (docs/13): the score is recomputed from the
 * returned script rather than trusting the DP value a second time.
 */
final class EditScriptVerifier {

    private EditScriptVerifier() {
    }

    static void assertConsistent(String a, String b, EditDistanceResult result, long insertCost,
                                 long deleteCost, long substituteCost, long transposeCost) {
        String[] ops = result.getOperations();
        int i = 0;
        int j = 0;
        long cost = 0;
        for (String op : ops) {
            switch (op) {
                case EditDistanceResult.MATCH:
                    assertEquals(a.charAt(i), b.charAt(j), "MATCH must pair equal characters");
                    i++;
                    j++;
                    break;
                case EditDistanceResult.SUBSTITUTE:
                    i++;
                    j++;
                    cost += substituteCost;
                    break;
                case EditDistanceResult.DELETE:
                    i++;
                    cost += deleteCost;
                    break;
                case EditDistanceResult.INSERT:
                    j++;
                    cost += insertCost;
                    break;
                case EditDistanceResult.TRANSPOSE:
                    assertEquals(a.charAt(i), b.charAt(j + 1), "transpose pairs swapped chars (a)");
                    assertEquals(a.charAt(i + 1), b.charAt(j), "transpose pairs swapped chars (b)");
                    i += 2;
                    j += 2;
                    cost += transposeCost;
                    break;
                default:
                    fail("unknown edit operation: " + op);
            }
        }
        assertEquals(a.length(), i, "script must consume all of source");
        assertEquals(b.length(), j, "script must consume all of target");
        assertEquals(result.getDistance(), cost, "script cost must equal reported distance");
    }
}
