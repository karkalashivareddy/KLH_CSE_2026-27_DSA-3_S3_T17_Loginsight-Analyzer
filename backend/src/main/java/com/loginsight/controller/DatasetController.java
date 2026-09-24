package com.loginsight.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.loginsight.dto.response.DatasetSummaryDto;
import com.loginsight.service.Dataset;
import com.loginsight.service.DatasetService;

/**
 * Dataset management endpoints (docs/12 §2). The available-sample list drives the import picker;
 * {@code POST /api/datasets/{name}} loads the named bundled dataset and establishes it as the
 * current in-memory dataset; {@code POST /api/datasets/demo} loads the deterministic demo corpus and
 * {@code POST /api/datasets} ingests an uploaded file (docs/API.md §4).
 */
@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @GetMapping
    public List<String> available() {
        return datasetService.availableSamples();
    }

    /** Load the built-in deterministic "Demo Dataset" (docs/DATASET.md §5). */
    @PostMapping("/demo")
    public DatasetSummaryDto loadDemo() {
        return DatasetSummaryDto.from(datasetService.loadDemo(),
                "Deterministic demo corpus generated at 2026-09-13, re-anchored to the current hour.");
    }

    /** Ingest an uploaded raw log file (JSONL or canonical text), auto-detected by the parser. */
    @PostMapping
    public DatasetSummaryDto upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "name", required = false) String name) {
        String datasetName = name == null || name.isBlank()
                ? file.getOriginalFilename() == null
                        ? "uploaded-logs" : file.getOriginalFilename()
                : name;
        try {
            Dataset dataset = datasetService.ingest(datasetName, file.getInputStream());
            return DatasetSummaryDto.from(dataset,
                    "Ingested " + dataset.size() + " events (" + dataset.failedLines()
                            + " lines skipped).");
        } catch (java.io.IOException e) {
            throw new com.loginsight.exception.DatasetException("Failed to read uploaded file", e);
        }
    }

    @PostMapping("/{name}")
    public ResponseEntity<Map<String, Object>> load(@PathVariable String name) {
        try {
            Dataset dataset = datasetService.loadSample(name);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("loaded", true);
            body.put("datasetName", dataset.name());
            body.put("size", dataset.size());
            body.put("totalLines", dataset.totalLines());
            body.put("failedLines", dataset.failedLines());
            return ResponseEntity.ok(body);
        } catch (com.loginsight.exception.DatasetException e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("loaded", false);
            body.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
    }

    @DeleteMapping
    public Map<String, Object> clear() {
        datasetService.clear();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loaded", false);
        return body;
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> current() {
        return datasetService.currentDataset().map(dataset -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("loaded", true);
            body.put("datasetName", dataset.name());
            body.put("size", dataset.size());
            body.put("totalLines", dataset.totalLines());
            body.put("failedLines", dataset.failedLines());
            body.put("loadedAt", dataset.loadedAt().toString());
            return ResponseEntity.ok(body);
        }).orElseGet(() -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("loaded", false);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        });
    }
}