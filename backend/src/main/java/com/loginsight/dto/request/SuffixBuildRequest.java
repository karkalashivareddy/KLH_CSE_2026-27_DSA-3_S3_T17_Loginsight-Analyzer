package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/string/suffix/build} (docs/12 §1). {@code text} may be {@code null} or
 * blank space to build the suffix array over the loaded dataset instead.
 */
public record SuffixBuildRequest(String text) {

    public SuffixBuildRequest {
        text = text == null ? "" : text;
    }
}