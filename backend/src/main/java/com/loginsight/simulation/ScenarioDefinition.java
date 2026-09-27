package com.loginsight.simulation;

import java.util.List;

/**
 * A declarative, fully deterministic live-simulation scenario.
 *
 * <p>Each scenario is a fixed description of a failure mode: which services degrade, which error
 * signatures appear, how the event volume moves, and what the incident detector is expected to
 * surface. Nothing here is learned or inferred — the same {@code (scenario, seed)} pair always
 * produces the same event progression, so the demo is repeatable and the evidence is checkable.</p>
 *
 * @param id                 stable machine key, also the default route segment
 * @param title              product-facing name
 * @param summary            one-sentence description used on scenario cards
 * @param seed               default deterministic seed for the scenario
 * @param durationTicks      simulated ticks before the scenario loops back to its healthy baseline
 * @param onsetTick          first tick in which the failure signal becomes observable
 * @param peakTick           first tick in which the failure is at full intensity
 * @param recoveryTick       first tick in which the signal decays again
 * @param originService      the service the scenario treats as the initial degradation point
 * @param affectedServices   services named directly by the failure (origin first)
 * @param volumeMultiplier   peak event-volume multiplier relative to the healthy baseline
 * @param peakErrorRate      target share of ERROR/FATAL events at peak intensity, 0..1
 * @param peakLatencyMs      target p95 response time at peak intensity
 * @param errorSignatures    literal substrings the Aho-Corasick matcher watches for
 * @param expectedSignal     the signal the sliding-window detector is expected to raise
 * @param expectedIncident   the incident title the heuristic detector is expected to open
 * @param severity           severity the detector is expected to assign
 */
public record ScenarioDefinition(
        String id,
        String title,
        String summary,
        long seed,
        int durationTicks,
        int onsetTick,
        int peakTick,
        int recoveryTick,
        String originService,
        List<String> affectedServices,
        double volumeMultiplier,
        double peakErrorRate,
        long peakLatencyMs,
        List<String> errorSignatures,
        String expectedSignal,
        String expectedIncident,
        String severity) {

    public ScenarioDefinition {
        affectedServices = List.copyOf(affectedServices);
        errorSignatures = List.copyOf(errorSignatures);
    }

    /**
     * Normalised failure intensity for a tick in {@code [0, 1]}.
     *
     * <p>Rises linearly from {@code onsetTick} to {@code peakTick}, holds through the peak plateau,
     * then decays linearly from {@code recoveryTick} back to zero at {@code durationTicks}. Outside
     * the scenario window the system is healthy, which is what makes the
     * red &rarr; amber &rarr; green resolution sequence observable.</p>
     */
    public double intensityAt(int tick) {
        if (tick < onsetTick || tick >= durationTicks) {
            return 0.0;
        }
        if (tick < peakTick) {
            return (double) (tick - onsetTick) / Math.max(1, peakTick - onsetTick);
        }
        if (tick < recoveryTick) {
            return 1.0;
        }
        double decaySpan = Math.max(1, durationTicks - recoveryTick);
        double remaining = (double) (durationTicks - tick) / decaySpan;
        return Math.max(0.0, Math.min(1.0, remaining));
    }

    /** Simulated event rate (events/second) for a tick. */
    public double rateAt(int tick, double healthyRate) {
        return healthyRate * (1.0 + (volumeMultiplier - 1.0) * intensityAt(tick));
    }

    /** Simulated p95 latency (ms) for a tick. */
    public long latencyAt(int tick, long healthyLatency) {
        double intensity = intensityAt(tick);
        long peak = Math.max(healthyLatency, peakLatencyMs);
        return Math.round(healthyLatency + (peak - healthyLatency) * intensity);
    }

    public boolean isValid() {
        return durationTicks > 0
                && onsetTick >= 0
                && peakTick >= onsetTick
                && recoveryTick >= peakTick
                && recoveryTick <= durationTicks
                && SimulationTopology.isKnown(originService);
    }
}
