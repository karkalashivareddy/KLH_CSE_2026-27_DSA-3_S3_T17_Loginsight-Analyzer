package com.loginsight.query.engine.dp;

import java.util.Map;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.tree.Tree;
import com.loginsight.dsa.dp.tree.TreeDiameterDP;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Tree DP: diameter of an unweighted tree (docs/12 §4). The diameter length and its endpoint pair
 * are the payload; the two-pass visit evidence (first BFS extremes, then DP) is intermediate.
 */
public final class TreeDpEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.TREE_DP;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.TREE_DIAMETER;
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
        TreeDiameterDP diameterSolver = new TreeDiameterDP();
        long diameter = diameterSolver.diameter(tree);
        int[] endpoints = diameterSolver.endpoints(tree);
        DpResult dp = diameterSolver.solve(tree);
        long elapsed = System.nanoTime() - start;

        Map<String, Object> result = Map.of("diameter", diameter, "endpoints", endpoints,
                "vertexCount", vertexCount);
        return Results.measured(type(), algorithm(), vertexCount, start, result,
                dp.getIntermediateData(), 4L * vertexCount, "O(n)",
                "O(n) adjacency + DP state",
                "diameter computed by the classic two-sweep/DP method");
    }
}