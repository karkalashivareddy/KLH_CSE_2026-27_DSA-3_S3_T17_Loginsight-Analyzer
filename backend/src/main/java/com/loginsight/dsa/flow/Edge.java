package com.loginsight.dsa.flow;

/**
 * Immutable definition of one <strong>original directed edge</strong> of a {@link FlowGraph}: its id,
 * endpoints, capacity and per-unit cost. It carries no flow value — flow belongs to a particular
 * run's {@link FlowResult} (or {@link MinCostFlowResult}), which reports a per-edge flow array aligned
 * to {@link #getId()}.
 *
 * <p>Distinguishing the three quantities is deliberate (docs/02 §7):</p>
 * <ul>
 *   <li>{@code capacity} — the original capacity {@code c(e)};</li>
 *   <li>{@code flow} — provided by the result, always {@code 0 <= f(e) <= c(e)};</li>
 *   <li>{@code residualCapacity} — provided by the result as {@code c(e) - f(e)} for this forward
 *       edge (the reverse residual edge carries {@code f(e)}).</li>
 * </ul>
 */
public final class Edge {

    private final int id;
    private final int from;
    private final int to;
    private final long capacity;
    private final long cost;

    Edge(int id, int from, int to, long capacity, long cost) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.capacity = capacity;
        this.cost = cost;
    }

    /** Index into the owning graph's edge list; also the index into a result's flow array. */
    public int getId() {
        return id;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public long getCapacity() {
        return capacity;
    }

    /** Per-unit cost used only by {@link MinCostMaxFlow}; {@code 0} for plain max-flow graphs. */
    public long getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "e" + id + "(" + from + "->" + to + ", cap=" + capacity
                + (cost != 0 ? ", cost=" + cost : "") + ")";
    }
}
