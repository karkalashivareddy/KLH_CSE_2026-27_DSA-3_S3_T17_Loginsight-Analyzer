package com.loginsight.simulation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded rolling window over the live event stream.
 *
 * <p>Holds at most {@code capacity} events and discards the oldest first, so a long-running
 * simulation can never grow without bound. It also maintains the aggregates the whole UI depends on:
 * cumulative counters, per-service counters and fixed-width latency buckets for a p95 estimate.</p>
 */
public final class RollingWindow {

    /** Latency samples above this ceiling are clamped; it is far beyond any realistic p95. */
    private static final long LATENCY_CEILING_MS = 12_000L;
    private static final int LATENCY_BUCKETS = 60;

    private final int capacity;
    private final ArrayDeque<SimulatedEvent> events = new ArrayDeque<>();
    private final Map<String, ServiceCounter> perService = new LinkedHashMap<>();

    private long generated;
    private long errors;
    private long warnings;
    private long latencySum;
    private final long[] latencyHistogram = new long[LATENCY_BUCKETS];

    private int currentTick = Integer.MIN_VALUE;
    private long tickEvents;
    private long tickErrors;

    /** One event plus the sequence number the simulation assigned to it. */
    public record SimulatedEvent(long sequence, int tick, com.loginsight.model.LogEvent event) {
    }

    /** Per-service counters exposed to the topology and the service-health strip. */
    public static final class ServiceCounter {
        private final String service;
        private long events;
        private long errors;
        private long warnings;
        private long latencySum;
        private long latencySamples;

        ServiceCounter(String service) {
            this.service = service;
        }

        public String service() {
            return service;
        }

        public long events() {
            return events;
        }

        public long errors() {
            return errors;
        }

        public long warnings() {
            return warnings;
        }

        public double errorRate() {
            return events == 0 ? 0.0 : (errors * 100.0) / events;
        }

        public long averageLatencyMs() {
            return latencySamples == 0 ? 0 : latencySum / latencySamples;
        }
    }

    public RollingWindow(int capacity) {
        this.capacity = Math.max(16, capacity);
    }

    /**
     * Starts a new tick, resetting the per-tick counters.
     *
     * <p>Per-tick figures are what the UI charts and the resolution rule read: cumulative aggregates
     * over a long run decay too slowly to answer "is this still happening?".</p>
     */
    public void beginTick(int tick) {
        if (tick != currentTick) {
            currentTick = tick;
            tickEvents = 0;
            tickErrors = 0;
        }
    }

    public long tickEvents() {
        return tickEvents;
    }

    public long tickErrors() {
        return tickErrors;
    }

    /** Error share of the current tick, 0..1. */
    public double tickErrorRate() {
        return tickEvents == 0 ? 0.0 : (tickErrors * 1.0) / tickEvents;
    }

    public void add(int tick, com.loginsight.model.LogEvent event, long sequence) {
        beginTick(tick);
        generated++;
        tickEvents++;
        com.loginsight.model.LogLevel level = event.getLevel();
        boolean error = level == com.loginsight.model.LogLevel.ERROR || level == com.loginsight.model.LogLevel.FATAL;
        boolean warn = level == com.loginsight.model.LogLevel.WARN;
        if (error) {
            errors++;
            tickErrors++;
        }
        if (warn) {
            warnings++;
        }
        long latency = Math.min(LATENCY_CEILING_MS, Math.max(0, event.getResponseTime()));
        latencySum += latency;
        latencyHistogram[(int) Math.min(LATENCY_BUCKETS - 1, latency / 200)]++;

        ServiceCounter counter = perService.computeIfAbsent(event.getService(), ServiceCounter::new);
        counter.events++;
        if (error) {
            counter.errors++;
        }
        if (warn) {
            counter.warnings++;
        }
        counter.latencySum += latency;
        counter.latencySamples++;

        events.addLast(new SimulatedEvent(sequence, tick, event));
        while (events.size() > capacity) {
            events.removeFirst();
        }
    }

    public int capacity() {
        return capacity;
    }

    public int size() {
        return events.size();
    }

    public long generated() {
        return generated;
    }

    public long errors() {
        return errors;
    }

    public long warnings() {
        return warnings;
    }

    public double errorRate() {
        return generated == 0 ? 0.0 : (errors * 100.0) / generated;
    }

    public long averageLatencyMs() {
        return generated == 0 ? 0 : latencySum / generated;
    }

    /** Latency percentile across the retained window; the histogram is 200 ms wide. */
    public long percentileLatencyMs(double percentile) {
        long total = 0;
        for (long count : latencyHistogram) {
            total += count;
        }
        if (total == 0) {
            return 0;
        }
        long target = (long) Math.ceil(total * Math.max(0.0, Math.min(1.0, percentile)));
        long running = 0;
        for (int index = 0; index < latencyHistogram.length; index++) {
            running += latencyHistogram[index];
            if (running >= target) {
                return (index + 1) * 200L;
            }
        }
        return LATENCY_CEILING_MS;
    }

    public List<ServiceCounter> services() {
        return new ArrayList<>(perService.values());
    }

    public ServiceCounter service(String name) {
        return perService.get(name);
    }

    /** The retained events, oldest first. */
    public List<SimulatedEvent> snapshot() {
        return List.copyOf(events);
    }

    /** The most recent events, newest first, capped at {@code limit}. */
    public List<SimulatedEvent> recent(int limit) {
        List<SimulatedEvent> result = new ArrayList<>(Math.min(limit, events.size()));
        var iterator = events.descendingIterator();
        while (iterator.hasNext() && result.size() < limit) {
            result.add(iterator.next());
        }
        return result;
    }

    /** Concatenated searchable text for the retained events — the Aho-Corasick input. */
    public String searchableText(int limit) {
        StringBuilder builder = new StringBuilder();
        for (SimulatedEvent entry : recent(limit)) {
            builder.append(entry.event().searchableText()).append('\n');
        }
        return builder.toString();
    }

    /** Clears every counter while keeping the configured capacity. */
    public void clear() {
        events.clear();
        perService.clear();
        generated = 0;
        errors = 0;
        warnings = 0;
        latencySum = 0;
        currentTick = Integer.MIN_VALUE;
        tickEvents = 0;
        tickErrors = 0;
        java.util.Arrays.fill(latencyHistogram, 0L);
    }
}
