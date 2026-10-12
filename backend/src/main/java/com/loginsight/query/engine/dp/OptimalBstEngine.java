package com.loginsight.query.engine.dp;

import java.util.Map;

import com.loginsight.dsa.dp.interval.OptimalBinarySearchTree;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Interval DP: optimal binary search tree over a sorted key sequence with access frequencies
 * (docs/12 §4). The minimal expected search cost is the payload; the cost / root tables are
 * intermediate evidence.
 */
public final class OptimalBstEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.INTERVAL_DP;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.OPTIMAL_BINARY_SEARCH_TREE;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        long[] freqs = (long[]) context.getParam("freqs");
        if (freqs == null || freqs.length == 0) {
            throw new InvalidQueryException("freqs must list at least one access frequency");
        }
        QueryValidator.requireBounds(1, freqs.length, 400, "key count");

        long start = System.nanoTime();
        OptimalBinarySearchTree solver = new OptimalBinarySearchTree();
        long cost = solver.optimalCost(freqs);
        int[][] roots = solver.rootTable(freqs);
        Map<String, Object> result = Map.of("optimalCost", cost, "keys", freqs.length);
        return Results.measured(type(), algorithm(), freqs.length, start, result, roots,
                (long) roots.length * (roots.length == 0 ? 0 : roots[0].length) * 8,
                "O(n³)", "O(n²) cost/root tables",
                "Knuth's interval DP; ties broken toward the first ideal root");
    }
}