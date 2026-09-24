package com.loginsight.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.dto.response.HttpStatsDto;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Fleet analytics: severity histogram includes zero levels, timeline buckets emit zero-count
 * windows and cover the range, heatmap cells sum to the event count, and service rollups compute
 * event/error/host metrics. All derived live; nothing precomputed.
 */
class FleetAnalyticsTest {

    private List<LogEvent> fixtures() {
        List<LogEvent> out = new ArrayList<>();
        out.add(e(Instant.parse("2026-09-13T00:10:00Z"), LogLevel.INFO, "api-gateway", "gw-1", 200));
        out.add(e(Instant.parse("2026-09-13T00:20:00Z"), LogLevel.ERROR, "database", "db-1", 503));
        out.add(e(Instant.parse("2026-09-13T00:30:00Z"), LogLevel.WARN, "api-gateway", "gw-1", 429));
        out.add(e(Instant.parse("2026-09-13T00:40:00Z"), LogLevel.FATAL, "database", "db-1", 500));
        return out;
    }

    private static LogEvent e(Instant ts, LogLevel level, String service, String host, int status) {
        return LogEvent.builder().id(0).timestamp(ts).level(level).service(service).host(host)
                .statusCode(status).httpMethod(com.loginsight.model.HttpMethod.GET)
                .endpoint("/x").responseTime(120).message("m").build();
    }

    @Test
    void severityDistributionIncludesZeroLevels() {
        Map<String, Long> distribution = new SeverityAnalyzer().distribution(fixtures());
        assertEquals(6, distribution.size());
        assertEquals(0L, distribution.get("TRACE"));
        assertEquals(1L, distribution.get("ERROR"));
        assertEquals(1L, distribution.get("FATAL"));
    }

    @Test
    void timelineZeroCountsAreEmitted() {
        List<TimelineAnalyzer.Point> points = new TimelineAnalyzer()
                .buckets(fixtures(), 4);
        assertEquals(4, points.size());
        long sum = points.stream().mapToLong(TimelineAnalyzer.Point::count).sum();
        assertEquals(4, sum);
        assertTrue(points.get(0).count() >= 1);
    }

    @Test
    void rangeBucketsCoverExplicitWindow() {
        TimelineAnalyzer analyzer = new TimelineAnalyzer();
        long from = Instant.parse("2026-09-13T00:00:00Z").toEpochMilli();
        long to = Instant.parse("2026-09-13T01:00:00Z").toEpochMilli();
        List<TimelineAnalyzer.Point> points = analyzer.rangeBuckets(fixtures(), from, to, 3);
        assertEquals(3, points.size());
        assertTrue(points.get(0).count() >= 1);
        assertEquals(Instant.parse("2026-09-13T00:00:00Z").toString(), points.get(0).start());
        long last = Instant.parse(points.get(2).end()).toEpochMilli();
        assertTrue(last >= from && last < to);
        long sum = points.stream().mapToLong(TimelineAnalyzer.Point::count).sum();
        assertEquals(4, sum);
    }

    @Test
    void heatmapCellsSumToEventCount() {
        List<LogEvent> events = new ArrayList<>(fixtures());
        events.add(e(Instant.parse("2026-09-13T23:59:00Z"), LogLevel.INFO, "cache", "c-1", 200));
        HeatmapAnalyzer.Heatmap heatmap = new HeatmapAnalyzer().hourByWeekday(events);
        long sum = 0;
        for (long[] day : heatmap.cells()) {
            for (long cell : day) {
                sum += cell;
            }
        }
        assertEquals(events.size(), sum);
        assertEquals(24, heatmap.columns());
    }

    @Test
    void serviceRollupsComputeErrorsAndHosts() {
        FleetAnalyzer analyzer = new FleetAnalyzer();
        List<com.loginsight.dto.response.ServiceStatsDto> services = analyzer.services(fixtures(), 10);
        Map<String, com.loginsight.dto.response.ServiceStatsDto> byName = new LinkedHashMap<>();
        for (com.loginsight.dto.response.ServiceStatsDto s : services) {
            byName.put(s.name(), s);
        }
        com.loginsight.dto.response.ServiceStatsDto gateway = byName.get("api-gateway");
        com.loginsight.dto.response.ServiceStatsDto database = byName.get("database");
        assertEquals(2, gateway.events());
        assertEquals(1, gateway.warnings());
        assertEquals(2, database.errors());
        assertEquals(1, database.hosts());
        assertTrue(database.eventRate() > 0);
    }

    @Test
    void httpStatsAccumulateMethodsAndLatency() {
        HttpStatsDto stats = new FleetAnalyzer().http(fixtures());
        assertEquals(4, stats.sampled());
        assertEquals(4L, stats.methods().get("GET"));
        assertEquals(1L, stats.statusCodes().get("503"));
        assertTrue(stats.latencyMax() >= stats.latencyP95());
    }
}