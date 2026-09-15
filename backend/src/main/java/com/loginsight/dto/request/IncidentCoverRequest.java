package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/approx/incident-cover} (docs/12 §7). Models incident-to-service
 * relationships: {@code relationships} is a list of {@code {a,b}} pairs over the {@code services}
 * graph; the engine reports a candidate vertex cover on that graph so on-call teams see the
 * smallest set of services that touches every incident edge.
 */
public record IncidentCoverRequest(String[] services, String[][] relationships) {
}