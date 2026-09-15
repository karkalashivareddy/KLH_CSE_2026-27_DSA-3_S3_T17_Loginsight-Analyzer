package com.loginsight.dsa.randomized;

/**
 * Transversal evidence wrapper for randomised algorithms.
 *
 * <p>Records the algorithm name, a human-readable summary, and an optional explanatory
 * note.  Used by {@link RandomizedHash} (and potentially other randomised algorithms) to
 * carry a uniform, unmodifiable evidence object across the codebase.</p>
 */
public final class RandomizedResult {

    private final String algorithm;
    private final String summary;
    private final String note;

    private RandomizedResult(String algorithm, String summary, String note) {
        this.algorithm = algorithm;
        this.summary = summary;
        this.note = note;
    }

    /**
     * @param algorithm short algorithm identifier (e.g. {@code "UniversalHashing"})
     * @param summary   human-readable evidence summary
     * @return a new result
     */
    public static RandomizedResult of(String algorithm, String summary) {
        return new RandomizedResult(algorithm, summary, "");
    }

    /**
     * @param algorithm short algorithm identifier
     * @param summary   human-readable evidence summary
     * @param note      optional explanatory note (guarantee, caveat, or complexity hint)
     * @return a new result
     */
    public static RandomizedResult of(String algorithm, String summary, String note) {
        return new RandomizedResult(algorithm, summary, note);
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getSummary() {
        return summary;
    }

    public String getNote() {
        return note;
    }

    @Override
    public String toString() {
        return "RandomizedResult{algorithm='" + algorithm + "', summary='" + summary + "'}";
    }
}
