package com.loginsight.incident;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loginsight.dto.response.IncidentDto;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Heuristic incident detection: a quiet baseline with one dense error window must surface as an
 * incident whose window contains that burst, tagged with a truthful method description.
 */
class IncidentDetectorTest {

    private LogEvent errorAt(Instant ts) {
        return LogEvent.builder().id(0).timestamp(ts).level(LogLevel.ERROR)
                .service("database").message("connection refused").build();
    }

    @Test
    void detectsTheDenseErrorWindow() {
        Instant start = Instant.parse("2026-09-13T00:00:00Z");
        List<LogEvent> events = new ArrayList<>();
        for (int hour = 0; hour < 2; hour++) {
            for (int minute = 0; minute < 60; minute++) {
                events.add(errorAt(start.plus(hour, ChronoUnit.HOURS)
                        .plus(minute, ChronoUnit.MINUTES)));
            }
        }
        // Two occupied windows ~ 120 errors, baseline = 120/24 = 5 → threshold = max(3, 15) = 15.
        // A real burst of 40 errors inside minute 00..04 must be detected.
        Instant burstStart = start.plus(1, ChronoUnit.HOURS).plus(0, ChronoUnit.MINUTES);
        for (int i = 0; i < 40; i++) {
            events.add(errorAt(burstStart));
        }

        List<IncidentDto> incidents = new IncidentDetector().detect(events, 10);
        assertTrue(!incidents.isEmpty());
        IncidentDto burst = incidents.get(0);
        assertTrue(burst.start().compareTo(burstStart.toString()) <= 0);
        assertTrue(burst.end().compareTo(burstStart.toString()) > 0);
        assertTrue(burst.eventCount() >= 40);
        assertTrue(burst.method().startsWith("Heuristic"));
        assertTrue(burst.services().contains("database"));
        assertEquals("connection refused", burst.primaryPattern());
    }

    @Test
    void quietDatasetYieldsNoIncidents() {
        Instant start = Instant.parse("2026-09-13T00:00:00Z");
        List<LogEvent> events = new ArrayList<>();
        for (int minute = 0; minute < 60; minute++) {
            events.add(errorAt(start.plus(minute, ChronoUnit.MINUTES)));
        }
        assertEquals(0, new IncidentDetector().detect(events, 10).size());
    }

    @Test
    void emptyDatasetYieldsNoIncidents() {
        assertEquals(0, new IncidentDetector().detect(List.of(), 10).size());
    }
}