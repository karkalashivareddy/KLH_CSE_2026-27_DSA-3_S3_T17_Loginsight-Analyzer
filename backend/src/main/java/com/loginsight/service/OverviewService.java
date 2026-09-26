package com.loginsight.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.loginsight.analytics.FleetAnalyzer;
import com.loginsight.analytics.HeatmapAnalyzer;
import com.loginsight.analytics.SeverityAnalyzer;
import com.loginsight.analytics.TimelineAnalyzer;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.dto.response.OverviewDto;
import com.loginsight.dto.response.PatternDto;
import com.loginsight.dto.response.ServiceStatsDto;
import com.loginsight.exception.DatasetException;
import com.loginsight.index.LogIndexService;
import com.loginsight.incident.IncidentDetector;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;
import com.loginsight.pattern.PatternExtractor;

/**
 * Composes the real-time operation dashboard from the loaded dataset (docs/API.md §2). Throws a
 * {@link DatasetException} (→ 404) when no dataset is loaded so the UI presents its first-run
 * state instead of zeros.
 */
@Service
public class OverviewService {

    private final DatasetService datasetService;
    private final LogIndexService indexService;
    private final FleetAnalyzer fleetAnalyzer = new FleetAnalyzer();
    private final SeverityAnalyzer severityAnalyzer = new SeverityAnalyzer();
    private final TimelineAnalyzer timelineAnalyzer = new TimelineAnalyzer();
    private final HeatmapAnalyzer heatmapAnalyzer = new HeatmapAnalyzer();
    private final PatternExtractor patternExtractor = new PatternExtractor();
    private final IncidentDetector incidentDetector = new IncidentDetector();

    public OverviewService(DatasetService datasetService, LogIndexService indexService) {
        this.datasetService = datasetService;
        this.indexService = indexService;
    }

    public OverviewDto snapshot(String rangeId) {
        Dataset dataset = datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"));
        List<LogEvent> datasetEvents = dataset.events();
        TimelineAnalyzer.Range range = TimelineAnalyzer.Range.lookup(rangeId);

        Instant latestTimestamp = null;
        for (LogEvent event : datasetEvents) {
            Instant timestamp = event.getTimestamp();
            if (timestamp != null && (latestTimestamp == null || timestamp.isAfter(latestTimestamp))) {
                latestTimestamp = timestamp;
            }
        }
        Instant windowEnd = latestTimestamp == null ? Instant.now() : latestTimestamp;
        Instant windowStart = windowEnd.minusMillis(range.millis());
        List<LogEvent> events = datasetEvents.stream()
                .filter(event -> {
                    Instant timestamp = event.getTimestamp();
                    return timestamp != null && !timestamp.isBefore(windowStart)
                            && !timestamp.isAfter(windowEnd);
                })
                .toList();

        long n = events.size();
        long errors = 0;
        long warnings = 0;
        for (LogEvent event : events) {
            LogLevel level = event.getLevel();
            if (level == LogLevel.ERROR || level == LogLevel.FATAL) {
                errors++;
            } else if (level == LogLevel.WARN) {
                warnings++;
            }
        }
        double eventsPerMinute = (double) n / (range.millis() / 60_000.0);

        long windowStartMillis = windowStart.toEpochMilli();
        long windowEndMillis = windowEnd.toEpochMilli();
        List<OverviewDto.TimelinePoint> timeline = timelineAnalyzer
                .rangeBuckets(events, windowStartMillis, windowEndMillis, range.bucketCount())
                .stream()
                .map(p -> new OverviewDto.TimelinePoint(p.start(), p.end(), p.count()))
                .toList();

        Map<String, Long> severity = severityAnalyzer.distribution(events);
        List<ServiceStatsDto> topServices = fleetAnalyzer.services(events, 6);
        List<PatternDto> topPatterns = patternExtractor.extract(events, 8);

        List<LogAvatar> critical = new ArrayList<>();
        for (LogEvent event : events) {
            if ((event.getLevel() == LogLevel.ERROR || event.getLevel() == LogLevel.FATAL)
                    && event.getTimestamp() != null) {
                critical.add(new LogAvatar(event));
            }
        }
        critical.sort((a, b) -> {
            int byTimestamp = Long.compare(b.timestamp, a.timestamp);
            return byTimestamp != 0 ? byTimestamp : Long.compare(b.event.getId(), a.event.getId());
        });
        List<LogEventDto> recentCritical = new ArrayList<>();
        for (int i = 0; i < Math.min(12, critical.size()); i++) {
            recentCritical.add(LogEventDto.from(critical.get(i).event));
        }

        HeatmapAnalyzer.Heatmap heatmap = heatmapAnalyzer.hourByWeekday(events);
        long activeIncidents = incidentDetector.detect(events, 200).size();
        long services = events.stream().map(LogEvent::getService)
                .filter(java.util.Objects::nonNull).distinct().count();
        long hosts = events.stream().map(LogEvent::getHost)
                .filter(java.util.Objects::nonNull).distinct().count();

        return new OverviewDto(dataset.name(), "Operational", n, errors, warnings, services, hosts,
                eventsPerMinute, activeIncidents, timeline, range.id(), severity, topServices,
                topPatterns, recentCritical,
                new OverviewDto.Heatmap(heatmap.days(), heatmap.columns(), heatmap.cells()),
                fleetAnalyzer.http(events).statusCodes(), datasetEvents.size(),
                windowStart.toString(), windowEnd.toString(), "selected-window");
    }

    private record LogAvatar(LogEvent event, long timestamp) {
        LogAvatar(LogEvent event) {
            this(event, event.getTimestamp() == null ? Long.MIN_VALUE
                    : event.getTimestamp().toEpochMilli());
        }
    }
}