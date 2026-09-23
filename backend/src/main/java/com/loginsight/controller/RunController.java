package com.loginsight.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.loginsight.dto.request.RunRequest;
import com.loginsight.dto.response.RunSummaryDto;
import com.loginsight.run.RunRecord;
import com.loginsight.service.RunService;

/**
 * Run history and SSE trace replay (docs/REBUILD_BASELINE Phase-4). Runs are created synchronously
 * against a real trace-instrumented algorithm and their recorded steps are replayed as an SSE
 * stream of {@code meta / step / complete} events.
 */
@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunService runs;

    public RunController(RunService runs) {
        this.runs = runs;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RunRequest request) {
        return ResponseEntity.ok(runs.create(request));
    }

    @GetMapping
    public List<RunSummaryDto> list() {
        return runs.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RunRecord> get(@PathVariable String id) {
        return ResponseEntity.ok(runs.get(id));
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String id) {
        return runs.stream(id);
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<?> result(@PathVariable String id) {
        RunRecord run = runs.get(id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("runId", run.runId());
        body.put("status", run.status());
        body.put("result", run.result());
        body.put("error", run.error());
        body.put("stepCount", run.stepCount());
        body.put("executionTimeNanos", run.executionTimeNanos());
        body.put("truncated", run.truncated());
        return ResponseEntity.ok(body);
    }
}