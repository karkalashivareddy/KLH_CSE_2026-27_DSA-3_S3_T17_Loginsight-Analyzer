package com.loginsight.parser;

/**
 * Thrown when the input content cannot be identified as any supported {@link LogFormat}.
 */
public class UnsupportedLogFormatException extends ParserException {

    public UnsupportedLogFormatException(String message) {
        super(message);
    }
}