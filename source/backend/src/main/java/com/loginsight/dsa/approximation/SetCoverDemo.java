package com.loginsight.dsa.approximation;

/**
 * Algorithm: <strong>greedy set cover</strong> ({@code ln n} approximation) — Module 5 APX demo.
 *
 * <h2>Problem</h2>
 * Given a universe of elements {@code 0..universeSize-1} and a family of sets over it, choose the fewest
 * sets whose union is the whole universe. Exact set cover is NP-hard; greedy is the classical
 * logarithmic approximation.
 *
 * <h2>Core idea</h2>
 * Repeatedly pick the set that covers the most <em>still-uncovered</em> elements, until everything is
 * covered or no set makes progress. Ties are broken by set index (deterministic).
 *
 * <h2>State</h2>
 * A {@code boolean[] covered} marker over the universe and the list of chosen set indices.
 *
 * <h2>Approximation guarantee</h2>
 * For a set family whose largest set has {@code Delta <= n} elements, greedy is a
 * {@code H(Delta) <= 1 + ln n} approximation: {@code |greedy| <= H(Delta) * OPT}. The result reports
 * the harmonic bound {@code H(n) = 1 + 1/2 + ... + 1/n}, the worst-case upper bound — a worst-case
 * claim, not a per-instance guarantee.
 *
 * <h2>Correctness intuition</h2>
 * Greedy always covers at least a fraction {@code 1/OPT} of the remaining elements per set (because
 * the remaining optimum option covers at least that fraction), giving the harmonic bound; termination
 * is immediate.
 *
 * <h2>Complexity</h2>
 * Time {@code O(|U| * |S|)} per effective round, at most {@code |U|} rounds — implemented with raw
 * scans so {@code O(|U|^2 * |S|)} worst case, space {@code O(|U| + |selected|)}.
 *
 * <h2>Edge cases</h2>
 * Empty universe → empty cover; empty sets never help; duplicates inside a set are harmless; if the
 * family cannot cover the universe the result is flagged {@link SetCoverResult#isAllCovered()} {@code false}.
 */
public final class SetCoverDemo {

    public SetCoverResult greedyCover(int universeSize, int[][] sets) {
        if (universeSize < 0) {
            throw new IllegalArgumentException("universeSize must be >= 0 but was " + universeSize);
        }
        if (sets == null) {
            throw new IllegalArgumentException("sets must not be null");
        }
        long start = System.nanoTime();
        boolean[] covered = new boolean[universeSize];
        for (int s = 0; s < sets.length; s++) {
            for (int element : sets[s]) {
                if (element < 0 || element >= universeSize) {
                    throw new IllegalArgumentException("set " + s + " contains out-of-range element "
                            + element);
                }
            }
        }
        int[] chosen = new int[sets.length];
        int chosenCount = 0;
        int coveredCount = 0;
        boolean progressed = true;
        while (progressed && coveredCount < universeSize) {
            progressed = false;
            int bestSet = -1;
            int bestGain = 0;
            for (int s = 0; s < sets.length; s++) {
                int gain = 0;
                for (int element : sets[s]) {
                    if (!covered[element]) {
                        gain++;
                    }
                }
                if (gain > bestGain) {
                    bestGain = gain;
                    bestSet = s;
                }
            }
            if (bestSet != -1) {
                for (int element : sets[bestSet]) {
                    if (!covered[element]) {
                        covered[element] = true;
                        coveredCount++;
                    }
                }
                chosen[chosenCount++] = bestSet;
                progressed = true;
            }
        }
        int[] selected = new int[chosenCount];
        System.arraycopy(chosen, 0, selected, 0, chosenCount);
        long elapsed = System.nanoTime() - start;
        boolean allCovered = coveredCount == universeSize;
        double harmonic = harmonicNumber(universeSize);
        String notes = "greedy set cover is an H(Delta) <= 1+ln n approximation (APX); exact set cover"
                + " is NP-hard";
        return new SetCoverResult(universeSize, selected, coveredCount, allCovered, chosenCount,
                harmonic, notes, elapsed);
    }

    /** {@code 1 + 1/2 + ... + 1/n} (0 for {@code n <= 0}, matching an empty universe). */
    static double harmonicNumber(int n) {
        double sum = 0;
        for (int i = 1; i <= n; i++) {
            sum += 1.0 / i;
        }
        return sum;
    }
}