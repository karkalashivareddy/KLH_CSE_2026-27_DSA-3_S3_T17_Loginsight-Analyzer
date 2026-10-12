package com.loginsight.trace;

import java.util.List;
import java.util.Map;

/**
 * A single observable operation of a traced algorithm execution.
 *
 * <p>Every step is produced by the real algorithm run while it executes and is consumed by the
 * Algorithm Laboratory front-end to render step-by-step playback. A step never contains a hidden or
 * invented state: {@code state} mirrors the values the algorithm actually held at that point
 * (text cursor, pattern state, DP cell, residual capacities, …).</p>
 *
 * @param index       1-based position of the step within the full trace
 * @param operation   short machine-readable tag, e.g. {@code COMPARE}, {@code FALLBACK},
 *                    {@code AUGMENT}, {@code WITNESS}, {@code PARTITION}
 * @param description human-readable explanation of what happened at this step
 * @param state       named values describing the algorithm state (free-form, but must be a
 *                    JSON-serialisable structure)
 * @param highlighted indices/positions that the visualiser should emphasise at this step
 * @param metrics     numeric side-observations (comparisons made, flow pushed, window hash, …)
 */
public record AlgorithmStep(int index, String operation, String description,
                            Map<String, Object> state, List<Integer> highlighted,
                            Map<String, Object> metrics) {

    public AlgorithmStep {
        state = state == null ? Map.of() : state;
        highlighted = highlighted == null ? List.of() : highlighted;
        metrics = metrics == null ? Map.of() : metrics;
    }
}