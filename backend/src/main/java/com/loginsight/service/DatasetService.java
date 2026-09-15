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

import com.loginsight.exception.DatasetException;
import com.loginsight.model.LogEvent;
import com.loginsight.parser.LogParseResult;
import com.loginsight.parser.LogParser;
import com.loginsight.parser.LogParserFactory;

/**
 * Session-scoped in-memory dataset store (docs/02 §6). Holds the current {@link Dataset} and hides
 * storage behind methods only, so a future store can be substituted without touching controllers.
 *
 * <p>Phase 2 scope: import named files from the bundled {@code sample-data} directory, clear, and
 * page through events. Streaming upload and the REST layer arrive with the controllers (Phase 10).</p>
 */
@Service
public class DatasetService {

    private final LogParserFactory parserFactory = new LogParserFactory();
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
        Dataset dataset = new Dataset(fileName, assignIds(result.successfulEvents()),
                result.totalLines(), result.failureCount(), Instant.now());
        this.currentDataset = dataset;
        return dataset;
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