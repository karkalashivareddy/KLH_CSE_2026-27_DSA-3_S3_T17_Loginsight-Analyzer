package com.loginsight.datasets;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.loginsight.model.HttpMethod;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * Deterministic demo dataset generator (docs/DATASET.md §5).
 *
 * <p>Produces a realistic, reproducible corpus of structured log events across the eight demo
 * services so the product is immediately usable after startup. The seeded {@link Random} fixes the
 * <em>content</em> (services, messages, patterns, host/user/trace ids); only the wall-clock anchor
 * is taken from the current hour so the corpus always looks recent in the dashboard. Everything here
 * is clearly demo data — never presented as production input.</p>
 *
 * <p>The generator deliberately reuses a small per-service set of message templates (so pattern
 * analysis and repeated-substring features have real signal) and injects a handful of correlated
 * error bursts (so incident detection has genuine windows to find). It honours no {@code java.util}
 * restriction; it is application data-fabrication code, not a course-scoped algorithm.</p>
 */
public final class DemoDatasetGenerator {

    public static final String DEMO_NAME = "Demo Dataset";
    private static final long SEED = 20260913L;
    private static final int DEFAULT_EVENTS = 14_000;

    /** Burst-service events are clustered into the first minutes of their burst hour. */
    private static final long BURST_SPIKE_MS = 10 * 60 * 1000L;

    /** [hourOffset, windowSizeHours, serviceIndex] — correlated error bursts. */
    private static final int[][] BURSTS = {
            {2, 1, 6},   // database "connection refused" episode
            {6, 1, 1},   // auth failure episode
            {11, 1, 6},  // database pool exhaustion
            {15, 1, 1},  // auth lockout episode
            {19, 1, 6},  // database replica lag + timeouts
            {22, 1, 3}   // payment timeout episode
    };

    private static final class Template {
        final String message;
        final LogLevel level;
        final int status;
        final long defaultDurationMs;
        final long durationRangeMs;

        Template(String message, LogLevel level, int status, long defaultDurationMs,
                 long durationRangeMs) {
            this.message = message;
            this.level = level;
            this.status = status;
            this.defaultDurationMs = defaultDurationMs;
            this.durationRangeMs = durationRangeMs;
        }
    }

    private static final class ServiceSpec {
        final String name;
        final String host;
        final String endpoint;
        final List<Template> templates;

        ServiceSpec(String name, String host, String endpoint, List<Template> templates) {
            this.name = name;
            this.host = host;
            this.endpoint = endpoint;
            this.templates = templates;
        }
    }

    private final Random random = new Random(SEED);
    private volatile List<LogEvent> cached;

    /** The generated dataset, cached after first construction. */
    public synchronized List<LogEvent> generate() {
        if (cached != null) {
            return cached;
        }
        cached = generate(DEFAULT_EVENTS);
        return cached;
    }

    /** Regenerate with the given event count (used by tests). Anchored to the current hour. */
    public synchronized List<LogEvent> generate(int eventsCount) {
        return build(eventsCount, Instant.now().truncatedTo(ChronoUnit.HOURS));
    }

    /** Build the corpus anchored at {@code anchor} (the tail end of the 24h span). */
    public List<LogEvent> build(int eventsCount, Instant anchor) {
        List<LogEvent> out = new ArrayList<>(eventsCount);
        long epochStart = anchor.minus(24, ChronoUnit.HOURS).toEpochMilli();
        long spanMs = 24L * 3_600_000L;

        for (int i = 0; i < eventsCount; i++) {
            long uniformTs = epochStart + (long) (random.nextDouble() * spanMs);
            int hourOffset = (int) ((uniformTs - epochStart) / 3_600_000L);
            if (hourOffset > 23) {
                hourOffset = 23;
            }

            int serviceIndex = pickService(hourOffset);
            ServiceSpec service = SERVICES[serviceIndex];
            boolean inBurst = inBurst(hourOffset, serviceIndex);
            long ts = uniformTs;
            if (inBurst) {
                ts = epochStart + (long) hourOffset * 3_600_000L
                        + (long) (random.nextDouble() * BURST_SPIKE_MS);
            }
            Instant timestamp = Instant.ofEpochMilli(ts);
            Template template = pickTemplate(service, inBurst);
            String message = render(template.message);
            int status = template.status;
            HttpMethod method = randomMethod();
            long responseTime = template.defaultDurationMs
                    + (long) (random.nextDouble() * template.durationRangeMs);
            String requestId = "req-" + (10000 + random.nextInt(89000));
            String userId = random.nextDouble() < 0.82 ? "user-" + (1000 + random.nextInt(9000)) : null;
            String ipAddress = fakeIp();
            String raw = rawLine(timestamp, template.level, service, status, method,
                    responseTime, requestId, userId, ipAddress, message);

            out.add(LogEvent.builder()
                    .timestamp(timestamp)
                    .level(template.level)
                    .service(service.name)
                    .host(service.host)
                    .ipAddress(ipAddress)
                    .httpMethod(method)
                    .endpoint(service.endpoint)
                    .statusCode(status)
                    .responseTime(responseTime)
                    .requestId(requestId)
                    .userId(userId)
                    .traceId(hex(32))
                    .spanId(hex(16))
                    .url("https://api.demo.loginsight.local" + service.endpoint)
                    .source("demo-stream")
                    .rawMessage(raw)
                    .message(message)
                    .build());
        }
        return out;
    }

    private static String rawLine(Instant ts, LogLevel level, ServiceSpec service, int status,
                                  HttpMethod method, long responseTime, String requestId,
                                  String userId, String ipAddress, String message) {
        return ts + " | " + level + " | " + service.name + " | " + ipAddress + " | " + method
                + " | " + service.endpoint + " | " + status + " | " + responseTime + " | "
                + requestId + " | " + (userId == null ? "-" : userId) + " | " + message;
    }

    private boolean inBurst(int hourOffset, int serviceIndex) {
        for (int[] burst : BURSTS) {
            if (hourOffset >= burst[0] && hourOffset < burst[0] + burst[1] && burst[2] == serviceIndex) {
                return true;
            }
        }
        return false;
    }

    private int pickService(int hourOffset) {
        double[] weights = new double[SERVICES.length];
        for (int i = 0; i < SERVICES.length; i++) {
            weights[i] = i == 0 ? 26 : 9;
        }
        for (int[] burst : BURSTS) {
            if (hourOffset >= burst[0] && hourOffset < burst[0] + burst[1]) {
                weights[burst[2]] += 900;
            }
        }
        double total = 0;
        for (double w : weights) {
            total += w;
        }
        double r = random.nextDouble() * total;
        double acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (r <= acc) {
                return i;
            }
        }
        return 0;
    }

    private Template pickTemplate(ServiceSpec service, boolean inBurst) {
        List<Template> templates = service.templates;
        if (inBurst) {
            List<Template> loud = new ArrayList<>();
            for (Template template : templates) {
                if (template.level == LogLevel.ERROR || template.level == LogLevel.FATAL) {
                    loud.add(template);
                }
            }
            if (!loud.isEmpty()) {
                return loud.get(random.nextInt(loud.size()));
            }
        }
        return templates.get(random.nextInt(templates.size()));
    }

    private String render(String template) {
        return template
                .replace("{user}", "user-" + (1000 + random.nextInt(9000)))
                .replace("{ip}", fakeIp())
                .replace("{id}", String.valueOf(1000 + random.nextInt(9000)))
                .replace("{ms}", String.valueOf(50 + random.nextInt(4000)))
                .replace("{path}", PATH_POOL[random.nextInt(PATH_POOL.length)]);
    }

    private static final String[] PATH_POOL = {
            "/api/users", "/api/orders", "/api/payments", "/api/inventory", "/api/cart",
            "/health", "/login", "/logout", "/api/notifications", "/api/products"
    };

    private HttpMethod randomMethod() {
        HttpMethod[] methods = HttpMethod.values();
        return methods[random.nextInt(methods.length)];
    }

    private String fakeIp() {
        return (random.nextInt(223) + 1) + "." + random.nextInt(256) + "."
                + random.nextInt(256) + "." + (random.nextInt(253) + 1);
    }

    private String hex(int chars) {
        StringBuilder sb = new StringBuilder(chars);
        for (int i = 0; i < chars; i++) {
            sb.append(Integer.toHexString(random.nextInt(16)));
        }
        return sb.toString();
    }

    private static final ServiceSpec[] SERVICES = {
            new ServiceSpec("api-gateway", "gw-1", "/health", List.of(
                    t("request completed", LogLevel.INFO, 200, 60, 140),
                    t("request completed", LogLevel.INFO, 200, 60, 140),
                    t("request failed", LogLevel.ERROR, 500, 1200, 800),
                    t("slow response: {ms}ms exceeded threshold", LogLevel.WARN, 200, 950, 250),
                    t("rate limit exceeded for client {ip}", LogLevel.WARN, 429, 20, 10),
                    t("route not found: {path}", LogLevel.WARN, 404, 8, 12),
                    t("invalid authentication token", LogLevel.ERROR, 401, 15, 25),
                    t("upstream timeout on {path}", LogLevel.ERROR, 504, 3000, 1500))),
            new ServiceSpec("auth-service", "auth-1", "/login", List.of(
                    t("login successful for {user}", LogLevel.INFO, 200, 90, 110),
                    t("session created for {user}", LogLevel.INFO, 201, 40, 60),
                    t("authentication failed for {user}", LogLevel.ERROR, 401, 55, 45),
                    t("invalid token: expired", LogLevel.ERROR, 401, 20, 30),
                    t("password reset requested", LogLevel.INFO, 202, 120, 80),
                    t("account locked after repeated failures", LogLevel.ERROR, 423, 30, 20),
                    t("user {id} not found", LogLevel.WARN, 404, 15, 15),
                    t("token issued for {user}", LogLevel.DEBUG, 201, 10, 10))),
            new ServiceSpec("user-service", "user-1", "/api/users", List.of(
                    t("user profile updated", LogLevel.INFO, 200, 80, 120),
                    t("user {id} registered", LogLevel.INFO, 201, 150, 100),
                    t("validation error on {path}", LogLevel.WARN, 422, 10, 10),
                    t("profile fetch failed for {user}", LogLevel.ERROR, 500, 400, 300),
                    t("address changed for {user}", LogLevel.DEBUG, 200, 40, 20))),
            new ServiceSpec("payment-service", "pay-1", "/api/payments", List.of(
                    t("payment processed", LogLevel.INFO, 200, 420, 380),
                    t("payment failed: insufficient funds", LogLevel.ERROR, 402, 150, 50),
                    t("payment declined: invalid card", LogLevel.WARN, 402, 90, 60),
                    t("payment timeout", LogLevel.ERROR, 504, 4500, 1500),
                    t("refund issued", LogLevel.INFO, 200, 500, 300),
                    t("retry scheduled for failed payment", LogLevel.DEBUG, 202, 20, 10))),
            new ServiceSpec("order-service", "order-1", "/api/orders", List.of(
                    t("order created", LogLevel.INFO, 201, 200, 150),
                    t("order status updated", LogLevel.INFO, 200, 60, 40),
                    t("order failed: stock unavailable", LogLevel.ERROR, 409, 180, 120),
                    t("order cancelled", LogLevel.INFO, 200, 80, 40),
                    t("cart checkout initiated", LogLevel.DEBUG, 200, 30, 20))),
            new ServiceSpec("notification-service", "notify-1", "/api/notifications", List.of(
                    t("email sent", LogLevel.INFO, 200, 300, 200),
                    t("push notification delivered", LogLevel.INFO, 200, 120, 80),
                    t("sms delivery failed", LogLevel.ERROR, 500, 600, 400),
                    t("notification queue backlog growing", LogLevel.WARN, 200, 100, 50))),
            new ServiceSpec("database", "db-1", "/internal/query", List.of(
                    t("query executed", LogLevel.INFO, 200, 45, 55),
                    t("connection refused", LogLevel.ERROR, 503, 25, 25),
                    t("connection pool exhausted", LogLevel.ERROR, 503, 60, 40),
                    t("query timeout", LogLevel.ERROR, 504, 5000, 2000),
                    t("deadlock detected on table orders", LogLevel.FATAL, 500, 200, 100),
                    t("slow query: {ms}ms", LogLevel.WARN, 200, 1800, 1200),
                    t("replica lag {ms}ms", LogLevel.WARN, 200, 1500, 1000))),
            new ServiceSpec("cache", "cache-1", "/internal/cache", List.of(
                    t("cache hit", LogLevel.DEBUG, 200, 2, 3),
                    t("cache miss", LogLevel.DEBUG, 200, 5, 5),
                    t("cache eviction", LogLevel.INFO, 200, 3, 4),
                    t("redis connection reset", LogLevel.WARN, 200, 30, 40),
                    t("cache write failed", LogLevel.ERROR, 503, 50, 40)))
    };

    private static Template t(String message, LogLevel level, int status, long base, long range) {
        return new Template(message, level, status, base, range);
    }
}