package com.loginsight.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

class TextLogParserTest {

    private static final String VALID_LINE =
            "2026-09-13T10:00:01Z | ERROR | AUTH | 10.0.0.1 | POST | /login | 401 | 45"
                    + " | req-001 | user-101 | authentication failed";

    private final TextLogParser parser = new TextLogParser();

    private LogParseResult parse(String... lines) {
        String body = String.join("\n", lines);
        return parser.parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesValidLine() {
        LogParseResult result = parse(VALID_LINE);
        assertEquals(1, result.totalLines());
        assertEquals(1, result.successCount());
        assertEquals(0, result.failureCount());

        LogEvent event = result.successfulEvents().get(0);
        assertEquals(java.time.Instant.parse("2026-09-13T10:00:01Z"), event.getTimestamp());
        assertEquals(LogLevel.ERROR, event.getLevel());
        assertEquals("AUTH", event.getService());
        assertEquals("10.0.0.1", event.getIpAddress());
        assertEquals(HttpMethod.POST, event.getHttpMethod());
        assertEquals("/login", event.getEndpoint());
        assertEquals(401, event.getStatusCode());
        assertEquals(45, event.getResponseTime());
        assertEquals("req-001", event.getRequestId());
        assertEquals("user-101", event.getUserId());
        assertEquals("authentication failed", event.getMessage());
    }

    @Test
    void anonymousUserIdBecomesNull() {
        LogParseResult result = parse(VALID_LINE.replace("user-101", "-"));
        assertNull(result.successfulEvents().get(0).getUserId());
    }

    @Test
    void blankLinesSkipped() {
        LogParseResult result = parse("", "  ", VALID_LINE);
        assertEquals(1, result.totalLines(), "blank lines are not counted as records");
        assertEquals(1, result.successCount());
    }

    @Test
    void milisecondTimestampAccepted() {
        LogParseResult result = parse(
                VALID_LINE.replace("2026-09-13T10:00:01Z", "2026-09-13T10:00:01.123Z"));
        assertEquals(0, result.failureCount());
    }

    @Test
    void tooFewFieldsIsFailure() {
        LogParseResult result = parse("2026-09-13T10:00:01Z | ERROR | AUTH");
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("fields"));
        assertEquals(1, result.failures().get(0).getLineNumber());
    }

    @Test
    void badTimestampIsFailure() {
        LogParseResult result = parse(VALID_LINE.replace("2026-09-13T10:00:01Z", "not-a-time"));
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("timestamp"));
    }

    @Test
    void badLevelIsFailure() {
        LogParseResult result = parse(VALID_LINE.replace("| ERROR |", "| ALERT |"));
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("log level"));
    }

    @Test
    void badHttpMethodIsFailure() {
        LogParseResult result = parse(VALID_LINE.replace("| POST |", "| INVENT |"));
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("HTTP method"));
    }

    @Test
    void nonNumericStatusCodeIsFailure() {
        LogParseResult result = parse(VALID_LINE.replace("| 401 |", "| four-oh-one |"));
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("statusCode"));
    }

    @Test
    void strayPipeInMessageIsFailure() {
        LogParseResult result = parse(
                VALID_LINE.replace("authentication failed", "query failed | retry occurred"));
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("fields"));
    }

    @Test
    void statusCodeOutOfRangeIsFailure() {
        LogParseResult result = parse(VALID_LINE.replace("| 401 |", "| 999 |"));
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("statusCode"));
    }

    @Test
    void partialImportKeepsSuccessfulLines() {
        LogParseResult result = parse("garbage line", VALID_LINE, "2026-09-13T10:00:02Z | ERROR | AUTH");
        assertEquals(3, result.totalLines());
        assertEquals(2, result.failureCount());
        assertEquals(1, result.successCount());
        assertEquals(1, result.failures().get(0).getLineNumber(), "first bad line reported");
        assertEquals(3, result.failures().get(1).getLineNumber(), "second bad line reported");
    }

    @Test
    void reportsSupportedFormat() {
        assertEquals(LogFormat.TEXT, parser.supportedFormat());
    }
}