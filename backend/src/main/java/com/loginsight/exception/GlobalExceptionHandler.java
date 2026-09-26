package com.loginsight.exception;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Maps every failure to the documented clean JSON error shape (docs/02 §10, docs/12): status, error
 * type, safe message, timestamp and request path. Stack traces are never exposed to clients; domain
 * and engine failures are logged server-side only.
 *
 * <p>Client mistakes (malformed JSON, missing/mistyped query parameters, unparsable dates, index and
 * bounds failures) resolve to the matching 4xx instead of falling through to the generic 500, so an
 * invalid request never looks like a server fault and never carries internals.</p>
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

    /** Unreadable/absent request body: HTTP 400 with a fixed, leak-free message. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadableBody(HttpMessageNotReadableException e,
                                                               HttpServletRequest request) {
        LOG.debug("Unreadable body for {}: {}", request.getRequestURI(), e.getMessage());
        return body(HttpStatus.BAD_REQUEST, "HttpMessageNotReadableException",
                "Request body is missing or is not valid JSON for this endpoint", request);
    }

    /** Bean-validation failures on a request body: HTTP 400. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> invalidBinding(BindException e,
                                                              HttpServletRequest request) {
        String fields = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        String message = fields.isBlank() ? "Request validation failed" : "Request validation failed: " + fields;
        return body(HttpStatus.BAD_REQUEST, e.getClass().getSimpleName(), message, request);
    }

    /** Missing required query/path parameter or request header: HTTP 400. */
    @ExceptionHandler({MissingServletRequestParameterException.class,
            MissingPathVariableException.class, MissingRequestHeaderException.class})
    public ResponseEntity<Map<String, Object>> missingRequestPart(Exception e,
                                                                   HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, e.getClass().getSimpleName(), e.getMessage(), request);
    }

    /** A request parameter could not be converted to the declared type: HTTP 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeMismatch(MethodArgumentTypeMismatchException e,
                                                            HttpServletRequest request) {
        String required = e.getRequiredType() == null ? "the expected type"
                : e.getRequiredType().getSimpleName();
        return body(HttpStatus.BAD_REQUEST, "MethodArgumentTypeMismatchException",
                "Parameter '" + e.getName() + "' must be a valid " + required, request);
    }

    /** Unparsable date/time literal (Instant, LocalDate, …): HTTP 400. */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, Object>> invalidDate(DateTimeParseException e,
                                                           HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, "DateTimeParseException",
                "Invalid date/time value: " + truncate(e.getParsedString()), request);
    }

    /** Index/bounds failures from validated inputs: HTTP 400, never an internal error. */
    @ExceptionHandler(IndexOutOfBoundsException.class)
    public ResponseEntity<Map<String, Object>> outOfBounds(IndexOutOfBoundsException e,
                                                           HttpServletRequest request) {
        LOG.debug("Bounds failure for {}: {}", request.getRequestURI(), e.getMessage());
        return body(HttpStatus.BAD_REQUEST, "IndexOutOfBoundsException",
                "The request selects a position outside the valid range", request);
    }

    /** Unknown endpoint or unknown static resource: HTTP 404. */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> unknownResource(Exception e,
                                                               HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, e.getClass().getSimpleName(),
                "No endpoint or resource matches " + request.getMethod() + " " + request.getRequestURI(),
                request);
    }

    /** Wrong verb on an existing path: HTTP 405 with the supported verbs listed. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> methodNotAllowed(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String allowed = e.getSupportedHttpMethods() == null ? ""
                : " (supported: " + e.getSupportedHttpMethods() + ")";
        return body(HttpStatus.METHOD_NOT_ALLOWED, "HttpRequestMethodNotSupportedException",
                e.getMethod() + " is not supported on this endpoint" + allowed, request);
    }

    /** Unsupported/absent request or response content type: HTTP 415 / HTTP 406. */
    @ExceptionHandler({HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class})
    public ResponseEntity<Map<String, Object>> badMediaType(Exception e,
                                                             HttpServletRequest request) {
        HttpStatus status = e instanceof HttpMediaTypeNotAcceptableException
                ? HttpStatus.NOT_ACCEPTABLE : HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        return body(status, e.getClass().getSimpleName(),
                "Unsupported content type for this endpoint", request);
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

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 80 ? value : value.substring(0, 80) + "…";
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
