package com.loginsight.query.engine.string;

import java.util.Map;

import com.loginsight.dsa.string.StringSearchResult;
import com.loginsight.dsa.string.ZAlgorithm;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Z-array matcher: builds the Z-array over {@code pattern + separator + text}; every entry equal to
 * the pattern length is a match. The Z-array itself is exposed as intermediate data.
 */
public final class ZSearchEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.PATTERN_SEARCH;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.Z;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        QueryValidator.requirePattern(context.getPattern());
        StringSearchResult matched = new ZAlgorithm().match(context.getText(), context.getPattern());
        Map<String, Object> result = Map.of("matchCount", matched.getMatchCount(),
                "positions", matched.getMatchPositions());
        int[] z = matched.intermediateDataAsInts();
        return Results.timed(type(), algorithm(), context.getText().length(), matched.getExecutionTimeNanos(),
                result, matched.getIntermediateData(), 4L * z.length,
                matched.getTimeComplexity(), matched.getSpaceComplexity(),
                "Z[i] = length of the longest prefix of the working string matching its suffix at i");
    }
}