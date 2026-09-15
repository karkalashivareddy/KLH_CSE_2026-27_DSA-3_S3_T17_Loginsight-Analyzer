package com.loginsight.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InvalidQueryExceptionTest {

    @Test
    void constructorWithMessage() {
        InvalidQueryException ex = new InvalidQueryException("test message");
        assertEquals("test message", ex.getMessage());
    }

    @Test
    void constructorWithMessageAndCause() {
        Throwable cause = new IllegalArgumentException("cause");
        InvalidQueryException ex = new InvalidQueryException("test message", cause);
        assertEquals("test message", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void isRuntimeException() {
        InvalidQueryException ex = new InvalidQueryException("test");
        assertTrue(ex instanceof RuntimeException);
    }
}