package com.loginsight.query.engine.approximation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.approximation.ApproximationResult;
import com.loginsight.dsa.approximation.UndirectedGraph;
import com.loginsight.dsa.approximation.VertexCoverApproximation;
import com.loginsight.dto.request.VertexCoverRequest;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.Results;

/**
 * Greedy vertex-cover 2-approximation (docs/12 §7): repeatedly pick an edge and take both endpoints
 * until every edge is hit. The cover (as service names) and the approximation-ratio bound are the
 * payload; the maximal-matching evidence behind the 2-approx guarantee is intermediate.
 */
public final class VertexCoverEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.APPROXIMATE_COVER;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.VERTEX_COVER;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        VertexCoverRequest request = (VertexCoverRequest) context.getRequest();
        Map<String, Integer> index = new LinkedHashMap<>();
        for (String node : request.nodes()) {
            addVertex(index, node);
        }
        for (String[] edge : request.edges()) {
            if (edge == null || edge.length != 2) {
                throw new InvalidQueryException("each edge must be a {a,b} pair");
            }
            addVertex(index, edge[0]);
            addVertex(index, edge[1]);
        }
        if (index.isEmpty()) {
            throw new InvalidQueryException("at least one node or edge is required");
        }

        UndirectedGraph graph = new UndirectedGraph(index.size());
        for (String[] edge : request.edges()) {
            graph.addEdge(index.get(edge[0]), index.get(edge[1]));
        }
        if (graph.edgeCount() == 0) {
            throw new InvalidQueryException("at least one edge is required");
        }

        long start = System.nanoTime();
        ApproximationResult approx = new VertexCoverApproximation().approximateVertexCover(graph);
        long elapsed = System.nanoTime() - start;

        List<String> cover = new ArrayList<>();
        for (int vertex : approx.getCover()) {
            cover.add(index.entrySet().stream()
                    .filter(entry -> entry.getValue() == vertex)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("unknown-" + vertex));
        }
        Map<String, Object> result = Map.of("cover", cover, "coverSize", approx.getCoverSize(),
                "matchingSize", approx.getMatchingSize(), "lowerBound", approx.getLowerBound(),
                "approximationRatio", approx.getApproximationRatio(),
                "vertexCount", graph.vertexCount());
        long memory = 16L * graph.edgeCount();
        return Results.measured(type(), algorithm(), graph.vertexCount(), start, result,
                Map.of("matching", approx.getMatchingSize(), "ratioBound",
                        approx.getApproximationRatio()), memory,
                approx.getTimeComplexity(), approx.getSpaceComplexity(), approx.getNotes());
    }

    private static void addVertex(Map<String, Integer> index, String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidQueryException("node names must not be blank");
        }
        index.putIfAbsent(name, index.size());
    }
}