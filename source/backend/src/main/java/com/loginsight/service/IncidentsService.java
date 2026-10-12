package com.loginsight.service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.loginsight.dto.response.IncidentDto;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.exception.DatasetException;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.incident.IncidentDetector;
import com.loginsight.model.LogEvent;

/**
 * Incident read model (docs/API.md §6). Incidents are re-derived from the loaded dataset on every
 * request through the heuristic {@link IncidentDetector}; the dashboard count, the list and the
 * detail view always show the same evidence, never a cached guess.
 *
 * <p>List, count, detail and evidence all resolve the same capped detection set
 * ({@link #MAX_INCIDENTS}), so an incident beyond the twentieth is still reachable by id and never
 * disagrees with what the list reports.</p>
 */
@Service
public class IncidentsService {

    /** Upper bound on the incidents one detection pass returns; shared by list/count/detail. */
    public static final int MAX_INCIDENTS = 200;
    /** Upper bound on the supporting log rows one evidence request may return. */
    public static final int MAX_EVIDENCE_LOGS = 1_000;

    private final DatasetService datasetService;
    private final IncidentDetector incidentDetector = new IncidentDetector();

    public IncidentsService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    public List<IncidentDto> detect(int limit) {
        if (limit < 1) {
            throw new InvalidQueryException("limit must be >= 1, got " + limit);
        }
        return incidentDetector.detect(currentEvents(), Math.min(limit, MAX_INCIDENTS));
    }

    public IncidentDto.IncidentDetail detail(long id, int logLimit) {
        IncidentDto incident = find(id);
        List<LogEventDto> events = eventsInWindow(incident.start(), incident.end(),
                boundedEvidence(logLimit), 0);
        return new IncidentDto.IncidentDetail(incident, events, events.size());
    }

    /** The incident with the given id from the same detection set the list and count report. */
    public IncidentDto find(long id) {
        for (IncidentDto incident : incidentDetector.detect(currentEvents(), MAX_INCIDENTS)) {
            if (incident.id() == id) {
                return incident;
            }
        }
        throw new DatasetException("Incident not found in the current dataset: " + id);
    }

    public List<LogEventDto> eventsInWindow(String fromIso, String toIso, int limit, int offset) {
        if (limit < 1) {
            throw new InvalidQueryException("limit must be >= 1, got " + limit);
        }
        if (offset < 0) {
            throw new InvalidQueryException("offset must be >= 0, got " + offset);
        }
        Instant from = parse(fromIso, "from");
        Instant to = parse(toIso, "to");
        if (from.isAfter(to)) {
            throw new InvalidQueryException("from must be earlier than or equal to 'to'");
        }
        int cap = boundedEvidence(limit);
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
                if (out.size() >= cap) {
                    break;
                }
                out.add(LogEventDto.from(event));
            }
        }
        return out;
    }

    public long activeCount() {
        return incidentDetector.detect(currentEvents(), MAX_INCIDENTS).size();
    }

    private static int boundedEvidence(int limit) {
        return Math.min(limit, MAX_EVIDENCE_LOGS);
    }

    private static Instant parse(String iso, String field) {
        if (iso == null || iso.isBlank()) {
            throw new InvalidQueryException(field + " must be an ISO-8601 instant");
        }
        try {
            return Instant.parse(iso);
        } catch (DateTimeParseException e) {
            throw new InvalidQueryException(field + " must be an ISO-8601 instant, got '" + iso + "'");
        }
    }

    private List<LogEvent> currentEvents() {
        return datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"))
                .events();
    }
}
