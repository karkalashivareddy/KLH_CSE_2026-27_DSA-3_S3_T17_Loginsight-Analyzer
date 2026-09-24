package com.loginsight.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.loginsight.dto.response.IncidentDto;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.exception.DatasetException;
import com.loginsight.incident.IncidentDetector;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Incident read model (docs/API.md §6). Incidents are re-derived from the loaded dataset on every
 * request through the heuristic {@link IncidentDetector}; the dashboard count, the list and the
 * detail view always show the same evidence, never a cached guess.
 */
@Service
public class IncidentsService {

    private final DatasetService datasetService;
    private final IncidentDetector incidentDetector = new IncidentDetector();

    public IncidentsService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    public List<IncidentDto> detect(int limit) {
        return incidentDetector.detect(currentEvents(), limit);
    }

    public IncidentDto.IncidentDetail detail(long id, int logLimit) {
        for (IncidentDto incident : detect(20)) {
            if (incident.id() == id) {
                List<LogEventDto> events = eventsInWindow(incident.start(), incident.end(),
                        logLimit, 0);
                return new IncidentDto.IncidentDetail(incident, events, events.size());
            }
        }
        throw new DatasetException("Incident not found in the current dataset: " + id);
    }

    public List<LogEventDto> eventsInWindow(String fromIso, String toIso, int limit, int offset) {
        java.time.Instant from = java.time.Instant.parse(fromIso);
        java.time.Instant to = java.time.Instant.parse(toIso);
        List<LogEventDto> out = new ArrayList<>();
        int skipped = 0;
        for (LogEvent event : currentEvents()) {
            if (event.getTimestamp() == null) {
                continue;
            }
            long ts = event.getTimestamp().toEpochMilli();
            if (ts >= from.toEpochMilli() && ts < to.toEpochMilli()) {
                if (skipped++ < offset) {
                    continue;
                }
                if (out.size() >= limit) {
                    break;
                }
                out.add(LogEventDto.from(event));
            }
        }
        return out;
    }

    public long activeCount() {
        return detect(20).size();
    }

    private List<LogEvent> currentEvents() {
        return datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"))
                .events();
    }
}