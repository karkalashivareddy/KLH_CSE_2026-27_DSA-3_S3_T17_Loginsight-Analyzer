package com.loginsight.dsa.flow;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Independent brute-force oracles for the flow engine, used only by tests.
 *
 * <p>These are deliberately <em>not</em> flow algorithms: they enumerate candidate solutions
 * exhaustively, so agreement with them is real evidence rather than a second implementation of the
 * same idea.</p>
 *
 * <ul>
 *   <li>{@link #maxFlow} — enumerate every integral flow within capacities ({@code prod(c_e + 1)}
 *       assignments), keep those satisfying conservation, maximise the value;</li>
 *   <li>{@link #minCutCapacity} — enumerate every vertex subset separating source from sink, take the
 *       minimum crossing capacity;</li>
 *   <li>{@link #minCostMaxFlow} — among the enumerated flows attaining the maximum value, minimise
 *       cost;</li>
 *   <li>{@link #maxMatching} — backtracking over left vertices for bipartite matching.</li>
 * </ul>
 *
 * <p>Only safe on the tiny graphs the tests construct (a few edges, small capacities).</p>
 */
final class FlowOracle {

    private static final long INFEASIBLE = Long.MIN_VALUE;

    private FlowOracle() {
    }

    static long maxFlow(FlowGraph graph, int source, int sink) {
        long[] flow = new long[graph.edgeCount()];
        long[] best = {Long.MIN_VALUE};
        searchValue(graph, source, sink, 0, flow, best);
        assertTrue(best[0] != Long.MIN_VALUE, "graph admits at least the zero flow");
        return best[0];
    }

    static long minCutCapacity(FlowGraph graph, int source, int sink) {
        int n = graph.vertexCount();
        long best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << n); mask++) {
            if ((mask & (1 << source)) == 0 || (mask & (1 << sink)) != 0) {
                continue;
            }
            long capacity = 0;
            for (int id = 0; id < graph.edgeCount(); id++) {
                boolean fromSide = (mask & (1 << graph.edgeFrom(id))) != 0;
                boolean toSide = (mask & (1 << graph.edgeTo(id))) != 0;
                if (fromSide && !toSide) {
                    capacity += graph.edgeCapacity(id);
                }
            }
            if (capacity < best) {
                best = capacity;
            }
        }
        return best;
    }

    /** Returns {@code {maxFlowValue, minCostAmongMaxFlows}}. */
    static long[] minCostMaxFlow(FlowGraph graph, int source, int sink) {
        long maxValue = maxFlow(graph, source, sink);
        long[] flow = new long[graph.edgeCount()];
        long[] bestCost = {Long.MAX_VALUE};
        searchCost(graph, source, sink, maxValue, 0, flow, bestCost);
        assertTrue(bestCost[0] != Long.MAX_VALUE, "a max flow exists");
        return new long[]{maxValue, bestCost[0]};
    }

    /** Maximum bipartite matching by backtracking; {@code adjacency[left][right]}. */
    static int maxMatching(boolean[][] adjacency, int leftCount, int rightCount) {
        int[] rightPartner = new int[rightCount];
        for (int r = 0; r < rightCount; r++) {
            rightPartner[r] = -1;
        }
        return matchLeft(adjacency, leftCount, 0, rightPartner);
    }

    private static int matchLeft(boolean[][] adjacency, int leftCount, int left, int[] rightPartner) {
        if (left == leftCount) {
            return 0;
        }
        int best = matchLeft(adjacency, leftCount, left + 1, rightPartner);
        for (int right = 0; right < rightPartner.length; right++) {
            if (adjacency[left][right] && rightPartner[right] == -1) {
                rightPartner[right] = left;
                int candidate = 1 + matchLeft(adjacency, leftCount, left + 1, rightPartner);
                rightPartner[right] = -1;
                if (candidate > best) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static void searchValue(FlowGraph graph, int source, int sink, int index, long[] flow,
                                    long[] best) {
        if (index == graph.edgeCount()) {
            long value = feasibleValue(graph, flow, source, sink);
            if (value != INFEASIBLE && value > best[0]) {
                best[0] = value;
            }
            return;
        }
        long capacity = graph.edgeCapacity(index);
        for (long f = 0; f <= capacity; f++) {
            flow[index] = f;
            searchValue(graph, source, sink, index + 1, flow, best);
        }
    }

    private static void searchCost(FlowGraph graph, int source, int sink, long target, int index,
                                   long[] flow, long[] bestCost) {
        if (index == graph.edgeCount()) {
            if (feasibleValue(graph, flow, source, sink) != target) {
                return;
            }
            long cost = 0;
            for (int id = 0; id < graph.edgeCount(); id++) {
                cost += flow[id] * graph.edgeCost(id);
            }
            if (cost < bestCost[0]) {
                bestCost[0] = cost;
            }
            return;
        }
        long capacity = graph.edgeCapacity(index);
        for (long f = 0; f <= capacity; f++) {
            flow[index] = f;
            searchCost(graph, source, sink, target, index + 1, flow, bestCost);
        }
    }

    /** The flow value if conservation holds, else {@link #INFEASIBLE}. */
    private static long feasibleValue(FlowGraph graph, long[] flow, int source, int sink) {
        long[] balance = new long[graph.vertexCount()];
        for (int id = 0; id < graph.edgeCount(); id++) {
            balance[graph.edgeFrom(id)] -= flow[id];
            balance[graph.edgeTo(id)] += flow[id];
        }
        for (int v = 0; v < graph.vertexCount(); v++) {
            if (v != source && v != sink && balance[v] != 0) {
                return INFEASIBLE;
            }
        }
        long value = -balance[source];
        return value >= 0 ? value : INFEASIBLE;
    }
}
