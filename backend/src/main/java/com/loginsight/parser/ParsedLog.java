package com.loginsight.parser;

import com.loginsight.model.LogEvent;

/**
 * Outcome of parsing a single line of raw log input. A successful parse carries the normalized
 * {@link LogEvent}; a failure carries the source line number and a human-readable reason.
 */
public final class ParsedLog {

    private final boolean success;
    private final LogEvent event;
    private final int lineNumber;
    private final String failureReason;
    private final String rawLine;

    private ParsedLog(LogEvent event, int lineNumber, String rawLine) {
        this.success = true;
        this.event = event;
        this.lineNumber = lineNumber;
        this.failureReason = null;
        this.rawLine = rawLine;
    }

    private ParsedLog(int lineNumber, String rawLine, String failureReason) {
        this.success = false;
        this.event = null;
        this.lineNumber = lineNumber;
        this.failureReason = failureReason;
        this.rawLine = rawLine;
    }

    public static ParsedLog success(LogEvent event, int lineNumber, String rawLine) {
        return new ParsedLog(event, lineNumber, rawLine);
    }

    public static ParsedLog failure(int lineNumber, String rawLine, String failureReason) {
        return new ParsedLog(lineNumber, rawLine, failureReason);
    }

    public boolean isSuccess() {
        return success;
    }

    public LogEvent getEvent() {
        return event;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getRawLine() {
        return rawLine;
    }
}