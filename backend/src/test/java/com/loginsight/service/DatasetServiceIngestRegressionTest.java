package com.loginsight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.loginsight.parser.ParserException;

/**
 * Regression guard for {@link DatasetService#ingest(String, InputStream)}.
 *
 * <p>The old guard was {@code !name.isEmpty() && raw.length == 0}: it dereferenced a possibly-null
 * name before normalising it (so {@code ingest(null, in)} threw a raw {@code NullPointerException}
 * and the {@code name == null} fallback two lines further down was unreachable), and the name
 * conjunct meant a blank name silently skipped the empty-stream rejection. A null stream also
 * reached {@code readNBytes} deep inside {@code readBounded}.</p>
 */
class DatasetServiceIngestRegressionTest {

    private static final String ONE_JSONL_RECORD = "{\"timestamp\":\"2026-09-13T10:00:01Z\","
            + "\"level\":\"ERROR\",\"message\":\"boom\"}\n";

    private static DatasetService service() {
        return new DatasetService("unused-for-ingest");
    }

    private static ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void nullNameFallsBackToTheImportedLogsNameInsteadOfThrowing() {
        Dataset dataset = service().ingest(null, stream(ONE_JSONL_RECORD));
        assertEquals("imported-logs", dataset.name());
        assertEquals(1, dataset.size());
    }

    @Test
    void blankNameFallsBackToTheImportedLogsName() {
        assertEquals("imported-logs", service().ingest("", stream(ONE_JSONL_RECORD)).name());
        assertEquals("imported-logs", service().ingest("   ", stream(ONE_JSONL_RECORD)).name());
    }

    @Test
    void suppliedNameIsAcceptedUnchangedApartFromTrimming() {
        assertEquals("inline", service().ingest("inline", stream(ONE_JSONL_RECORD)).name());
        assertEquals("inline", service().ingest("  inline  ", stream(ONE_JSONL_RECORD)).name());
    }

    @Test
    void emptyStreamIsRejectedRegardlessOfTheSuppliedName() {
        DatasetService service = service();
        assertThrows(ParserException.class, () -> service.ingest("named", stream("")));
        assertThrows(ParserException.class, () -> service.ingest("", stream("")));
        assertThrows(ParserException.class, () -> service.ingest(null, stream("")));
        assertTrue(service.currentDataset().isEmpty(),
                "a rejected stream must not install a dataset");
    }

    @Test
    void blankOnlyStreamIsAlsoRejected() {
        // Every line is blank, so no format can be detected from the probe stream.
        assertThrows(ParserException.class, () -> service().ingest("named", stream("\n\n   \n")));
    }

    @Test
    void nullStreamIsRejectedWithAnIllegalArgument() {
        DatasetService service = service();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.ingest("named", null));
        assertTrue(thrown.getMessage().contains("must not be null"), thrown.getMessage());
        assertThrows(IllegalArgumentException.class, () -> service.ingest(null, null));
        assertTrue(service.currentDataset().isEmpty(),
                "a null stream must not install a dataset");
    }

    @Test
    void oversizedStreamIsStillRejectedAsADatasetFailure() {
        byte[] tooBig = new byte[DatasetService.MAX_INGEST_BYTES + 1];
        java.util.Arrays.fill(tooBig, (byte) '\n');
        assertThrows(com.loginsight.exception.DatasetException.class,
                () -> service().ingest(null, new ByteArrayInputStream(tooBig)));
    }
}