package com.loginsight.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Snapshot backing the LogInsight dashboard (docs/API.md §2). Every number is derived live from the
 * currently loaded dataset; the endpoint answers 404 when nothing is loaded so the UI can show its
 * first-run state.
 */
public record OverviewDto(String dataset, String systemStatus, long events, long errors,
                          long warnings, long services, long hosts, double eventsPerMinute,
                          long activeIncidents, List<TimelinePoint> timeline, String range,
                          Map<String, Long> severity, List<ServiceStatsDto> topServices,
                          List<PatternDto> topPatterns, List<LogEventDto> recentCritical,
                          Heatmap heatmap, Map<String, Long> statusCodes) {

    public record TimelinePoint(String start, String end, long count) {
    }

    public record Heatmap(String[] days, int columns, long[][] cells) {
    }
}