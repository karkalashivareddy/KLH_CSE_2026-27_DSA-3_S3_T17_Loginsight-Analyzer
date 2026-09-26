package com.loginsight.query.engine.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.dp.editdistance.LevenshteinDistance;
import com.loginsight.dto.request.FuzzyRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Approximate search over the dataset's log lines (docs/12 §1): each line's edit distance to the
 * query is computed with the Levenshtein DP table; lines within {@code maxDistance} are returned
 * ranked by increasing distance. The per-line distance array doubles as intermediate evidence.
 */
public final class FuzzySearchEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.FUZZY_SEARCH;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.LEVENSHTEIN;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        FuzzyRequest request = (FuzzyRequest) context.getRequest();
        QueryValidator.requireNotBlank(request.query(), "query");
        if (request.maxDistance() == null || request.maxDistance() < 0) {
            throw new com.loginsight.exception.InvalidQueryException("maxDistance must be >= 0");
        }
        String text = context.getText();
        String[] lines = text.split("\n", -1);
        QueryValidator.requireFuzzyBudget(lines.length, request.query().length());
        LevenshteinDistance distance = new LevenshteinDistance();

        long start = System.nanoTime();
        List<long[]> distances = new ArrayList<>();
        List<Map<String, Object>> matches = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            long d = distance.distance(lines[i], request.query());
            distances.add(new long[]{i, d});
            if (d <= request.maxDistance()) {
                matches.add(Map.of("lineNumber", i, "text", lines[i], "distance", d));
            }
        }
        matches.sort(java.util.Comparator
                .comparingLong((Map<String, Object> m) -> ((Number) m.get("distance")).longValue())
                .thenComparingInt(m -> ((Number) m.get("lineNumber")).intValue()));
        long elapsed = System.nanoTime() - start;

        Map<String, Object> result = Map.of("totalMatches", matches.size(),
                "maxDistance", request.maxDistance(), "matches", matches);
        return Results.measured(type(), algorithm(), text.length(), start, result, distances,
                text.length() * 16L, "O(lines · |query|)", "O(|query|) rolling window per line",
                "edit-distance fuzzy match against the loaded dataset lines");
    }
}