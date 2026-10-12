package com.loginsight.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class LogParserFactoryTest {

    private final LogParserFactory factory = new LogParserFactory();

    @Test
    void detectsJsonLineFromLeadingBrace() {
        String line = "{\"timestamp\":\"2026-09-13T10:00:01Z\",\"message\":\"x\"}\nmore";
        assertEquals(LogFormat.JSONL, factory.detectFormat(stream(line)));
        assertInstanceOf(JsonLogParser.class, factory.parserFor(stream(line)));
    }

    @Test
    void detectsTextAsDefault() {
        String line = "2026-09-13T10:00:01Z | INFO | USER | 10.0.0.1 | GET | /api/users | 200 | 12"
                + " | req-001 | - | request completed";
        assertEquals(LogFormat.TEXT, factory.detectFormat(stream(line)));
        assertInstanceOf(TextLogParser.class, factory.parserFor(stream(line)));
    }

    @Test
    void skipsLeadingBlankLinesBeforeDetection() {
        String body = "\n\n  \n{\"timestamp\":\"t\",\"message\":\"x\"}";
        assertEquals(LogFormat.JSONL, factory.detectFormat(stream(body)));
    }

    @Test
    void parserForRespectsExplicitFormat() {
        assertInstanceOf(TextLogParser.class, factory.parserFor(LogFormat.TEXT));
        assertInstanceOf(JsonLogParser.class, factory.parserFor(LogFormat.JSONL));
    }

    @Test
    void emptyStreamRejected() {
        assertThrows(UnsupportedLogFormatException.class,
                () -> factory.detectFormat(stream("")));
        assertThrows(UnsupportedLogFormatException.class,
                () -> factory.detectFormat(stream("  \n \n")));
    }

    @Test
    void nullStreamRejected() {
        assertThrows(UnsupportedLogFormatException.class, () -> factory.detectFormat(null));
    }

    private static ByteArrayInputStream stream(String body) {
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }
}