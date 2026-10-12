package com.loginsight.dsa.string;

/**
 * Common contract for single-pattern exact string matchers. Implementations are pure Java, contain
 * no Spring references, and never delegate the matching itself to {@code String.indexOf},
 * {@code contains} or regular expressions (docs/02 §8).
 *
 * <p>Character model: all matchers operate on the UTF-16 {@code char} representation of the input
 * (one position = one {@code char}). This is documented per class; tests include multilingual
 * (Indian-language) text to keep the contract honest.</p>
 */
public interface StringMatcher {

    /**
     * Find every start position of {@code pattern} in {@code text}.
     *
     * @param text    the text to search; must not be null
     * @param pattern the pattern to find; must not be null or empty
     * @return uniform {@link StringSearchResult}; an empty result (no matches) when the pattern
     *         is longer than the text
     * @throws IllegalArgumentException if {@code text} is null, {@code pattern} is null, or
     *         {@code pattern} is empty
     */
    StringSearchResult match(String text, String pattern);
}