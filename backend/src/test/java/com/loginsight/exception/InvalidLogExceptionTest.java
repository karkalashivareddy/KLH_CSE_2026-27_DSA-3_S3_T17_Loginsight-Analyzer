package com.loginsight.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InvalidLogExceptionTest {

    @Test
    void constructorWithMessageAndLineNumber() {
        InvalidLogException ex = new InvalidLogException("invalid log", 42);
        assertEquals("invalid log", ex.getMessage());
        assertEquals(42, ex.getLineNumber());
    }

    @Test
    void constructorWithMessageLineNumberAndCause() {
        Throwable cause = new IllegalArgumentException("cause");
        InvalidLogException ex = new InvalidLogException("invalid log", 42, cause);
        assertEquals("invalid log", ex.getMessage());
        assertEquals(42, ex.getLineNumber());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void isRuntimeException() {
        InvalidLogException ex = new InvalidLogException("test", 1);
        assertTrue(ex instanceof RuntimeException);
    }
}