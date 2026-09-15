package com.loginsight.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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