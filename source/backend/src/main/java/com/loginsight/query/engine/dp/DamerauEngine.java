package com.loginsight.query.engine.dp;

import java.util.List;
import java.util.Map;

import com.loginsight.dsa.dp.editdistance.DamerauLevenshteinDistance;
import com.loginsight.dsa.dp.editdistance.EditDistanceResult;
import com.loginsight.dto.request.DistanceRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Damerau-Levenshtein distance (docs/12 §4): adds the adjacent-transposition move to the edit
 * metric. The operation list distinguishes {@code TRANSPOSE} from edit/insert/delete.
 */
public final class DamerauEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.EDIT_DISTANCE;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.DAMERAU_LEVENSHTEIN;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        DistanceRequest request = (DistanceRequest) context.getRequest();
        String a = QueryValidator.requireNotBlank(request.a(), "a");
        String b = QueryValidator.requireNotBlank(request.b(), "b");
        QueryValidator.requireQuadraticSequences(a, b);
        boolean showMatrix = Boolean.TRUE.equals(request.showMatrix());

        long start = System.nanoTime();
        EditDistanceResult edit = new DamerauLevenshteinDistance().reconstruct(a, b);
        Map<String, Object> result = Map.of("distance", edit.getDistance(), "a", a, "b", b,
                "operations", List.of(edit.getOperations()));
        Object intermediate = showMatrix ? edit.getMatrix() : null;
        long memory = showMatrix ? (long) edit.getMatrix().length * (edit.getMatrix().length == 0
                ? 0 : edit.getMatrix()[0].length) * 8 : 0;
        return Results.measured(type(), algorithm(), a.length() + b.length(), start, result,
                intermediate, memory, "O(|a|·|b|)", "O(|a|·|b|) DP table",
                DamerauLevenshteinDistance.variant() + " metric with transposition move");
    }
}