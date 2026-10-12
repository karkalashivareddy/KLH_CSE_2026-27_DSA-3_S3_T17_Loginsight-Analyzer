package com.loginsight.dsa.dp.bitmask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.SmallRandom;
import org.junit.jupiter.api.Test;

class HamiltonianPathTest {

    @Test
    void pathGraphHasHamiltonianPathFromEndpoint() {
        HamiltonianPath graph = new HamiltonianPath(4);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        HamiltonianPathResult result = graph.find(0);
        assertTrue(result.exists());
        assertValidPath(4, result.getPath());
        assertEquals(0, result.getPath()[0]);
    }

    @Test
    void startMattersOnAPathGraph() {
        HamiltonianPath graph = new HamiltonianPath(4);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        assertFalse(graph.find(2).exists(), "an interior vertex cannot cover both directions");
    }

    @Test
    void findAnySucceedsOnConnectedPath() {
        HamiltonianPath graph = new HamiltonianPath(4);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        assertTrue(graph.findAny().exists());
    }

    @Test
    void completeGraphAlwaysHasPath() {
        HamiltonianPath graph = new HamiltonianPath(5);
        for (int u = 0; u < 5; u++) {
            for (int v = u + 1; v < 5; v++) {
                graph.addEdge(u, v);
            }
        }
        for (int start = 0; start < 5; start++) {
            assertTrue(graph.find(start).exists());
            assertValidPath(5, graph.find(start).getPath());
        }
    }

    @Test
    void triangleWorks() {
        HamiltonianPath graph = new HamiltonianPath(3);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 0);
        assertTrue(graph.findAny().exists());
    }

    @Test
    void noEdgesMeansNoPath() {
        HamiltonianPath graph = new HamiltonianPath(3);
        assertFalse(graph.findAny().exists());
    }

    @Test
    void singleVertex() {
        HamiltonianPath graph = new HamiltonianPath(1);
        HamiltonianPathResult result = graph.find(0);
        assertTrue(result.exists());
        assertEquals(1, result.getPath().length);
        assertEquals(0, result.getPath()[0]);
    }

    @Test
    void edgeHelpers() {
        HamiltonianPath graph = new HamiltonianPath(3);
        graph.addEdge(new int[]{0, 1}, new int[]{1, 2});
        assertTrue(graph.hasEdge(0, 1));
        assertTrue(graph.hasEdge(1, 0));
        assertTrue(graph.hasEdge(1, 2));
        assertFalse(graph.hasEdge(0, 2));
        assertEquals(3, graph.vertexCount());
    }

    @Test
    void matchesBruteForceOnRandomGraphs() {
        SmallRandom random = new SmallRandom(31337L);
        for (int trial = 0; trial < 40; trial++) {
            int n = 1 + random.nextInt(6);
            boolean[][] adj = new boolean[n][n];
            HamiltonianPath graph = new HamiltonianPath(n);
            for (int u = 0; u < n; u++) {
                for (int v = u + 1; v < n; v++) {
                    if (random.nextInt(3) == 0) {
                        graph.addEdge(u, v);
                        adj[u][v] = true;
                        adj[v][u] = true;
                    }
                }
            }
            for (int start = 0; start < n; start++) {
                assertEquals(bruteForce(adj, start), graph.find(start).exists(),
                        "trial " + trial + " start " + start);
            }
            assertEquals(bruteForceAny(adj), graph.findAny().exists(), "trial " + trial);
        }
    }

    @Test
    void envelopeView() {
        HamiltonianPath graph = new HamiltonianPath(3);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        DpResult result = graph.match();
        assertEquals("Hamiltonian path", result.getAlgorithm());
        assertEquals(3, result.getInputSize());
        assertEquals(Boolean.TRUE, result.getResult());
        assertTrue(result.getIntermediateData() instanceof int[]);
    }

    @Test
    void rejectsInvalidConstructionAndEdges() {
        assertThrows(IllegalArgumentException.class, () -> new HamiltonianPath(0));
        assertThrows(IllegalArgumentException.class,
                () -> new HamiltonianPath(HamiltonianPath.MAX_VERTICES + 1));
        HamiltonianPath graph = new HamiltonianPath(3);
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(0, 0));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(0, 3));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> graph.find(3));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(null, new int[]{0}));
    }

    private static void assertValidPath(int n, int[] path) {
        assertEquals(n, path.length);
        boolean[] seen = new boolean[n];
        for (int city : path) {
            assertFalse(seen[city], "vertex repeated: " + city);
            seen[city] = true;
        }
    }

    private static boolean bruteForce(boolean[][] adj, int start) {
        int n = adj.length;
        boolean[] used = new boolean[n];
        used[start] = true;
        return search(adj, start, 1, used);
    }

    private static boolean search(boolean[][] adj, int last, int count, boolean[] used) {
        int n = adj.length;
        if (count == n) {
            return true;
        }
        for (int next = 0; next < n; next++) {
            if (!used[next] && adj[last][next]) {
                used[next] = true;
                if (search(adj, next, count + 1, used)) {
                    used[next] = false;
                    return true;
                }
                used[next] = false;
            }
        }
        return false;
    }

    private static boolean bruteForceAny(boolean[][] adj) {
        int n = adj.length;
        for (int start = 0; start < n; start++) {
            if (bruteForce(adj, start)) {
                return true;
            }
        }
        return false;
    }
}
