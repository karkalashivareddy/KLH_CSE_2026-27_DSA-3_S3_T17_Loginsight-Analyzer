package com.loginsight.analytics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Severity distribution of a dataset. Every level is present in the output (zero counts included),
 * ordered TRACE → FATAL, matching the canonical severity scale used across the product.
 */
public final class SeverityAnalyzer {

    public static final List<String> ORDER = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL");

    public Map<String, Long> distribution(List<LogEvent> events) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (String level : ORDER) {
            out.put(level, 0L);
        }
        for (LogEvent event : events) {
            LogLevel level = event.getLevel();
            if (level != null) {
                out.merge(level.name(), 1L, Long::sum);
            }
        }
        return out;
    }
}