/**
 * Hand-written DSA engine. This is the syllabus-authentic core of LogInsight Analyzer.
 *
 * <p>ARCHITECTURE RULE (see docs/02 §8): algorithm implementations in this package must not
 * delegate to {@code java.util} collections/sorting to hide the required logic. The DSA engine
 * stays free of Spring/web references and is developed in later phases.</p>
 *
 * <p>Phase boundary: Phase 1 creates the package skeleton; Phase 2 adds the analytics engine; Phase 3
 * implements the string algorithm engine under {@code string/}, {@code string/aho} and
 * {@code string/suffix}; Phase 4 implements the advanced dynamic programming engine under {@code dp/}
 * ({@code dp/editdistance}, {@code dp/alignment}, {@code dp/interval}, {@code dp/bitmask},
 * {@code dp/tree}, {@code dp/sos}); Phase 5 implements the network flow and matching engine under
 * {@code flow/} plus the {@code common/} queue and stack it relies on; Phase 6 implements the
 * NP-completeness and approximation engine under {@code approximation/} (maximal matching, vertex cover
 * 2-approximation, FPT bounded branching, kernelization, knapsack FPTAS, set cover, reductions);
 * Phase 7 implements the randomized algorithms engine under {@code randomized/} (randomized
 * quicksort, Miller-Rabin primality, universal and FKS perfect hashing, reservoir sampling, plus
 * the shared seeded {@link com.loginsight.dsa.randomized.RandomSource} and overflow-safe
 * {@link com.loginsight.dsa.randomized.ModularArithmetic} supports); Phase 8 implements the
 * parallel algorithms engine under {@code parallel/} (parallel reduction, Blelloch prefix scan,
 * parallel merge sort, work/span analysis and benchmark) — the only package permitted to use
 * {@code java.util.concurrent}, per docs/02 §8.2.</p>
 */
package com.loginsight.dsa;