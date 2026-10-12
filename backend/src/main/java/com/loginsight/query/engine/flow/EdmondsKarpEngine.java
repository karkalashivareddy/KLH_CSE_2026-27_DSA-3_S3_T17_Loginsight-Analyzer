package com.loginsight.query.engine.flow;

import com.loginsight.dsa.flow.EdmondsKarp;
import com.loginsight.dsa.flow.FlowResult;
import com.loginsight.dto.request.FlowRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;

/**
 * Max-flow with Edmonds-Karp, the BFS breadth-first variant of Ford-Fulkerson (docs/12 §3). Using a
 * BFS shortest-path search bounds augmentation count to O(V·E²).
 */
public final class EdmondsKarpEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.SERVICE_FLOW;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.EDMONDS_KARP;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        FlowGraphFactory.NamedFlow named = FlowGraphFactory.build((FlowRequest) context.getRequest());
        FlowResult flow = new EdmondsKarp().maxFlow(named.graph(), named.source(), named.sink());
        return FlowPayloads.wrap(named, flow, type(), algorithm());
    }
}