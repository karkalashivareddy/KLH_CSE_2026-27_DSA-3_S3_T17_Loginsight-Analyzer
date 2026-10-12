package com.loginsight.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Small directed service-dependency graph over service names built from log events (docs/03 graph).
 * An edge {@code a -> b} records that a request flowed from service {@code a} to service {@code b}
 * (events sharing a {@code requestId} across services, ordered by timestamp). Pure Java; the 
 * web/analytics layer reads it via {@link ServiceGraphBuilder}.
 */
public final class ServiceDependencyGraph {

    private final Map<String, Set<String>> outbound = new LinkedHashMap<>();
    private final Map<String, Set<String>> inbound = new LinkedHashMap<>();

    public void addNode(String service) {
        outbound.computeIfAbsent(service, k -> new LinkedHashSet<>());
        inbound.computeIfAbsent(service, k -> new LinkedHashSet<>());
    }

    public void addDependency(String from, String to) {
        addNode(from);
        addNode(to);
        if (!from.equals(to)) {
            outbound.get(from).add(to);
            inbound.get(to).add(from);
        }
    }

    public Set<String> nodes() {
        return Collections.unmodifiableSet(outbound.keySet());
    }

    public List<String> successors(String node) {
        return outbound.getOrDefault(node, Set.of()).stream().toList();
    }

    public List<String> predecessors(String node) {
        return inbound.getOrDefault(node, Set.of()).stream().toList();
    }

    public int nodeCount() {
        return outbound.size();
    }

    public int edgeCount() {
        int count = 0;
        for (Set<String> targets : outbound.values()) {
            count += targets.size();
        }
        return count;
    }

    public int outDegree(String node) {
        return outbound.getOrDefault(node, Set.of()).size();
    }

    public int inDegree(String node) {
        return inbound.getOrDefault(node, Set.of()).size();
    }

    /** All services reachable from {@code start} following outbound edges (BFS). */
    public List<String> reachableFrom(String start) {
        List<String> visited = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        if (outbound.containsKey(start)) {
            queue.add(start);
            seen.add(start);
        }
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            visited.add(current);
            for (String next : outbound.getOrDefault(current, Set.of())) {
                if (seen.add(next)) {
                    queue.addLast(next);
                }
            }
        }
        return visited;
    }

    @Override
    public String toString() {
        return "ServiceDependencyGraph[" + nodeCount() + " services, " + edgeCount() + " edges]";
    }
}