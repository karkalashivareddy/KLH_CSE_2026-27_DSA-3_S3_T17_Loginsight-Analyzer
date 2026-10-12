package com.loginsight.dto.response;

import java.util.List;

import com.loginsight.simulation.ScenarioDefinition;
import com.loginsight.simulation.ScenarioEventFactory;

/**
 * One scenario offered by the Scenario Lab ({@code GET /api/scenarios}).
 *
 * <p>Includes the declared expectations so the UI can show, up front, what signal and incident the
 * deterministic scenario is designed to produce — a testable claim rather than a surprise. The
 * {@code source} field is always {@code live-simulation}: these events are generated, never ingested
 * from a real deployment.</p>
 */
public record ScenarioDto(
        String id,
        String title,
        String summary,
        long seed,
        int durationSeconds,
        int onsetSeconds,
        int peakSeconds,
        int recoverySeconds,
        List<String> affectedServices,
        List<String> errorSignatures,
        String expectedSignal,
        String expectedIncident,
        String severity,
        String source,
        String label) {

    public static ScenarioDto from(ScenarioDefinition scenario) {
        int millisPerSecond = 1000;
        int tickMillis = (int) ScenarioEventFactory.TICK_MILLIS;
        return new ScenarioDto(
                scenario.id(),
                scenario.title(),
                scenario.summary(),
                scenario.seed(),
                scenario.durationTicks() * tickMillis / millisPerSecond,
                scenario.onsetTick() * tickMillis / millisPerSecond,
                scenario.peakTick() * tickMillis / millisPerSecond,
                scenario.recoveryTick() * tickMillis / millisPerSecond,
                scenario.affectedServices(),
                scenario.errorSignatures(),
                scenario.expectedSignal(),
                scenario.expectedIncident(),
                scenario.severity(),
                "live-simulation",
                "Deterministic simulation — generated traffic, not captured telemetry");
    }
}
