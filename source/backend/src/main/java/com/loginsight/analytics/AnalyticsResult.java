package com.loginsight.analytics;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable snapshot of dataset-level descriptive statistics.
 */
public final class AnalyticsResult {

    private final long totalEvents;
    private final long totalValidEvents;
    private final long totalFailedLines;
    private final Map<String, Integer> levelFrequency;
    private final Map<String, Integer> serviceFrequency;
    private final Map<Integer, Integer> statusCodeFrequency;
    private final Map<String, Integer> endpointFrequency;
    private final long sumResponseTimeMs;
    private final long minResponseTimeMs;
    private final long maxResponseTimeMs;

    public AnalyticsResult(long totalEvents, long totalValidEvents, long totalFailedLines,
                           Map<String, Integer> levelFrequency, Map<String, Integer> serviceFrequency,
                           Map<Integer, Integer> statusCodeFrequency, Map<String, Integer> endpointFrequency,
                           long sumResponseTimeMs, long minResponseTimeMs, long maxResponseTimeMs) {
        this.totalEvents = totalEvents;
        this.totalValidEvents = totalValidEvents;
        this.totalFailedLines = totalFailedLines;
        this.levelFrequency = Collections.unmodifiableMap(new TreeMap<>(levelFrequency));
        this.serviceFrequency = Collections.unmodifiableMap(new TreeMap<>(serviceFrequency));
        this.statusCodeFrequency = Collections.unmodifiableMap(new TreeMap<>(statusCodeFrequency));
        this.endpointFrequency = Collections.unmodifiableMap(new TreeMap<>(endpointFrequency));
        this.sumResponseTimeMs = sumResponseTimeMs;
        this.minResponseTimeMs = minResponseTimeMs;
        this.maxResponseTimeMs = maxResponseTimeMs;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public long getTotalValidEvents() {
        return totalValidEvents;
    }

    public long getTotalFailedLines() {
        return totalFailedLines;
    }

    public Map<String, Integer> getLevelFrequency() {
        return levelFrequency;
    }

    public Map<String, Integer> getServiceFrequency() {
        return serviceFrequency;
    }

    public Map<Integer, Integer> getStatusCodeFrequency() {
        return statusCodeFrequency;
    }

    public Map<String, Integer> getEndpointFrequency() {
        return endpointFrequency;
    }

    public long getSumResponseTimeMs() {
        return sumResponseTimeMs;
    }

    public long getMinResponseTimeMs() {
        return minResponseTimeMs;
    }

    public long getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }

    public double getAverageResponseTimeMs() {
        return totalValidEvents == 0 ? 0.0 : (double) sumResponseTimeMs / totalValidEvents;
    }
}