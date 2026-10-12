package com.loginsight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loginsight.dto.response.IncidentDto;
import com.loginsight.exception.DatasetException;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Incident read model: the list, the count and the detail view resolve the same capped detection
 * set, so an incident past the twentieth is still reachable, and negative limits / inverted ranges
 * are rejected as 4xx input errors instead of silently returning nothing.
 */
class IncidentsServiceTest {

    private static final Instant START = Instant.parse("2026-09-13T00:00:00Z");

    private DatasetService datasetWith(List<LogEvent> events) {
        DatasetService service = new DatasetService(".");
        service.ingest("test-corpus", new java.io.ByteArrayInputStream(
                render(events).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return service;
    }

    private static String render(List<LogEvent> events) {
        StringBuilder sb = new StringBuilder();
        for (LogEvent event : events) {
            sb.append("{\"timestamp\":\"").append(event.getTimestamp())
                    .append("\",\"level\":\"").append(event.getLevel().name())
                    .append("\",\"service\":\"").append(event.getService())
                    .append("\",\"message\":\"").append(event.getMessage())
                    .append("\"}\n");
        }
        return sb.toString();
    }

    /** 25 separated bursts, each far above the quiet baseline, so more than 20 incidents exist. */
    private static List<LogEvent> manyIncidentEvents() {
        List<LogEvent> events = new ArrayList<>();
        Instant quiet = START;
        for (int minute = 0; minute < 24 * 60; minute++) {
            events.add(error(quiet.plus(minute, ChronoUnit.MINUTES), "heartbeat ok"));
        }
        for (int burst = 0; burst < 25; burst++) {
            Instant at = START.plus(24, ChronoUnit.HOURS).plus(burst * 30L, ChronoUnit.MINUTES);
            for (int i = 0; i < 40; i++) {
                events.add(error(at, "connection refused"));
            }
        }
        return events;
    }

    private static LogEvent error(Instant ts, String message) {
        return LogEvent.builder().timestamp(ts).level(LogLevel.ERROR)
                .service("database").message(message).build();
    }

    @Test
    void incidentsBeyondTheTwentiethAreStillReachable() {
        IncidentsService service = new IncidentsService(datasetWith(manyIncidentEvents()));
        List<IncidentDto> incidents = service.detect(IncidentsService.MAX_INCIDENTS);
        assertTrue(incidents.size() > 20, "fixture must produce more than 20 incidents");
        long lastId = incidents.get(incidents.size() - 1).id();
        assertTrue(lastId > 20, "the last incident id must exceed the old 20-item window");

        IncidentDto.IncidentDetail detail = service.detail(lastId, 10);
        assertEquals(lastId, detail.incident().id());
        assertNotNull(detail.events());
        assertEquals(service.activeCount(), incidents.size(),
                "count, list and detail must agree on the same detection set");
        assertEquals(lastId, service.find(lastId).id());
    }

    @Test
    void listLimitIsClampedToTheSharedCap() {
        IncidentsService service = new IncidentsService(datasetWith(manyIncidentEvents()));
        assertTrue(service.detect(Integer.MAX_VALUE).size() <= IncidentsService.MAX_INCIDENTS);
    }

    @Test
    void negativeAndZeroLimitsAreRejected() {
        IncidentsService service = new IncidentsService(datasetWith(manyIncidentEvents()));
        assertThrows(InvalidQueryException.class, () -> service.detect(0));
        assertThrows(InvalidQueryException.class, () -> service.detect(-1));
        assertThrows(InvalidQueryException.class, () -> service.detail(1L, 0));
        assertThrows(InvalidQueryException.class, () -> service.detail(1L, -5));
    }

    @Test
    void negativeOffsetIsRejected() {
        IncidentsService service = new IncidentsService(datasetWith(manyIncidentEvents()));
        String start = START.toString();
        String end = START.plus(48, ChronoUnit.HOURS).toString();
        assertThrows(InvalidQueryException.class,
                () -> service.eventsInWindow(start, end, 10, -1));
    }

    @Test
    void unparsableAndInvertedRangesAreRejected() {
        IncidentsService service = new IncidentsService(datasetWith(manyIncidentEvents()));
        assertThrows(InvalidQueryException.class,
                () -> service.eventsInWindow("not-a-date", START.toString(), 10, 0));
        assertThrows(InvalidQueryException.class,
                () -> service.eventsInWindow(START.toString(), "13-09-2026", 10, 0));
        assertThrows(InvalidQueryException.class, () -> service.eventsInWindow(
                START.plus(2, ChronoUnit.HOURS).toString(), START.toString(), 10, 0));
    }

    @Test
    void evidenceWindowIsHonoured() {
        IncidentsService service = new IncidentsService(datasetWith(manyIncidentEvents()));
        String from = START.toString();
        String to = START.plus(10, ChronoUnit.MINUTES).toString();
        assertEquals(10, service.eventsInWindow(from, to, 10, 0).size());
        assertEquals(3, service.eventsInWindow(from, to, 10, 7).size(), "offset skips rows");
    }

    @Test
    void unknownIncidentIsNotFoundAndMissingDatasetIsNotFound() {
        IncidentsService service = new IncidentsService(datasetWith(manyIncidentEvents()));
        assertThrows(DatasetException.class, () -> service.find(999_999L));
        assertThrows(DatasetException.class, () -> new IncidentsService(new DatasetService("."))
                .detect(5));
    }
}
