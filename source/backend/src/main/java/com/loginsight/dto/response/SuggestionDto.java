package com.loginsight.dto.response;

/**
 * A typeahead candidate for the search bar (docs/API.md §3.5). {@code type} is one of
 * {@code service | host | endpoint | level | source | status | query} and {@code value} is the exact
 * value a user can click to commit (e.g. {@code service:auth-service}).
 */
public record SuggestionDto(String type, String label, String value) {
}