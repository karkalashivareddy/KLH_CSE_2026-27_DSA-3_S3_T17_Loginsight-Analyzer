package com.loginsight.model;

/**
 * Classification of an algorithmic query (docs/02 §4, docs/04 §2). Designed so the query engine can
 * route a request to the correct strategy upfront and so the UI can label every envelope.
 */
public enum QueryType {

    PATTERN_SEARCH,
    FUZZY_SEARCH,
    MULTI_PATTERN_SEARCH,
    DOCUMENT_SIMILARITY,
    SEQUENCE_ALIGNMENT,
    SUFFIX_ANALYSIS,
    SERVICE_FLOW,
    MIN_CUT,
    HASH,
    APPROXIMATE_COVER,
    PRIMALITY_TEST,
    STREAM_SAMPLE,
    BENCHMARK,
    EDIT_DISTANCE,
    GLOBAL_ALIGNMENT,
    LOCAL_ALIGNMENT,
    INTERVAL_DP,
    BITMASK_DP,
    TREE_DP,
    SOS_DP,
    MATCHING,
    MIN_COST_FLOW,
    SET_COVER,
    RANDOMIZED_SORT,
    PARALLEL_REDUCE,
    PARALLEL_SCAN,
    PARALLEL_SORT
}