package com.loginsight.query.engine.flow;

import com.loginsight.dsa.flow.FlowResult;
import com.loginsight.dsa.flow.FordFulkerson;
import com.loginsight.dto.request.FlowRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;

/**
 * Max-flow with the Ford-Fulkerson DFS augmenting-path search over the service graph (docs/12 §3).
 * The augmentation count is reported so the O(E·f) profile stays visible.
 */
public final class FordFulkersonEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.SERVICE_FLOW;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.FORD_FULKERSON;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        FlowGraphFactory.NamedFlow named = FlowGraphFactory.build((FlowRequest) context.getRequest());
        FlowResult flow = new FordFulkerson().maxFlow(named.graph(), named.source(), named.sink());
        return FlowPayloads.wrap(named, flow, type(), algorithm());
    }
}