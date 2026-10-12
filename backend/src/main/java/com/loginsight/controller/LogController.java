package com.loginsight.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.request.LogSearchRequest;
import com.loginsight.dto.response.DatasetStatsDto;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.dto.response.LogSearchResponse;
import com.loginsight.exception.DatasetException;
import com.loginsight.search.LogSearchService;
import com.loginsight.service.LogService;

/**
 * Log explorer endpoints (docs/12 §2): the seeded log view, the bootstrap sample event and the
 * dataset statistics dashboard, plus the single-event reader and the explorer (docs/API.md §3.4).
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;
    private final LogSearchService logSearchService;

    public LogController(LogService logService, LogSearchService logSearchService) {
        this.logService = logService;
        this.logSearchService = logSearchService;
    }

    @GetMapping
    public List<LogEventDto> list(@RequestParam(defaultValue = "100") int limit,
                                   @RequestParam(defaultValue = "0") int offset) {
        return logService.list(limit, offset);
    }

    /** Dataset-backed explorer with structured filters, paging and free-text search. */
    @GetMapping("/explore")
    public LogSearchResponse explore(@RequestParam(value = "q", defaultValue = "") String q,
                                     @RequestParam(required = false) Instant from,
                                     @RequestParam(required = false) Instant to,
                                     @RequestParam(value = "page", defaultValue = "1") int page,
                                     @RequestParam(value = "size", defaultValue = "25") int size,
                                     @RequestParam(value = "sort", defaultValue = "timestamp:desc")
                                     String sort) {
        return logSearchService.search(new LogSearchRequest(q, from, to, page, size, sort));
    }

    @GetMapping("/first")
    public LogEventDto first() {
        return logService.first();
    }

    @GetMapping("/stats")
    public DatasetStatsDto stats() {
        return logService.stats();
    }

    /** Single event by its dataset-assigned id; 404 when the dataset is unloaded or id unknown. */
    @GetMapping("/{id}")
    public LogEventDto byId(@PathVariable long id) {
        return logService.byId(id);
    }
}