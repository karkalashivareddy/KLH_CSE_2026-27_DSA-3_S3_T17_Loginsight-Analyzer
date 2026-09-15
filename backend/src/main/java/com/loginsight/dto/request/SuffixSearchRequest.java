package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/string/suffix/search} (docs/12 §1). {@code text} is optional — when empty
 * the pattern is searched against the last built suffix array / the loaded dataset.
 */
public record SuffixSearchRequest(String text, String pattern) {

    public SuffixSearchRequest {
        pattern = pattern == null ? "" : pattern;
    }
}