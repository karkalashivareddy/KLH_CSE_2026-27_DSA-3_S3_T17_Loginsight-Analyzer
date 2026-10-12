package com.loginsight.dsa.string;

/**
 * Shared input-contract enforcement for the single-pattern matchers. Keeps every matcher's
 * documented empty/pattern-larger-than-text contract identical (docs/13 §5).
 */
final class TextValidator {

    private TextValidator() {
    }

    static void requireNonNull(String text, String pattern) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (pattern == null) {
            throw new IllegalArgumentException("pattern must not be null");
        }
    }

    static void requireNonEmpty(String pattern) {
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("pattern must not be empty");
        }
    }
}