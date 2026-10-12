package com.loginsight.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.QueryResult;

/**
 * Routes a fully-resolved {@link QueryContext} to its {@link QueryEngine} under the composite
 * {@link QueryKey} {@code (queryType, algorithm)} (docs/02 §4). Engines are collected by Spring,
 * which keeps the engine classes free of annotations; an unregistered key is an internal wiring
 * error surfaced as a 400-domain message.
 */
@Component
public class QueryDispatcher {

    private final Map<QueryKey, QueryEngine> engines = new LinkedHashMap<>();

    public QueryDispatcher(List<QueryEngine> engineList) {
        if (engineList != null) {
            for (QueryEngine engine : engineList) {
                engines.put(QueryKey.of(engine.type(), engine.algorithm()), engine);
            }
        }
    }

    /**
     * Dispatch to the engine charged with the context's (queryType, algorithm) pair.
     *
     * @throws InvalidQueryException if no engine is registered for the pair
     */
    public QueryResult dispatch(QueryContext context) {
        QueryKey key = QueryKey.of(context.getQueryType(), context.getAlgorithm());
        QueryEngine engine = engines.get(key);
        if (engine == null) {
            throw new InvalidQueryException("No engine is registered for " + key);
        }
        return engine.execute(context);
    }

    public int engineCount() {
        return engines.size();
    }
}