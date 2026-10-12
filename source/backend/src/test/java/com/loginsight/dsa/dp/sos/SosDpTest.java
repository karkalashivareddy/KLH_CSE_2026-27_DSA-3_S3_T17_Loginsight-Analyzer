package com.loginsight.dsa.dp.sos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.SmallRandom;
import org.junit.jupiter.api.Test;

class SosDpTest {

    private final SOSDP sos = new SOSDP();

    @Test
    void twoBitExample() {
        assertArrayEquals(new long[]{1, 3, 4, 10}, sos.subsetSums(new long[]{1, 2, 3, 4}, 2));
    }

    @Test
    void oneBitExample() {
        assertArrayEquals(new long[]{5, 11}, sos.subsetSums(new long[]{5, 6}, 1));
    }

    @Test
    void zeroBits() {
        assertArrayEquals(new long[]{7}, sos.subsetSums(new long[]{7}, 0));
    }

    @Test
    void matchesDirectSubmaskEnumeration() {
        SmallRandom random = new SmallRandom(123456L);
        for (int trial = 0; trial < 20; trial++) {
            int bits = random.nextInt(7);
            int size = 1 << bits;
            long[] values = new long[size];
            for (int i = 0; i < size; i++) {
                values[i] = random.nextInt(20);
            }
            assertArrayEquals(direct(values, bits), sos.subsetSums(values, bits),
                    "trial " + trial + " bits " + bits);
        }
    }

    @Test
    void singleSetBitAccumulates() {
        // bits = 3, only value[1] non-zero -> every mask containing bit 0 has that value.
        long[] values = new long[8];
        values[1] = 9;
        long[] result = sos.subsetSums(values, 3);
        for (int mask = 0; mask < 8; mask++) {
            assertEquals((mask & 1) != 0 ? 9 : 0, result[mask], "mask " + mask);
        }
    }

    @Test
    void envelopeView() {
        DpResult result = sos.solve(new long[]{1, 2, 3, 4}, 2);
        assertEquals("SOS DP", result.getAlgorithm());
        assertEquals(2, result.getInputSize());
        assertEquals(1 + 3 + 4 + 10, result.resultAsLong());
        assertEquals("O(n * 2^n)", result.getTimeComplexity());
        assertTrue(result.getIntermediateData() instanceof long[]);
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> sos.subsetSums(null, 2));
        assertThrows(IllegalArgumentException.class, () -> sos.subsetSums(new long[]{1, 2, 3}, 2));
        assertThrows(IllegalArgumentException.class, () -> sos.subsetSums(new long[]{1}, -1));
        assertThrows(IllegalArgumentException.class,
                () -> sos.subsetSums(new long[1], SOSDP.MAX_BITS + 1));
    }

    private static long[] direct(long[] values, int bits) {
        int size = 1 << bits;
        long[] result = new long[size];
        for (int mask = 0; mask < size; mask++) {
            long sum = 0;
            int sub = mask;
            while (true) {
                sum += values[sub];
                if (sub == 0) {
                    break;
                }
                sub = (sub - 1) & mask;
            }
            result[mask] = sum;
        }
        return result;
    }
}
