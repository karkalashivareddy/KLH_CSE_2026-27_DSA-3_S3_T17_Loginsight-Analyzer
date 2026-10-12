package com.loginsight.dsa.flow;

/**
 * Shared argument checks for the network-flow engine. Invalid input must surface as
 * {@link IllegalArgumentException} at the boundary, never as an index error deep inside a residual
 * update.
 *
 * <p>Deliberately tiny — not a validation framework.</p>
 */
final class FlowValidator {

    private FlowValidator() {
    }

    static void requireNonNull(Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                throw new IllegalArgumentException("argument " + i + " must not be null");
            }
        }
    }

    static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0 but was " + value);
        }
    }

    static void requireInRange(int value, int lowInclusive, int highInclusive, String name) {
        if (value < lowInclusive || value > highInclusive) {
            throw new IllegalArgumentException(name + " must be in [" + lowInclusive + ", "
                    + highInclusive + "] but was " + value);
        }
    }

    /**
     * Validates a (graph, source, sink) triple shared by every max-flow entry point: non-null graph,
     * source and sink in range, and {@code source != sink}.
     */
    static void requireFlowEndpoint(FlowGraph graph, int source, int sink) {
        requireNonNull(graph);
        int n = graph.vertexCount();
        if (n == 0) {
            throw new IllegalArgumentException("graph must contain at least one vertex");
        }
        requireInRange(source, 0, n - 1, "source");
        requireInRange(sink, 0, n - 1, "sink");
        if (source == sink) {
            throw new IllegalArgumentException("source and sink must differ (both " + source + ")");
        }
    }
}
