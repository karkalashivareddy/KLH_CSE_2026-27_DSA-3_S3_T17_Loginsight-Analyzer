package com.loginsight.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.loginsight.model.LogEvent;

/**
 * Immutable in-memory snapshot of a parsed log dataset (docs/02 §6).
 *
 * <p>Events are stored in ingestion order and carry dataset-assigned sequential ids starting at 0.
 * Malformed lines are not dropped silently: the counts are preserved for partial-import reporting
 * (FR-1).</p>
 */
public final class Dataset {

    private final String name;
    private final List<LogEvent> events;
    private final int totalLines;
    private final int failedLines;
    private final Instant loadedAt;

    public Dataset(String name, List<LogEvent> events, int totalLines, int failedLines, Instant loadedAt) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.events = List.copyOf(events);
        this.totalLines = totalLines;
        this.failedLines = failedLines;
        this.loadedAt = loadedAt == null ? Instant.now() : loadedAt;
    }

    public String name() {
        return name;
    }

    public List<LogEvent> events() {
        return events;
    }

    /** Total input lines considered, including failures. */
    public int totalLines() {
        return totalLines;
    }

    /** Number of lines that failed to parse. */
    public int failedLines() {
        return failedLines;
    }

    /** Number of successfully imported events. */
    public int size() {
        return events.size();
    }

    public Instant loadedAt() {
        return loadedAt;
    }
}