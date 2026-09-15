package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/approx/vertex-cover} (docs/12 §7). {@code edges} is a list of undirected
 * {@code {a,b}} pairs over {@code nodes}. The engine returns the greedy 2-approximation together
 * with its theoretical gap notes.
 */
public record VertexCoverRequest(String[] nodes, String[][] edges) {
}