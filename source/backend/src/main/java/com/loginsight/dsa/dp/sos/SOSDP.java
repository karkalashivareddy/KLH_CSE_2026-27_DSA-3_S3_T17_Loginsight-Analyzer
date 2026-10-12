package com.loginsight.dsa.dp.sos;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.DpValidator;

/**
 * Algorithm: Sum Over Subsets (SOS) DP / subset-sum zeta transform.
 * <p>
 * Purpose: for every bitmask, aggregate a value over <em>all</em> of its submasks at once — used for
 * subset-based service/incident category roll-ups (Playground).
 * <p>
 * Input: {@code values[0 .. 2^n - 1]} and a bit count {@code n}.
 * <p>
 * Output: {@code F[mask] = sum of values[sub] over all sub with (sub & mask) == sub}.
 * <p>
 * State: {@code F[mask]} after processing bit {@code i} equals the sum of {@code values[sub]} over all
 * submasks {@code sub} of {@code mask} that may differ from {@code mask} only in bits
 * {@code 0..i}. Initially {@code F = values} (no bits freed).
 * <p>
 * Base case: {@code F = values.clone()} — before any bit is relaxed, only {@code sub == mask} counts.
 * <p>
 * Recurrence / transition (the standard SOS sweep):
 * <pre>
 *   for each bit i in 0..n-1:
 *       for each mask with bit i set:
 *           F[mask] += F[mask ^ (1 << i)]     // add subsets that drop bit i
 * </pre>
 * <p>
 * After all {@code n} bits, {@code F[mask]} aggregates every submask.
 * <p>
 * Final answer: the array {@code F}.
 * <p>
 * Why subproblems overlap: {@code F[mask ^ (1<<i)]} is reused by every supermask of {@code mask} that
 * sets bit {@code i}, so enumerating all submasks independently for each mask is wasteful; the sweep
 * shares the partial sums.
 * <p>
 * Time Complexity: O(n · 2^n). Space Complexity: O(2^n).
 * <p>
 * Overflow safety: the table is {@code long}; callers with large {@code values} should keep the
 * aggregate within {@code long} (documented, not silently truncated).
 * <p>
 * Size cap: {@link #MAX_BITS} bounds the {@code 2^n} table.
 */
public final class SOSDP {

    /** Largest supported bit count for the {@code 2^n} table. */
    public static final int MAX_BITS = 22;
    private static final String ALGORITHM = "SOS DP";

    /** {@code F[mask]} = sum of {@code values[sub]} over all submasks {@code sub} of {@code mask}. */
    public long[] subsetSums(long[] values, int bits) {
        DpValidator.requireNonNull((Object) values);
        DpValidator.requireInRange(bits, 0, MAX_BITS, "bits");
        int size = 1 << bits;
        if (values.length != size) {
            throw new IllegalArgumentException("values length " + values.length
                    + " does not match 2^" + bits + " = " + size);
        }
        long[] f = values.clone();
        for (int i = 0; i < bits; i++) {
            int bit = 1 << i;
            for (int mask = 0; mask < size; mask++) {
                if ((mask & bit) != 0) {
                    f[mask] += f[mask ^ bit];
                }
            }
        }
        return f;
    }

    /** Envelope view; {@code result} = sum of the whole table, {@code intermediateData} = the table. */
    public DpResult solve(long[] values, int bits) {
        long start = System.nanoTime();
        long[] f = subsetSums(values, bits);
        long elapsed = System.nanoTime() - start;
        long total = 0;
        for (long value : f) {
            total += value;
        }
        return new DpResult(ALGORITHM, bits, total, elapsed,
                "O(n * 2^n)", "O(2^n)", f);
    }
}
