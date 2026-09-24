package com.loginsight.index;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.loginsight.model.LogEvent;

/**
 * In-memory log index over a loaded dataset (docs/ARCHITECTURE.md). The index is the searchable
 * substrate every investigation feature reads from: it holds per-field position lists (severity,
 * service, host, source, HTTP status), a timestamp-sorted position array for time-range queries, and
 * a token inverted index over message text for cheap candidate narrowing.
 *
 * <p>Positions are dataset event positions (which equal the dataset-assigned event ids). Every list
 * is kept sorted ascending so filters can be intersected in linear two-pointer passes. The index is
 * a plain in-memory application structure — the DSA contribution lives in the {@code dsa} search
 * engine, which operates on the actual record text once candidates are narrowed here.</p>
 */
public final class LogIndex {

    private final List<LogEvent> events;
    private final Map<String, int[]> bySeverity = new LinkedHashMap<>();
    private final Map<String, int[]> byService = new LinkedHashMap<>();
    private final Map<String, int[]> byHost = new LinkedHashMap<>();
    private final Map<String, int[]> byStatus = new LinkedHashMap<>();
    private final Map<String, int[]> bySource = new LinkedHashMap<>();
    private final Map<String, int[]> byTrace = new LinkedHashMap<>();
    private final Map<String, int[]> tokens = new LinkedHashMap<>();
    private final int[] sortedByTimestamp;
    private final long[] timestamps;
    private final int size;

    public LogIndex(List<LogEvent> events) {
        this.events = List.copyOf(events);
        this.size = events.size();

        // First pass: bucket positions per field and collect timestamp order.
        java.util.List<long[]> stampWindows = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            LogEvent event = events.get(i);
            if (event.getLevel() != null) {
                bucket(bySeverity, event.getLevel().name(), i);
            }
            bucket(byService, event.getService(), i);
            bucket(byHost, event.getHost(), i);
            bucket(byStatus, statusKey(event.getStatusCode()), i);
            bucket(bySource, event.getSource(), i);
            bucket(byTrace, event.getTraceId(), i);
if (event.getTimestamp() != null) {
            stampWindows.add(new long[]{event.getTimestamp().toEpochMilli(), i});
        }
        indexTokens(event, i);
    }
    java.util.List<Integer> sortedList = new ArrayList<>(size);
        for (long[] w : stampWindows) {
            sortedList.add((int) w[1]);
        }
        sortedList.sort((a, b) -> Long.compare(
                events.get(a).getTimestamp().toEpochMilli(),
                events.get(b).getTimestamp().toEpochMilli()));
        sortedByTimestamp = new int[sortedList.size()];
        timestamps = new long[sortedList.size()];
        for (int i = 0; i < sortedList.size(); i++) {
            sortedByTimestamp[i] = sortedList.get(i);
            timestamps[i] = events.get(sortedList.get(i)).getTimestamp().toEpochMilli();
        }
    }

    private static String statusKey(int statusCode) {
        return statusCode == 0 ? null : Integer.toString(statusCode);
    }

    /** Tokenises message text (and service/endpoint) into the inverted index. */
    private void indexTokens(LogEvent event, int position) {
        StringBuilder sb = new StringBuilder();
        if (event.getService() != null) {
            sb.append(event.getService()).append(' ');
        }
        if (event.getEndpoint() != null) {
            sb.append(event.getEndpoint()).append(' ');
        }
        sb.append("$").append(event.searchableText().toLowerCase(java.util.Locale.ROOT));
        StringTokenizer tokenizer = new StringTokenizer(sb.toString(), " /:{}|.@-_");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.isEmpty() || token.length() == 1) {
                continue;
            }
            bucket(tokens, token, position);
        }
    }

    private static void bucket(Map<String, int[]> target, String key, int position) {
        if (key == null || key.isBlank()) {
            return;
        }
        int[] existing = target.get(key);
        if (existing == null) {
            target.put(key, new int[]{position});
            return;
        }
        int[] grown = new int[existing.length + 1];
        System.arraycopy(existing, 0, grown, 0, existing.length);
        grown[existing.length] = position;
        target.put(key, grown);
    }

    /* ── Filters ─────────────────────────────────────────────────────────────────────────── */

    /** Positions in the given list whose event has any of the provided levels. */
    public int[] bySeverity(List<String> levels) {
        int[] acc = null;
        for (String level : levels) {
            int[] list = bySeverity.get(level == null ? null : level.toUpperCase(java.util.Locale.ROOT));
            if (list != null) {
                acc = acc == null ? list : intersect(acc, list);
            }
        }
        return acc == null ? new int[0] : acc;
    }

    public int[] byService(String service) {
        return lookup(byService, service);
    }

    public int[] byHost(String host) {
        return lookup(byHost, host);
    }

    public int[] byStatus(String status) {
        return lookup(byStatus, status);
    }

    public int[] bySource(String source) {
        return lookup(bySource, source);
    }

    public int[] byTrace(String traceId) {
        return lookup(byTrace, traceId);
    }

    public int[] byToken(String token) {
        return lookup(tokens, token.toLowerCase(java.util.Locale.ROOT));
    }

    public int[] byTokenAll(List<String> tokens) {
        int[] acc = null;
        for (String token : tokens) {
            int[] list = byToken(token);
            if (list == null || list.length == 0) {
                return new int[0];
            }
            acc = acc == null ? list : intersect(acc, list);
        }
        return acc == null ? new int[0] : acc;
    }

    private static int[] lookup(Map<String, int[]> target, String key) {
        int[] list = target.get(key);
        return list == null ? new int[0] : list;
    }

    /** Positions of events within the timestamp window {@code [from, to)} (epoch millis). */
    public int[] inTimeWindow(long fromInclusive, long toExclusive) {
        int lo = lowerBound(timestamps, fromInclusive);
        int hi = upperBound(timestamps, toExclusive);
        if (lo >= hi || hi > sortedByTimestamp.length) {
            return new int[0];
        }
        int[] out = new int[hi - lo];
        System.arraycopy(sortedByTimestamp, lo, out, 0, hi - lo);
        return out;
    }

    private static int lowerBound(long[] array, long key) {
        int lo = 0;
        int hi = array.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (array[mid] < key) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private static int upperBound(long[] array, long key) {
        int lo = 0;
        int hi = array.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (array[mid] < key) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /** Sorted intersection of two sorted position arrays (two-pointer, O(n+m)). */
    public static int[] intersect(int[] a, int[] b) {
        int i = 0;
        int j = 0;
        int k = 0;
        int[] out = new int[Math.min(a.length, b.length)];
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                out[k++] = a[i];
                i++;
                j++;
            } else if (a[i] < b[j]) {
                i++;
            } else {
                j++;
            }
        }
        return k == out.length ? out : trim(out, k);
    }

    private static int[] trim(int[] array, int size) {
        int[] out = new int[size];
        System.arraycopy(array, 0, out, 0, size);
        return out;
    }

    /** Combine every position list in {@code groups} into one sorted unique array. */
    public static int[] unionAll(List<int[]> groups) {
        int total = 0;
        for (int[] g : groups) {
            total += g.length;
        }
        int[] out = new int[total];
        int k = 0;
        for (int[] g : groups) {
            for (int v : g) {
                if (k == 0 || out[k - 1] != v) {
                    out[k++] = v;
                }
            }
        }
        return trim(out, k);
    }

    public int size() {
        return size;
    }

    public LogEvent eventAt(int position) {
        return events.get(position);
    }

    public long timestampAt(int index) {
        return timestamps[index];
    }

    public int sortedCount() {
        return sortedByTimestamp.length;
    }
}