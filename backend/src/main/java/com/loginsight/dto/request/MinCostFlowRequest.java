package com.loginsight.dto.request;

/**
 * Body of {@code POST /api/flow/min-cost} (docs/12 §3). Suppliers ship unit demand to consumers;
 * {@code costEdges} lists {@code {from,to,cost}} triples. The engine computes the cheapest feasible
 * shipment (min-cost max-flow) and the cost of the current assignment.
 */
public record MinCostFlowRequest(String[] suppliers, String[] demand, String[][] costEdges) {
}