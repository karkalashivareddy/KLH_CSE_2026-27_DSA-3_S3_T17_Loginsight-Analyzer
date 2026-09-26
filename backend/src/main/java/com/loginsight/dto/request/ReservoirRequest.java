package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/random/sample} (docs/12 §5). Samples exactly {@code k} values from a
 * stream of {@code size} tokens (dataset-derived) or from the explicit {@code values} array when
 * non-empty. Validation rejects {@code k <= 0}.
 */
public record ReservoirRequest(Integer k, Integer size, long[] values) {

    public ReservoirRequest {
        k = k == null ? 10 : k;
        size = size == null ? 0 : size;
        if (values != null && values.length == 0) {
            values = null;
        }
    }
}
