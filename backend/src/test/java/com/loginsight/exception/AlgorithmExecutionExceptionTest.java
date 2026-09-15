package com.loginsight.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AlgorithmExecutionExceptionTest {

    @Test
    void constructorWithMessage() {
        AlgorithmExecutionException ex = new AlgorithmExecutionException("algorithm failed");
        assertEquals("algorithm failed", ex.getMessage());
    }

    @Test
    void constructorWithMessageAndCause() {
        Throwable cause = new IllegalStateException("state error");
        AlgorithmExecutionException ex = new AlgorithmExecutionException("algorithm failed", cause);
        assertEquals("algorithm failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void isRuntimeException() {
        AlgorithmExecutionException ex = new AlgorithmExecutionException("test");
        assertTrue(ex instanceof RuntimeException);
    }
}