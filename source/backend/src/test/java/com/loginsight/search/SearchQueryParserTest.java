package com.loginsight.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.loginsight.exception.InvalidQueryException;

class SearchQueryParserTest {

    @Test
    void parsesStructuredFields() {
        SearchQuery query = new SearchQueryParser().parse(
                "level:ERROR service:auth-service host:auth-1 status:401"
                        + " trace:abc source:demo-stream request:req-42",
                null, null, "timestamp:asc");
        assertEquals("", query.freeText());
        assertEquals(java.util.List.of("ERROR"), query.levels());
        assertEquals("auth-service", query.service());
        assertEquals("auth-1", query.host());
        assertEquals("401", query.status());
        assertEquals("abc", query.traceId());
        assertEquals("demo-stream", query.source());
        assertEquals("req-42", query.requestId());
        assertEquals("timestamp:asc", query.sort());
        assertTrue(query.hasFilters());
    }

    @Test
    void messageFieldAndBareWordsBecomeFreeText() {
        SearchQuery query = new SearchQueryParser().parse(
                "message:\"connection refused\" gateway", null, null, null);
        assertEquals("connection refused gateway", query.freeText());
        assertNull(query.service());
    }

    @Test
    void unknownFieldDegradesToFreeText() {
        SearchQuery query = new SearchQueryParser().parse("bogus:value timeout", null, null, null);
        assertEquals("bogus:value timeout", query.freeText());
    }

    @Test
    void fromAndToArePreserved() {
        Instant from = Instant.parse("2026-09-13T00:00:00Z");
        Instant to = Instant.parse("2026-09-13T01:00:00Z");
        SearchQuery query = new SearchQueryParser().parse("level:WARN", from, to, null);
        assertEquals(from, query.from());
        assertEquals(to, query.to());
    }

    @Test
    void emptyValueThrows() {
        assertThrows(InvalidQueryException.class,
                () -> new SearchQueryParser().parse("level:", null, null, null));
    }

    @Test
    void blankQueryProducesEmptyState() {
        SearchQuery query = new SearchQueryParser().parse("   ", null, null, null);
        assertEquals("", query.freeText());
        assertTrue(query.levels().isEmpty());
    }
}