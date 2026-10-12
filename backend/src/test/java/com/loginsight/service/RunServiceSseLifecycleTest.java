package com.loginsight.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.loginsight.exception.InvalidQueryException;
import com.loginsight.run.RunRecord;
import com.loginsight.run.RunStore;

/**
 * Regression guard for the {@code GET /api/runs/{id}/events} emitter lifecycle.
 *
 * <p>{@link RunService#stream(String)} used to register no {@code onCompletion}/{@code onTimeout}/
 * {@code onError} callbacks at all, unlike {@code LiveSimulationService} and
 * {@code LiveStreamService}. A client that disconnected mid-replay therefore left the emit loop
 * writing into a dead connection for the rest of the run and never released the emitter.</p>
 */
class RunServiceSseLifecycleTest {

    private static final int STEP_COUNT = 5;

    private final RunStore store = new RunStore();
    private final RunService service = new RunService(new TraceService(), store);

    private SseEmitter streamRun() {
        store.put(record());
        return service.stream("run-1");
    }

    private static RunRecord record() {
        List<Map<String, Object>> steps = new ArrayList<>();
        for (int i = 0; i < STEP_COUNT; i++) {
            steps.add(Map.of("index", i, "operation", "STEP"));
        }
        Instant now = Instant.now();
        return new RunRecord("run-1", "kmp", "Knuth-Morris-Pratt", "String Matching",
                "COMPLETED", now, now, STEP_COUNT, 1_000L, false,
                "O(n + m)", "O(m)", Map.of(), Map.of("matchCount", 1), steps, null);
    }

    @AfterEach
    void stopPool() {
        service.shutdown();
    }

    // --- reflective accessors for the Spring emitter internals used as the assertion target ---

    private static Object field(Object target, Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot read " + owner.getSimpleName() + "." + name, e);
        }
    }

    /** The {@code Runnables} / {@code Consumers} a service registered on the emitter's slot. */
    @SuppressWarnings("unchecked")
    private static List<Object> lifecycleDelegates(SseEmitter emitter, String slot) {
        Object holder = field(emitter, ResponseBodyEmitter.class, slot);
        return holder == null ? null : (List<Object>) field(holder, holder.getClass(), "delegates");
    }

    private static boolean emitterCompleted(SseEmitter emitter) {
        return Boolean.TRUE.equals(field(emitter, ResponseBodyEmitter.class, "complete"));
    }

    private static void awaitCompleted(SseEmitter emitter) {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!emitterCompleted(emitter) && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
    }

    // --- lifecycle callbacks are wired ---

    @Test
    void streamRegistersCompletionTimeoutAndErrorCallbacks() {
        SseEmitter emitter = streamRun();
        assertFalse(lifecycleDelegates(emitter, "completionCallback") == null
                        || lifecycleDelegates(emitter, "completionCallback").isEmpty(),
                "onCompletion must be registered so a finished or disconnected client is released");
        assertFalse(lifecycleDelegates(emitter, "timeoutCallback") == null
                        || lifecycleDelegates(emitter, "timeoutCallback").isEmpty(),
                "onTimeout must be registered");
        assertFalse(lifecycleDelegates(emitter, "errorCallback") == null
                        || lifecycleDelegates(emitter, "errorCallback").isEmpty(),
                "onError must be registered");
    }

    @Test
    void aCompletionCallbackTerminatesAndReleasesTheEmitter() {
        SseEmitter emitter = streamRun();
        List<Object> delegates = lifecycleDelegates(emitter, "completionCallback");
        assertNotNull(delegates, "onCompletion must be registered");
        assertFalse(delegates.isEmpty(), "onCompletion must have at least one delegate");
        ((Runnable) delegates.get(0)).run();
        assertTrue(emitterCompleted(emitter),
                "the client-side completion must terminate the emit loop and release the emitter");
    }

    @Test
    void aTimeoutCallbackTerminatesAndReleasesTheEmitter() {
        SseEmitter emitter = streamRun();
        List<Object> delegates = lifecycleDelegates(emitter, "timeoutCallback");
        assertNotNull(delegates, "onTimeout must be registered");
        assertFalse(delegates.isEmpty(), "onTimeout must have at least one delegate");
        ((Runnable) delegates.get(0)).run();
        assertTrue(emitterCompleted(emitter));
    }

    @Test
    @SuppressWarnings("unchecked")
    void anErrorCallbackTerminatesAndReleasesTheEmitter() {
        SseEmitter emitter = streamRun();
        List<Object> delegates = lifecycleDelegates(emitter, "errorCallback");
        assertNotNull(delegates, "onError must be registered");
        assertFalse(delegates.isEmpty(), "onError must have at least one delegate");
        Consumer<Throwable> callback = (Consumer<Throwable>) delegates.get(0);
        assertDoesNotThrow(() -> callback.accept(new IllegalStateException("client went away")));
        assertTrue(emitterCompleted(emitter));
    }

    // --- a normal full replay is unchanged ---

    @Test
    void aFullReplayStillCompletesTheEmitter() {
        SseEmitter emitter = streamRun();
        assertEquals(Long.valueOf(60_000L), emitter.getTimeout());
        awaitCompleted(emitter);
        assertTrue(emitterCompleted(emitter), "an uninterrupted replay completes normally");
    }

    @Test
    void anUnknownRunIdIsStillARejectedRequest() {
        assertThrows(InvalidQueryException.class, () -> service.stream("missing-run"));
    }

    @Test
    void shutdownIsIdempotentAndDoesNotHang() {
        assertDoesNotThrow(() -> {
            service.shutdown();
            service.shutdown();
        });
    }
}