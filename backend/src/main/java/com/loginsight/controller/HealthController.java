package com.loginsight.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.query.QueryDispatcher;
import com.loginsight.service.DatasetService;

/**
 * Health surface of the platform (docs/12 §9): liveness and a dataset readiness probe used by the
 * UI bootstrap and acceptance tests. The dataset probe answers 200/404 depending on whether a
 * dataset has been imported, which the front-end gates on before enabling the search scenarios.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DatasetService datasetService;
    private final QueryDispatcher queryDispatcher;
    private final String serviceName;

    public HealthController(DatasetService datasetService, QueryDispatcher queryDispatcher,
                            @Value("${spring.application.name:loginsight-analyzer}") String serviceName) {
        this.datasetService = datasetService;
        this.queryDispatcher = queryDispatcher;
        this.serviceName = serviceName;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", serviceName);
        body.put("timestamp", Instant.now().toString());
        return body;
    }

    @GetMapping("/ready")
    public Map<String, Object> ready() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", serviceName);
        body.put("timestamp", Instant.now().toString());
        body.put("ready", true);
        return body;
    }

    /** Deep status: uptime, dataset presence and the number of registered algorithm engines. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", serviceName);
        body.put("timestamp", Instant.now().toString());
        body.put("uptimeMillis", System.currentTimeMillis() - bootTimeMillis);
        body.put("datasetLoaded", datasetService.currentDataset().isPresent());
        body.put("datasetName", datasetService.currentDataset()
                .map(dataset -> dataset.name()).orElse(null));
        body.put("datasetSize", datasetService.currentDataset().map(dataset -> dataset.size()).orElse(0));
        body.put("engines", queryDispatcher.engineCount());
        return body;
    }

    /** Dataset readiness probe: 200 with dataset info, or 404 when no dataset has been loaded. */
    @GetMapping("/dataset")
    public ResponseEntity<Map<String, Object>> dataset() {
        return datasetService.currentDataset().map(dataset -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("loaded", true);
            body.put("datasetName", dataset.name());
            body.put("size", dataset.size());
            body.put("totalLines", dataset.totalLines());
            body.put("failedLines", dataset.failedLines());
            return ResponseEntity.ok(body);
        }).orElseGet(() -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("loaded", false);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(body);
        });
    }

    private static final long bootTimeMillis = System.currentTimeMillis();
}