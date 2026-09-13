package com.loginsight.service;

import org.springframework.stereotype.Service;

import com.loginsight.analytics.AnalyticsResult;
import com.loginsight.analytics.FrequencyAnalyzer;
import com.loginsight.exception.DatasetException;

/**
 * Coordination facade for dataset statistics (docs/02 §7). The analyzers themselves are pure Java;
 * this service injects the in-memory dataset and hands it to the analyzers, keeping chart-relevant
 * numbers ready for the Phase 10 REST layer.
 */
@Service
public class AnalyticsService {

    private final DatasetService datasetService;
    private final FrequencyAnalyzer frequencyAnalyzer;

    public AnalyticsService(DatasetService datasetService, FrequencyAnalyzer frequencyAnalyzer) {
        this.datasetService = datasetService;
        this.frequencyAnalyzer = frequencyAnalyzer;
    }

    /** Frequency statistics for the currently loaded dataset. */
    public AnalyticsResult analyze() {
        Dataset dataset = datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"));
        return frequencyAnalyzer.analyze(dataset);
    }
}