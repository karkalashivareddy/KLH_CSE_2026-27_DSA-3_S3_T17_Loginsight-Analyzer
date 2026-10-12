package com.loginsight.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.loginsight.exception.DatasetException;
import com.loginsight.model.LogEvent;
import com.loginsight.support.SampleDataGenerator;
import com.loginsight.support.SampleDataTestSupport;

class DatasetServiceTest {

    private static String sampleDataDir;

    @BeforeAll
    static void setUp() throws Exception {
        sampleDataDir = SampleDataTestSupport.sampleDataDir().toString();
        SampleDataTestSupport.ensureGenerated();
    }

    private DatasetService service() {
        return new DatasetService(sampleDataDir);
    }

    @Test
    void loadSampleParsesAndCounts() {
        DatasetService service = service();
        Dataset dataset = service.loadSample(SampleDataGenerator.LOGS_SMALL);
        assertEquals(1000, dataset.size());
        assertEquals(1000, dataset.totalLines());
        assertEquals(0, dataset.failedLines());
        assertEquals(SampleDataGenerator.LOGS_SMALL, dataset.name());
        assertNotNull(dataset.loadedAt());
    }

    @Test
    void loadSampleAssignsSequentialIds() {
        DatasetService service = service();
        Dataset dataset = service.loadSample(SampleDataGenerator.LOGS_JSON);
        List<LogEvent> events = dataset.events();
        assertFalse(events.isEmpty());
        for (int i = 0; i < events.size(); i++) {
            assertEquals(i, events.get(i).getId(), "ids must be sequential from 0");
        }
    }

    @Test
    void loadSampleMissingFileThrows() {
        DatasetService service = service();
        assertThrows(DatasetException.class, () -> service.loadSample("does-not-exist.txt"));
    }

    @Test
    void loadSampleRejectsPathTraversal() {
        DatasetService service = service();
        assertThrows(DatasetException.class, () -> service.loadSample("../pom.xml"));
        assertThrows(DatasetException.class, () -> service.loadSample("a/../../pom.xml"));
        assertThrows(DatasetException.class, () -> service.loadSample("..\\pom.xml"));
        assertThrows(DatasetException.class, () -> service.loadSample(sampleDataDir + "/../pom.xml"),
                "an absolute path that normalises outside the directory is rejected");
        assertThrows(DatasetException.class, () -> service.loadSample(sampleDataDir),
                "the directory itself is not a sample file");
        assertThrows(DatasetException.class, () -> service.loadSample(""));
        assertThrows(DatasetException.class, () -> service.loadSample(null));
        assertTrue(service.currentDataset().isEmpty(), "a rejected name must not install a dataset");
    }

    @Test
    void ingestRejectsAnOversizedStream() {
        DatasetService service = service();
        byte[] tooBig = new byte[DatasetService.MAX_INGEST_BYTES + 1];
        java.util.Arrays.fill(tooBig, (byte) '\n');
        assertThrows(DatasetException.class, () -> service.ingest("huge",
                new java.io.ByteArrayInputStream(tooBig)));
    }

    @Test
    void ingestStillAcceptsARegularStream() {
        DatasetService service = service();
        String jsonl = "{\"timestamp\":\"2026-09-13T10:00:01Z\",\"level\":\"ERROR\","
                + "\"message\":\"boom\"}\n";
        Dataset dataset = service.ingest("inline", new java.io.ByteArrayInputStream(
                jsonl.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals(1, dataset.size());
    }

    @Test
    void clearDropsCurrentDataset() {
        DatasetService service = service();
        service.loadSample(SampleDataGenerator.LOGS_SMALL);
        assertTrue(service.currentDataset().isPresent());
        service.clear();
        assertTrue(service.currentDataset().isEmpty());
    }

    @Test
    void eventsPaging() {
        DatasetService service = service();
        service.loadSample(SampleDataGenerator.LOGS_SMALL);
        List<LogEvent> page1 = service.events(1, 100);
        List<LogEvent> page2 = service.events(2, 100);
        assertEquals(100, page1.size());
        assertEquals(100, page2.size());
        assertEquals(0, page1.get(0).getId());
        assertNotEquals(page1.get(0).getId(), page2.get(0).getId(), "pages must not overlap");
        assertEquals(100, service.events(10, 100).size(), "1000 is divisible by 100, last page is full");
        assertTrue(service.events(11, 100).isEmpty(), "page beyond the end is empty");
    }

    @Test
    void eventsRequireLoadedDataset() {
        DatasetService service = service();
        assertThrows(DatasetException.class, () -> service.events(1, 10));
    }

    @Test
    void eventsRejectNonPositivePagingArgs() {
        DatasetService service = service();
        service.loadSample(SampleDataGenerator.LOGS_SMALL);
        assertThrows(IllegalArgumentException.class, () -> service.events(0, 10));
        assertThrows(IllegalArgumentException.class, () -> service.events(1, 0));
    }
}