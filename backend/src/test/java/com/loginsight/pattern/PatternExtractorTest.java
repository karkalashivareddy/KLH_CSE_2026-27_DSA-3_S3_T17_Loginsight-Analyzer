package com.loginsight.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loginsight.dto.response.PatternDto;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Heuristic pattern extraction: variable tokens (ids, IPs, numeric values) normalise to
 * {@code <*>} and templates aggregate by frequency with the first example preserved.
 */
class PatternExtractorTest {

    @Test
    void normalizesVariablesAndNumbers() {
        assertEquals("User <*> failed login from <*>",
                PatternExtractor.normalizeMessage("User 1001 failed login from 10.0.0.5"));
        assertEquals("slow response <*> exceeded threshold",
                PatternExtractor.normalizeMessage("slow response: 842ms exceeded threshold"));
        assertEquals("query executed",
                PatternExtractor.normalizeMessage("query executed"));
    }

    @Test
    void extractsAndAggregatesTemplates() {
        List<LogEvent> events = new ArrayList<>();
        events.add(msg("authentication failed for user-1001", "auth-service", LogLevel.ERROR));
        events.add(msg("authentication failed for user-2001", "auth-service", LogLevel.ERROR));
        events.add(msg("authentication failed for user-3001", "auth-service", LogLevel.ERROR));
        events.add(msg("login successful for user-1001", "auth-service", LogLevel.INFO));
        events.add(msg("payment processed", "payment-service", LogLevel.INFO));

        List<PatternDto> patterns = new PatternExtractor().extract(events, 10);
        assertTrue(patterns.stream().anyMatch(p -> p.count() == 3
                && "authentication failed for <*>".equals(p.template())));
        assertTrue(patterns.stream().anyMatch(p -> p.count() == 1
                && "payment processed".equals(p.template())));
        PatternDto auth = patterns.stream()
                .filter(p -> p.template().startsWith("authentication")).findFirst().orElseThrow();
        assertEquals("authentication failed for user-1001", auth.example());
        assertEquals("ERROR", auth.level());
    }

    @Test
    void topTemplatePicksMostFrequentShape() {
        List<String> messages = List.of(
                "connection refused from 10.0.0.1",
                "connection refused from 10.0.0.2",
                "connection refused from 10.0.0.3",
                "query timeout on orders");
        String top = new PatternExtractor().topTemplate(messages);
        assertEquals("connection refused from <*>", top);
    }

    private static LogEvent msg(String message, String service, LogLevel level) {
        return LogEvent.builder()
                .id(0)
                .timestamp(Instant.parse("2026-09-13T00:00:00Z"))
                .level(level)
                .service(service)
                .message(message)
                .build();
    }
}