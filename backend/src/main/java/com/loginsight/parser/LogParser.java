package com.loginsight.parser;

import java.io.InputStream;

/**
 * Parsing service provider: turns a raw log stream into a {@link LogParseResult}.
 * Implementations must never abort on a single malformed line; failures are captured per line so
 * partial imports are visible (docs/02 §2, FR-1).
 */
public interface LogParser {

    /**
     * Parse the whole stream.
     *
     * @param input the raw bytes; closed by the caller
     * @return aggregated per-line results; never null, never empty on non-empty input
     * @throws LogParseException on unrecoverable stream-level failures
     */
    LogParseResult parse(InputStream input);

    /** The format this parser understands. */
    LogFormat supportedFormat();
}