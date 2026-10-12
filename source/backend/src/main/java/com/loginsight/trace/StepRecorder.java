package com.loginsight.trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collector for {@link AlgorithmStep}s while a traced algorithm runs.
 *
 * <p>The recorder lives on the traced execution path only — the untraced algorithm methods are
 * never instrumented, so ordinary performance and correctness are untouched. Steps are bounded
 * ({@value #MAX_STEPS}) so an adversarial or huge demonstration input cannot flood the API
 * payload; when the bound is reached trailing steps are dropped and flagged via
 * {@link #isTruncated()}.</p>
 */
public final class StepRecorder {

    /** Hard ceiling on the number of recorded steps in one trace. */
    public static final int MAX_STEPS = 400;

    private final List<AlgorithmStep> steps = new ArrayList<>();
    private boolean truncated;

    public void record(String operation, String description, Map<String, Object> state,
                       List<Integer> highlighted, Map<String, Object> metrics) {
        if (steps.size() >= MAX_STEPS) {
            truncated = true;
            return;
        }
        steps.add(new AlgorithmStep(steps.size() + 1, operation, description,
                state == null ? Map.of() : state,
                highlighted == null ? List.of() : highlighted,
                metrics == null ? Map.of() : metrics));
    }

    /** Convenience overload for steps without highlighted indices or metrics. */
    public void record(String operation, String description, Map<String, Object> state) {
        record(operation, description, state, List.of(), Map.of());
    }

    public boolean isTruncated() {
        return truncated;
    }

    public List<AlgorithmStep> collect() {
        return new ArrayList<>(steps);
    }

    /** Immutable snapshot perspective of an array (converted to a boxed list). */
    public static <T> List<T> snapshotOf(T[] values, int length) {
        return length <= 0 ? List.of() : Arrays.asList(Arrays.copyOf(values, length));
    }

    /** Builds an insertion-ordered mutable state map. */
    public static Map<String, Object> state(Object... kvs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kvs.length; i += 2) {
            m.put(String.valueOf(kvs[i]), kvs[i + 1]);
        }
        return m;
    }
}