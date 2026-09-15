package com.loginsight.service;

import org.springframework.stereotype.Service;

import com.loginsight.dto.request.HashRequest;
import com.loginsight.dto.request.MillerRabinRequest;
import com.loginsight.dto.request.QuicksortRequest;
import com.loginsight.dto.request.ReservoirRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryDispatcher;

/**
 * Orchestrates the randomized-algorithm scenarios (docs/12 §5): Miller-Rabin primality, reservoir
 * sampling, universal hashing and randomized quicksort. Seeds are fixed per request for
 * reproducibility, except sampling which uses the sharing-backed unseeded source.
 */
@Service
public class RandomizedService {

    private final QueryDispatcher dispatcher;

    public RandomizedService(QueryDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public AlgorithmResultDto millerRabin(MillerRabinRequest request) {
        QueryContext context = QueryContext
                .builder(QueryType.PRIMALITY_TEST, AlgorithmType.MILLER_RABIN)
                .request(request)
                .param("seed", request.n() ^ 0x5EED)
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    public AlgorithmResultDto sample(ReservoirRequest request) {
        QueryContext context = QueryContext
                .builder(QueryType.STREAM_SAMPLE, AlgorithmType.RESERVOIR_SAMPLING)
                .request(request)
                .values(request.values())
                .param("seed", 42L)
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    public AlgorithmResultDto hash(HashRequest request) {
        return dispatch(QueryType.HASH, AlgorithmType.UNIVERSAL_HASH, request);
    }

    public AlgorithmResultDto quicksort(QuicksortRequest request) {
        return dispatch(QueryType.RANDOMIZED_SORT, AlgorithmType.RANDOMIZED_QUICKSORT, request);
    }

    private AlgorithmResultDto dispatch(QueryType queryType, AlgorithmType algorithm, Object request) {
        QueryContext context = QueryContext.builder(queryType, algorithm).request(request).build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }
}