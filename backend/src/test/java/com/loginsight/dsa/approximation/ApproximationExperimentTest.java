package com.loginsight.dsa.approximation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Deterministic, test-only comparison across all three Module 5 vertex-cover tools on fixed small
 * graphs: the exact subset-enumeration oracle, the 2-approximation, and the FPT decision. Output is a
 * plain text table that makes the exact-vs-approximation-vs-FPT trade-off visible and verified at the
 * same time (docs/13 "ApproximationExperimentTest").
 */
class ApproximationExperimentTest {

    private final VertexCoverApproximation approx = new VertexCoverApproximation();
    private final BoundedVertexCover fpt = new BoundedVertexCover();

    @Test
    void exactVsApproximationVsFptTable() {
        List<UndirectedGraph> graphs = new ArrayList<>();
        graphs.add(build(2, new int[][]{{0, 1}}));
        graphs.add(build(3, new int[][]{{0, 1}, {1, 2}}));
        graphs.add(build(4, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}}));
        graphs.add(build(5, new int[][]{{0, 1}, {0, 2}, {0, 3}, {0, 4}}));
        graphs.add(build(6, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 0}}));
        graphs.add(build(4, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {0, 2}}));

        StringWriter buffer = new StringWriter();
        PrintWriter out = new PrintWriter(buffer);
        out.println("instance : n : OPT : approx : ratio : FPT(OPT-1) : FPT(OPT) : FPT(OPT+1)");
        for (int i = 0; i < graphs.size(); i++) {
            UndirectedGraph graph = graphs.get(i);
            int optimum = ApproxTestSupport.exactMinimumVertexCover(graph);
            ApproximationResult result = approx.approximateVertexCover(graph);
            boolean[] coverMask = IndependentSetReduction.selectionMask(graph.vertexCount(), result.getCover());
            assertTrue(IndependentSetReduction.isVertexCover(graph, coverMask), "approx must be a valid cover");
            assertTrue(result.getCoverSize() >= optimum && result.getCoverSize() <= 2 * optimum,
                    "approx must be within [OPT, 2*OPT]");
            double ratio = (double) result.getCoverSize() / optimum;
            assertTrue(ratio >= 1.0 - 1e-9 && ratio <= 2.0 + 1e-9);
            boolean fptTight = fpt.hasVertexCover(graph, optimum - 1);
            boolean fptExact = fpt.hasVertexCover(graph, optimum);
            boolean fptLoose = fpt.hasVertexCover(graph, optimum + 1);
            assertEquals(false, fptTight, "no cover of size OPT-1");
            assertEquals(true, fptExact, "cover of size OPT exists");
            assertEquals(true, fptLoose, "cover of size OPT+1 exists");
            int[] certificate = fpt.findVertexCover(graph, optimum + 1);
            assertNotNull(certificate);
            assertTrue(certificate.length <= optimum + 1);
            out.println("G" + i + " : " + graph.vertexCount() + " : " + optimum + " : "
                    + result.getCoverSize() + " : " + String.format("%.3f", ratio) + " : "
                    + fptTight + " : " + fptExact + " : " + fptLoose);
        }
        out.flush();
        String table = buffer.toString();
        assertTrue(table.contains("G0"), "row for the single-edge graph is printed");
        assertTrue(table.contains("FPT(OPT-1)"), "header row is printed");
    }

    private static UndirectedGraph build(int n, int[][] edges) {
        UndirectedGraph graph = new UndirectedGraph(n);
        for (int[] edge : edges) {
            graph.addEdge(edge[0], edge[1]);
        }
        return graph;
    }
}