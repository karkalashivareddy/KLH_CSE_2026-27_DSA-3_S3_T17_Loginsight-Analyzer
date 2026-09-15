package com.loginsight.exception;

/**
 * Raised when an algorithm engine fails during execution after validation passed. Maps to HTTP 500
 * with a safe, non-stack-trace message via {@code GlobalExceptionHandler} (docs/02 §10).
 */
public class AlgorithmExecutionException extends RuntimeException {

    public AlgorithmExecutionException(String message) {
        super(message);
    }

    public AlgorithmExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}