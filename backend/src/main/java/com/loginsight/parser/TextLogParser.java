package com.loginsight.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.loginsight.exception.InvalidLogException;
import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Parser for the canonical pipe-delimited text format (sample-data/README.md §1):
 * {@code timestamp | LEVEL | SERVICE | ipAddress | HTTP_METHOD | endpoint | statusCode | responseTime | requestId | userId | message}.
 *
 * <p>The format is deliberately strict: the delimiter is {@code " | "} and the message (final
 * field) must not contain that sequence. Every malformed line is reported individually via
 * {@link LogParseResult}; no line aborts the import. Blank lines are skipped entirely.</p>
 */
public class TextLogParser implements LogParser {

    private static final int EXPECTED_FIELD_COUNT = 11;
    private static final String DELIMITER = " | ";

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
            throw new LogParseException("Failed reading text log stream", e);
        }
        return new LogParseResult(entries);
    }

    private ParsedLog parseLine(String line, int lineNumber) {
        try {
            String[] parts = line.split(" \\| ", -1);
            if (parts.length != EXPECTED_FIELD_COUNT) {
                throw new InvalidLogException(
                        "expected " + EXPECTED_FIELD_COUNT + " fields separated by ' | ' but found "
                                + parts.length, lineNumber);
            }
            LogEvent event = LogEvent.builder()
                    .timestamp(parseTimestamp(parts[0], lineNumber))
                    .level(parseLevel(parts[1], lineNumber))
                    .service(parts[2])
                    .ipAddress(parts[3])
                    .httpMethod(parseHttpMethod(parts[4], lineNumber))
                    .endpoint(parts[5])
                    .statusCode(parseInt(parts[6], "statusCode", lineNumber))
                    .responseTime(parseLong(parts[7], "responseTime", lineNumber))
                    .requestId(parts[8])
                    .userId(parseUserId(parts[9]))
                    .message(parts[10])
                    .build();
            return ParsedLog.success(event, lineNumber, line);
        } catch (InvalidLogException e) {
            return ParsedLog.failure(lineNumber, line, e.getMessage());
        } catch (RuntimeException e) {
            return ParsedLog.failure(lineNumber, line, "invalid field: " + e.getMessage());
        }
    }

    private static Instant parseTimestamp(String value, int lineNumber) {
        try {
            return Instant.parse(value);
        } catch (RuntimeException e) {
            throw new InvalidLogException("invalid timestamp: '" + value + "'", lineNumber);
        }
    }

    private static LogLevel parseLevel(String value, int lineNumber) {
        try {
            return LogLevel.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidLogException("invalid log level: '" + value + "'", lineNumber);
        }
    }

    private static HttpMethod parseHttpMethod(String value, int lineNumber) {
        try {
            return HttpMethod.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidLogException("invalid HTTP method: '" + value + "'", lineNumber);
        }
    }

    private static int parseInt(String value, String fieldName, int lineNumber) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidLogException("invalid " + fieldName + ": '" + value + "'", lineNumber);
        }
    }

    private static long parseLong(String value, String fieldName, int lineNumber) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new InvalidLogException("invalid " + fieldName + ": '" + value + "'", lineNumber);
        }
    }

    private static String parseUserId(String value) {
        return "-".equals(value) ? null : value;
    }

    @Override
    public LogFormat supportedFormat() {
        return LogFormat.TEXT;
    }
}