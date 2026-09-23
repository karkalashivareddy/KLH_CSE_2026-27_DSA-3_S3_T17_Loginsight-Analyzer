package com.loginsight.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.loginsight.catalog.AlgorithmCatalog;
import com.loginsight.catalog.AlgorithmInfo;
import com.loginsight.dto.RequestFactory;
import com.loginsight.dto.request.RunRequest;
import com.loginsight.dto.response.RunSummaryDto;
import com.loginsight.dto.response.TraceResponseDto;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.run.RunRecord;
import com.loginsight.run.RunStore;

import jakarta.annotation.PreDestroy;

/**
 * Run lifecycle + SSE replay (docs/REBUILD_BASELINE Phase-4). Runs are executed synchronously
 * against the real trace-instrumented algorithms, stored in the bounded {@link RunStore}, and
 * replayed over {@code GET /api/runs/{id}/events} as an SSE stream of the genuinely recorded steps
 * plus a meta and a complete event. Replay is a faithful emission of recorded events; it never
 * synthesises state while streaming.
 */
@Service
public class RunService {

    private static final long SSE_TIMEOUT_MS = 60_000L;

    private final TraceService trace;
    private final RunStore store;
    private final ExecutorService emitterPool = Executors.newFixedThreadPool(2);

    public RunService(TraceService trace, RunStore store) {
        this.trace = trace;
        this.store = store;
    }

    public List<RunSummaryDto> list() {
        List<RunSummaryDto> out = new ArrayList<>();
        for (RunRecord run : store.newestFirst()) {
            out.add(summary(run));
        }
        return out;
    }

    public RunRecord get(String runId) {
        RunRecord run = store.get(runId);
        if (run == null) {
            throw new InvalidQueryException("Unknown run id: " + runId);
        }
        return run;
    }

    public RunRecord create(RunRequest request) {
        AlgorithmInfo info = AlgorithmCatalog.byKey(request.algorithm())
                .orElseThrow(() -> new InvalidQueryException(
                        "Unknown traceable algorithm: " + request.algorithm()));
        if (!info.tracked()) {
            throw new InvalidQueryException(
                    request.algorithm() + " is not trace-instrumented");
        }
        Map<String, Object> input = request.input() == null ? Map.of() : request.input();
        String runId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        TraceResponseDto dto;
        try {
            dto = execute(info.key(), input);
        } catch (Exception e) {
            RunRecord failed = new RunRecord(runId, info.key(), info.name(), info.moduleLabel(),
                    "FAILED", now, Instant.now(), 0, 0L, false, info.timeComplexity(),
                    info.spaceComplexity(), input, null, List.of(), safeMessage(e));
            store.put(failed);
            return failed;
        }

        RunRecord completed = new RunRecord(runId, info.key(), info.name(), info.moduleLabel(),
                "COMPLETED", now, Instant.now(), dto.steps().size(), dto.executionTimeNanos(),
                dto.truncated(), dto.timeComplexity(), dto.spaceComplexity(), input, dto.result(),
                dto.steps(), null);
        store.put(completed);
        return completed;
    }

    public SseEmitter stream(String runId) {
        RunRecord run = get(runId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitterPool.execute(() -> emit(emitter, run));
        return emitter;
    }

    // ------------------------------------------------------------------ internals

    private void emit(SseEmitter emitter, RunRecord run) {
        try {
            emitter.send(SseEmitter.event().name("meta").data(meta(run)));
            if ("COMPLETED".equals(run.status())) {
                for (Map<String, Object> step : run.steps()) {
                    emitter.send(SseEmitter.event().name("step").data(step));
                }
            }
            emitter.send(SseEmitter.event().name("complete").data(complete(run)));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private static Map<String, Object> meta(RunRecord run) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("runId", run.runId());
        out.put("algorithm", run.algorithm());
        out.put("algorithmName", run.algorithmName());
        out.put("category", run.category());
        out.put("status", run.status());
        out.put("stepCount", run.steps().size());
        out.put("truncated", run.truncated());
        out.put("timeComplexity", run.timeComplexity());
        out.put("spaceComplexity", run.spaceComplexity());
        out.put("executionTimeNanos", run.executionTimeNanos());
        out.put("result", run.result());
        out.put("error", run.error());
        return out;
    }

    private static Map<String, Object> complete(RunRecord run) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("runId", run.runId());
        out.put("stepCount", run.steps().size());
        out.put("executionTimeNanos", run.executionTimeNanos());
        out.put("truncated", run.truncated());
        out.put("status", run.status());
        return out;
    }

    private TraceResponseDto execute(String key, Map<String, Object> input) {
        return switch (key) {
            case "naive" -> trace.naive(RequestFactory.search(input));
            case "kmp" -> trace.kmp(RequestFactory.search(input));
            case "z" -> trace.z(RequestFactory.search(input));
            case "rabinkarp" -> trace.rabinKarp(RequestFactory.search(input));
            case "levenshtein" -> trace.levenshtein(RequestFactory.edit(input, true));
            case "matrixchain" -> trace.matrixChain(RequestFactory.matrixChain(input));
            case "fordfulkerson" -> trace.fordFulkerson(RequestFactory.flow(input));
            case "edmondskarp" -> trace.edmondsKarp(RequestFactory.flow(input));
            case "dinic" -> trace.dinic(RequestFactory.flow(input));
            case "vertexcover" -> trace.vertexCover(RequestFactory.vertexCover(input));
            case "quicksort" -> trace.quicksort(RequestFactory.quicksort(input));
            case "millerrabin" -> trace.millerRabin(RequestFactory.millerRabin(input));
            case "reservoir" -> trace.reservoir(RequestFactory.reservoir(input));
            default -> throw new InvalidQueryException(
                    "Unknown traceable algorithm: " + key);
        };
    }

    private static RunSummaryDto summary(RunRecord run) {
        return new RunSummaryDto(run.runId(), run.algorithm(), run.algorithmName(),
                run.category(), run.status(), run.createdAt(), run.completedAt(),
                run.stepCount(), run.executionTimeNanos(), run.truncated(),
                run.timeComplexity(), run.spaceComplexity());
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    @PreDestroy
    void shutdown() {
        emitterPool.shutdown();
    }
}