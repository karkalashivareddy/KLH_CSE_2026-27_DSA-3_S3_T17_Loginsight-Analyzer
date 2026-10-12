/**
 * Custom data structures used to keep algorithm internals transparent (docs/02 §8.3).
 *
 * <p>Populated on demand rather than speculatively. Phase 5 adds the two traversals the network-flow
 * engine needs to make its search order explicit:</p>
 * <ul>
 *   <li>{@link com.loginsight.dsa.common.CustomQueue} — growable circular FIFO used by the BFS
 *       traversals of Edmonds-Karp, Dinic and the min-cut reachability search;</li>
 *   <li>{@link com.loginsight.dsa.common.CustomStack} — growable LIFO used by the iterative depth-first
 *       augmenting search of Ford-Fulkerson.</li>
 * </ul>
 *
 * <p>Other structures named in docs/03 ({@code CustomArrayList}, {@code CustomDeque}, {@code CustomTrie},
 * {@code IntArrayQueue}) are intentionally <em>not</em> implemented yet: nothing in Phases 1–5 requires
 * them, and adding unused code would be speculation. They can be introduced when a later algorithm
 * genuinely needs them.</p>
 */
package com.loginsight.dsa.common;
