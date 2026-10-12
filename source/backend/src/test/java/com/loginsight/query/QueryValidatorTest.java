package com.loginsight.query;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.loginsight.dto.request.MillerRabinRequest;
import com.loginsight.dto.request.ReservoirRequest;
import com.loginsight.exception.InvalidQueryException;

/**
 * Unit tests of the central validation gate (docs/12 §12): every rule message asserted by the
 * acceptance tests is produced here.
 */
class QueryValidatorTest {

    @Test
    void blankPatternRejected() {
        InvalidQueryException e = assertThrows(InvalidQueryException.class,
                () -> QueryValidator.requirePattern("   "));
        assertEquals("pattern must not be blank", e.getMessage());
    }

    @Test
    void nullTextRejected() {
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireText(null));
    }

    @Test
    void oversizedTextRejected() {
        assertThrows(InvalidQueryException.class,
                () -> QueryValidator.requireText("x".repeat(2_000_001)));
    }

    @Test
    void sizesOutsideBoundsRejected() {
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireSizes(0));
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireSizes(10_000_001));
    }

    @Test
    void negativeParallelismRejected() {
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireParallelism(-1));
    }

    @Test
    void parallelismIsClampedToAMachineRelativeCap() {
        int cap = QueryValidator.maxParallelism();
        assertTrue(cap >= 2 && cap <= QueryValidator.MAX_PARALLELISM);
        assertEquals(cap, QueryValidator.resolveParallelism(1_000_000));
        assertEquals(4, QueryValidator.resolveParallelism(4));
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors()),
                QueryValidator.resolveParallelism(0));
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors()),
                QueryValidator.resolveParallelism(null));
    }

    @Test
    void quadraticSequencesAreBoundedByLengthAndCells() {
        QueryValidator.requireQuadraticSequences("kitten", "sitting");
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireQuadraticSequences(
                "x".repeat(QueryValidator.MAX_QUADRATIC_SEQUENCE + 1), "y"));
        assertThrows(InvalidQueryException.class,
                () -> QueryValidator.requireQuadraticSequences(4_000, 7_000));
    }

    @Test
    void fuzzyBudgetRejectsAnOversizedSweep() {
        QueryValidator.requireFuzzyBudget(1_000, 20);
        assertThrows(InvalidQueryException.class,
                () -> QueryValidator.requireFuzzyBudget(10_000_000, 20));
    }

    @Test
    void patternSetIsBoundedByCountAndTotalLength() {
        QueryValidator.requirePatternSet(new String[]{"error", "timeout"});
        String[] tooMany = new String[QueryValidator.MAX_PATTERNS + 1];
        java.util.Arrays.fill(tooMany, "x");
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requirePatternSet(tooMany));
        StringBuilder long1 = new StringBuilder();
        for (int i = 0; i <= QueryValidator.MAX_TOTAL_PATTERN_LENGTH; i++) {
            long1.append('x');
        }
        assertThrows(InvalidQueryException.class,
                () -> QueryValidator.requirePatternSet(new String[]{long1.toString()}));
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requirePatternSet(
                new String[]{"  "}));
    }

    @Test
    void millerRabinRejectsTooManyRounds() {
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireMillerRabin(
                new MillerRabinRequest(17L, QueryValidator.MAX_MILLER_RABIN_ROUNDS + 1)));
    }

    @Test
    void reservoirRejectsOversizedStreamAndCapacity() {
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireReservoir(
                new ReservoirRequest(2, QueryValidator.MAX_RESERVOIR_STREAM + 1, new long[0]), 0));
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireReservoir(
                new ReservoirRequest(QueryValidator.MAX_RESERVOIR_K + 1, 0, new long[0]), 0));
    }

    @Test
    void sweepIsBoundedByCountAndSize() {
        assertArrayEquals(new int[]{20, 10}, QueryValidator.requireSweep(new int[]{20, 10}),
                "the sweep is returned as given; the benchmark orders it afterwards");
        int[] tooMany = new int[QueryValidator.MAX_SWEEP_SIZES + 1];
        java.util.Arrays.fill(tooMany, 10);
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireSweep(tooMany));
        assertThrows(InvalidQueryException.class,
                () -> QueryValidator.requireSweep(new int[]{QueryValidator.MAX_BENCHMARK_SIZE + 1}));
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireSweep(new int[0]));
    }

    @Test
    void millerRabinRejectsSmallN() {
        assertThrows(InvalidQueryException.class,
                () -> QueryValidator.requireMillerRabin(new MillerRabinRequest(1L, 20)));
    }

    @Test
    void millerRabinRejectsZeroRounds() {
        assertThrows(InvalidQueryException.class,
                () -> QueryValidator.requireMillerRabin(new MillerRabinRequest(17L, 0)));
    }

    @Test
    void reservoirRejectsZeroK() {
        assertThrows(InvalidQueryException.class,
                () -> QueryValidator.requireReservoir(new ReservoirRequest(0, 16, new long[0]), 16));
    }

    @Test
    void reservoirRequiresStream() {
        assertThrows(InvalidQueryException.class,
                () -> QueryValidator.requireReservoir(new ReservoirRequest(5, 0, new long[0]), 0));
    }

    @Test
    void kBeyondInputSizeRejected() {
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireK(4, 3));
    }

    @Test
    void nonNegativeGuard() {
        assertThrows(InvalidQueryException.class, () -> QueryValidator.requireNonNegative(-1, "cost"));
        QueryValidator.requireNonNegative(0, "cost");
    }
}