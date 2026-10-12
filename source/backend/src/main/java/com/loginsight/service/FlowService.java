package com.loginsight.service;

import org.springframework.stereotype.Service;

import com.loginsight.dto.request.FlowRequest;
import com.loginsight.dto.request.MatchingRequest;
import com.loginsight.dto.request.MinCostFlowRequest;
import com.loginsight.dto.request.TreeRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryDispatcher;

/**
 * Orchestrates the flow-family scenarios (docs/12 §3): max-flow (three algorithms), min-cut,
 * bipartite matching and min-cost flow. Each request is dispatched to the registered engine under
 * its (queryType, algorithm) key.
 */
@Service
public class FlowService {

    private final QueryDispatcher dispatcher;

    public FlowService(QueryDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public AlgorithmResultDto fordFulkerson(FlowRequest request) {
        return dispatch(QueryType.SERVICE_FLOW, AlgorithmType.FORD_FULKERSON, request);
    }

    public AlgorithmResultDto edmondsKarp(FlowRequest request) {
        return dispatch(QueryType.SERVICE_FLOW, AlgorithmType.EDMONDS_KARP, request);
    }

    public AlgorithmResultDto dinic(FlowRequest request) {
        return dispatch(QueryType.SERVICE_FLOW, AlgorithmType.DINIC, request);
    }

    public AlgorithmResultDto minCut(FlowRequest request) {
        return dispatch(QueryType.MIN_CUT, AlgorithmType.MIN_CUT, request);
    }

    public AlgorithmResultDto matching(MatchingRequest request) {
        return dispatch(QueryType.MATCHING, AlgorithmType.BIPARTITE_MATCHING, request);
    }

    public AlgorithmResultDto minCost(MinCostFlowRequest request) {
        return dispatch(QueryType.MIN_COST_FLOW, AlgorithmType.MIN_COST_MAX_FLOW, request);
    }

    private AlgorithmResultDto dispatch(QueryType queryType, AlgorithmType algorithm, Object request) {
        QueryContext context = QueryContext.builder(queryType, algorithm).request(request).build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }
}