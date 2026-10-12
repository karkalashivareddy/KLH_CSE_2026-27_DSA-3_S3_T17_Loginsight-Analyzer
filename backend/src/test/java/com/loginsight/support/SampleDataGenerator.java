package com.loginsight.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic generator for the bundled sample datasets (sample-data/README.md).
 *
 * <p>Every documented file is produced from the fixed seed {@link #SEED}, so running this helper
 * twice yields byte-identical files. Files are committed as static artifacts; {@code
 * generateIfMissing} is used by the tests, and {@code generateAll} is available for one-off
 * regeneration.</p>
 */
public final class SampleDataGenerator {

    public static final long SEED = 20260913L;

    public static final String LOGS_SMALL = "logs-small.txt";
    public static final String LOGS_MEDIUM = "logs-medium.txt";
    public static final String LOGS_LARGE = "logs-large.txt";
    public static final String LOGS_MALFORMED = "logs-malformed.txt";
    public static final String LOGS_JSON = "logs-json.jsonl";
    public static final String LOGS_MULTI_SERVICE = "logs-multi-service.txt";

    private static final String[] SERVICES = {
            "API_GATEWAY", "AUTH", "USER", "PAYMENT", "INVENTORY", "ORDER", "DATABASE", "NOTIFICATION",
    };
    private static final String[] LEVELS = {"INFO", "DEBUG", "WARN", "ERROR"};
    private static final String[] METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH"};
    private static final String[] ENDPOINTS = {
            "/login", "/logout", "/api/users", "/api/orders", "/api/orders/{id}",
            "/api/payments", "/api/inventory", "/api/notifications", "/health",
    };
    private static final int[] STATUS_OK = {200, 201, 204};
    private static final int[] STATUS_ERROR = {400, 401, 403, 404, 500, 502, 503};

    private static final String[][] ERROR_TEMPLATES = {
            {"authentication failed", "AUTH"},
            {"connection timeout", null},
            {"database connection failed", "DATABASE"},
            {"payment service unavailable", "PAYMENT"},
            {"inventory lookup failed", "INVENTORY"},
    };
    private static final String[] INFO_MESSAGES = {
            "request completed", "cache miss", "user session created", "order processed successfully",
            "notification sent", "data validation failed", "resource not found",
    };

    private SampleDataGenerator() {
    }

    /** Regenerate every file unconditionally (used for one-off regeneration of the artifacts). */
    public static void generateAll(Path sampleDataDir) throws IOException {
        Files.createDirectories(sampleDataDir);
        Random rnd = new RandomFixed(SEED);
        writeFile(sampleDataDir.resolve(LOGS_SMALL), generateText(1000, rnd));
        writeFile(sampleDataDir.resolve(LOGS_MEDIUM), generateText(10000, rnd));
        writeFile(sampleDataDir.resolve(LOGS_LARGE), generateText(100000, rnd));
        writeFile(sampleDataDir.resolve(LOGS_MALFORMED), generateMalformed(400, rnd));
        writeFile(sampleDataDir.resolve(LOGS_JSON), generateJsonl(5000, rnd));
        writeFile(sampleDataDir.resolve(LOGS_MULTI_SERVICE), generateMultiService(8000, rnd));
    }

    /** Regenerate only files that are missing or empty, so committed artifacts stay authoritative. */
    public static void generateIfMissing(Path sampleDataDir) throws IOException {
        Files.createDirectories(sampleDataDir);
        boolean missing = !Files.exists(sampleDataDir.resolve(LOGS_SMALL))
                || !Files.exists(sampleDataDir.resolve(LOGS_MEDIUM))
                || !Files.exists(sampleDataDir.resolve(LOGS_LARGE))
                || !Files.exists(sampleDataDir.resolve(LOGS_MALFORMED))
                || !Files.exists(sampleDataDir.resolve(LOGS_JSON))
                || !Files.exists(sampleDataDir.resolve(LOGS_MULTI_SERVICE));
        if (missing) {
            generateAll(sampleDataDir);
        }
    }

    private static void writeFile(Path path, List<String> lines) throws IOException {
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static List<String> generateText(int count, Random rnd) {
        List<String> lines = new ArrayList<>(count);
        Instant base = Instant.parse("2026-09-13T00:00:00Z");
        for (int i = 0; i < count; i++) {
            String service = SERVICES[rnd.nextInt(SERVICES.length)];
            String level = pickLevel(rnd, service);
            String method = METHODS[rnd.nextInt(METHODS.length)];
            String endpoint = ENDPOINTS[rnd.nextInt(ENDPOINTS.length)];
            int status = (rnd.nextInt(4) == 0) ? STATUS_ERROR[rnd.nextInt(STATUS_ERROR.length)]
                    : STATUS_OK[rnd.nextInt(STATUS_OK.length)];
            if ("AUTH".equals(service)) {
                status = rnd.nextInt(2) == 0 ? 401 : status;
            }
            boolean anon = rnd.nextInt(5) == 0;
            String userId = anon ? "-" : "user-" + (100 + rnd.nextInt(9000));
            lines.add(join(
                    base.plus(i, ChronoUnit.MILLIS).toString(),
                    level,
                    service,
                    ip(rnd),
                    method,
                    endpoint,
                    String.valueOf(status),
                    String.valueOf(1 + rnd.nextInt(1500)),
                    "req-" + (1000 + rnd.nextInt(90000)),
                    userId,
                    pickMessage(rnd, level, service)));
        }
        return lines;
    }

    private static List<String> generateMultiService(int count, Random rnd) {
        List<String> lines = new ArrayList<>(count);
        Instant base = Instant.parse("2026-09-13T00:00:00Z");
        int lineIndex = 0;
        int traceSeq = 0;
        while (lineIndex < count) {
            String requestId = "req-" + (traceSeq++);
            int chainLength = 2 + rnd.nextInt(3);
            List<String> chain = new ArrayList<>(chainLength);
            chain.add("API_GATEWAY");
            while (chain.size() < chainLength) {
                chain.add(SERVICES[1 + rnd.nextInt(SERVICES.length - 1)]);
            }
            for (int hop = 0; hop < chainLength && lineIndex < count; hop++, lineIndex++) {
                String service = chain.get(hop);
                String level = pickLevel(rnd, service);
                String method = METHODS[rnd.nextInt(METHODS.length)];
                String endpoint = ENDPOINTS[rnd.nextInt(ENDPOINTS.length)];
                int status = STATUS_OK[rnd.nextInt(STATUS_OK.length)];
                boolean anon = rnd.nextInt(5) == 0;
                lines.add(join(
                        base.plus(lineIndex, ChronoUnit.MILLIS).toString(),
                        level,
                        service,
                        ip(rnd),
                        method,
                        endpoint,
                        String.valueOf(status),
                        String.valueOf(1 + rnd.nextInt(1500)),
                        requestId,
                        anon ? "-" : "user-" + (100 + rnd.nextInt(9000)),
                        pickMessage(rnd, level, service)));
            }
        }
        return lines;
    }

    private static List<String> generateJsonl(int count, Random rnd) {
        List<String> lines = new ArrayList<>(count);
        Instant base = Instant.parse("2026-09-13T00:00:00Z");
        for (int i = 0; i < count; i++) {
            String service = SERVICES[rnd.nextInt(SERVICES.length)];
            String level = pickLevel(rnd, service);
            lines.add(jsonLine(base, i, rnd, service, level, "host-" + (1 + rnd.nextInt(8))));
        }
        return lines;
    }

    private static List<String> generateMalformed(int target, Random rnd) {
        List<String> lines = new ArrayList<>(target);
        Instant base = Instant.parse("2026-09-13T00:00:00Z");
        int i = 0;
        while (lines.size() < target) {
            String service = SERVICES[i % SERVICES.length];
            int mode = rnd.nextInt(25);
            if (mode == 0) {
                lines.add(badTimestampLine(base, i));
            } else if (mode == 1) {
                lines.add(missingFieldsLine(base, i));
            } else if (mode == 2) {
                lines.add(badLevelLine(base, i));
            } else if (mode == 3) {
                lines.add(strayPipeLine(base, i));
            } else {
                lines.add(join(
                        base.plus(i, ChronoUnit.MILLIS).toString(),
                        "INFO",
                        service,
                        ip(rnd),
                        "GET",
                        ENDPOINTS[i % ENDPOINTS.length],
                        "200",
                        "12",
                        "req-" + (1000 + i),
                        "-",
                        "request completed"));
            }
            i++;
        }
        return lines;
    }

    private static String badTimestampLine(Instant base, int i) {
        String badTime = i % 2 == 0 ? "not-a-timestamp" : "2026-02-30T25:00:00Z";
        return join(badTime, "INFO", "USER", "10.0.0.1", "GET", "/api/users", "200", "12",
                "req-" + (1000 + i), "-", "request completed");
    }

    private static String missingFieldsLine(Instant base, int i) {
        return join(base.plus(i, ChronoUnit.MILLIS).toString(), "INFO", "USER", "10.0.0.1");
    }

    private static String badLevelLine(Instant base, int i) {
        return join(base.plus(i, ChronoUnit.MILLIS).toString(), "ALERT", "AUTH", "10.0.0.1",
                "POST", "/login", "500", "23", "req-" + (1000 + i), "-", "unknown level");
    }

    private static String strayPipeLine(Instant base, int i) {
        return join(base.plus(i, ChronoUnit.MILLIS).toString(), "ERROR", "DATABASE", "10.0.0.1",
                "GET", "/db", "500", "99", "req-" + (1000 + i), "-",
                "query failed | retry occurred");
    }

    private static String jsonLine(Instant base, int i, Random rnd, String service, String level,
                                   String host) {
        StringBuilder sb = new StringBuilder("{\"timestamp\":\"");
        sb.append(base.plus(i, ChronoUnit.MILLIS).toString());
        sb.append("\",\"level\":\"").append(level);
        sb.append("\",\"service\":\"").append(service);
        sb.append("\",\"host\":\"").append(host);
        sb.append("\",\"ipAddress\":\"").append(ip(rnd));
        sb.append("\",\"httpMethod\":\"").append(METHODS[rnd.nextInt(METHODS.length)]);
        sb.append("\",\"endpoint\":\"").append(ENDPOINTS[rnd.nextInt(ENDPOINTS.length)]);
        int status = STATUS_OK[rnd.nextInt(STATUS_OK.length)];
        sb.append("\",\"statusCode\":").append(status);
        sb.append(",\"responseTime\":").append(1 + rnd.nextInt(1500));
        sb.append(",\"requestId\":\"req-").append(1000 + rnd.nextInt(90000));
        sb.append("\",\"userId\":\"user-").append(100 + rnd.nextInt(9000));
        sb.append("\",\"message\":\"").append(escapeJson(pickMessage(rnd, level, service)));
        sb.append("\"}");
        return sb.toString();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String pickLevel(Random rnd, String service) {
        int roll = rnd.nextInt(10);
        if ("DATABASE".equals(service) || "AUTH".equals(service)) {
            if (roll < 3) {
                return "ERROR";
            }
        }
        if (roll < 4) {
            return "ERROR";
        }
        if (roll < 5) {
            return "WARN";
        }
        if (roll < 8) {
            return "INFO";
        }
        return "DEBUG";
    }

    private static String pickMessage(Random rnd, String level, String service) {
        if ("ERROR".equals(level)) {
            if (rnd.nextInt(3) == 0) {
                return ERROR_TEMPLATES[rnd.nextInt(ERROR_TEMPLATES.length)][0];
            }
        }
        if ("WARN".equals(level)) {
            return "retry scheduled after " + (1 + rnd.nextInt(5)) + " attempts";
        }
        return INFO_MESSAGES[rnd.nextInt(INFO_MESSAGES.length)];
    }

    private static String ip(Random rnd) {
        return (1 + rnd.nextInt(223)) + "." + rnd.nextInt(256) + "."
                + rnd.nextInt(256) + "." + (1 + rnd.nextInt(254));
    }

    private static String join(String... parts) {
        return String.join(" | ", parts);
    }

    /**
     * Deterministic {@link Random} wrapper: guarantees identical sequences for the same seed on
     * every JVM so regenerated artifacts are reproducible.
     */
    private static final class RandomFixed extends Random {
        private RandomFixed(long seed) {
            super(seed);
        }
    }
}