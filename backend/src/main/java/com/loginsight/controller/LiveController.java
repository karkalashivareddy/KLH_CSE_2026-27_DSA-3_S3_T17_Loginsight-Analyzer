package com.loginsight.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.loginsight.service.LiveStreamService;

/**
 * Live endpoint (docs/API.md §9). Frames are SSE events named {@code start}, {@code batch} and
 * {@code replay-complete}; the {@code source: "demo-replay"} label in every payload tells clients
 * the traffic is a bounded replay of the loaded dataset, not a real-time feed.
 */
@RestController
@RequestMapping("/api/live")
public class LiveController {

    private final LiveStreamService liveStreamService;

    public LiveController(LiveStreamService liveStreamService) {
        this.liveStreamService = liveStreamService;
    }

    @GetMapping(produces = "text/event-stream")
    public SseEmitter stream(@RequestParam(defaultValue = "50") int batchSize,
                             @RequestParam(defaultValue = "700") long intervalMs) {
        return liveStreamService.subscribe(
                LiveStreamService.requireBatchSize(batchSize),
                LiveStreamService.requireIntervalMillis(intervalMs));
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return liveStreamService.status();
    }
}