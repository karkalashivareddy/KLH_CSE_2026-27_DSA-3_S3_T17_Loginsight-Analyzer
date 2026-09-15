package com.loginsight.dto.response;

import java.time.Instant;

import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;
import com.loginsight.model.HttpMethod;

/**
 * Serializable view of a {@link LogEvent} for the log explorer endpoints (docs/12 §2). Every field
 * is a wire-safe primitive or enum name; no internal model object is serialised directly.
 */
public record LogEventDto(long id, Instant timestamp, String level, String service, String host,
                          String ipAddress, String httpMethod, String endpoint, int statusCode,
                          long responseTime, String requestId, String userId, String message) {

    public static LogEventDto from(LogEvent event) {
        LogLevel levelValue = event.getLevel();
        HttpMethod methodValue = event.getHttpMethod();
        return new LogEventDto(event.getId(), event.getTimestamp(),
                levelValue == null ? null : levelValue.name(),
                event.getService(), event.getHost(), event.getIpAddress(),
                methodValue == null ? null : methodValue.name(),
                event.getEndpoint(), event.getStatusCode(), event.getResponseTime(),
                event.getRequestId(), event.getUserId(), event.getMessage());
    }
}