package com.loginsight.dsa.randomized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.trace.TracedResult;

/**
 * Regression guard for the shared argument validation of {@link MillerRabin#test} and
 * {@link MillerRabin#testTracked}.
 *
 * <p>Both entry points used to diverge: {@code test} ignored a null {@code mode} and
 * {@code testTracked} performed no {@code rng}/{@code rounds} validation at all, so a
 * {@code PROBABILISTIC} tracked run with {@code rounds <= 0} built an empty witness array, skipped
 * the witness loop entirely and reported composites as prime. Both now share
 * {@code validateArguments}.</p>
 */
class MillerRabinValidationRegressionTest {

    private final MillerRabin mr = new MillerRabin();

    /** Known primes exercised by the tracked/untracked agreement checks. */
    private static final long[] PRIMES = {
        2L, 3L, 5L, 97L, 7919L, 2_147_483_647L, 9_223_372_036_854_775_783L
    };

    /** Known composites, including Carmichael numbers and strong pseudoprimes. */
    private static final long[] COMPOSITES = {
        9L, 561L, 1105L, 1729L, 341_550_071_728_321L, 3_825_123_056_546_413_051L
    };

    private static boolean tracedVerdict(TracedResult result) {
        Object prime = result.result() instanceof Map<?, ?> map ? map.get("prime") : null;
        assertNotNull(prime, "traced result payload must expose a verdict");
        return (Boolean) prime;
    }

    private static String tracedMode(TracedResult result) {
        Object mode = result.result() instanceof Map<?, ?> map ? map.get("mode") : null;
        return mode == null ? null : String.valueOf(mode);
    }

    // --- a null mode is a rejected argument, never a NullPointerException ---

    @Test
    void testRejectsANullModeWithAnIllegalArgument() {
        RandomSource rng = RandomSource.seeded(7L);
        for (long n : new long[]{0L, 1L, 2L, 3L, 4L, 7L, 97L, 561L}) {
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> mr.test(n, null, rng, 5),
                    "n=" + n + " must be rejected, not silently mislabelled PROBABILISTIC");
            assertEquals("mode must not be null", thrown.getMessage());
        }
    }

    @Test
    void testRejectsANullModeWithoutARandomSource() {
        assertThrows(IllegalArgumentException.class, () -> mr.test(97L, null, null, 5));
    }

    @Test
    void testTrackedRejectsANullModeWithAnIllegalArgument() {
        RandomSource rng = RandomSource.seeded(7L);
        for (long n : new long[]{0L, 1L, 2L, 3L, 4L, 7L, 97L, 561L}) {
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> mr.testTracked(n, null, rng, 5),
                    "n=" + n + " must be rejected before mode.name() is ever read");
            assertEquals("mode must not be null", thrown.getMessage());
        }
    }

    // --- testTracked applies the same rng/rounds validation as test ---

    @Test
    void testTrackedProbabilisticRequiresRng() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> mr.testTracked(7L, MillerRabin.Mode.PROBABILISTIC, null, 5));
        assertEquals("rng must not be null for PROBABILISTIC mode", thrown.getMessage());
    }

    @Test
    void testTrackedProbabilisticRequiresPositiveRounds() {
        // Regression guard: with rounds <= 0 the tracked path used to build an empty base array,
        // never enter the witness loop and return a "prime" verdict for the composite 561.
        for (int rounds : new int[]{0, -1, Integer.MIN_VALUE}) {
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> mr.testTracked(561L, MillerRabin.Mode.PROBABILISTIC,
                            RandomSource.seeded(1L), rounds));
            assertTrue(thrown.getMessage().startsWith("rounds must be > 0"),
                    "unexpected message: " + thrown.getMessage());
        }
    }

    @Test
    void testTrackedRejectsANegativeCandidate() {
        assertThrows(IllegalArgumentException.class,
                () -> mr.testTracked(-1L, MillerRabin.Mode.DETERMINISTIC, null, 0));
    }

    // --- regression guard: a validated tracked run never reports a composite as prime ---

    @Test
    void trackedProbabilisticAgreesWithTestOnKnownPrimes() {
        for (long p : PRIMES) {
            boolean expected = mr.test(p, MillerRabin.Mode.PROBABILISTIC,
                    RandomSource.seeded(11L), 20).isPrime();
            TracedResult traced = mr.testTracked(p, MillerRabin.Mode.PROBABILISTIC,
                    RandomSource.seeded(11L), 20);
            assertTrue(expected, "p=" + p + " must be a probable prime");
            assertEquals(expected, tracedVerdict(traced), "tracked/untracked disagree for p=" + p);
            assertEquals("PROBABILISTIC", tracedMode(traced));
        }
    }

    @Test
    void trackedProbabilisticAgreesWithTestOnKnownComposites() {
        for (long c : COMPOSITES) {
            boolean expected = mr.test(c, MillerRabin.Mode.PROBABILISTIC,
                    RandomSource.seeded(11L), 20).isPrime();
            TracedResult traced = mr.testTracked(c, MillerRabin.Mode.PROBABILISTIC,
                    RandomSource.seeded(11L), 20);
            assertFalse(expected, "c=" + c + " must be composite");
            assertFalse(tracedVerdict(traced),
                    "tracked run reported composite " + c + " as prime (empty witness loop)");
            assertEquals(expected, tracedVerdict(traced), "tracked/untracked disagree for c=" + c);
        }
    }

    @Test
    void trackedProbabilisticRunsTheWitnessLoopForEveryRound() {
        // A composite that a single base catches: the traced verdict must not depend on how many
        // rounds were requested, and must never flip to "prime" because the loop was skipped.
        TracedResult oneRound = mr.testTracked(561L, MillerRabin.Mode.PROBABILISTIC,
                RandomSource.seeded(3L), 1);
        TracedResult manyRounds = mr.testTracked(561L, MillerRabin.Mode.PROBABILISTIC,
                RandomSource.seeded(3L), 25);
        assertFalse(tracedVerdict(oneRound));
        assertFalse(tracedVerdict(manyRounds));
        assertFalse(oneRound.steps().isEmpty(), "the decomposition and witness steps are recorded");
    }

    // --- the deterministic path is unchanged ---

    @Test
    void trackedDeterministicAgreesWithIsDefinitePrime() {
        long[] values = {0L, 1L, 2L, 3L, 4L, 17L, 561L, 2_147_483_647L,
                9_223_372_036_854_775_783L};
        for (long n : values) {
            TracedResult traced = mr.testTracked(n, MillerRabin.Mode.DETERMINISTIC, null, 0);
            assertEquals(mr.isDefinitePrime(n), tracedVerdict(traced), "n=" + n);
            assertEquals("DETERMINISTIC", tracedMode(traced));
        }
    }

    @Test
    void trackedDeterministicIgnoresRngAndRounds() {
        TracedResult traced = mr.testTracked(17L, MillerRabin.Mode.DETERMINISTIC,
                RandomSource.seeded(5L), 0);
        assertTrue(tracedVerdict(traced));
        assertEquals("DETERMINISTIC", tracedMode(traced));
    }
}