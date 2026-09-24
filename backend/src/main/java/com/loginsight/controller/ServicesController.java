package com.loginsight.controller;

import java.util.ArrayList;
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
import com.loginsight.service.DatasetService;

/**
 * Service health endpoints (docs/API.md §8): per-service rollups, a drill-down detail view with a
 * 24-hour activity series and top endpoints, and HTTP / host aggregation views on
 * {@code /api/analytics}. All values are computed live from the loaded dataset.
 */
@RestController
@RequestMapping("/api/services")
public class ServicesController {

    private final DatasetService datasetService;
    private final FleetAnalyzer fleetAnalyzer = new FleetAnalyzer();
    private final TimelineAnalyzer timelineAnalyzer = new TimelineAnalyzer();

    public ServicesController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @GetMapping
    public List<ServiceStatsDto> services(@RequestParam(defaultValue = "50") int limit) {
        return fleetAnalyzer.services(currentEvents(), limit);
    }

    @GetMapping("/{name}")
    public ServiceStatsDto.ServiceDetail service(@PathVariable String name,
                                                 @RequestParam(defaultValue = "50") int recent) {
        List<LogEvent> events = currentEvents();
        ServiceStatsDto summary = fleetAnalyzer.service(events, name);
        if (summary == null) {
            throw new DatasetException("Service not found in the current dataset: " + name);
        }
        List<LogEventDto> recentEvents = new ArrayList<>();
        long total = 0;
        for (LogEvent event : events) {
            if (name.equals(event.getService())) {
                total++;
            }
        }
        List<LogEvent> serviceEvents = new ArrayList<>();
        for (LogEvent event : events) {
            if (name.equals(event.getService())) {
                serviceEvents.add(event);
            }
        }
        serviceEvents.sort((a, b) -> {
            if (a.getTimestamp() == null) {
                return 1;
            }
            if (b.getTimestamp() == null) {
                return -1;
            }
            return Long.compare(b.getTimestamp().toEpochMilli(), a.getTimestamp().toEpochMilli());
        });
        for (int i = 0; i < serviceEvents.size() && recentEvents.size() < recent; i++) {
            recentEvents.add(LogEventDto.from(serviceEvents.get(i)));
        }

        long end = System.currentTimeMillis();
        long start = end - TimelineAnalyzer.Range.H24.millis();
        List<Map<String, Object>> activity = new ArrayList<>();
        for (TimelineAnalyzer.Point point : timelineAnalyzer
                .rangeBuckets(events.stream().filter(e -> name.equals(e.getService())).toList(),
                        start, end, 24)) {
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