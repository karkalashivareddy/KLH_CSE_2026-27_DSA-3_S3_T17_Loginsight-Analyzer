package com.loginsight.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.catalog.AlgorithmCatalog;
import com.loginsight.catalog.AlgorithmInfo;
import com.loginsight.service.SearchBenchmarkService;

/**
 * Analysis / insights endpoints (docs/API.md §10): the algorithm catalogue the Algorithm
 * Laboratory screens render from, and the measured search benchmark that runs the four string
 * matchers against the loaded dataset.
 */
@RestController
@RequestMapping("/api/analysis")
public class InsightsController {

    private final SearchBenchmarkService benchmarkService;

    public InsightsController(SearchBenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    /** Exposed algorithms grouped by module, for the analysis and algorithm pages. */
    @GetMapping("/algorithms")
    public List<Map<String, Object>> algorithms() {
        List<Map<String, Object>> groups = new ArrayList<>();
        Map<String, List<AlgorithmInfo>> byModule = new LinkedHashMap<>();
        for (AlgorithmInfo info : AlgorithmCatalog.algorithms()) {
            byModule.computeIfAbsent(info.moduleLabel(), m -> new ArrayList<>()).add(info);
        }
        for (Map.Entry<String, List<AlgorithmInfo>> entry : byModule.entrySet()) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("module", entry.getKey());
            List<Map<String, Object>> items = new ArrayList<>();
            for (AlgorithmInfo info : entry.getValue()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("key", info.key());
                item.put("name", info.name());
                item.put("problem", info.problem());
                item.put("queryType", info.queryType());
                item.put("algorithmType", info.algorithmType());
                item.put("canonicalEndpoint", info.canonicalEndpoint());
                item.put("traceEndpoint", info.traceEndpoint());
                item.put("timeComplexity", info.timeComplexity());
                item.put("spaceComplexity", info.spaceComplexity());
                item.put("tracked", info.tracked());
                item.put("description", info.description());
                items.add(item);
            }
            group.put("algorithms", items);
            groups.add(group);
        }
        return groups;
    }

    /** Measured search benchmark: same pattern, same haystack, four matchers (docs/API.md §10). */
    @GetMapping("/benchmarks/search")
    public Map<String, Object> searchBenchmark(@RequestParam("pattern") String pattern) {
        return benchmarkService.run(pattern);
    }
}