package com.loginsight.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DatasetExceptionTest {

    @Test
    void constructorWithMessage() {
        DatasetException ex = new DatasetException("dataset not found");
        assertEquals("dataset not found", ex.getMessage());
    }

    @Test
    void constructorWithMessageAndCause() {
        Throwable cause = new java.io.IOException("IO error");
        DatasetException ex = new DatasetException("dataset not found", cause);
        assertEquals("dataset not found", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void isRuntimeException() {
        DatasetException ex = new DatasetException("test");
        assertTrue(ex instanceof RuntimeException);
    }
}