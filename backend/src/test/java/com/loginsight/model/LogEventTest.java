package com.loginsight.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class LogEventTest {

    private static LogEvent.Builder baseBuilder() {
        return LogEvent.builder()
                .timestamp(Instant.parse("2026-09-13T10:00:01Z"))
                .level(LogLevel.ERROR)
                .statusCode(401)
                .responseTime(45)
                .message("authentication failed");
    }

    @Test
    void builderDefaultsIdToMinusOne() {
        LogEvent event = baseBuilder().build();
        assertEquals(-1, event.getId());
    }

    @Test
    void missingTimestampRejected() {
        LogEvent.Builder builder = baseBuilder();
        assertThrows(NullPointerException.class, () -> builder.timestamp(null).build());
    }

    @Test
    void blankMessageRejected() {
        LogEvent.Builder builder = baseBuilder().message("   ");
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void statusCodeRangeEnforced() {
        assertThrows(IllegalArgumentException.class, () -> baseBuilder().statusCode(99).build());
        assertThrows(IllegalArgumentException.class, () -> baseBuilder().statusCode(600).build());
        assertDoesNotThrow(() -> baseBuilder().statusCode(0).build(), "0 = unrecorded (JSONL optional)");
        assertDoesNotThrow(() -> baseBuilder().statusCode(100).build());
        assertDoesNotThrow(() -> baseBuilder().statusCode(599).build());
    }

    @Test
    void negativeResponseTimeRejected() {
        assertThrows(IllegalArgumentException.class, () -> baseBuilder().responseTime(-1).build());
    }

    @Test
    void withIdProducesCopyWithSameIdentifiers() {
        LogEvent original = baseBuilder().build();
        LogEvent copy = original.withId(42);
        assertEquals(42, copy.getId());
        assertEquals(-1, original.getId());
        assertEquals(original, copy, "equals ignores the id");
        assertEquals(original.hashCode(), copy.hashCode(), "hashCode ignores the id");
        assertEquals(original.getMessage(), copy.getMessage());
    }

    @Test
    void equalsIgnoresIdOnly() {
        LogEvent a = baseBuilder().build().withId(1);
        LogEvent b = baseBuilder().build().withId(2);
        LogEvent different = baseBuilder().message("other failure").build();
        assertEquals(a, b);
        assertNotEquals(a, different);
    }
}