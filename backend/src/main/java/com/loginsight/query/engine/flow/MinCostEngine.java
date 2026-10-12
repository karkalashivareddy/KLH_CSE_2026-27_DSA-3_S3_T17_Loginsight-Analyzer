package com.loginsight.query.engine.flow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.flow.FlowGraph;
import com.loginsight.dsa.flow.MinCostFlowResult;
import com.loginsight.dsa.flow.MinCostMaxFlow;
import com.loginsight.dto.request.MinCostFlowRequest;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.Results;

/**
 * Min-cost max-flow over the supplier→consumer graph (docs/12 §3): every supplier's unit demand
 * ships to a consumer; the engine finds the cheapest total shipment and reports the assignments and
 * their per-edge cost via successive shortest paths (Bellman-Ford here).
 */
public final class MinCostEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.MIN_COST_FLOW;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.MIN_COST_MAX_FLOW;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        MinCostFlowRequest request = (MinCostFlowRequest) context.getRequest();
        if (request.suppliers() == null || request.suppliers().length == 0) {
            throw new InvalidQueryException("suppliers must not be empty");
        }
        if (request.demand() == null || request.demand().length == 0) {
            throw new InvalidQueryException("demand must not be empty");
        }

        int source = 0;
        int sink = 1;
        int supplierBase = 2;
        int consumerBase = supplierBase + request.suppliers().length;
        int vertexCount = consumerBase + request.demand().length;
        Map<String, Integer> supplierIndex = new LinkedHashMap<>();
        for (int i = 0; i < request.suppliers().length; i++) {
            supplierIndex.put(request.suppliers()[i], supplierBase + i);
        }
        Map<String, Integer> consumerIndex = new LinkedHashMap<>();
        for (int i = 0; i < request.demand().length; i++) {
            consumerIndex.put(request.demand()[i], consumerBase + i);
        }

        FlowGraph graph = new FlowGraph(vertexCount);
        for (String supplier : request.suppliers()) {
            graph.addEdge(source, supplierIndex.get(supplier), 1L);
        }
        for (String consumer : request.demand()) {
            graph.addEdge(consumerIndex.get(consumer), sink, 1L);
        }
        for (String[] edge : request.costEdges()) {
            if (edge == null || edge.length < 3) {
                throw new InvalidQueryException("each cost edge must be {from,to,cost}");
            }
            Integer from = supplierIndex.get(edge[0]);
            Integer to = consumerIndex.get(edge[1]);
            if (from == null) {
                throw new InvalidQueryException("unknown supplier '" + edge[0] + "'");
            }
            if (to == null) {
                throw new InvalidQueryException("unknown consumer '" + edge[1] + "'");
            }
            long cost;
            try {
                cost = Long.parseLong(edge[2]);
            } catch (NumberFormatException e) {
                throw new InvalidQueryException("cost of '" + edge[0] + "->" + edge[1]
                        + "' must be a number");
            }
            if (cost < 0) {
                throw new InvalidQueryException("costs must be >= 0");
            }
            graph.addEdge(from, to, 1L, cost);
        }

        long start = System.nanoTime();
        MinCostFlowResult flow = new MinCostMaxFlow().minCostMaxFlow(graph, source, sink);
        long elapsed = System.nanoTime() - start;

        List<Map<String, Object>> assignments = new ArrayList<>();
        for (int id = 0; id < flow.getEdgeCount(); id++) {
            if (flow.flowOf(id) > 0 && !edgeFromSource(id, flow) && !edgeToSink(id, flow)) {
                com.loginsight.dsa.flow.Edge edge = flow.getEdges()[id];
                String from = supplier(edge.getFrom(), request.suppliers());
                String to = consumer(edge.getTo(), request.demand());
                assignments.add(Map.of("from", from, "to", to, "units", flow.flowOf(id),
                        "unitCost", edge.getCost()));
            }
        }
        Map<String, Object> result = Map.of("totalFlow", flow.getTotalFlow(),
                "totalCost", flow.getTotalCost(), "augmentingPaths",
                flow.getAugmentingPathCount(), "assignments", assignments);
        long memory = 16L * flow.getEdgeCount();
        return Results.measured(type(), algorithm(), vertexCount, start, result, assignments,
                memory, flow.getTimeComplexity(), flow.getSpaceComplexity(),
                "successive shortest paths (Bellman-Ford relaxation per augmentation)");
    }

    private static boolean edgeFromSource(int id, MinCostFlowResult flow) {
        com.loginsight.dsa.flow.Edge edge = flow.getEdges()[id];
        return edge.getCost() == 0L && (edge.getFrom() == 0 || edge.getTo() == 1);
    }

    private static boolean edgeToSink(int id, MinCostFlowResult flow) {
        com.loginsight.dsa.flow.Edge edge = flow.getEdges()[id];
        return edge.getTo() == 1;
    }

    private static String supplier(int vertex, String[] suppliers) {
        int index = vertex - 2;
        return index >= 0 && index < suppliers.length ? suppliers[index] : "?";
    }

    private static String consumer(int vertex, String[] consumers) {
        int index = vertex - 2 - consumers.length;
        return index >= 0 && index < consumers.length ? consumers[index] : "?";
    }
}