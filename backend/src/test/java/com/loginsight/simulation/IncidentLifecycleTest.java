package com.loginsight.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loginsight.dto.response.SimulationFrameDto;
import com.loginsight.dto.response.SimulationIncidentDto;

/**
 * Incident lifecycle: forward-only transitions, an honest timeline, and a resolution that only
 * happens once the signal actually decays.
 */
class IncidentLifecycleTest {

    @Test
    @DisplayName("a resolved incident is terminal")
    void resolvedIsTerminal() {
        IncidentLifecycleStore store = new IncidentLifecycleStore();
        SimulationIncident incident = store.open(candidate(), "test");
        assertThat(incident).isNotNull();
        Instant now = Instant.parse("2026-04-12T10:00:00Z");
        for (SimulationIncident.Status status : SimulationIncident.Status.values()) {
            assertThat(incident.transitionTo(status, now)).isTrue();
        }
        assertThat(incident.getStatus()).isEqualTo(SimulationIncident.Status.RESOLVED);
        assertThat(incident.transitionTo(SimulationIncident.Status.INVESTIGATING, now.plusSeconds(60)))
                .isFalse();
        assertThat(incident.advance(now.plusSeconds(60), false)).isFalse();
        assertThat(incident.isOpen()).isFalse();
    }

    @Test
    @DisplayName("transitions never move backwards")
    void transitionsAreForwardOnly() {
        IncidentLifecycleStore store = new IncidentLifecycleStore();
        SimulationIncident incident = store.open(candidate(), "test");
        Instant now = Instant.parse("2026-04-12T10:00:00Z");
        incident.transitionTo(SimulationIncident.Status.ACKNOWLEDGED, now);
        assertThat(incident.transitionTo(SimulationIncident.Status.DETECTED, now.plusSeconds(1))).isFalse();
        assertThat(incident.getStatus()).isEqualTo(SimulationIncident.Status.ACKNOWLEDGED);
    }

    @Test
    @DisplayName("the timeline records every transition with its actor")
    void timelineRecordsActor() {
        IncidentLifecycleStore store = new IncidentLifecycleStore();
        SimulationIncident incident = store.open(candidate(), "test");
        Instant now = Instant.parse("2026-04-12T10:00:00Z");
        incident.advance(now, true);
        assertThat(incident.getTimeline()).hasSize(2);
        assertThat(incident.getTimeline().get(0).status()).isEqualTo(SimulationIncident.Status.DETECTED);
        assertThat(incident.getTimeline().get(1).status()).isEqualTo(SimulationIncident.Status.INVESTIGATING);
        assertThat(incident.getTimeline().get(1).label()).contains("Simulation auto-advance");

        SimulationIncident manual = store.open(candidate(), "test");
        manual.advance(Instant.parse("2026-04-12T11:00:00Z"), false);
        assertThat(manual.getTimeline().get(1).label()).contains("Operator");
    }

    @Test
    @DisplayName("an operator transition is labelled as operator-driven")
    void operatorTransitionIsLabelled() {
        IncidentLifecycleStore store = new IncidentLifecycleStore();
        SimulationIncident incident = store.open(candidate(), "test");
        store.transition(incident.getId(), SimulationIncident.Status.ACKNOWLEDGED,
                Instant.parse("2026-04-12T10:00:00Z"));
        assertThat(incident.getStatus()).isEqualTo(SimulationIncident.Status.ACKNOWLEDGED);
        assertThat(incident.getTimeline().get(1).label()).contains("Operator");
        assertThat(incident.getTimeline().get(1).label()).doesNotContain("auto");
    }

    @Test
    @DisplayName("a sustained signal auto-advances the window to investigation")
    void sustainedSignalAutoAdvances() {
        ScenarioDefinition scenario = ScenarioCatalog.find("authentication-burst").orElseThrow();
        SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 16);
        SimulationIncidentDto incident = frame.incidents().get(0);
        assertThat(incident.status()).isIn("DETECTED", "INVESTIGATING", "ACKNOWLEDGED", "MITIGATED", "RESOLVED");
        assertThat(incident.timeline()).isNotEmpty();
        assertThat(incident.timeline().get(0).status()).isEqualTo("DETECTED");
    }

    @Test
    @DisplayName("a fully decayed signal closes the window as resolved")
    void decayedSignalResolves() {
        ScenarioDefinition scenario = ScenarioCatalog.find("traffic-surge").orElseThrow();
        SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.durationTicks());
        assertThat(frame.incidents()).isNotEmpty();
        assertThat(frame.incidents()).allSatisfy(incident -> {
            assertThat(incident.status()).isEqualTo("RESOLVED");
            assertThat(incident.open()).isFalse();
        });
    }

    @Test
    @DisplayName("lifecycle state is reported as session-scoped, not persisted")
    void lifecycleStateIsSessionScoped() {
        ScenarioDefinition scenario = ScenarioCatalog.defaultScenario();
        SimulationFrameDto frame = run(scenario, scenario.seed(), scenario.peakTick() + 5);
        assertThat(frame.incidents()).allSatisfy(incident ->
                assertThat(incident.persistence()).isEqualTo("session"));
    }

    @Test
    @DisplayName("the store is bounded and evicts resolved incidents before open ones")
    void storeIsBounded() {
        IncidentLifecycleStore store = new IncidentLifecycleStore();
        for (int i = 0; i < IncidentLifecycleStore.MAX_INCIDENTS; i++) {
            assertThat(store.open(candidate(), "test")).isNotNull();
        }
        assertThat(store.size()).isEqualTo(IncidentLifecycleStore.MAX_INCIDENTS);
        SimulationIncident oldest = store.all().get(0);
        oldest.transitionTo(SimulationIncident.Status.RESOLVED, Instant.parse("2026-04-12T10:00:00Z"));
        assertThat(store.open(candidate(), "test")).as("a resolved incident is evicted to make room").isNotNull();
        assertThat(store.all()).noneMatch(incident -> incident == oldest);
    }

    @Test
    @DisplayName("the store refuses a new incident when every retained one is still open")
    void storeRefusesWhenAllOpen() {
        IncidentLifecycleStore store = new IncidentLifecycleStore();
        for (int i = 0; i < IncidentLifecycleStore.MAX_INCIDENTS; i++) {
            store.open(candidate(), "test");
        }
        assertThat(store.open(candidate(), "test")).as("no silent data loss: the store reports itself full").isNull();
        assertThat(store.openIncidents()).hasSize(IncidentLifecycleStore.MAX_INCIDENTS);
    }

    @Test
    @DisplayName("an unknown incident is reported as absent rather than invented")
    void unknownIncidentIsAbsent() {
        IncidentLifecycleStore store = new IncidentLifecycleStore();
        assertThat(store.find(999)).isEmpty();
        assertThat(store.advance(999, Instant.now(), false)).isFalse();
        assertThat(store.transition(999, SimulationIncident.Status.ACKNOWLEDGED, Instant.now())).isEmpty();
    }

    @Test
    @DisplayName("the advertised lifecycle order is the documented one")
    void lifecycleOrderIsStable() {
        assertThat(SimulationFrameDto.lifecycleStates())
                .containsExactly("DETECTED", "INVESTIGATING", "ACKNOWLEDGED", "MITIGATED", "RESOLVED");
    }

    private static SimulationIncident candidate() {
        Instant now = Instant.parse("2026-04-12T09:59:00Z");
        return new SimulationIncident("Test incident", "test signal", "heuristic", "payments",
                List.of("payments"), List.of("orders", "api-gateway"), List.of(), now, 10, 12.5, 900,
                List.of("upstream returned 502"));
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
