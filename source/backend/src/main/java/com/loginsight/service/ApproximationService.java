package com.loginsight.service;

import org.springframework.stereotype.Service;

import com.loginsight.dto.request.IncidentCoverRequest;
import com.loginsight.dto.request.SetCoverRequest;
import com.loginsight.dto.request.VertexCoverRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryDispatcher;

/**
 * Orchestrates the approximation scenarios (docs/12 §7): vertex cover, incident-on-call cover and
 * greedy set cover. Routes to the approximation engines regardless of the UI used to send them.
 */
@Service
public class ApproximationService {

    private final QueryDispatcher dispatcher;

    public ApproximationService(QueryDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public AlgorithmResultDto vertexCover(VertexCoverRequest request) {
        return dispatch(QueryType.APPROXIMATE_COVER, AlgorithmType.VERTEX_COVER, request);
    }

    public AlgorithmResultDto incidentCover(IncidentCoverRequest request) {
        return dispatch(QueryType.APPROXIMATE_COVER, AlgorithmType.MAXIMAL_MATCHING, request);
    }

    public AlgorithmResultDto setCover(SetCoverRequest request) {
        return dispatch(QueryType.SET_COVER, AlgorithmType.SET_COVER, request);
    }

    private AlgorithmResultDto dispatch(QueryType queryType, AlgorithmType algorithm, Object request) {
        QueryContext context = QueryContext.builder(queryType, algorithm).request(request).build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }
}