package com.loginsight.analytics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.model.LogEvent;

/**
 * Buckets events into equal-width time windows. Handles two modes:
 * <ul>
 *   <li>{@link #buckets(List, int)} over the dataset's own span (dashboard baseline)</li>
 *   <li>{@link #rangeBuckets(List, long, long, int)} over an explicit window (live/dashboard ranges)</li>
 * </ul>
 * Windows with no records are still emitted (zero-count buckets), so client charts never invent gaps.
 */
public final class TimelineAnalyzer {

    public record Point(String start, String end, long count) {
    }

    /** Fixed preset ranges understood by the overview endpoint. */
    public record Range(String id, long millis) {
        public static final Range M5 = new Range("5m", 5 * 60_000L);
        public static final Range M15 = new Range("15m", 15 * 60_000L);
        public static final Range H1 = new Range("1h", 3_600_000L);
        public static final Range H6 = new Range("6h", 6 * 3_600_000L);
        public static final Range H24 = new Range("24h", 24 * 3_600_000L);

        public static Range lookup(String id) {
            return switch (id == null ? "1h" : id) {
                case "5m" -> M5;
                case "15m" -> M15;
                case "6h" -> H6;
                case "24h" -> H24;
                default -> H1;
            };
        }

        public int bucketCount() {
            return switch (millis < 3_600_000L ? "short" : "long") {
                case "short" -> 10;
                default -> millis <= 3_600_000L ? 12 : 24;
            };
        }
    }

    public List<Point> buckets(List<LogEvent> events, int buckets) {
        if (events.isEmpty()) {
            return List.of();
        }
        long earliest = Long.MAX_VALUE;
        long latest = Long.MIN_VALUE;
        for (LogEvent event : events) {
            if (event.getTimestamp() != null) {
                long epoch = event.getTimestamp().toEpochMilli();
                earliest = Math.min(earliest, epoch);
                latest = Math.max(latest, epoch);
            }
        }
        if (earliest == Long.MAX_VALUE) {
            return List.of();
        }
        int count = Math.max(1, buckets);
        long span = Math.max(1, latest - earliest + 1);
        long width = (span + count - 1) / count;
        long[] counts = new long[count];
        for (LogEvent event : events) {
            if (event.getTimestamp() == null) {
                continue;
            }
            int idx = (int) Math.min(count - 1,
                    (event.getTimestamp().toEpochMilli() - earliest) / width);
            counts[idx]++;
        }
        List<Point> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long start = earliest + (long) i * width;
            long end = earliest + (long) (i + 1) * width - 1;
            out.add(new Point(Instant.ofEpochMilli(start).toString(),
                    Instant.ofEpochMilli(end).toString(), counts[i]));
        }
        return out;
    }

    /** Bucket events within {@code [from, to)}; absolute counts per window, then daily averages. */
    public List<Point> rangeBuckets(List<LogEvent> events, long from, long to, int buckets) {
        int count = Math.max(1, buckets);
        long width = Math.max(1, (to - from) / count);
        long[] counts = new long[count];
        for (LogEvent event : events) {
            if (event.getTimestamp() == null) {
                continue;
            }
            long ts = event.getTimestamp().toEpochMilli();
            if (ts < from || ts > to) {
                continue;
            }
            int idx = (int) Math.min(count - 1, (ts - from) / width);
            counts[idx]++;
        }
        List<Point> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long start = from + (long) i * width;
            long end = from + (long) (i + 1) * width - 1;
            out.add(new Point(Instant.ofEpochMilli(start).toString(),
                    Instant.ofEpochMilli(end).toString(), counts[i]));
        }
        return out;
    }

    /** Per-key histogram (service, level, status, host) over a full dataset. */
    public Map<String, Long> histogram(List<LogEvent> events, java.util.function.Function<LogEvent, String> key) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (LogEvent event : events) {
            String value = key.apply(event);
            if (value != null && !value.isBlank()) {
                out.merge(value, 1L, Long::sum);
            }
        }
        return out;
    }
}