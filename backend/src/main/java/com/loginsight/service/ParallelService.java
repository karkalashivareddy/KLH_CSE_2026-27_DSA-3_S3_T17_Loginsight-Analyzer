package com.loginsight.service;

import org.springframework.stereotype.Service;

import com.loginsight.dto.request.ParallelReduceRequest;
import com.loginsight.dto.request.ParallelScanRequest;
import com.loginsight.dto.request.ParallelSortRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryDispatcher;

/**
 * Orchestrates the parallel scenarios (docs/12 §8): parallel reduction, prefix scan and sort.
 * Threading is internal to the engines; these endpoints report the deterministic-witness values the
 * acceptance tests assert on.
 */
@Service
public class ParallelService {

    private final QueryDispatcher dispatcher;

    public ParallelService(QueryDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public AlgorithmResultDto reduce(ParallelReduceRequest request) {
        return dispatch(QueryType.PARALLEL_REDUCE, AlgorithmType.PARALLEL_REDUCE, request);
    }

    public AlgorithmResultDto scan(ParallelScanRequest request) {
        return dispatch(QueryType.PARALLEL_SCAN, AlgorithmType.PARALLEL_PREFIX_SCAN, request);
    }

    public AlgorithmResultDto sort(ParallelSortRequest request) {
        return dispatch(QueryType.PARALLEL_SORT, AlgorithmType.PARALLEL_SORT, request);
    }

    private AlgorithmResultDto dispatch(QueryType queryType, AlgorithmType algorithm, Object request) {
        QueryContext context = QueryContext.builder(queryType, algorithm).request(request).build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }
}