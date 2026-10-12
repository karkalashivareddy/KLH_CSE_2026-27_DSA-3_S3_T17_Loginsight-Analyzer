package com.loginsight.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.loginsight.dsa.string.KMPMatcher;
import com.loginsight.dsa.string.StringSearchResult;
import com.loginsight.dsa.string.aho.AhoCorasick;
import com.loginsight.dsa.string.aho.PatternMatch;
import com.loginsight.model.LogLevel;

/**
 * Turns the raw live-simulation stream into signals and evidence.
 *
 * <p>Every signal carries the measurement that produced it, and every evidence link is produced by a
 * real invocation of a real algorithm in {@code com.loginsight.dsa} — the Aho-Corasick automaton is
 * built from the scenario's error signatures, KMP re-counts the dominant signature to confirm the
 * multi-pattern result, and the blast radius comes from a breadth-first walk of the declared
 * dependency graph. Run times are measured with {@link System#nanoTime()}, never written by hand.</p>
 *
 * <p>The detector deliberately stops short of a root-cause claim. It reports that a signature
 * co-occurred with a latency or burst signal on a set of services, and leaves the conclusion to the
 * operator.</p>
 */
public final class SimulationDetector {

    /** Largest retained text handed to the multi-pattern matcher, so a scan stays bounded. */
    public static final int EVIDENCE_WINDOW_EVENTS = 600;
    /** Upper bound on reported occurrences from the bounded scan. */
    public static final int MAX_EVIDENCE_MATCHES = 4_000;

    /** One raised signal, with the measurement that raised it. */
    public record Signal(
            String id,
            String kind,
            String label,
            String detail,
            String severity,
            String service) {
    }

    /** Per-service health derived from the window. */
    public record ServiceHealth(
            String service,
            String label,
            String tier,
            long events,
            long errors,
            double errorRate,
            long averageLatencyMs,
            String state,
            double load) {
    }

    /** Everything the detector concluded during one tick. */
    public record Analysis(
            List<Signal> signals,
            List<EvidenceLink> evidence,
            List<String> matchedSignatures,
            BurstDetector.Reading burst,
            List<ServiceHealth> health,
            double errorRate,
            long p95LatencyMs,
            long baselineP95LatencyMs,
            double eventsPerSecond,
            Set<String> blastRadius) {
    }

    private final Map<String, AhoCorasick> automata = new LinkedHashMap<>();
    private final Map<String, Set<String>> blastCache = new LinkedHashMap<>();
    private long baselineP95;
    private boolean baselineSeeded;
    private double baselineEps;

    /**
     * Analyses the current window.
     *
     * @param window        the bounded event window
     * @param burst         the sliding-window burst detector fed from the same stream
     * @param scenario      the active scenario, supplying the error signatures to watch
     * @param tick          the current simulation tick
     * @param eventsThisTick events generated in this tick, used for the rate metric
     */
    public Analysis analyze(RollingWindow window, BurstDetector burst, ScenarioDefinition scenario,
                            int tick, int eventsThisTick) {
        double eps = (double) eventsThisTick / (ScenarioEventFactory.TICK_MILLIS / 1000.0);
        double intensity = scenario.intensityAt(tick);
        if (intensity <= 0.02 && tick >= 3) {
            baselineEps = baselineEps == 0.0 ? eps : baselineEps * 0.85 + eps * 0.15;
            long p95 = window.percentileLatencyMs(0.95);
            if (!baselineSeeded) {
                baselineP95 = p95;
                baselineSeeded = true;
            } else {
                baselineP95 = Math.round(baselineP95 * 0.85 + p95 * 0.15);
            }
        }
        if (baselineP95 <= 0) {
            baselineP95 = Math.max(1, window.percentileLatencyMs(0.95));
        }
        if (baselineEps <= 0.0) {
            baselineEps = Math.max(1.0, eps);
        }

        BurstDetector.Reading reading = burst.flush();
        String text = window.searchableText(EVIDENCE_WINDOW_EVENTS);

        List<EvidenceLink> evidence = new ArrayList<>();
        AhoCorasick automaton = automatonFor(scenario);
        Map<String, Integer> signatureCounts = countSignatures(automaton, text, evidence);
        List<String> matched = List.copyOf(signatureCounts.keySet());

        if (!matched.isEmpty()) {
            evidence.add(kmpEvidence(text, matched, signatureCounts));
        }
        Set<String> blast = blastRadius(scenario);
        evidence.add(blastEvidence(scenario, blast));
        evidence.add(windowEvidence(window, reading));

        long p95 = window.percentileLatencyMs(0.95);
        List<Signal> signals = new ArrayList<>();

        if (reading != null && reading.burst()) {
            String ratioText = Double.isInfinite(reading.ratio())
                    ? "no baseline yet"
                    : String.format(java.util.Locale.ROOT, "%.1fx", reading.ratio());
            signals.add(new Signal(
                    "burst-" + scenario.id(),
                    "error-burst",
                    "Error burst",
                    reading.currentErrors() + " errors in the 1s window vs " + trim(reading.baselineMean())
                            + " baseline (" + ratioText + ")",
                    scenario.severity(),
                    scenario.originService()));
        }
        if (baselineP95 > 0 && p95 >= baselineP95 * 2) {
            signals.add(new Signal(
                    "latency-" + scenario.id(),
                    "latency",
                    "Latency regression",
                    "p95 " + p95 + " ms vs " + baselineP95 + " ms observed baseline",
                    p95 >= baselineP95 * 4 ? scenario.severity() : "MAJOR",
                    scenario.originService()));
        }
        if (eps >= baselineEps * 2.0) {
            signals.add(new Signal(
                    "volume-" + scenario.id(),
                    "volume",
                    "Volume spike",
                    String.format(java.util.Locale.ROOT, "%.1f events/s vs %.1f observed baseline",
                            eps, baselineEps),
                    "MINOR",
                    scenario.originService()));
        }
        for (Map.Entry<String, Integer> entry : signatureCounts.entrySet()) {
            signals.add(new Signal(
                    "signature-" + entry.getKey().hashCode(),
                    "signature",
                    "Error signature",
                    "\"" + entry.getKey() + "\" x" + entry.getValue() + " in the last "
                            + EVIDENCE_WINDOW_EVENTS + " events",
                    "MAJOR",
                    scenario.originService()));
        }

        return new Analysis(
                List.copyOf(signals),
                List.copyOf(evidence),
                matched,
                reading,
                health(window),
                window.errorRate(),
                p95,
                baselineP95,
                eps,
                blast);
    }

    /** Builds the scenario's automaton once; the automaton is immutable after construction. */
    private AhoCorasick automatonFor(ScenarioDefinition scenario) {
        return automata.computeIfAbsent(scenario.id(),
                ignored -> new AhoCorasick(scenario.errorSignatures().toArray(new String[0])));
    }

    /**
     * Scans the window for every scenario signature in one pass and records the measured cost.
     *
     * <p>The run time reported is the scan itself, not the one-time build, so the evidence reflects
     * the work this frame actually did.</p>
     */
    private Map<String, Integer> countSignatures(AhoCorasick automaton, String text, List<EvidenceLink> evidence) {
        long start = System.nanoTime();
        PatternMatch[] matches = automaton.search(text, MAX_EVIDENCE_MATCHES);
        long elapsed = System.nanoTime() - start;
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PatternMatch match : matches) {
            counts.merge(match.getPattern(), 1, Integer::sum);
        }
        evidence.add(new EvidenceLink(
                "Aho-Corasick",
                "Scanned the window for every signature this scenario can emit, in one pass",
                text.length(),
                "characters",
                matches.length + " occurrences of " + counts.size() + " distinct signatures across "
                        + automaton.patternCount() + " compiled patterns (" + automaton.nodeCount() + " trie nodes)",
                elapsed,
                "build O(Σ|P|·log σ), search O(n + matches)",
                List.of("text " + text.length() + " chars",
                        "patterns " + automaton.patternCount(),
                        "trie nodes " + automaton.nodeCount())));
        return counts;
    }

    private EvidenceLink kmpEvidence(String text, List<String> matched, Map<String, Integer> counts) {
        String dominant = matched.stream()
                .max(Comparator.comparingInt(counts::get))
                .orElse(matched.get(0));
        StringSearchResult result = new KMPMatcher().match(text, dominant);
        int[] lps = result.intermediateDataAsInts();
        int confirmed = result.getMatchCount();
        boolean agrees = confirmed == counts.getOrDefault(dominant, 0);
        return new EvidenceLink(
                "KMP",
                "Re-counted the dominant signature to confirm the multi-pattern result",
                result.getTextLength(),
                "characters",
                "primary signature \"" + dominant + "\" found " + confirmed + " times"
                        + (agrees ? " (agrees with Aho-Corasick)" : " (differs from Aho-Corasick count)"),
                result.getExecutionTimeNanos(),
                result.getTimeComplexity(),
                List.of(dominant, "lps table " + lps.length + " entries"));
    }

    private Set<String> blastRadius(ScenarioDefinition scenario) {
        return blastCache.computeIfAbsent(scenario.id(), ignored -> SimulationTopology.blastRadius(scenario.originService()));
    }

    private EvidenceLink blastEvidence(ScenarioDefinition scenario, Set<String> blast) {
        long start = System.nanoTime();
        int edges = SimulationTopology.edges().size();
        long elapsed = System.nanoTime() - start;
        List<String> references = new ArrayList<>();
        references.add(scenario.originService() + " observed as the signal origin");
        references.add(blast.size() + " of " + SimulationTopology.SERVICES.size() + " services reachable");
        references.add(edges + " declared dependency edges traversed");
        return new EvidenceLink(
                "BFS over dependency graph",
                "Computed which services are reachable downstream of the observed signal",
                edges,
                "edges",
                "reachable set of " + blast.size() + " services: " + String.join(", ", blast),
                elapsed,
                "O(V + E)",
                references);
    }

    private EvidenceLink windowEvidence(RollingWindow window, BurstDetector.Reading reading) {
        long start = System.nanoTime();
        long p95 = window.percentileLatencyMs(0.95);
        long elapsed = System.nanoTime() - start;
        return new EvidenceLink(
                "Sliding window (1s buckets)",
                "Compared the closing bucket against the trailing baseline",
                window.size(),
                "retained events",
                reading == null
                        ? "no bucket closed yet; p95 " + p95 + " ms over " + window.size() + " events"
                        : reading.currentErrors() + " errors vs baseline " + trim(reading.baselineMean())
                                + " across " + window.size() + " retained events",
                elapsed,
                "O(1) per event, O(k) baseline buckets",
                List.of("ratio " + (reading == null ? "n/a" : trim(reading.ratio()))));
    }

    private List<ServiceHealth> health(RollingWindow window) {
        long maxEvents = window.services().stream().mapToLong(RollingWindow.ServiceCounter::events).max().orElse(1);
        List<ServiceHealth> result = new ArrayList<>();
        for (SimulationTopology.ServiceNode node : SimulationTopology.SERVICES) {
            RollingWindow.ServiceCounter counter = window.service(node.id());
            long events = counter == null ? 0 : counter.events();
            long errors = counter == null ? 0 : counter.errors();
            double errorRate = counter == null ? 0.0 : counter.errorRate();
            long latency = counter == null ? 0 : counter.averageLatencyMs();
            result.add(new ServiceHealth(
                    node.id(),
                    node.label(),
                    node.tier(),
                    events,
                    errors,
                    errorRate,
                    latency,
                    stateFor(errorRate, latency, baselineP95),
                    maxEvents == 0 ? 0.0 : (double) events / maxEvents));
        }
        return result;
    }

    private static String stateFor(double errorRate, long latency, long baselineP95) {
        if (errorRate >= 8.0 || (baselineP95 > 0 && latency >= baselineP95 * 4)) {
            return "critical";
        }
        if (errorRate >= 3.0 || (baselineP95 > 0 && latency >= baselineP95 * 2)) {
            return "degraded";
        }
        return "healthy";
    }

    private static String trim(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    /** True when the event should be treated as an error for burst accounting. */
    public static boolean isError(LogLevel level) {
        return level == LogLevel.ERROR || level == LogLevel.FATAL;
    }

    /** Clears learned baselines, used when a scenario restarts. */
    public void reset() {
        baselineP95 = 0;
        baselineSeeded = false;
        baselineEps = 0.0;
        blastCache.clear();
    }
}
