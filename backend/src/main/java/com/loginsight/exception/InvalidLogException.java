package com.loginsight.exception;

/**
 * Thrown when a log event fails domain validation (e.g. null timestamp, out-of-range status code).
 */
public class InvalidLogException extends RuntimeException {

    private final int lineNumber;

    public InvalidLogException(String message, int lineNumber) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public InvalidLogException(String message, int lineNumber, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
