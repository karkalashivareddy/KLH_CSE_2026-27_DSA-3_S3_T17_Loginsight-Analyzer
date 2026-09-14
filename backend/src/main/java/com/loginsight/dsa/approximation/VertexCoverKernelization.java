package com.loginsight.dsa.approximation;

/**
 * Algorithm: <strong>kernelization rules for parameterized (minimum) vertex cover</strong>.
 *
 * <h2>Problem</h2>
 * Given {@code (G, k)}, shrink the instance to a smaller equivalent one without changing the answer to
 * "<em>is there a vertex cover of size {@code <= k}?</em>". This turns preprocessing into the subject
 * of study — the FPT program's kernelization step.
 *
 * <h2>Safe reduction rules implemented</h2>
 * <ol>
 *   <li><strong>Rule 0 (self-loop)</strong> — a self-loop at {@code v} forces {@code v} into every
 *       cover: select {@code v}, delete it and its incident edges, decrement {@code k}.</li>
 *   <li><strong>Rule 1 (isolated vertex)</strong> — a vertex of degree 0 can never help, so it is
 *       ignored (it carries no edges to cover).</li>
 *   <li><strong>Rule 2 (high degree)</strong> — if {@code degree(v) > k}, then {@code v} must belong
 *       to <em>every</em> cover of size at most {@code k}: else all {@code degree(v)} neighbours would
 *       have to be chosen, needing {@code >= degree(v) > k} vertices. Select {@code v}, delete it and
 *       its incident edges, decrement {@code k}.</li>
 * </ol>
 * Rules are applied relative to the current graph and current {@code k} and repeated until no rule
 * fires (each fire removes a vertex, so it terminates). The final graph has maximum degree
 * {@code <= remainingK}.
 *
 * <h2>Correctness argument</h2>
 * Each rule keeps {@code (G, k)} decision-equivalent: the answer for the original instance is true
 * exactly when the answer for the reduced instance with the decremented budget is true. This is
 * verified in the tests by comparing the exact cover size of the original and reduced instances
 * ({@link KernelizationResult#originalTau(int)}).
 *
 * <h2>Honest bound statement</h2>
 * These two rules alone do <em>not</em> establish a specific kernel-size bound such as {@code O(k^2)}
 * — that requires further combinatorial rules not implemented here. The documentation therefore makes
 * no claim of a numeric kernel bound; the delivered value is the answer-preserving reduction itself.
 *
 * <h2>Complexity</h2>
 * Each rule scan is {@code O(E)}; at most {@code n} vertices are removed, so time {@code O(nE)} worst
 * case, space {@code O(V + E)}.
 *
 * <h2>Edge cases</h2>
 * Negative {@code k} / null graph rejected; empty or edgeless graphs reduce trivially; a budget that
 * runs out while edges remain is reported as {@link KernelizationResult#isInfeasible()}.
 */
public final class VertexCoverKernelization {

    public KernelizationResult kernelize(UndirectedGraph graph, int k) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k must be >= 0 but was " + k);
        }
        long start = System.nanoTime();
        int n = graph.vertexCount();
        boolean[] forced = new boolean[n];
        int forcedCount = 0;
        UndirectedGraph work = graph;
        int remainingK = k;
        boolean infeasible = false;

        boolean fired = true;
        int guard = 0;
        while (fired && !infeasible) {
            fired = false;
            for (int v = 0; v < n; v++) {
                if (forced[v] || !work.hasVertex(v)) {
                    continue;
                }
                if (work.degree(v) > remainingK || hasSelfLoop(work, v)) {
                    if (remainingK == 0) {
                        infeasible = true;
                        break;
                    }
                    forced[v] = true;
                    forcedCount++;
                    work = work.withoutVertex(v);
                    remainingK--;
                    fired = true;
                    break;
                }
            }
            if (++guard > n + 1) {
                break;
            }
        }
        int[] forcedVertices = collectForced(forced, n, forcedCount);
        long elapsed = System.nanoTime() - start;
        String notes = "degree rule (degree > k) and self-loop rule; preserves tau(G) <= k answers;"
                + " no numeric kernel-size bound is claimed without the full rule set";
        return new KernelizationResult(forcedVertices, work, remainingK, infeasible, elapsed, notes);
    }

    private static boolean hasSelfLoop(UndirectedGraph graph, int v) {
        for (int id = 0; id < graph.edgeCount(); id++) {
            UndirectedEdge edge = graph.edge(id);
            if (edge.isSelfLoop() && edge.touches(v)) {
                return true;
            }
        }
        return false;
    }

    private static int[] collectForced(boolean[] forced, int n, int count) {
        int[] result = new int[count];
        int index = 0;
        for (int v = 0; v < n; v++) {
            if (forced[v]) {
                result[index++] = v;
            }
        }
        return result;
    }
}