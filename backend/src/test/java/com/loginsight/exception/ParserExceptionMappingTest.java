package com.loginsight.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.loginsight.parser.LogParseException;
import com.loginsight.parser.ParserException;
import com.loginsight.parser.UnsupportedLogFormatException;

/**
 * {@code ParserException} and its subclasses are client-input failures, so they resolve to the same
 * 400 envelope as the other validation exceptions instead of falling through to the generic 500.
 *
 * <p>The messages surfaced are the parser's own, user-authored descriptions of the rejected content
 * — never a stack trace and never an internal detail.</p>
 */
class ParserExceptionMappingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private final MockHttpServletRequest request = request("/api/datasets");

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
    void parserExceptionIsBadRequestWithItsOwnMessage() {
        ResponseEntity<Map<String, Object>> response = handler.badRequest(
                new ParserException("input stream is empty"), request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, body(response).get("status"));
        assertEquals("ParserException", body(response).get("error"));
        assertEquals("input stream is empty", body(response).get("message"));
        assertEquals("/api/datasets", body(response).get("path"));
        assertNotNull(body(response).get("timestamp"));
    }

    @Test
    void parserExceptionSubclassesKeepTheirOwnTypeAndMessage() {
        ResponseEntity<Map<String, Object>> unsupported = handler.badRequest(
                new UnsupportedLogFormatException("cannot detect log format"), request);
        assertEquals(HttpStatus.BAD_REQUEST, unsupported.getStatusCode());
        assertEquals("UnsupportedLogFormatException", body(unsupported).get("error"));
        assertEquals("cannot detect log format", body(unsupported).get("message"));

        ResponseEntity<Map<String, Object>> parse = handler.badRequest(
                new LogParseException("Failed reading text log stream"), request);
        assertEquals(HttpStatus.BAD_REQUEST, parse.getStatusCode());
        assertEquals("LogParseException", body(parse).get("error"));
        assertEquals("Failed reading text log stream", body(parse).get("message"));
    }

    @Test
    void parserExceptionMappingDoesNotLeakTheCause() {
        ParserException withCause = new ParserException("Failed reading ingested logs",
                new IllegalStateException("jdbc://user:pass@host/db blew up"));
        String body = String.valueOf(handler.badRequest(withCause, request).getBody());
        assertFalse(body.contains("jdbc://"));
        assertFalse(body.contains("\\tat "));
    }
}