package com.loginsight.query.engine.dp;

import java.util.List;
import java.util.Map;

import com.loginsight.dsa.dp.editdistance.EditDistanceResult;
import com.loginsight.dsa.dp.editdistance.WeightedEditDistance;
import com.loginsight.dto.request.WeightedEditRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Weighted edit distance (docs/12 §4): insert/delete/substitute each carry an explicit cost,
 * defaulting to 1 (plain Levenshtein). The per-operation costs are surfaced in the result payload.
 */
public final class WeightedEditEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.EDIT_DISTANCE;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.WEIGHTED_EDIT_DISTANCE;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        WeightedEditRequest request = (WeightedEditRequest) context.getRequest();
        String a = QueryValidator.requireNotBlank(request.a(), "a");
        String b = QueryValidator.requireNotBlank(request.b(), "b");

        WeightedEditDistance weighted = new WeightedEditDistance(request.insertCost(),
                request.deleteCost(), request.substituteCost());

        long start = System.nanoTime();
        EditDistanceResult edit = weighted.reconstruct(a, b);
        Map<String, Object> result = Map.of("distance", edit.getDistance(), "a", a, "b", b,
                "insertCost", request.insertCost(), "deleteCost", request.deleteCost(),
                "substituteCost", request.substituteCost(),
                "operations", List.of(edit.getOperations()));
        return Results.measured(type(), algorithm(), a.length() + b.length(), start, result, null,
                0L, "O(|a|·|b|)", "O(|a|·|b|) DP table",
                "Levenshtein generalization with per-operation weights");
    }
}