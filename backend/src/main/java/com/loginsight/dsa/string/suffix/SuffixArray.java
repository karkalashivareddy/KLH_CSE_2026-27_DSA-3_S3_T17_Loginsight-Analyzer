package com.loginsight.dsa.string.suffix;

import com.loginsight.dsa.string.StringSearchResult;

/**
 * Algorithm: suffix array construction by prefix doubling with a hand-written counting (radix)
 * sort.
 * <p>
 * Purpose: produce, for every suffix of the text, its rank in full lexicographic order as an int
 * array of suffix starting positions. A suffix array is the backbone used by the later substring
 * query layer (find every occurrence of a phrase in the log line space).
 * <p>
 * Input: a single text string (length n).
 * <p>
 * Output: {@code sa} where {@code sa[0]} is the start of the lexicographically smallest suffix and
 * {@code sa[n-1]} the largest; positions are duplicated-free and cover {@code [0, n)}.
 * <p>
 * Preprocessing: a unique sentinel U+0000 smaller than every other Java {@code char} is appended to
 * the text, so sorting cyclic shifts of {@code text + U+0000} coincides with sorting suffixes of the
 * text. First round: counting sort on the leading character with compressed equivalence classes,
 * then double the compared length each round (k = 1, 2, 4, ...) — sort suffixes on the pair
 * {@code (rank of [i..i+k), rank of [i+k..i+2k))}: subtract k from each index for the second
 * component, counting sort on the first. Pair classes are recomputed each round; the sentinel entry
 * is dropped at the end.
 * <p>
 * Time Complexity: O(n log n). Space Complexity: O(n).
 * <p>
 * Important invariant: after round k the string is divided into groups of suffixes sharing the same
 * 2k-length prefix; once classes == n each suffix is alone (fully sorted). Sorting never delegates
 * to {@code Arrays.sort}, suffix-string sorting, or any library sort (docs/02 §8).
 * <p>
 * Empty-contract: empty text yields an empty suffix array; {@link #search(String)} rejects an empty
 * pattern but otherwise answers by binary search over the array in O(m log n).
 */
public final class SuffixArray {

    private final String text;
    private final int[] suffixArray;

    private SuffixArray(String text, int[] suffixArray) {
        this.text = text;
        this.suffixArray = suffixArray;
    }

    /**
     * Build the suffix array of {@code text} (UTF-16 code-unit model: a truncated-char suffix of a
     * surrogate pair sorts on the surrogate units — documented so the behavior stays predictable).
     */
    public static SuffixArray build(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        char[] original = text.toCharArray();
        int n = original.length;
        if (n == 0) {
            return new SuffixArray(text, new int[0]);
        }

        // Append a unique sentinel ('\0', smaller than every Java char that can occur in the
        // text) so sorting the cyclic shifts of text+'$' equals sorting its suffixes. The sentinel
        // is later stripped from the final array.
        String withSentinel = text + '\0';
        char[] s = withSentinel.toCharArray();
        int N = n + 1;

        int[] p = new int[N];
        int[] c = new int[N];
        initialSort(s, p, c);
        int classes = classesCount(c);

        int[] pn = new int[N];
        int[] cn = new int[N];
        for (int h = 0; (1 << h) < N; h++) {
            int step = 1 << h;
            for (int i = 0; i < N; i++) {
                pn[i] = p[i] - step;
                if (pn[i] < 0) {
                    pn[i] += N;
                }
            }
            countingSortByRank(pn, c, classes, p);
            cn[p[0]] = 0;
            classes = 1;
            for (int i = 1; i < N; i++) {
                int cur1 = c[p[i]];
                int cur2 = c[(p[i] + step) % N];
                int prev1 = c[p[i - 1]];
                int prev2 = c[(p[i - 1] + step) % N];
                if (cur1 != prev1 || cur2 != prev2) {
                    classes++;
                }
                cn[p[i]] = classes - 1;
            }
            int[] swap = c;
            c = cn;
            cn = swap;
            if (classes == N) {
                break;
            }
        }

        int[] suffixArray = new int[n];
        int written = 0;
        for (int i = 0; i < N; i++) {
            if (p[i] != n) {
                suffixArray[written++] = p[i];
            }
        }
        return new SuffixArray(text, suffixArray);
    }

    /** First round: counting sort of all suffixes on their leading character, then classes. */
    private static void initialSort(char[] s, int[] p, int[] c) {
        int n = s.length;
        int maxChar = 0;
        for (char ch : s) {
            if (ch > maxChar) {
                maxChar = ch;
            }
        }
        int radix = Math.max(maxChar + 1, n);
        int[] cnt = new int[radix];
        for (char ch : s) {
            cnt[ch]++;
        }
        for (int i = 1; i < radix; i++) {
            cnt[i] += cnt[i - 1];
        }
        for (int i = n - 1; i >= 0; i--) {
            p[--cnt[s[i]]] = i;
        }
        c[p[0]] = 0;
        int classes = 1;
        for (int i = 1; i < n; i++) {
            if (s[p[i]] != s[p[i - 1]]) {
                classes++;
            }
            c[p[i]] = classes - 1;
        }
    }

    private static int classesCount(int[] c) {
        int max = 0;
        for (int value : c) {
            if (value > max) {
                max = value;
            }
        }
        return max + 1;
    }

    /** Counting sort {@code pn} into {@code p} by the class (first component) of each element. */
    private static void countingSortByRank(int[] pn, int[] c, int classes, int[] p) {
        int[] cnt = new int[classes];
        for (int i = 0; i < pn.length; i++) {
            cnt[c[pn[i]]]++;
        }
        for (int i = 1; i < classes; i++) {
            cnt[i] += cnt[i - 1];
        }
        for (int i = pn.length - 1; i >= 0; i--) {
            p[--cnt[c[pn[i]]]] = pn[i];
        }
    }

    /**
     * Find every start position of {@code pattern} using a stable binary search over the suffix
     * array (lower and upper bound), i.e. O(m log n) hand-written character comparisons.
     */
    public int[] search(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("pattern must not be null or empty");
        }
        char[] s = text.toCharArray();
        int n = s.length;
        int m = pattern.length();
        if (m > n) {
            return new int[0];
        }
        int lo = 0;
        int hi = n;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (compareToSuffix(pattern, s, suffixArray[mid]) <= 0) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        int lower = lo;
        hi = n;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (compareToSuffix(pattern, s, suffixArray[mid]) < 0) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        int upper = lo;
        int[] result = new int[upper - lower];
        for (int i = 0; i < result.length; i++) {
            result[i] = suffixArray[lower + i];
        }
        return result;
    }

    /** Strip every found match into the uniform result shape (used by the later query layer). */
    public StringSearchResult match(String pattern) {
        long start = System.nanoTime();
        int[] positions = search(pattern);
        long elapsed = System.nanoTime() - start;
        return new StringSearchResult("SuffixArray", pattern, text.length(), positions, elapsed,
                "O(m log n) search", "O(n) build", null);
    }

    private static int compareToSuffix(String pattern, char[] s, int suffixStart) {
        int m = pattern.length();
        int remaining = s.length - suffixStart;
        int limit = Math.min(m, remaining);
        for (int i = 0; i < limit; i++) {
            char a = pattern.charAt(i);
            char b = s[suffixStart + i];
            if (a != b) {
                return a - b;
            }
        }
        // Every pattern character was consumed: the pattern is a prefix of the suffix (or equal),
        // which for substring search counts as an exact "starts with" match regardless of length.
        return m <= remaining ? 0 : 1;
    }

    /** The raw suffix array (ints = suffix starting positions in lexicographic order). */
    public int[] getSuffixArray() {
        return suffixArray;
    }

    public int length() {
        return text.length();
    }

    /** Lexicographically i-th suffix of the text, for verification/reference purposes. */
    public String suffixAt(int i) {
        return text.substring(suffixArray[i]);
    }
}