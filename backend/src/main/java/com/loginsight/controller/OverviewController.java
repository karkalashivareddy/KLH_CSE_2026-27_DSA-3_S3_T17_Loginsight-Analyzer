package com.loginsight.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.response.OverviewDto;
import com.loginsight.service.OverviewService;

/**
 * Dashboard endpoint (docs/API.md §2). Every number is derived live from the loaded dataset; when
 * nothing is loaded the request maps to 404 ({@code DatasetException}) and the UI shows its
 * first-run state with the demo-dataset prompt.
 */
@RestController
@RequestMapping("/api")
public class OverviewController {

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping("/overview")
    public OverviewDto overview(@RequestParam(value = "range", defaultValue = "1h") String range) {
        return overviewService.snapshot(range);
    }
}