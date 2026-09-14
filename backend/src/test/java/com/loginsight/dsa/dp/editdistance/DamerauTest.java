package com.loginsight.dsa.dp.editdistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loginsight.dsa.dp.DpResult;
import org.junit.jupiter.api.Test;

class DamerauTest {

    private final DamerauLevenshteinDistance damerau = new DamerauLevenshteinDistance();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    @Test
    void transpositionCostsOne() {
        assertEquals(1, damerau.distance("CA", "AC"));
        assertEquals(2, levenshtein.distance("CA", "AC"));
    }

    @Test
    void commonTypo() {
        assertEquals(1, damerau.distance("erorr", "error"));
    }

    @Test
    void osaVersusUnrestrictedClassicExample() {
        // "CA" -> "ABC": unrestricted Damerau-Levenshtein is 2, OSA is 3.
        assertEquals(3, damerau.distance("CA", "ABC"));
    }

    @Test
    void fallsBackToLevenshteinWhenNoTranspositionHelps() {
        assertEquals(levenshtein.distance("kitten", "sitting"), damerau.distance("kitten", "sitting"));
        assertEquals(levenshtein.distance("abc", "xyz"), damerau.distance("abc", "xyz"));
    }

    @Test
    void emptyAndEqualCases() {
        assertEquals(0, damerau.distance("", ""));
        assertEquals(3, damerau.distance("abc", ""));
        assertEquals(3, damerau.distance("", "abc"));
        assertEquals(0, damerau.distance("same", "same"));
    }

    @Test
    void reconstructionUsesTransposeAndIsConsistent() {
        EditDistanceResult result = damerau.reconstruct("erorr", "error");
        assertEquals(1, result.getDistance());
        assertTrue(containsOperation(result, "TRANSPOSE"), "reconstruction must use a transposition");
        EditScriptVerifier.assertConsistent("erorr", "error", result, 1, 1, 1, 1);
    }

    private static boolean containsOperation(EditDistanceResult result, String operation) {
        for (String op : result.getOperations()) {
            if (operation.equals(op)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void reconstructionConsistentAcrossCases() {
        String[][] pairs = {
                {"CA", "AC"},
                {"CA", "ABC"},
                {"kitten", "sitting"},
                {"", "abc"},
                {"abc", ""},
                {"abcdef", "badcfe"}
        };
        for (String[] pair : pairs) {
            EditDistanceResult result = damerau.reconstruct(pair[0], pair[1]);
            assertEquals(damerau.distance(pair[0], pair[1]), result.getDistance());
            EditScriptVerifier.assertConsistent(pair[0], pair[1], result, 1, 1, 1, 1);
        }
    }

    @Test
    void matrixBaseCases() {
        long[][] matrix = damerau.buildMatrix("ab", "ba");
        assertEquals(1, matrix[2][2]);
        assertEquals(0, matrix[0][0]);
    }

    @Test
    void variantIsDocumented() {
        assertNotNull(DamerauLevenshteinDistance.variant());
        assertTrue(DamerauLevenshteinDistance.variant().contains("Optimal String Alignment"));
    }

    @Test
    void envelopeView() {
        DpResult result = damerau.match("erorr", "error");
        assertEquals("Damerau-Levenshtein (OSA)", result.getAlgorithm());
        assertEquals(1, result.resultAsLong());
    }

    @Test
    void rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> damerau.distance(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> damerau.distance("a", null));
        assertThrows(IllegalArgumentException.class, () -> damerau.reconstruct("a", null));
    }
}
