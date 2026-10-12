package com.loginsight.query.engine.dp;

import java.util.Map;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.tree.RerootingDP;
import com.loginsight.dsa.dp.tree.Tree;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Tree DP: sum of distances to every node via re-rooting (docs/12 §4). The per-root total-distance
 * array is the payload; the two-pass DP state (contribution of each oriented edge) is intermediate.
 */
public final class RerootingEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.TREE_DP;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.REROOTING_DP;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        int[] from = (int[]) context.getParam("from");
        int[] to = (int[]) context.getParam("to");
        int vertexCount = context.getParam("vertexCount") instanceof Integer v ? v : 0;
        if (from == null || to == null || from.length != to.length) {
            throw new InvalidQueryException("from/to tree edges are required and must match");
        }
        QueryValidator.requireBounds(1, vertexCount, 10_000, "vertex count");
        for (int v : from) {
            QueryValidator.requireBounds(0, v, vertexCount - 1, "edge endpoint");
        }
        for (int v : to) {
            QueryValidator.requireBounds(0, v, vertexCount - 1, "edge endpoint");
        }

        long start = System.nanoTime();
        Tree tree = Tree.of(vertexCount, from, to);
        RerootingDP rerooting = new RerootingDP();
        long[] sums = rerooting.sumDistances(tree);
        DpResult dp = rerooting.solve(tree);
        long elapsed = System.nanoTime() - start;

        Map<String, Object> result = Map.of("sumDistances", sums, "vertexCount", vertexCount);
        return Results.measured(type(), algorithm(), vertexCount, start, result,
                dp.getIntermediateData(), 8L * vertexCount, "O(n)",
                "O(n) adjacency + two DP passes",
                "re-rooting lets every root share one traversal's work");
    }
}