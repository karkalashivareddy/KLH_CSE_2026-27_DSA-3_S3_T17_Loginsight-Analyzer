/**
 * Advanced dynamic programming engine (DSA-3 Module 3): editdistance/*, alignment/*, interval/*,
 * bitmask/*, tree/*, sos/*.
 *
 * <p>Implemented in Phase 4 (docs/03, docs/04):</p>
 * <ul>
 *   <li>{@link com.loginsight.dsa.dp.editdistance.LevenshteinDistance} — Wagner-Fischer edit distance.</li>
 *   <li>{@link com.loginsight.dsa.dp.editdistance.DamerauLevenshteinDistance} — Optimal String
 *       Alignment variant (insert/delete/substitute/adjacent-transpose).</li>
 *   <li>{@link com.loginsight.dsa.dp.editdistance.WeightedEditDistance} — configurable operation costs.</li>
 *   <li>{@link com.loginsight.dsa.dp.alignment.NeedlemanWunsch} — global sequence alignment.</li>
 *   <li>{@link com.loginsight.dsa.dp.alignment.SmithWaterman} — local sequence alignment.</li>
 *   <li>{@link com.loginsight.dsa.dp.interval.MatrixChainMultiplication} — interval DP.</li>
 *   <li>{@link com.loginsight.dsa.dp.interval.OptimalBinarySearchTree} — interval DP.</li>
 *   <li>{@link com.loginsight.dsa.dp.bitmask.BitmaskTSP} — Held-Karp bitmask DP.</li>
 *   <li>{@link com.loginsight.dsa.dp.bitmask.HamiltonianPath} — subset/bitmask DP.</li>
 *   <li>{@link com.loginsight.dsa.dp.tree.TreeDiameterDP}, {@link com.loginsight.dsa.dp.tree.TreeCentroidDP},
 *       {@link com.loginsight.dsa.dp.tree.TreeSubsetSumDP}, {@link com.loginsight.dsa.dp.tree.RerootingDP}
 *       — tree DP over a validated {@link com.loginsight.dsa.dp.tree.Tree}.</li>
 *   <li>{@link com.loginsight.dsa.dp.sos.SOSDP} — sum-over-subsets zeta transform.</li>
 * </ul>
 *
 * <p>Every algorithm returns the uniform {@link com.loginsight.dsa.dp.DpResult}, exposes its DP
 * table/structures for teaching, and documents state, base case, recurrence, transition, final answer
 * and complexity in the class javadoc. Input validation raises {@code IllegalArgumentException}
 * rather than letting an index exception escape. Costs use {@code long} with a documented sentinel
 * strategy, never an unprotected {@code Integer.MAX_VALUE + x}. Ties are broken deterministically and
 * documented.</p>
 *
 * <p><strong>When DP is the right tool — and when it is not.</strong> Dynamic programming applies when
 * a problem has (1) <em>optimal substructure</em> (an optimal solution is built from optimal
 * solutions to subproblems) and (2) <em>overlapping subproblems</em> (the same subproblem recurs, so
 * memoisation/tabulation removes exponential recomputation). The pen-and-paper examples in this
 * engine all satisfy both. DP is <em>not</em> the right tool when either property is absent: shortest
 * paths on a graph are better served by BFS/Dijkstra/graph algorithms (Module 4), activity selection
 * and Huffman coding by greedy, and many NP-hard problems (TSP, set cover, vertex cover) by
 * approximation/branch-and-bound (Module 5) when exact DP is infeasible. This engine never implements
 * a fake DP for such problems; where a demo is NP-hard the exact bitmask DP is size-capped and
 * documented as such.</p>
 */
package com.loginsight.dsa.dp;
