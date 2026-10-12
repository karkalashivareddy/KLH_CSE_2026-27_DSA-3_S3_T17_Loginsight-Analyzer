package com.loginsight.query.engine.dp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.bitmask.HamiltonianPath;
import com.loginsight.dsa.dp.bitmask.HamiltonianPathResult;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Bitmask DP: Hamiltonian path over an undirected graph (docs/12 §4). The path (if one exists) from
 * a chosen start is the payload; the per-mask reachability table is intermediate evidence.
 */
public final class HamiltonianEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.BITMASK_DP;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.HAMILTONIAN_PATH;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        int[] from = (int[]) context.getParam("from");
        int[] to = (int[]) context.getParam("to");
        int vertexCount = context.getParam("vertexCount") instanceof Integer v ? v : 0;
        int start = context.getParam("start") instanceof Integer s ? s : 0;
        if (from == null || to == null || from.length != to.length) {
            throw new InvalidQueryException("from/to edge endpoints are required and must match");
        }
        QueryValidator.requireBounds(0, start, Math.max(0, vertexCount - 1), "start");
        QueryValidator.requireBounds(2, vertexCount, 20, "vertex count");

        long begin = System.nanoTime();
        HamiltonianPath hp = new HamiltonianPath(vertexCount);
        for (int v : from) {
            QueryValidator.requireBounds(0, v, vertexCount - 1, "edge endpoint");
        }
        for (int v : to) {
            QueryValidator.requireBounds(0, v, vertexCount - 1, "edge endpoint");
        }
        hp.addEdge(from, to);
        HamiltonianPathResult found = hp.find(start);
        DpResult dp = hp.match();
        long elapsed = System.nanoTime() - begin;

        List<Integer> path = new ArrayList<>();
        for (int v : found.getPath()) {
            path.add(v);
        }
        Map<String, Object> result = Map.of("exists", found.exists(), "path", path,
                "start", start, "vertexCount", vertexCount);
        return Results.measured(type(), algorithm(), vertexCount, begin, result,
                dp.getIntermediateData(), (long) (1 << vertexCount) * vertexCount * 8,
                "O(n·2ⁿ)", "O(n·2ⁿ)",
                found.exists() ? "Hamiltonian path found from vertex " + start
                        : "no Hamiltonian path through every vertex exists");
    }
}