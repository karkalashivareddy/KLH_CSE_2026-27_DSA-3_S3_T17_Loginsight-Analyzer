package com.loginsight.query.engine.approximation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.approximation.SetCoverDemo;
import com.loginsight.dsa.approximation.SetCoverResult;
import com.loginsight.dto.request.SetCoverRequest;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.Results;

/**
 * Greedy set-cover (docs/12 §7): repeatedly take the set covering the most uncovered universe
 * elements. The selected sets and their coverage are the payload; the harmonic-approximation bound
 * and greedy-step count are intermediate.
 */
public final class SetCoverEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.SET_COVER;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.SET_COVER;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        SetCoverRequest request = (SetCoverRequest) context.getRequest();
        String[] universe = request.universe();
        Map<String, String[]> sets = request.sets();
        if (universe == null || universe.length == 0) {
            throw new InvalidQueryException("universe must not be empty");
        }
        if (sets == null || sets.isEmpty()) {
            throw new InvalidQueryException("sets must not be empty");
        }

        Map<String, Integer> elementIndex = new LinkedHashMap<>();
        for (int i = 0; i < universe.length; i++) {
            if (universe[i] == null) {
                throw new InvalidQueryException("universe must not contain null elements");
            }
            elementIndex.put(universe[i], i);
        }
        int[][] setsById = new int[sets.size()][];
        String[] setNames = sets.keySet().toArray(new String[0]);
        for (int i = 0; i < setNames.length; i++) {
            String[] elements = sets.get(setNames[i]);
            int[] ids = new int[elements.length];
            for (int j = 0; j < elements.length; j++) {
                Integer id = elementIndex.get(elements[j]);
                if (id == null) {
                    throw new InvalidQueryException("set '" + setNames[i]
                            + "' references unknown universe element '" + elements[j] + "'");
                }
                ids[j] = id;
            }
            setsById[i] = ids;
        }

        long start = System.nanoTime();
        SetCoverResult result = new SetCoverDemo().greedyCover(universe.length, setsById);
        long elapsed = System.nanoTime() - start;

        java.util.List<String> selected = new java.util.ArrayList<>();
        for (int index : result.getSelectedSetIndices()) {
            selected.add(setNames[index]);
        }
        Map<String, Object> payload = Map.of("coveredCount", result.getCoveredCount(),
                "universeSize", result.getUniverseSize(), "allCovered", result.isAllCovered(),
                "selectedSets", selected, "selectedSetCount", result.getSelectedSetCount());
        Map<String, Object> intermediate = Map.of("greedySteps", result.getGreedySteps(),
                "harmonicBound", result.getHarmonicBound());
        return Results.measured(type(), algorithm(), universe.length, start, payload, intermediate,
                0L, "O(S·U) per step greedy", "O(U) coverage marks",
                result.getNotes());
    }
}