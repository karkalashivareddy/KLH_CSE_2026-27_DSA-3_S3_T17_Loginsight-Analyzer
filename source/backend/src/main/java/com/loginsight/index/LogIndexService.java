package com.loginsight.index;

import org.springframework.stereotype.Service;

import com.loginsight.exception.DatasetException;
import com.loginsight.service.Dataset;
import com.loginsight.service.DatasetService;

/**
 * Builds and caches the {@link LogIndex} for the currently loaded dataset. The index is rebuilt
 * lazily whenever the dataset instance changes; within a dataset it is immutable and safely shared.
 */
@Service
public class LogIndexService {

    private final DatasetService datasetService;
    private volatile Dataset indexedDataset;
    private volatile LogIndex index;

    public LogIndexService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    /**
     * The index of the currently loaded dataset.
     *
     * @throws DatasetException when no dataset is loaded
     */
    public LogIndex current() {
        Dataset dataset = datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"));
        if (index == null || indexedDataset != dataset) {
            this.index = new LogIndex(dataset.events());
            this.indexedDataset = dataset;
        }
        return index;
    }

    /** True when a dataset is loaded and an index can be produced. */
    public boolean isAvailable() {
        return datasetService.currentDataset().isPresent();
    }

    /** Drop the cached index (e.g. after dataset replacement). */
    public void invalidate() {
        this.index = null;
        this.indexedDataset = null;
    }
}