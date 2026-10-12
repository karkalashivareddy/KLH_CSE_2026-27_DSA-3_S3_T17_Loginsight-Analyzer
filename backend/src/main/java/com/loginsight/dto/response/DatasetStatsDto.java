package com.loginsight.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Response of {@code GET /api/logs/stats} (docs/12 §2). Frequencies come from the analytics layer;
 * time-window buckets, top lists and derived rates are computed over the real dataset — never
 * fabricated numbers.
 */
public record DatasetStatsDto(long totalLogs, long errors, long warnings, long services,
                              long uniqueIps, String topError, double avgResponseTimeMs,
                              double requestsPerMinute, Map<String, Integer> levels,
                              List<Map<String, Object>> topServices,
                              Map<String, Integer> statusCodes,
                              List<Map<String, Object>> logsOverTime) {
}