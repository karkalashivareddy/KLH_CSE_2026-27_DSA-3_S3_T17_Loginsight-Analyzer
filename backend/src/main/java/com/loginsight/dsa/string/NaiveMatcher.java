package com.loginsight.dsa.string;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.loginsight.trace.StepRecorder;
import com.loginsight.trace.TracedResult;

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

    /**
     * Trace-capable search path used by the Algorithm Laboratory. Runs the identical start-position
     * loop while recording every character comparison, alignment failure and match.
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
        int[] positions = new int[n - m + 1 > 0 ? n - m + 1 : 0];
        int count = 0;
        int comparisons = 0;
        for (int i = 0; i <= n - m; i++) {
            boolean matched = true;
            for (int j = 0; j < m; j++) {
                comparisons++;
                boolean equal = t[i + j] == p[j];
                recorder.record("COMPARE", "Compare text[" + (i + j) + "]='" + t[i + j]
                        + "' with pattern[" + j + "]='" + p[j] + "'",
                        StepRecorder.state("phase", "search", "i", i, "j", j, "textC", t[i + j],
                                "patternC", p[j], "equal", equal, "matchCount", count),
                        List.of(i + j, j), Map.of("comparisons", comparisons));
                if (!equal) {
                    matched = false;
                    recorder.record("MISMATCH", "Mismatch at alignment " + i + " char " + j
                            + "; restart from position " + (i + 1),
                            StepRecorder.state("phase", "search", "i", i, "j", j, "textC",
                                    t[i + j], "patternC", p[j]),
                            List.of(i + j), Map.of("comparisons", comparisons));
                    break;
                }
            }
            if (matched) {
                positions[count++] = i;
                recorder.record("MATCH", "Full match found at offset " + i
                        + " after comparing all " + m + " characters from scratch.",
                        StepRecorder.state("phase", "search", "i", i, "j", m, "matchCount", count,
                                "position", i),
                        List.of(i), Map.of("comparisons", comparisons));
            }
        }
        long elapsed = System.nanoTime() - start;
        int[] result = trim(positions, count);
        return new TracedResult("Naive", Map.of("matchCount", count, "positions", boxed(result)),
                Map.of("steps", recorder.collect(), "truncated", recorder.isTruncated()),
                recorder.collect(), elapsed, "O(n * m)", "O(1) auxiliary");
    }

    private static List<Integer> boxed(int[] values) {
        List<Integer> out = new ArrayList<>(values.length);
        for (int v : values) {
            out.add(v);
        }
        return out;
    }
}