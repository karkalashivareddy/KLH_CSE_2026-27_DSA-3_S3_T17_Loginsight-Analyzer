package com.loginsight.query;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryType;

/**
 * Composite routing key of the dispatcher: lets several engines share one {@link QueryType} as long
 * as their {@link AlgorithmType} differs (docs/02 §4).
 */
public record QueryKey(QueryType type, AlgorithmType algorithm) {

    public QueryKey {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(algorithm, "algorithm");
    }

    public static QueryKey of(QueryType type, AlgorithmType algorithm) {
        return new QueryKey(type, algorithm);
    }
}