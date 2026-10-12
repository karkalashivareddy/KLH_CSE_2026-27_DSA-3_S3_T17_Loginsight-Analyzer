package com.loginsight.query.engine.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.flow.Edge;
import com.loginsight.dsa.flow.MinCutResult;
import com.loginsight.dsa.flow.MinCut;
import com.loginsight.dto.request.FlowRequest;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.Results;

/**
 * Min-cut analysis (docs/12 §3): after max-flow terminates, the S-side is every vertex still
 * reachable from the source in the residual network; the cut edges are exactly the original edges
 * crossing that boundary. The cut capacity is reported as part of the payload.
 */
public final class MinCutEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.MIN_CUT;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.MIN_CUT;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        FlowGraphFactory.NamedFlow named = FlowGraphFactory.build((FlowRequest) context.getRequest());
        MinCutResult cut = new MinCut().minCut(named.graph(), named.source(), named.sink());

        List<String> sourceSide = new ArrayList<>();
        List<String> sinkSide = new ArrayList<>();
        for (int v = 0; v < cut.getVertexCount(); v++) {
            if (cut.isOnSourceSide(v)) {
                sourceSide.add(named.names()[v]);
            } else {
                sinkSide.add(named.names()[v]);
            }
        }
        List<Map<String, Object>> cutEdges = new ArrayList<>();
        for (Edge edge : cut.getCutEdges()) {
            cutEdges.add(Map.of("from", named.names()[edge.getFrom()],
                    "to", named.names()[edge.getTo()], "capacity", edge.getCapacity()));
        }
        Map<String, Object> result = Map.of("cutCapacity", cut.getCutCapacity(),
                "sourceSide", sourceSide, "sinkSide", sinkSide, "cutEdges", cutEdges,
                "source", named.names()[cut.getSource()], "sink", named.names()[cut.getSink()]);
        long memory = 16L * named.graph().edgeCount();
        return Results.timed(type(), algorithm(), cut.getVertexCount(), cut.getExecutionTimeNanos(),
                result, Map.of("sourceSideMask", cut.sourceSide()), memory,
                cut.getTimeComplexity(), "O(V) reachability flags",
                "cut capacity " + cut.getCutCapacity() + " (min-cut = max-flow)");
    }
}