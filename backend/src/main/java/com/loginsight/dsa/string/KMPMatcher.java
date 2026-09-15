package com.loginsight.dsa.string;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.loginsight.trace.AlgorithmStep;
import com.loginsight.trace.StepRecorder;
import com.loginsight.trace.TracedResult;

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

    /**
     * Trace-capable search path used by the Algorithm Laboratory. Runs the identical LPS-build and
     * search loops while recording one {@link AlgorithmStep} per observable operation (LPS cell,
     * comparison, fallback, match). The result equals the untraced variant for the same input.
     *
     * @return traced result whose {@code steps} can drive step-by-step playback
     */
    public TracedResult matchTracked(String text, String pattern) {
        TextValidator.requireNonNull(text, pattern);
        TextValidator.requireNonEmpty(pattern);
        char[] t = text.toCharArray();
        char[] p = pattern.toCharArray();
        int n = t.length;
        int m = p.length;
        StepRecorder recorder = new StepRecorder();
        long start = System.nanoTime();

        int[] lps = buildLpsTracked(p, recorder);

        int[] positions = new int[n];
        int count = 0;
        int i = 0;
        int j = 0;
        int comparisons = 0;
        int fallbacks = 0;
        while (i < n) {
            char textChar = t[i];
            char patternChar = p[j];
            comparisons++;
            if (textChar == patternChar) {
                recorder.record("COMPARE", "Text '" + textChar + "' at index " + i
                        + " equals pattern '" + patternChar + "' at index " + j + "; advance both.",
                        StepRecorder.state("phase", "search", "i", i, "j", j, "textC", textChar,
                                "patternC", patternChar, "lps", Arrays.copyOf(lps, m),
                                "match", textChar == patternChar),
                        List.of(i, j), Map.of("comparisons", comparisons, "fallbacks", fallbacks));
                i++;
                j++;
            }
            if (j == m) {
                positions[count++] = i - j;
                recorder.record("MATCH", "Pattern fully matched ending at index " + (i - 1)
                        + "; record start " + (i - j) + " and fall back to lps[" + (m - 1) + "]="
                        + lps[m - 1] + ".",
                        StepRecorder.state("phase", "search", "i", i, "j", j, "start", i - j,
                                "lps", Arrays.copyOf(lps, m), "matchCount", count, "positions",
                                positionsToString(positions, count)),
                        List.of(i - j), Map.of("comparisons", comparisons, "fallbacks", fallbacks));
                j = lps[j - 1];
            } else if (i < n && t[i] != p[j]) {
                if (j != 0) {
                    fallbacks++;
                    recorder.record("FALLBACK", "Mismatch at text index " + i + "; reuse lps["
                            + (j - 1) + "]=" + lps[j - 1] + " instead of restarting.",
                            StepRecorder.state("phase", "search", "i", i, "j", j, "textC", t[i],
                                    "patternC", p[j], "lps", Arrays.copyOf(lps, m),
                                    "fallbackTo", lps[j - 1]),
                            List.of(j - 1), Map.of("comparisons", comparisons, "fallbacks", fallbacks));
                    j = lps[j - 1];
                } else {
                    comparisons++;
                    recorder.record("COMPARE", "Mismatch with j=0 at text index " + i + "; slide text"
                            + " cursor to " + (i + 1) + " (no prefix to reuse).",
                            StepRecorder.state("phase", "search", "i", i, "j", 0, "textC", t[i],
                                    "patternC", p[0], "lps", Arrays.copyOf(lps, m)),
                            List.of(i), Map.of("comparisons", comparisons, "fallbacks", fallbacks));
                    i++;
                }
            }
        }
        long elapsed = System.nanoTime() - start;
        int[] trimmed = copy(positions, count);
        return new TracedResult("KMP", Map.of("matchCount", count, "positions", toList(trimmed)),
                Map.of("lps", Arrays.copyOf(lps, m), "steps", recorder.collect(), "truncated",
                        recorder.isTruncated(), "phase", "search"),
                recorder.collect(), elapsed, "O(n + m)", "O(m) LPS");
    }

    private static int[] buildLpsTracked(char[] pattern, StepRecorder recorder) {
        int m = pattern.length;
        int[] lps = new int[m];
        recorder.record("LPS_INIT", "Initialise lps[0]=0 (a single character has no proper prefix).",
                StepRecorder.state("phase", "lps", "i", 0, "len", 0, "lps", Arrays.copyOf(lps, m)));
        int len = 0;
        int i = 1;
        while (i < m) {
            if (pattern[i] == pattern[len]) {
                len++;
                lps[i] = len;
                recorder.record("LPS_MATCH", "pattern[" + i + "] = '" + pattern[i]
                        + "' equals pattern[" + (len - 1) + "]; lps[" + i + "]=" + len + ".",
                        StepRecorder.state("phase", "lps", "i", i, "len", len, "lps",
                                Arrays.copyOf(lps, m)));
                i++;
            } else if (len != 0) {
                recorder.record("LPS_FALLBACK", "Mismatch; fall back len to lps[len-1]="
                        + lps[len - 1] + ".",
                        StepRecorder.state("phase", "lps", "i", i, "len", lps[len - 1], "lps",
                                Arrays.copyOf(lps, m)));
                len = lps[len - 1];
            } else {
                lps[i] = 0;
                recorder.record("LPS_ZERO", "No prefix/suffix overlap; lps[" + i + "]=0.",
                        StepRecorder.state("phase", "lps", "i", i, "len", 0, "lps",
                                Arrays.copyOf(lps, m)));
                i++;
            }
        }
        return lps;
    }

    private static String positionsToString(int[] positions, int size) {
        if (size == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int k = 0; k < size; k++) {
            if (k > 0) {
                sb.append(", ");
            }
            sb.append(positions[k]);
        }
        return sb.append(']').toString();
    }

    private static List<Integer> toList(int[] values) {
        List<Integer> out = new ArrayList<>(values.length);
        for (int v : values) {
            out.add(v);
        }
        return out;
    }
}