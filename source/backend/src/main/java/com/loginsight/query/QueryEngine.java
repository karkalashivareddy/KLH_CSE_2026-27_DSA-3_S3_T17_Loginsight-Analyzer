package com.loginsight.query;

import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;

/**
 * Strategy contract for every algorithmic scenario (docs/02 §4). Engines are plain Spring-less
 * classes registered with the {@link QueryDispatcher} under a composite {@code (queryType,
 * algorithm)} key, so several engines may share one {@link QueryType} umbrella (e.g. EDIT_DISTANCE
 * for Levenshtein, Damerau and weighted variants). Engines receive a fully-resolved
 * {@link QueryContext} and never touch the dataset or HTTP request themselves.
 */
public interface QueryEngine {

    QueryType type();

    AlgorithmType algorithm();

    QueryResult execute(QueryContext context);
}