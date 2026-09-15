package com.loginsight.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.model.LogEvent;

/**
 * Builds a {@link ServiceDependencyGraph} in one ordered pass over the dataset, grouping events by
 * {@code requestId} in ingestion order (docs/03 graph). Also exposes the per-service counts used by
 * the {@link TopKFrequentAnalyzer}. Pure Java, no Spring.
 */
public final class ServiceGraphBuilder {

    private final ServiceDependencyGraph graph = new ServiceDependencyGraph();
    private final Map<String, Integer> requestTrail = new LinkedHashMap<>();

    /**
     * Fold one dataset into the graph: for every requestId, consecutive distinct services become a
     * dependency edge {@code a -> b}.
     */
    public ServiceDependencyGraph build(List<LogEvent> events) {
        Map<String, List<LogEvent>> grouped = new LinkedHashMap<>();
        for (LogEvent event : events) {
            if (event.getService() == null) {
                continue;
            }
            String requestId = event.getRequestId() == null ? "\u0000" : event.getRequestId();
            grouped.computeIfAbsent(requestId, k -> new ArrayList<>()).add(event);
        }
        for (Map.Entry<String, List<LogEvent>> entry : grouped.entrySet()) {
            String previous = null;
            for (LogEvent event : entry.getValue()) {
                String service = event.getService();
                graph.addNode(service);
                if (previous != null) {
                    graph.addDependency(previous, service);
                    requestTrail.merge(previous + "->" + service, 1, Integer::sum);
                }
                previous = service;
            }
        }
        return graph;
    }

    /** Edge frequency for consecutive {a,b} pairs as {a-b → count}. */
    public Map<String, Integer> requestTrailCounts() {
        return Map.copyOf(requestTrail);
    }

    /** Node frequency {service → event count}. */
    public Map<String, Integer> nodeCountsFrom(List<LogEvent> events) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (LogEvent event : events) {
            if (event.getService() != null) {
                counts.merge(event.getService(), 1, Integer::sum);
            }
        }
        return counts;
    }

    /** LinkedSorted implementation of frequency answer. */
    public List<Map.Entry<String, Integer>> topNodes(List<LogEvent> events, int limit) {
        return nodeCountsFrom(events).entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey()).reversed())
                .limit(limit)
                .toList();
    }

    public int reachableFrom(String service) {
        return graph.reachableFrom(service).size();
    }

    static Comparator<Map.Entry<String, Integer>> byValueThenKey() {
        return Map.Entry.<String, Integer>comparingByValue()
                .thenComparing(Map.Entry.comparingByKey());
    }
}