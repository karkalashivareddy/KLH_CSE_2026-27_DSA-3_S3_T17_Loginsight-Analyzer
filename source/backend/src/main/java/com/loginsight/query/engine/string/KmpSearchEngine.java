package com.loginsight.query.engine.string;

import java.util.Map;

import com.loginsight.dsa.string.KMPMatcher;
import com.loginsight.dsa.string.StringSearchResult;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * KMP matcher with its hand-built LPS failure function exposed as intermediate data. The LPS array
 * and the measured pass come from the KMP engine itself.
 */
public final class KmpSearchEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.PATTERN_SEARCH;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.KMP;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        QueryValidator.requirePattern(context.getPattern());
        StringSearchResult matched = new KMPMatcher().match(context.getText(), context.getPattern());
        Map<String, Object> result = Map.of("matchCount", matched.getMatchCount(),
                "positions", matched.getMatchPositions());
        return Results.timed(type(), algorithm(), context.getText().length(), matched.getExecutionTimeNanos(),
                result, matched.getIntermediateData(), 4L * matched.intermediateDataAsInts().length,
                matched.getTimeComplexity(), matched.getSpaceComplexity(),
                "LPS failure function lets the text cursor never move backwards");
    }
}