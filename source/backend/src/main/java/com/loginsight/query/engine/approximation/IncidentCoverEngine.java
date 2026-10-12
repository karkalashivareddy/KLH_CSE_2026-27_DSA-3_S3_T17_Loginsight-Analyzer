package com.loginsight.query.engine.approximation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.approximation.MaximalMatching;
import com.loginsight.dsa.approximation.UndirectedGraph;
import com.loginsight.dsa.approximation.UndirectedEdge;
import com.loginsight.dto.request.IncidentCoverRequest;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.Results;

/**
 * Incident-to-service cover (docs/12 §7): the on-call view asks for the smallest set of services
 * touching every incident edge. A maximal matching is built greedily and its endpoints form a
 * vertex cover of size ≤ 2× optimal — the same 2-approximation used for node cover, labelled with
 * the MAXIMAL_MATCHING algorithm so the routing key stays distinct.
 */
public final class IncidentCoverEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.APPROXIMATE_COVER;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.MAXIMAL_MATCHING;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        IncidentCoverRequest request = (IncidentCoverRequest) context.getRequest();
        Map<String, Integer> index = new LinkedHashMap<>();
        for (String service : request.services()) {
            index.putIfAbsent(service, index.size());
        }
        if (index.isEmpty()) {
            throw new InvalidQueryException("services must not be empty");
        }

        UndirectedGraph graph = new UndirectedGraph(index.size());
        for (String[] relationship : request.relationships()) {
            if (relationship == null || relationship.length != 2) {
                throw new InvalidQueryException("each relationship must be a {a,b} pair");
            }
            Integer u = index.get(relationship[0]);
            Integer v = index.get(relationship[1]);
            if (u == null) {
                throw new InvalidQueryException("unknown service '" + relationship[0] + "'");
            }
            if (v == null) {
                throw new InvalidQueryException("unknown service '" + relationship[1] + "'");
            }
            graph.addEdge(u, v);
        }
        if (graph.edgeCount() == 0) {
            throw new InvalidQueryException("at least one relationship edge is required");
        }

        long start = System.nanoTime();
        int[] matchedEdges = new MaximalMatching().matchingEdgeIds(graph);
        int matchingSize = new MaximalMatching().matchingSize(graph);
        long elapsed = System.nanoTime() - start;

        java.util.Set<Integer> coverVertices = new java.util.LinkedHashSet<>();
        for (int edgeId : matchedEdges) {
            UndirectedEdge edge = graph.edge(edgeId);
            coverVertices.add(edge.getU());
            coverVertices.add(edge.getV());
        }
        List<String> cover = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : index.entrySet()) {
            if (coverVertices.contains(entry.getValue())) {
                cover.add(entry.getKey());
            }
        }
        List<String> uncovered = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : index.entrySet()) {
            if (!coverVertices.contains(entry.getValue())) {
                uncovered.add(entry.getKey());
            }
        }
        Map<String, Object> result = Map.of("cover", cover, "coverSize", cover.size(),
                "matchingSize", matchingSize, "uncovered", uncovered);
        long memory = 16L * graph.edgeCount();
        return Results.measured(type(), algorithm(), graph.vertexCount(), start, result,
                Map.of("matchedEdgeCount", matchedEdges.length), memory, "O(E)",
                "O(V + E) matching", "maximal matching endpoints: vertex cover ≤ 2× optimal");
    }
}