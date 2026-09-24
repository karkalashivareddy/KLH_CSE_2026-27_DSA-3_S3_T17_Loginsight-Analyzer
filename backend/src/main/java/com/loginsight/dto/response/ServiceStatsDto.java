package com.loginsight.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Aggregated per-service analytics derived from the loaded dataset (docs/API.md §8).
 */
public record ServiceStatsDto(String name, long events, long errors, long warnings,
                              double eventRate, int hosts, String latestAt,
                              Map<String, Long> severity) {

    public record Endpoint(String endpoint, long count) {
    }

    public record ServiceDetail(ServiceStatsDto summary, List<LogEventDto> recentEvents,
                                long totalEvents, List<Map<String, Object>> activity) {
    }
}