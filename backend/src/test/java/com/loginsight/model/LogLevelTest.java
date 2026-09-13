package com.loginsight.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LogLevelTest {

    @Test
    void fromStringNormalizesCase() {
        assertEquals(LogLevel.INFO, LogLevel.fromString("INFO"));
        assertEquals(LogLevel.INFO, LogLevel.fromString("info"));
        assertEquals(LogLevel.ERROR, LogLevel.fromString("Error"));
        assertEquals(LogLevel.TRACE, LogLevel.fromString(" trace "));
    }

    @Test
    void fromStringRejectsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromString("ALERT"));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromString("  "));
    }

    @Test
    void exposesAllSixLevels() {
        assertEquals(6, LogLevel.values().length);
        assertArrayEquals(new String[]{"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"},
                java.util.Arrays.stream(LogLevel.values()).map(Enum::name).toArray());
    }
}