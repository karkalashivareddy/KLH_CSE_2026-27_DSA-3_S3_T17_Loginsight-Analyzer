package com.loginsight.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.loginsight.support.SampleDataGenerator;
import com.loginsight.support.SampleDataTestSupport;

/**
 * Verifies the six committed sample-data files are present and carry the correct line counts
 * after the deterministic generator has been exercised.
 */
@Tag("gen")
class SampleDataGeneratorTest {

    private static Path dir;

    @BeforeAll
    static void setUp() throws Exception {
        dir = SampleDataTestSupport.sampleDataDir();
        SampleDataGenerator.generateIfMissing(dir);
    }

    @Test
    void logsSmallExists() throws Exception {
        assertLines(SampleDataGenerator.LOGS_SMALL, 1000);
    }

    @Test
    void logsMediumExists() throws Exception {
        assertLines(SampleDataGenerator.LOGS_MEDIUM, 10000);
    }

    @Test
    void logsLargeExists() throws Exception {
        assertLines(SampleDataGenerator.LOGS_LARGE, 100000);
    }

    @Test
    void logsJsonExists() throws Exception {
        assertLines(SampleDataGenerator.LOGS_JSON, 5000);
    }

    @Test
    void logsMultiServiceExists() throws Exception {
        assertLines(SampleDataGenerator.LOGS_MULTI_SERVICE, 8000);
    }

    @Test
    void logsMalformedExists() throws Exception {
        assertLines(SampleDataGenerator.LOGS_MALFORMED, 400);
    }

    private static void assertLines(String fileName, int expected) throws Exception {
        List<String> lines = Files.readAllLines(
                dir.resolve(fileName), StandardCharsets.UTF_8);
        assertEquals(expected, lines.size(),
                fileName + " must contain " + expected + " lines (was " + lines.size() + ")");
    }
}