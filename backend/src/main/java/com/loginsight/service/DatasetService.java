package com.loginsight.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.loginsight.datasets.DemoDatasetGenerator;
import com.loginsight.exception.DatasetException;
import com.loginsight.model.LogEvent;
import com.loginsight.parser.LogParseResult;
import com.loginsight.parser.LogParser;
import com.loginsight.parser.LogParserFactory;

/**
 * Session-scoped in-memory dataset store (docs/02 §6 and docs/API.md §4). Holds the current
 * {@link Dataset} and hides storage behind methods only, so a future store can be substituted
 * without touching controllers.
 *
 * <p>Beside importing the bundled {@code sample-data} files, this service can generate the
 * deterministic {@linkplain #loadDemo() demo dataset} and ingest an arbitrary raw stream (uploaded
 * or pasted content) into a fresh in-memory dataset.</p>
 */
@Service
public class DatasetService {

    private final LogParserFactory parserFactory = new LogParserFactory();
    private final DemoDatasetGenerator demoGenerator = new DemoDatasetGenerator();
    private final String sampleDataDir;
    private volatile Dataset currentDataset;

    public DatasetService(
            @Value("${loginsight.sample-data.dir:../sample-data}") String sampleDataDir) {
        this.sampleDataDir = sampleDataDir;
    }

    /**
     * Parse a file from the bundled sample-data directory into a new current dataset.
     *
     * @param fileName e.g. {@code logs-medium.txt}
     * @return the freshly loaded dataset
     * @throws DatasetException if the file is missing or unreadable
     */
    public Dataset loadSample(String fileName) {
        Path path = Paths.get(sampleDataDir, fileName);
        if (!Files.exists(path)) {
            throw new DatasetException("Sample dataset not found: " + path.toAbsolutePath());
        }
        LogParseResult result;
        try (InputStream probe = Files.newInputStream(path)) {
            LogParser parser = parserFactory.parserFor(probe);
            try (InputStream in = Files.newInputStream(path)) {
                result = parser.parse(in);
            }
        } catch (IOException e) {
            throw new DatasetException("Failed reading sample dataset '" + fileName + "'", e);
        }
        return install(fileName, result);
    }

    /** Load the built-in deterministic demo dataset (docs/DATASET.md §5). */
    public Dataset loadDemo() {
        List<LogEvent> events = demoGenerator.generate();
        return install(DemoDatasetGenerator.DEMO_NAME, events);
    }

    /**
     * Ingest an arbitrary raw log stream (file upload or pasted content) as a new current dataset.
     *
     * @param name human-readable dataset name
     * @param in   the raw stream, auto-detected as JSONL or canonical text
     * @return the freshly loaded dataset
     * @throws DatasetException if the stream could not be parsed
     */
    public Dataset ingest(String name, InputStream in) {
        byte[] raw;
        try {
            raw = in.readAllBytes();
        } catch (IOException e) {
            throw new DatasetException("Failed reading ingested logs", e);
        }
        if (!name.isEmpty() && raw.length == 0) {
            throw new DatasetException("Log ingestion produced no records");
        }
        LogParseResult result;
        try (InputStream probe = new java.io.ByteArrayInputStream(raw)) {
            LogParser parser = parserFactory.parserFor(probe);
            try (InputStream read = new java.io.ByteArrayInputStream(raw)) {
                result = parser.parse(read);
            }
        } catch (IOException e) {
            throw new DatasetException("Failed reading ingested logs", e);
        }
        if (result == null || result.successfulEvents().isEmpty()) {
            throw new DatasetException("Log ingestion produced no records");
        }
        return install(name == null || name.isBlank() ? "imported-logs" : name, result);
    }

    /** Names of the bundled sample datasets available for import (docs/12 §2). */
    public List<String> availableSamples() {
        Path dir = Paths.get(sampleDataDir);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (java.util.stream.Stream<Path> paths = Files.list(dir)) {
            List<String> names = new ArrayList<>();
            for (Path path : paths.sorted()
                    .filter(p -> !Files.isDirectory(p) && !p.getFileName().toString()
                            .equals("README.md"))
                    .toList()) {
                names.add(path.getFileName().toString());
            }
            return names;
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Drop the current dataset. */
    public void clear() {
        this.currentDataset = null;
    }

    /** Currently loaded dataset, or empty when none has been imported yet. */
    public Optional<Dataset> currentDataset() {
        return Optional.ofNullable(currentDataset);
    }

    /** Page through the current dataset's events in ingestion order. */
    public List<LogEvent> events(int page, int pageSize) {
        Dataset dataset = requireDataset();
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("page and pageSize must both be positive");
        }
        int from = Math.min((page - 1) * pageSize, dataset.size());
        int to = Math.min(from + pageSize, dataset.size());
        return new ArrayList<>(dataset.events().subList(from, to));
    }

    private Dataset install(String name, LogParseResult result) {
        return install(name, assignIds(result.successfulEvents()), result.totalLines(),
                result.failureCount());
    }

    private Dataset install(String name, List<LogEvent> events) {
        return install(name, assignIds(events), events.size(), 0);
    }

    private Dataset install(String name, List<LogEvent> events, int totalLines, int failedLines) {
        Dataset dataset = new Dataset(name, events, totalLines, failedLines, Instant.now());
        this.currentDataset = dataset;
        return dataset;
    }

    private Dataset requireDataset() {
        Dataset dataset = currentDataset;
        if (dataset == null) {
            throw new DatasetException("No dataset loaded");
        }
        return dataset;
    }

    private static List<LogEvent> assignIds(List<LogEvent> events) {
        List<LogEvent> assigned = new ArrayList<>(events.size());
        long id = 0;
        for (LogEvent event : events) {
            assigned.add(event.withId(id++));
        }
        return assigned;
    }
}