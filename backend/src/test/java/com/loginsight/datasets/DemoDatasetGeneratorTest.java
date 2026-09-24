package com.loginsight.datasets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Determinism and shape of the demo corpus (docs/DATASET.md §5): two fresh generators seeded
 * identically must reproduce the exact same events; the corpus must span the 24h window ending at
 * the anchor, cover all eight services, and carry ERROR/FATAL material plus trace ids.
 */
class DemoDatasetGeneratorTest {

    @Test
    void twoFreshGeneratorsProduceIdenticalCorpus() {
        List<LogEvent> first = new DemoDatasetGenerator().generate();
        List<LogEvent> second = new DemoDatasetGenerator().generate();
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i += 977) {
            LogEvent a = first.get(i);
            LogEvent b = second.get(i);
            assertEquals(a.getTimestamp(), b.getTimestamp());
            assertEquals(a.getService(), b.getService());
            assertEquals(a.getMessage(), b.getMessage());
            assertEquals(a.getTraceId(), b.getTraceId());
            assertEquals(a.getRawMessage(), b.getRawMessage());
        }
    }

    @Test
    void cachedGenerateIsStableWithinAnInstance() {
        DemoDatasetGenerator generator = new DemoDatasetGenerator();
        assertEquals(generator.generate().size(), generator.generate().size());
        assertEquals(generator.generate().get(0).getTraceId(),
                generator.generate().get(0).getTraceId());
    }

    @Test
    void corpusSpansThe24hWindowEndingAtTheAnchor() {
        Instant anchor = Instant.parse("2026-09-13T00:00:00Z");
        List<LogEvent> events = new DemoDatasetGenerator().build(2000, anchor);
        Instant min = anchor.minus(24, ChronoUnit.HOURS);
        for (LogEvent event : events) {
            Instant ts = event.getTimestamp();
            assertTrue(!ts.isBefore(min) && !ts.isAfter(anchor),
                    "timestamp outside the 24h window: " + ts);
        }
    }

    @Test
    void corpusCoversAllEightServicesAndErrorMaterial() {
        List<LogEvent> events = new DemoDatasetGenerator().build(4000,
                Instant.parse("2026-09-13T00:00:00Z"));
        List<String> expected = List.of("api-gateway", "auth-service", "user-service",
                "payment-service", "order-service", "notification-service", "database", "cache");
        for (String service : expected) {
            assertTrue(events.stream().anyMatch(e -> service.equals(e.getService())),
                    "missing service: " + service);
        }
        assertTrue(events.stream().anyMatch(e -> e.getLevel() == LogLevel.ERROR));
        assertTrue(events.stream().anyMatch(e -> e.getLevel() == LogLevel.FATAL));
        assertTrue(events.stream().allMatch(e -> e.getTraceId() != null));
        assertTrue(events.stream().allMatch(e -> "demo-stream".equals(e.getSource())));
        assertTrue(events.stream().allMatch(e -> e.getRawMessage() != null));
    }

    @Test
    void demoUrlAndHostShape() {
        List<LogEvent> events = new DemoDatasetGenerator().build(1000,
                Instant.parse("2026-09-13T00:00:00Z"));
        for (LogEvent event : events) {
            assertTrue(event.getUrl().startsWith("https://api.demo.loginsight.local"));
        }
    }
}