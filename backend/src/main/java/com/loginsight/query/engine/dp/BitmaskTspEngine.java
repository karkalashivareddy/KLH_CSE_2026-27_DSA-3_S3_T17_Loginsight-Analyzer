package com.loginsight.query.engine.dp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.bitmask.BitmaskTSP;
import com.loginsight.dsa.dp.bitmask.TspResult;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Bitmask DP: travelling-salesman tour over a distance matrix (docs/12 §4). The closed tour and its
 * cost are the payload; the DP table over {@code (mask, last-city)} is intermediate evidence.
 */
public final class BitmaskTspEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.BITMASK_DP;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.BITMASK_TSP;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        long[][] costs = (long[][]) context.getParam("costs");
        if (costs == null || costs.length == 0 || costs.length != costs[0].length) {
            throw new InvalidQueryException("costs must be a square matrix with n >= 1 cities");
        }
        int startCity = context.getParam("start") instanceof Integer i ? i : 0;
        QueryValidator.requireBounds(0, startCity, costs.length - 1, "start");
        QueryValidator.requireBounds(1, costs.length, 14, "city count");

        long start = System.nanoTime();
        BitmaskTSP tsp = new BitmaskTSP(costs, startCity);
        TspResult tour = tsp.solve();
        DpResult dp = tsp.match();
        long elapsed = System.nanoTime() - start;

        List<Integer> path = new ArrayList<>();
        for (int city : tour.getPath()) {
            path.add(city);
        }
        Map<String, Object> result = Map.of("hasTour", tour.hasTour(),
                "minCost", tour.getMinCost(), "path", path);
        int maskWidth = 1 << costs.length;
        return Results.measured(type(), algorithm(), costs.length, start, result,
                dp.getIntermediateData(), (long) maskWidth * costs.length * 8,
                "O(n²·2ⁿ)", "O(n·2ⁿ)",
                "held & karp over masks; start city " + startCity + (tour.hasTour()
                        ? ", closed tour" : ", no tour returned"));
    }
}