package com.loginsight.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

class JsonLogParserTest {

    private static final String VALID_LINE =
            "{\"timestamp\":\"2026-09-13T10:00:01Z\",\"level\":\"ERROR\",\"service\":\"AUTH\","
                    + "\"host\":\"api-1\",\"ipAddress\":\"10.0.0.1\",\"httpMethod\":\"POST\","
                    + "\"endpoint\":\"/login\",\"statusCode\":401,\"responseTime\":45,"
                    + "\"requestId\":\"req-001\",\"userId\":\"user-101\","
                    + "\"message\":\"authentication failed\"}";

    private final JsonLogParser parser = new JsonLogParser();

    private LogParseResult parse(String... lines) {
        String body = String.join("\n", lines);
        return parser.parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesValidLine() {
        LogParseResult result = parse(VALID_LINE);
        assertEquals(1, result.successCount());
        assertEquals(0, result.failureCount());
        LogEvent event = result.successfulEvents().get(0);
        assertEquals(Instant.parse("2026-09-13T10:00:01Z"), event.getTimestamp());
        assertEquals(LogLevel.ERROR, event.getLevel());
        assertEquals("AUTH", event.getService());
        assertEquals("api-1", event.getHost());
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
    void onlyTimestampAndMessageRequired() {
        String line = "{\"timestamp\":\"2026-09-13T10:00:01Z\",\"message\":\"hello\"}";
        LogParseResult result = parse(line);
        assertEquals(1, result.successCount());
        assertEquals("hello", result.successfulEvents().get(0).getMessage());
    }

    @Test
    void missingTimestampRejected() {
        String line = "{\"message\":\"hello\"}";
        LogParseResult result = parse(line);
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("timestamp"));
    }

    @Test
    void nonJsonLineIsFailure() {
        LogParseResult result = parse("this is not json");
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("JSON"));
    }

    @Test
    void nonObjectJsonIsFailure() {
        String line = "[\"array\", \"not object\"]";
        LogParseResult result = parse(line);
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("object"));
    }

    @Test
    void unknownLevelIsFailure() {
        String line = VALID_LINE.replace("\"ERROR\"", "\"ALERT\"");
        LogParseResult result = parse(line);
        assertEquals(1, result.failureCount());
        assertTrue(result.failures().get(0).getFailureReason().contains("log level"));
    }

    @Test
    void blankLinesSkipped() {
        LogParseResult result = parse("", "  ", VALID_LINE);
        assertEquals(1, result.totalLines());
        assertEquals(1, result.successCount());
    }

    @Test
    void textValueInNumericFieldIsFailure() {
        String line = VALID_LINE.replace("\"statusCode\":401", "\"statusCode\":\"401\"");
        LogParseResult result = parse(line);
        assertEquals(1, result.failureCount());
    }

    @Test
    void reportsSupportedFormat() {
        assertEquals(LogFormat.JSONL, parser.supportedFormat());
    }
}