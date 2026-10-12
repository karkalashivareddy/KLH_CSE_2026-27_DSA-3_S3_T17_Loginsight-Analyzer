package com.loginsight.dto.response;

/**
 * A discovered recurring log message structure (docs/API.md §5). {@code template} is a normalized
 * message shape where variable tokens (numeric values, ids, IPs, hashes) are replaced by
 * {@code <*>}; {@code count} is the number of events sharing the shape; {@code example} and
 * {@code level} describe the first event seen for the pattern. Extraction is heuristic
 * (rule/token-based), never ML.
 */
public record PatternDto(String template, long count, String example, String level) {
}