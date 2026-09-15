package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/dp/tsp} (docs/12 §4). {@code costs[i][j]} is the travel cost between
 * cities; {@code start} defaults to city 0.
 */
public record TspRequest(long[][] costs, Integer start) {

    public TspRequest {
        start = start == null ? 0 : start;
    }
}