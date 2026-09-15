package com.loginsight.dsa.randomized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RandomHashTest {

    private static final long PRIME = UniversalHashFamily.DEFAULT_PRIME;

    // --- Reproducibility ---

    @Test
    void sameKeySameSeedReproducible() {
        RandomSource rng = RandomSource.seeded(42L);
        UniversalHashFamily family = UniversalHashFamily.random(PRIME, 100, rng);
        assertEquals(family.hash(12345L), family.hash(12345L),
                "Same family instance must produce the same hash for the same key");
    }

    @Test
    void sameSeedSameFamilyReproducible() {
        UniversalHashFamily f1 = UniversalHashFamily.random(PRIME, 100, RandomSource.seeded(42L));
        UniversalHashFamily f2 = UniversalHashFamily.random(PRIME, 100, RandomSource.seeded(42L));
        assertEquals(f1.hash(999L), f2.hash(999L),
                "Same seed must produce the same family parameters");
    }

    // --- Spread ---

    @Test
    void differentSeedsSpread() {
        // Two families with different seeds should hash a key to different values (with high
        // probability for most keys on a large enough table).
        long key = 42L;
        boolean differ = false;
        for (int attempt = 0; attempt < 50; attempt++) {
            UniversalHashFamily f1 = UniversalHashFamily.random(PRIME, 1000, RandomSource.seeded(attempt));
            UniversalHashFamily f2 = UniversalHashFamily.random(PRIME, 1000, RandomSource.seeded(attempt + 1000));
            if (f1.hash(key) != f2.hash(key)) {
                differ = true;
                break;
            }
        }
        assertTrue(differ, "Different seeds should produce different hash values");
    }

    // --- Collision probability bounded ---

    @Test
    void collisionProbabilityBounded() {
        // With m = 1000 and n = 500 random keys, expected collisions ≈ n(n-1)/(2m) ≈ 124.75
        // (consecutive small integers are NOT a valid check: a linear universal hash on an
        // un-wrapped arithmetic sequence has structure, so random keys from the full long
        // range are used instead).
        int m = 1000;
        int n = 500;
        int trials = 20;
        long totalCollisions = 0;
        for (int t = 0; t < trials; t++) {
            UniversalHashFamily family = UniversalHashFamily.random(PRIME, m, RandomSource.seeded(t));
            RandomSource keyRng = RandomSource.seeded(1000L + t);
            int[] counts = new int[m];
            int collisions = 0;
            for (int i = 0; i < n; i++) {
                int h = family.hash(keyRng.nextLong());
                if (counts[h] > 0) {
                    collisions++;
                }
                counts[h]++;
            }
            totalCollisions += collisions;
        }
        double avg = (double) totalCollisions / trials;
        // Expected collisions ≈ n(n-1)/(2m) = 500*499/2000 ≈ 124.75
        double expected = (double) n * (n - 1) / (2.0 * m);
        // Per-trial std dev is ~sqrt(n(n-1)/(2m)) ≈ 11; widen to ±30%.
        assertTrue(avg > 0.7 * expected && avg < 1.3 * expected,
                "Average collisions " + avg + " deviates too much from expected " + expected);
    }

    // --- Pairwise independence (educational demo, not a mathematical proof) ---

    @Test
    void pairwiseIndependenceOnSmallUniverse() {
        // For a small universe {0..U-1}, over many random families,
        // P(h(x) == h(y)) should be ≈ 1/m.
        int U = 50;
        int m = 10;
        int familyCount = 5000;
        int x = 7, y = 3; // distinct keys
        int collisionCount = 0;
        for (int i = 0; i < familyCount; i++) {
            UniversalHashFamily family = UniversalHashFamily.random(PRIME, m, RandomSource.seeded(i));
            if (family.hash(x) == family.hash(y)) {
                collisionCount++;
            }
        }
        double empiricalP = (double) collisionCount / familyCount;
        // Expected ≈ 1/m = 0.1; allow ±0.03 (≈ 4 sigma for n=5000)
        assertTrue(Math.abs(empiricalP - 1.0 / m) < 0.03,
                "Empirical collision probability " + empiricalP + " deviates from 1/m=" + (1.0 / m));
    }

    // --- RandomizedHash demo ---

    @Test
    void randomizedHashDemoCollectsStatistics() {
        long[] keys = {10, 25, 40, 55, 70};
        UniversalHashFamily family = UniversalHashFamily.random(PRIME, 7, RandomSource.seeded(42L));
        RandomizedHash demo = new RandomizedHash(keys, family);
        RandomizedHash.HashStatistics stats = demo.getStatistics();
        assertEquals(5, stats.keyCount());
        assertEquals(7, stats.tableSize());
        assertTrue(stats.collisionPairs() >= 0);
        assertTrue(stats.maxBucketSize() >= 1);
    }

    @Test
    void randomizedHashConsistentWithFamily() {
        long[] keys = {100, 200, 300};
        UniversalHashFamily family = UniversalHashFamily.random(PRIME, 5, RandomSource.seeded(1L));
        RandomizedHash demo = new RandomizedHash(keys, family);
        for (long key : keys) {
            assertEquals(family.hash(key), demo.hash(key));
        }
    }

    @Test
    void randomizedResultWrapsEvidence() {
        RandomizedResult result = RandomizedResult.of("UniversalHashing",
                "m=100, collisions=3",
                "collision probability ≈ 1/m");
        assertEquals("UniversalHashing", result.getAlgorithm());
        assertFalse(result.getSummary().isEmpty());
    }

    // --- Parameter validation ---

    @Test
    void invalidAThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new UniversalHashFamily(0, 0, PRIME, 10));
    }

    @Test
    void invalidMThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new UniversalHashFamily(1, 0, PRIME, 0));
    }

    @Test
    void mEqualsPThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new UniversalHashFamily(1, 0, PRIME, (int) PRIME));
    }

    @Test
    void universalHashNegativeKeysWork() {
        UniversalHashFamily family = UniversalHashFamily.random(PRIME, 100, RandomSource.seeded(1L));
        int h = family.hash(-42L);
        assertTrue(h >= 0 && h < 100);
    }
}
