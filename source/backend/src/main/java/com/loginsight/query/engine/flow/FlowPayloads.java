package com.loginsight.query.engine.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.flow.Edge;
import com.loginsight.dsa.flow.FlowResult;
import com.loginsight.model.QueryResult;
import com.loginsight.query.Results;

/**
 * Builds the wire-ready payload map shared by every max-flow engine: the flow value, the named
 * terminal ids, and the per-original-edge {@code from/to/capacity/flow/residual} detail.
 */
final class FlowPayloads {

    private FlowPayloads() {
    }

    static QueryResult wrap(FlowGraphFactory.NamedFlow named, FlowResult flow,
                            com.loginsight.model.QueryType type,
                            com.loginsight.model.AlgorithmType algorithm) {
        long memory = 16L * named.graph().edgeCount();
        List<Map<String, Object>> edges = new ArrayList<>();
        for (int id = 0; id < flow.getEdgeCount(); id++) {
            Edge edge = flow.getEdges()[id];
            edges.add(Map.of("from", named.names()[edge.getFrom()],
                    "to", named.names()[edge.getTo()], "capacity", edge.getCapacity(),
                    "flow", flow.flowOf(edge.getId()), "residual", flow.residualOf(edge.getId())));
        }
        Map<String, Object> result = Map.of("maxFlow", flow.getMaxFlow(),
                "source", named.names()[flow.getSource()], "sink", named.names()[flow.getSink()],
                "vertexCount", flow.getVertexCount(), "augmentingPaths",
                flow.getAugmentationCount(), "edges", edges);
        return Results.timed(type, algorithm, flow.getVertexCount(), flow.getExecutionTimeNanos(),
                result, edges, memory, flow.getTimeComplexity(), flow.getSpaceComplexity(),
                flow.getAugmentationCount() + " augmenting paths discovered");
    }
}