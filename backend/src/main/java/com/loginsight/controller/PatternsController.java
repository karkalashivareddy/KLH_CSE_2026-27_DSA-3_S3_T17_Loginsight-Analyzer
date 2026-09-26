package com.loginsight.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.response.LogEventDto;
import com.loginsight.dto.response.PatternDto;
import com.loginsight.exception.DatasetException;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;
import com.loginsight.pattern.PatternExtractor;
import com.loginsight.query.QueryValidator;
import com.loginsight.service.DatasetService;

/**
 * Message-pattern endpoints (docs/API.md §5). Patterns are computed with the heuristic token
 * normaliser ({@code PatternExtractor}); the UI is required to label them "heuristic", not ML.
 */
@RestController
@RequestMapping("/api/patterns")
public class PatternsController {

    private static final int MAX_LIMIT = 200;
    private static final int MAX_TEMPLATE_LENGTH = 2_000;

    private final DatasetService datasetService;
    private final PatternExtractor patternExtractor = new PatternExtractor();

    public PatternsController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    /** Top patterns across the whole dataset, or narrow to a single level. */
    @GetMapping
    public List<PatternDto> patterns(@RequestParam(value = "level", defaultValue = "all") String level,
                                     @RequestParam(defaultValue = "50") int limit) {
        QueryValidator.requireBounds(1, limit, MAX_LIMIT, "limit");
        if (level != null && level.length() > 32) {
            throw new InvalidQueryException("level must be at most 32 characters");
        }
        List<LogEvent> events = currentEvents();
        String wanted = (level == null || level.isBlank())
                ? "ALL"
                : level.trim().toUpperCase(Locale.ROOT);
        if (!"ALL".equals(wanted)) {
            LogLevel filter;
            try {
                filter = LogLevel.valueOf(wanted);
            } catch (IllegalArgumentException e) {
                throw new InvalidQueryException("level must be one of all, trace, debug, info, "
                        + "warn, error, fatal");
            }
            List<LogEvent> filtered = new ArrayList<>();
            for (LogEvent event : events) {
                if (event.getLevel() == filter) {
                    filtered.add(event);
                }
            }
            events = filtered;
        }
        return patternExtractor.extract(events, limit);
    }

    /** Sample events belonging to a pattern template (docs/API.md §5.2). */
    @GetMapping("/examples")
    public List<LogEventDto> examples(@RequestParam("template") String template,
                                      @RequestParam(defaultValue = "50") int limit) {
        QueryValidator.requireBounds(1, limit, MAX_LIMIT, "limit");
        if (template == null || template.isBlank()) {
            throw new InvalidQueryException("template must not be blank");
        }
        if (template.length() > MAX_TEMPLATE_LENGTH) {
            throw new InvalidQueryException("template must be at most " + MAX_TEMPLATE_LENGTH
                    + " characters");
        }
        List<LogEventDto> out = new ArrayList<>();
        for (LogEvent event : currentEvents()) {
            String message = event.getMessage();
            if (message != null && template.equals(PatternExtractor.normalizeMessage(message))) {
                out.add(LogEventDto.from(event));
                if (out.size() >= limit) {
                    break;
                }
            }
        }
        return out;
    }

    private List<LogEvent> currentEvents() {
        return datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"))
                .events();
    }
}