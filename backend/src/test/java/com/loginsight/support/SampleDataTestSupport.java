package com.loginsight.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Locates the repository's {@code sample-data} directory regardless of where the test JVM started
 * (repo root or {@code backend/}). Guarantees the six committed datasets exist before tests that
 * depend on them run.
 */
public final class SampleDataTestSupport {

    private SampleDataTestSupport() {
    }

    public static Path sampleDataDir() {
        Path cwd = Paths.get("").toAbsolutePath();
        Path direct = cwd.resolve("sample-data");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        return cwd.resolve("..").resolve("sample-data").normalize();
    }

    /** Regenerates files only when missing or empty; committed artifacts stay authoritative. */
    public static void ensureGenerated() throws IOException {
        SampleDataGenerator.generateIfMissing(sampleDataDir());
    }
}