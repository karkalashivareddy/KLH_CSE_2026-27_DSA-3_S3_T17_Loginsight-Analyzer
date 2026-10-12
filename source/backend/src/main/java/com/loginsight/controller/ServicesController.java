package com.loginsight.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.analytics.FleetAnalyzer;
import com.loginsight.analytics.TimelineAnalyzer;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.dto.response.ServiceStatsDto;
import com.loginsight.exception.DatasetException;
import com.loginsight.model.LogEvent;
import com.loginsight.query.QueryValidator;
import com.loginsight.service.DatasetService;

/**
 * Service health endpoints (docs/API.md §8): per-service rollups, a drill-down detail view with a
 * 24-hour activity series and top endpoints, and HTTP / host aggregation views on
 * {@code /api/analytics}. All values are computed live from the loaded dataset.
 */
@RestController
@RequestMapping("/api/services")
public class ServicesController {

    private static final int MAX_SERVICE_LIMIT = 200;
    private static final int MAX_RECENT_EVENTS = 500;
    private static final int ACTIVITY_BUCKETS = 24;

    private final DatasetService datasetService;
    private final FleetAnalyzer fleetAnalyzer = new FleetAnalyzer();
    private final TimelineAnalyzer timelineAnalyzer = new TimelineAnalyzer();

    public ServicesController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @GetMapping
    public List<ServiceStatsDto> services(@RequestParam(defaultValue = "50") int limit) {
        QueryValidator.requireBounds(1, limit, MAX_SERVICE_LIMIT, "limit");
        return fleetAnalyzer.services(currentEvents(), limit);
    }

    @GetMapping("/{name}")
    public ServiceStatsDto.ServiceDetail service(@PathVariable String name,
                                                 @RequestParam(defaultValue = "50") int recent) {
        QueryValidator.requireNotBlank(name, "name");
        QueryValidator.requireBounds(1, recent, MAX_RECENT_EVENTS, "recent");
        List<LogEvent> events = currentEvents();
        ServiceStatsDto summary = fleetAnalyzer.service(events, name);
        if (summary == null) {
            throw new DatasetException("Service not found in the current dataset: " + name);
        }
        List<LogEvent> serviceEvents = new ArrayList<>();
        for (LogEvent event : events) {
            if (name.equals(event.getService())) {
                serviceEvents.add(event);
            }
        }
        long total = serviceEvents.size();
        serviceEvents.sort(Comparator
                .comparing(LogEvent::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingLong(LogEvent::getId));
        List<LogEventDto> recentEvents = new ArrayList<>();
        for (int i = 0; i < serviceEvents.size() && recentEvents.size() < recent; i++) {
            recentEvents.add(LogEventDto.from(serviceEvents.get(i)));
        }

        long latest = Long.MIN_VALUE;
        for (LogEvent event : events) {
            if (event.getTimestamp() != null) {
                latest = Math.max(latest, event.getTimestamp().toEpochMilli());
            }
        }
        long end = latest == Long.MIN_VALUE ? System.currentTimeMillis()
                : latest == Long.MAX_VALUE ? latest : latest + 1;
        long start = end - TimelineAnalyzer.Range.H24.millis();
        List<Map<String, Object>> activity = new ArrayList<>();
        for (TimelineAnalyzer.Point point : timelineAnalyzer
                .rangeBuckets(serviceEvents, start, end, ACTIVITY_BUCKETS)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("start", point.start());
            row.put("end", point.end());
            row.put("count", point.count());
            activity.add(row);
        }
        return new ServiceStatsDto.ServiceDetail(summary, recentEvents, total, activity);
    }

    private List<LogEvent> currentEvents() {
        return datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"))
                .events();
    }
}