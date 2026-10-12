package com.loginsight.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

class TopKFrequentAnalyzerTest {

    private static LogEvent event(String service, String endpoint, LogLevel level, String ip) {
        return LogEvent.builder()
                .timestamp(Instant.parse("2026-09-13T10:00:00Z"))
                .level(level)
                .service(service)
                .host("localhost")
                .ipAddress(ip)
                .httpMethod(HttpMethod.GET)
                .endpoint(endpoint)
                .statusCode(200)
                .responseTime(10)
                .requestId("req-1")
                .userId("user-1")
                .message("test message")
                .build();
    }

    private static LogEvent eventNullLevel(String service, String endpoint, String ip) {
        return LogEvent.builder()
                .timestamp(Instant.parse("2026-09-13T10:00:00Z"))
                .level(null)
                .service(service)
                .host("localhost")
                .ipAddress(ip)
                .httpMethod(HttpMethod.GET)
                .endpoint(endpoint)
                .statusCode(200)
                .responseTime(10)
                .requestId("req-1")
                .userId("user-1")
                .message("test message")
                .build();
    }

    @Test
    void topKByService() {
        TopKFrequentAnalyzer analyzer = new TopKFrequentAnalyzer();
        var events = List.of(
                event("AUTH", "/login", LogLevel.INFO, "10.0.0.1"),
                event("USER", "/api/users", LogLevel.INFO, "10.0.0.2"),
                event("AUTH", "/login", LogLevel.ERROR, "10.0.0.3"),
                event("AUTH", "/logout", LogLevel.DEBUG, "10.0.0.4"),
                event("DATABASE", "/db", LogLevel.INFO, "10.0.0.5"));

        List<Map.Entry<String, Integer>> result = analyzer.topK(events, TopKFrequentAnalyzer.Dimension.SERVICE, 2);
        assertEquals(2, result.size());
        assertEquals("AUTH", result.get(0).getKey());
        assertEquals(3, result.get(0).getValue());
    }

    @Test
    void topKByEndpoint() {
        TopKFrequentAnalyzer analyzer = new TopKFrequentAnalyzer();
        var events = List.of(
                event("AUTH", "/login", LogLevel.INFO, "10.0.0.1"),
                event("USER", "/api/users", LogLevel.INFO, "10.0.0.2"),
                event("AUTH", "/login", LogLevel.ERROR, "10.0.0.3"));

        List<Map.Entry<String, Integer>> result = analyzer.topK(events, TopKFrequentAnalyzer.Dimension.ENDPOINT, 1);
        assertEquals(1, result.size());
        assertEquals("/login", result.get(0).getKey());
        assertEquals(2, result.get(0).getValue());
    }

    @Test
    void topKByLevel() {
        TopKFrequentAnalyzer analyzer = new TopKFrequentAnalyzer();
        var events = List.of(
                event("AUTH", "/login", LogLevel.INFO, "10.0.0.1"),
                event("USER", "/api/users", LogLevel.ERROR, "10.0.0.2"),
                event("AUTH", "/login", LogLevel.ERROR, "10.0.0.3"),
                event("AUTH", "/logout", LogLevel.DEBUG, "10.0.0.4"));

        List<Map.Entry<String, Integer>> result = analyzer.topK(events, TopKFrequentAnalyzer.Dimension.LEVEL, 2);
        assertEquals(2, result.size());
        assertEquals("ERROR", result.get(0).getKey());
        assertEquals(2, result.get(0).getValue());
        assertEquals("INFO", result.get(1).getKey());
        assertEquals(1, result.get(1).getValue());
    }

    @Test
    void topKByIp() {
        TopKFrequentAnalyzer analyzer = new TopKFrequentAnalyzer();
        var events = List.of(
                event("AUTH", "/login", LogLevel.INFO, "10.0.0.1"),
                event("USER", "/api/users", LogLevel.INFO, "10.0.0.2"),
                event("AUTH", "/login", LogLevel.ERROR, "10.0.0.1"));

        List<Map.Entry<String, Integer>> result = analyzer.topK(events, TopKFrequentAnalyzer.Dimension.IP, 1);
        assertEquals(1, result.size());
        assertEquals("10.0.0.1", result.get(0).getKey());
        assertEquals(2, result.get(0).getValue());
    }

    @Test
    void topKRespectsLimit() {
        TopKFrequentAnalyzer analyzer = new TopKFrequentAnalyzer();
        var events = List.of(
                event("AUTH", "/login", LogLevel.INFO, "10.0.0.1"),
                event("USER", "/api/users", LogLevel.INFO, "10.0.0.2"),
                event("DATABASE", "/db", LogLevel.INFO, "10.0.0.3"));

        List<Map.Entry<String, Integer>> result = analyzer.topK(events, TopKFrequentAnalyzer.Dimension.SERVICE, 2);
        assertEquals(2, result.size());
    }

    @Test
    void topKHandlesZeroLimit() {
        TopKFrequentAnalyzer analyzer = new TopKFrequentAnalyzer();
        var events = List.of(event("AUTH", "/login", LogLevel.INFO, "10.0.0.1"));

        List<Map.Entry<String, Integer>> result = analyzer.topK(events, TopKFrequentAnalyzer.Dimension.SERVICE, 0);
        assertTrue(result.isEmpty());
    }

    @Test
    void topKHandlesEmptyEvents() {
        TopKFrequentAnalyzer analyzer = new TopKFrequentAnalyzer();
        List<Map.Entry<String, Integer>> result = analyzer.topK(List.of(), TopKFrequentAnalyzer.Dimension.SERVICE, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    void topKHandlesNullKey() {
        TopKFrequentAnalyzer analyzer = new TopKFrequentAnalyzer();
        var events = List.of(eventNullLevel("AUTH", "/login", "10.0.0.1"));

        List<Map.Entry<String, Integer>> result = analyzer.topK(events, TopKFrequentAnalyzer.Dimension.LEVEL, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    void asBucketsProducesCorrectStructure() {
        TopKFrequentAnalyzer analyzer = new TopKFrequentAnalyzer();
        var events = List.of(event("AUTH", "/login", LogLevel.INFO, "10.0.0.1"));

        List<Map.Entry<String, Integer>> ranked = analyzer.topK(events, TopKFrequentAnalyzer.Dimension.SERVICE, 1);
        List<Map<String, Object>> buckets = analyzer.asBuckets(ranked);

        assertEquals(1, buckets.size());
        assertEquals("AUTH", buckets.get(0).get("service"));
        assertEquals(1, buckets.get(0).get("count"));
    }
}