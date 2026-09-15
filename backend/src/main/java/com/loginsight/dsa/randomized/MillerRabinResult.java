package com.loginsight.dsa.randomized;

/**
 * Immutable result of a {@link MillerRabin} primality test.
 *
 * <p>Records the input {@code n}, the verdict (prime / composite), the mode used, the
 * number of rounds applied, the witnesses (bases) tested, and a human-readable note on
 * the guarantee strength.</p>
 */
public final class MillerRabinResult {

    private final long n;
    private final boolean prime;
    private final String mode;
    private final int rounds;
    private final long[] witnesses;
    private final String note;

    MillerRabinResult(long n, boolean prime, String mode, int rounds,
                      long[] witnesses, String note) {
        this.n = n;
        this.prime = prime;
        this.mode = mode;
        this.rounds = rounds;
        this.witnesses = witnesses.clone();
        this.note = note;
    }

    /** The tested value. */
    public long getN() {
        return n;
    }

    /** {@code true} when the test concluded {@code n} is (probably) prime. */
    public boolean isPrime() {
        return prime;
    }

    /** {@code "DETERMINISTIC"} or {@code "PROBABILISTIC"}. */
    public String getMode() {
        return mode;
    }

    /** Number of Miller–Rabin rounds applied. */
    public int getRounds() {
        return rounds;
    }

    /** Defensive copy of the bases used as witnesses. */
    public long[] getWitnesses() {
        return witnesses.clone();
    }

    /** Explanation of the guarantee strength. */
    public String getNote() {
        return note;
    }

    @Override
    public String toString() {
        return "MillerRabinResult{n=" + n + ", prime=" + prime + ", mode=" + mode
                + ", rounds=" + rounds + ", note='" + note + "'}";
    }
}
