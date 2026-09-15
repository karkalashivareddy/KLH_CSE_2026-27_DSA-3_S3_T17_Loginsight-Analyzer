package com.loginsight.dto.response;

import java.time.Instant;

/**
 * The documented error payload (docs/12): status, error type, safe message, timestamp and request
 * path. Produced by {@code GlobalExceptionHandler}; stack traces never reach the client.
 */
public record ErrorResponseDto(int status, String error, String message, Instant timestamp,
                               String path) {
}