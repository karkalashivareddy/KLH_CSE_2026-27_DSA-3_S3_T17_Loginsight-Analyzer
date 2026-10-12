package com.loginsight.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.request.HashRequest;
import com.loginsight.dto.request.MillerRabinRequest;
import com.loginsight.dto.request.QuicksortRequest;
import com.loginsight.dto.request.ReservoirRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.service.RandomizedService;

/**
 * Randomized-algorithm endpoints (docs/12 §5): Miller-Rabin primality, one-pass reservoir sampling,
 * universal hashing and randomized quicksort.
 */
@RestController
@RequestMapping("/api/random")
public class RandomizedController {

    private final RandomizedService randomizedService;

    public RandomizedController(RandomizedService randomizedService) {
        this.randomizedService = randomizedService;
    }

    @PostMapping("/prime")
    public AlgorithmResultDto prime(@RequestBody MillerRabinRequest request) {
        return randomizedService.millerRabin(request);
    }

    @PostMapping("/sample")
    public AlgorithmResultDto sample(@RequestBody ReservoirRequest request) {
        return randomizedService.sample(request);
    }

    @PostMapping("/hash")
    public AlgorithmResultDto hash(@RequestBody HashRequest request) {
        return randomizedService.hash(request);
    }

    @PostMapping("/quicksort")
    public AlgorithmResultDto quicksort(@RequestBody QuicksortRequest request) {
        return randomizedService.quicksort(request);
    }
}