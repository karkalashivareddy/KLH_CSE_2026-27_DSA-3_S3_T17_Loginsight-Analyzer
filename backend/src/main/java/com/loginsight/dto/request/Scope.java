package com.loginsight.dto.request;

/**
 * Where a search reads its input from (docs/12 §1). {@code DATASET} renders the loaded log dataset
 * into the searchable text; {@code EXPLICIT} uses the {@code text} passed in the request body.
 */
public enum Scope {
    DATASET,
    EXPLICIT
}