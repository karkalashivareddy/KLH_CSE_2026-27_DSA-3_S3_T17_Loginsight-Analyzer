package com.loginsight.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.loginsight.service.DpService;

/**
 * DP and document-similarity endpoints (docs/12 §4): edit-distance metrics, Needleman-Wunsch /
 * Smith-Waterman alignment, interval DP (matrix chain, OBST), bitmask DP (TSP, Hamiltonian), tree
 * DP (diameter, re-rooting) and sum-over-subsets.
 */
@RestController
@RequestMapping("/api/dp")
public class DpController {

    private final DpService dpService;

    public DpController(DpService dpService) {
        this.dpService = dpService;
    }

    @PostMapping("/levenshtein")
    public AlgorithmResultDto levenshtein(@RequestBody DistanceRequest request) {
        return dpService.levenshtein(request);
    }

    @PostMapping("/damerau")
    public AlgorithmResultDto damerau(@RequestBody DistanceRequest request) {
        return dpService.damerau(request);
    }

    @PostMapping("/weighted-edit")
    public AlgorithmResultDto weightedEdit(@RequestBody WeightedEditRequest request) {
        return dpService.weightedEdit(request);
    }

    @PostMapping("/global")
    public AlgorithmResultDto global(@RequestBody AlignmentRequest request) {
        return dpService.global(request);
    }

    @PostMapping("/local")
    public AlgorithmResultDto local(@RequestBody AlignmentRequest request) {
        return dpService.local(request);
    }

    @PostMapping("/matrix-chain")
    public AlgorithmResultDto matrixChain(@RequestBody MatrixChainRequest request) {
        return dpService.matrixChain(request);
    }

    @PostMapping("/obst")
    public AlgorithmResultDto obst(@RequestBody ObstRequest request) {
        return dpService.obst(request);
    }

    @PostMapping("/tsp")
    public AlgorithmResultDto tsp(@RequestBody TspRequest request) {
        return dpService.tsp(request);
    }

    @PostMapping("/hamiltonian")
    public AlgorithmResultDto hamiltonian(@RequestBody HamiltonianRequest request) {
        return dpService.hamiltonian(request);
    }

    @PostMapping("/tree")
    public AlgorithmResultDto tree(@RequestBody TreeRequest request) {
        return dpService.tree(request, AlgorithmType.TREE_DIAMETER);
    }

    @PostMapping("/rerooting")
    public AlgorithmResultDto rerooting(@RequestBody TreeRequest request) {
        return dpService.tree(request, AlgorithmType.REROOTING_DP);
    }

    @PostMapping("/sos")
    public AlgorithmResultDto sos(@RequestBody SosRequest request) {
        return dpService.sos(request);
    }
}