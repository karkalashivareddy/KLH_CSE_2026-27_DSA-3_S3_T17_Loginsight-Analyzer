package com.loginsight.analytics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.model.LogEvent;
import com.loginsight.service.Dataset;

/**
 * Computes dataset-level frequency statistics (levels, services, status codes, endpoints) and
 * response-time bounds over the events of a {@link Dataset}. Pure Java with no Spring dependency,
 * so it is unit-testable standalone and reusable by later analyzers (TopKAnalyzer,
 * TimeWindowAnalyzer).
 */
public final class FrequencyAnalyzer {

    public AnalyticsResult analyze(Dataset dataset) {
        List<LogEvent> events = dataset.events();

        Map<String, Integer> levelFrequency = new HashMap<>();
        Map<String, Integer> serviceFrequency = new HashMap<>();
        Map<Integer, Integer> statusCodeFrequency = new HashMap<>();
        Map<String, Integer> endpointFrequency = new HashMap<>();

        long sumResponseTimeMs = 0;
        long minResponseTimeMs = Long.MAX_VALUE;
        long maxResponseTimeMs = Long.MIN_VALUE;

        for (LogEvent event : events) {
            if (event.getLevel() != null) {
                levelFrequency.merge(event.getLevel().name(), 1, Integer::sum);
            }
            if (event.getService() != null) {
                serviceFrequency.merge(event.getService(), 1, Integer::sum);
            }
            int status = event.getStatusCode();
            if (status != 0) {
                statusCodeFrequency.merge(status, 1, Integer::sum);
            }
            if (event.getEndpoint() != null) {
                endpointFrequency.merge(event.getEndpoint(), 1, Integer::sum);
            }
            long rt = event.getResponseTime();
            sumResponseTimeMs += rt;
            minResponseTimeMs = Math.min(minResponseTimeMs, rt);
            maxResponseTimeMs = Math.max(maxResponseTimeMs, rt);
        }

        long totalValid = events.size();
        long min = totalValid == 0 ? 0 : minResponseTimeMs;
        long max = totalValid == 0 ? 0 : maxResponseTimeMs;

        return new AnalyticsResult(dataset.totalLines(), totalValid, dataset.failedLines(),
                levelFrequency, serviceFrequency, statusCodeFrequency, endpointFrequency,
                sumResponseTimeMs, min, max);
    }
}