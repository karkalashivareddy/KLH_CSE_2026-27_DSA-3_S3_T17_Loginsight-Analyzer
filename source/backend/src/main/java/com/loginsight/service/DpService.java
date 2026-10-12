package com.loginsight.service;

import org.springframework.stereotype.Service;

import com.loginsight.dto.request.AlignmentRequest;
import com.loginsight.dto.request.DistanceRequest;
import com.loginsight.dto.request.HamiltonianRequest;
import com.loginsight.dto.request.MatrixChainRequest;
import com.loginsight.dto.request.ObstRequest;
import com.loginsight.dto.request.SosRequest;
import com.loginsight.dto.request.TreeRequest;
import com.loginsight.dto.request.TspRequest;
import com.loginsight.dto.request.WeightedEditRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryDispatcher;

/**
 * Orchestrates the DP / similarity scenarios (docs/12 §4): edit-distance metrics, global/local
 * alignment, interval DP (matrix chain, OBST), bitmask DP (TSP, Hamiltonian) and tree/SOS DP. Each
 * service call resolves the request into context params the {@link QueryDispatcher} routes on.
 */
@Service
public class DpService {

    private final QueryDispatcher dispatcher;

    public DpService(QueryDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public AlgorithmResultDto levenshtein(DistanceRequest request) {
        return dispatch(QueryType.EDIT_DISTANCE, AlgorithmType.LEVENSHTEIN, request);
    }

    public AlgorithmResultDto damerau(DistanceRequest request) {
        return dispatch(QueryType.EDIT_DISTANCE, AlgorithmType.DAMERAU_LEVENSHTEIN, request);
    }

    public AlgorithmResultDto weightedEdit(WeightedEditRequest request) {
        return dispatch(QueryType.EDIT_DISTANCE, AlgorithmType.WEIGHTED_EDIT_DISTANCE, request);
    }

    public AlgorithmResultDto global(AlignmentRequest request) {
        return dispatch(QueryType.GLOBAL_ALIGNMENT, AlgorithmType.NEEDLEMAN_WUNSCH, request);
    }

    public AlgorithmResultDto local(AlignmentRequest request) {
        return dispatch(QueryType.LOCAL_ALIGNMENT, AlgorithmType.SMITH_WATERMAN, request);
    }

    public AlgorithmResultDto matrixChain(MatrixChainRequest request) {
        Integer vertexCount = null;
        QueryContext context = QueryContext
                .builder(QueryType.INTERVAL_DP, AlgorithmType.MATRIX_CHAIN)
                .request(request)
                .param("dims", request.dims())
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    public AlgorithmResultDto obst(ObstRequest request) {
        QueryContext context = QueryContext
                .builder(QueryType.INTERVAL_DP, AlgorithmType.OPTIMAL_BINARY_SEARCH_TREE)
                .request(request)
                .param("freqs", request.freqs())
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    public AlgorithmResultDto tsp(TspRequest request) {
        QueryContext context = QueryContext
                .builder(QueryType.BITMASK_DP, AlgorithmType.BITMASK_TSP)
                .request(request)
                .param("costs", request.costs())
                .param("start", request.start())
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    public AlgorithmResultDto hamiltonian(HamiltonianRequest request) {
        int vertexCount = trailingVertexCount(request.from(), request.to());
        QueryContext context = QueryContext
                .builder(QueryType.BITMASK_DP, AlgorithmType.HAMILTONIAN_PATH)
                .request(request)
                .param("from", request.from())
                .param("to", request.to())
                .param("start", request.start())
                .param("vertexCount", vertexCount)
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    public AlgorithmResultDto tree(TreeRequest request, AlgorithmType variant) {
        int vertexCount = request.vertexCount() != null && request.vertexCount() > 0
                ? request.vertexCount() : trailingVertexCount(request.from(), request.to());
        QueryContext context = QueryContext
                .builder(QueryType.TREE_DP, variant)
                .request(request)
                .param("from", request.from())
                .param("to", request.to())
                .param("vertexCount", vertexCount)
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    public AlgorithmResultDto sos(SosRequest request) {
        QueryContext context = QueryContext
                .builder(QueryType.SOS_DP, AlgorithmType.SOS_DP)
                .request(request)
                .param("values", request.values())
                .param("bits", request.bits())
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    private AlgorithmResultDto dispatch(QueryType queryType, AlgorithmType algorithm, Object request) {
        QueryContext context = QueryContext.builder(queryType, algorithm).request(request).build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    private static int trailingVertexCount(int[] from, int[] to) {
        int max = -1;
        if (from != null) {
            for (int v : from) {
                max = Math.max(max, v);
            }
        }
        if (to != null) {
            for (int v : to) {
                max = Math.max(max, v);
            }
        }
        return max + 1;
    }
}