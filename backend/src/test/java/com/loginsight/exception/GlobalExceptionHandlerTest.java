package com.loginsight.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.format.DateTimeParseException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Error-mapping contract (docs/12 §12): client mistakes resolve to safe 4xx JSON in the documented
 * shape, and no handler ever hands a stack trace to the caller.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = request("/api/search");

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
        req.setRequestURI(uri);
        return req;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        return body;
    }

    @Test
    void malformedBodyIsBadRequestWithoutLeakingTheParserMessage() {
        HttpMessageNotReadableException cause =
                new HttpMessageNotReadableException("Unexpected token at position 42: SECRET",
                        (org.springframework.http.HttpInputMessage) null);
        ResponseEntity<Map<String, Object>> response = handler.unreadableBody(cause, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, body(response).get("status"));
        assertEquals("HttpMessageNotReadableException", body(response).get("error"));
        assertFalse(String.valueOf(body(response).get("message")).contains("SECRET"));
        assertEquals("/api/search", body(response).get("path"));
    }

    @Test
    void missingParameterIsBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.missingRequestPart(
                new MissingServletRequestParameterException("q", "String"), request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(String.valueOf(body(response).get("message")).contains("q"));
    }

    @Test
    void typeMismatchIsBadRequestAndNamesTheParameter() {
        MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException("abc",
                int.class, "page", null, null);
        ResponseEntity<Map<String, Object>> response = handler.typeMismatch(e, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(String.valueOf(body(response).get("message")).contains("page"));
        assertTrue(String.valueOf(body(response).get("message")).contains("int"));
    }

    @Test
    void unparsableDateIsBadRequest() {
        ResponseEntity<Map<String, Object>> response =
                handler.invalidDate(new DateTimeParseException("nope", "13-09-2026", 0), request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(String.valueOf(body(response).get("message")).contains("13-09-2026"));
    }

    @Test
    void indexFailuresAreBadRequestNotServerErrors() {
        ResponseEntity<Map<String, Object>> response =
                handler.outOfBounds(new ArrayIndexOutOfBoundsException("Index 99 out of bounds"), request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("IndexOutOfBoundsException", body(response).get("error"));
        assertFalse(String.valueOf(body(response).get("message")).contains("99"));
    }

    @Test
    void unknownRouteIsNotFound() {
        assertEquals(HttpStatus.NOT_FOUND,
                handler.unknownResource(
                        new NoHandlerFoundException("POST", "/api/nope", null), request)
                        .getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                handler.unknownResource(new NoResourceFoundException(
                        org.springframework.http.HttpMethod.GET, "/static/nope.png"), request)
                        .getStatusCode());
    }

    @Test
    void unsupportedMethodIsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException e = new HttpRequestMethodNotSupportedException("GET");
        ResponseEntity<Map<String, Object>> response = handler.methodNotAllowed(e, request);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(405, body(response).get("status"));
    }

    @Test
    void domainValidationKeepsItsStatusAndType() {
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.badRequest(new InvalidQueryException("pattern must not be blank"), request)
                        .getStatusCode());
        ResponseEntity<Map<String, Object>> notFound =
                handler.notFound(new DatasetException("No dataset loaded"), request);
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
        assertEquals("DatasetException", body(notFound).get("error"));
    }

    @Test
    void unexpectedFailuresStayGenericAndStackFree() {
        ResponseEntity<Map<String, Object>> response = handler.unexpected(
                new IllegalStateException("jdbc://user:pass@host/db blew up"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected internal error occurred", body(response).get("message"));
        assertFalse(String.valueOf(body(response)).contains("jdbc://"));
        assertNotNull(body(response).get("timestamp"));
    }
}
