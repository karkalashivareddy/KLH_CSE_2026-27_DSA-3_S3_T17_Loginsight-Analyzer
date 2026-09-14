package com.loginsight.dsa.string;

/**
 * Algorithm: Naive pattern matching (baseline).
 * <p>
 * Purpose: establish the reference implementation that every other matcher is cross-checked
 * against, and demonstrate why preprocessing matters.
 * <p>
 * Input: text (length n), pattern (length m).
 * <p>
 * Output: every start index i in [0, n-m] where text[i..i+m) equals the pattern, including
 * overlapping matches.
 * <p>
 * Preprocessing: none.
 * <p>
 * Time Complexity: O(n·m) worst case — for every starting position a fresh m-character comparison
 * is performed.
 * <p>
 * Space Complexity: O(1) auxiliary (excluding the result storage).
 * <p>
 * Important invariant: a match is accepted only after the full m characters agree; no prior
 * information about the text is reused between positions, which is precisely what KMP/Z improve.
 * <p>
 * Empty-contract: pattern must be non-empty; an empty or null pattern is rejected. A pattern longer
 * than the text yields an empty result. On UTF-16 {@code char} units, one position = one char.
 */
public final class NaiveMatcher implements StringMatcher {

    private static final String ALGORITHM = "Naive";

    @Override
    public StringSearchResult match(String text, String pattern) {
        TextValidator.requireNonNull(text, pattern);
        TextValidator.requireNonEmpty(pattern);
        char[] t = text.toCharArray();
        char[] p = pattern.toCharArray();
        int n = t.length;
        int m = p.length;

        long start = System.nanoTime();
        int[] positions = new int[n - m + 1 > 0 ? n - m + 1 : 0];
        int count = 0;
        for (int i = 0; i <= n - m; i++) {
            if (matchesAt(t, p, i)) {
                positions[count++] = i;
            }
        }
        long elapsed = System.nanoTime() - start;

        return new StringSearchResult(ALGORITHM, pattern, n, trim(positions, count), elapsed,
                "O(n * m)", "O(1) auxiliary", null);
    }

    private static boolean matchesAt(char[] text, char[] pattern, int offset) {
        for (int j = 0; j < pattern.length; j++) {
            if (text[offset + j] != pattern[j]) {
                return false;
            }
        }
        return true;
    }

    private static int[] trim(int[] positions, int size) {
        if (size == positions.length) {
            return positions;
        }
        int[] trimmed = new int[size];
        System.arraycopy(positions, 0, trimmed, 0, size);
        return trimmed;
    }
}