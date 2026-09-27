package com.loginsight.simulation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A detected simulation incident, with a lifecycle the UI can animate and an operator can advance.
 *
 * <p>Detection is heuristic and says so. The incident records the signals that fired, the services
 * named by the detector, the evidence produced by real algorithm invocations, and a timestamped
 * timeline of lifecycle transitions. It makes no root-cause claim: the {@code originService} field
 * records where the signal was <em>observed</em> to start, not a proven cause.</p>
 */
public final class SimulationIncident {

    /** Lifecycle states, in forward order. */
    public enum Status {
        DETECTED, INVESTIGATING, ACKNOWLEDGED, MITIGATED, RESOLVED;

        /** The next legal state, or {@code null} when terminal. */
        public Status next() {
            int index = ordinal();
            return index + 1 < values().length ? values()[index + 1] : null;
        }

        public static Status parse(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim().toUpperCase();
            for (Status status : values()) {
                if (status.name().equals(normalized)) {
                    return status;
                }
            }
            return null;
        }
    }

    /** One entry on the incident timeline. */
    public record TimelineEntry(Instant at, Status status, String label) {
    }

    private int id;
    private String scenarioId;
    private final String title;
    private final String signal;
    private final String method;
    private final String originService;
    private final List<String> affectedServices;
    private final List<String> dependentServices;
    private final Instant detectedAt;
    private final Instant start;

    private Instant end;
    private int eventCount;
    private double errorRate;
    private long p95LatencyMs;
    private List<String> matchedSignatures;
    private List<EvidenceLink> evidence;
    private Instant lastUpdatedAt;
    private Status status = Status.DETECTED;
    private final List<TimelineEntry> timeline = new ArrayList<>();

    public SimulationIncident(String title,
                              String signal,
                              String method,
                              String originService,
                              List<String> affectedServices,
                              List<String> dependentServices,
                              List<EvidenceLink> evidence,
                              Instant detectedAt,
                              int eventCount,
                              double errorRate,
                              long p95LatencyMs,
                              List<String> matchedSignatures) {
        this.scenarioId = null;
        this.title = title;
        this.signal = signal;
        this.method = method;
        this.originService = originService;
        this.affectedServices = List.copyOf(affectedServices);
        this.dependentServices = List.copyOf(dependentServices);
        this.evidence = List.copyOf(evidence);
        this.detectedAt = detectedAt;
        this.start = detectedAt;
        this.lastUpdatedAt = detectedAt;
        this.eventCount = eventCount;
        this.errorRate = errorRate;
        this.p95LatencyMs = p95LatencyMs;
        this.matchedSignatures = List.copyOf(matchedSignatures);
        this.timeline.add(new TimelineEntry(detectedAt, Status.DETECTED, "Heuristic detector opened this window"));
    }

    public int getId() {
        return id;
    }

    /** Assigned by the lifecycle store when the incident is registered. */
    public void assignId(int assignedId, String scenario) {
        this.id = assignedId;
        this.scenarioId = scenario;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getTitle() {
        return title;
    }

    public String getSignal() {
        return signal;
    }

    public String getMethod() {
        return method;
    }

    public String getOriginService() {
        return originService;
    }

    public List<String> getAffectedServices() {
        return affectedServices;
    }

    public List<String> getDependentServices() {
        return dependentServices;
    }

    public List<EvidenceLink> getEvidence() {
        return evidence;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }

    public int getEventCount() {
        return eventCount;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public long getP95LatencyMs() {
        return p95LatencyMs;
    }

    public List<String> getMatchedSignatures() {
        return matchedSignatures;
    }

    public Status getStatus() {
        return status;
    }

    public List<TimelineEntry> getTimeline() {
        return List.copyOf(timeline);
    }

    /** True while the incident is still open for investigation. */
    public boolean isOpen() {
        return status != Status.RESOLVED;
    }

    /** The most recent time the metrics or evidence were refreshed. */
    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    /**
     * Refreshes the live measurements attached to an open incident.
     *
     * <p>Keeps the figures the UI shows current instead of freezing the values that happened to be
     * true in the frame the incident opened. Detection facts — title, signal, origin, declared
     * services and {@code detectedAt} — are immutable.</p>
     */
    public synchronized void updateMetrics(int eventCount, double errorRate, long p95LatencyMs,
                                          List<String> matchedSignatures, List<EvidenceLink> evidence, Instant at) {
        this.eventCount = eventCount;
        this.errorRate = errorRate;
        this.p95LatencyMs = p95LatencyMs;
        this.matchedSignatures = matchedSignatures == null ? List.of() : List.copyOf(matchedSignatures);
        this.evidence = evidence == null ? List.of() : List.copyOf(evidence);
        this.lastUpdatedAt = at;
    }

    /**
     * Advances the lifecycle by exactly one state.
     *
     * @param at        when the transition happened
     * @param automatic {@code true} when the simulation drove the step rather than an operator
     * @return {@code true} when the transition was applied
     */
    public synchronized boolean advance(Instant at, boolean automatic) {
        Status next = status.next();
        if (next == null) {
            return false;
        }
        status = next;
        timeline.add(new TimelineEntry(at, next, labelFor(next, automatic)));
        if (next == Status.RESOLVED) {
            end = at;
        }
        return true;
    }

    /** Sets an explicit state, used by the operator action endpoint. */
    public synchronized boolean transitionTo(Status target, Instant at) {
        return transitionTo(target, at, false);
    }

    /** Sets an explicit state, distinguishing operator-driven from simulation-driven moves. */
    public synchronized boolean transitionTo(Status target, Instant at, boolean automatic) {
        if (target == null || target.ordinal() < status.ordinal() || status == Status.RESOLVED) {
            return false;
        }
        if (target == status) {
            return true;
        }
        status = target;
        timeline.add(new TimelineEntry(at, target, labelFor(target, automatic)));
        if (target == Status.RESOLVED) {
            end = at;
        }
        return true;
    }

    private static String labelFor(Status status, boolean automatic) {
        String actor = automatic ? "Simulation auto-advance" : "Operator";
        return switch (status) {
            case DETECTED -> "Heuristic detector opened this window";
            case INVESTIGATING -> actor + ": " + (automatic
                    ? "signal sustained across consecutive windows"
                    : "investigation started");
            case ACKNOWLEDGED -> actor + ": " + (automatic
                    ? "acknowledged for tracking"
                    : "signal acknowledged");
            case MITIGATED -> actor + ": " + (automatic
                    ? "failure intensity decayed, mitigation window opened"
                    : "mitigation applied");
            case RESOLVED -> actor + ": " + (automatic
                    ? "error rate returned to the observed baseline, window closed"
                    : "closed manually");
        };
    }
}
