package com.loginsight.query.engine.flow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.flow.BipartiteMatching;
import com.loginsight.dsa.flow.FlowGraph;
import com.loginsight.dsa.flow.MatchingResult;
import com.loginsight.dto.request.MatchingRequest;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.Results;

/**
 * Maximum matching in the bipartite incident-to-resource graph (docs/12 §3). Left vertices are the
 * incidents, right vertices the resources; each {@code {incident, resource}} edge carries unit
 * capacity, so the maximum matching equals the maximum flow across the split.
 */
public final class MatchingEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.MATCHING;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.BIPARTITE_MATCHING;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        MatchingRequest request = (MatchingRequest) context.getRequest();
        if (request.incidents() == null || request.incidents().length == 0) {
            throw new InvalidQueryException("incidents must not be empty");
        }
        if (request.resources() == null || request.resources().length == 0) {
            throw new InvalidQueryException("resources must not be empty");
        }

        Map<String, Integer> left = new LinkedHashMap<>();
        for (int i = 0; i < request.incidents().length; i++) {
            if (request.incidents()[i] == null || request.incidents()[i].isBlank()) {
                throw new InvalidQueryException("incidents must not contain blank entries");
            }
            left.put(request.incidents()[i], i);
        }
        Map<String, Integer> right = new LinkedHashMap<>();
        for (int i = 0; i < request.resources().length; i++) {
            right.put(request.resources()[i], i);
        }

        int leftCount = request.incidents().length;
        int rightCount = request.resources().length;
        FlowGraph graph = new FlowGraph(leftCount + rightCount);
        java.util.List<String> edgeNames = new ArrayList<>();
        for (String[] edge : request.edges()) {
            if (edge == null || edge.length != 2) {
                throw new InvalidQueryException("each matching edge must be a {from,to} pair");
            }
            Integer from = left.get(edge[0]);
            Integer to = right.get(edge[1]);
            if (from == null) {
                throw new InvalidQueryException("unknown incident '" + edge[0] + "'");
            }
            if (to == null) {
                throw new InvalidQueryException("unknown resource '" + edge[1] + "'");
            }
            graph.addEdge(from, to, 1L);
            edgeNames.add(edge[0] + "->" + edge[1]);
        }
        if (graph.edgeCount() == 0) {
            throw new InvalidQueryException("at least one matching edge is required");
        }

        long start = System.nanoTime();
        MatchingResult matching = new BipartiteMatching().maxMatching(graph, leftCount, rightCount);
        long elapsed = System.nanoTime() - start;

        List<Map<String, String>> pairs = new ArrayList<>();
        for (int[] pair : matching.getMatchedPairs()) {
            pairs.add(Map.of("incident", request.incidents()[pair[0]],
                    "resource", request.resources()[pair[1]]));
        }
        Map<String, Object> result = Map.of("matchingSize", matching.getMatchingSize(),
                "pairs", pairs, "incidents", request.incidents().length,
                "resources", request.resources().length);
        long memory = 16L * graph.edgeCount();
        return Results.measured(type(), algorithm(), graph.edgeCount(), start, result, edgeNames,
                memory, "O(V·E) via Dinic on the split network", "O(V + E)",
                "unit capacities: max matching equals max flow across the split");
    }
}