package com.loginsight.search;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.loginsight.exception.InvalidQueryException;

/**
 * Parser for the documented LogInsight query syntax (docs/API.md §3).
 *
 * <p>Grammar (one or more space-separated tokens):</p>
 * <pre>
 *   token       := field ":" value | phrase | word
 *   field       := level | service | host | source | status | trace | request | message
 *   value       := word | "\"" word+ "\""
 * </pre>
 *
 * <p>Recognised fields map to index filters; {@code message:"..."} and any bare phrase/word become
 * the free-text pattern searched with the string-search engine. Unknown {@code x:y} tokens are
 * treated as free text so searches degrade gracefully instead of erroring. Timestamps in the
 * {@code from}/{@code to} fields (supported by the API request body, not the query string) are
 * handled separately.</p>
 */
public final class SearchQueryParser {

    private static final List<String> KNOWN_FIELDS = List.of(
            "level", "service", "host", "source", "status", "trace", "request", "message");

    public SearchQuery parse(String raw, Instant from, Instant to, String sort) {
        if (raw == null || raw.isBlank()) {
            return new SearchQuery("", List.of(), null, null, null, null, null, null,
                    from, to, sort);
        }
        List<String> levels = new ArrayList<>();
        String service = null;
        String host = null;
        String source = null;
        String status = null;
        String trace = null;
        String request = null;
        StringBuilder freeText = new StringBuilder();

        for (Token token : tokenize(raw)) {
            int colon = token.text.indexOf(':');
            if (colon > 0 && KNOWN_FIELDS.contains(token.text.substring(0, colon))) {
                String field = token.text.substring(0, colon);
                String value = unquote(token.text.substring(colon + 1));
                if (value.isEmpty()) {
                    throw new InvalidQueryException("Empty value for field '" + field
                            + "' (expected " + field + ":<value>)");
                }
                switch (field) {
                    case "level" -> levels.add(value.toUpperCase(java.util.Locale.ROOT));
                    case "service" -> service = value;
                    case "host" -> host = value;
                    case "source" -> source = value;
                    case "status" -> status = value;
                    case "trace" -> trace = value;
                    case "request" -> request = value;
                    case "message" -> append(freeText, value);
                    default -> append(freeText, token.text);
                }
            } else {
                append(freeText, unquote(token.text));
            }
        }
        return new SearchQuery(freeText.toString().trim(), levels, service, host, source, status,
                trace, request, from, to, sort);
    }

    private static void append(StringBuilder sb, String value) {
        if (value.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(value);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static List<Token> tokenize(String raw) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(new Token(current.toString()));
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens.add(new Token(current.toString()));
        }
        return tokens;
    }

    private record Token(String text) {
    }
}