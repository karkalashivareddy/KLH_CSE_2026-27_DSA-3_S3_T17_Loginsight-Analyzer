package com.loginsight.dsa.parallel;

import java.util.Arrays;

/**
 * Derives <b>work</b>, <b>span</b> and <b>parallelism</b> from an explicit task schedule.
 *
 * <p>A schedule is a tree of {@link Task} nodes.  Every node has a unit cost representing the
 * sequential work of that task.  The node's children either run <b>in parallel</b> with each
 * other (the node's span is the maximum child span) or <b>sequentially</b> (the node's span is
 * the sum of child spans).  This mirrors how the parallel engine actually decomposes work, and
 * lets a viva trace the displayed work/span numbers back to a concrete task tree.</p>
 *
 * <ul>
 *   <li><b>Work</b> = sum of every task cost (total number of unit operations).</li>
 *   <li><b>Span</b> = the critical path: the longest chain of dependent work along which no
 *       parallelism helps (with parallel children taking only the max).</li>
 *   <li><b>Parallelism</b> = work / span = the average number of operations that could be in
 *       flight, i.e. an upper bound on achievable speedup.</li>
 * </ul>
 */
public final class WorkSpanAnalyzer {

    /** A single atomic unit of work in a schedule, with optional children. */
    public static final class Task {
        private final String name;
        private final long cost;
        private final Task[] children;
        private final boolean childrenInParallel;

        private Task(String name, long cost, Task[] children, boolean childrenInParallel) {
            this.name = name;
            this.cost = cost;
            this.children = children;
            this.childrenInParallel = childrenInParallel;
        }

        /** A leaf task with no children. */
        public static Task leaf(String name, long cost) {
            if (name == null) {
                throw new IllegalArgumentException("task name must not be null");
            }
            if (cost < 0) {
                throw new IllegalArgumentException("task cost must be >= 0, got " + cost);
            }
            return new Task(name, cost, new Task[0], false);
        }

        /** A node whose children run in parallel (span takes the maximum child span). */
        public static Task parallelNode(String name, long cost, Task... children) {
            if (name == null) {
                throw new IllegalArgumentException("task name must not be null");
            }
            if (cost < 0) {
                throw new IllegalArgumentException("task cost must be >= 0, got " + cost);
            }
            if (children == null) {
                throw new IllegalArgumentException("children must not be null");
            }
            return new Task(name, cost, children.clone(), true);
        }

        /** A node whose children run sequentially one after another (span sums them). */
        public static Task sequentialNode(String name, long cost, Task... children) {
            if (name == null) {
                throw new IllegalArgumentException("task name must not be null");
            }
            if (cost < 0) {
                throw new IllegalArgumentException("task cost must be >= 0, got " + cost);
            }
            if (children == null) {
                throw new IllegalArgumentException("children must not be null");
            }
            return new Task(name, cost, children.clone(), false);
        }

        /** Builds a balanced binary parallel-reduce tree over {@code leaves} leaves. */
        public static Task binaryReduceTree(String name, int leaves, long leafCost, long internalCost) {
            if (leaves < 1) {
                throw new IllegalArgumentException("leaves must be >= 1, got " + leaves);
            }
            if (leafCost < 0 || internalCost < 0) {
                throw new IllegalArgumentException("costs must be >= 0");
            }
            Task[] level = new Task[leaves];
            for (int i = 0; i < leaves; i++) {
                level[i] = Task.leaf(name + ".leaf" + i, leafCost);
            }
            while (level.length > 1) {
                int next = ParallelSupport.ceilDiv(level.length, 2);
                Task[] parent = new Task[next];
                for (int i = 0; i < next; i++) {
                    if (2 * i + 1 < level.length) {
                        parent[i] = Task.parallelNode(name + ".reduce" + i, internalCost,
                                level[2 * i], level[2 * i + 1]);
                    } else {
                        parent[i] = level[2 * i];
                    }
                }
                level = parent;
            }
            return Task.parallelNode(name, 0, level[0]);
        }
    }

    /** Work/span result with the critical path as a readable chain. */
    public static final class WorkSpanResult {
        private final long work;
        private final long span;
        private final double parallelism;
        private final String criticalPath;

        private WorkSpanResult(long work, long span, double parallelism, String criticalPath) {
            this.work = work;
            this.span = span;
            this.parallelism = parallelism;
            this.criticalPath = criticalPath;
        }

        public long getWork() {
            return work;
        }

        public long getSpan() {
            return span;
        }

        /** work / span — maximum achievable speedup for this schedule. */
        public double getParallelism() {
            return parallelism;
        }

        /** Names along the critical path, e.g. {@code "upSweep >= reduce2 >= leaf0"}. */
        public String getCriticalPath() {
            return criticalPath;
        }
    }

    private WorkSpanAnalyzer() {
    }

    /** Total work of a schedule: every cost summed once. */
    public static long work(Task t) {
        long total = t.cost;
        for (Task child : t.children) {
            total += work(child);
        }
        return total;
    }

    /** Critical-path span of a schedule. */
    public static long span(Task t) {
        if (t.children.length == 0) {
            return t.cost;
        }
        long childrenSpan = 0;
        if (t.childrenInParallel) {
            for (Task child : t.children) {
                childrenSpan = Math.max(childrenSpan, span(child));
            }
        } else {
            for (Task child : t.children) {
                childrenSpan += span(child);
            }
        }
        return t.cost + childrenSpan;
    }

    /** Computes work, span, parallelism ratio and the critical-path chain of a schedule. */
    public static WorkSpanResult analyze(Task t) {
        if (t == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        long w = work(t);
        long s = span(t);
        String path = criticalPath(t);
        return new WorkSpanResult(w, s, s == 0 ? 1.0 : (double) w / s, path);
    }

    private static String criticalPath(Task t) {
        if (t.children.length == 0) {
            return t.name;
        }
        String deepest = null;
        if (t.childrenInParallel) {
            long maxSpan = -1;
            for (Task child : t.children) {
                long cs = span(child);
                if (cs > maxSpan) {
                    maxSpan = cs;
                    deepest = criticalPath(child);
                }
            }
        } else {
            String[] parts = new String[t.children.length];
            long[] spans = new long[t.children.length];
            for (int i = 0; i < t.children.length; i++) {
                spans[i] = span(t.children[i]);
                parts[i] = criticalPath(t.children[i]);
            }
            // longest child chain (the critical path is the dependency sum; the names of all
            // children appear, but the "critical" name is a tie chose from the longest child).
            int maxIdx = 0;
            for (int i = 1; i < spans.length; i++) {
                if (spans[i] > spans[maxIdx]) {
                    maxIdx = i;
                }
            }
            deepest = parts[maxIdx];
        }
        return t.name + " >= " + deepest;
    }
}