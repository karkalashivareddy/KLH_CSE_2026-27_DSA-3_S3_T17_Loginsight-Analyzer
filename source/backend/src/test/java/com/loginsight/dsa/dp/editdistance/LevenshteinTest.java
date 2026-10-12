package com.loginsight.dsa.dp.editdistance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loginsight.dsa.dp.DpResult;
import org.junit.jupiter.api.Test;

class LevenshteinTest {

    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    @Test
    void knownDistances() {
        assertEquals(3, levenshtein.distance("kitten", "sitting"));
        assertEquals(2, levenshtein.distance("flaw", "lawn"));
        assertEquals(1, levenshtein.distance("abc", "abd"));
        assertEquals(3, levenshtein.distance("abc", "xyz"));
    }

    @Test
    void emptyStringCases() {
        assertEquals(0, levenshtein.distance("", ""));
        assertEquals(3, levenshtein.distance("abc", ""));
        assertEquals(3, levenshtein.distance("", "abc"));
        assertEquals(0, levenshtein.distance("same", "same"));
    }

    @Test
    void symmetricMetric() {
        assertEquals(levenshtein.distance("AUTH-USER-DB", "AUTH-USER-BD"),
                levenshtein.distance("AUTH-USER-BD", "AUTH-USER-DB"));
        assertEquals(2, levenshtein.distance("AUTH-USER-DB", "AUTH-USER-BD"));
    }

    @Test
    void twoRowMatchesFullMatrix() {
        String[][] pairs = {
                {"kitten", "sitting"},
                {"", "abc"},
                {"abcdef", "fedcba"},
                {"service-A", "service-B"},
                {"aaaaaaaa", "aaaa"}
        };
        for (String[] pair : pairs) {
            assertEquals(levenshtein.distance(pair[0], pair[1]),
                    levenshtein.distanceTwoRow(pair[0], pair[1]),
                    "two-row distance must match full matrix for " + pair[0] + "/" + pair[1]);
        }
    }

    @Test
    void matrixHasCorrectShapeAndBaseCases() {
        long[][] matrix = levenshtein.buildMatrix("cat", "cart");
        assertEquals(4, matrix.length);
        assertEquals(5, matrix[0].length);
        for (int i = 0; i <= 3; i++) {
            assertEquals(i, matrix[i][0]);
        }
        for (int j = 0; j <= 4; j++) {
            assertEquals(j, matrix[0][j]);
        }
        assertEquals(1, matrix[3][4]);
    }

    @Test
    void reconstructionReproducesDistanceAndTransform() {
        String[][] pairs = {
                {"kitten", "sitting"},
                {"", "abc"},
                {"abc", ""},
                {"flaw", "lawn"},
                {"loginsight", "logininsight"},
                {"", ""}
        };
        for (String[] pair : pairs) {
            EditDistanceResult result = levenshtein.reconstruct(pair[0], pair[1]);
            assertEquals(levenshtein.distance(pair[0], pair[1]), result.getDistance());
            EditScriptVerifier.assertConsistent(pair[0], pair[1], result, 1, 1, 1, 1);
        }
    }

    @Test
    void exactMatchProducesAllMatches() {
        EditDistanceResult result = levenshtein.reconstruct("abc", "abc");
        assertEquals(0, result.getDistance());
        assertArrayEquals(new String[]{"MATCH", "MATCH", "MATCH"}, result.getOperations());
    }

    @Test
    void unicodeCodeUnits() {
        assertEquals(1, levenshtein.distance("अरे", "अरे!"));
        assertEquals(0, levenshtein.distance("याव", "याव"));
    }

    @Test
    void envelopeView() {
        DpResult result = levenshtein.match("kitten", "sitting");
        assertEquals("Levenshtein", result.getAlgorithm());
        assertEquals(6 * 7, result.getInputSize());
        assertEquals(3, result.resultAsLong());
        assertEquals("O(n * m)", result.getTimeComplexity());
        assertTrue(result.getIntermediateData() instanceof long[][]);
    }

    @Test
    void rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> levenshtein.distance(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> levenshtein.distance("a", null));
        assertThrows(IllegalArgumentException.class, () -> levenshtein.reconstruct(null, "a"));
    }

    @Test
    void editDistanceResultAccessors() {
        EditDistanceResult result = levenshtein.reconstruct("ab", "cd");
        assertEquals("Levenshtein", result.getAlgorithm());
        assertEquals(result.getOperations().length, result.getOperationCount());
        assertArrayEquals(new long[]{0, 1, 2}, result.getMatrix()[0]);
        assertTrue(result.toString().contains("distance="));
    }

    @Test
    void dpResultAccessors() {
        DpResult result = levenshtein.match("a", "b");
        assertEquals("O(n * m)", result.getSpaceComplexity());
        assertTrue(result.getIntermediateData() instanceof long[][]);
        assertTrue(result.toString().contains("Levenshtein"));
    }

    @Test
    void weightedUnitCostsEqualLevenshtein() {
        WeightedEditDistance weighted = new WeightedEditDistance(1, 1, 1);
        assertEquals(levenshtein.distance("kitten", "sitting"), weighted.distance("kitten", "sitting"));
        assertEquals(levenshtein.distance("flaw", "lawn"), weighted.distance("flaw", "lawn"));
    }

    @Test
    void weightedPrefersCheapDeleteInsertOverExpensiveSubstitute() {
        WeightedEditDistance weighted = new WeightedEditDistance(1, 1, 3);
        assertEquals(6, weighted.distance("abc", "xyz"));
    }

    @Test
    void weightedFreeInsert() {
        WeightedEditDistance weighted = new WeightedEditDistance(0, 1, 1);
        assertEquals(0, weighted.distance("abc", "abcdef"));
        assertEquals(0, weighted.distance("abc", "xxxabc"));
    }

    @Test
    void weightedReconstructionConsistent() {
        WeightedEditDistance weighted = new WeightedEditDistance(2, 1, 5);
        EditDistanceResult result = weighted.reconstruct("kitten", "sitting");
        assertEquals(weighted.distance("kitten", "sitting"), result.getDistance());
        EditScriptVerifier.assertConsistent("kitten", "sitting", result, 2, 1, 5, 1);
    }

    @Test
    void weightedExposesCostsAndRejectsNegative() {
        WeightedEditDistance weighted = new WeightedEditDistance(2, 3, 4);
        assertEquals(2, weighted.getInsertCost());
        assertEquals(3, weighted.getDeleteCost());
        assertEquals(4, weighted.getSubstituteCost());
        assertThrows(IllegalArgumentException.class, () -> new WeightedEditDistance(-1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new WeightedEditDistance(1, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new WeightedEditDistance(1, 1, -1));
    }

    @Test
    void weightedRejectsNullInput() {
        WeightedEditDistance weighted = new WeightedEditDistance(1, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> weighted.distance(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> weighted.distance("a", null));
        assertThrows(IllegalArgumentException.class, () -> weighted.reconstruct("a", null));
        assertThrows(IllegalArgumentException.class, () -> weighted.match(null, "a"));
    }

    @Test
    void weightedEmptyAndEnvelope() {
        WeightedEditDistance weighted = new WeightedEditDistance(2, 3, 4);
        assertEquals(6, weighted.distance("", "abc"));
        assertEquals(9, weighted.distance("abc", ""));
        DpResult result = weighted.match("ab", "abc");
        assertEquals("Weighted edit distance", result.getAlgorithm());
        assertEquals(2, result.resultAsLong());
    }
}
