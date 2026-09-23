# 07 - Network Flow (theory supplement)

Honest register of flow topics — implemented vs conceptual.

## Implemented (real engines, trace-instrumented)

| Algorithm | Key | Endpoint | Notes |
|---|---|---|---|
| Ford-Fulkerson (DFS augmenting paths) | `fordfulkerson` | `POST /api/flow/ford-fulkerson` · `POST /api/trace/flow/fordfulkerson` | traces every augmenting path + bottleneck |
| Edmonds-Karp (BFS, shortest paths) | `edmondskarp` | `POST /api/flow/edmonds-karp` · `POST /api/trace/flow/edmondskarp` | BFS paths |
| Dinic (level graphs + blocking flow) | `dinic` | `POST /api/flow/dinic` · `POST /api/trace/flow/dinic` | phase-level trace |
| Min Cut (max-flow value) | `mincut` | `POST /api/flow/min-cut` | sourceSide / sinkSide / cutEdges / cutCapacity |
| Bipartite Matching | `bipartitematching` | `POST /api/flow/matching` | O(E√V) |
| Min-Cost Max-Flow (SSP) | `mincostflow` | `POST /api/flow/min-cost-flow` | O(f_max · V · E) |

`FlowCrossCheckTest` proves Ford-Fulkerson == Edmonds-Karp == Dinic on the same graphs. Dinic's
reported complexity is `O(V²E)`.

## Conceptual (documented, not implemented)

- **Cycle-cancelling min-cost flow** (successive-shortest-path is implemented instead). Equivalent
  optimality guarantee; cycle-cancelling is a roadmap alternative for demonstrating negative-cost
  cycle detection on the residual graph.

## Lab map

Frontend: Laboratory group **Graph & Flow** (3 traceable). TextHack "Dependency Flow" executes the
Dinic engine and proposes `dinic` → `edmondskarp` → `fordfulkerson` → `min_cut` lab jumps.