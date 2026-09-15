package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/fuzzy/search} — approximate match against a log line by edit distance
 * (docs/12 §1). {@code maxDistance} bounds the per-line edit distance; the engine returns the
 * matching lines that fall within it together with their distances.
 */
public record FuzzyRequest(String text, String query, Integer maxDistance) {

    public FuzzyRequest {
        maxDistance = maxDistance == null ? 2 : maxDistance;
    }
}