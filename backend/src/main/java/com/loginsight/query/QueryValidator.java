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
    private static final int MAX_SEQUENCE_LENGTH = 5_000;

    private QueryValidator() {
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
        int p = parallelism == null ? 0 : parallelism;
        if (p < 0) {
            throw error("parallelism must be >= 0 (0 lets the engine choose)");
        }
        return p;
    }

    public static long requireMillerRabin(MillerRabinRequest request) {
        if (request.n() == null || request.n() < 2) {
            throw error("n must be >= 2");
        }
        if (request.rounds() < 1) {
            throw error("rounds must be >= 1");
        }
        return request.n();
    }

    public static void requireReservoir(ReservoirRequest request, long streamSize) {
        if (request.k() == null || request.k() < 1) {
            throw error("k must be >= 1");
        }
        long size = Math.max(request.size(), streamSize);
        if (size > 0 && request.k() > size) {
            throw error("k must not exceed the stream size (" + size + ")");
        }
        if (size == 0) {
            throw error("a stream of size >= 1 is required to sample from");
        }
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