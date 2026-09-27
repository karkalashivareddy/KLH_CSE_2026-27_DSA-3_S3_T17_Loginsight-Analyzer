package com.loginsight.simulation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.loginsight.graph.ServiceDependencyGraph;

/**
 * The canonical eight-service topology the live simulation runs on.
 *
 * <p>Unlike the dataset-backed {@code /api/analytics/dependencies} view — which reports adjacency
 * inferred from observed request identifiers — this graph is <em>declared</em> for the simulation. It
 * exists so the scenario engine can reason about propagation and blast radius, and so the 2D/3D
 * topology has a stable node set before the first event is emitted. It is labelled as a modelled
 * dependency graph in the UI, never as observed infrastructure.</p>
 */
public final class SimulationTopology {

    /** A service node: display name, short id, tier and baseline request share. */
    public record ServiceNode(String id, String label, String tier, double baseShare) {
    }

    public static final List<ServiceNode> SERVICES = List.of(
            new ServiceNode("api-gateway", "API Gateway", "edge", 1.00),
            new ServiceNode("auth", "Auth", "app", 0.42),
            new ServiceNode("orders", "Orders", "app", 0.86),
            new ServiceNode("payments", "Payments", "app", 0.74),
            new ServiceNode("inventory", "Inventory", "app", 0.38),
            new ServiceNode("postgres", "Database", "data", 0.30),
            new ServiceNode("redis", "Cache", "data", 0.24),
            new ServiceNode("notifications", "Notifications", "async", 0.18));

    /** Declared caller -&gt; callee dependencies. */
    public static final Map<String, List<String>> DEPENDENCIES = buildDependencies();

    private static Map<String, List<String>> buildDependencies() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("api-gateway", List.of("auth", "orders", "payments"));
        map.put("auth", List.of("redis"));
        map.put("orders", List.of("payments", "inventory", "postgres", "redis"));
        map.put("payments", List.of("postgres", "redis"));
        map.put("inventory", List.of("postgres"));
        map.put("notifications", List.of("redis"));
        return Map.copyOf(map);
    }

    private SimulationTopology() {
    }

    /** All declared dependency pairs. */
    public static List<Edge> edges() {
        return DEPENDENCIES.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(target -> new Edge(entry.getKey(), target)))
                .toList();
    }

    /** A single declared dependency edge. */
    public record Edge(String source, String target) {
    }

    /** A fresh graph instance for BFS propagation and Dijkstra-style cost reasoning. */
    public static ServiceDependencyGraph graph() {
        ServiceDependencyGraph graph = new ServiceDependencyGraph();
        SERVICES.forEach(node -> graph.addNode(node.id()));
        DEPENDENCIES.forEach((source, targets) -> targets.forEach(target -> graph.addDependency(source, target)));
        return graph;
    }

    /** Services that call {@code service} directly. */
    public static Set<String> directDependents(String service) {
        Set<String> dependents = new LinkedHashSet<>();
        DEPENDENCIES.forEach((source, targets) -> {
            if (targets.contains(service)) {
                dependents.add(source);
            }
        });
        return dependents;
    }

    /**
     * Blast radius of a failure originating at {@code service}: the full upstream closure, i.e. every
     * service with a declared call path to it.
     *
     * <p>Traversal runs over {@code predecessors} rather than {@code successors}, because a failing
     * dependency propagates <em>up</em> the call chain: Payments failing endangers Orders and the
     * API Gateway, not the services Payments itself calls.</p>
     */
    public static Set<String> blastRadius(String service) {
        ServiceDependencyGraph graph = graph();
        Set<String> visited = new LinkedHashSet<>();
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        visited.add(service);
        queue.add(service);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            for (String caller : graph.predecessors(current)) {
                if (visited.add(caller)) {
                    queue.addLast(caller);
                }
            }
        }
        return visited;
    }

    public static String label(String id) {
        for (ServiceNode node : SERVICES) {
            if (node.id().equals(id)) {
                return node.label();
            }
        }
        return id;
    }

    public static boolean isKnown(String id) {
        return SERVICES.stream().anyMatch(node -> node.id().equals(id));
    }
}
