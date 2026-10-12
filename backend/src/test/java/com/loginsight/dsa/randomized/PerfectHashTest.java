package com.loginsight.dsa.randomized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerfectHashTest {

    // --- Membership ---

    @Test
    void containsInsertedKeys() {
        long[] keys = {10, 25, 40, 55, 70};
        PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(42L));
        for (long key : keys) {
            assertTrue(ph.contains(key), key + " should be found");
        }
    }

    @Test
    void doesNotContainAbsentKeys() {
        long[] keys = {10, 25, 40, 55, 70};
        PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(42L));
        assertFalse(ph.contains(100));
        assertFalse(ph.contains(0));
        assertFalse(ph.contains(-1));
    }

    @Test
    void emptyKeys() {
        PerfectHash ph = new PerfectHash(new long[0], RandomSource.seeded(1L));
        assertEquals(0, ph.size());
        assertFalse(ph.contains(1));
    }

    @Test
    void singleKey() {
        PerfectHash ph = new PerfectHash(new long[]{42}, RandomSource.seeded(1L));
        assertTrue(ph.contains(42));
        assertFalse(ph.contains(0));
        assertFalse(ph.contains(43));
    }

    // --- Deterministic same seed ---

    @Test
    void deterministicSameSeed() {
        long[] keys = {100, 200, 300, 400, 500};
        PerfectHash ph1 = new PerfectHash(keys, RandomSource.seeded(42L));
        PerfectHash ph2 = new PerfectHash(keys, RandomSource.seeded(42L));
        for (long key : keys) {
            assertEquals(ph1.contains(key), ph2.contains(key));
        }
    }

    // --- Different seeds produce valid structures ---

    @Test
    void differentSeedsStillValid() {
        long[] keys = {10, 20, 30, 40, 50};
        PerfectHash ph1 = new PerfectHash(keys, RandomSource.seeded(1L));
        PerfectHash ph2 = new PerfectHash(keys, RandomSource.seeded(2L));
        for (long key : keys) {
            assertTrue(ph1.contains(key));
            assertTrue(ph2.contains(key));
        }
    }

    // --- Negative keys ---

    @Test
    void negativeKeysWork() {
        long[] keys = {-100, -50, 0, 50, 100};
        PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(42L));
        for (long key : keys) {
            assertTrue(ph.contains(key), key + " should be found");
        }
    }

    // --- Structural properties ---

    @Test
    void sizeMatchesKeyCount() {
        long[] keys = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(1L));
        assertEquals(10, ph.size());
    }

    @Test
    void totalSecondarySlotsNonNegative() {
        long[] keys = {10, 20, 30};
        PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(1L));
        assertTrue(ph.getTotalSecondarySlots() >= 0);
    }

    @Test
    void maxSecondarySizeNonNegative() {
        long[] keys = {10, 20, 30};
        PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(1L));
        assertTrue(ph.getMaxSecondarySize() >= 0);
    }

    @Test
    void sentinelNotInKeySet() {
        long[] keys = {0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5};
        PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(42L));
        long sentinel = ph.getSentinel();
        for (long key : keys) {
            assertNotEquals(sentinel, key, "Sentinel must not be a key");
        }
    }

    @Test
    void firstLevelProperties() {
        long[] keys = {10, 20, 30};
        PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(1L));
        assertEquals(3, ph.getFirstLevelSize());
        assertTrue(ph.getFirstA() >= 1);
        assertTrue(ph.getFirstB() >= 0);
        assertEquals(UniversalHashFamily.DEFAULT_PRIME, ph.getFirstPrime());
    }

    @Test
    void sentinelIsNeverReportedPresent() {
        // Regression: a non-key query equal to the sentinel used to return true when its
        // hash landed on an empty secondary slot (seed 7 reproduces the false positive).
        long[] keys = {10, 25, 40, 55, 70};
        for (long s = 0; s < 30; s++) {
            PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(s));
            assertFalse(ph.contains(ph.getSentinel()),
                    "seed " + s + ": sentinel " + ph.getSentinel() + " must not be reported present");
        }
    }

    @Test
    void sentinelFallbackMinValueIsNotReportedPresent() {
        // Key set contains every sentinel candidate {0,1,-1,...,5,-5}, forcing the
        // Long.MIN_VALUE fallback; the guard must still reject it.
        long[] keys = {0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5};
        PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(42L));
        assertEquals(Long.MIN_VALUE, ph.getSentinel());
        assertFalse(ph.contains(Long.MIN_VALUE));
    }

    @Test
    void duplicateKeysRejected() {
        long[] keys = {1, 2, 1, 3};
        assertThrows(IllegalArgumentException.class,
                () -> new PerfectHash(keys, RandomSource.seeded(42L)));
    }

    @Test
    void duplicateKeysRejectedSingleKeyRepeated() {
        assertThrows(IllegalArgumentException.class,
                () -> new PerfectHash(new long[]{7, 7, 7}, RandomSource.seeded(1L)));
    }

    // --- Immutability ---

    @Test
    void insertThrows() {
        PerfectHash ph = new PerfectHash(new long[]{1}, RandomSource.seeded(1L));
        assertThrows(UnsupportedOperationException.class, () -> ph.insert(99));
    }

    @Test
    void removeThrows() {
        PerfectHash ph = new PerfectHash(new long[]{1}, RandomSource.seeded(1L));
        assertThrows(UnsupportedOperationException.class, () -> ph.remove(1));
    }

    @Test
    void nullKeysThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new PerfectHash(null, RandomSource.seeded(1L)));
    }

    // --- Larger sets ---

    @Test
    void largerSetMembership() {
        int n = 200;
        long[] keys = new long[n];
        for (int i = 0; i < n; i++) {
            keys[i] = i * 100L + 7;
        }
        PerfectHash ph = new PerfectHash(keys, RandomSource.seeded(42L));
        for (long key : keys) {
            assertTrue(ph.contains(key), key + " should be found in larger set");
        }
        // Check a few absent keys
        assertFalse(ph.contains(99999));
        assertFalse(ph.contains(-7));
    }
}
