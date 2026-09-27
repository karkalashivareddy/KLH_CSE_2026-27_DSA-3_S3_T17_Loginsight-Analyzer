package com.loginsight.dto.response;

import java.time.Instant;
import java.util.List;

import com.loginsight.simulation.EvidenceLink;
import com.loginsight.simulation.SimulationDetector;
import com.loginsight.simulation.SimulationIncident;

/**
 * Wire shape for a simulation incident.
 *
 * <p>{@code method} records <em>how</em> the window was opened — always a heuristic detector plus
 * measured evidence, never a root-cause claim. {@code persistence} is {@code session} because the
 * lifecycle store is in-memory and resets with the browser session.</p>
 */
public record SimulationIncidentDto(
        int id,
        String scenarioId,
        String title,
        String signal,
        String method,
        String severity,
        String status,
        boolean open,
        String originService,
        List<String> affectedServices,
        List<String> blastRadius,
        Instant detectedAt,
        Instant start,
        Instant end,
        Instant lastUpdatedAt,
        int eventCount,
        double errorRate,
        long p95LatencyMs,
        List<String> matchedSignatures,
        List<EvidenceDto> evidence,
        List<TimelineEntryDto> timeline,
        String persistence) {

    /** One lifecycle transition. */
    public record TimelineEntryDto(Instant at, String status, String label) {
    }

    /** One measured algorithm invocation supporting the incident. */
    public record EvidenceDto(
            String algorithm,
            String purpose,
            int inputSize,
            String inputUnit,
            String result,
            long runtimeNanos,
            double runtimeMicros,
            String complexity,
            List<String> references) {
    }

    public static SimulationIncidentDto from(SimulationIncident incident, String severity) {
        return new SimulationIncidentDto(
                incident.getId(),
                incident.getScenarioId(),
                incident.getTitle(),
                incident.getSignal(),
                incident.getMethod(),
                severity,
                incident.getStatus().name(),
                incident.isOpen(),
                incident.getOriginService(),
                incident.getAffectedServices(),
                incident.getDependentServices(),
                incident.getDetectedAt(),
                incident.getStart(),
                incident.getEnd(),
                incident.getLastUpdatedAt(),
                incident.getEventCount(),
                Math.round(incident.getErrorRate() * 100) / 100.0,
                incident.getP95LatencyMs(),
                incident.getMatchedSignatures(),
                incident.getEvidence().stream().map(SimulationIncidentDto::evidence).toList(),
                incident.getTimeline().stream()
                        .map(entry -> new TimelineEntryDto(entry.at(), entry.status().name(), entry.label()))
                        .toList(),
                "session");
    }

    private static EvidenceDto evidence(EvidenceLink link) {
        return new EvidenceDto(
                link.algorithm(),
                link.purpose(),
                link.inputSize(),
                link.inputUnit(),
                link.result(),
                link.runtimeNanos(),
                Math.round(link.runtimeNanos() / 100.0) / 10.0,
                link.complexity(),
                link.references());
    }

    /** Signal wire shape, always carrying the measurement that produced it. */
    public record SignalDto(String id, String kind, String label, String detail, String severity, String service) {
        public static SignalDto from(SimulationDetector.Signal signal) {
            return new SignalDto(signal.id(), signal.kind(), signal.label(), signal.detail(),
                    signal.severity(), signal.service());
        }
    }
}
