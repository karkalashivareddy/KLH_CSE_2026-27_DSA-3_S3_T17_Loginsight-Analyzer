package com.loginsight.query;

import java.util.function.Supplier;

import com.loginsight.dto.request.MillerRabinRequest;
import com.loginsight.dto.request.ReservoirRequest;
import com.loginsight.exception.InvalidQueryException;

/**
 * Central validation gate of the query layer (docs/02 §10). Every crossing of the vectoring layer is
 * checked here before dispatch; failures surface as {@link InvalidQueryException} → HTTP 400 with a
 * human-readable message. Rule texts live here so acceptor tests and the docs agree verbatim.
 */
public final class QueryValidator {

    private static final int MAX_TEXT_LENGTH = 2_000_000;
    private static final int MAX_PATTERN_LENGTH = 100_000;

    /** Longest single sequence a quadratic DP (edit distance / alignment) may be handed. */
    public static final int MAX_QUADRATIC_SEQUENCE = 5_000;
    /** Largest DP cell budget (|a| · |b|) a quadratic table may allocate. */
    public static final int MAX_QUADRATIC_CELLS = 25_000_000;
    /** Largest per-line budget (lines · |query|) the fuzzy search may sweep. */
    public static final int MAX_FUZZY_CELLS = 20_000_000;
    /** Largest number of patterns a single Aho-Corasick automaton may hold. */
    public static final int MAX_PATTERNS = 1_024;
    /** Largest total pattern characters (Σ|P|) a single automaton may hold. */
    public static final int MAX_TOTAL_PATTERN_LENGTH = 200_000;
    /** Largest reported occurrence list one Aho-Corasick scan may produce. */
    public static final int MAX_OCCURRENCES = 200_000;
    /** Largest number of Miller-Rabin random bases a request may ask for. */
    public static final int MAX_MILLER_RABIN_ROUNDS = 1_000;
    /** Largest synthetic reservoir stream a request may ask for. */
    public static final int MAX_RESERVOIR_STREAM = 2_000_000;
    /** Largest explicit reservoir stream array a request may carry. */
    public static final int MAX_RESERVOIR_VALUES = 1_000_000;
    /** Largest reservoir capacity (memory is O(k)). */
    public static final int MAX_RESERVOIR_K = 100_000;
    /** Largest number of input sizes one benchmark sweep may carry. */
    public static final int MAX_SWEEP_SIZES = 12;
    /** Largest single input size one benchmark sweep may measure. */
    public static final int MAX_BENCHMARK_SIZE = 2_000_000;
    /** Absolute ceiling on worker threads, whatever the machine reports. */
    public static final int MAX_PARALLELISM = 64;

    private QueryValidator() {
    }

    /**
     * Machine-relative worker cap: four threads per reported core, never more than
     * {@value #MAX_PARALLELISM}. Thread pools are created per call, so an unbounded request
     * parallelism would otherwise let one request exhaust the machine.
     */
    public static int maxParallelism() {
        return Math.max(2, Math.min(MAX_PARALLELISM,
                Runtime.getRuntime().availableProcessors() * 4));
    }

    /**
     * Resolves a requested worker count to a safe one: {@code null} or {@code <= 0} selects the
     * machine's core count, anything larger is clamped to {@link #maxParallelism()}.
     */
    public static int resolveParallelism(Integer requested) {
        int p = requested == null ? 0 : requested;
        if (p < 0) {
            throw error("parallelism must be >= 0 (0 lets the engine choose)");
        }
        if (p == 0) {
            return Math.max(1, Runtime.getRuntime().availableProcessors());
        }
        return Math.min(p, maxParallelism());
    }

    public static String requireNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw error(name + " must not be blank");
        }
        return value;
    }

    public static String requirePattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw error("pattern must not be blank");
        }
        if (pattern.length() > MAX_PATTERN_LENGTH) {
            throw error("pattern is too long (max " + MAX_PATTERN_LENGTH + " characters)");
        }
        return pattern;
    }

    public static String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw error("text must not be empty");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw error("text is too long (max " + MAX_TEXT_LENGTH + " characters)");
        }
        return text;
    }

    public static void requireSizes(int size) {
        requireBounds(1, size, 10_000_000, "size");
    }

    public static void requireSizes(int size, int max) {
        requireBounds(1, size, max, "size");
    }

    public static int requireParallelism(Integer parallelism) {
        return resolveParallelism(parallelism);
    }

    /** Bounds one side and the cell budget of a quadratic DP (edit distance / alignment). */
    public static void requireQuadraticSequences(String a, String b) {
        requireQuadraticSequences(a.length(), b.length());
    }

    /** Length-based form of {@link #requireQuadraticSequences(String, String)}. */
    public static void requireQuadraticSequences(long lenA, long lenB) {
        if (lenA > MAX_QUADRATIC_SEQUENCE || lenB > MAX_QUADRATIC_SEQUENCE) {
            throw error("each sequence must be at most " + MAX_QUADRATIC_SEQUENCE
                    + " entries (this DP fills an O(n·m) table)");
        }
        long cells = lenA * lenB;
        if (cells > MAX_QUADRATIC_CELLS) {
            throw error("the DP table would need " + cells + " cells; the limit is "
                    + MAX_QUADRATIC_CELLS + " (" + lenA + " x " + lenB + ")");
        }
    }

    /** Bounds a per-line edit-distance sweep (fuzzy search) by its total cell budget. */
    public static void requireFuzzyBudget(int lines, int queryLength) {
        long cells = (long) lines * (long) queryLength;
        if (cells > MAX_FUZZY_CELLS) {
            throw error("the fuzzy sweep would need " + cells + " DP cells; the limit is "
                    + MAX_FUZZY_CELLS + " (" + lines + " lines x " + queryLength + " chars)");
        }
    }

    /** Bounds the pattern set of one Aho-Corasick automaton (count and Σ|P|). */
    public static void requirePatternSet(String[] patterns) {
        if (patterns.length > MAX_PATTERNS) {
            throw error("at most " + MAX_PATTERNS + " patterns are supported per automaton, got "
                    + patterns.length);
        }
        long total = 0;
        for (String pattern : patterns) {
            requirePattern(pattern);
            total += pattern.length();
        }
        if (total > MAX_TOTAL_PATTERN_LENGTH) {
            throw error("the total pattern length must be at most " + MAX_TOTAL_PATTERN_LENGTH
                    + " characters, got " + total);
        }
    }

    public static long requireMillerRabin(MillerRabinRequest request) {
        if (request.n() == null || request.n() < 2) {
            throw error("n must be >= 2");
        }
        if (request.rounds() < 1) {
            throw error("rounds must be >= 1");
        }
        if (request.rounds() > MAX_MILLER_RABIN_ROUNDS) {
            throw error("rounds must be at most " + MAX_MILLER_RABIN_ROUNDS);
        }
        return request.n();
    }

    public static void requireReservoir(ReservoirRequest request, long streamSize) {
        if (request.k() == null || request.k() < 1) {
            throw error("k must be >= 1");
        }
        if (request.k() > MAX_RESERVOIR_K) {
            throw error("k must be at most " + MAX_RESERVOIR_K + " (memory is O(k))");
        }
        if (request.size() != null && request.size() > MAX_RESERVOIR_STREAM) {
            throw error("size must be at most " + MAX_RESERVOIR_STREAM);
        }
        long size = Math.max(request.size(), streamSize);
        if (size > MAX_RESERVOIR_STREAM) {
            throw error("the stream must hold at most " + MAX_RESERVOIR_STREAM + " elements, got "
                    + size);
        }
        if (size > 0 && request.k() > size) {
            throw error("k must not exceed the stream size (" + size + ")");
        }
        if (size == 0) {
            throw error("a stream of size >= 1 is required to sample from");
        }
    }

    /** Bounds the number and size of the input sizes one benchmark sweep may measure. */
    public static int[] requireSweep(int[] sizes) {
        if (sizes == null || sizes.length == 0) {
            throw error("sizes must be a non-empty array");
        }
        if (sizes.length > MAX_SWEEP_SIZES) {
            throw error("a benchmark sweep runs at most " + MAX_SWEEP_SIZES + " sizes, got "
                    + sizes.length);
        }
        for (int size : sizes) {
            requireBounds(1, size, MAX_BENCHMARK_SIZE, "size");
        }
        return sizes.clone();
    }

    public static int requireK(int k, int n) {
        if (k < 1 || (n > 0 && k > n)) {
            throw error("k must be between 1 and the input size (" + n + ")");
        }
        return k;
    }

    public static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw error(name + " must be >= 0");
        }
    }

    public static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw error(name + " must be > 0");
        }
    }

    public static void requireBounds(int low, int value, int high, String name) {
        if (value < low || value > high) {
            throw error(name + " must be between " + low + " and " + high + ", got " + value);
        }
    }

    public static <T> T requireState(Supplier<T> supplier) {
        return supplier.get();
    }

    public static boolean requireCoverageTarget(int covered) {
        if (covered < 1 || covered > 10_000) {
            throw error("coverage target must be between 1 and 10000");
        }
        return true;
    }

    private static InvalidQueryException error(String message) {
        return new InvalidQueryException(message);
    }
}