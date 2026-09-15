package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/approx/set-cover} (docs/12 §7). {@code universe} lists every element that
 * must be covered; {@code sets} maps a set name to its elements. The engine returns the greedy
 * O(log n) cover.
 */
public record SetCoverRequest(String[] universe, java.util.Map<String, String[]> sets) {
}