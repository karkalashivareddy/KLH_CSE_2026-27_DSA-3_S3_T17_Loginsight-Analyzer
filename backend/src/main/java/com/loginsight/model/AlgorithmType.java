package com.loginsight.model;

/**
 * Identifies the concrete algorithm behind a query (docs/04 §2). The value appears in every
 * AlgorithmResult envelope so the UI and the acceptance tests can audit which engine ran.
 */
public enum AlgorithmType {

    NAIVE,
    KMP,
    Z,
    RABIN_KARP,
    AHO_CORASICK,
    SUFFIX_ARRAY,
    KASAI_LCP,
    LEVENSHTEIN,
    DAMERAU_LEVENSHTEIN,
    WEIGHTED_EDIT_DISTANCE,
    NEEDLEMAN_WUNSCH,
    SMITH_WATERMAN,
    MATRIX_CHAIN,
    OPTIMAL_BINARY_SEARCH_TREE,
    BITMASK_TSP,
    HAMILTONIAN_PATH,
    TREE_DIAMETER,
    REROOTING_DP,
    SOS_DP,
    FORD_FULKERSON,
    EDMONDS_KARP,
    DINIC,
    MIN_CUT,
    BIPARTITE_MATCHING,
    MIN_COST_MAX_FLOW,
    VERTEX_COVER,
    MAXIMAL_MATCHING,
    SET_COVER,
    MILLER_RABIN,
    RESERVOIR_SAMPLING,
    UNIVERSAL_HASH,
    RANDOMIZED_QUICKSORT,
    PARALLEL_REDUCE,
    PARALLEL_PREFIX_SCAN,
    PARALLEL_SORT,
    BENCHMARK
}