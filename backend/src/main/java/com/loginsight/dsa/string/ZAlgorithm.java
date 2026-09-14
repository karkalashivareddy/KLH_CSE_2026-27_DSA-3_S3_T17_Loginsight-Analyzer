package com.loginsight.dsa.string;

/**
 * Algorithm: Z-function based pattern matching.
 * <p>
 * Purpose: compute, for every position of a search working string, the longest prefix of the whole
 * string that starts there (the Z-array), then read off matches where the Z value equals the
 * pattern length.
 * <p>
 * Input: text (length n), pattern (length m). Search is performed on the concatenation
 * {@code pattern + separator + text}.
 * <p>
 * Output: every start index of the pattern in the text, including overlapping matches.
 * <p>
 * Preprocessing: the Z-array uses the standard {@code [l, r]} window: when position i lies inside
 * the currently known rightmost matching window, {@code z[i] = min(r - i + 1, z[i - l])} and only
 * the single remaining comparison extension may grow beyond it. Without the window the work would
 * be O(n·m); with it the amortized work is linear.
 * <p>
 * Time Complexity: O(n + m). Space Complexity: O(n + m) for the working string and Z-array.
 * <p>
 * Separator safety: a character absent from BOTH pattern and text is chosen (the implementation
 * scans the union of the two inputs). This guarantees Z never "spills" across the pattern/text
 * boundary, so a value of exactly m at a text position is a genuine match.
 * <p>
 * Important invariant: the window [l, r] is kept such that every position inside it reuses an
 * already-computed Z value instead of restarting the comparison, which is why repeated-characters
 * inputs (e.g. {@code aaa...a}) still run in linear time.
 */
public final class ZAlgorithm implements StringMatcher {

    private static final String ALGORITHM = "Z";

    @Override
    public StringSearchResult match(String text, String pattern) {
        TextValidator.requireNonNull(text, pattern);
        TextValidator.requireNonEmpty(pattern);
        char[] t = text.toCharArray();
        char[] p = pattern.toCharArray();
        int n = t.length;
        int m = p.length;

        long start = System.nanoTime();
        char separator = findSeparator(p, t);
        char[] work = concat(p, separator, t);
        int[] z = buildZArray(work);

        int[] positions = new int[n];
        int count = 0;
        for (int i = m + 1; i < work.length; i++) {
            if (z[i] == m) {
                positions[count++] = i - (m + 1);
            }
        }
        long elapsed = System.nanoTime() - start;

        return new StringSearchResult(ALGORITHM, pattern, n, copy(positions, count), elapsed,
                "O(n + m)", "O(n + m)", z);
    }

    /**
     * Z-array over a working string: {@code z[i]} = length of the longest common prefix of the
     * working string with the suffix starting at i. z[0] = length of the string by the standard
     * definition (unused by the search loop).
     */
    public static int[] buildZArray(char[] s) {
        int len = s.length;
        int[] z = new int[len];
        if (len == 0) {
            return z;
        }
        z[0] = len;
        int l = 0;
        int r = 0;
        for (int i = 1; i < len; i++) {
            if (i <= r) {
                z[i] = Math.min(r - i + 1, z[i - l]);
            }
            while (i + z[i] < len && s[z[i]] == s[i + z[i]]) {
                z[i]++;
            }
            if (i + z[i] - 1 > r) {
                l = i;
                r = i + z[i] - 1;
            }
        }
        return z;
    }

    /** Pick a char that appears in neither the pattern nor the text (guaranteed to exist). */
    static char findSeparator(char[] pattern, char[] text) {
        boolean[] present = new boolean[Character.MAX_VALUE + 1];
        mark(present, pattern);
        mark(present, text);
        for (int c = 1; c <= Character.MAX_VALUE; c++) {
            if (!present[c]) {
                return (char) c;
            }
        }
        throw new IllegalStateException("input covers every char; cannot construct a separator");
    }

    private static void mark(boolean[] present, char[] chars) {
        for (char c : chars) {
            present[c] = true;
        }
    }

    private static char[] concat(char[] a, char separator, char[] b) {
        char[] out = new char[a.length + 1 + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        out[a.length] = separator;
        System.arraycopy(b, 0, out, a.length + 1, b.length);
        return out;
    }

    private static int[] copy(int[] positions, int size) {
        int[] trimmed = new int[size];
        System.arraycopy(positions, 0, trimmed, 0, size);
        return trimmed;
    }
}