package com.loginsight.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.request.IncidentCoverRequest;
import com.loginsight.dto.request.SetCoverRequest;
import com.loginsight.dto.request.VertexCoverRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.service.ApproximationService;

/**
 * Approximation-algorithm endpoints (docs/12 §7): greedy vertex cover on the service graph, the
 * incident-on-call cover and the greedy set cover.
 */
@RestController
@RequestMapping("/api/approx")
public class ApproximationController {

    private final ApproximationService approximationService;

    public ApproximationController(ApproximationService approximationService) {
        this.approximationService = approximationService;
    }

    @PostMapping("/vertex-cover")
    public AlgorithmResultDto vertexCover(@RequestBody VertexCoverRequest request) {
        return approximationService.vertexCover(request);
    }

    @PostMapping("/incident-cover")
    public AlgorithmResultDto incidentCover(@RequestBody IncidentCoverRequest request) {
        return approximationService.incidentCover(request);
    }

    @PostMapping("/set-cover")
    public AlgorithmResultDto setCover(@RequestBody SetCoverRequest request) {
        return approximationService.setCover(request);
    }
}