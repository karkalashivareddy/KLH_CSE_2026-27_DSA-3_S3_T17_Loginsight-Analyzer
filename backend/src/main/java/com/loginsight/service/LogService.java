package com.loginsight.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.loginsight.analytics.ErrorPatternAnalyzer;
import com.loginsight.analytics.TimeWindowAnalyzer;
import com.loginsight.dto.response.DatasetStatsDto;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.exception.DatasetException;
import com.loginsight.graph.TopKFrequentAnalyzer;
import com.loginsight.model.LogEvent;
import com.loginsight.query.QueryContext;

/**
 * Log-read model and dataset-backed stats (docs/12 §2). Everything reported here is computed from
 * the currently loaded dataset - no fabricated numbers. The rendered {@link #searchableText()} is
 * the seeded haystack for {@code Scope.DATASET} searches.
 */
@Service
public class LogService {

    private final DatasetService datasetService;
    private final ErrorPatternAnalyzer errorPatternAnalyzer = new ErrorPatternAnalyzer();
    private final TimeWindowAnalyzer timeWindowAnalyzer = new TimeWindowAnalyzer();
    private final TopKFrequentAnalyzer topKFrequentAnalyzer = new TopKFrequentAnalyzer();

    public LogService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    /** First log event of the current dataset (bootstrap widget). */
    public LogEventDto first() {
        return LogEventDto.from(currentEvents().get(0));
    }

    /** Single event by its dataset-assigned id; DatasetException → 404 when unknown. */
    public LogEventDto byId(long id) {
        List<LogEvent> events = currentEvents();
        if (id < 0 || id >= events.size()) {
            throw new DatasetException("Log id out of range for the current dataset: " + id);
        }
        return LogEventDto.from(events.get((int) id));
    }

    /** Slice of the current dataset from {@code offset} for {@code limit} rows. */
    public List<LogEventDto> list(int limit, int offset) {
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
        List<LogEvent> events = currentEvents();
        int from = Math.min(offset, events.size());
        int to = Math.min(from + limit, events.size());
        return events.subList(from, to).stream().map(LogEventDto::from).toList();
    }

    /** Dataset statistics covering analysis, aggregation and rate derivation. */
    public DatasetStatsDto stats() {
        List<LogEvent> events = currentEvents();
        int n = events.size();

        Map<String, Integer> levels = new LinkedHashMap<>();
        Map<String, Integer> statusCodes = new LinkedHashMap<>();
        java.util.Set<String> services = new java.util.LinkedHashSet<>();
        java.util.Set<String> ips = new java.util.LinkedHashSet<>();
        long sumResponseTimeMs = 0;
        int errors = 0;
        int warnings = 0;
        long earliest = Long.MAX_VALUE;
        long latest = Long.MIN_VALUE;
        for (LogEvent event : events) {
            if (event.getLevel() != null) {
                levels.merge(event.getLevel().name(), 1, Integer::sum);
                if (event.getLevel() == com.loginsight.model.LogLevel.ERROR) {
                    errors++;
                }
                if (event.getLevel() == com.loginsight.model.LogLevel.WARN) {
                    warnings++;
                }
            }
            if (event.getStatusCode() != 0) {
                statusCodes.merge(String.valueOf(event.getStatusCode()), 1, Integer::sum);
            }
            if (event.getService() != null) {
                services.add(event.getService());
            }
            if (event.getIpAddress() != null) {
                ips.add(event.getIpAddress());
            }
            sumResponseTimeMs += event.getResponseTime();
            if (event.getTimestamp() != null) {
                long epoch = event.getTimestamp().toEpochMilli();
                earliest = Math.min(earliest, epoch);
                latest = Math.max(latest, epoch);
            }
        }
        double avgResponseTimeMs = n == 0 ? 0 : (double) sumResponseTimeMs / n;
        double requestsPerMinute = 0;
        if (n > 1 && earliest != Long.MAX_VALUE && latest > earliest) {
            double minutes = (latest - earliest) / 60_000.0;
            requestsPerMinute = minutes <= 0 ? n : (double) (n - 1) / minutes;
        }

        List<Map<String, Object>> topServices = topKFrequentAnalyzer
                .asBuckets(topKFrequentAnalyzer.topK(events, TopKFrequentAnalyzer.Dimension.SERVICE, 5));
        return new DatasetStatsDto(n, errors, warnings, services.size(), ips.size(),
                errorPatternAnalyzer.topError(events), avgResponseTimeMs, requestsPerMinute,
                levels, topServices, statusCodes,
                timeWindowAnalyzer.windows(events, TimeWindowAnalyzer.DEFAULT_BUCKETS));
    }

    /** Rendered searchable text of the current dataset, or a 404-domain failure when none loaded. */
    public String searchableText() {
        return QueryContext.renderDataset(currentEvents());
    }

    /** All events of the current dataset; used only by the query layer's source resolution. */
    public List<LogEvent> events() {
        return new ArrayList<>(currentEvents());
    }

    public long datasetSize() {
        return datasetService.currentDataset().map(Dataset::size).orElse(0);
    }

    private List<LogEvent> currentEvents() {
        return datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"))
                .events();
    }
}