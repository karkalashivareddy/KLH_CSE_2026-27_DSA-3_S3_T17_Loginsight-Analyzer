package com.loginsight.exception;

/**
 * Thrown when the in-memory dataset cannot satisfy a request (missing file, I/O error, etc.).
 */
public class DatasetException extends RuntimeException {

    public DatasetException(String message) {
        super(message);
    }

    public DatasetException(String message, Throwable cause) {
        super(message, cause);
    }
}
