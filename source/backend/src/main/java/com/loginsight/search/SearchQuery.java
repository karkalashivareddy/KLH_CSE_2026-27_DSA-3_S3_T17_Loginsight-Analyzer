package com.loginsight.search;

import java.time.Instant;
import java.util.List;

/**
 * A parsed LogInsight investigation query (docs/API.md §3).
 *
 * <p>Free text is a phrase searched in the event's searchable text via the DSA engine (KMP by
 * default). All other fields are filters resolved through the in-memory {@code LogIndex} and are
 * optional ({@code null} means "no filter"). The query parser accepts the documented syntax:</p>
 *
 * <pre>
 * level:ERROR service:auth-service status:500 message:"connection refused"
 * </pre>
 */
public record SearchQuery(String freeText, List<String> levels, String service, String host,
                          String source, String status, String traceId, String requestId,
                          Instant from, Instant to, String sort) {

    public boolean hasFilters() {
        return !levels.isEmpty() || service != null || host != null || source != null
                || status != null || traceId != null || requestId != null
                || from != null || to != null;
    }
}