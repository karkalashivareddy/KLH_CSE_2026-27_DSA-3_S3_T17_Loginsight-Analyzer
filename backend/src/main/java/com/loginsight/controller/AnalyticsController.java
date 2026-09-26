package com.loginsight.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.analytics.ErrorPatternAnalyzer;
import com.loginsight.analytics.FleetAnalyzer;
import com.loginsight.analytics.HeatmapAnalyzer;
import com.loginsight.analytics.TimeWindowAnalyzer;
import com.loginsight.dto.response.HttpStatsDto;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.graph.ServiceDependencyGraph;
import com.loginsight.graph.ServiceGraphBuilder;
import com.loginsight.graph.TopKFrequentAnalyzer;
import com.loginsight.model.LogEvent;
import com.loginsight.query.QueryValidator;
import com.loginsight.service.LogService;

/**
 * Analytics endpoints (docs/12 §2, §6): service-dependency topology, error patterns, top-K
 * frequencies and the time-window traffic series, plus HTTP and host aggregation. All answers are
 * computed live from the currently loaded dataset through the pure-Java analyzers; nothing is
 * precomputed or fabricated.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final int MAX_ERROR_LIMIT = 100;
    private static final int MAX_TOP_LIMIT = 50;
    private static final int MAX_WINDOW_BUCKETS = 100;
    private static final int MAX_HOST_LIMIT = 200;

    private final LogService logService;
    private final ErrorPatternAnalyzer errorPatternAnalyzer = new ErrorPatternAnalyzer();
    private final TopKFrequentAnalyzer topKFrequentAnalyzer = new TopKFrequentAnalyzer();
    private final TimeWindowAnalyzer timeWindowAnalyzer = new TimeWindowAnalyzer();
    private final FleetAnalyzer fleetAnalyzer = new FleetAnalyzer();
    private final HeatmapAnalyzer heatmapAnalyzer = new HeatmapAnalyzer();

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
        QueryValidator.requireBounds(1, limit, MAX_ERROR_LIMIT, "limit");
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
        QueryValidator.requireBounds(1, limit, MAX_TOP_LIMIT, "limit");
        if (dimension != null && dimension.length() > 32) {
            throw new InvalidQueryException("dimension must be at most 32 characters");
        }
        String normalized = dimension == null ? "" : dimension.trim().toLowerCase(Locale.ROOT);
        TopKFrequentAnalyzer.Dimension dim = switch (normalized) {
            case "service" -> TopKFrequentAnalyzer.Dimension.SERVICE;
            case "endpoint" -> TopKFrequentAnalyzer.Dimension.ENDPOINT;
            case "level" -> TopKFrequentAnalyzer.Dimension.LEVEL;
            case "ip" -> TopKFrequentAnalyzer.Dimension.IP;
            default -> throw new InvalidQueryException(
                    "dimension must be one of service, endpoint, level, ip");
        };
        return topKFrequentAnalyzer.asBuckets(
                topKFrequentAnalyzer.topK(logService.events(), dim, limit));
    }

    /** Time-window traffic series for the existing charts. */
    @GetMapping("/windows")
    public List<Map<String, Object>> windows(@RequestParam(defaultValue = "10") int buckets) {
        QueryValidator.requireBounds(1, buckets, MAX_WINDOW_BUCKETS, "buckets");
        return timeWindowAnalyzer.windows(logService.events(), buckets);
    }

    /** HTTP analytics (status distribution, methods, top endpoints, measured latency percentiles). */
    @GetMapping("/http")
    public HttpStatsDto http() {
        return fleetAnalyzer.http(logService.events());
    }

    /** Per-host rollups: events, errors, error rate (docs/API.md §8). */
    @GetMapping("/hosts")
    public List<Map<String, Object>> hosts(@RequestParam(defaultValue = "25") int limit) {
        QueryValidator.requireBounds(1, limit, MAX_HOST_LIMIT, "limit");
        return fleetAnalyzer.hosts(logService.events(), limit);
    }

    /** Hour-of-day × day-of-week activity heatmap over the loaded dataset. */
    @GetMapping("/heatmap")
    public HeatmapAnalyzer.Heatmap heatmap() {
        return heatmapAnalyzer.hourByWeekday(logService.events());
    }
}