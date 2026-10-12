package com.loginsight.dsa.dp;

/**
 * Small shared argument checks for the DP engine. Uses plain checks so invalid input surfaces as
 * {@link IllegalArgumentException} (the documented engine contract) instead of an obscure
 * {@code ArrayIndexOutOfBoundsException} deep inside a DP table.
 *
 * <p>Deliberately not a validation framework: only the handful of checks the DP algorithms need.</p>
 */
public final class DpValidator {

    private DpValidator() {
    }

    /** Rejects any {@code null} argument; reports the first offending index in the message. */
    public static void requireNonNull(Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                throw new IllegalArgumentException("argument " + i + " must not be null");
            }
        }
    }

    public static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0 but was " + value);
        }
    }

    public static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0 but was " + value);
        }
    }

    public static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be > 0 but was " + value);
        }
    }

    public static void requireInRange(int value, int lowInclusive, int highInclusive, String name) {
        if (value < lowInclusive || value > highInclusive) {
            throw new IllegalArgumentException(name + " must be in [" + lowInclusive + ", "
                    + highInclusive + "] but was " + value);
        }
    }
}
