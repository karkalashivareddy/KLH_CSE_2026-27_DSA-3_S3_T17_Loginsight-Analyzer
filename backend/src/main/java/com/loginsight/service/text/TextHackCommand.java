package com.loginsight.service.text;

import com.loginsight.exception.InvalidQueryException;

/**
 * The six TextHack query classes (docs/REBUILD_BASELINE Phase-3). Each class routes to a fixed,
 * course-valid engine; the caller only picks the class and supplies input.
 */
public enum TextHackCommand {

    /** KMP linear-time pattern search over the corpus. */
    PATTERN_SEARCH("Pattern Search",
            "Locate every occurrence of a pattern in the log corpus with a linear-time matcher."),

    /** Bounded Levenshtein fuzzy matching. */
    FUZZY_MATCH("Fuzzy Match",
            "Surface near-duplicate lines whose edit distance to the query stays within a bound."),

    /** Needleman-Wunsch global alignment similarity. */
    DOCUMENT_SIMILARITY("Document Similarity",
            "Align two documents end-to-end and report a real alignment identity ratio."),

    /** Dinic max-flow over the service graph. */
    CITATION_FLOW("Dependency Flow",
            "Measure the maximum supported flow through the dependency graph between two services."),

    /** Vertex-cover 2-approximation over project dependencies. */
    PROJECT_SCHEDULING("Project Scheduling",
            "Choose the smallest set of reviewable items covering every dependency edge "
                    + "(NP-hard; the 2-approximation is guaranteed, the optimum is not)."),

    /** Miller-Rabin probable-primality test. */
    PRIME_TESTING("Prime Testing",
            "Classify a large integer as prime or composite with a Monte-Carlo witness search.");

    private final String label;
    private final String description;

    TextHackCommand(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public static TextHackCommand parse(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidQueryException("queryClass is required");
        }
        try {
            return TextHackCommand.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidQueryException("Unknown queryClass: " + value
                    + " (expected one of PATTERN_SEARCH, FUZZY_MATCH, DOCUMENT_SIMILARITY, "
                    + "CITATION_FLOW, PROJECT_SCHEDULING, PRIME_TESTING)");
        }
    }
}