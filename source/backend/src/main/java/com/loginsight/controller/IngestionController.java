package com.loginsight.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.response.DatasetSummaryDto;
import com.loginsight.service.DatasetService;

/**
 * Ingestion endpoints (docs/API.md §4): dataset status for the Datasets/Ingestion screens, and the
 * one-click demo load. Uploads live on {@code POST /api/datasets} (multipart) under the dataset
 * controller. Everything reported comes from the real parse/load outcome.
 */
@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

    private final DatasetService datasetService;

    public IngestionController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> dataset = new LinkedHashMap<>();
        datasetService.currentDataset().ifPresentOrElse(d -> {
            dataset.put("loaded", true);
            dataset.put("datasetName", d.name());
            dataset.put("size", d.size());
            dataset.put("totalLines", d.totalLines());
            dataset.put("failedLines", d.failedLines());
            dataset.put("loadedAt", d.loadedAt().toString());
        }, () -> dataset.put("loaded", false));
        body.put("dataset", dataset);
        body.put("parser", "auto-detect (JSONL or canonical text)");
        body.put("streaming", "demo replay stream — not real-time");
        List<String> samples = datasetService.availableSamples();
        body.put("availableSamples", samples);
        body.put("sampleCount", samples.size());
        return body;
    }

    /** One-click demo load for the Datasets and Ingestion pages (docs/DATASET.md §5). */
    @PostMapping("/demo")
    public DatasetSummaryDto loadDemo() {
        return DatasetSummaryDto.from(datasetService.loadDemo(),
                "Deterministic demo corpus generated at 2026-09-13, re-anchored to the current hour.");
    }
}