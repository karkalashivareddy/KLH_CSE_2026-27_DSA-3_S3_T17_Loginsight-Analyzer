package com.loginsight.dsa.randomized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModularArithmeticTest {

    @Test
    void normalizePositive() {
        assertEquals(3, ModularArithmetic.normalize(3, 7));
    }

    @Test
    void normalizeZero() {
        assertEquals(0, ModularArithmetic.normalize(0, 7));
    }

    @Test
    void normalizeNegative() {
        assertEquals(4, ModularArithmetic.normalize(-3, 7));
    }

    @Test
    void normalizeMultipleOfMod() {
        assertEquals(0, ModularArithmetic.normalize(14, 7));
    }

    @Test
    void addModBasic() {
        assertEquals(3, ModularArithmetic.addMod(1, 2, 7));
    }

    @Test
    void addModWraparound() {
        assertEquals(1, ModularArithmetic.addMod(5, 3, 7));
    }

    @Test
    void addModLargeOperands() {
        long mod = Long.MAX_VALUE; // 2^63 - 1
        long a = mod - 1;
        long b = 1;
        assertEquals(0, ModularArithmetic.addMod(a, b, mod));
    }

    @Test
    void addModNoOverflow() {
        // a + b > Long.MAX_VALUE before mod, but addMod must not overflow
        long mod = 1_000_000_007L;
        long a = mod - 1;
        long b = mod - 1;
        long expected = (2L * mod - 2L) % mod;
        assertEquals(expected, ModularArithmetic.addMod(a, b, mod));
    }

    @Test
    void multiplyModBasic() {
        assertEquals(6, ModularArithmetic.multiplyMod(2, 3, 7));
    }

    @Test
    void multiplyModWraparound() {
        // 5 * 3 = 15 = 2*7 + 1, so 15 mod 7 = 1
        assertEquals(1, ModularArithmetic.multiplyMod(5, 3, 7));
    }

    @Test
    void multiplyModZero() {
        assertEquals(0, ModularArithmetic.multiplyMod(0, 999, 7));
    }

    @Test
    void multiplyModOne() {
        assertEquals(5, ModularArithmetic.multiplyMod(5, 1, 11));
    }

    @Test
    void multiplyModLargeValuesNoOverflow() {
        // 2^62 * 2^62 mod (2^61-1) must not overflow
        long mod = 2305843009213693951L; // 2^61 - 1
        long a = 1L << 62;
        long b = 1L << 62;
        long result = ModularArithmetic.multiplyMod(a, b, mod);
        // 2^62 = 2·(2^61−1) + 2, so 2^62 ≡ 2 (mod 2^61−1); product ≡ 4
        assertEquals(4, result);
    }

    @Test
    void multiplyModMatchesBigIntegerOverflowCases() {
        java.math.BigInteger mod = java.math.BigInteger.valueOf(2305843009213693951L);
        RandomSource rng = RandomSource.seeded(7L);
        for (int i = 0; i < 200; i++) {
            long a = rng.nextLong();
            long b = rng.nextLong();
            long expected = java.math.BigInteger.valueOf(a)
                    .multiply(java.math.BigInteger.valueOf(b))
                    .mod(mod).longValue();
            assertEquals(expected, ModularArithmetic.multiplyMod(a, b, mod.longValue()),
                    "multiplyMod(" + a + ", " + b + ", mod)");
        }
    }

    @Test
    void powModBasic() {
        assertEquals(1, ModularArithmetic.powMod(2, 0, 7));
        assertEquals(2, ModularArithmetic.powMod(2, 1, 7));
        assertEquals(4, ModularArithmetic.powMod(2, 2, 7));
        assertEquals(1, ModularArithmetic.powMod(2, 3, 7));
    }

    @Test
    void powModExp10() {
        // 3^10 = 59049; 59049 mod 1000 = 49
        assertEquals(49, ModularArithmetic.powMod(3, 10, 1000));
    }

    @Test
    void powModMod1() {
        assertEquals(0, ModularArithmetic.powMod(5, 100, 1));
    }

    @Test
    void powModMatchesBigIntegerOverflowCases() {
        java.math.BigInteger mod = java.math.BigInteger.valueOf(1000000007L);
        RandomSource rng = RandomSource.seeded(11L);
        for (int i = 0; i < 30; i++) {
            long base = rng.nextLong();
            long exp = rng.nextInt(1000);
            long expected = java.math.BigInteger.valueOf(base)
                    .modPow(java.math.BigInteger.valueOf(exp), mod).longValue();
            assertEquals(expected, ModularArithmetic.powMod(base, exp, mod.longValue()),
                    "powMod(" + base + ", " + exp + ", mod)");
        }
    }

    @Test
    void normalizeThrowsOnZeroMod() {
        assertThrows(ArithmeticException.class, () -> ModularArithmetic.normalize(1, 0));
    }

    @Test
    void addModThrowsOnZeroMod() {
        assertThrows(ArithmeticException.class, () -> ModularArithmetic.addMod(1, 2, 0));
    }

    @Test
    void multiplyModThrowsOnNegativeMod() {
        assertThrows(ArithmeticException.class, () -> ModularArithmetic.multiplyMod(1, 2, -1));
    }
}
