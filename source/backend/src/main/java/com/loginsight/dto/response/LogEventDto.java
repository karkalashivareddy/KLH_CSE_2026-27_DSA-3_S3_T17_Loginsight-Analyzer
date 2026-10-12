package com.loginsight.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;
import com.loginsight.model.HttpMethod;

/**
 * Serializable view of a {@link LogEvent} for the log explorer endpoints (docs/API.md). Every field
 * is a wire-safe primitive or enum name; no internal model object is serialised directly. Optional
 * observability fields ({@code traceId}, {@code spanId}, {@code url}, {@code source},
 * {@code rawMessage}, {@code attributes}) are included as-is. {@link #severityNumber} is derived
 * from the normalized level via {@link LogEvent#severityNumber(LogLevel)}.
 */
public record LogEventDto(long id, Instant timestamp, int severityNumber, String level,
                          String service, String host, String ipAddress, String httpMethod,
                          String endpoint, int statusCode, long responseTime, String requestId,
                          String userId, String message, String traceId, String spanId, String url,
                          String source, String rawMessage, Map<String, String> attributes) {

    public static LogEventDto from(LogEvent event) {
        LogLevel levelValue = event.getLevel();
        HttpMethod methodValue = event.getHttpMethod();
        Map<String, String> attributes = event.getAttributes() == null
                ? Map.of() : new TreeMap<>(event.getAttributes());
        return new LogEventDto(event.getId(), event.getTimestamp(),
                event.getSeverityNumber(),
                levelValue == null ? null : levelValue.name(),
                event.getService(), event.getHost(), event.getIpAddress(),
                methodValue == null ? null : methodValue.name(),
                event.getEndpoint(), event.getStatusCode(), event.getResponseTime(),
                event.getRequestId(), event.getUserId(), event.getMessage(),
                event.getTraceId(), event.getSpanId(), event.getUrl(), event.getSource(),
                event.getRawMessage(), attributes);
    }
}