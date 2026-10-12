/**
 * NP-completeness and approximation module (DSA-3 Module 5), implemented and tested in Phase 6.
 *
 * <h2>Executable algorithms</h2>
 * <ul>
 *   <li>{@link com.loginsight.dsa.approximation.MaximalMatching} — greedy maximal matching, {@code O(E)}.</li>
 *   <li>{@link com.loginsight.dsa.approximation.VertexCoverApproximation} — vertex cover 2-approximation
 *       via maximal matching; reports cover, matching, a proven lower bound and the ratio.</li>
 *   <li>{@link com.loginsight.dsa.approximation.BoundedVertexCover} — exact FPT decision/certificate,
 *       bounded branching {@code O(2^k (V+E))}. This is exact for small {@code k}, not approximation.</li>
 *   <li>{@link com.loginsight.dsa.approximation.VertexCoverKernelization} — self-loop + high-degree
 *       kernelization rules that preserve the {@code tau(G) <= k} answer.</li>
 *   <li>{@link com.loginsight.dsa.approximation.KnapsackFPTAS} — 0/1 knapsack value-scaling FPTAS with
 *       the {@code A >= (1-eps) OPT} guarantee.</li>
 *   <li>{@link com.loginsight.dsa.approximation.SetCoverDemo} — greedy set cover, {@code H(n)} APX bound.</li>
 *   <li>{@link com.loginsight.dsa.approximation.IndependentSetReduction} — vertex cover ↔ independent
 *       set transformation and verifiers.</li>
 *   <li>{@link com.loginsight.dsa.approximation.ComplementGraph} — clique ↔ independent set in the
 *       complement transformation and verifiers.</li>
 * </ul>
 *
 * <h2>Shared model</h2>
 * {@link com.loginsight.dsa.approximation.UndirectedGraph} is the single undirected, simple graph
 * definition (documented duplicate/self-loop policies, deterministic edge order,
 * label-preserving {@code withoutVertex} for FPT branching).
 *
 * <h2>Honesty rules (enforced here and in the tests)</h2>
 * Production approximation classes never compute an optimum; optimum values exist only inside
 * independent test oracles (subset enumeration for vertex cover, exact DP for knapsack). Complexity and
 * theory claims are stated conservatively — see {@code docs/np-completeness.md} and the class javadoc.
 * Conceptually-documented topics that are <em>not</em> implemented as code (P/NP/co-NP, Cook-Levin, the
 * reduction zoo narrative, PTAS class, APX class) are explicitly marked as documentation-only there.
 */
package com.loginsight.dsa.approximation;