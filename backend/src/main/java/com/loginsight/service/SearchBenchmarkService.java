package com.loginsight.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.loginsight.dsa.string.KMPMatcher;
import com.loginsight.dsa.string.NaiveMatcher;
import com.loginsight.dsa.string.RabinKarpMatcher;
import com.loginsight.dsa.string.StringSearchResult;
import com.loginsight.dsa.string.ZAlgorithm;
import com.loginsight.exception.DatasetException;
import com.loginsight.query.QueryContext;

/**
 * Honest search benchmark (docs/API.md §10). All four single-pattern matchers run over the exact
 * same dataset haystack ({@link QueryContext#renderDataset(List)}) with the user's pattern; every
 * reported time is measured on that run. No fabricated numbers, no averaged marketing charts —
 * one measured execution per matcher, winner = smallest measured time.
 */
@Service
public class SearchBenchmarkService {

    private final DatasetService datasetService;

    public SearchBenchmarkService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    public Map<String, Object> run(String pattern) {
        String text = QueryContext.renderDataset(datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"))
                .events());
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("pattern is required");
        }
        List<Match> matches = new ArrayList<>();
        matches.add(run("Naive", new NaiveMatcher(), text, pattern));
        matches.add(run("KMP", new KMPMatcher(), text, pattern));
        matches.add(run("Z-Algorithm", new ZAlgorithm(), text, pattern));
        matches.add(run("Rabin-Karp", new RabinKarpMatcher(), text, pattern));

        matches.sort((a, b) -> Long.compare(a.executionTimeNanos, b.executionTimeNanos));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("problem", "PATTERN_SEARCH");
        body.put("dataset", datasetService.currentDataset().map(d -> d.name()).orElse(""));
        body.put("pattern", pattern);
        body.put("textLength", text.length());
        body.put("textPreview", text.length() <= 140
                ? text : text.substring(0, 140) + "\u2026");
        List<Map<String, Object>> results = new ArrayList<>();
        for (Match match : matches) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("algorithm", match.algorithm);
            row.put("matchCount", match.result.getMatchCount());
            row.put("timeNanos", match.executionTimeNanos);
            row.put("timeMs", match.executionTimeNanos / 1_000_000.0);
            row.put("timeComplexity", match.result.getTimeComplexity());
            row.put("spaceComplexity", match.result.getSpaceComplexity());
            results.add(row);
        }
        body.put("results", results);
        body.put("winner", matches.get(0).algorithm);
        body.put("note", "Measured once per matcher over the loaded dataset haystack — "
                + "no fabricated averages.");
        return body;
    }

    private static Match run(String algorithm, com.loginsight.dsa.string.StringMatcher matcher,
                             String text, String pattern) {
        StringSearchResult result = matcher.match(text, pattern);
        return new Match(algorithm, result.getExecutionTimeNanos(), result);
    }

    private record Match(String algorithm, long executionTimeNanos, StringSearchResult result) {
    }
}