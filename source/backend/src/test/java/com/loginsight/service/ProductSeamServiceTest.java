package com.loginsight.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.loginsight.controller.ServicesController;
import com.loginsight.dto.response.DatasetStatsDto;
import com.loginsight.dto.response.OverviewDto;
import com.loginsight.dto.response.ServiceStatsDto;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.index.LogIndexService;
import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

class ProductSeamServiceTest {

    @Test
    void firstReturnsNullForAnEmptyDataset() {
        DatasetService datasets = mock(DatasetService.class);
        when(datasets.currentDataset()).thenReturn(Optional.of(
                new Dataset("empty", List.of(), 0, 0, Instant.now())));

        assertNull(new LogService(datasets).first());
    }

    @Test
    void serviceDetailKeepsEqualTimestampsStableAndAnchorsActivityToData() {
        Instant timestamp = Instant.parse("2026-09-13T10:00:00Z");
        List<LogEvent> events = List.of(
                event(1, timestamp, "api-gateway"),
                event(2, timestamp, "api-gateway"),
                event(3, timestamp, "api-gateway"));
        DatasetService datasets = mock(DatasetService.class);
        when(datasets.currentDataset()).thenReturn(Optional.of(
                new Dataset("fixture", events, events.size(), 0, Instant.now())));

        ServiceStatsDto.ServiceDetail detail = new ServicesController(datasets)
                .service("api-gateway", 10);

        assertEquals(List.of(1L, 2L, 3L), detail.recentEvents().stream()
                .map(event -> event.id()).toList());
        assertEquals(24, detail.activity().size());
        assertEquals(events.size(), detail.activity().stream()
                .mapToLong(row -> ((Number) row.get("count")).longValue()).sum());
    }

    @Test
    void statsUseSharedErrorAndLatencySemantics() {
        Instant start = Instant.parse("2026-09-13T10:00:00Z");
        List<LogEvent> events = List.of(
                LogEvent.builder().id(1).timestamp(start).level(LogLevel.ERROR).service("api")
                        .responseTime(10).message("failed").build(),
                LogEvent.builder().id(2).timestamp(start.plusSeconds(60)).level(LogLevel.FATAL)
                        .service("api").responseTime(20).message("fatal").build(),
                LogEvent.builder().id(3).timestamp(start.plusSeconds(120)).level(LogLevel.INFO)
                        .service("api").message("healthy").build());
        DatasetService datasets = mock(DatasetService.class);
        when(datasets.currentDataset()).thenReturn(Optional.of(
                new Dataset("fixture", events, events.size(), 0, Instant.now())));

        DatasetStatsDto stats = new LogService(datasets).stats();

        assertEquals(2, stats.errors());
        assertEquals(15.0, stats.avgResponseTimeMs(), 0.0001);
        assertEquals(1.5, stats.requestsPerMinute(), 0.0001);
    }

    @Test
    void overviewScopesEveryMetricToTheSelectedWindow() {
        Instant latest = Instant.parse("2026-09-13T12:00:00Z");
        List<LogEvent> events = List.of(
                overviewEvent(1, latest.minusSeconds(7200), LogLevel.ERROR, "old", "old-host", 418,
                        "old failure"),
                overviewEvent(2, latest.minusSeconds(240), LogLevel.INFO, "api", "gw", 200,
                        "selected info"),
                overviewEvent(3, latest.minusSeconds(180), LogLevel.WARN, "api", "gw", 429,
                        "selected warning"),
                overviewEvent(4, latest.minusSeconds(120), LogLevel.ERROR, "worker", "worker-host", 503,
                        "selected failure"),
                overviewEvent(5, latest.minusSeconds(60), LogLevel.ERROR, "worker", "worker-host", 500,
                        "selected failure"),
                overviewEvent(6, latest, LogLevel.FATAL, "api", "gw", 500,
                        "selected failure"));
        Dataset dataset = new Dataset("fixture", events, events.size(), 0, Instant.now());
        DatasetService datasets = mock(DatasetService.class);
        when(datasets.currentDataset()).thenReturn(Optional.of(dataset));

        OverviewDto snapshot = new OverviewService(datasets, mock(LogIndexService.class))
                .snapshot("5m");

        assertEquals("fixture", snapshot.dataset());
        assertEquals("Operational", snapshot.systemStatus());
        assertEquals("5m", snapshot.range());
        assertEquals("selected-window", snapshot.scope());
        assertEquals(5, snapshot.events());
        assertEquals(6, snapshot.datasetEvents());
        assertEquals(3, snapshot.errors());
        assertEquals(1, snapshot.warnings());
        assertEquals(2, snapshot.services());
        assertEquals(2, snapshot.hosts());
        assertEquals(1.0, snapshot.eventsPerMinute(), 0.0001);
        assertEquals(0, snapshot.activeIncidents());
        assertEquals(2L, snapshot.severity().get("ERROR"));
        assertEquals(1L, snapshot.severity().get("FATAL"));
        assertEquals(1L, snapshot.severity().get("WARN"));
        assertEquals("api", snapshot.topServices().get(0).name());
        assertEquals(3, snapshot.topServices().get(0).events());
        assertTrue(snapshot.topPatterns().stream()
                .noneMatch(pattern -> "old failure".equals(pattern.example())));
        assertEquals(List.of(6L, 5L, 4L), snapshot.recentCritical().stream()
                .map(event -> event.id()).toList());
        assertEquals(5, snapshot.timeline().stream()
                .mapToLong(point -> point.count()).sum());
        long heatmapEvents = 0;
        for (long[] row : snapshot.heatmap().cells()) {
            for (long cell : row) {
                heatmapEvents += cell;
            }
        }
        assertEquals(5, heatmapEvents);
        assertEquals(2L, snapshot.statusCodes().get("500"));
        assertEquals(1L, snapshot.statusCodes().get("503"));
        assertEquals(1L, snapshot.statusCodes().get("429"));
        assertEquals(1L, snapshot.statusCodes().get("200"));
        assertEquals(null, snapshot.statusCodes().get("418"));
        assertEquals(latest.minusSeconds(300).toString(), snapshot.windowStart());
        assertEquals(latest.toString(), snapshot.windowEnd());
    }

    @Test
    void liveReplaySnapshotIsChronologicalAndDoesNotMutateDataset() {
        Instant timestamp = Instant.parse("2026-09-13T10:00:00Z");
        List<LogEvent> source = new ArrayList<>(List.of(
                event(3, timestamp.plusSeconds(60), "third"),
                event(1, timestamp, "first"),
                event(2, timestamp, "second")));
        Dataset dataset = new Dataset("fixture", source, source.size(), 0, Instant.now());

        List<LogEvent> ordered = LiveStreamService.chronologicalSnapshot(dataset.events());

        assertEquals(List.of(1L, 2L, 3L), ordered.stream()
                .map(LogEvent::getId).toList());
        assertEquals(List.of(3L, 1L, 2L), dataset.events().stream()
                .map(LogEvent::getId).toList());
        assertEquals(List.of(3L, 1L, 2L), source.stream()
                .map(LogEvent::getId).toList());
    }

    @Test
    void liveControlsHaveExplicitBoundsAndShutdownIsSafe() {
        assertEquals(1, LiveStreamService.requireBatchSize(1));
        assertEquals(200, LiveStreamService.requireBatchSize(200));
        assertEquals(100L, LiveStreamService.requireIntervalMillis(100L));
        assertEquals(60_000L, LiveStreamService.requireIntervalMillis(60_000L));
        assertThrows(InvalidQueryException.class, () -> LiveStreamService.requireBatchSize(0));
        assertThrows(InvalidQueryException.class, () -> LiveStreamService.requireBatchSize(201));
        assertThrows(InvalidQueryException.class,
                () -> LiveStreamService.requireIntervalMillis(99L));
        assertThrows(InvalidQueryException.class,
                () -> LiveStreamService.requireIntervalMillis(60_001L));

        DatasetService datasets = mock(DatasetService.class);
        LiveStreamService service = new LiveStreamService(datasets);
        assertDoesNotThrow(service::shutdown);
    }

    private static LogEvent overviewEvent(long id, Instant timestamp, LogLevel level,
                                          String service, String host, int status, String message) {
        return LogEvent.builder()
                .id(id)
                .timestamp(timestamp)
                .level(level)
                .service(service)
                .host(host)
                .httpMethod(HttpMethod.GET)
                .statusCode(status)
                .message(message)
                .build();
    }

    private static LogEvent event(long id, Instant timestamp, String service) {
        return LogEvent.builder()
                .id(id)
                .timestamp(timestamp)
                .level(LogLevel.INFO)
                .service(service)
                .message("event " + id)
                .build();
    }
}
