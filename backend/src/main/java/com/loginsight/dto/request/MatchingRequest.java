package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/flow/matching} (docs/12 §3). {@code incidents}/{@code resources} name
 * the two partitions; {@code edges} is a list of {@code {from,to}} pairs. Returns the maximum
 * bipartite matching (size + assigned pairs).
 */
public record MatchingRequest(String[] incidents, String[] resources, String[][] edges) {
}