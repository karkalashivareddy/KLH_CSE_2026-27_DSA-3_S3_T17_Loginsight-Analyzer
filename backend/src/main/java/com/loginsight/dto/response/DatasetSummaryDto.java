package com.loginsight.dto.response;

/**
 * Stable summary of a loaded or imported dataset (docs/API.md §4). Every field is derived from the
 * actual parse/ingest result — counts are never guessed.
 */
public record DatasetSummaryDto(String loaded, String datasetName, int size, int totalLines,
                                int failedLines, String loadedAt, String note) {

    public static DatasetSummaryDto from(com.loginsight.service.Dataset dataset, String note) {
        return new DatasetSummaryDto("true", dataset.name(), dataset.size(), dataset.totalLines(),
                dataset.failedLines(), dataset.loadedAt().toString(), note);
    }

    public static DatasetSummaryDto absent(String note) {
        return new DatasetSummaryDto("false", null, 0, 0, 0, null, note);
    }
}