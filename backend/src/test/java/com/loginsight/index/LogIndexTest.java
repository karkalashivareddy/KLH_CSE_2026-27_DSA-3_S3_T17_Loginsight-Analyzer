package com.loginsight.index;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * In-memory index correctness: per-field position lists, two-pointer intersection and the
 * binary-search time window all operate on event positions and stay sorted.
 */
class LogIndexTest {

    private List<LogEvent> fixtures() {
        List<LogEvent> out = new ArrayList<>();
        out.add(event(0, "2026-09-13T00:00:00Z", LogLevel.INFO, "api-gateway", "gw-1", 200,
                "request completed"));
        out.add(event(1, "2026-09-13T00:01:00Z", LogLevel.ERROR, "database", "db-1", 503,
                "connection refused"));
        out.add(event(2, "2026-09-13T00:02:00Z", LogLevel.WARN, "api-gateway", "gw-1", 429,
                "rate limit exceeded"));
        out.add(event(3, "2026-09-13T00:03:00Z", LogLevel.ERROR, "auth-service", "auth-1", 401,
                "authentication failed"));
        return out;
    }

    private static LogEvent event(long id, String iso, LogLevel level, String service, String host,
                                  int status, String message) {
        return LogEvent.builder()
                .id(id)
                .timestamp(Instant.parse(iso))
                .level(level)
                .service(service)
                .host(host)
                .statusCode(status)
                .message(message)
                .build();
    }

    @Test
    void severityAndServiceFiltersReturnPositions() {
        LogIndex index = new LogIndex(fixtures());
        assertArrayEquals(new int[]{1, 3}, index.bySeverity(List.of("ERROR")));
        assertArrayEquals(new int[]{0, 2}, index.byService("api-gateway"));
        assertArrayEquals(new int[]{3}, index.byHost("auth-1"));
    }

    @Test
    void timeWindowUsesBinarySearch() {
        LogIndex index = new LogIndex(fixtures());
        long from = Instant.parse("2026-09-13T00:01:00Z").toEpochMilli();
        long to = Instant.parse("2026-09-13T00:03:00Z").toEpochMilli();
        assertArrayEquals(new int[]{1, 2}, index.inTimeWindow(from, to));
    }

    @Test
    void intersectMergesSortedArrays() {
        int[] a = {0, 2, 4, 6};
        int[] b = {2, 4, 5};
        assertArrayEquals(new int[]{2, 4}, LogIndex.intersect(a, b));
    }

    @Test
    void tokenPostingsNarrowMessages() {
        LogIndex index = new LogIndex(fixtures());
        int[] posts = index.byToken("refused");
        assertEquals(1, posts.length);
        assertEquals(1, posts[0]);
    }

    @Test
    void emptyIndexBehaves() {
        LogIndex index = new LogIndex(List.of());
        assertEquals(0, index.size());
        assertEquals(0, index.bySeverity(List.of("ERROR")).length);
        assertEquals(0, index.inTimeWindow(0, Long.MAX_VALUE).length);
    }

    @Test
    void unknownValuesYieldEmptyLists() {
        LogIndex index = new LogIndex(fixtures());
        assertTrue(index.byService("nope").length == 0);
        assertTrue(index.byStatus("999").length == 0);
    }
}