package com.loginsight.model;

/**
 * Severity level of a log event, normalised to uppercase.
 */
public enum LogLevel {

    TRACE, DEBUG, INFO, WARN, ERROR, FATAL;

    /**
     * Parse a raw string into a {@code LogLevel}, tolerating mixed-case input.
     *
     * @throws IllegalArgumentException if the value does not match any constant
     */
    public static LogLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Log level must not be null or blank");
        }
        return valueOf(value.trim().toUpperCase());
    }
}
