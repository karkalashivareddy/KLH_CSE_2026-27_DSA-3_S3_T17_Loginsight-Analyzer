package com.loginsight.pattern;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.dto.response.PatternDto;
import com.loginsight.model.LogEvent;

/**
 * Heuristic log-pattern extractor (docs/ALGORITHMS.md, docs/API.md §5).
 *
 * <p>Every message is tokenized and normalized: tokens that look variable (pure numbers, ids with
 * embedded digits, dotted-quad IPs, hex/hash values) are replaced by {@code <*>}; the surviving
 * shape is the pattern template. Templates are then aggregated by frequency. This is a
 * rule-based/token normalization approach — explicitly <em>not</em> ML, and documented as such in
 * the UI.</p>
 */
public final class PatternExtractor {

    /** Extract the top {@code limit} message patterns across the dataset, ordered by count. */
    public List<PatternDto> extract(List<LogEvent> events, int limit) {
        Map<String, PatternAccumulator> byTemplate = new LinkedHashMap<>();
        for (LogEvent event : events) {
            String message = event.getMessage();
            if (message == null) {
                continue;
            }
            String template = normalizeMessage(message);
            if (template.isEmpty()) {
                continue;
            }
            byTemplate.computeIfAbsent(template, PatternAccumulator::new).add(event);
        }
        List<PatternAccumulator> ordered = new ArrayList<>(byTemplate.values());
        ordered.sort((a, b) -> Long.compare(b.count, a.count));
        List<PatternDto> out = new ArrayList<>(Math.min(limit, ordered.size()));
        for (int i = 0; i < Math.min(limit, ordered.size()); i++) {
            PatternAccumulator acc = ordered.get(i);
            out.add(new PatternDto(acc.template, acc.count, acc.example,
                    acc.exampleLevel == null ? null : acc.exampleLevel.name()));
        }
        return out;
    }

    /** Simplest usable primitive: template strings for a set of messages (for incident labelling). */
    public String topTemplate(List<String> messages) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String message : messages) {
            String template = normalizeMessage(message);
            if (!template.isEmpty()) {
                counts.merge(template, 1L, Long::sum);
            }
        }
        String best = null;
        long bestCount = 0;
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    /** Tokenize a message and replace variable tokens with {@code <*>}. */
    public static String normalizeMessage(String message) {
        String[] tokens = message.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (isVariable(token)) {
                append(sb, "<*>");
            } else {
                append(sb, stripPunctuation(token));
            }
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String token) {
        if (token.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(token);
    }

    private static boolean isVariable(String token) {
        if (token.contains(".")) {
            String[] parts = token.split("\\.");
            if (parts.length == 4 && allNumeric(parts)) {
                return true; // dotted-quad IPv4
            }
        }
        if (token.length() >= 8 && isHexish(token)) {
            return true;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isDigit(c) || c == '#' || c == '{' || c == '}' || c == '$') {
                return true;
            }
        }
        return false;
    }

    private static boolean allNumeric(String[] parts) {
        for (String part : parts) {
            if (part.isEmpty()) {
                return false;
            }
            for (int i = 0; i < part.length(); i++) {
                if (!Character.isDigit(part.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isHexish(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                    || c == '-' )) {
                return false;
            }
        }
        return true;
    }

    private static String stripPunctuation(String token) {
        return token.trim().replaceAll("[^\\p{L}\\p{N}._-]", "");
    }

    private static final class PatternAccumulator {
        final String template;
        long count;
        String example;
        com.loginsight.model.LogLevel exampleLevel;

        PatternAccumulator(String template) {
            this.template = template;
        }

        void add(LogEvent event) {
            count++;
            if (example == null) {
                example = event.getMessage();
                exampleLevel = event.getLevel();
            }
        }
    }
}