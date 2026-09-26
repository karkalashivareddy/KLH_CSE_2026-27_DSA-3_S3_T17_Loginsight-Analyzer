package com.loginsight.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

class ServiceGraphBuilderTest {

    private static LogEvent event(String requestId, String service, String endpoint) {
        return LogEvent.builder()
                .timestamp(Instant.parse("2026-09-13T10:00:00Z"))
                .level(LogLevel.INFO)
                .service(service)
                .host("localhost")
                .ipAddress("127.0.0.1")
                .httpMethod(HttpMethod.GET)
                .endpoint(endpoint)
                .statusCode(200)
                .responseTime(10)
                .requestId(requestId)
                .userId("user-1")
                .message("test message")
                .build();
    }

    @Test
    void buildCreatesNodesAndEdgesFromRequestChains() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        var events = List.of(
                event("req-1", "AUTH", "/login"),
                event("req-1", "USER", "/api/users"),
                event("req-1", "DATABASE", "/db"),
                event("req-2", "CLIENT", "/api"),
                event("req-2", "API_GATEWAY", "/gateway"),
                event("req-2", "AUTH", "/login"));

        ServiceDependencyGraph graph = builder.build(events);

        assertEquals(5, graph.nodeCount());
        assertEquals(4, graph.edgeCount());
        assertTrue(graph.successors("AUTH").contains("USER"));
        assertTrue(graph.successors("USER").contains("DATABASE"));
        assertTrue(graph.successors("CLIENT").contains("API_GATEWAY"));
        assertTrue(graph.successors("API_GATEWAY").contains("AUTH"));
    }

    private static LogEvent orderedEvent(long id, String timestamp, String requestId,
                                         String service) {
        return LogEvent.builder()
                .id(id)
                .timestamp(Instant.parse(timestamp))
                .level(LogLevel.INFO)
                .service(service)
                .message("event " + id)
                .requestId(requestId)
                .build();
    }

    @Test
    void buildDoesNotGroupNullRequestIdsTogether() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        ServiceDependencyGraph graph = builder.build(List.of(
                orderedEvent(1, "2026-09-13T10:03:00Z", null, "A"),
                orderedEvent(2, "2026-09-13T10:01:00Z", null, "B"),
                orderedEvent(3, "2026-09-13T10:02:00Z", null, "C")));

        assertEquals(3, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
        assertTrue(builder.requestTrailCounts().isEmpty());
    }

    @Test
    void buildOrdersRequestGroupsByTimestampThenId() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        ServiceDependencyGraph graph = builder.build(List.of(
                orderedEvent(4, "2026-09-13T10:01:00Z", "req-1", "D"),
                orderedEvent(1, "2026-09-13T10:02:00Z", "req-1", "C"),
                orderedEvent(3, "2026-09-13T10:00:00Z", "req-1", "B"),
                orderedEvent(2, "2026-09-13T10:00:00Z", "req-1", "A")));

        assertEquals(Map.of("A->B", 1, "B->D", 1, "D->C", 1),
                builder.requestTrailCounts());
        assertEquals(3, graph.edgeCount());
        assertTrue(graph.successors("A").contains("B"));
        assertTrue(graph.successors("B").contains("D"));
        assertTrue(graph.successors("D").contains("C"));
    }

    @Test
    void buildDoesNotEmitSelfTransitions() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        ServiceDependencyGraph graph = builder.build(List.of(
                orderedEvent(1, "2026-09-13T10:00:00Z", "req-1", "A"),
                orderedEvent(2, "2026-09-13T10:01:00Z", "req-1", "A"),
                orderedEvent(3, "2026-09-13T10:02:00Z", "req-1", "B")));

        assertEquals(1, graph.edgeCount());
        assertEquals(Map.of("A->B", 1), builder.requestTrailCounts());
    }

    @Test
    void buildIgnoresNullService() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        var events = List.of(
                event("req-1", null, "/login"),
                event("req-1", "USER", "/api/users"));

        ServiceDependencyGraph graph = builder.build(events);
        assertEquals(1, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
    }

    @Test
    void requestTrailCountsReturnsEdgeFrequencies() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        var events = List.of(
                event("req-1", "AUTH", "/login"),
                event("req-1", "USER", "/api/users"),
                event("req-2", "AUTH", "/login"),
                event("req-2", "USER", "/api/users"),
                event("req-2", "DATABASE", "/db"));

        builder.build(events);
        Map<String, Integer> trails = builder.requestTrailCounts();

        assertEquals(2, trails.get("AUTH->USER"));
        assertEquals(1, trails.get("USER->DATABASE"));
    }

    @Test
    void nodeCountsFromReturnsServiceFrequencies() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        var events = List.of(
                event("req-1", "AUTH", "/login"),
                event("req-1", "USER", "/api/users"),
                event("req-2", "AUTH", "/login"));

        Map<String, Integer> counts = builder.nodeCountsFrom(events);
        assertEquals(2, counts.get("AUTH"));
        assertEquals(1, counts.get("USER"));
    }

    @Test
    void topNodesReturnsSortedByCount() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        var events = List.of(
                event("req-1", "AUTH", "/login"),
                event("req-1", "USER", "/api/users"),
                event("req-2", "AUTH", "/login"),
                event("req-2", "AUTH", "/login"),
                event("req-3", "DATABASE", "/db"));

        List<Map.Entry<String, Integer>> top = builder.topNodes(events, 2);
        assertEquals(2, top.size());
        assertEquals("AUTH", top.get(0).getKey());
        assertEquals(3, top.get(0).getValue());
        assertEquals("USER", top.get(1).getKey());
        assertEquals(1, top.get(1).getValue());
    }

    @Test
    void topNodesRespectsLimit() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        var events = List.of(
                event("req-1", "AUTH", "/login"),
                event("req-2", "USER", "/api"),
                event("req-3", "DATABASE", "/db"));

        List<Map.Entry<String, Integer>> top = builder.topNodes(events, 2);
        assertEquals(2, top.size());
    }

    @Test
    void topNodesHandlesZeroLimit() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        var events = List.of(event("req-1", "AUTH", "/login"));

        List<Map.Entry<String, Integer>> top = builder.topNodes(events, 0);
        assertTrue(top.isEmpty());
    }

    @Test
    void reachableFromDelegatesToGraph() {
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        var events = List.of(
                event("req-1", "AUTH", "/login"),
                event("req-1", "USER", "/api/users"));

        builder.build(events);
        assertEquals(2, builder.reachableFrom("AUTH"));
        assertEquals(0, builder.reachableFrom("UNKNOWN"));
    }
}