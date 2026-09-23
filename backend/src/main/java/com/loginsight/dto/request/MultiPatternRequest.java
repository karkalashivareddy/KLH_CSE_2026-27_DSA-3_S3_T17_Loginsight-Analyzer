package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/search/multi} (docs/12 §1): several patterns searched in one pass with
 * the Aho-Corasick automaton. {@code scope}/{@code text} follow the {@link Scope} contract; a
 * missing scope defaults to {@link Scope#EXPLICIT} (the given text), matching {@link RequestFactory}.
 */
public record MultiPatternRequest(String[] patterns, String text, Scope scope) {

    public MultiPatternRequest {
        scope = scope == null ? Scope.EXPLICIT : scope;
    }
}