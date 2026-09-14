package com.loginsight.dsa.string;

/**
 * Algorithm: Knuth-Morris-Pratt pattern matching with hand-built LPS (longest proper prefix that is
 * also a suffix) failure function.
 * <p>
 * Purpose: eliminate the re-comparison wasteful in naive search by carrying a "how much of the
 * pattern already matches" state forward after a mismatch, instead of restarting at position+1.
 * <p>
 * Input: text (length n), pattern (length m).
 * <p>
 * Output: every start index of the pattern in the text, including overlapping matches.
 * <p>
 * Preprocessing: the LPS array {@code lps[j]} = length of the longest proper prefix of
 * pattern[0..j] that is also a suffix of pattern[0..j]. Built in one left-to-right pass in O(m).
 * <p>
 * Time Complexity: O(n + m) — text is never walked backwards; on mismatch the pattern state is
 * pulled back via LPS instead of restarting.
 * <p>
 * Space Complexity: O(m) for the LPS array.
 * <p>
 * Important invariant: after the text cursor has passed position i, the pattern state j is exactly
 * the length of the longest prefix of the pattern that is a suffix of text[0..i]; a match at i-j+1
 * is real only when j == m.
 * <p>
 * Empty-contract: empty/null pattern rejected; pattern longer than text yields an empty result.
 */
public final class KMPMatcher implements StringMatcher {

    private static final String ALGORITHM = "KMP";

    @Override
    public StringSearchResult match(String text, String pattern) {
        TextValidator.requireNonNull(text, pattern);
        TextValidator.requireNonEmpty(pattern);
        char[] t = text.toCharArray();
        char[] p = pattern.toCharArray();
        int n = t.length;
        int m = p.length;

        long start = System.nanoTime();
        int[] lps = buildLps(p);
        int[] positions = new int[n];
        int count = 0;
        int i = 0;
        int j = 0;
        while (i < n) {
            if (t[i] == p[j]) {
                i++;
                j++;
            }
            if (j == m) {
                positions[count++] = i - j;
                j = lps[j - 1];
            } else if (i < n && t[i] != p[j]) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        long elapsed = System.nanoTime() - start;

        return new StringSearchResult(ALGORITHM, pattern, n, copy(positions, count), elapsed,
                "O(n + m)", "O(m) LPS", lps);
    }

    /**
     * Build the failure function over the pattern.
     *
     * @param pattern non-empty pattern
     * @return lps where lps[0] = 0 and lps[j] = longest proper prefix of pattern[0..j] that is
     *         also a suffix of pattern[0..j]
     */
    public static int[] buildLps(char[] pattern) {
        int m = pattern.length;
        int[] lps = new int[m];
        int len = 0;
        int i = 1;
        while (i < m) {
            if (pattern[i] == pattern[len]) {
                len++;
                lps[i] = len;
                i++;
            } else if (len != 0) {
                len = lps[len - 1];
            } else {
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }

    private static int[] copy(int[] positions, int size) {
        int[] trimmed = new int[size];
        System.arraycopy(positions, 0, trimmed, 0, size);
        return trimmed;
    }
}