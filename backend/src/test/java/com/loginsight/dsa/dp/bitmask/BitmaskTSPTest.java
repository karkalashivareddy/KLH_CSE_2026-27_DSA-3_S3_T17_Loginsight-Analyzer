package com.loginsight.dsa.dp.bitmask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loginsight.dsa.dp.DpResult;
import com.loginsight.dsa.dp.SmallRandom;
import org.junit.jupiter.api.Test;

class BitmaskTSPTest {

    private static final long INF = BitmaskTSP.INF;

    @Test
    void classicFourCityTour() {
        long[][] dist = {
                {0, 10, 15, 20},
                {10, 0, 35, 25},
                {15, 35, 0, 30},
                {20, 25, 30, 0}
        };
        TspResult result = new BitmaskTSP(dist, 0).solve();
        assertTrue(result.hasTour());
        assertEquals(80, result.getMinCost());
        assertValidTour(dist, 0, result);
    }

    @Test
    void singleCityIsFreeTour() {
        TspResult result = new BitmaskTSP(new long[][]{{0}}, 0).solve();
        assertTrue(result.hasTour());
        assertEquals(0, result.getMinCost());
        assertEquals(1, result.getPath().length);
        assertEquals(0, result.getPath()[0]);
    }

    @Test
    void noTourWhenClosingEdgeMissing() {
        long[][] dist = {
                {0, 10, -1},
                {10, 0, 5},
                {-1, 5, 0}
        };
        TspResult result = new BitmaskTSP(dist, 0).solve();
        assertFalse(result.hasTour());
        assertEquals(INF, result.getMinCost());
        assertEquals(0, result.getPath().length);
    }

    @Test
    void startCityRespected() {
        long[][] dist = {
                {0, 3, 7, 2},
                {3, 0, 4, 9},
                {7, 4, 0, 5},
                {2, 9, 5, 0}
        };
        TspResult result = new BitmaskTSP(dist, 2).solve();
        assertTrue(result.hasTour());
        assertValidTour(dist, 2, result);
    }

    @Test
    void matchesBruteForceOnRandomSmallInputs() {
        SmallRandom random = new SmallRandom(777001L);
        for (int trial = 0; trial < 25; trial++) {
            int n = 2 + random.nextInt(5);
            long[][] dist = new long[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    long w = random.nextInt(40);
                    dist[i][j] = w;
                    dist[j][i] = w;
                }
            }
            int start = random.nextInt(n);
            long expected = bruteForce(dist, start);
            TspResult result = new BitmaskTSP(dist, start).solve();
            if (expected >= INF) {
                assertFalse(result.hasTour(), "trial " + trial + " should have no tour");
            } else {
                assertTrue(result.hasTour(), "trial " + trial + " should have a tour");
                assertEquals(expected, result.getMinCost(), "trial " + trial);
                assertValidTour(dist, start, result);
            }
        }
    }

    @Test
    void deterministicResult() {
        long[][] dist = {
                {0, 10, 15, 20},
                {10, 0, 35, 25},
                {15, 35, 0, 30},
                {20, 25, 30, 0}
        };
        BitmaskTSP tsp = new BitmaskTSP(dist, 0);
        assertEquals(tsp.solve().getMinCost(), tsp.solve().getMinCost());
    }

    @Test
    void envelopeView() {
        long[][] dist = {
                {0, 10, 15, 20},
                {10, 0, 35, 25},
                {15, 35, 0, 30},
                {20, 25, 30, 0}
        };
        DpResult result = new BitmaskTSP(dist, 0).match();
        assertEquals("Bitmask TSP", result.getAlgorithm());
        assertEquals(4, result.getInputSize());
        assertEquals(80, result.resultAsLong());
        assertTrue(result.getIntermediateData() instanceof int[]);
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new BitmaskTSP(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new BitmaskTSP(new long[0][0], 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BitmaskTSP(new long[][]{{0, 1}, {1}}, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BitmaskTSP(new long[][]{{0, 1}, {1, -5}}, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BitmaskTSP(new long[][]{{0, 1}, {1, 0}}, 2));
        long[][] tooBig = new long[BitmaskTSP.MAX_CITIES + 1][BitmaskTSP.MAX_CITIES + 1];
        assertThrows(IllegalArgumentException.class, () -> new BitmaskTSP(tooBig, 0));
    }

    private static void assertValidTour(long[][] dist, int start, TspResult result) {
        int n = dist.length;
        int[] path = result.getPath();
        assertEquals(n, path.length, "path must cover every city");
        assertEquals(start, path[0], "path must start at the configured city");
        boolean[] seen = new boolean[n];
        long cost = 0;
        for (int i = 0; i < n; i++) {
            int city = path[i];
            assertFalse(seen[city], "city visited twice: " + city);
            seen[city] = true;
            int next = path[(i + 1) % n];
            assertTrue(dist[city][next] != BitmaskTSP.UNREACHABLE, "path uses an unreachable edge");
            cost += dist[city][next];
        }
        assertEquals(result.getMinCost(), cost, "recomputed path cost must equal reported minimum");
    }

    private static long bruteForce(long[][] dist, int start) {
        int n = dist.length;
        boolean[] used = new boolean[n];
        used[start] = true;
        return search(dist, start, start, 1, used);
    }

    private static long search(long[][] dist, int start, int last, int count, boolean[] used) {
        int n = dist.length;
        if (count == n) {
            return dist[last][start] == BitmaskTSP.UNREACHABLE ? INF : dist[last][start];
        }
        long best = INF;
        for (int next = 0; next < n; next++) {
            if (used[next] || dist[last][next] == BitmaskTSP.UNREACHABLE) {
                continue;
            }
            used[next] = true;
            long candidate = dist[last][next] + search(dist, start, next, count + 1, used);
            used[next] = false;
            if (candidate < best) {
                best = candidate;
            }
        }
        return best;
    }
}
