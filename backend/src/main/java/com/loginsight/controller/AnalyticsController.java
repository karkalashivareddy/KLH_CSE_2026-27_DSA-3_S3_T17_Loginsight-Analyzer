package com.loginsight.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.analytics.ErrorPatternAnalyzer;
import com.loginsight.analytics.TimeWindowAnalyzer;
import com.loginsight.graph.ServiceDependencyGraph;
import com.loginsight.graph.ServiceGraphBuilder;
import com.loginsight.graph.TopKFrequentAnalyzer;
import com.loginsight.model.LogEvent;
import com.loginsight.service.LogService;

/**
 * Analytics endpoints (docs/12 §2, §6): service-dependency topology, error patterns, top-K
 * frequencies and the time-window traffic series. All answers are computed live from the currently
 * loaded dataset through the pure-Java analyzers; nothing is precomputed or fabricated.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final LogService logService;
    private final ErrorPatternAnalyzer errorPatternAnalyzer = new ErrorPatternAnalyzer();
    private final TopKFrequentAnalyzer topKFrequentAnalyzer = new TopKFrequentAnalyzer();
    private final TimeWindowAnalyzer timeWindowAnalyzer = new TimeWindowAnalyzer();

    public AnalyticsController(LogService logService) {
        this.logService = logService;
    }

    /** Directed service-dependency graph with edge weights (docs/06). */
    @GetMapping("/dependencies")
    public Map<String, Object> dependencies() {
        List<LogEvent> events = logService.events();
        ServiceGraphBuilder builder = new ServiceGraphBuilder();
        ServiceDependencyGraph graph = builder.build(events);
        Map<String, Integer> trail = builder.requestTrailCounts();

        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, Integer> nodeCounts = builder.nodeCountsFrom(events);
        for (String service : graph.nodes()) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", service);
            node.put("events", nodeCounts.getOrDefault(service, 0));
            node.put("outDegree", graph.outDegree(service));
            node.put("inDegree", graph.inDegree(service));
            nodes.add(node);
        }
        List<Map<String, Object>> edges = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : trail.entrySet()) {
            String[] parts = entry.getKey().split("->", 2);
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("source", parts[0]);
            edge.put("target", parts[1]);
            edge.put("weight", entry.getValue());
            edges.add(edge);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nodes", nodes);
        body.put("edges", edges);
        body.put("nodeCount", graph.nodeCount());
        body.put("edgeCount", graph.edgeCount());
        return body;
    }

    /** Top error and warning messages with their frequencies. */
    @GetMapping("/errors")
    public Map<String, Object> errors(@RequestParam(defaultValue = "10") int limit) {
        List<LogEvent> events = logService.events();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("topError", errorPatternAnalyzer.topError(events));
        body.put("patterns", errorPatternAnalyzer.topErrors(events, limit));
        return body;
    }

    /** Top-K of a fleet dimension: service, endpoint, level, ip. */
    @GetMapping("/top")
    public List<Map<String, Object>> top(@RequestParam(defaultValue = "service") String dimension,
                                         @RequestParam(defaultValue = "5") int limit) {
        TopKFrequentAnalyzer.Dimension dim = switch (dimension.toLowerCase()) {
            case "endpoint" -> TopKFrequentAnalyzer.Dimension.ENDPOINT;
            case "level" -> TopKFrequentAnalyzer.Dimension.LEVEL;
            case "ip" -> TopKFrequentAnalyzer.Dimension.IP;
            default -> TopKFrequentAnalyzer.Dimension.SERVICE;
        };
        return topKFrequentAnalyzer.asBuckets(
                topKFrequentAnalyzer.topK(logService.events(), dim, limit));
    }

    /** Time-window traffic series for the existing charts. */
    @GetMapping("/windows")
    public List<Map<String, Object>> windows(@RequestParam(defaultValue = "10") int buckets) {
        return timeWindowAnalyzer.windows(logService.events(), buckets);
    }
}