package com.loginsight.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.response.IncidentDto;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.service.IncidentsService;

/**
 * Incident investigation endpoints (docs/API.md §6). Detection is the documented heuristic
 * (5-minute elevated-error windows against the dataset baseline); every incident exposes its
 * supporting logs so an operator can verify the claim.
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentsController {

    private final IncidentsService incidentsService;

    public IncidentsController(IncidentsService incidentsService) {
        this.incidentsService = incidentsService;
    }

    @GetMapping
    public List<IncidentDto> list(@RequestParam(defaultValue = "20") int limit) {
        return incidentsService.detect(limit);
    }

    @GetMapping("/count")
    public long count() {
        return incidentsService.activeCount();
    }

    @GetMapping("/{id}")
    public IncidentDto.IncidentDetail detail(@PathVariable long id,
                                             @RequestParam(defaultValue = "100") int logs) {
        return incidentsService.detail(id, logs);
    }

    /** Supporting evidence for an incident: the error events inside its time window. */
    @GetMapping("/{id}/logs")
    public List<LogEventDto> logs(@PathVariable long id,
                                  @RequestParam(defaultValue = "100") int limit,
                                  @RequestParam(defaultValue = "0") int offset) {
        IncidentDto incident = incidentsService.find(id);
        return incidentsService.eventsInWindow(incident.start(), incident.end(), limit, offset);
    }
}