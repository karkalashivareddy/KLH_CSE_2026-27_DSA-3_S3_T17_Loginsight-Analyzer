package com.loginsight.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.analytics.AnalyticsResult;
import com.loginsight.analytics.FrequencyAnalyzer;
import com.loginsight.exception.DatasetException;
import com.loginsight.support.SampleDataGenerator;
import com.loginsight.support.SampleDataTestSupport;

class AnalyticsServiceTest {

    private static final String SAMPLE_DATA_DIR;

    static {
        try {
            SampleDataTestSupport.ensureGenerated();
            SAMPLE_DATA_DIR = SampleDataTestSupport.sampleDataDir().toString();
        } catch (IOException e) {
            throw new IllegalStateException("sample data generation failed", e);
        }
    }

    private final DatasetService datasetService = new DatasetService(SAMPLE_DATA_DIR);
    private final AnalyticsService service =
            new AnalyticsService(datasetService, new FrequencyAnalyzer());

    @Test
    void analyzeWithNoDatasetThrows() {
        assertThrows(DatasetException.class, () -> service.analyze());
    }

    @Test
    void analyzeSmallSampleConsistency() {
        datasetService.loadSample(SampleDataGenerator.LOGS_SMALL);
        AnalyticsResult result = service.analyze();
        assertEquals(1000, result.getTotalValidEvents());
        assertEquals(1000, result.getTotalEvents());
        assertEquals(0, result.getTotalFailedLines());

        long levelSum = result.getLevelFrequency().values().stream().mapToLong(Integer::longValue).sum();
        long serviceSum = result.getServiceFrequency().values().stream().mapToLong(Integer::longValue).sum();
        long statusSum = result.getStatusCodeFrequency().values().stream().mapToLong(Integer::longValue).sum();
        assertEquals(1000, levelSum, "sum of level counts must equal dataset size");
        assertEquals(1000, serviceSum, "sum of service counts must equal dataset size");
        assertEquals(1000, statusSum, "sum of status counts must equal dataset size");

        assertTrue(result.getMinResponseTimeMs() >= 0);
        assertTrue(result.getMaxResponseTimeMs() >= result.getMinResponseTimeMs());
        assertTrue(result.getAverageResponseTimeMs() >= result.getMinResponseTimeMs());
        assertTrue(result.getAverageResponseTimeMs() <= result.getMaxResponseTimeMs());
    }

    @Test
    void analyzeMalformedReflectsPartialImport() {
        datasetService.loadSample(SampleDataGenerator.LOGS_MALFORMED);
        AnalyticsResult result = service.analyze();
        assertEquals(400, result.getTotalEvents(), "raw line count preserved");
        assertEquals(result.getTotalEvents() - result.getTotalFailedLines(), result.getTotalValidEvents());
        assertTrue(result.getTotalFailedLines() > 0, "malformed dataset must report failed lines");
        assertTrue(result.getTotalFailedLines() > result.getTotalValidEvents() / 8,
                "expected roughly 16% malformed lines");
    }

    @Test
    void servicesStatusDistributionPresent() {
        datasetService.loadSample(SampleDataGenerator.LOGS_MEDIUM);
        AnalyticsResult result = service.analyze();
        Map<String, Integer> services = result.getServiceFrequency();
        Map<Integer, Integer> statuses = result.getStatusCodeFrequency();
        assertEquals(8, services.size(), "all eight canonical services must appear");
        assertTrue(statuses.containsKey(401) && statuses.get(401) > 0,
                "401 must appear in the data (AUTH cluster)");
    }
}