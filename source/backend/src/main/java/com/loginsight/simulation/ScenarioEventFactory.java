package com.loginsight.simulation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Deterministic event generation for a live-simulation scenario.
 *
 * <p>The factory owns the canonical {@link LogEvent} shape used by the whole product, so simulated
 * traffic is indistinguishable in structure from ingested telemetry: same fields, same enums, same
 * searchability. The only difference is provenance, and that is carried explicitly in
 * {@code source} and the {@code scenarioId} attribute.</p>
 *
 * <p>All randomness comes from the session's {@link DeterministicRandom}, so a given
 * {@code (scenario, seed)} pair yields an identical event sequence on every run.</p>
 *
 * <p>Every generated event also receives a <em>deterministic unique identity</em>. It is composed of
 * the session ordinal in the high bits and the per-session emission sequence in the low bits, so it is
 * stable for a given {@code (session, scenario, seed, tick)} and unique across sessions and across
 * events. A generated event is therefore a first-class {@link LogEvent}, not a placeholder row: it can
 * be keyed, linked and de-duplicated by a client exactly like an ingested one.</p>
 */
public final class ScenarioEventFactory {

    /** Wall-clock span one simulated tick represents. */
    public static final long TICK_MILLIS = 250L;

    /**
     * Bits reserved for the per-session emission sequence.
     *
     * <p>The worst case a single session can emit is {@code maxFrames (20 000) * MAX_EVENTS_PER_FRAME
     * (120)} = 2 400 000 events, which fits comfortably in 24 bits. The mask is applied defensively so
     * an unexpected emission rate degrades into a reported id collision rather than an id that bleeds
     * into the neighbouring session's range.</p>
     */
    public static final int SEQUENCE_BITS = 24;

    /** Highest value the per-session sequence may take before it is wrapped by the mask. */
    public static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1L;

    /**
     * First identity handed out by the generated-simulation range.
     *
     * <p>Ingested datasets are assigned sequential ids from 0 by {@code DatasetService}. Generated
     * identities start far above any dataset, so a generated id can never be confused with an ingested
     * one even when both are on screen.</p>
     */
    public static final long FIRST_GENERATED_ID = 1L << SEQUENCE_BITS;

    private static final Map<String, List<Route>> ROUTES = routes();
    private static final List<String> HOST_SUFFIXES = List.of("a", "b", "c");

    private record Route(HttpMethod method, String endpoint, int healthyStatus, int failureStatus) {
    }

    private static Map<String, List<Route>> routes() {
        Map<String, List<Route>> map = new LinkedHashMap<>();
        map.put("api-gateway", List.of(
                new Route(HttpMethod.GET, "/v1/products", 200, 502),
                new Route(HttpMethod.POST, "/v1/checkout", 201, 500),
                new Route(HttpMethod.GET, "/v1/orders/{id}", 200, 502)));
        map.put("auth", List.of(
                new Route(HttpMethod.POST, "/v1/token/verify", 200, 401),
                new Route(HttpMethod.GET, "/v1/session", 200, 401)));
        map.put("orders", List.of(
                new Route(HttpMethod.POST, "/v1/orders", 201, 500),
                new Route(HttpMethod.GET, "/v1/orders/{id}", 200, 502),
                new Route(HttpMethod.PATCH, "/v1/orders/{id}", 200, 500)));
        map.put("payments", List.of(
                new Route(HttpMethod.POST, "/v1/payments/authorize", 200, 502),
                new Route(HttpMethod.POST, "/v1/payments/capture", 201, 500),
                new Route(HttpMethod.GET, "/v1/payments/{id}", 200, 504)));
        map.put("inventory", List.of(
                new Route(HttpMethod.GET, "/v1/inventory/{sku}", 200, 503),
                new Route(HttpMethod.PATCH, "/v1/inventory/{sku}", 200, 503)));
        map.put("postgres", List.of(
                new Route(HttpMethod.POST, "/internal/query", 200, 500),
                new Route(HttpMethod.GET, "/internal/pool", 200, 503)));
        map.put("redis", List.of(
                new Route(HttpMethod.GET, "/internal/cache", 200, 503),
                new Route(HttpMethod.PUT, "/internal/cache", 204, 503)));
        map.put("notifications", List.of(
                new Route(HttpMethod.POST, "/internal/notify", 202, 500)));
        return Map.copyOf(map);
    }

    private static final Map<LogLevel, String> HEALTHY_MESSAGES = Map.of(
            LogLevel.INFO, "request completed",
            LogLevel.DEBUG, "cache lookup completed",
            LogLevel.TRACE, "dispatching request segment");

    private final ScenarioDefinition scenario;
    private final DeterministicRandom random;
    private final Instant origin;
    private final long idBase;
    private long sequence;

    public ScenarioEventFactory(ScenarioDefinition scenario, DeterministicRandom random, Instant origin) {
        this(scenario, random, origin, 0L);
    }

    /**
     * @param sessionOrdinal process-wide session counter; makes every emitted id unique across
     *     concurrently running sessions. Identical for a given run, so ids stay reproducible.
     */
    public ScenarioEventFactory(ScenarioDefinition scenario, DeterministicRandom random, Instant origin,
                                long sessionOrdinal) {
        this.scenario = scenario;
        this.random = random;
        this.origin = origin;
        this.idBase = FIRST_GENERATED_ID + (Math.max(0L, sessionOrdinal) << SEQUENCE_BITS);
    }

    /**
     * Deterministic identity for the next emitted event: the session's id range plus its emission
     * sequence. Never negative, never {@code -1}, and stable for a given run.
     */
    public long nextEventId() {
        return idBase + (sequence & SEQUENCE_MASK);
    }

    /**
     * Generates every event for one tick.
     *
     * @param tick         zero-based tick index; drives both timestamps and scenario intensity
     * @param healthyRate  baseline events/second across the whole fleet
     * @return the tick's events in emission order
     */
    public List<LogEvent> generate(int tick, double healthyRate) {
        double intensity = scenario.intensityAt(tick);
        double rate = scenario.rateAt(tick, healthyRate);
        int count = Math.max(1, (int) Math.round(rate * (TICK_MILLIS / 1000.0)));
        List<LogEvent> events = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            events.add(next(tick, intensity, index, count));
        }
        return events;
    }

    private LogEvent next(int tick, double intensity, int index, int tickCount) {
        String service = pickService(tick, intensity);
        Route route = random.pick(ROUTES.getOrDefault(service, ROUTES.get("api-gateway")));
        boolean failing = inFailure(service, intensity) && random.chance(failureProbability(tick));
        double serviceIntensity = inFailure(service, intensity) ? intensity : 0.0;

        LogLevel level = resolveLevel(failing, serviceIntensity);
        int status = failing ? route.failureStatus() : route.healthyStatus();
        long responseTime = resolveLatency(serviceIntensity, failing);
        Instant timestamp = origin
                .plusMillis(tick * TICK_MILLIS)
                .plusMillis((long) ((index * TICK_MILLIS) / Math.max(1, tickCount)));
        long eventId = nextEventId();
        sequence++;

        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("scenarioId", scenario.id());
        attributes.put("tick", Integer.toString(tick));
        attributes.put("source", "live-simulation");
        attributes.put("intensity", String.format(java.util.Locale.ROOT, "%.2f", serviceIntensity));
        if ("deployment-regression".equals(scenario.id()) && tick >= scenario.onsetTick() - 4
                && tick < scenario.onsetTick() + 2) {
            attributes.put("release", "checkout_v3");
            attributes.put("build", "2026.04.12-rc3");
        }

        return LogEvent.builder()
                .id(eventId)
                .timestamp(timestamp)
                .level(level)
                .service(service)
                .host(hostFor(service))
                .ipAddress("10.14." + (Math.floorMod(service.hashCode(), 200) + 10) + "." + (sequence % 200 + 5))
                .httpMethod(route.method())
                .endpoint(route.endpoint())
                .statusCode(status)
                .responseTime(responseTime)
                .requestId("req-" + Long.toHexString(origin.toEpochMilli()) + "-" + sequence)
                .userId("u-" + (sequence % 900 + 100))
                .message(message(level, service, route, status, responseTime, serviceIntensity))
                .traceId("tr-" + Long.toHexString(origin.toEpochMilli() + (sequence / 3)) + "-" + sequence)
                .spanId("sp-" + Long.toHexString(sequence * 7919L))
                .url("https://api.loginsight.local" + route.endpoint())
                .source("live-simulation")
                .rawMessage(null)
                .attributes(attributes)
                .build();
    }

    /**
     * Chooses the emitting service. Failing services are over-represented during the failure window
     * so the visible event stream tracks the affected tier instead of staying flat.
     */
    private String pickService(int tick, double intensity) {
        List<String> affected = scenario.affectedServices();
        if (intensity > 0.02 && !affected.isEmpty() && random.chance(0.34 + 0.5 * intensity)) {
            return random.pick(affected);
        }
        double roll = random.nextDouble();
        double total = 0;
        for (SimulationTopology.ServiceNode node : SimulationTopology.SERVICES) {
            total += node.baseShare();
            if (roll * totalOfShares() <= total) {
                return node.id();
            }
        }
        return "api-gateway";
    }

    private static double totalOfShares() {
        return SimulationTopology.SERVICES.stream()
                .mapToDouble(SimulationTopology.ServiceNode::baseShare)
                .sum();
    }

    private boolean inFailure(String service, double intensity) {
        return intensity > 0.02 && scenario.affectedServices().contains(service);
    }

    private double failureProbability(int tick) {
        double base = Math.min(0.92, scenario.peakErrorRate() * 1.6);
        double intensity = scenario.intensityAt(tick);
        return base * (0.15 + 0.85 * intensity);
    }

    private LogLevel resolveLevel(boolean failing, double intensity) {
        if (!failing) {
            if (random.chance(0.08)) {
                return LogLevel.DEBUG;
            }
            if (random.chance(0.012)) {
                return LogLevel.WARN;
            }
            return random.chance(0.02) ? LogLevel.TRACE : LogLevel.INFO;
        }
        double roll = random.nextDouble();
        if (roll < 0.11 + 0.16 * intensity) {
            return LogLevel.FATAL;
        }
        if (roll < 0.5 + 0.3 * intensity) {
            return LogLevel.WARN;
        }
        return LogLevel.ERROR;
    }

    private long resolveLatency(double intensity, boolean failing) {
        long healthy = 90;
        long target = Math.max(healthy, scenario.peakLatencyMs());
        long base = Math.round(healthy + (target - healthy) * intensity);
        long jitter = Math.round(base * random.between(0.65, 1.55));
        if (failing && random.chance(0.22)) {
            jitter = Math.round(jitter * random.between(1.8, 3.4));
        }
        return Math.max(1, jitter);
    }

    private String message(LogLevel level, String service, Route route, int status, long responseTime, double intensity) {
        if (intensity > 0.12 && (level == LogLevel.ERROR || level == LogLevel.FATAL || level == LogLevel.WARN)) {
            String signature = scenario.errorSignatures()
                    .get(random.nextInt(Math.max(1, scenario.errorSignatures().size())));
            return signature + " in " + service + " (" + route.method() + " " + route.endpoint() + ")";
        }
        if (level == LogLevel.FATAL) {
            return "unrecoverable handler failure in " + service;
        }
        if (level == LogLevel.ERROR) {
            return "request failed in " + service + " with status " + status;
        }
        if (level == LogLevel.WARN) {
            return "elevated latency in " + service + " (" + responseTime + " ms)";
        }
        String base = HEALTHY_MESSAGES.getOrDefault(level, "request completed");
        return base + " in " + service + " (" + route.method() + " " + route.endpoint() + " " + status + " "
                + responseTime + "ms)";
    }

    private static String hostFor(String service) {
        int index = Math.floorMod(service.hashCode(), HOST_SUFFIXES.size());
        return service + "-" + HOST_SUFFIXES.get(index);
    }

    public long emitted() {
        return sequence;
    }
}
