package com.loginsight.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.request.FlowRequest;
import com.loginsight.dto.request.MatchingRequest;
import com.loginsight.dto.request.MinCostFlowRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.service.FlowService;

/**
 * Flow-family endpoints (docs/12 §3): max-flow with three algorithms, min-cut, bipartite matching
 * and min-cost flow over the service graph.
 */
@RestController
@RequestMapping("/api/flow")
public class FlowController {

    private final FlowService flowService;

    public FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    @PostMapping
    public AlgorithmResultDto fordFulkerson(@RequestBody FlowRequest request) {
        return flowService.fordFulkerson(request);
    }

    @PostMapping("/edmonds-karp")
    public AlgorithmResultDto edmondsKarp(@RequestBody FlowRequest request) {
        return flowService.edmondsKarp(request);
    }

    @PostMapping("/dinic")
    public AlgorithmResultDto dinic(@RequestBody FlowRequest request) {
        return flowService.dinic(request);
    }

    @PostMapping("/min-cut")
    public AlgorithmResultDto minCut(@RequestBody FlowRequest request) {
        return flowService.minCut(request);
    }

    @PostMapping("/matching")
    public AlgorithmResultDto matching(@RequestBody MatchingRequest request) {
        return flowService.matching(request);
    }

    @PostMapping("/min-cost")
    public AlgorithmResultDto minCost(@RequestBody MinCostFlowRequest request) {
        return flowService.minCost(request);
    }
}