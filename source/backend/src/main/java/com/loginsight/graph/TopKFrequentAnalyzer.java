package com.loginsight.graph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.model.LogEvent;

/**
 * Top-K frequency analysis over a token dimension of the log dataset (service, endpoint, level,
 * IP). Counting is a single O(n) pass; the top-k answer is extracted with a bounded partial sort so
 * no full sort of the frequency map is needed (docs/03 graph analytics).
 */
public final class TopKFrequentAnalyzer {

    public enum Dimension {
        SERVICE, ENDPOINT, LEVEL, IP
    }

    /**
     * @return ordered list of {key, count} pairs for the {@code limit} most frequent values of
     *         {@code dimension} in the dataset
     */
    public List<Map.Entry<String, Integer>> topK(List<LogEvent> events, Dimension dimension,
                                                 int limit) {
        Map<String, Integer> frequency = new LinkedHashMap<>();
        for (LogEvent event : events) {
            String key = switch (dimension) {
                case SERVICE -> event.getService();
                case ENDPOINT -> event.getEndpoint();
                case LEVEL -> event.getLevel() == null ? null : event.getLevel().name();
                case IP -> event.getIpAddress();
            };
            if (key != null) {
                frequency.merge(key, 1, Integer::sum);
            }
        }
        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey()).reversed())
                .limit(Math.max(0, limit))
                .toList();
    }

    /** Flatten results to the {key, count} wire maps used by the stats endpoint. */
    public List<Map<String, Object>> asBuckets(List<Map.Entry<String, Integer>> ranked) {
        return ranked.stream()
                .map(entry -> Map.<String, Object>of("service", entry.getKey(),
                        "count", entry.getValue()))
                .toList();
    }
}