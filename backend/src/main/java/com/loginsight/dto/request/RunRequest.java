package com.loginsight.dto.request;

import java.util.Map;

/**
 * Body of {@code POST /api/runs} (docs/REBUILD_BASELINE Phase-4). {@code algorithm} is a catalogue
 * key of a trace-instrumented algorithm and {@code input} mirrors that algorithm's request DTO.
 */
public record RunRequest(String algorithm, Map<String, Object> input) {
}