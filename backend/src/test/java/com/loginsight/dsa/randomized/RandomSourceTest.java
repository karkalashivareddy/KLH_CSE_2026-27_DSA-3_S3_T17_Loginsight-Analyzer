package com.loginsight.dsa.randomized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RandomSourceTest {

    @Test
    void seededSameSeedProducesSameSequence() {
        RandomSource a = RandomSource.seeded(42L);
        RandomSource b = RandomSource.seeded(42L);
        for (int i = 0; i < 100; i++) {
            assertEquals(a.nextInt(1000), b.nextInt(1000));
        }
    }

    @Test
    void seededDifferentSeedsProduceDifferentSequences() {
        RandomSource a = RandomSource.seeded(1L);
        RandomSource b = RandomSource.seeded(2L);
        boolean differ = false;
        for (int i = 0; i < 10; i++) {
            if (a.nextInt(1000) != b.nextInt(1000)) {
                differ = true;
                break;
            }
        }
        assertTrue(differ, "Different seeds should produce different sequences");
    }

    @Test
    void nextIntRespectsBound() {
        RandomSource rng = RandomSource.seeded(99L);
        for (int i = 0; i < 1000; i++) {
            int v = rng.nextInt(10);
            assertTrue(v >= 0 && v < 10, "nextInt(10) returned " + v);
        }
    }

    @Test
    void nextLongRespectsBound() {
        RandomSource rng = RandomSource.seeded(77L);
        for (int i = 0; i < 1000; i++) {
            long v = rng.nextLong(50);
            assertTrue(v >= 0 && v < 50, "nextLong(50) returned " + v);
        }
    }

    @Test
    void unseededReturnsNonNull() {
        RandomSource rng = RandomSource.unseeded();
        assertNotNull(rng);
        assertFalse(rng.isSeeded());
        assertNull(rng.seed());
    }

    @Test
    void seededReportsSeed() {
        RandomSource rng = RandomSource.seeded(123L);
        assertTrue(rng.isSeeded());
        assertEquals(123L, rng.seed());
    }

    @Test
    void nextIntThrowsOnZeroBound() {
        RandomSource rng = RandomSource.seeded(1L);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> rng.nextInt(0));
    }

    @Test
    void nextIntThrowsOnNegativeBound() {
        RandomSource rng = RandomSource.seeded(1L);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> rng.nextInt(-1));
    }
}
