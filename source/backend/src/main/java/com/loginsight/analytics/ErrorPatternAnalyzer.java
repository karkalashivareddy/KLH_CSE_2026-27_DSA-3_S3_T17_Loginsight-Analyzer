package com.loginsight.analytics;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Aggregates the most frequent error/warning messages of a dataset for the health view. Pure Java,
 * unit-testable standalone (docs/03 analytics package).
 */
public final class ErrorPatternAnalyzer {

    /** Most frequent {@link LogLevel#ERROR} message; null when none exists. */
    public String topError(List<LogEvent> events) {
        Map<String, Integer> messageFrequency = new HashMap<>();
        for (LogEvent event : events) {
            if (event.getLevel() == LogLevel.ERROR && event.getMessage() != null) {
                messageFrequency.merge(event.getMessage(), 1, Integer::sum);
            }
        }
        String top = null;
        int best = 0;
        for (Map.Entry<String, Integer> entry : messageFrequency.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                top = entry.getKey();
            }
        }
        return top;
    }

    /** Top {@code limit} error messages as {message → count}. */
    public Map<String, Integer> topErrors(List<LogEvent> events, int limit) {
        Map<String, Integer> messageFrequency = new LinkedHashMap<>();
        for (LogEvent event : events) {
            if ((event.getLevel() == LogLevel.ERROR || event.getLevel() == LogLevel.WARN)
                    && event.getMessage() != null) {
                messageFrequency.merge(event.getMessage(), 1, Integer::sum);
            }
        }
        Map<String, Integer> top = new LinkedHashMap<>();
        List<Map.Entry<String, Integer>> sorted = messageFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
            top.put(sorted.get(i).getKey(), sorted.get(i).getValue());
        }
        return top;
    }

    /** Distinct non-null values of {@link LogEvent#getService()} in a dataset. */
    public Set<String> distinctServices(List<LogEvent> events) {
        java.util.LinkedHashSet<String> services = new java.util.LinkedHashSet<>();
        for (LogEvent event : events) {
            if (event.getService() != null) {
                services.add(event.getService());
            }
        }
        return services;
    }
}