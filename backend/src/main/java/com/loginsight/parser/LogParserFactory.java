package com.loginsight.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Resolves the {@link LogFormat} of a raw stream and returns the matching {@link LogParser}.
 *
 * <p>Detection heuristic: the first non-blank byte is {@code '{'} and the line parses as JSON
 * (starts with an object) → JSONL; otherwise the canonical text format is assumed.</p>
 */
public final class LogParserFactory {

    private static final int MAX_PROBE_BYTES = 4096;

    private final TextLogParser textParser = new TextLogParser();
    private final JsonLogParser jsonParser = new JsonLogParser();

    public LogParser parserFor(InputStream input) {
        return parserFor(detectFormat(input));
    }

    public LogParser parserFor(LogFormat format) {
        return switch (format) {
            case TEXT -> textParser;
            case JSONL -> jsonParser;
        };
    }

    public LogFormat detectFormat(InputStream input) {
        if (input == null) {
            throw new UnsupportedLogFormatException("input must not be null");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String probe = firstNonWhitespace(line);
                if (probe.startsWith("{")) {
                    return LogFormat.JSONL;
                }
                return LogFormat.TEXT;
            }
        } catch (IOException e) {
            throw new UnsupportedLogFormatException("cannot detect log format: " + e.getMessage());
        }
        throw new UnsupportedLogFormatException("input stream is empty");
    }

    private static String firstNonWhitespace(String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return line.substring(i, Math.min(line.length(), i + MAX_PROBE_BYTES));
    }
}