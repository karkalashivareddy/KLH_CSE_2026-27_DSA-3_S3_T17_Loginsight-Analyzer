package com.loginsight.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loginsight.exception.InvalidLogException;
import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Parser for newline-delimited JSON logs (sample-data/README.md §2). Every object may carry the
 * full canonical field set plus the {@code host} field (JSONL-only). All fields are optional
 * except {@code timestamp} and {@code message}.
 *
 * <p>Each line is parsed independently; malformed JSON or missing mandatory fields are recorded as
 * per-line failures and never abort the import. Blank lines are skipped.</p>
 */
public class JsonLogParser implements LogParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LogParseResult parse(InputStream input) {
        List<ParsedLog> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                entries.add(parseLine(line.trim(), lineNumber));
            }
        } catch (IOException e) {
            throw new LogParseException("Failed reading JSON Lines stream", e);
        }
        return new LogParseResult(entries);
    }

    private ParsedLog parseLine(String line, int lineNumber) {
        try {
            JsonNode node = objectMapper.readTree(line);
            if (!node.isObject()) {
                throw new InvalidLogException("JSON value must be an object", lineNumber);
            }
            LogEvent event = toEvent(node, lineNumber);
            return ParsedLog.success(event, lineNumber, line);
        } catch (InvalidLogException e) {
            return ParsedLog.failure(lineNumber, line, e.getMessage());
        } catch (Exception e) {
            return ParsedLog.failure(lineNumber, line, "invalid JSON: " + e.getMessage());
        }
    }

    private LogEvent toEvent(JsonNode node, int lineNumber) {
        return LogEvent.builder()
                .timestamp(parseTimestamp(node, lineNumber))
                .level(parseLevel(node, lineNumber))
                .service(textOrNull(node, "service"))
                .host(textOrNull(node, "host"))
                .ipAddress(textOrNull(node, "ipAddress"))
                .httpMethod(parseHttpMethod(node, lineNumber))
                .endpoint(textOrNull(node, "endpoint"))
                .statusCode(parseInt(node, "statusCode", 0, lineNumber))
                .responseTime(parseLong(node, "responseTime", 0, lineNumber))
                .requestId(textOrNull(node, "requestId"))
                .userId(textOrNull(node, "userId"))
                .traceId(textOrNull(node, "traceId"))
                .spanId(textOrNull(node, "spanId"))
                .url(textOrNull(node, "url"))
                .source(textOrNull(node, "source"))
                .attributes(collectAttributes(node))
                .message(textOrNull(node, "message"))
                .rawMessage(node.toString())
                .build();
    }

    /** Picks up every unrecognised top-level field into the free-form attributes bag. */
    private static Map<String, String> collectAttributes(JsonNode node) {
        Map<String, String> attributes = new LinkedHashMap<>();
        Set<String> known = Set.of("timestamp", "level", "service", "host", "ipAddress",
                "httpMethod", "endpoint", "statusCode", "responseTime", "requestId", "userId",
                "traceId", "spanId", "url", "source", "message");
        node.fields().forEachRemaining(entry -> {
            if (!known.contains(entry.getKey())) {
                JsonNode value = entry.getValue();
                attributes.put(entry.getKey(),
                        value == null || value.isNull() ? null : value.asText());
            }
        });
        return attributes;
    }

    private static Instant parseTimestamp(JsonNode node, int lineNumber) {
        String value = textOrNull(node, "timestamp");
        if (value == null) {
            throw new InvalidLogException("timestamp is required in JSON log line", lineNumber);
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException e) {
            throw new InvalidLogException("invalid timestamp: '" + value + "'", lineNumber);
        }
    }

    private static LogLevel parseLevel(JsonNode node, int lineNumber) {
        String value = textOrNull(node, "level");
        if (value == null) {
            return null;
        }
        try {
            return LogLevel.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidLogException("invalid log level: '" + value + "'", lineNumber);
        }
    }

    private static HttpMethod parseHttpMethod(JsonNode node, int lineNumber) {
        String value = textOrNull(node, "httpMethod");
        if (value == null) {
            return null;
        }
        try {
            return HttpMethod.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidLogException("invalid HTTP method: '" + value + "'", lineNumber);
        }
    }

    private static int parseInt(JsonNode node, String field, int fallback, int lineNumber) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (!value.isIntegralNumber()) {
            throw new InvalidLogException("invalid " + field + ": not an integer", lineNumber);
        }
        return value.asInt();
    }

    private static long parseLong(JsonNode node, String field, long fallback, int lineNumber) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (!value.isIntegralNumber()) {
            throw new InvalidLogException("invalid " + field + ": not an integer", lineNumber);
        }
        return value.asLong();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    @Override
    public LogFormat supportedFormat() {
        return LogFormat.JSONL;
    }
}