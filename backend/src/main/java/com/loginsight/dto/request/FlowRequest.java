package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/flow} (docs/12 §3). {@code nodes} lists all vertices; {@code edges}
 * carry capacities; {@code source}/{@code sink} name the terminals. Referencing an undeclared node
 * or {@code source == sink} fails validation before the engine runs.
 */
public record FlowRequest(String source, String sink, String[] nodes, GraphEdge[] edges) {
}