package com.loginsight.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.response.DatasetStatsDto;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.service.LogService;

/**
 * Log explorer endpoints (docs/12 §2): the seeded log view, the bootstrap sample event and the
 * dataset statistics dashboard.
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public List<LogEventDto> list(@RequestParam(defaultValue = "100") int limit,
                                   @RequestParam(defaultValue = "0") int offset) {
        return logService.list(limit, offset);
    }

    @GetMapping("/first")
    public LogEventDto first() {
        return logService.first();
    }

    @GetMapping("/stats")
    public DatasetStatsDto stats() {
        return logService.stats();
    }
}