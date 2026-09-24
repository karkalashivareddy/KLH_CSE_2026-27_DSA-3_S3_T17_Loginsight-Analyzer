package com.loginsight.incident;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.dto.response.IncidentDto;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;
import com.loginsight.pattern.PatternExtractor;

/**
 * Heuristic incident detector (docs/ALGORITHMS.md, docs/API.md §6).
 *
 * <p>Algorithm (clearly heuristic):</p>
 * <ol>
 *   <li>Take every ERROR/FATAL event of the dataset.</li>
 *   <li>Bucket them into fixed 5-minute windows over the dataset's time span.</li>
 *   <li>Compute the baseline (average errors per window) and mark windows whose volume is at least
 *       {@code max(3, 3 × baseline)} — an "elevated error window".</li>
 *   <li>Merge consecutive (or one-window-apart) elevated windows into incidents.</li>
 *   <li>Label each incident with its top services and primary message pattern.</li>
 * </ol>
 *
 * <p>This is a rule-based anomaly grouping, not a trained model; the UI labels the method and lets
 * the user inspect every supporting log.</p>
 */
public final class IncidentDetector {

    public static final Duration WINDOW = Duration.ofMinutes(5);
    private static final double THRESHOLD_FACTOR = 3.0;
    private static final long MIN_ELEVATED = 3;

    private final PatternExtractor patternExtractor = new PatternExtractor();

    /** Detect incidents across the dataset; returns up to {@code limit}, largest first. */
    public List<IncidentDto> detect(List<LogEvent> events, int limit) {
        List<LogEvent> errors = new ArrayList<>();
        Instant earliest = null;
        Instant latest = null;
        for (LogEvent event : events) {
            if (event.getLevel() == LogLevel.ERROR || event.getLevel() == LogLevel.FATAL) {
                if (event.getTimestamp() != null) {
                    errors.add(event);
                    if (earliest == null || event.getTimestamp().isBefore(earliest)) {
                        earliest = event.getTimestamp();
                    }
                    if (latest == null || event.getTimestamp().isAfter(latest)) {
                        latest = event.getTimestamp();
                    }
                }
            }
        }
        if (errors.isEmpty() || earliest == null || latest == null) {
            return List.of();
        }

        long startMillis = earliest.toEpochMilli();
        long windowMillis = WINDOW.toMillis();
        long span = Math.max(windowMillis, latest.toEpochMilli() - startMillis + 1);
        int windowCount = (int) Math.min(5000, Math.max(1, (span + windowMillis - 1) / windowMillis));
        int[] windows = new int[windowCount];
        for (LogEvent event : errors) {
            int idx = (int) Math.min(windowCount - 1,
                    (event.getTimestamp().toEpochMilli() - startMillis) / windowMillis);
            windows[idx]++;
        }
        double baseline = (double) errors.size() / windowCount;
        long threshold = Math.max(MIN_ELEVATED, (long) Math.ceil(baseline * THRESHOLD_FACTOR));

        List<int[]> spans = new ArrayList<>();
        int currentStart = -1;
        int currentEnd = -1;
        for (int i = 0; i < windowCount; i++) {
            boolean elevated = windows[i] >= threshold;
            if (elevated) {
                if (currentStart == -1) {
                    currentStart = i;
                    currentEnd = i;
                } else if (i <= currentEnd + 1) {
                    currentEnd = i;
                } else {
                    spans.add(new int[]{currentStart, currentEnd});
                    currentStart = i;
                    currentEnd = i;
                }
            }
        }
        if (currentStart != -1) {
            spans.add(new int[]{currentStart, currentEnd});
        }

        List<IncidentDto> out = new ArrayList<>();
        long id = 1;
        for (int[] windowSpan : spans) {
            Instant incidentStart = Instant.ofEpochMilli(startMillis + (long) windowSpan[0] * windowMillis);
            Instant incidentEnd = Instant.ofEpochMilli(Math.min(latest.toEpochMilli(),
                    startMillis + (long) windowSpan[1] * windowMillis + windowMillis));
            List<String> messages = new ArrayList<>();
            Map<String, Integer> serviceCounts = new LinkedHashMap<>();
            long eventCount = 0;
            for (LogEvent event : errors) {
                long ts = event.getTimestamp().toEpochMilli();
                if (ts >= incidentStart.toEpochMilli() && ts < incidentEnd.toEpochMilli()) {
                    eventCount++;
                    serviceCounts.merge(event.getService() == null ? "?" : event.getService(),
                            1, Integer::sum);
                    if (event.getMessage() != null) {
                        messages.add(event.getMessage());
                    }
                }
            }
            if (eventCount == 0) {
                continue;
            }
            List<String> topServices = serviceCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .toList();
            long incidentId = id++;
            out.add(new IncidentDto(incidentId,
                    incidentStart.toString(), incidentEnd.toString(), topServices, eventCount,
                    patternExtractor.topTemplate(messages), "OPEN",
                    "Heuristic: 5-minute error-rate threshold (max(3, 3×baseline=" + threshold
                            + " errors/window))"));
        }
        out.sort((a, b) -> Long.compare(b.eventCount(), a.eventCount()));
        if (out.size() > limit) {
            return new ArrayList<>(out.subList(0, limit));
        }
        return out;
    }
}