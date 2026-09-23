package com.loginsight.run;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * One recorded algorithm run (docs/REBUILD_BASELINE Phase-4). A run always belongs to a
 * trace-instrumented algorithm; {@code steps} are the genuinely recorded steps of the execution
 * (bounded by the recorder ceiling) and {@code truncated} is set by the recorder, never guessed.
 *
 * @param status QUEUED / RUNNING / COMPLETED / FAILED
 */
public record RunRecord(
        String runId,
        String algorithm,
        String algorithmName,
        String category,
        String status,
        Instant createdAt,
        Instant completedAt,
        int stepCount,
        long executionTimeNanos,
        boolean truncated,
        String timeComplexity,
        String spaceComplexity,
        Map<String, Object> input,
        Object result,
        List<Map<String, Object>> steps,
        String error) {
}