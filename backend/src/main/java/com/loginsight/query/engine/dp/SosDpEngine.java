package com.loginsight.query.engine.dp;

import java.util.Map;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.sos.SOSDP;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.Results;

/**
 * Sum-over-subsets DP (docs/12 §4): for a value array of size {@code 2^bits}, computes every
 * {@code subsetSum[mask]} across submask transitions in O(bits·2^bits) instead of O(3^bits). The
 * computed array is the payload; the DP matrix is intermediate.
 */
public final class SosDpEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.SOS_DP;
    }

    @Override
    public com.loginsight.model.AlgorithmType algorithm() {
        return com.loginsight.model.AlgorithmType.SOS_DP;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        long[] values = (long[]) context.getParam("values");
        int bits = context.getParam("bits") instanceof Integer b ? b : 0;
        if (values == null) {
            throw new InvalidQueryException("values must not be null");
        }
        int expected = 1 << bits;
        if (values.length != expected || values.length < 2 || values.length > (1 << 16)) {
            throw new InvalidQueryException(
                    "values must contain exactly 2^bits elements (2^" + bits + " = " + expected + ")");
        }
        long start = System.nanoTime();
        SOSDP solver = new SOSDP();
        long[] sums = solver.subsetSums(values, bits);
        DpResult dp = solver.solve(values, bits);
        long elapsed = System.nanoTime() - start;

        Map<String, Object> result = Map.of("subsetSums", sums, "bits", bits, "size", sums.length);
        return Results.measured(type(), algorithm(), values.length, start, result,
                dp.getIntermediateData(), (long) (bits + 1) * values.length * 8,
                "O(bits·2^bits)", "O(2^bits) DP layers",
                "submask enumeration replaced by the SOS transition (docs/12 §4)");
    }
}