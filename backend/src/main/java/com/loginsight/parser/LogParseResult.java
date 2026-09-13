package com.loginsight.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.loginsight.model.LogEvent;

/**
 * Aggregated result of parsing a whole log stream: one {@link ParsedLog} per source line plus
 * derived counters. Failed lines never abort the import (FR-1: malformed records are rejected with
 * useful parse errors without aborting the whole import).
 */
public final class LogParseResult {

    private final List<ParsedLog> entries;
    private final int totalLines;
    private final int successCount;
    private final int failureCount;

    public LogParseResult(List<ParsedLog> entries) {
        this.entries = List.copyOf(entries);
        int ok = 0;
        for (ParsedLog entry : entries) {
            if (entry.isSuccess()) {
                ok++;
            }
        }
        this.successCount = ok;
        this.totalLines = entries.size();
        this.failureCount = totalLines - ok;
    }

    /** Per-line parse outcomes in source order. */
    public List<ParsedLog> entries() {
        return entries;
    }

    /** Number of source lines considered (blank lines are skipped by parsers). */
    public int totalLines() {
        return totalLines;
    }

    public int successCount() {
        return successCount;
    }

    public int failureCount() {
        return failureCount;
    }

    /** All successfully parsed events in source order, id field untouched (still -1). */
    public List<LogEvent> successfulEvents() {
        List<LogEvent> events = new ArrayList<>(successCount);
        for (ParsedLog entry : entries) {
            if (entry.isSuccess()) {
                events.add(entry.getEvent());
            }
        }
        return events;
    }

    /** Only the failed entries, for reporting (line numbers + reasons). */
    public List<ParsedLog> failures() {
        return entries.stream().filter(e -> !e.isSuccess()).collect(Collectors.toList());
    }
}