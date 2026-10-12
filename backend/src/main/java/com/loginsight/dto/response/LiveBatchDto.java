package com.loginsight.dto.response;

import java.util.List;

/**
 * One batch of the live stream (docs/API.md §9). {@code source} is always labelled truthfully:
 * {@code demo-replay} means events are being replayed from the loaded dataset at a bounded rate — a
 * demonstration of streaming, not a real-time feed.
 */
public record LiveBatchDto(long sequence, String source, String dataset, long emittedCount,
                           long total, List<LogEventDto> events) {
}