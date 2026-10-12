package com.loginsight.parser;

/**
 * Base class for all parsing failures (unsupported format, I/O problems, unparsable stream).
 */
public class ParserException extends RuntimeException {

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}