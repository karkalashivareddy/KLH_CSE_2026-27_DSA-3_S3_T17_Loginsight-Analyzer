package com.loginsight.parser;

/**
 * Thrown when an individual line of a supported format cannot be parsed. Parsers in this module
 * record such failures per-line inside {@link LogParseResult} rather than aborting the whole
 * stream; this exception is used for low-level conditions that currently cannot be recovered.
 */
public class LogParseException extends ParserException {

    public LogParseException(String message) {
        super(message);
    }

    public LogParseException(String message, Throwable cause) {
        super(message, cause);
    }
}