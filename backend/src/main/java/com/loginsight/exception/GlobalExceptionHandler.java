package com.loginsight.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Maps every failure to the documented clean JSON error shape (docs/02 §10, docs/12): status, error
 * type, safe message, timestamp and request path. Stack traces are never exposed to clients; domain
 * and engine failures are logged server-side only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Validation failures: HTTP 400. */
    @ExceptionHandler({InvalidQueryException.class, InvalidLogException.class,
            IllegalArgumentException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<Map<String, Object>> badRequest(Exception e, HttpServletRequest request) {
        String type = e.getClass().getSimpleName();
        String message = e instanceof MaxUploadSizeExceededException
                ? "Uploaded file exceeds the configured size limit"
                : e.getMessage();
        return body(HttpStatus.BAD_REQUEST, type, message, request);
    }

    /** Dataset/sample not present: HTTP 404 (docs/12 status table). */
    @ExceptionHandler(DatasetException.class)
    public ResponseEntity<Map<String, Object>> notFound(DatasetException e,
                                                        HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, "DatasetException", e.getMessage(), request);
    }

    /** Engine failure after validation: HTTP 500, safe message. */
    @ExceptionHandler(AlgorithmExecutionException.class)
    public ResponseEntity<Map<String, Object>> executionFailure(AlgorithmExecutionException e,
                                                                HttpServletRequest request) {
        LOG.warn("Algorithm execution failed for {}: {}", request.getRequestURI(), e.getMessage());
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "AlgorithmExecutionException",
                "The algorithm failed to complete: " + e.getMessage(), request);
    }

    /** Anything else: HTTP 500, generic message, full stack logged. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e, HttpServletRequest request) {
        LOG.error("Unexpected error for {}", request.getRequestURI(), e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, e.getClass().getSimpleName(),
                "An unexpected internal error occurred", request);
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String error,
                                                            String message,
                                                            HttpServletRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status.value());
        payload.put("error", error);
        payload.put("message", message);
        payload.put("timestamp", Instant.now().toString());
        payload.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(payload);
    }
}