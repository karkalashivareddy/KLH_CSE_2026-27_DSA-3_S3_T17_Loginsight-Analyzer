package com.loginsight.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;
import com.loginsight.service.Dataset;

class FrequencyAnalyzerTest {

    private static LogEvent event(LogLevel level, String service, String endpoint, int statusCode, long responseTime) {
        return LogEvent.builder()
                .timestamp(Instant.parse("2026-09-13T10:00:00Z"))
                .level(level)
                .service(service)
                .host("localhost")
                .ipAddress("127.0.0.1")
                .httpMethod(HttpMethod.GET)
                .endpoint(endpoint)
                .statusCode(statusCode)
                .responseTime(responseTime)
                .requestId("req-1")
                .userId("user-1")
                .message("test message")
                .build();
    }

    private static Dataset dataset(List<LogEvent> events) {
        return new Dataset("test", events, events.size(), 0, Instant.now());
    }

    @Test
    void analyzeEmptyDataset() {
        FrequencyAnalyzer analyzer = new FrequencyAnalyzer();
        Dataset ds = dataset(List.of());
        AnalyticsResult result = analyzer.analyze(ds);

        assertEquals(0, result.getTotalEvents());
        assertEquals(0, result.getTotalValidEvents());
        assertEquals(0, result.getTotalFailedLines());
        assertTrue(result.getLevelFrequency().isEmpty());
        assertTrue(result.getServiceFrequency().isEmpty());
        assertTrue(result.getStatusCodeFrequency().isEmpty());
        assertTrue(result.getEndpointFrequency().isEmpty());
        assertEquals(0, result.getMinResponseTimeMs());
        assertEquals(0, result.getMaxResponseTimeMs());
        assertEquals(0.0, result.getAverageResponseTimeMs());
    }

    @Test
    void analyzeCountsLevelsServicesStatusesEndpoints() {
        FrequencyAnalyzer analyzer = new FrequencyAnalyzer();
        var events = List.of(
                event(LogLevel.INFO, "AUTH", "/login", 200, 10),
                event(LogLevel.ERROR, "AUTH", "/login", 500, 20),
                event(LogLevel.WARN, "USER", "/api/users", 404, 30),
                event(LogLevel.DEBUG, "AUTH", "/logout", 200, 40));
        Dataset ds = dataset(events);
        AnalyticsResult result = analyzer.analyze(ds);

        assertEquals(4, result.getTotalEvents());
        assertEquals(4, result.getTotalValidEvents());
        assertEquals(0, result.getTotalFailedLines());

        assertEquals(1, result.getLevelFrequency().get("INFO"));
        assertEquals(1, result.getLevelFrequency().get("ERROR"));
        assertEquals(1, result.getLevelFrequency().get("WARN"));
        assertEquals(1, result.getLevelFrequency().get("DEBUG"));

        assertEquals(3, result.getServiceFrequency().get("AUTH"));
        assertEquals(1, result.getServiceFrequency().get("USER"));

        assertEquals(2, result.getStatusCodeFrequency().get(200));
        assertEquals(1, result.getStatusCodeFrequency().get(500));
        assertEquals(1, result.getStatusCodeFrequency().get(404));

        assertEquals(2, result.getEndpointFrequency().get("/login"));
        assertEquals(1, result.getEndpointFrequency().get("/api/users"));
        assertEquals(1, result.getEndpointFrequency().get("/logout"));
    }

    @Test
    void analyzeResponseTimeStats() {
        FrequencyAnalyzer analyzer = new FrequencyAnalyzer();
        var events = List.of(
                event(LogLevel.INFO, "AUTH", "/login", 200, 10),
                event(LogLevel.INFO, "USER", "/api", 200, 20),
                event(LogLevel.INFO, "DB", "/query", 200, 30));
        Dataset ds = dataset(events);
        AnalyticsResult result = analyzer.analyze(ds);

        assertEquals(10, result.getMinResponseTimeMs());
        assertEquals(30, result.getMaxResponseTimeMs());
        assertEquals(20.0, result.getAverageResponseTimeMs());
    }

    @Test
    void analyzeIgnoresZeroStatusCode() {
        FrequencyAnalyzer analyzer = new FrequencyAnalyzer();
        var events = List.of(
                event(LogLevel.INFO, "AUTH", "/login", 0, 10),
                event(LogLevel.INFO, "USER", "/api", 200, 20));
        Dataset ds = dataset(events);
        AnalyticsResult result = analyzer.analyze(ds);

        assertFalse(result.getStatusCodeFrequency().containsKey(0));
        assertEquals(1, result.getStatusCodeFrequency().get(200));
    }

    @Test
    void analyzeNullLevelIsIgnored() {
        FrequencyAnalyzer analyzer = new FrequencyAnalyzer();
        var events = List.of(
                new LogEvent.Builder()
                        .timestamp(Instant.parse("2026-09-13T10:00:00Z"))
                        .level(null)
                        .service("AUTH")
                        .host("localhost")
                        .ipAddress("127.0.0.1")
                        .httpMethod(HttpMethod.GET)
                        .endpoint("/login")
                        .statusCode(200)
                        .responseTime(10)
                        .requestId("req-1")
                        .userId("user-1")
                        .message("test message")
                        .build());
        Dataset ds = dataset(events);
        AnalyticsResult result = analyzer.analyze(ds);

        assertFalse(result.getLevelFrequency().containsKey("null"));
        assertEquals(1, result.getServiceFrequency().get("AUTH"));
    }
}