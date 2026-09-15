package com.loginsight.query.engine.dp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.dp.editdistance.EditDistanceResult;
import com.loginsight.dsa.dp.editdistance.LevenshteinDistance;
import com.loginsight.dto.request.DistanceRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Levenshtein DP distance (docs/12 §4). The full edit matrix is exposed as intermediate data when
 * requested; the traceback operations (insert/delete/substitute) form the primary result payload.
 */
public final class LevenshteinEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.EDIT_DISTANCE;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.LEVENSHTEIN;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        DistanceRequest request = (DistanceRequest) context.getRequest();
        String a = QueryValidator.requireNotBlank(request.a(), "a");
        String b = QueryValidator.requireNotBlank(request.b(), "b");
        boolean showMatrix = Boolean.TRUE.equals(request.showMatrix());

        long start = System.nanoTime();
        EditDistanceResult edit = new LevenshteinDistance().reconstruct(a, b);
        Map<String, Object> result = Map.of("distance", edit.getDistance(), "a", a, "b", b,
                "operations", List.of(edit.getOperations()));
        Object intermediate = showMatrix ? edit.getMatrix() : null;
        long memory = showMatrix ? (long) edit.getMatrix().length * (edit.getMatrix().length == 0
                ? 0 : edit.getMatrix()[0].length) * 8 : 0;
        return Results.measured(type(), algorithm(), a.length() + b.length(), start, result,
                intermediate, memory, "O(|a|·|b|)", "O(|a|·|b|) DP table",
                showMatrix ? "full DP matrix exposed" : "matrix suppressed (showMatrix=false)");
    }
}