package com.loginsight.dto.response;

import java.time.Instant;

/**
 * Wire summary of a recorded run (docs/REBUILD_BASELINE Phase-4). No steps are shipped here - the
 * trace is replayed over the SSE event stream on demand.
 */
public record RunSummaryDto(String runId, String algorithm, String algorithmName, String category,
                            String status, Instant createdAt, Instant completedAt, int stepCount,
                            long executionTimeNanos, boolean truncated, String timeComplexity,
                            String spaceComplexity) {
}