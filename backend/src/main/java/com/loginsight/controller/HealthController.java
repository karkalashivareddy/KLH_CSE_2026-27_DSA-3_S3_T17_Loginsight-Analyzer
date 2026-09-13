package com.loginsight.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal liveness endpoint for the Phase 1 skeleton. Confirms the embedded web layer is up.
 * The full health contract (dataset state, algorithm count) is completed once those subsystems
 * exist (Phases 2 and 9).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "loginsight-analyzer");
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}