package com.loginsight.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.loginsight.dto.response.LiveBatchDto;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.exception.DatasetException;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.LogEvent;
import com.loginsight.query.QueryValidator;

import jakarta.annotation.PreDestroy;

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

    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 200;
    public static final long MIN_INTERVAL_MS = 100L;
    public static final long MAX_INTERVAL_MS = 60_000L;
    public static final int SCHEDULER_THREADS = 2;
    public static final int MAX_PENDING_STREAMS = 32;
    private static final long MAX_STREAM_DURATION_MS = 6 * 60 * 60 * 1000L;

    private final DatasetService datasetService;
    private final Set<ReplayTask> activeStreams = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final AtomicInteger threadNumber = new AtomicInteger();
    private final ThreadPoolExecutor scheduler = new ThreadPoolExecutor(
            SCHEDULER_THREADS,
            SCHEDULER_THREADS,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING_STREAMS),
            runnable -> {
                Thread thread = new Thread(runnable,
                        "loginsight-live-" + threadNumber.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy());

    public LiveStreamService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    public static int requireBatchSize(int batchSize) {
        QueryValidator.requireBounds(MIN_BATCH_SIZE, batchSize, MAX_BATCH_SIZE, "batchSize");
        return batchSize;
    }

    public static long requireIntervalMillis(long intervalMillis) {
        if (intervalMillis < MIN_INTERVAL_MS || intervalMillis > MAX_INTERVAL_MS) {
            throw new InvalidQueryException("intervalMs must be between " + MIN_INTERVAL_MS
                    + " and " + MAX_INTERVAL_MS + " milliseconds");
        }
        return intervalMillis;
    }

    public SseEmitter subscribe(int batchSize, long intervalMillis) {
        int size = requireBatchSize(batchSize);
        long interval = requireIntervalMillis(intervalMillis);
        if (stopping.get()) {
            throw new InvalidQueryException("Live replay is shutting down");
        }
        Dataset dataset = datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"));
        SseEmitter emitter = new SseEmitter(MAX_STREAM_DURATION_MS);
        ReplayTask task = new ReplayTask(emitter, chronologicalSnapshot(dataset.events()),
                dataset.name(), size, interval);
        activeStreams.add(task);
        emitter.onCompletion(task::cancel);
        emitter.onTimeout(task::cancel);
        emitter.onError(error -> task.cancel());
        try {
            task.future = scheduler.submit(task);
        } catch (RejectedExecutionException e) {
            task.fail(e);
        }
        return emitter;
    }

    static List<LogEvent> chronologicalSnapshot(List<LogEvent> events) {
        List<LogEvent> snapshot = new ArrayList<>(events);
        snapshot.sort(Comparator.comparing(LogEvent::getTimestamp)
                .thenComparingLong(LogEvent::getId));
        return List.copyOf(snapshot);
    }

    private final class ReplayTask implements Runnable {
        private final SseEmitter emitter;
        private final List<LogEvent> events;
        private final String datasetName;
        private final int batchSize;
        private final long interval;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile Future<?> future;

        private ReplayTask(SseEmitter emitter, List<LogEvent> events, String datasetName,
                           int batchSize, long interval) {
            this.emitter = emitter;
            this.events = events;
            this.datasetName = datasetName;
            this.batchSize = batchSize;
            this.interval = interval;
        }

        @Override
        public void run() {
            try {
                if (closed.get()) {
                    return;
                }
                long sequence = 0;
                long emitted = 0;
                int total = events.size();
                int cursor = 0;
                emitter.send(SseEmitter.event().name("start")
                        .data(Map.of("source", "demo-replay", "dataset", datasetName,
                                "total", total, "batchSize", batchSize,
                                "paceMs", interval, "ordering", "timestamp,id",
                                "label", "Demo replay stream — events are replayed from the "
                                        + "loaded dataset, not captured in real time.")));
                while (cursor < total && !closed.get()) {
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
                            .data(new LiveBatchDto(sequence, "demo-replay", datasetName,
                                    emitted, total, dtos)));
                    if (cursor < total && !closed.get()) {
                        Thread.sleep(interval);
                    }
                }
                if (!closed.get()) {
                    emitter.send(SseEmitter.event().name("replay-complete")
                            .data(Map.of("emitted", emitted, "total", total)));
                    completeNormally();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancel();
            } catch (Exception e) {
                if (!closed.get()) {
                    LOG.debug("Live stream closed: {}", e.getMessage());
                    fail(e);
                }
            } finally {
                if (!closed.get()) {
                    completeNormally();
                }
                activeStreams.remove(this);
            }
        }

        private void cancel() {
            if (closed.compareAndSet(false, true)) {
                Future<?> current = future;
                if (current != null) {
                    current.cancel(true);
                }
                scheduler.purge();
                completeEmitter();
                activeStreams.remove(this);
            }
        }

        private void completeNormally() {
            if (closed.compareAndSet(false, true)) {
                completeEmitter();
                activeStreams.remove(this);
            }
        }

        private void fail(Throwable error) {
            if (closed.compareAndSet(false, true)) {
                try {
                    emitter.completeWithError(error);
                } catch (RuntimeException ignored) {
                }
                activeStreams.remove(this);
            }
        }

        private void completeEmitter() {
            try {
                emitter.complete();
            } catch (RuntimeException ignored) {
            }
        }
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

    @PreDestroy
    void shutdown() {
        stopping.set(true);
        for (ReplayTask task : activeStreams) {
            task.cancel();
        }
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
