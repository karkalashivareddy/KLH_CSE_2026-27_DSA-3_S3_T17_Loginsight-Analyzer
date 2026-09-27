package com.loginsight.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loginsight.dto.response.SimulationFrameDto;
import com.loginsight.dto.response.SimulationIncidentDto;

/**
 * Detection and evidence: the heuristic detector has to actually fire on the failure window, and every
 * evidence link has to be a real, reproducible measurement.
 */
class SimulationDetectionTest {

    @Test
    @DisplayName("a checkout cascade raises an incident during the failure window")
    void cascadeRaisesIncident() {
        ScenarioDefinition scenario = ScenarioCatalog.find("checkout-5xx-cascade").orElseThrow();
        SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 20);
        assertThat(frame.incidents()).as("an incident is opened for the declared failure").isNotEmpty();
        SimulationIncidentDto incident = frame.incidents().get(0);
        assertThat(incident.scenarioId()).isEqualTo(scenario.id());
        assertThat(incident.title()).isEqualTo(scenario.expectedIncident());
        assertThat(incident.signal()).isEqualTo(scenario.expectedSignal());
        assertThat(incident.open()).as("still open at peak").isTrue();
        assertThat(incident.status()).isIn("DETECTED", "INVESTIGATING", "ACKNOWLEDGED", "MITIGATED");
        assertThat(incident.originService()).isEqualTo("payments");
        assertThat(incident.blastRadius()).contains("payments", "orders", "api-gateway");
        assertThat(incident.affectedServices()).contains("payments", "orders", "api-gateway");
        assertThat(incident.method()).contains("Heuristic");
    }

    @Test
    @DisplayName("a rising failure is never labelled mitigated before the peak")
    void risingFailureIsNotMitigated() {
        ScenarioDefinition scenario = ScenarioCatalog.find("checkout-5xx-cascade").orElseThrow();
        for (int tick = scenario.onsetTick() + 4; tick < scenario.recoveryTick(); tick += 3) {
            SimulationFrameDto frame = run(scenario, scenario.seed(), tick);
            assertThat(frame.incidents())
                    .as("no mitigation before recovery at tick %s", tick)
                    .allSatisfy(incident ->
                            assertThat(incident.status()).isIn("DETECTED", "INVESTIGATING", "ACKNOWLEDGED"));
        }
    }

    @Test
    @DisplayName("the event rate reflects the current tick, not a cumulative total")
    void eventRateIsPerTick() {
        ScenarioDefinition scenario = ScenarioCatalog.find("traffic-surge").orElseThrow();
        SimulationFrameDto early = run(scenario, scenario.seed(), 4);
        SimulationFrameDto later = run(scenario, scenario.seed(), 40);
        assertThat(early.eventsPerSecond()).isGreaterThan(0.0);
        assertThat(later.eventsPerSecond())
                .as("a cumulative total would grow roughly tenfold over 40 ticks")
                .isLessThan(early.eventsPerSecond() * 4.0);
    }

    @Test
    @DisplayName("incident evidence is produced by real algorithm invocations")
    void evidenceIsReal() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 10);
        assertThat(frame.evidence()).isNotEmpty();
        assertThat(frame.evidence()).anySatisfy(link -> {
            assertThat(link.algorithm()).contains("Aho-Corasick");
            assertThat(link.inputSize()).isPositive();
            assertThat(link.result()).isNotBlank();
            assertThat(link.complexity()).isNotBlank();
        });
        assertThat(frame.evidence()).anySatisfy(link -> {
            assertThat(link.algorithm()).isEqualTo("Sliding window (1s buckets)");
            assertThat(link.purpose()).contains("baseline");
        });
    }

    @Test
    @DisplayName("the Aho-Corasick scan is measured on every frame, not only the first")
    void ahoScanIsMeasuredEveryFrame() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        for (int tick : new int[] {scenario.peakTick() + 5, scenario.peakTick() + 25, scenario.peakTick() + 45}) {
            SimulationFrameDto frame = run(scenario, scenario.seed(), tick);
            assertThat(frame.evidence()).as("Aho-Corasick evidence at tick %s", tick)
                    .anySatisfy(link -> assertThat(link.algorithm()).contains("Aho-Corasick"));
        }
    }

    @Test
    @DisplayName("evidence run times are measured and non-negative")
    void evidenceRunTimesAreMeasured() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 10);
        assertThat(frame.evidence()).allSatisfy(link -> {
            assertThat(link.runtimeNanos()).isNotNegative();
            assertThat(link.runtimeMicros()).isNotNegative();
        });
    }

    @Test
    @DisplayName("signals carry the measurement that produced them")
    void signalsAreMeasured() {
        ScenarioDefinition scenario = ScenarioCatalog.find("database-latency-spike").orElseThrow();
        SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 12);
        assertThat(frame.signals()).isNotEmpty();
        assertThat(frame.signals()).allSatisfy(signal -> {
            assertThat(signal.id()).isNotBlank();
            assertThat(signal.kind()).isNotBlank();
            assertThat(signal.detail()).isNotBlank();
            assertThat(signal.severity()).isNotBlank();
        });
        assertThat(frame.signals()).anySatisfy(signal ->
                assertThat(signal.detail()).containsAnyOf("baseline", "p95", "errors/s"));
    }

    @Test
    @DisplayName("the healthy baseline phase produces no incident")
    void healthyPhaseIsQuiet() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        SimulationFrameDto frame = run(scenario, scenario.seed(), 4);
        assertThat(frame.incidents()).isEmpty();
        assertThat(frame.matchedSignatures()).isEmpty();
    }

    @Test
    @DisplayName("error rate and latency rise into the failure window and recover after it")
    void metricsTrackTheFailureWindow() {
        ScenarioDefinition scenario = ScenarioCatalog.find("payment-timeout").orElseThrow();
        SimulationFrameDto healthy = run(scenario, scenario.seed(), 6);
        SimulationFrameDto peak = run(scenario, scenario.seed(), scenario.peakTick() + 8);
        SimulationFrameDto recovered = run(scenario, scenario.seed(), scenario.durationTicks() + 2);
        assertThat(peak.errorRate()).isGreaterThan(healthy.errorRate());
        assertThat(peak.p95LatencyMs()).isGreaterThan(healthy.p95LatencyMs());
        assertThat(recovered.incidents()).allSatisfy(incident -> {
            assertThat(incident.open()).isFalse();
            assertThat(incident.status()).isEqualTo("RESOLVED");
        });
    }

    @Test
    @DisplayName("the topology payload is the declared graph, labelled as declared")
    void topologyIsDeclaredAndLabelled() {
        SimulationFrameDto frame = run(ScenarioCatalog.defaultScenario(), 1L, 2);
        SimulationFrameDto.TopologyDto topology = frame.topology();
        assertThat(topology.kind()).isEqualTo("declared");
        assertThat(topology.label()).contains("declared");
        assertThat(topology.nodes()).hasSize(SimulationTopology.SERVICES.size());
        assertThat(topology.edges()).isNotEmpty();
        assertThat(topology.nodes().stream().map(SimulationFrameDto.TopologyDto.Node::id))
                .allMatch(SimulationTopology::isKnown);
    }

    @Test
    @DisplayName("service health is reported for every topology service and rolls up to critical")
    void healthCoversTheTopology() {
        ScenarioDefinition scenario = ScenarioCatalog.find("cache-failure").orElseThrow();
        SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 10);
        assertThat(frame.health()).hasSize(SimulationTopology.SERVICES.size());
        assertThat(frame.health()).allSatisfy(health -> {
            assertThat(health.state()).isIn("healthy", "degraded", "critical");
            assertThat(health.errorRate()).isBetween(0.0, 100.0);
            assertThat(health.load()).isBetween(0.0, 1.0);
        });
        assertThat(frame.health()).anySatisfy(health ->
                assertThat(health.state()).isIn("degraded", "critical"));
    }

    @Test
    @DisplayName("the blast radius is flagged on the affected services in the health payload")
    void blastRadiusIsFlagged() {
        ScenarioDefinition scenario = ScenarioCatalog.find("cache-failure").orElseThrow();
        SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 10);
        assertThat(frame.health()).filteredOn(SimulationFrameDto.ServiceHealthDto::inBlastRadius)
                .extracting(SimulationFrameDto.ServiceHealthDto::service)
                .contains("redis");
    }

    @Test
    @DisplayName("every declared scenario produces its own expected signal")
    void everyScenarioProducesItsSignal() {
        for (ScenarioDefinition scenario : ScenarioCatalog.all()) {
            SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 15);
            assertThat(frame.signals()).as("%s raises a signal", scenario.id()).isNotEmpty();
            assertThat(frame.incidents()).as("%s opens an incident", scenario.id()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("the deterministic seed is echoed in every frame")
    void seedIsEchoed() {
        SimulationFrameDto frame = run(ScenarioCatalog.defaultScenario(), 987654321L, 5);
        assertThat(frame.seed()).isEqualTo(987654321L);
        assertThat(frame.scenarioId()).isEqualTo(ScenarioCatalog.defaultScenario().id());
    }

    @Test
    @DisplayName("the frame payload is capped so a volume surge cannot flood one message")
    void framePayloadIsCapped() {
        ScenarioDefinition scenario = ScenarioCatalog.find("traffic-surge").orElseThrow();
        SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 5);
        assertThat(frame.events()).hasSizeLessThanOrEqualTo(SimulationSession.MAX_EVENTS_PER_FRAME);
    }

    @Test
    @DisplayName("the phase label names the current scenario stage")
    void phaseLabelTracksIntensity() {
        assertThat(SimulationSession.phaseFor(0.0)).isEqualTo("healthy");
        assertThat(SimulationSession.phaseFor(0.2)).isEqualTo("onset");
        assertThat(SimulationSession.phaseFor(0.6)).isEqualTo("degrading");
        assertThat(SimulationSession.phaseFor(1.0)).isEqualTo("peak");
    }

    private static SimulationFrameDto run(ScenarioDefinition scenario, long seed, int ticks) {
        SimulationSession session = new SimulationSession(scenario, seed, Instant.EPOCH);
        SimulationFrameDto frame = null;
        for (int tick = 0; tick < ticks; tick++) {
            frame = session.advance();
        }
        return frame;
    }

    private static List<SimulationIncidentDto> incidentsOf(SimulationFrameDto frame) {
        return frame.incidents();
    }
}
