package com.loginsight.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loginsight.dto.response.LogEventDto;
import com.loginsight.dto.response.SimulationFrameDto;
import com.loginsight.model.LogEvent;
import com.loginsight.model.LogLevel;

/**
 * The determinism contract: a frame is a pure function of {@code (scenario, seed, tick)}.
 *
 * <p>If these fail, the demo stops being reproducible and the recorded evidence stops being
 * checkable, so they are asserted strictly rather than loosely.</p>
 */
class SimulationSessionTest {

    private static final int TICKS = 60;

    @Test
    @DisplayName("the same scenario and seed produce an identical progression")
    void sameSeedProducesIdenticalFrames() {
        SimulationFrameDto first = run(ScenarioCatalog.defaultScenario(), 4242L, TICKS);
        SimulationFrameDto second = run(ScenarioCatalog.defaultScenario(), 4242L, TICKS);
        assertThat(progressionOf(second)).isEqualTo(progressionOf(first));
    }

    @Test
    @DisplayName("a different seed produces a different event progression")
    void differentSeedProducesDifferentEvents() {
        SimulationFrameDto first = run(ScenarioCatalog.defaultScenario(), 1L, TICKS);
        SimulationFrameDto second = run(ScenarioCatalog.defaultScenario(), 2L, TICKS);
        assertThat(second.events()).isNotEqualTo(first.events());
    }

    @Test
    @DisplayName("advancing one tick at a time equals advancing in one jump")
    void incrementalAndBulkAdvanceAgree() {
        SimulationSession session = new SimulationSession(ScenarioCatalog.defaultScenario(), 77L, Instant.EPOCH);
        SimulationFrameDto last = null;
        for (int i = 0; i < TICKS; i++) {
            last = session.advance();
        }
        assertThat(progressionOf(last)).isEqualTo(progressionOf(run(ScenarioCatalog.defaultScenario(), 77L, TICKS)));
    }

    /**
     * Everything that is a deterministic function of {@code (scenario, seed, tick)}: the generated
     * events, the aggregates, the signals, the health rollup and the incident list. Measured
     * wall-clock run times are deliberately excluded, since they cannot and should not be
     * reproducible.
     */
    private static Object progressionOf(SimulationFrameDto frame) {
        return List.of(
                frame.events(),
                frame.signals(),
                frame.health(),
                frame.incidents().stream()
                        .map(incident -> List.of(incident.status(), incident.title(), incident.signal(),
                                incident.originService(), incident.blastRadius(), incident.matchedSignatures()))
                        .toList(),
                frame.matchedSignatures(),
                frame.totalEvents(),
                frame.totalErrors(),
                frame.errorRate(),
                frame.p95LatencyMs(),
                frame.intensity(),
                frame.phase(),
                frame.evidence().stream()
                        .map(link -> link.algorithm() + '|' + link.inputSize() + '|' + link.result())
                        .toList());
    }

    @Test
    @DisplayName("every scenario generates events within its declared window")
    void allScenariosGenerateEvents() {
        for (ScenarioDefinition scenario : ScenarioCatalog.all()) {
            assertThat(scenario.isValid()).as("%s is internally consistent", scenario.id()).isTrue();
            SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 4);
            assertThat(frame.events()).as("%s emits events", scenario.id()).isNotEmpty();
            assertThat(frame.totalEvents()).as("%s accumulates events", scenario.id()).isPositive();
            assertThat(frame.source()).isEqualTo("live-simulation");
            assertThat(frame.label()).contains("not captured telemetry");
        }
    }

    @Test
    @DisplayName("generated events are canonical, labelled and well formed")
    void generatedEventsAreWellFormed() {
        SimulationSession session = new SimulationSession(ScenarioCatalog.defaultScenario(), 5L, Instant.EPOCH);
        for (int tick = 0; tick < 30; tick++) {
            session.advance();
        }
        List<LogEvent> events = session.recentEvents(200);
        assertThat(events).isNotEmpty();
        assertThat(events).allSatisfy(event -> {
            assertThat(event.getMessage()).isNotBlank();
            assertThat(event.getService()).isNotBlank();
            assertThat(event.getLevel()).isNotNull();
            assertThat(event.getStatusCode()).isBetween(100, 599);
            assertThat(event.getResponseTime()).isPositive();
            assertThat(event.getTimestamp()).isNotNull();
            assertThat(event.getSource()).isEqualTo("live-simulation");
            assertThat(event.getAttributes()).containsEntry("source", "live-simulation");
            assertThat(event.searchableText()).isNotBlank();
        });
        assertThat(events).extracting(LogEvent::getService)
                .allMatch(SimulationTopology::isKnown);
    }

    @Test
    @DisplayName("per-service counters cover every topology service and stay consistent")
    void serviceCountersAreConsistent() {
        SimulationSession session = new SimulationSession(ScenarioCatalog.defaultScenario(), 6L, Instant.EPOCH);
        for (int tick = 0; tick < 20; tick++) {
            session.advance();
        }
        assertThat(session.serviceCounters()).isNotEmpty().allSatisfy(counter -> {
            assertThat(counter.events()).isPositive();
            assertThat(counter.errorRate()).isBetween(0.0, 100.0);
            assertThat(counter.averageLatencyMs()).isPositive();
        });
    }

    @Test
    @DisplayName("the window stays bounded over a long run")
    void windowIsBounded() {
        SimulationSession session = new SimulationSession(
                ScenarioCatalog.find("traffic-surge").orElseThrow(), 9L, Instant.EPOCH);
        for (int tick = 0; tick < 200; tick++) {
            session.advance();
        }
        SimulationFrameDto frame = session.advance();
        assertThat(frame.windowSize()).isLessThanOrEqualTo(frame.windowCapacity());
    }

    @Test
    @DisplayName("a scenario ends after its declared window instead of looping forever")
    void scenarioCompletes() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        SimulationSession session = new SimulationSession(scenario, 1L, Instant.EPOCH);
        for (int tick = 0; tick < session.totalTicks(); tick++) {
            assertThat(session.completed()).isFalse();
            session.advance();
        }
        assertThat(session.completed()).isTrue();
    }

    @Test
    @DisplayName("a run keeps emitting a healthy cooldown tail so recovery stays observable")
    void cooldownTailIsEmittedAfterTheScenarioWindow() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        SimulationSession session = new SimulationSession(scenario, 1L, Instant.EPOCH);
        for (int tick = 0; tick < scenario.durationTicks(); tick++) {
            session.advance();
        }

        // Intensity is already back to zero here, but the run is not over: the rolling window still
        // holds the tail of the failure, so stopping now would strand every open incident.
        assertThat(session.completed()).isFalse();
        assertThat(session.totalTicks()).isEqualTo(scenario.durationTicks() + SimulationSession.COOLDOWN_TICKS);

        SimulationFrameDto lastFrame = null;
        for (int tick = scenario.durationTicks(); tick < session.totalTicks(); tick++) {
            lastFrame = session.advance();
        }

        assertThat(session.completed()).isTrue();
        assertThat(lastFrame).isNotNull();
        assertThat(lastFrame.phase()).isEqualTo("healthy");
        assertThat(lastFrame.intensity()).isLessThanOrEqualTo(SimulationSession.HEALTHY_INTENSITY);
    }

    @Test
    @DisplayName("an inconsistent scenario is rejected at construction")
    void inconsistentScenarioIsRejected() {
        ScenarioDefinition broken = new ScenarioDefinition(
                "broken", "Broken", "invalid phase ordering", 1L, 10, 8, 4, 2,
                "payments", List.of("payments"), 1.0, 0.1, 100, List.of("x"), "signal", "incident", "MINOR");
        assertThatThrownBy(() -> new SimulationSession(broken, 1L, Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("the blast radius of a dependency failure propagates upstream")
    void blastRadiusPropagatesUpstream() {
        var radius = SimulationTopology.blastRadius("postgres");
        assertThat(radius).contains("postgres", "orders", "payments", "inventory", "api-gateway");
        assertThat(radius).as("notifications does not depend on the database").doesNotContain("notifications");
    }

    @Test
    @DisplayName("a blast radius never contains an unknown service")
    void blastRadiusIsBoundedToTheTopology() {
        for (SimulationTopology.ServiceNode node : SimulationTopology.SERVICES) {
            assertThat(SimulationTopology.blastRadius(node.id()))
                    .allMatch(SimulationTopology::isKnown);
        }
    }

    @Test
    @DisplayName("intensity follows the declared onset, peak and recovery shape")
    void intensityFollowsDeclaredShape() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        assertThat(scenario.intensityAt(0)).isZero();
        assertThat(scenario.intensityAt(scenario.onsetTick())).isZero();
        assertThat(scenario.intensityAt(scenario.peakTick())).isEqualTo(1.0);
        assertThat(scenario.intensityAt(scenario.recoveryTick() - 1)).isEqualTo(1.0);
        assertThat(scenario.intensityAt(scenario.durationTicks())).isZero();
        assertThat(scenario.intensityAt(scenario.durationTicks() + 50)).isZero();
    }

    @Test
    @DisplayName("error levels are classified as errors and info as not")
    void errorClassification() {
        assertThat(SimulationDetector.isError(LogLevel.ERROR)).isTrue();
        assertThat(SimulationDetector.isError(LogLevel.FATAL)).isTrue();
        assertThat(SimulationDetector.isError(LogLevel.WARN)).isFalse();
        assertThat(SimulationDetector.isError(LogLevel.INFO)).isFalse();
    }

    @Test
    @DisplayName("every frame echoes the session id so operator actions can address it")
    void framesCarryTheSessionId() {
        ScenarioDefinition scenario = ScenarioCatalog.find("checkout-5xx-cascade").orElseThrow();
        SimulationSession session = new SimulationSession(scenario, scenario.seed(), Instant.EPOCH, "sim-42");
        for (int tick = 0; tick < 5; tick++) {
            assertThat(session.advance().sessionId()).isEqualTo("sim-42");
        }
    }

    @Test
    @DisplayName("a blank session id falls back to a local marker rather than emitting null")
    void blankSessionIdIsNormalised() {
        ScenarioDefinition scenario = ScenarioCatalog.find("checkout-5xx-cascade").orElseThrow();
        SimulationSession session = new SimulationSession(scenario, scenario.seed(), Instant.EPOCH, "  ");
        assertThat(session.advance().sessionId()).isEqualTo("session-local");
    }

    @Test
    @DisplayName("generated events carry a unique identity instead of a shared placeholder")
    void generatedEventsHaveUniqueIds() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        SimulationSession session = new SimulationSession(scenario, 9L, Instant.EPOCH, "sim-ids", 3L);
        java.util.Set<Long> seen = new java.util.LinkedHashSet<>();
        int total = 0;
        for (int tick = 0; tick < 40; tick++) {
            for (LogEventDto event : session.advance().events()) {
                assertThat(event.id()).isNotNegative();
                assertThat(seen.add(event.id())).as("duplicate generated event id %s", event.id()).isTrue();
                total++;
            }
        }
        assertThat(total).isPositive();
    }

    @Test
    @DisplayName("generated identities never collide with an ingested dataset identity")
    void generatedIdsClearTheDatasetRange() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        SimulationSession session = new SimulationSession(scenario, 9L, Instant.EPOCH, "sim-range", 0L);
        for (LogEventDto event : session.advance().events()) {
            assertThat(event.id()).isGreaterThanOrEqualTo(ScenarioEventFactory.FIRST_GENERATED_ID);
        }
    }

    @Test
    @DisplayName("two concurrent sessions never share a generated event identity")
    void concurrentSessionsHaveDisjointIds() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        SimulationSession first = new SimulationSession(scenario, 11L, Instant.EPOCH, "sim-a", 1L);
        SimulationSession second = new SimulationSession(scenario, 11L, Instant.EPOCH, "sim-b", 2L);
        java.util.Set<Long> firstIds = idsOf(first);
        java.util.Set<Long> secondIds = idsOf(second);
        assertThat(firstIds).isNotEmpty();
        assertThat(secondIds).doesNotContainAnyElementsOf(firstIds);
    }

    @Test
    @DisplayName("the same session ordinal reproduces the same identities for the same content")
    void identitiesAreReproducibleForTheSameOrdinal() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        assertThat(idsOf(new SimulationSession(scenario, 13L, Instant.EPOCH, "sim-x", 5L)))
                .isEqualTo(idsOf(new SimulationSession(scenario, 13L, Instant.EPOCH, "sim-y", 5L)));
    }

    private static java.util.Set<Long> idsOf(SimulationSession session) {
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        for (int tick = 0; tick < 10; tick++) {
            for (LogEventDto event : session.advance().events()) {
                ids.add(event.id());
            }
        }
        return ids;
    }

    private static SimulationFrameDto run(ScenarioDefinition scenario, long seed, int ticks) {
        SimulationSession session = new SimulationSession(scenario, seed, Instant.EPOCH);
        SimulationFrameDto frame = null;
        for (int tick = 0; tick < ticks; tick++) {
            frame = session.advance();
        }
        return frame;
    }
}
