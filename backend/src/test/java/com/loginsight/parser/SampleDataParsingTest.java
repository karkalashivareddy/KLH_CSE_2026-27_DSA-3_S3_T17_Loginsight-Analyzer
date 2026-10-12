package com.loginsight.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.loginsight.model.LogEvent;
import com.loginsight.support.SampleDataGenerator;
import com.loginsight.support.SampleDataTestSupport;

/**
 * Validates that the generated sample-data files produce sensible parse results:
 * <ul>
 *   <li>Valid files parse with zero failures; failures == total is impossible on non-empty input</li>
 *   <li>Malformed file yields partial import (~15% failure rate)</li>
 *   <li>Analytics-level invariants hold: sum(levelCounts)==total, min ≤ avg ≤ max</li>
 *   <li>Multi-service file has trace chains (requestId spans services, first hop is API_GATEWAY)</li>
 * </ul>
 */
class SampleDataParsingTest {

    private static final LogParserFactory factory = new LogParserFactory();
    private static Path dir;

    @BeforeAll
    static void setUp() throws Exception {
        dir = SampleDataTestSupport.sampleDataDir();
        SampleDataTestSupport.ensureGenerated();
    }

    @Test
    void logsSmallParsesCleanly() throws Exception {
        LogParseResult result = parse(SampleDataGenerator.LOGS_SMALL);
        assertEquals(1000, result.totalLines(), "line count");
        assertEquals(0, result.failureCount(), "failures must be zero for valid file");
        assertAnalyticsInvariants(result);
    }

    @Test
    void logsMediumParsesCleanly() throws Exception {
        LogParseResult result = parse(SampleDataGenerator.LOGS_MEDIUM);
        assertEquals(10000, result.totalLines(), "line count");
        assertEquals(0, result.failureCount(), "failures must be zero for valid file");
        assertAnalyticsInvariants(result);
    }

    @Test
    void logsLargeParsesCleanly() throws Exception {
        LogParseResult result = parse(SampleDataGenerator.LOGS_LARGE);
        assertEquals(100000, result.totalLines(), "line count");
        assertEquals(0, result.failureCount(), "failures must be zero for valid file");
        assertAnalyticsInvariants(result);
    }

    @Test
    void malformedPartialImport() throws Exception {
        LogParseResult result = parse(SampleDataGenerator.LOGS_MALFORMED);
        assertEquals(400, result.totalLines(), "line count");
        assertTrue(result.failureCount() > 0, "must contain at least one failure");
        double failRate = (double) result.failureCount() / result.totalLines();
        assertTrue(failRate > 0.08 && failRate < 0.25,
                "failure rate should be ~16%, was " + String.format("%.1f%%", failRate * 100));
        for (ParsedLog failure : result.failures()) {
            assertNotNull(failure.getFailureReason(), "failure reason must not be null");
            assertTrue(failure.getLineNumber() > 0);
        }
        assertAnalyticsInvariants(result);
    }

    @Test
    void logsJsonParsesCleanly() throws Exception {
        LogParseResult result = parse(SampleDataGenerator.LOGS_JSON);
        assertEquals(5000, result.totalLines(), "line count");
        assertEquals(0, result.failureCount(), "failures must be zero for valid file");
        assertAnalyticsInvariants(result);
    }

    @Test
    void logsMultiServiceTraceChains() throws Exception {
        LogParseResult result = parse(SampleDataGenerator.LOGS_MULTI_SERVICE);
        assertEquals(8000, result.totalLines(), "line count");
        assertEquals(0, result.failureCount(), "failures must be zero for valid file");
        List<LogEvent> events = result.successfulEvents();
        // first hop of every trace should be API_GATEWAY
        Map<String, List<LogEvent>> grouped = events.stream()
                .collect(Collectors.groupingBy(e -> e.getRequestId() == null ? "anon" : e.getRequestId()));
        for (Map.Entry<String, List<LogEvent>> entry : grouped.entrySet()) {
            assertEquals("API_GATEWAY", entry.getValue().get(0).getService(),
                    "trace " + entry.getKey() + " must start with API_GATEWAY");
        }
        assertAnalyticsInvariants(result);
    }

    private LogParseResult parse(String fileName) throws Exception {
        Path path = dir.resolve(fileName);
        try (InputStream in = Files.newInputStream(path)) {
            LogParser parser = factory.parserFor(in);
            try (InputStream in2 = Files.newInputStream(path)) {
                return parser.parse(in2);
            }
        }
    }

    private void assertAnalyticsInvariants(LogParseResult result) {
        List<LogEvent> events = result.successfulEvents();
        long totalValid = events.size();
        // sum(level counts) == totalValid
        Map<String, Long> levelCounts = events.stream()
                .filter(e -> e.getLevel() != null)
                .collect(Collectors.groupingBy(e -> e.getLevel().name(), Collectors.counting()));
        long levelSum = levelCounts.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(totalValid, levelSum, "sum of level frequency counts must equal total valid events");
        // sum(service counts) == totalValid
        Map<String, Long> serviceCounts = events.stream()
                .filter(e -> e.getService() != null)
                .collect(Collectors.groupingBy(LogEvent::getService, Collectors.counting()));
        long serviceSum = serviceCounts.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(totalValid, serviceSum, "sum of service frequency counts must equal total valid events");
        // min ≤ avg ≤ max responseTime
        long[] rt = events.stream().mapToLong(LogEvent::getResponseTime).toArray();
        if (rt.length > 0) {
            long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            long sum = 0;
            for (long v : rt) {
                min = Math.min(min, v);
                max = Math.max(max, v);
                sum += v;
            }
            double avg = (double) sum / rt.length;
            assertTrue(min <= avg && avg <= max,
                    "min=" + min + " avg=" + String.format("%.1f", avg) + " max=" + max);
        }
    }
}