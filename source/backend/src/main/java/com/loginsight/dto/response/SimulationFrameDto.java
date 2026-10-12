package com.loginsight.dto.response;

import java.util.List;

import com.loginsight.simulation.SimulationDetector;
import com.loginsight.simulation.SimulationIncident;
import com.loginsight.simulation.SimulationTopology;

/**
 * One frame of the live simulation stream ({@code GET /api/simulation/stream}).
 *
 * <p>Unlike the dataset replay stream, this frame is a complete analytical snapshot: the events
 * generated for the tick, the aggregate metrics, the raised signals, the measured algorithm evidence,
 * per-service health, the modelled topology and any open incidents. The frontend derives every
 * visualisation from this one payload instead of recomputing analytics in the browser.</p>
 *
 * <p>{@code source} is always {@code live-simulation} and {@code label} states plainly that the
 * traffic is generated. {@code determinism} names the seed that produced the sequence, so any frame
 * can be reproduced.</p>
 */
public record SimulationFrameDto(
        String sessionId,
        long sequence,
        String source,
        String label,
        String scenarioId,
        String scenarioTitle,
        long seed,
        int tick,
        int tickMillis,
        int eventsPerFrame,
        double intensity,
        String phase,
        long totalEvents,
        long totalErrors,
        double errorRate,
        double eventsPerSecond,
        long averageLatencyMs,
        long p95LatencyMs,
        long baselineP95LatencyMs,
        int windowSize,
        int windowCapacity,
        List<String> matchedSignatures,
        List<LogEventDto> events,
        List<SimulationIncidentDto.SignalDto> signals,
        List<SimulationIncidentDto.EvidenceDto> evidence,
        List<ServiceHealthDto> health,
        List<SimulationIncidentDto> incidents,
        TopologyDto topology) {

    /** Per-service health used by the health strip, the topology and the service explorer. */
    public record ServiceHealthDto(
            String service,
            String label,
            String tier,
            long events,
            long errors,
            double errorRate,
            long averageLatencyMs,
            String state,
            double load,
            boolean inBlastRadius) {
        public static ServiceHealthDto from(SimulationDetector.ServiceHealth health, boolean inBlastRadius) {
            return new ServiceHealthDto(
                    health.service(),
                    health.label(),
                    health.tier(),
                    health.events(),
                    health.errors(),
                    Math.round(health.errorRate() * 100) / 100.0,
                    health.averageLatencyMs(),
                    health.state(),
                    Math.round(health.load() * 1000) / 1000.0,
                    inBlastRadius);
        }
    }

    /** The declared dependency graph the simulation runs on. */
    public record TopologyDto(List<Node> nodes, List<Edge> edges, String kind, String label) {
        public record Node(String id, String label, String tier) {
        }

        public record Edge(String source, String target) {
        }

        public static TopologyDto declared() {
            return new TopologyDto(
                    SimulationTopology.SERVICES.stream()
                            .map(node -> new Node(node.id(), node.label(), node.tier()))
                            .toList(),
                    SimulationTopology.edges().stream()
                            .map(edge -> new Edge(edge.source(), edge.target()))
                            .toList(),
                    "declared",
                    "Modelled dependency graph for the simulation — declared, not observed from traffic");
        }
    }

    /** Lifecycle states the frontend may request, mirroring the backend enum. */
    public static List<String> lifecycleStates() {
        return java.util.Arrays.stream(SimulationIncident.Status.values()).map(Enum::name).toList();
    }
}
