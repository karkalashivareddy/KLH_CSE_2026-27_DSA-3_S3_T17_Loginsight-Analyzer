package com.loginsight.dsa.string.suffix;

/**
 * Algorithm: Kasai-longest common prefix array construction.
 * <p>
 * Purpose: from a suffix array produce the LCP array — {@code lcp[i]} = the length of the longest
 * common prefix of the {@code (i-1)}-ranked and {@code i}-ranked suffixes. With two neighboring
 * suffixes sharing k characters, their hn+h next suffixes in SA order share at least k-1, which
 * lets a single O(n) pass reuse the previous comparison instead of restarting per suffix.
 * <p>
 * Input: the original text and a valid suffix array over it (every index in [0, n) exactly once).
 * <p>
 * Output: array of length n with {@code lcp[0] = 0} and, for {@code 1 <= i < n},
 * {@code lcp[i] = LCP(suffixArray[i-1], suffixArray[i])}. The final value can legitimately stay
 * small; the array is filled left-to-right and never requires the naive O(n²) total comparison.
 * <p>
 * Preprocessing: nothing beyond the input; the {rank} inverse array of the suffix array is the only
 * auxiliary structure.
 * <p>
 * Time Complexity: O(n) (amortized; the while loop total is bounded by 2n). Space Complexity: O(n).
 * <p>
 * Important invariant: k never decreases by more than one per step (Kasai's observation), so the
 * number of successful character comparisons across the whole run is linear; failures are O(n).
 * <p>
 * Empty-contract: empty text with an empty suffix array yields an empty LCP array. A null input, or
 * a suffix array whose length does not match the text, is rejected.
 */
public final class KasaiLCP {

    private KasaiLCP() {
    }

    /**
     * @param text        the original text (UTF-16 char model)
     * @param suffixArray a valid suffix array of the text
     * @return {@code lcp} where {@code lcp[0] = 0} and
     *         {@code lcp[i] = LCP(suffixArray[i-1], suffixArray[i])} for i >= 1
     */
    public static int[] build(char[] text, int[] suffixArray) {
        if (text == null || suffixArray == null) {
            throw new IllegalArgumentException("text and suffixArray must not be null");
        }
        int n = text.length;
        if (suffixArray.length != n) {
            throw new IllegalArgumentException("suffixArray length must equal text length");
        }
        validatePermutation(suffixArray);
        if (n == 0) {
            return new int[0];
        }

        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            rank[suffixArray[i]] = i;
        }

        // Internal convention: near[i] = LCP(suffixArray[i], suffixArray[i + 1]).
        int[] near = new int[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (rank[i] == n - 1) {
                k = 0;
                continue;
            }
            int j = suffixArray[rank[i] + 1];
            while (i + k < n && j + k < n && text[i + k] == text[j + k]) {
                k++;
            }
            near[rank[i]] = k;
            if (k > 0) {
                k--;
            }
        }

        // Shift to the documented convention: lcp = [0, LCP(sa0, sa1), LCP(sa1, sa2), ...].
        int[] lcp = new int[n];
        for (int i = 0; i < n - 1; i++) {
            lcp[i + 1] = near[i];
        }
        return lcp;
    }

    private static void validatePermutation(int[] suffixArray) {
        boolean[] seen = new boolean[suffixArray.length];
        for (int value : suffixArray) {
            if (value < 0 || value >= suffixArray.length || seen[value]) {
                throw new IllegalArgumentException("suffixArray must be a permutation of 0..n-1");
            }
            seen[value] = true;
        }
    }
}