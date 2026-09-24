package com.loginsight.dto.response;

import java.util.List;
import java.util.Map;

/**
 * HTTP analytics over the events that carry HTTP metadata (docs/API.md §7).
 */
public record HttpStatsDto(Map<String, Long> statusCodes, Map<String, Long> methods,
                           List<Endpoint> endpoints, long latencyP50, long latencyP95,
                           long latencyMax, long sampled) {

    public record Endpoint(String endpoint, long count) {
    }
}