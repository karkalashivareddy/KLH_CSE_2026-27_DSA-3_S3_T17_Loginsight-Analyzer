package com.loginsight.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.loginsight.dto.response.ScenarioDto;
import com.loginsight.dto.response.SimulationFrameDto;
import com.loginsight.dto.response.SimulationIncidentDto;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.query.QueryValidator;
import com.loginsight.simulation.ScenarioCatalog;
import com.loginsight.simulation.ScenarioDefinition;
import com.loginsight.simulation.ScenarioEventFactory;
import com.loginsight.simulation.SimulationIncident;
import com.loginsight.simulation.SimulationSession;

import jakarta.annotation.PreDestroy;

/**
 * The deterministic live-simulation stream ({@code GET /api/simulation/stream}).
 *
 * <p>This is a separate service from {@link LiveStreamService} on purpose. The replay stream is
 * bounded dataset traffic; this one is <em>generated</em> traffic for a declared failure scenario, and
 * every frame it emits says so. Keeping them apart means the UI can never accidentally present
 * synthetic events as captured telemetry.</p>
 *
 * <p>Each subscription owns one {@link SimulationSession}, and the session's content is a pure
 * function of {@code (scenario, seed, tick)}. The {@code speed} parameter only changes how fast ticks
 * are <em>delivered</em> — it never changes which events are produced, so slowing the stream down
 * cannot change the outcome of a run.</p>
 *
 * <p>Bounded by design: a fixed thread pool with a bounded queue rejects excess subscriptions instead
 * of accumulating them, and a run ends after the scenario's declared window rather than looping
 * forever.</p>
 */
@Service
public class LiveSimulationService {

    private static final Logger LOG = LoggerFactory.getLogger(LiveSimulationService.class);

    public static final double MIN_SPEED = 0.25;
    public static final double MAX_SPEED = 8.0;
    public static final long MIN_INTERVAL_MS = 40L;
    public static final long MAX_INTERVAL_MS = 5_000L;
    public static final int SCHEDULER_THREADS = 4;
    public static final int MAX_PENDING_STREAMS = 16;
    public static final int MAX_ACTIVE_STREAMS = 24;
    public static final long MAX_STREAM_DURATION_MS = 15 * 60 * 1000L;
    /** Live sessions retained for operator actions after their stream ended. */
    public static final int RETAINED_SESSIONS = 16;
    /**
     * Identity ordinal used by the non-streaming {@link #sample} preview.
     *
     * <p>Pinned so preview frames stay byte-identical across runs. Streamed sessions use their own
     * ordinal so their event identities stay unique; a preview frame and a streamed frame are never
     * rendered in the same list.</p>
     */
    public static final long PREVIEW_SESSION_ORDINAL = 0L;

    private final Map<String, SimulationSession> sessions = new ConcurrentHashMap<>();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final AtomicInteger threadNumber = new AtomicInteger();
    private final AtomicLong sessionCounter = new AtomicLong();
    private final ThreadPoolExecutor scheduler = new ThreadPoolExecutor(
            SCHEDULER_THREADS,
            SCHEDULER_THREADS,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING_STREAMS),
            runnable -> {
                Thread thread = new Thread(runnable,
                        "loginsight-sim-" + threadNumber.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy());

    public static double requireSpeed(double speed) {
        if (Double.isNaN(speed) || speed < MIN_SPEED || speed > MAX_SPEED) {
            throw new InvalidQueryException("speed must be between " + MIN_SPEED + " and " + MAX_SPEED
                    + ", got " + speed);
        }
        return speed;
    }

    public static long requireIntervalMillis(long intervalMillis) {
        QueryValidator.requireBounds((int) MIN_INTERVAL_MS, (int) intervalMillis, (int) MAX_INTERVAL_MS, "intervalMs");
        return intervalMillis;
    }

    /** Resolves a scenario id, falling back to the default when none is supplied. */
    public static ScenarioDefinition requireScenario(String scenarioId) {
        if (scenarioId == null || scenarioId.isBlank()) {
            return ScenarioCatalog.defaultScenario();
        }
        return ScenarioCatalog.find(scenarioId).orElseThrow(
                () -> new InvalidQueryException("Unknown scenario '" + scenarioId + "'"));
    }

    /** Scenario catalogue for the Scenario Lab. */
    public List<ScenarioDto> catalog() {
        return ScenarioCatalog.all().stream().map(ScenarioDto::from).toList();
    }

    /** A single scenario, or the first scenario when the id is blank. */
    public ScenarioDto scenario(String scenarioId) {
        return ScenarioDto.from(scenarioId == null || scenarioId.isBlank()
                ? ScenarioCatalog.defaultScenario()
                : requireScenario(scenarioId));
    }

    /**
     * Starts a simulation stream.
     *
     * @param scenarioId    scenario key; blank selects the default
     * @param seed          deterministic seed; blank uses the scenario's declared default seed
     * @param speed         wall-clock delivery multiplier, 0.25..8; does not affect event content
     * @param intervalMs    base tick pacing before {@code speed} is applied
     * @param maxFrames     hard frame cap so a client cannot pin a thread indefinitely
     */
    public SseEmitter subscribe(String scenarioId, Long seed, Double speed, long intervalMs, int maxFrames) {
        if (stopping.get()) {
            throw new InvalidQueryException("Live simulation is shutting down");
        }
        if (sessions.size() >= MAX_ACTIVE_STREAMS + RETAINED_SESSIONS) {
            throw new InvalidQueryException("Too many concurrent simulations; try again shortly");
        }
        ScenarioDefinition scenario = requireScenario(scenarioId);
        double resolvedSpeed = speed == null ? 1.0 : requireSpeed(speed);
        long interval = requireIntervalMillis(intervalMs);
        QueryValidator.requireBounds(1, maxFrames, 20_000, "maxFrames");
        long resolvedSeed = seed == null ? scenario.seed() : seed;

        long ordinal = sessionCounter.incrementAndGet();
        String sessionId = "sim-" + ordinal;
        SimulationSession session = new SimulationSession(scenario, resolvedSeed, Instant.now(), sessionId, ordinal);
        sessions.put(sessionId, session);
        pruneSessions(sessionId);

        SseEmitter emitter = new SseEmitter(MAX_STREAM_DURATION_MS);
        SimulationTask task = new SimulationTask(emitter, sessionId, session, resolvedSpeed, interval, maxFrames);
        emitter.onCompletion(task::cancel);
        emitter.onTimeout(task::cancel);
        emitter.onError(error -> task.cancel());
        try {
            task.future = scheduler.submit(task);
        } catch (RejectedExecutionException e) {
            task.fail(e);
        }
        return emitter;
    }

    /** Looks up a live or retained session. */
    public Optional<SimulationSession> session(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * Applies an operator-driven lifecycle transition to a session incident.
     *
     * @return the updated incident, or empty when the session or incident is unknown
     */
    public Optional<SimulationIncidentDto> transition(String sessionId,
                                                      int incidentId,
                                                      SimulationIncident.Status target,
                                                      boolean advanceOneStep) {
        Optional<SimulationSession> session = session(sessionId);
        if (session.isEmpty() || target == null) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        SimulationIncident incident;
        if (advanceOneStep) {
            incident = session.get().incidents().find(incidentId)
                    .filter(candidate -> candidate.advance(now, false))
                    .orElse(null);
        } else {
            incident = session.get().incidents().transition(incidentId, target, now).orElse(null);
        }
        if (incident == null) {
            return Optional.empty();
        }
        return Optional.of(SimulationIncidentDto.from(incident, session.get().scenario().severity()));
    }

    /** Incident list for a session, used by the investigation view after the stream ends. */
    public List<SimulationIncidentDto> incidents(String sessionId) {
        return session(sessionId)
                .map(value -> value.incidents().all().stream()
                        .map(incident -> SimulationIncidentDto.from(incident, value.scenario().severity()))
                        .toList())
                .orElseGet(List::of);
    }

    /** Non-streaming status: what the Scenario Lab needs to render before a run starts. */
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", !stopping.get());
        out.put("source", "live-simulation");
        out.put("label", "Deterministic simulation — generated traffic, not captured telemetry");
        out.put("scenarios", ScenarioCatalog.all().size());
        out.put("activeSessions", sessions.size());
        out.put("maxConcurrentStreams", MAX_ACTIVE_STREAMS);
        out.put("speed", Map.of("min", MIN_SPEED, "max", MAX_SPEED, "default", 1.0));
        out.put("intervalMs", Map.of("min", MIN_INTERVAL_MS, "max", MAX_INTERVAL_MS, "default", 250));
        out.put("maxFrames", Map.of("min", 1, "max", 20_000, "default", 1_200));
        out.put("determinism", "Events are a pure function of (scenario, seed, tick). "
                + "Speed changes delivery pace only, never event content.");
        out.put("persistence", "Incident lifecycle is session-scoped and in-memory; it is not persisted.");
        return out;
    }

    /**
     * Runs a scenario forward without holding a stream open, returning the final frame.
     *
     * <p>Used by tests and by the Scenario Lab preview. The origin timestamp is fixed to the epoch and
     * the identity range is pinned to the preview range, so two runs of the same
     * {@code (scenario, seed, frames)} triple produce byte-identical frames — event identities
     * included. That is why this path deliberately does not consume a stream ordinal.</p>
     */
    public SimulationFrameDto sample(String scenarioId, Long seed, int frames) {
        ScenarioDefinition scenario = requireScenario(scenarioId);
        long resolvedSeed = seed == null ? scenario.seed() : seed;
        QueryValidator.requireBounds(1, frames, 1_000, "frames");
        long ordinal = sessionCounter.incrementAndGet();
        String sessionId = "sim-" + ordinal;
        SimulationSession session = new SimulationSession(scenario, resolvedSeed, Instant.EPOCH, sessionId,
                PREVIEW_SESSION_ORDINAL);
        SimulationFrameDto frame = null;
        for (int i = 0; i < frames; i++) {
            frame = session.advance();
        }
        // Retain the preview so operator actions (acknowledge, advance) address a real session.
        sessions.put(sessionId, session);
        pruneSessions(sessionId);
        return frame;
    }

    /** Keeps the retained-session map bounded, never evicting the session just created. */
    private void pruneSessions(String keep) {
        if (sessions.size() <= RETAINED_SESSIONS) {
            return;
        }
        List<String> keys = new ArrayList<>(sessions.keySet());
        for (int i = 0; i < keys.size() - RETAINED_SESSIONS; i++) {
            if (!keys.get(i).equals(keep)) {
                sessions.remove(keys.get(i));
            }
        }
    }

    private final class SimulationTask implements Runnable {
        private final SseEmitter emitter;
        private final String sessionId;
        private final SimulationSession session;
        private final double speed;
        private final long intervalMs;
        private final int maxFrames;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile Future<?> future;

        private SimulationTask(SseEmitter emitter, String sessionId, SimulationSession session,
                               double speed, long intervalMs, int maxFrames) {
            this.emitter = emitter;
            this.sessionId = sessionId;
            this.session = session;
            this.speed = speed;
            this.intervalMs = intervalMs;
            this.maxFrames = maxFrames;
        }

        @Override
        public void run() {
            try {
                if (closed.get()) {
                    return;
                }
                ScenarioDefinition scenario = session.scenario();
                emitter.send(SseEmitter.event().name("start").data(Map.ofEntries(
                        Map.entry("sessionId", sessionId),
                        Map.entry("source", "live-simulation"),
                        Map.entry("scenarioId", scenario.id()),
                        Map.entry("scenarioTitle", scenario.title()),
                        Map.entry("seed", session.seed()),
                        Map.entry("tickMillis", ScenarioEventFactory.TICK_MILLIS),
                        Map.entry("speed", speed),
                        Map.entry("paceMs", paceMillis()),
                        Map.entry("expectedSignal", scenario.expectedSignal()),
                        Map.entry("expectedIncident", scenario.expectedIncident()),
                        Map.entry("label", "Deterministic simulation — generated traffic, not captured telemetry"))));
                int sent = 0;
                while (!closed.get() && sent < maxFrames && !session.completed()) {
                    SimulationFrameDto frame = session.advance();
                    emitter.send(SseEmitter.event().name("frame").data(frame));
                    sent++;
                    if (!closed.get() && sent < maxFrames && !session.completed()) {
                        Thread.sleep(paceMillis());
                    }
                }
                if (!closed.get()) {
                    emitter.send(SseEmitter.event().name("complete").data(Map.of(
                            "sessionId", sessionId,
                            "frames", sent,
                            "ticks", session.tick(),
                            "reason", session.completed() ? "scenario-window-completed" : "frame-cap-reached")));
                    completeNormally();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancel();
            } catch (Exception e) {
                if (!closed.get()) {
                    LOG.debug("Simulation stream closed: {}", e.getMessage());
                    fail(e);
                }
            } finally {
                if (!closed.get()) {
                    completeNormally();
                }
            }
        }

        private long paceMillis() {
            return Math.max(1L, Math.round(intervalMs / speed));
        }

        private void cancel() {
            if (closed.compareAndSet(false, true)) {
                Future<?> current = future;
                if (current != null) {
                    current.cancel(true);
                }
                scheduler.purge();
                completeEmitter();
            }
        }

        private void completeNormally() {
            if (closed.compareAndSet(false, true)) {
                completeEmitter();
            }
        }

        private void fail(Throwable error) {
            if (closed.compareAndSet(false, true)) {
                try {
                    emitter.completeWithError(error);
                } catch (RuntimeException ignored) {
                }
            }
        }

        private void completeEmitter() {
            try {
                emitter.complete();
            } catch (RuntimeException ignored) {
            }
        }
    }

    @PreDestroy
    void shutdown() {
        stopping.set(true);
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sessions.clear();
    }
}
