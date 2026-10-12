package com.loginsight.dto.request;

/**
 * One edge of a flow or approximation request. {@code capacity} is the residual capacity; {@code
 * cost} only matters for min-cost flow and defaults to {@code 0}.
 */
public record GraphEdge(String from, String to, Long capacity, Long cost) {

    public GraphEdge {
        capacity = capacity == null ? 1L : capacity;
        cost = cost == null ? 0L : cost;
    }
}