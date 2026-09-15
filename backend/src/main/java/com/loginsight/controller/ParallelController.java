package com.loginsight.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.request.BenchmarkRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.dto.response.BenchmarkResultDto;
import com.loginsight.service.BenchmarkService;
import com.loginsight.service.ParallelService;

/**
 * Parallel and benchmark endpoints (docs/12 §8, §10). The parallel scenarios return the canonical
 * AlgorithmResult envelope with verified witnesses; the benchmark run returns the measured
 * sequential-vs-parallel rows.
 */
@RestController
@RequestMapping("/api")
public class ParallelController {

    private final ParallelService parallelService;
    private final BenchmarkService benchmarkService;

    public ParallelController(ParallelService parallelService, BenchmarkService benchmarkService) {
        this.parallelService = parallelService;
        this.benchmarkService = benchmarkService;
    }

    @PostMapping("/parallel/reduce")
    public AlgorithmResultDto reduce(@RequestBody com.loginsight.dto.request.ParallelReduceRequest request) {
        return parallelService.reduce(request);
    }

    @PostMapping("/parallel/scan")
    public AlgorithmResultDto scan(@RequestBody com.loginsight.dto.request.ParallelScanRequest request) {
        return parallelService.scan(request);
    }

    @PostMapping("/parallel/sort")
    public AlgorithmResultDto sort(@RequestBody com.loginsight.dto.request.ParallelSortRequest request) {
        return parallelService.sort(request);
    }

    @PostMapping("/benchmark/run")
    public List<BenchmarkResultDto> benchmark(@RequestBody BenchmarkRequest request) {
        return benchmarkService.run(request);
    }
}