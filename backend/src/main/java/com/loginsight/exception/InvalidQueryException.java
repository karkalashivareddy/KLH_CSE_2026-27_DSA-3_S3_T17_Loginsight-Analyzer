package com.loginsight.exception;

/**
 * Raised when a request fails validation in the query layer (docs/02 §10). Maps to HTTP 400 with a
 * clean JSON error body via {@code GlobalExceptionHandler}; never a stack trace.
 */
public class InvalidQueryException extends RuntimeException {

    public InvalidQueryException(String message) {
        super(message);
    }

    public InvalidQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}