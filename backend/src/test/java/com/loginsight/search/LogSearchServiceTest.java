package com.loginsight.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.loginsight.dto.request.LogSearchRequest;
import com.loginsight.dto.response.LogSearchResponse;
import com.loginsight.dto.response.SuggestionDto;
import com.loginsight.index.LogIndexService;
import com.loginsight.service.DatasetService;

/**
 * Product search end-to-end over the deterministic demo dataset: structured filters, KMP free text,
 * paging, "did you mean" on a nonsense phrase, and the typeahead suggestions.
 */
class LogSearchServiceTest {

    private final DatasetService datasetService = datasetService();
    private final LogSearchService searchService =
            new LogSearchService(datasetService, new LogIndexService(datasetService));

    private static DatasetService datasetService() {
        DatasetService service = new DatasetService(".");
        service.loadDemo();
        return service;
    }

    @Test
    void structuredFiltersNarrowResults() {
        LogSearchResponse response = searchService.search(
                new LogSearchRequest("level:ERROR service:database status:503", null, null, 1, 10,
                        null));
        assertTrue(response.total() > 0);
        assertTrue(response.matches().size() <= 10);
        for (LogSearchResponse.SearchHit hit : response.matches()) {
            assertEquals("ERROR", hit.event().level());
            assertEquals("database", hit.event().service());
            assertEquals("503", String.valueOf(hit.event().statusCode()));
        }
        assertEquals("", response.pattern());
        assertEquals("Structured filters only", response.strategy());
    }

    @Test
    void freeTextRunsKmpAndFillsAlgorithmMetadata() {
        LogSearchResponse response = searchService.search(
                new LogSearchRequest("connection refused", null, null, 1, 25, null));
        assertTrue(response.total() > 0);
        assertEquals("KMP", response.algorithm());
        assertEquals("connection refused", response.pattern());
        assertTrue(response.textSize() > 0);
        assertTrue(response.durationNanos() >= 0);
        assertEquals("Demo Dataset", response.dataset());
    }

    @Test
    void nonsensePhraseYieldsLevenshteinSuggestion() {
        LogSearchResponse response = searchService.search(
                new LogSearchRequest("paymentd proccessd", null, null, 1, 25, null));
        assertEquals(0, response.total());
        assertNotNull(response.suggestion());
        assertTrue(response.suggestion().suggestion().toLowerCase().contains("payment"));
        assertEquals("Levenshtein", response.suggestion().algorithm());
    }

    @Test
    void pagingIsBounded() {
        LogSearchResponse response = searchService.search(
                new LogSearchRequest("service:api-gateway", null, null, 2, 5, null));
        assertTrue(response.matches().size() <= 5);
        assertEquals(2, response.page());
        assertEquals(5, response.size());
        assertTrue(response.total() >= 5);
    }

    @Test
    void suggestReturnsCommittableFilters() {
        List<SuggestionDto> suggestions = searchService.suggest("auth", 12);
        assertTrue(suggestions.stream().anyMatch(s -> "service".equals(s.type())
                && "service:auth-service".equals(s.value())));
        List<SuggestionDto> levelSuggestions = searchService.suggest("error", 12);
        assertTrue(levelSuggestions.stream().anyMatch(s -> "level".equals(s.type())));
    }
}