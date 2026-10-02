package com.loginsight.simulation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.loginsight.dto.response.LogEventDto;
import com.loginsight.dto.response.SimulationFrameDto;
import com.loginsight.dto.response.SimulationIncidentDto;
import com.loginsight.model.LogEvent;

/**
 * One deterministic simulation run.
 *
 * <p>The session owns all mutable run state: the event factory, the bounded window, the sliding-window
 * burst detector, the analysis pass and the incident lifecycle. Its contract is that
 * {@code advance()} is a pure function of {@code (scenario, seed, tick)}. Nothing inside depends on
 * wall-clock time — the caller decides how fast to call, and the same tick always yields the same
 * events, the same signals and therefore the same incident. That is what makes the demo repeatable
 * and the evidence checkable.</p>
 *
 * <p>Instances are not thread-safe; {@link LiveSimulationService} confines each session to one
 * stream task.</p>
 */
public final class SimulationSession {

    /** Retained events per session; bounded so a long run cannot grow without limit. */
    public static final int WINDOW_CAPACITY = 4_000;
    /** Events emitted per frame, capped so a volume surge cannot flood one SSE message. */
    public static final int MAX_EVENTS_PER_FRAME = 120;
    /** Healthy baseline events per second across the fleet, before any scenario multiplier. */
    public static final double HEALTHY_RATE = 48.0;
    /** Consecutive signalling windows before the incident is auto-advanced to investigation. */
    public static final int SUSTAINED_TICKS_TO_INVESTIGATE = 8;
    /** Intensity below which the failure is treated as decaying. */
    public static final double MITIGATION_INTENSITY = 0.45;
    /** Intensity below which the failure is treated as fully present. */
    public static final double ONSET_INTENSITY = 0.35;
    /** Intensity at or below which the signal counts as back to baseline. */
    public static final double HEALTHY_INTENSITY = 0.02;
    /** Per-tick error share at or below which a window is considered recovered. */
    public static final double HEALTHY_TICK_ERROR_RATE = 0.01;
    /** Ceiling on simultaneously open incidents, so a run cannot flood the list. */
    public static final int MAX_OPEN_INCIDENTS = 3;
    /**
     * Healthy ticks emitted after the scenario window so recovery is observable.
     *
     * <p>Intensity already returns to zero at {@code durationTicks}, but the rolling window still
     * holds the tail of the failure at that point, so the measured per-tick error rate is still above
     * {@link #HEALTHY_TICK_ERROR_RATE} and an open incident can never satisfy the resolution rule.
     * Without this cooldown the run would end at the exact moment the system became healthy, and the
     * red -&gt; amber -&gt; green sequence would never be emitted. The cooldown is fully deterministic
     * and carries no incident signal of its own.
     */
    public static final int COOLDOWN_TICKS = 80;

    /** Last tick emitted by a run, including the healthy cooldown tail. */
    public int totalTicks() {
        return scenario.durationTicks() + COOLDOWN_TICKS;
    }

    private final ScenarioDefinition scenario;
    private final long seed;
    private final Instant origin;
    private final ScenarioEventFactory factory;
    private final RollingWindow window = new RollingWindow(WINDOW_CAPACITY);
    private final BurstDetector burst = new BurstDetector();
    private final SimulationDetector detector = new SimulationDetector();
    private final IncidentLifecycleStore store = new IncidentLifecycleStore();

    private final String sessionId;

    private int tick;
    private long sequence;
    private int sustainedWindows;
    private SimulationIncident openIncident;

    public SimulationSession(ScenarioDefinition scenario, long seed, Instant origin) {
        this(scenario, seed, origin, "session-local");
    }

    public SimulationSession(ScenarioDefinition scenario, long seed, Instant origin, String sessionId) {
        this(scenario, seed, origin, sessionId, 0L);
    }

    /**
     * @param sessionId       identifier echoed on every frame so a client can address operator actions
     *     without holding a stream open.
     * @param sessionOrdinal  process-wide session counter, used to give every emitted event a unique
     *     deterministic id that cannot collide with another session or with an ingested dataset id.
     */
    public SimulationSession(ScenarioDefinition scenario, long seed, Instant origin, String sessionId,
                             long sessionOrdinal) {
        if (scenario == null) {
            throw new IllegalArgumentException("scenario is required");
        }
        if (!scenario.isValid()) {
            throw new IllegalArgumentException("scenario is internally inconsistent: " + scenario.id());
        }
        this.scenario = scenario;
        this.seed = seed;
        this.origin = origin;
        this.sessionId = sessionId == null || sessionId.isBlank() ? "session-local" : sessionId;
        this.factory = new ScenarioEventFactory(scenario, DeterministicRandom.forScenario(scenario.id(), seed),
                origin, sessionOrdinal);
    }

    public synchronized ScenarioDefinition scenario() {
        return scenario;
    }

    public synchronized long seed() {
        return seed;
    }

    public synchronized int tick() {
        return tick;
    }

    public IncidentLifecycleStore incidents() {
        return store;
    }

    /** The most recent retained events, newest first; used by tests and diagnostics. */
    public List<LogEvent> recentEvents(int limit) {
        return window.recent(limit).stream().map(RollingWindow.SimulatedEvent::event).toList();
    }

    /** Per-service counters for the current window; used by tests and diagnostics. */
    public List<RollingWindow.ServiceCounter> serviceCounters() {
        return window.services();
    }

    /** Whether the scenario and its healthy cooldown tail have both been emitted. */
    public synchronized boolean completed() {
        return tick >= totalTicks();
    }

    /**
     * Advances one tick and returns the full analytical frame.
     *
     * <p>Deterministic: the same session advanced to the same tick always produces this frame.</p>
     */
    public synchronized SimulationFrameDto advance() {
        if (completed()) {
            return frame(List.of(), new SimulationDetector.Analysis(
                    List.of(), List.of(), List.of(), null, List.of(), 0, 0, 0, 0, java.util.Set.of()));
        }
        List<LogEvent> generated = factory.generate(tick, HEALTHY_RATE);
        int thisTick = Math.min(generated.size(), MAX_EVENTS_PER_FRAME);
        List<LogEventDto> payload = new java.util.ArrayList<>(thisTick);
        for (int i = 0; i < thisTick; i++) {
            LogEvent event = generated.get(i);
            window.add(tick, event, factory.emitted() - generated.size() + i + 1);
            burst.observe(tick, SimulationDetector.isError(event.getLevel()));
            payload.add(LogEventDto.from(event));
        }
        SimulationDetector.Analysis analysis = detector.analyze(window, burst, scenario, tick, thisTick);
        reconcile(analysis);
        tick++;
        return frame(payload, analysis);
    }

    /**
     * Keeps the open incident's lifecycle in step with the signal, and opens a new window when the
     * previous one closed.
     *
     * <p>Rules, in order of precedence:</p>
     * <ul>
     *   <li>an incident only opens once the failure has actually manifested — a primary signal plus a
     *       confirmed signature, after the onset phase, so a single early error never opens a window;</li>
     *   <li>it advances to investigation only after the signal has been sustained across consecutive
     *       windows;</li>
     *   <li>it advances to mitigated only once the scenario has passed its peak and the intensity is
     *       decaying, so a rising failure is never mislabelled as mitigated;</li>
     *   <li>it resolves when the intensity is back to the healthy threshold.</li>
     * </ul>
     */
    private void reconcile(SimulationDetector.Analysis analysis) {
        Instant now = origin.plusMillis((long) tick * ScenarioEventFactory.TICK_MILLIS);
        double intensity = scenario.intensityAt(tick);
        boolean primarySignal = analysis.signals().stream()
                .anyMatch(signal -> !signal.kind().equals("signature"));
        boolean signatureSeen = !analysis.matchedSignatures().isEmpty();
        boolean signalling = primarySignal && signatureSeen && intensity >= ONSET_INTENSITY;
        boolean recovered = intensity <= HEALTHY_INTENSITY;

        sustainedWindows = signalling ? sustainedWindows + 1 : 0;

        if (openIncident != null) {
            openIncident.updateMetrics(
                    (int) window.generated(),
                    analysis.errorRate(),
                    analysis.p95LatencyMs(),
                    analysis.matchedSignatures(),
                    analysis.evidence(),
                    now);
            if (openIncident.getStatus() == SimulationIncident.Status.DETECTED
                    && sustainedWindows >= SUSTAINED_TICKS_TO_INVESTIGATE) {
                openIncident.advance(now, true);
            }
            if (recovered && window.tickErrorRate() <= HEALTHY_TICK_ERROR_RATE) {
                openIncident.transitionTo(SimulationIncident.Status.RESOLVED, now, true);
                openIncident = null;
                sustainedWindows = 0;
                return;
            }
            if (tick >= scenario.recoveryTick()
                    && intensity < MITIGATION_INTENSITY
                    && openIncident.getStatus().ordinal() < SimulationIncident.Status.MITIGATED.ordinal()) {
                openIncident.transitionTo(SimulationIncident.Status.MITIGATED, now, true);
            }
            return;
        }

        if (signalling && tick >= 3 && store.openIncidents().size() < MAX_OPEN_INCIDENTS) {
            SimulationIncident candidate = new SimulationIncident(
                    scenario.expectedIncident(),
                    scenario.expectedSignal(),
                    "Heuristic detector: sliding-window burst, latency and volume thresholds, with the "
                            + "signature confirmed by Aho-Corasick and re-counted by KMP. Heuristic signal, "
                            + "not a proven root cause.",
                    scenario.originService(),
                    scenario.affectedServices(),
                    List.copyOf(analysis.blastRadius()),
                    analysis.evidence(),
                    now,
                    (int) window.generated(),
                    analysis.errorRate(),
                    analysis.p95LatencyMs(),
                    analysis.matchedSignatures());
            openIncident = store.open(candidate, scenario.id());
            if (openIncident != null) {
                sustainedWindows = 0;
            }
        }
    }

    private SimulationFrameDto frame(List<LogEventDto> events, SimulationDetector.Analysis analysis) {
        String phase = phaseFor(scenario.intensityAt(tick));
        List<SimulationIncidentDto> incidents = store.all().stream()
                .map(incident -> SimulationIncidentDto.from(incident, scenario.severity()))
                .toList();
        return new SimulationFrameDto(
                sessionId,
                sequence++,
                "live-simulation",
                "Deterministic simulation — generated traffic, not captured telemetry",
                scenario.id(),
                scenario.title(),
                seed,
                tick,
                (int) ScenarioEventFactory.TICK_MILLIS,
                events.size(),
                Math.round(scenario.intensityAt(tick) * 1000) / 1000.0,
                phase,
                window.generated(),
                window.errors(),
                Math.round(window.errorRate() * 100) / 100.0,
                Math.round(analysis.eventsPerSecond() * 10) / 10.0,
                window.averageLatencyMs(),
                analysis.p95LatencyMs(),
                analysis.baselineP95LatencyMs(),
                window.size(),
                window.capacity(),
                analysis.matchedSignatures(),
                events,
                analysis.signals().stream().map(SimulationIncidentDto.SignalDto::from).toList(),
                analysis.evidence().stream()
                        .map(link -> new SimulationIncidentDto.EvidenceDto(
                                link.algorithm(),
                                link.purpose(),
                                link.inputSize(),
                                link.inputUnit(),
                                link.result(),
                                link.runtimeNanos(),
                                Math.round(link.runtimeNanos() / 100.0) / 10.0,
                                link.complexity(),
                                link.references()))
                        .toList(),
                analysis.health().stream()
                        .map(health -> SimulationFrameDto.ServiceHealthDto.from(
                                health, analysis.blastRadius().contains(health.service())))
                        .toList(),
                incidents,
                SimulationFrameDto.TopologyDto.declared());
    }

    /** Named phase of the scenario, so the UI can label the timeline honestly. */
    public static String phaseFor(double intensity) {
        if (intensity <= 0.0) {
            return "healthy";
        }
        if (intensity < 0.35) {
            return "onset";
        }
        if (intensity < 0.85) {
            return "degrading";
        }
        return "peak";
    }

    /** Estimated wall-clock duration of a full run at a given tick pace. */
    public static Duration estimatedRunDuration(ScenarioDefinition scenario) {
        return Duration.ofMillis((long) (scenario.durationTicks() + COOLDOWN_TICKS) * ScenarioEventFactory.TICK_MILLIS);
    }
}
