package com.loginsight.dto.request;

/**
 * Body of the string-search endpoints (docs/12 §1). {@code scope} and {@code text} decide the
 * haystack: with {@code Scope.DATASET} (the default) the loaded log dataset is rendered into the
 * searchable text; with {@code Scope.EXPLICIT} the supplied {@code text} is used verbatim.
 *
 * <p>{@code doubleHash/base/prime} are Rabin-Karp knobs only and are ignored by other engines.</p>
 */
public record SearchRequest(String pattern, String text, Scope scope, Boolean doubleHash,
                            Long base, Long prime) {

    public SearchRequest {
        scope = scope == null ? Scope.DATASET : scope;
        doubleHash = doubleHash == null ? Boolean.FALSE : doubleHash;
        base = base == null ? 0L : base;
        prime = prime == null ? 0L : prime;
    }
}