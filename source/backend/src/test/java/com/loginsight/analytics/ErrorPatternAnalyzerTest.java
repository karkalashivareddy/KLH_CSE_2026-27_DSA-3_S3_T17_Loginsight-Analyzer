package com.loginsight.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

class ErrorPatternAnalyzerTest {

    private static LogEvent event(LogLevel level, String message, String service) {
        return LogEvent.builder()
                .timestamp(Instant.parse("2026-09-13T10:00:00Z"))
                .level(level)
                .service(service)
                .host("localhost")
                .ipAddress("127.0.0.1")
                .httpMethod(HttpMethod.GET)
                .endpoint("/test")
                .statusCode(200)
                .responseTime(10)
                .requestId("req-1")
                .userId("user-1")
                .message(message)
                .build();
    }

    @Test
    void topErrorReturnsMostFrequentErrorMessage() {
        ErrorPatternAnalyzer analyzer = new ErrorPatternAnalyzer();
        var events = List.of(
                event(LogLevel.ERROR, "connection timeout", "AUTH"),
                event(LogLevel.ERROR, "connection timeout", "AUTH"),
                event(LogLevel.ERROR, "authentication failed", "AUTH"),
                event(LogLevel.WARN, "retry scheduled", "USER"));

        String top = analyzer.topError(events);
        assertEquals("connection timeout", top);
    }

    @Test
    void topErrorReturnsNullWhenNoErrors() {
        ErrorPatternAnalyzer analyzer = new ErrorPatternAnalyzer();
        var events = List.of(
                event(LogLevel.INFO, "request completed", "AUTH"),
                event(LogLevel.WARN, "retry scheduled", "USER"));

        String top = analyzer.topError(events);
        assertNull(top);
    }

    @Test
    void topErrorReturnsNullWhenOnlyWarnings() {
        ErrorPatternAnalyzer analyzer = new ErrorPatternAnalyzer();
        var events = List.of(
                event(LogLevel.WARN, "retry scheduled", "USER"),
                event(LogLevel.INFO, "request completed", "DB"));

        String top = analyzer.topError(events);
        assertNull(top);
    }

    @Test
    void topErrorsReturnsTopErrorAndWarningMessages() {
        ErrorPatternAnalyzer analyzer = new ErrorPatternAnalyzer();
        var events = List.of(
                event(LogLevel.ERROR, "connection timeout", "AUTH"),
                event(LogLevel.ERROR, "connection timeout", "AUTH"),
                event(LogLevel.ERROR, "authentication failed", "AUTH"),
                event(LogLevel.WARN, "retry scheduled", "USER"),
                event(LogLevel.WARN, "retry scheduled", "USER"),
                event(LogLevel.INFO, "request completed", "DB"));

        Map<String, Integer> top = analyzer.topErrors(events, 2);
        assertEquals(2, top.size());
        assertEquals(2, top.get("connection timeout"));
        assertEquals(2, top.get("retry scheduled"));
    }

    @Test
    void topErrorsRespectsLimit() {
        ErrorPatternAnalyzer analyzer = new ErrorPatternAnalyzer();
        var events = List.of(
                event(LogLevel.ERROR, "error 1", "AUTH"),
                event(LogLevel.ERROR, "error 2", "USER"),
                event(LogLevel.WARN, "warn 1", "DB"),
                event(LogLevel.WARN, "warn 2", "AUTH"));

        Map<String, Integer> top = analyzer.topErrors(events, 2);
        assertEquals(2, top.size());
    }

    @Test
    void topErrorsHandlesZeroLimit() {
        ErrorPatternAnalyzer analyzer = new ErrorPatternAnalyzer();
        var events = List.of(event(LogLevel.ERROR, "error 1", "AUTH"));

        Map<String, Integer> top = analyzer.topErrors(events, 0);
        assertTrue(top.isEmpty());
    }

    @Test
    void distinctServicesReturnsUniqueServices() {
        ErrorPatternAnalyzer analyzer = new ErrorPatternAnalyzer();
        var events = List.of(
                event(LogLevel.INFO, "msg 1", "AUTH"),
                event(LogLevel.INFO, "msg 2", "USER"),
                event(LogLevel.INFO, "msg 3", "AUTH"),
                event(LogLevel.INFO, "msg 4", "DB"),
                event(LogLevel.INFO, "msg 5", "AUTH"));

        Set<String> services = analyzer.distinctServices(events);
        assertEquals(3, services.size());
        assertTrue(services.contains("AUTH"));
        assertTrue(services.contains("USER"));
        assertTrue(services.contains("DB"));
    }

    @Test
    void distinctServicesIgnoresNullService() {
        ErrorPatternAnalyzer analyzer = new ErrorPatternAnalyzer();
        var events = List.of(
                event(LogLevel.INFO, "msg 1", "AUTH"),
                new LogEvent.Builder()
                        .timestamp(Instant.parse("2026-09-13T10:00:00Z"))
                        .level(LogLevel.INFO)
                        .service(null)
                        .host("localhost")
                        .ipAddress("127.0.0.1")
                        .httpMethod(HttpMethod.GET)
                        .endpoint("/test")
                        .statusCode(200)
                        .responseTime(10)
                        .requestId("req-1")
                        .userId("user-1")
                        .message("test message")
                        .build());

        Set<String> services = analyzer.distinctServices(events);
        assertEquals(1, services.size());
        assertTrue(services.contains("AUTH"));
    }
}