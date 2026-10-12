/**
 * Randomized algorithms engine (DSA-3 Module 6A).
 *
 * <p>Every randomized algorithm in this package receives its randomness from a
 * {@link com.loginsight.dsa.randomized.RandomSource} instance by dependency injection; there is
 * <b>no hidden global mutable RNG state</b>. Tests use {@code RandomSource.seeded(seed)} for
 * deterministic, reproducible runs; production callers use {@code RandomSource.unseeded()}.</p>
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@link com.loginsight.dsa.randomized.RandomizedQuickSort} – randomized quicksort
 *       (Hoare partition, random pivot, tail-call elimination); expected O(n log n).</li>
 *   <li>{@link com.loginsight.dsa.randomized.MillerRabin} – Miller-Rabin primality test in
 *       deterministic (7-witness set, correct for all {@code long}) and Monte Carlo
 *       (probabilistic) modes, backed by overflow-safe modular arithmetic.</li>
 *   <li>{@link com.loginsight.dsa.randomized.ModularArithmetic} – overflow-safe double-and-add
 *       multiplication and square-and-multiply exponentiation (no {@code BigInteger}).</li>
 *   <li>{@link com.loginsight.dsa.randomized.UniversalHashFamily} – pairwise-independent
 *       family {@code h(x) = ((a·x + b) mod p) mod m}; collision probability ≈ 1/m.</li>
 *   <li>{@link com.loginsight.dsa.randomized.RandomizedHash} – educational hash-table demo
 *       exposing collisions, max chain length and load factor (not a disguised HashMap).</li>
 *   <li>{@link com.loginsight.dsa.randomized.PerfectHash} – FKS two-level perfect hashing with
 *       O(1) worst-case membership and expected O(n) space.</li>
 *   <li>{@link com.loginsight.dsa.randomized.ReservoirSampling} – Algorithm R uniform K-sample
 *       over an unknown-length stream with O(K) storage.</li>
 *   <li>{@link com.loginsight.dsa.randomized.RandomizedResult} – transversal evidence wrapper
 *       for randomized computations.</li>
 * </ul>
 *
 * <p>Randomness is reproducible only under a fixed seed; the statistical guarantees
 * (expected depth, collision probability, reservoir uniformity) are probability statements about
 * the family, verified empirically in tests but never claimed as per-run guarantees.</p>
 *
 * <p>Phase 1 skeleton created; implemented and tested in Phase 7.</p>
 */
package com.loginsight.dsa.randomized;