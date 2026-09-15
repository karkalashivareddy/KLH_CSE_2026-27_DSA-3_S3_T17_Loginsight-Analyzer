package com.loginsight.query.engine.flow;

import java.util.LinkedHashMap;
import java.util.Map;

import com.loginsight.dsa.flow.FlowGraph;
import com.loginsight.dto.request.FlowRequest;
import com.loginsight.dto.request.GraphEdge;
import com.loginsight.exception.InvalidQueryException;

/**
 * Builds the {@link FlowGraph} for the flow scenario from a {@link FlowRequest}, mapping service
 * names to vertex ids and rejecting unknown nodes, a missing source/sink, {@code source == sink}
 * and non-positive capacities before any algorithm runs (docs/12 §3).
 */
public final class FlowGraphFactory {

    private FlowGraphFactory() {
    }

    public record NamedFlow(FlowGraph graph, String[] names, int source, int sink) {
    }

    public static NamedFlow build(FlowRequest request) {
        if (request.source() == null || request.sink() == null) {
            throw new InvalidQueryException("source and sink are required");
        }
        if (request.source().equals(request.sink())) {
            throw new InvalidQueryException("source must differ from sink");
        }
        String[] nodes = request.nodes();
        if (nodes == null || nodes.length < 2) {
            throw new InvalidQueryException("nodes must name at least 2 services");
        }
        for (String node : nodes) {
            if (node == null || node.isBlank()) {
                throw new InvalidQueryException("nodes must not contain blank entries");
            }
        }
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < nodes.length; i++) {
            index.put(nodes[i], i);
        }
        int source = requireVertex(index, request.source());
        int sink = requireVertex(index, request.sink());
        if (source == sink) {
            throw new InvalidQueryException("source must differ from sink");
        }

        FlowGraph graph = new FlowGraph(nodes.length);
        GraphEdge[] edges = request.edges();
        if (edges != null) {
            for (int i = 0; i < edges.length; i++) {
                GraphEdge edge = edges[i];
                int from = requireVertex(index, edge.from());
                int to = requireVertex(index, edge.to());
                if (edge.capacity() == null || edge.capacity() <= 0) {
                    throw new InvalidQueryException("edge " + i + " capacity must be > 0");
                }
                if (edge.cost() != null && edge.cost() < 0) {
                    throw new InvalidQueryException("edge " + i + " cost must be >= 0");
                }
                graph.addEdge(from, to, edge.capacity());
            }
            if (graph.edgeCount() == 0) {
                throw new InvalidQueryException("at least one edge is required");
            }
        } else {
            throw new InvalidQueryException("edges are required");
        }
        return new NamedFlow(graph, nodes.clone(), source, sink);
    }

    private static int requireVertex(Map<String, Integer> index, String name) {
        Integer vertex = index.get(name);
        if (vertex == null) {
            throw new InvalidQueryException("unknown node '" + name + "' — must reference nodes[]");
        }
        return vertex;
    }
}