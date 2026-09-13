package com.loginsight.model;

/**
 * HTTP method observed in a log event.
 */
public enum HttpMethod {

    GET, POST, PUT, DELETE, PATCH;

    /**
     * Parse a raw string into an {@code HttpMethod}, tolerating mixed-case input.
     *
     * @throws IllegalArgumentException if the value does not match any constant
     */
    public static HttpMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be null or blank");
        }
        return valueOf(value.trim().toUpperCase());
    }
}
