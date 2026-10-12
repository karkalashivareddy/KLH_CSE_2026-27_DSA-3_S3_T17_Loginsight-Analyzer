package com.loginsight.dto.request;

/**
 * Request body for an operator-driven incident lifecycle action on a live simulation
 * ({@code POST /api/simulation/incidents/{id}/transition}).
 *
 * <p>Either name a target {@code status} or set {@code advance: true} to move the incident one step
 * along the lifecycle. Transitions only ever move forward; a resolved incident is terminal.</p>
 *
 * @param sessionId the session that produced the incident
 * @param status    explicit target state, or {@code null} to use {@code advance}
 * @param advance   move one step instead of jumping to an explicit state
 */
public record SimulationTransitionRequest(String sessionId, String status, Boolean advance) {

    /** Whether the caller asked for a single-step advance. */
    public boolean isAdvance() {
        return Boolean.TRUE.equals(advance);
    }
}
