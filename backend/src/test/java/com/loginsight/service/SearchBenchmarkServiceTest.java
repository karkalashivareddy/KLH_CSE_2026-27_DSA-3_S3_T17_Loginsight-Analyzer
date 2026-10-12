package com.loginsight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.dsa.string.KMPMatcher;
import com.loginsight.dsa.string.NaiveMatcher;
import com.loginsight.dsa.string.RabinKarpMatcher;
import com.loginsight.dsa.string.ZAlgorithm;

/**
 * The measured search benchmark is honest: it runs four matchers over one shared haystack and
 * reports per-matcher measured times plus the fastest winner.
 */
class SearchBenchmarkServiceTest {

    @Test
    void runsAllFourMatchersOnTheDemoDataset() {
        DatasetService datasetService = new DatasetService(".");
        datasetService.loadDemo();
        Map<String, Object> body = new SearchBenchmarkService(datasetService).run("connection");
        assertEquals("PATTERN_SEARCH", body.get("problem"));
        assertTrue((Integer) body.get("textLength") > 0);
        List<?> results = (List<?>) body.get("results");
        assertEquals(4, results.size());
        assertTrue(body.get("winner") instanceof String);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) results;
        Map<String, String> expectedComplexity = Map.of(
                "Naive", "O(n * m)", "KMP", "O(n + m)",
                "Z-Algorithm", "O(n + m)", "Rabin-Karp", "O(n + m) avg, O(n*m) worst");
        for (Map<String, Object> row : rows) {
            assertEquals(expectedComplexity.get(row.get("algorithm")),
                    row.get("timeComplexity"));
            assertTrue((long) row.get("timeNanos") >= 0);
            assertTrue((int) row.get("matchCount") >= 0);
        }
        assertNotNull(body.get("note"));
    }

    @Test
    void matchersAgreeOnMatchCountsForSameInput() {
        String text = "error at 03:14 gateway timeout error at 03:15";
        String pattern = "error";
        int naive = new NaiveMatcher().match(text, pattern).getMatchCount();
        int kmp = new KMPMatcher().match(text, pattern).getMatchCount();
        int zed = new ZAlgorithm().match(text, pattern).getMatchCount();
        int rabin = new RabinKarpMatcher().match(text, pattern).getMatchCount();
        assertEquals(naive, kmp);
        assertEquals(kmp, zed);
        assertEquals(zed, rabin);
        assertEquals(2, naive);
    }
}