package com.loginsight.query.engine.dp;

import java.util.Map;

import com.loginsight.dsa.dp.interval.MatrixChainMultiplication;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Interval DP: chain-matrix multiplication criterion over a dimension vector (docs/12 §4). The
 * optimal parenthesization string and the min scalar cost are the payload; cost and split matrices
 * are intermediate evidence.
 */
public final class MatrixChainEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.INTERVAL_DP;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.MATRIX_CHAIN;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        int[] dims = (int[]) context.getParam("dims");
        if (dims == null || dims.length < 2) {
            throw new InvalidQueryException("dims must list at least 2 dimensions");
        }
        for (int dim : dims) {
            if (dim <= 0) {
                throw new InvalidQueryException("dimensions must be positive");
            }
        }
        long maxProducts = (long) (dims.length - 1) * (dims.length - 1);
        QueryValidator.requireBounds(2, dims.length, 200, "matrix count");

        long start = System.nanoTime();
        MatrixChainMultiplication solver = new MatrixChainMultiplication();
        long cost = solver.minCost(dims);
        String parenthesization = solver.parenthesization(dims);
        long[][] costMatrix = solver.computeCost(dims);
        int[][] splitTable = solver.splitTable(dims);

        Map<String, Object> result = Map.of("minCost", cost, "parenthesization", parenthesization);
        Map<String, Object> intermediate = Map.of("costMatrix", costMatrix,
                "splitTable", splitTable, "matrixCount", dims.length - 1);
        return Results.measured(type(), algorithm(), (int) maxProducts + dims.length, start, result,
                intermediate, ((long) costMatrix.length * (costMatrix.length == 0 ? 0 : costMatrix[0].length)
                        + (long) splitTable.length * (splitTable.length == 0 ? 0 : splitTable[0].length)) * 8,
                "O(n³)", "O(n²) cost + split tables",
                "interval DP over the dimension vector");
    }
}