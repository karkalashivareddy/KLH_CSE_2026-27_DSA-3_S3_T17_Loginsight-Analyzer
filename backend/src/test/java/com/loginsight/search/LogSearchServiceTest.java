package com.loginsight.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loginsight.dto.request.LogSearchRequest;
import com.loginsight.dto.response.LogSearchResponse;
import com.loginsight.dto.response.SuggestionDto;
import com.loginsight.exception.InvalidQueryException;
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

    @Test
    void repeatedLevelsWidenTheSearch() {
        LogSearchResponse both = searchService.search(
                new LogSearchRequest("level:ERROR level:WARN", null, null, 1, 200, null));
        LogSearchResponse errorsOnly = searchService.search(
                new LogSearchRequest("level:ERROR", null, null, 1, 200, null));
        assertTrue(both.total() > errorsOnly.total(),
                "OR semantics: adding level:WARN must not narrow the result");
        for (LogSearchResponse.SearchHit hit : both.matches()) {
            assertTrue("ERROR".equals(hit.event().level()) || "WARN".equals(hit.event().level()));
        }
    }

    @Test
    void requestIdFilterNarrowsResults() {
        String requestId = datasetService.currentDataset().orElseThrow().events().stream()
                .map(com.loginsight.model.LogEvent::getRequestId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElseThrow();
        LogSearchResponse response = searchService.search(
                new LogSearchRequest("request:" + requestId, null, null, 1, 25, null));
        assertTrue(response.total() > 0, "the indexed request id must match at least one event");
        for (LogSearchResponse.SearchHit hit : response.matches()) {
            assertEquals(requestId, hit.event().requestId());
        }
    }

    @Test
    void aFilterValueThatMatchesNothingYieldsNoResults() {
        LogSearchResponse response = searchService.search(
                new LogSearchRequest("request:definitely-not-present", null, null, 1, 25, null));
        assertEquals(0, response.total());
        assertTrue(response.matches().isEmpty());
        assertEquals(0, searchService.search(
                new LogSearchRequest("service:no-such-service", null, null, 1, 25, null)).total());
    }

    @Test
    void hugePageDoesNotOverflowBackToTheFirstPage() {
        LogSearchResponse response = searchService.search(
                new LogSearchRequest("service:api-gateway", null, null, Integer.MAX_VALUE, 25, null));
        assertTrue(response.matches().isEmpty(),
                "page * size must be computed in 64-bit arithmetic");
        assertEquals(Integer.MAX_VALUE, response.page());
    }

    @Test
    void oversizedPageYieldsNoHitsButKeepsTheTotal() {
        LogSearchResponse response = searchService.search(
                new LogSearchRequest("connection refused", null, null, 100_000, 25, null));
        assertTrue(response.total() > 0);
        assertTrue(response.matches().isEmpty());
    }

    @Test
    void invertedDateRangeIsRejected() {
        assertThrows(InvalidQueryException.class, () -> searchService.search(
                new LogSearchRequest("error", Instant.parse("2026-09-13T12:00:00Z"),
                        Instant.parse("2026-09-13T10:00:00Z"), 1, 25, null)));
    }

    @Test
    void suggestLimitIsClamped() {
        assertTrue(searchService.suggest("a", 0).size() >= 1);
        assertTrue(searchService.suggest("a", 100_000).size() <= LogSearchService.MAX_SUGGESTIONS);
    }
}