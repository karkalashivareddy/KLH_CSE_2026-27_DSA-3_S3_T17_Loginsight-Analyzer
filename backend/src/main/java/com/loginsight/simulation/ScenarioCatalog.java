package com.loginsight.simulation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The catalogue of live-simulation scenarios exposed by {@code GET /api/scenarios}.
 *
 * <p>Seven scenarios, ordered from the most legible demonstration (a checkout 5xx cascade) to the
 * subtler ones (a deployment regression that raises latency and a new error signature without a 5xx
 * spike). Every entry is declared here rather than generated so the Scenario Lab can render accurate
 * cards and the incident detector can be checked against a documented expectation.</p>
 */
public final class ScenarioCatalog {

    private static final Map<String, ScenarioDefinition> BY_ID = build();

    private static Map<String, ScenarioDefinition> build() {
        Map<String, ScenarioDefinition> map = new LinkedHashMap<>();
        for (ScenarioDefinition definition : List.of(
                checkoutCascade(),
                databaseLatencySpike(),
                authenticationBurst(),
                cacheFailure(),
                trafficSurge(),
                deploymentRegression(),
                paymentTimeout())) {
            map.put(definition.id(), definition);
        }
        return Map.copyOf(map);
    }

    /** Every scenario in presentation order. */
    public static List<ScenarioDefinition> all() {
        return List.copyOf(BY_ID.values());
    }

    /** Looks a scenario up by id, case-insensitively. */
    public static Optional<ScenarioDefinition> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase();
        return BY_ID.values().stream()
                .filter(definition -> definition.id().equals(normalized))
                .findFirst();
    }

    /** The scenario used when a client does not name one. */
    public static ScenarioDefinition defaultScenario() {
        return checkoutCascade();
    }

    private static ScenarioDefinition checkoutCascade() {
        return new ScenarioDefinition(
                "checkout-5xx-cascade",
                "Checkout 5xx Cascade",
                "Payments begins returning 502 to Orders, which surfaces as checkout failures at the API gateway.",
                20260412L,
                240, 12, 40, 168,
                "payments",
                List.of("payments", "orders", "api-gateway"),
                1.65, 0.34, 1850,
                List.of("upstream returned 502", "payment authorization failed", "circuit breaker open"),
                "5xx burst on payments above the sliding-window baseline",
                "Checkout failures caused by payment upstream 502s",
                "CRITICAL");
    }

    private static ScenarioDefinition databaseLatencySpike() {
        return new ScenarioDefinition(
                "database-latency-spike",
                "Database Latency Spike",
                "Connection pool exhaustion on the database service pushes p95 latency up across every data-backed service.",
                771903L,
                240, 16, 48, 176,
                "postgres",
                List.of("postgres", "orders", "payments", "inventory"),
                1.28, 0.14, 2400,
                List.of("connection pool exhausted", "statement timeout", "deadlock detected"),
                "p95 latency on data-backed services above twice the observed baseline",
                "Database latency spike affecting order and payment paths",
                "MAJOR");
    }

    private static ScenarioDefinition authenticationBurst() {
        return new ScenarioDefinition(
                "authentication-burst",
                "Authentication Burst",
                "Token validation failures spike at Auth and are retried by the gateway, multiplying downstream load.",
                31415926L,
                220, 10, 34, 150,
                "auth",
                List.of("auth", "api-gateway", "redis"),
                1.42, 0.27, 940,
                List.of("token validation failed", "jwt signature mismatch", "session store unreachable"),
                "authentication failure burst with gateway retry amplification",
                "Authentication failures caused by token validation errors",
                "MAJOR");
    }

    private static ScenarioDefinition cacheFailure() {
        return new ScenarioDefinition(
                "cache-failure",
                "Cache Failure",
                "The cache becomes unreachable, so every read falls through to the database and saturates its pool.",
                1618033L,
                240, 14, 44, 172,
                "redis",
                List.of("redis", "orders", "auth", "payments"),
                1.35, 0.19, 1650,
                List.of("cache unavailable", "cache miss storm", "read-through fallback engaged"),
                "cache failure with a database fallback storm",
                "Cache outage causing database saturation",
                "MAJOR");
    }

    private static ScenarioDefinition trafficSurge() {
        return new ScenarioDefinition(
                "traffic-surge",
                "Traffic Surge",
                "A marketing push multiplies inbound traffic. Volume climbs sharply, latency follows, and rate limiting engages.",
                99887766L,
                200, 8, 36, 140,
                "api-gateway",
                List.of("api-gateway", "auth", "orders", "payments"),
                2.30, 0.11, 1250,
                List.of("rate limit exceeded", "upstream saturation", "queue depth above threshold"),
                "event rate above twice the observed baseline with queue growth",
                "Traffic surge saturating the edge tier",
                "MINOR");
    }

    private static ScenarioDefinition deploymentRegression() {
        return new ScenarioDefinition(
                "deployment-regression",
                "Deployment Regression",
                "A new Payments build introduces a null-handler regression: elevated latency and a new error signature, but no 5xx cascade.",
                5150071L,
                260, 20, 60, 200,
                "payments",
                List.of("payments", "orders"),
                1.10, 0.12, 1450,
                List.of("NullPointerException in CheckoutHandler", "handler not registered", "feature flag checkout_v3 enabled"),
                "new unmatched error signature after a release marker",
                "Payments regression introduced by the checkout_v3 release",
                "MAJOR");
    }

    private static ScenarioDefinition paymentTimeout() {
        return new ScenarioDefinition(
                "payment-timeout",
                "Payment Timeout",
                "The payment processor accepts connections but stops responding inside the timeout budget.",
                2718281L,
                240, 12, 42, 170,
                "payments",
                List.of("payments", "orders"),
                1.22, 0.21, 3000,
                List.of("connection timeout", "upstream deadline exceeded", "payment capture retry"),
                "payment timeout burst with no status-code change",
                "Payment timeouts starving the checkout path",
                "CRITICAL");
    }

    private ScenarioCatalog() {
    }
}
