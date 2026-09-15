package com.loginsight.dsa.randomized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReservoirSamplingTest {

    // --- Edge cases ---

    @Test
    void kZeroAlwaysEmpty() {
        ReservoirSampling rs = new ReservoirSampling(0, RandomSource.seeded(1L));
        for (long i = 0; i < 100; i++) {
            rs.offer(i);
        }
        assertEquals(0, rs.sample().length);
        assertEquals(100, rs.countSeen());
    }

    @Test
    void kGreaterThanStreamReturnsAll() {
        ReservoirSampling rs = new ReservoirSampling(10, RandomSource.seeded(1L));
        long[] items = {100, 200, 300};
        for (long item : items) {
            rs.offer(item);
        }
        long[] sample = rs.sample();
        assertEquals(3, sample.length);
        // All items should be in the sample (in order of offer)
        assertEquals(100, sample[0]);
        assertEquals(200, sample[1]);
        assertEquals(300, sample[2]);
    }

    @Test
    void kEqualsStreamReturnsAll() {
        ReservoirSampling rs = new ReservoirSampling(3, RandomSource.seeded(1L));
        rs.offer(10);
        rs.offer(20);
        rs.offer(30);
        long[] sample = rs.sample();
        assertEquals(3, sample.length);
    }

    @Test
    void k1ReturnsSingleElement() {
        ReservoirSampling rs = new ReservoirSampling(1, RandomSource.seeded(42L));
        rs.offer(99);
        long[] sample = rs.sample();
        assertEquals(1, sample.length);
        assertEquals(99, sample[0]);
    }

    @Test
    void sampleReturnsDefensiveCopy() {
        ReservoirSampling rs = new ReservoirSampling(5, RandomSource.seeded(1L));
        rs.offer(1);
        rs.offer(2);
        long[] s1 = rs.sample();
        s1[0] = -999; // mutate the copy
        long[] s2 = rs.sample();
        assertEquals(1, s2[0]); // original unaffected
        assertEquals(2, s2[1]);
    }

    // --- Deterministic with fixed seed ---

    @Test
    void deterministicSameSeed() {
        long[] input = {10, 20, 30, 40, 50, 60, 70, 80};
        int k = 3;

        ReservoirSampling rs1 = new ReservoirSampling(k, RandomSource.seeded(42L));
        ReservoirSampling rs2 = new ReservoirSampling(k, RandomSource.seeded(42L));
        for (long v : input) {
            rs1.offer(v);
            rs2.offer(v);
        }
        assertEquals(rs1.countSeen(), rs2.countSeen());
        long[] s1 = rs1.sample();
        long[] s2 = rs2.sample();
        assertEquals(s1.length, s2.length);
        for (int i = 0; i < s1.length; i++) {
            assertEquals(s1[i], s2[i], "Element " + i + " differs");
        }
    }

    // --- Statistical uniformity test ---

    @Test
    void uniformityStatisticalTest() {
        // Stream n=10 items, k=3, run many trials.
        // Each item should appear in ~30% of the reservoirs (k/n = 3/10).
        int n = 10;
        int k = 3;
        int trials = 40000;
        int[] frequency = new int[n];

        for (int t = 0; t < trials; t++) {
            RandomSource rng = RandomSource.seeded(t);
            ReservoirSampling rs = new ReservoirSampling(k, rng);
            for (int i = 0; i < n; i++) {
                rs.offer(i);
            }
            long[] sample = rs.sample();
            boolean[] inSample = new boolean[n];
            for (long v : sample) {
                inSample[(int) v] = true;
            }
            for (int i = 0; i < n; i++) {
                if (inSample[i]) {
                    frequency[i]++;
                }
            }
        }

        double expected = (double) k / n; // 0.3
        double tolerance = 0.015;
        for (int i = 0; i < n; i++) {
            double observed = (double) frequency[i] / trials;
            assertTrue(Math.abs(observed - expected) < tolerance,
                    "Item " + i + " frequency " + observed
                            + " deviates from expected " + expected
                            + " (tolerance " + tolerance + ")");
        }
    }

    @Test
    void largeStreamSize() {
        ReservoirSampling rs = new ReservoirSampling(10, RandomSource.seeded(1L));
        for (long i = 0; i < 100_000; i++) {
            rs.offer(i);
        }
        assertEquals(100_000, rs.countSeen());
        assertEquals(10, rs.sample().length);
    }

    @Test
    void getKReturnsCorrectValue() {
        assertEquals(7, new ReservoirSampling(7, RandomSource.seeded(1L)).getK());
    }
}
