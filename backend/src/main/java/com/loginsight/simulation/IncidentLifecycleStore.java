package com.loginsight.simulation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Session-scoped store for simulation incidents and their lifecycle transitions.
 *
 * <p>Deliberately in-memory and deliberately labelled as such: the API returns
 * {@code persistence: "session"} so the UI can state that lifecycle state lives for the browser
 * session only and is not written to disk. This mirrors how the dataset layer already handles
 * unpersisted state, rather than implying a durable incident database that does not exist.</p>
 *
 * <p>Bounded by {@code maxIncidents}: once full, the oldest <em>resolved</em> incident is evicted. If
 * every incident is still open, nothing is dropped and the store simply stops accepting new ones,
 * which is visible to the client as a full store rather than as silent data loss.</p>
 */
public final class IncidentLifecycleStore {

    /** Upper bound on retained incidents per session. */
    public static final int MAX_INCIDENTS = 50;

    private final Map<Integer, SimulationIncident> incidents = new LinkedHashMap<>();
    private int nextId = 1;

    /**
     * Registers a newly detected incident.
     *
     * @param incident    the candidate produced by the detector
     * @param scenarioId  the scenario the candidate came from
     * @return the registered incident, or {@code null} when the store is full of open incidents
     */
    public synchronized SimulationIncident open(SimulationIncident incident, String scenarioId) {
        if (incidents.size() >= MAX_INCIDENTS) {
            Optional<Integer> evictable = incidents.entrySet().stream()
                    .filter(entry -> !entry.getValue().isOpen())
                    .map(Map.Entry::getKey)
                    .findFirst();
            if (evictable.isEmpty()) {
                return null;
            }
            incidents.remove(evictable.get());
        }
        incident.assignId(nextId++, scenarioId);
        incidents.put(incident.getId(), incident);
        return incident;
    }

    public synchronized Optional<SimulationIncident> find(int id) {
        return Optional.ofNullable(incidents.get(id));
    }

    /** All incidents, oldest first. */
    public synchronized List<SimulationIncident> all() {
        return List.copyOf(incidents.values());
    }

    public synchronized List<SimulationIncident> openIncidents() {
        return incidents.values().stream().filter(SimulationIncident::isOpen).toList();
    }

    public synchronized int size() {
        return incidents.size();
    }

    /** Applies an explicit operator-driven transition; returns the incident either way. */
    public synchronized Optional<SimulationIncident> transition(int id, SimulationIncident.Status target, Instant at) {
        SimulationIncident incident = incidents.get(id);
        if (incident == null) {
            return Optional.empty();
        }
        incident.transitionTo(target, at, false);
        return Optional.of(incident);
    }

    /**
     * Advances an incident by exactly one lifecycle step.
     *
     * @return {@code true} when the incident advanced
     */
    public synchronized boolean advance(int id, Instant at, boolean automatic) {
        SimulationIncident incident = incidents.get(id);
        return incident != null && incident.advance(at, automatic);
    }

    /**
     * Reconciles every open incident with the requested state, e.g. moving a window to
     * {@code MITIGATED} once the signal decays.
     *
     * @return the number of incidents that changed
     */
    public synchronized int reconcile(SimulationIncident.Status target, Instant at, boolean automatic) {
        int applied = 0;
        for (SimulationIncident incident : new ArrayList<>(incidents.values())) {
            if (incident.isOpen() && incident.getStatus().ordinal() < target.ordinal()
                    && incident.transitionTo(target, at, automatic)) {
                applied++;
            }
        }
        return applied;
    }

    public synchronized void clear() {
        incidents.clear();
        nextId = 1;
    }
}
