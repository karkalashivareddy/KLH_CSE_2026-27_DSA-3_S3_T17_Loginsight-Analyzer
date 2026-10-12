package com.loginsight.query.engine.flow;

import com.loginsight.dsa.flow.Dinic;
import com.loginsight.dsa.flow.FlowResult;
import com.loginsight.dto.request.FlowRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;

/**
 * Max-flow with Dinic's layered-networks blocking-flow augmenter (docs/12 §3): level graphs by BFS,
 * blocking flows by DFS, iterating until no path to the sink remains — O(V²·E) worst case.
 */
public final class DinicEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.SERVICE_FLOW;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.DINIC;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        FlowGraphFactory.NamedFlow named = FlowGraphFactory.build((FlowRequest) context.getRequest());
        FlowResult flow = new Dinic().maxFlow(named.graph(), named.source(), named.sink());
        return FlowPayloads.wrap(named, flow, type(), algorithm());
    }
}