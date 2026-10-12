package com.loginsight.query.engine.approximation;

import java.util.LinkedHashMap;
import java.util.Map;

import com.loginsight.dsa.approximation.UndirectedGraph;
import com.loginsight.dto.request.VertexCoverRequest;
import com.loginsight.exception.InvalidQueryException;

/**
 * Builds an {@link UndirectedGraph} (and the name→id mapping) for the vertex-cover scenario from a
 * {@link VertexCoverRequest}, validating node names and the shape of every edge. Shared by the
 * ordinary {@link VertexCoverEngine} and the trace-capable laboratory path so both run against the
 * same problem definition.
 */
public final class VertexCoverGraphBuilder {

    private VertexCoverGraphBuilder() {
    }

    public record Build(UndirectedGraph graph, String[] names) {
    }

    public static Build build(VertexCoverRequest request) {
        if (request.nodes() == null && (request.edges() == null || request.edges().length == 0)) {
            throw new InvalidQueryException("at least one node or edge is required");
        }
        Map<String, Integer> index = new LinkedHashMap<>();
        if (request.nodes() != null) {
            for (String node : request.nodes()) {
                addVertex(index, node);
            }
        }
        if (request.edges() != null) {
            for (String[] edge : request.edges()) {
                if (edge == null || edge.length != 2 || edge[0] == null || edge[1] == null
                        || edge[0].isBlank() || edge[1].isBlank()) {
                    throw new InvalidQueryException("each edge must be a non-blank {a,b} pair");
                }
                addVertex(index, edge[0]);
                addVertex(index, edge[1]);
            }
        }
        if (index.isEmpty()) {
            throw new InvalidQueryException("at least one node or edge is required");
        }
        UndirectedGraph graph = new UndirectedGraph(index.size());
        if (request.edges() != null) {
            for (String[] edge : request.edges()) {
                graph.addEdge(index.get(edge[0]), index.get(edge[1]));
            }
        }
        return new Build(graph, index.keySet().toArray(new String[0]));
    }

    private static void addVertex(Map<String, Integer> index, String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidQueryException("node names must not be blank");
        }
        index.putIfAbsent(name, index.size());
    }
}