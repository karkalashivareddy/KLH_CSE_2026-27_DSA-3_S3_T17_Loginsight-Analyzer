package com.loginsight.analytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.model.LogEvent;

/**
 * Buckets a dataset's events into equal-width time windows for the traffic chart. Window boundaries
 * are derived from the real first/last timestamps; {@code null} or missing timestamps count toward
 * a "no timestamp" bucket so nothing is silently dropped. Pure Java (docs/03 analytics package).
 */
public final class TimeWindowAnalyzer {

    /** Fixed number of chart buckets used by the stats endpoint. */
    public static final int DEFAULT_BUCKETS = 10;

    /**
     * Count events per window over the span of the dataset.
     *
     * @return list of {start, end, count} maps, one per bucket, plus optionally an
     *         {@code (null) -> count} entry when events lack timestamps
     */
    public List<Map<String, Object>> windows(List<LogEvent> events, int buckets) {
        int bucketCount = Math.max(1, buckets);
        long earliest = Long.MAX_VALUE;
        long latest = Long.MIN_VALUE;
        int noTimestamp = 0;
        for (LogEvent event : events) {
            if (event.getTimestamp() == null) {
                noTimestamp++;
                continue;
            }
            long epoch = event.getTimestamp().toEpochMilli();
            earliest = Math.min(earliest, epoch);
            latest = Math.max(latest, epoch);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        if (earliest == Long.MAX_VALUE) {
            result.add(bucket("<no timestamp>", "<no timestamp>", noTimestamp));
            return result;
        }

        long span = Math.max(1, latest - earliest + 1);
        long width = (span + bucketCount - 1) / bucketCount;
        int[] counts = new int[bucketCount];
        for (LogEvent event : events) {
            if (event.getTimestamp() == null) {
                continue;
            }
            int index = (int) Math.min(bucketCount - 1,
                    (event.getTimestamp().toEpochMilli() - earliest) / width);
            counts[index]++;
        }
        for (int i = 0; i < bucketCount; i++) {
            long start = earliest + (long) i * width;
            long end = earliest + (long) (i + 1) * width - 1;
            result.add(bucket(java.time.Instant.ofEpochMilli(start).toString(),
                    java.time.Instant.ofEpochMilli(end).toString(), counts[i]));
        }
        if (noTimestamp > 0) {
            result.add(bucket("<no timestamp>", "<no timestamp>", noTimestamp));
        }
        return result;
    }

    private static Map<String, Object> bucket(String start, String end, int count) {
        return java.util.Map.of("start", start, "end", end, "count", count);
    }
}