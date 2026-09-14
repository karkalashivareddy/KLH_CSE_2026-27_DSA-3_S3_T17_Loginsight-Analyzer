/**
 * Network flow module (DSA-3 Module 4) — hand-written maximum-flow, minimum-cut, bipartite-matching and
 * minimum-cost-flow algorithms, implemented and tested in Phase 5.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.loginsight.dsa.flow.FlowGraph} — immutable directed capacitated graph with optional
 *       per-unit edge costs; the single shared problem definition.</li>
 *   <li>{@link com.loginsight.dsa.flow.Edge} — one original edge (id, endpoints, capacity, cost).</li>
 *   <li>{@link com.loginsight.dsa.flow.FordFulkerson} — maximum flow, depth-first augmenting paths,
 *       {@code O(E * f_max)}.</li>
 *   <li>{@link com.loginsight.dsa.flow.EdmondsKarp} — maximum flow, breadth-first shortest augmenting
 *       paths, {@code O(V E^2)}.</li>
 *   <li>{@link com.loginsight.dsa.flow.Dinic} — maximum flow via level graph and blocking flows,
 *       {@code O(V^2 E)} general, {@code O(E sqrt(V))} on unit-capacity networks.</li>
 *   <li>{@link com.loginsight.dsa.flow.MinCut} — minimum {@code s}-{@code t} cut via max-flow/min-cut.</li>
 *   <li>{@link com.loginsight.dsa.flow.BipartiteMatching} — maximum bipartite matching by flow
 *       reduction.</li>
 *   <li>{@link com.loginsight.dsa.flow.MinCostMaxFlow} — minimum-cost maximum flow by successive
 *       shortest paths with Bellman-Ford, {@code O(f_max * V * E)}.</li>
 * </ul>
 *
 * <h2>Results</h2>
 * {@link com.loginsight.dsa.flow.FlowResult}, {@link com.loginsight.dsa.flow.MinCutResult},
 * {@link com.loginsight.dsa.flow.MatchingResult} and
 * {@link com.loginsight.dsa.flow.MinCostFlowResult} are immutable snapshots reporting per-edge flow so
 * callers never touch mutable solver state.
 *
 * <h2>Internal design</h2>
 * {@link com.loginsight.dsa.flow.ResidualNetwork} is the one shared, package-private residual
 * representation: each original edge becomes a forward/reverse residual pair at consecutive indices
 * {@code e} and {@code e ^ 1}. Every algorithm augments on top of it, and validation is centralised in
 * the package-private {@code FlowValidator}. No third-party graph or flow library is used, and no
 * {@code java.util} collection drives any traversal.
 */
package com.loginsight.dsa.flow;
