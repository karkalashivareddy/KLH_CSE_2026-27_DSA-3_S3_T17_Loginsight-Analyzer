package com.loginsight.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Normalized, immutable representation of a single log record.
 *
 * <p>{@code id} is assigned by the dataset layer when an event is ingested; before that it is
 * {@code -1} and is excluded from {@link #equals} / {@link #hashCode}. All remaining fields map
 * directly to the canonical text and JSON Lines formats defined in {@code sample-data/README.md}.</p>
 */
public final class LogEvent {

    private final long id;
    private final Instant timestamp;
    private final LogLevel level;
    private final String service;
    private final String host;
    private final String ipAddress;
    private final HttpMethod httpMethod;
    private final String endpoint;
    private final int statusCode;
    private final long responseTime;
    private final String requestId;
    private final String userId;
    private final String message;

    private LogEvent(Builder builder) {
        this.id = builder.id;
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp is required");
        this.level = builder.level;
        this.service = builder.service;
        this.host = builder.host;
        this.ipAddress = builder.ipAddress;
        this.httpMethod = builder.httpMethod;
        this.endpoint = builder.endpoint;
        this.statusCode = builder.statusCode;
        this.responseTime = builder.responseTime;
        this.requestId = builder.requestId;
        this.userId = builder.userId;
        this.message = Objects.requireNonNull(builder.message, "message is required");
        if (this.message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        if (this.statusCode != 0 && (this.statusCode < 100 || this.statusCode > 599)) {
            throw new IllegalArgumentException(
                    "statusCode must be 0 (unrecorded) or within [100, 599]: " + this.statusCode);
        }
        if (this.responseTime < 0) {
            throw new IllegalArgumentException("responseTime must not be negative: " + this.responseTime);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Copy of this event carrying the given dataset-assigned id.
     */
    public LogEvent withId(long newId) {
        return LogEvent.builder()
                .id(newId)
                .timestamp(timestamp)
                .level(level)
                .service(service)
                .host(host)
                .ipAddress(ipAddress)
                .httpMethod(httpMethod)
                .endpoint(endpoint)
                .statusCode(statusCode)
                .responseTime(responseTime)
                .requestId(requestId)
                .userId(userId)
                .message(message)
                .build();
    }

    public long getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getService() {
        return service;
    }

    public String getHost() {
        return host;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LogEvent other)) {
            return false;
        }
        return Objects.equals(timestamp, other.timestamp)
                && level == other.level
                && Objects.equals(service, other.service)
                && Objects.equals(host, other.host)
                && Objects.equals(ipAddress, other.ipAddress)
                && httpMethod == other.httpMethod
                && Objects.equals(endpoint, other.endpoint)
                && statusCode == other.statusCode
                && responseTime == other.responseTime
                && Objects.equals(requestId, other.requestId)
                && Objects.equals(userId, other.userId)
                && Objects.equals(message, other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, level, service, host, ipAddress, httpMethod, endpoint,
                statusCode, responseTime, requestId, userId, message);
    }

    @Override
    public String toString() {
        return "LogEvent{id=" + id + ", timestamp=" + timestamp + ", level=" + level
                + ", service=" + service + ", message=" + message + '}';
    }

    public static final class Builder {

        private long id = -1;
        private Instant timestamp;
        private LogLevel level;
        private String service;
        private String host;
        private String ipAddress;
        private HttpMethod httpMethod;
        private String endpoint;
        private int statusCode;
        private long responseTime;
        private String requestId;
        private String userId;
        private String message;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder level(LogLevel level) {
            this.level = level;
            return this;
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder httpMethod(HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder responseTime(long responseTime) {
            this.responseTime = responseTime;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public LogEvent build() {
            return new LogEvent(this);
        }
    }
}