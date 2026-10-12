package com.loginsight.dto.request;

/**
 * Body of the Levenshtein/Damerau endpoints (docs/12 §4). {@code showMatrix} exposes the DP table as
 * {@code intermediateData}; otherwise the matrix is suppressed from the wire payload.
 */
public record DistanceRequest(String a, String b, Boolean showMatrix) {

    public DistanceRequest {
        showMatrix = showMatrix == null ? Boolean.FALSE : showMatrix;
    }
}