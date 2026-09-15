package com.loginsight.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.request.DistanceRequest;
import com.loginsight.dto.request.FlowRequest;
import com.loginsight.dto.request.MatrixChainRequest;
import com.loginsight.dto.request.MillerRabinRequest;
import com.loginsight.dto.request.QuicksortRequest;
import com.loginsight.dto.request.ReservoirRequest;
import com.loginsight.dto.request.SearchRequest;
import com.loginsight.dto.request.VertexCoverRequest;
import com.loginsight.dto.response.TraceResponseDto;
import com.loginsight.service.TraceService;

/**
 * Trace API — the Algorithm Laboratory backend. Exposes the trace-capable execution paths of the
 * flagship string, DP, flow, approximation and randomized algorithms, plus the laboratory catalogue.
 *
 * <p>The catalogue is metadata for the workspace; each {@code /api/trace/...} endpoint runs the real
 * instrumented algorithm and returns its ordered steps so the front-end can replay a genuine
 * execution (never an invented one).</p>
 */
@RestController
@RequestMapping("/api/trace")
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/catalog")
    public List<Map<String, Object>> catalog() {
        return List.copyOf(traceService.catalog());
    }

    @PostMapping("/search/naive")
    public TraceResponseDto naive(@RequestBody SearchRequest request) {
        return traceService.naive(request);
    }

    @PostMapping("/search/kmp")
    public TraceResponseDto kmp(@RequestBody SearchRequest request) {
        return traceService.kmp(request);
    }

    @PostMapping("/search/z")
    public TraceResponseDto z(@RequestBody SearchRequest request) {
        return traceService.z(request);
    }

    @PostMapping("/search/rabin-karp")
    public TraceResponseDto rabinKarp(@RequestBody SearchRequest request) {
        return traceService.rabinKarp(request);
    }

    @PostMapping("/dp/levenshtein")
    public TraceResponseDto levenshtein(@RequestBody DistanceRequest request) {
        return traceService.levenshtein(request);
    }

    @PostMapping("/dp/matrix-chain")
    public TraceResponseDto matrixChain(@RequestBody MatrixChainRequest request) {
        return traceService.matrixChain(request);
    }

    @PostMapping("/flow/ford-fulkerson")
    public TraceResponseDto fordFulkerson(@RequestBody FlowRequest request) {
        return traceService.fordFulkerson(request);
    }

    @PostMapping("/flow/edmonds-karp")
    public TraceResponseDto edmondsKarp(@RequestBody FlowRequest request) {
        return traceService.edmondsKarp(request);
    }

    @PostMapping("/flow/dinic")
    public TraceResponseDto dinic(@RequestBody FlowRequest request) {
        return traceService.dinic(request);
    }

    @PostMapping("/approx/vertex-cover")
    public TraceResponseDto vertexCover(@RequestBody VertexCoverRequest request) {
        return traceService.vertexCover(request);
    }

    @PostMapping("/random/quicksort")
    public TraceResponseDto quicksort(@RequestBody QuicksortRequest request) {
        return traceService.quicksort(request);
    }

    @PostMapping("/random/prime")
    public TraceResponseDto millerRabin(@RequestBody MillerRabinRequest request) {
        return traceService.millerRabin(request);
    }

    @PostMapping("/random/sample")
    public TraceResponseDto reservoir(@RequestBody ReservoirRequest request) {
        return traceService.reservoir(request);
    }
}