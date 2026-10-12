package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/dp/weighted-edit} (docs/12 §4). All three costs default to {@code 1},
 * which degrades the algorithm to the plain Levenshtein metric.
 */
public record WeightedEditRequest(String a, String b, Integer insertCost, Integer deleteCost,
                                  Integer substituteCost) {

    public WeightedEditRequest {
        insertCost = insertCost == null ? 1 : insertCost;
        deleteCost = deleteCost == null ? 1 : deleteCost;
        substituteCost = substituteCost == null ? 1 : substituteCost;
    }
}