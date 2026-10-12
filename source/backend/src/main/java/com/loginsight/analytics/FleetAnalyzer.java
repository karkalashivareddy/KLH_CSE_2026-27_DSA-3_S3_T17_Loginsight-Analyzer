package com.loginsight.analytics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.dto.response.HttpStatsDto;
import com.loginsight.dto.response.ServiceStatsDto;
import com.loginsight.model.LogEvent;

/**
 * Service / HTTP / host aggregation analyzers. All analytics are computed live from the dataset;
 * nothing is precomputed or fabricated.
 */
public final class FleetAnalyzer {

    /** Per-service rollups: events, errors, warnings, hosts, latest event, severity histogram. */
    public List<ServiceStatsDto> services(List<LogEvent> events, int limit) {
        Map<String, Accumulator> byService = new LinkedHashMap<>();
        for (LogEvent event : events) {
            String service = event.getService() == null ? "(unknown)" : event.getService();
            byService.computeIfAbsent(service, Accumulator::new).add(event);
        }
        List<Accumulator> ordered = new ArrayList<>(byService.values());
        ordered.sort((a, b) -> Long.compare(b.events, a.events));
        List<ServiceStatsDto> out = new ArrayList<>(Math.min(limit, ordered.size()));
        for (int i = 0; i < Math.min(limit, ordered.size()); i++) {
            out.add(ordered.get(i).toDto());
        }
        return out;
    }

    public ServiceStatsDto service(List<LogEvent> events, String name) {
        Accumulator acc = new Accumulator(name);
        boolean seen = false;
        for (LogEvent event : events) {
            if (name.equals(event.getService())) {
                acc.add(event);
                seen = true;
            }
        }
        if (!seen) {
            return null;
        }
        return acc.toDto();
    }

    /** Latency percentiles over events that carry a response time. */
    public long percentile(List<LogEvent> events, double p) {
        long[] latencies = events.stream()
                .filter(e -> e.getResponseTime() > 0)
                .mapToLong(LogEvent::getResponseTime)
                .sorted()
                .toArray();
        if (latencies.length == 0) {
            return 0;
        }
        int index = (int) Math.ceil(p / 100.0 * latencies.length) - 1;
        return latencies[Math.max(0, Math.min(latencies.length - 1, index))];
    }

    public HttpStatsDto http(List<LogEvent> events) {
        Map<String, Long> statusCodes = new LinkedHashMap<>();
        Map<String, Long> methods = new LinkedHashMap<>();
        Map<String, Long> endpointsCount = new LinkedHashMap<>();
        List<Long> latencies = new ArrayList<>();
        long sampled = 0;
        for (LogEvent event : events) {
            boolean http = event.getHttpMethod() != null || event.getStatusCode() != 0;
            if (!http) {
                continue;
            }
            sampled++;
            if (event.getStatusCode() != 0) {
                statusCodes.merge(Integer.toString(event.getStatusCode()), 1L, Long::sum);
            }
            if (event.getHttpMethod() != null) {
                methods.merge(event.getHttpMethod().name(), 1L, Long::sum);
            }
            if (event.getEndpoint() != null) {
                endpointsCount.merge(event.getEndpoint(), 1L, Long::sum);
            }
            if (event.getResponseTime() > 0) {
                latencies.add(event.getResponseTime());
            }
        }
        List<HttpStatsDto.Endpoint> topEndpoints = new ArrayList<>();
        endpointsCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> topEndpoints.add(new HttpStatsDto.Endpoint(e.getKey(), e.getValue())));
        long[] sorted = latencies.stream().mapToLong(Long::longValue).sorted().toArray();
        long p50 = percentileOf(sorted, 0.50);
        long p95 = percentileOf(sorted, 0.95);
        long max = sorted.length == 0 ? 0 : sorted[sorted.length - 1];
        return new HttpStatsDto(statusCodes, methods, topEndpoints, p50, p95, max, sampled);
    }

    private static long percentileOf(long[] sorted, double p) {
        if (sorted.length == 0) {
            return 0;
        }
        int index = (int) Math.ceil(p * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }

    /** Per-host rollup: events, errors, error rate. */
    public List<Map<String, Object>> hosts(List<LogEvent> events, int limit) {
        Map<String, Accumulator> byHost = new LinkedHashMap<>();
        for (LogEvent event : events) {
            String host = event.getHost() == null ? "(no host)" : event.getHost();
            byHost.computeIfAbsent(host, Accumulator::new).add(event);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        byHost.values().stream()
                .sorted((a, b) -> Long.compare(b.events, a.events))
                .limit(limit)
                .forEach(acc -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("host", acc.name);
                    row.put("events", acc.events);
                    row.put("errors", acc.errors);
                    row.put("warnings", acc.warnings);
                    row.put("errorRate", acc.errorRate());
                    out.add(row);
                });
        return out;
    }

    private static final class Accumulator {
        final String name;
        long events;
        long errors;
        long warnings;
        long latest;
        final java.util.Set<String> hosts = new java.util.LinkedHashSet<>();
        final Map<String, Long> severity = new LinkedHashMap<>();

        Accumulator(String name) {
            this.name = name;
        }

        void add(LogEvent event) {
            events++;
            if (event.getLevel() != null) {
                if (event.getLevel() == com.loginsight.model.LogLevel.ERROR
                        || event.getLevel() == com.loginsight.model.LogLevel.FATAL) {
                    errors++;
                }
                if (event.getLevel() == com.loginsight.model.LogLevel.WARN) {
                    warnings++;
                }
                severity.merge(event.getLevel().name(), 1L, Long::sum);
            }
            if (event.getHost() != null) {
                hosts.add(event.getHost());
            }
            if (event.getTimestamp() != null) {
                latest = Math.max(latest, event.getTimestamp().toEpochMilli());
            }
        }

        double errorRate() {
            return events == 0 ? 0 : (double) errors * 100 / events;
        }

        ServiceStatsDto toDto() {
            return new ServiceStatsDto(name, events, errors, warnings, errorRate(), hosts.size(),
                    latest == 0 ? null : java.time.Instant.ofEpochMilli(latest).toString(), severity);
        }
    }
}