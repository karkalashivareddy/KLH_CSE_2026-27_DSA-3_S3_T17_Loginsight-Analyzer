package com.loginsight.dsa.randomized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class MillerRabinTest {

    private final MillerRabin mr = new MillerRabin();

    // --- Deterministic mode ---

    @Test
    void nBelow2IsComposite() {
        assertFalse(mr.isDefinitePrime(0));
        assertFalse(mr.isDefinitePrime(1));
    }

    @Test
    void smallPrimes() {
        for (long p : new long[]{2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47}) {
            assertTrue(mr.isDefinitePrime(p), p + " should be prime");
        }
    }

    @Test
    void smallComposites() {
        for (long c : new long[]{4, 6, 8, 9, 10, 12, 14, 15, 21, 25, 27, 33, 35, 49}) {
            assertFalse(mr.isDefinitePrime(c), c + " should be composite");
        }
    }

    @Test
    void carmichaelNumbersAreComposite() {
        for (long c : new long[]{561, 1105, 1729, 2465, 2821, 6601, 8911}) {
            assertFalse(mr.isDefinitePrime(c),
                    "Carmichael number " + c + " should be composite");
        }
    }

    @Test
    void mersennePrime2p31minus1() {
        assertTrue(mr.isDefinitePrime(2_147_483_647L)); // 2^31 - 1
    }

    @Test
    void mersennePrime2p61minus1() {
        assertTrue(mr.isDefinitePrime(2_305_843_009_213_693_951L)); // 2^61 - 1
    }

    @Test
    void longMaxValueIsComposite() {
        assertFalse(mr.isDefinitePrime(Long.MAX_VALUE)); // 2^63 - 1
    }

    @Test
    void largePrime2p63minus25() {
        // 9223372036854775783 is claimed prime (2^63 - 25)
        assertTrue(mr.isDefinitePrime(9_223_372_036_854_775_783L));
    }

    @Test
    void pseudoprimeToSmallBases() {
        // 341550071728321 is pseudoprime to bases 2,3,5,7,11,13 but composite
        assertFalse(mr.isDefinitePrime(341_550_071_728_321L));
    }

    @Test
    void strongPseudoprimeToFirst9Primes() {
        // 3825123056546413051 is strong pseudoprime to bases 2,3,5,7,11,13,17,19,23
        // but our witness 325 catches it
        assertFalse(mr.isDefinitePrime(3_825_123_056_546_413_051L));
    }

    @Test
    void negativeNThrows() {
        assertThrows(IllegalArgumentException.class, () -> mr.isDefinitePrime(-1));
    }

    @Test
    void evenComposites() {
        assertFalse(mr.isDefinitePrime(100));
        assertFalse(mr.isDefinitePrime(1_000_000));
        assertFalse(mr.isDefinitePrime(Long.MAX_VALUE - 1)); // even
    }

    @Test
    void deterministicResultAgreesWithBigInteger() {
        long[] testValues = {0, 1, 2, 3, 4, 5, 17, 561, 1105, 2147483647L,
                9223372036854775807L, 9223372036854775783L};
        for (long n : testValues) {
            boolean expected = BigInteger.valueOf(n).isProbablePrime(100);
            assertEquals(expected, mr.isDefinitePrime(n),
                    "Mismatch for n=" + n);
        }
    }

    // --- Probabilistic mode ---

    @Test
    void probabilisticCorrectForKnownPrimes() {
        RandomSource rng = RandomSource.seeded(42L);
        long[] primes = {2, 3, 5, 17, 97, 2147483647L};
        for (long p : primes) {
            MillerRabinResult result = mr.isProbablePrime(p, 20, rng);
            assertTrue(result.isPrime(), p + " should be probable prime (20 rounds)");
        }
    }

    @Test
    void probabilisticCorrectForComposites() {
        RandomSource rng = RandomSource.seeded(42L);
        long[] composites = {4, 561, 1105, 100};
        for (long c : composites) {
            MillerRabinResult result = mr.isProbablePrime(c, 20, rng);
            assertFalse(result.isPrime(), c + " should be composite (20 rounds)");
        }
    }

    @Test
    void probabilisticRoundBehavior() {
        RandomSource rng = RandomSource.seeded(99L);
        MillerRabinResult r1 = mr.isProbablePrime(97, 1, rng);
        MillerRabinResult r2 = mr.isProbablePrime(97, 10, RandomSource.seeded(99L));
        assertEquals(1, r1.getRounds());
        assertEquals(10, r2.getRounds());
    }

    @Test
    void probabilisticModeRequiresRng() {
        assertThrows(IllegalArgumentException.class,
                () -> mr.test(7, MillerRabin.Mode.PROBABILISTIC, null, 5));
    }

    @Test
    void probabilisticModeRequiresPositiveRounds() {
        assertThrows(IllegalArgumentException.class,
                () -> mr.test(7, MillerRabin.Mode.PROBABILISTIC, RandomSource.seeded(1L), 0));
    }

    @Test
    void resultModeAndWitnesses() {
        MillerRabinResult result = mr.test(17, MillerRabin.Mode.DETERMINISTIC, null, 0);
        assertEquals("DETERMINISTIC", result.getMode());
        assertEquals(7, result.getRounds());
        assertNotNull(result.getWitnesses());
        assertEquals(7, result.getWitnesses().length);
    }

    @Test
    void resultToString() {
        MillerRabinResult result = mr.test(17, MillerRabin.Mode.DETERMINISTIC, null, 0);
        assertNotNull(result.toString());
    }
}
