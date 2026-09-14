package com.loginsight.dsa.string.aho;

/**
 * A single occurrence of a pattern found by {@link AhoCorasick}.
 *
 * @param patternId the index of the pattern in the pattern array supplied at construction
 * @param pattern   the pattern text itself (bound here for rendering convenience)
 * @param start     start position (UTF-16 code-unit offset) in the searched text
 */
public final class PatternMatch {

    private final int patternId;
    private final String pattern;
    private final int start;

    public PatternMatch(int patternId, String pattern, int start) {
        this.patternId = patternId;
        this.pattern = pattern;
        this.start = start;
    }

    public int getPatternId() {
        return patternId;
    }

    public String getPattern() {
        return pattern;
    }

    public int getStart() {
        return start;
    }

    @Override
    public String toString() {
        return "PatternMatch{id=" + patternId + ", pattern='" + pattern + "', start=" + start + '}';
    }
}