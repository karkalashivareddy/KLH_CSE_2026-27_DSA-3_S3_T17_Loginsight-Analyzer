package com.loginsight.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.loginsight.dto.response.LiveBatchDto;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.exception.DatasetException;
import com.loginsight.model.LogEvent;

/**
 * The labelled live stream (docs/API.md §9).
 *
 * <p>Because the backend owns static datasets, the stream is an honest replay: on subscription the
 * events of the currently loaded dataset are emitted oldest-first in bounded batches at a
 * configurable pace, and the stream reports {@code replay-complete} when the dataset is exhausted
 * instead of inventing traffic. The UI labels the source "demo replay stream — not real-time".</p>
 */
@Service
public class LiveStreamService {

    private static final Logger LOG = LoggerFactory.getLogger(LiveStreamService.class);

    private final DatasetService datasetService;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread thread = new Thread(r, "loginsight-live");
                thread.setDaemon(true);
                return thread;
            });

    public LiveStreamService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    /** Subscribe a replay stream; batches of {@code batchSize} emitted every {@code intervalMillis}. */
    public SseEmitter subscribe(int batchSize, long intervalMillis) {
        int size = Math.max(1, Math.min(200, batchSize));
        long interval = Math.max(100L, intervalMillis);
        SseEmitter emitter = new SseEmitter(0L);
        List<LogEvent> events = currentEvents();
        emitter.onCompletion(() -> Thread.currentThread().interrupt());
        emitter.onTimeout(() -> Thread.currentThread().interrupt());
        emitter.onError(t -> Thread.currentThread().interrupt());
        startReplay(emitter, events, size, interval);
        return emitter;
    }

    /** Long-running replay of the current dataset, oldest first, at a bounded rate. */
    private void startReplay(SseEmitter emitter, List<LogEvent> events, int batchSize,
                             long interval) {
        scheduler.execute(() -> {
            try {
                long sequence = 0;
                long emitted = 0;
                int total = events.size();
                int cursor = 0;
                emitter.send(SseEmitter.event().name("start")
                        .data(Map.of("source", "demo-replay", "dataset", datasetName(),
                                "total", total, "batchSize", batchSize,
                                "paceMs", interval,
                                "label", "Demo replay stream — events are replayed from the "
                                        + "loaded dataset, not captured in real time.")));
                while (cursor < total) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    int from = cursor;
                    int to = Math.min(total, cursor + batchSize);
                    List<LogEventDto> dtos = new ArrayList<>(to - from);
                    for (int i = from; i < to; i++) {
                        dtos.add(LogEventDto.from(events.get(i)));
                    }
                    cursor = to;
                    emitted = cursor;
                    sequence++;
                    emitter.send(SseEmitter.event().name("batch")
                            .data(new LiveBatchDto(sequence, "demo-replay", datasetName(),
                                    emitted, total, dtos)));
                    if (cursor < total) {
                        Thread.sleep(interval);
                    }
                }
                emitter.send(SseEmitter.event().name("replay-complete")
                        .data(Map.of("emitted", emitted, "total", total)));
                emitter.complete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.debug("Live stream closed: {}", e.getMessage());
                try {
                    emitter.completeWithError(e);
                } catch (IllegalStateException ignored) {
                    // emitter already closed by the client
                }
            }
        });
    }

    /** Status without subscribing: dataset, total events, stream label. */
    public Map<String, Object> status() {
        Map<String, Object> out = new HashMap<>();
        try {
            List<LogEvent> events = currentEvents();
            out.put("enabled", true);
            out.put("dataset", datasetName());
            out.put("total", events.size());
            out.put("source", "demo-replay");
            out.put("label", "Demo replay stream — not real-time");
        } catch (DatasetException e) {
            out.put("enabled", false);
            out.put("dataset", null);
            out.put("total", 0);
            out.put("source", null);
            out.put("label", "No dataset loaded");
        }
        return out;
    }

    private String datasetName() {
        return datasetService.currentDataset().map(d -> d.name()).orElse("");
    }

    private List<LogEvent> currentEvents() {
        return datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"))
                .events();
    }
}