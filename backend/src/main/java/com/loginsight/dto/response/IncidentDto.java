package com.loginsight.dto.response;

import java.util.List;

/**
 * An evidence-based incident descriptor (docs/API.md §6). Incidents are detected heuristically by
 * the {@code IncidentDetector}: 5-minute windows whose error volume exceeds a threshold derived
 * from the dataset's own baseline are merged into incidents. The method is always labelled —
 * nothing here is claimed to be ML.
 */
public record IncidentDto(long id, String start, String end, List<String> services,
                          long eventCount, String primaryPattern, String status, String method) {

    public record IncidentDetail(IncidentDto incident, List<LogEventDto> events, long total) {
    }
}