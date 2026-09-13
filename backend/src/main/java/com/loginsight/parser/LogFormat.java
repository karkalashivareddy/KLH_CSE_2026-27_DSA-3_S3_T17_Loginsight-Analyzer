package com.loginsight.parser;

/**
 * Recognised raw log encodings.
 */
public enum LogFormat {

    /** Canonical pipe-delimited text format from sample-data/README.md §1. */
    TEXT,

    /** Newline-delimited JSON objects from sample-data/README.md §2. */
    JSONL
}