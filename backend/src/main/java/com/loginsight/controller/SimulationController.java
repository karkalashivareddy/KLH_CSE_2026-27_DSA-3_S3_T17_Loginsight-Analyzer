package com.loginsight.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.loginsight.dto.request.SimulationTransitionRequest;
import com.loginsight.dto.response.ScenarioDto;
import com.loginsight.dto.response.SimulationFrameDto;
import com.loginsight.dto.response.SimulationIncidentDto;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.service.LiveSimulationService;
import com.loginsight.simulation.SimulationIncident;

/**
 * Scenario catalogue and deterministic live simulation ({@code /api/scenarios}, {@code /api/simulation}).
 *
 * <p>The stream emits SSE events named {@code start}, {@code frame} and {@code complete}. Every payload
 * carries {@code source: "live-simulation"} and a label stating that the traffic is generated, so these
 * events can never be mistaken for captured telemetry. The separate {@code /api/live} endpoint remains
 * the labelled dataset replay.</p>
 *
 * <p>Determinism contract: a frame is a pure function of {@code (scenario, seed, tick)}. The
 * {@code speed} parameter changes only how fast frames are delivered.</p>
 */
@RestController
@RequestMapping("/api")
public class SimulationController {

    private final LiveSimulationService simulationService;

    public SimulationController(LiveSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /** Every declared scenario, in presentation order. */
    @GetMapping("/scenarios")
    public List<ScenarioDto> scenarios() {
        return simulationService.catalog();
    }

    /** A single scenario by id. */
    @GetMapping("/scenarios/{id}")
    public ScenarioDto scenario(@PathVariable String id) {
        return simulationService.scenario(id);
    }

    /** The default scenario, used when the client has not chosen one. */
    @GetMapping("/scenarios/default")
    public ScenarioDto defaultScenario() {
        return simulationService.scenario(null);
    }

    /** Capabilities and constraints of the simulation, for rendering the Scenario Lab controls. */
    @GetMapping("/simulation/status")
    public Map<String, Object> status() {
        return simulationService.status();
    }

    /**
     * The deterministic stream.
     *
     * @param scenario   scenario id; blank selects the default
     * @param seed       deterministic seed; blank uses the scenario's declared seed
     * @param speed      wall-clock delivery multiplier (0.25..8); never changes event content
     * @param intervalMs base tick pacing before speed is applied
     * @param maxFrames  hard frame cap
     */
    @GetMapping(value = "/simulation/stream", produces = "text/event-stream")
    public SseEmitter stream(@RequestParam(required = false) String scenario,
                             @RequestParam(required = false) Long seed,
                             @RequestParam(required = false) Double speed,
                             @RequestParam(defaultValue = "250") long intervalMs,
                             @RequestParam(defaultValue = "1200") int maxFrames) {
        return simulationService.subscribe(scenario, seed, speed, intervalMs, maxFrames);
    }

    /** Runs a scenario forward without a stream and returns the final frame. */
    @GetMapping("/simulation/sample")
    public SimulationFrameDto sample(@RequestParam(required = false) String scenario,
                                     @RequestParam(required = false) Long seed,
                                     @RequestParam(defaultValue = "1") int frames) {
        return simulationService.sample(scenario, seed, frames);
    }

    /** Incidents recorded for a session, so an investigation can continue after the stream ends. */
    @GetMapping("/simulation/incidents")
    public List<SimulationIncidentDto> incidents(@RequestParam String sessionId) {
        return simulationService.incidents(sessionId);
    }

    /** Operator-driven lifecycle action; transitions only move forward. */
    @PostMapping("/simulation/incidents/{id}/transition")
    @ResponseStatus(HttpStatus.OK)
    public SimulationIncidentDto transition(@PathVariable int id,
                                            @RequestBody SimulationTransitionRequest request) {
        SimulationIncident.Status target = SimulationIncident.Status.parse(request.status());
        return simulationService
                .transition(request.sessionId(), id, target, request.isAdvance())
                .orElseThrow(() -> new InvalidQueryException(
                        "Unknown session or incident; start a stream to obtain a sessionId"));
    }

    /** The lifecycle states accepted by the transition endpoint. */
    @GetMapping("/simulation/lifecycle")
    public List<String> lifecycle() {
        return SimulationFrameDto.lifecycleStates();
    }
}
