package com.loginsight.query.engine.string;

import java.util.Map;

import com.loginsight.dsa.string.NaiveMatcher;
import com.loginsight.dsa.string.StringSearchResult;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Baseline pattern matcher — reference implementation every other search engine cross-checks
 * against. Result positions and the measured time come straight from the Naive engine.
 */
public final class NaiveSearchEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.PATTERN_SEARCH;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.NAIVE;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        QueryValidator.requirePattern(context.getPattern());
        StringSearchResult matched = new NaiveMatcher().match(context.getText(), context.getPattern());
        Map<String, Object> result = Map.of("matchCount", matched.getMatchCount(),
                "positions", matched.getMatchPositions());
        return Results.timed(type(), algorithm(), context.getText().length(), matched.getExecutionTimeNanos(),
                result, matched.getIntermediateData(), 0L, matched.getTimeComplexity(),
                matched.getSpaceComplexity(), "Naive scanning baseline: every start position compared from scratch");
    }
}