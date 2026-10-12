package com.loginsight.dto.response;

import java.util.List;

/**
 * Response of the product search endpoints (docs/API.md §3).
 *
 * <p>The {@code strategy}/{@code algorithm}/{@code pattern}/{@code patternLength}/{@code textSize}
 * fields describe how the free-text pattern was executed by the DSA engine, and
 * {@code durationNanos} is measured on the actual run. {@code suggestion} carries an algorithmic
 * "did you mean" when a search produced no matches.</p>
 */
public record LogSearchResponse(String query, String strategy, String algorithm, String pattern,
                                int patternLength, long textSize, long durationNanos, long total,
                                int page, int size, String sort, String dataset,
                                List<SearchHit> matches, FuzzySuggestion suggestion) {

    public record SearchHit(LogEventDto event, String snippet, int matchCount) {
    }

    public record FuzzySuggestion(String suggestion, int similarityPct, long matchCount,
                                  long distance, String algorithm) {
    }
}