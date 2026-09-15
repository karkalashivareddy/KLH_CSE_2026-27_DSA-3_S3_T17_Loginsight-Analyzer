package com.loginsight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loginsight.dsa.approximation.UndirectedGraph;
import com.loginsight.dsa.approximation.VertexCoverApproximation;
import com.loginsight.dsa.dp.editdistance.LevenshteinDistance;
import com.loginsight.dsa.dp.interval.MatrixChainMultiplication;
import com.loginsight.dsa.flow.Dinic;
import com.loginsight.dsa.flow.EdmondsKarp;
import com.loginsight.dsa.flow.FlowGraph;
import com.loginsight.dsa.flow.FlowResult;
import com.loginsight.dsa.flow.FordFulkerson;
import com.loginsight.dsa.randomized.MillerRabin;
import com.loginsight.dsa.randomized.RandomSource;
import com.loginsight.dsa.randomized.RandomizedQuickSort;
import com.loginsight.dsa.randomized.ReservoirSampling;
import com.loginsight.dsa.string.KMPMatcher;
import com.loginsight.dsa.string.NaiveMatcher;
import com.loginsight.dsa.string.RabinKarpMatcher;
import com.loginsight.dsa.string.ZAlgorithm;
import com.loginsight.dsa.string.StringSearchResult;
import com.loginsight.trace.TracedResult;

/**
 * Cross-check that every trace-capable execution path produces exactly the same answer as its
 * untraced sibling on the same input. The step recording must never change the algorithm outcome —
 * the trace is a lens, not a fork.
 */
class TraceCrossCheckTest {

    @Test
    void naiveKmpZAndRabinKarpAgree() {
        String text = "ABABABCABABABCABABABC";
        String pattern = "ABABC";
        int expected = new NaiveMatcher().match(text, pattern).getMatchCount();
        assertEquals(expected, tracedCount(new KMPMatcher().matchTracked(text, pattern)));
        assertEquals(expected, tracedCount(new ZAlgorithm().matchTracked(text, pattern)));
        assertEquals(expected, tracedCount(new RabinKarpMatcher()
                .matchTracked(text, pattern, false)));
        assertEquals(expected, tracedCount(new RabinKarpMatcher()
                .matchTracked(text, pattern, true)));
    }

    @SuppressWarnings("unchecked")
    private static int tracedCount(TracedResult traced) {
        return ((Map<String, Integer>) traced.result()).get("matchCount");
    }

    @Test
    void levenshteinTracedMatchesUntraced() {
        LevenshteinDistance d = new LevenshteinDistance();
        long expected = d.distance("kitten", "sitting");
        TracedResult traced = d.matchTracked("kitten", "sitting");
        @SuppressWarnings("unchecked")
        long actual = ((Map<String, Object>) traced.result()).get("distance")
                instanceof Number n ? n.longValue() : -1L;
        assertEquals(expected, actual);
        assertTrue(traced.steps().size() > 0);
    }

    @Test
    void matrixChainTracedMatchesUntraced() {
        MatrixChainMultiplication m = new MatrixChainMultiplication();
        int[] dims = {10, 20, 30, 40, 50};
        long expected = m.minCost(dims);
        TracedResult traced = m.solveTracked(dims);
        @SuppressWarnings("unchecked")
        long actual = ((Map<String, Object>) traced.result()).get("minCost")
                instanceof Number n ? n.longValue() : -1L;
        assertEquals(expected, actual);
        assertTrue(traced.steps().size() > 0);
    }

    @Test
    void maxFlowTracedMatchesUntracedForAllThree() {
        FlowGraph graph = sampleFlow();
        long expected = new FordFulkerson().maxFlow(graph, 0, 3).getMaxFlow();

        long ff = tracedFlow(new FordFulkerson().maxFlowTracked(graph, 0, 3));
        long ek = tracedFlow(new EdmondsKarp().maxFlowTracked(graph, 0, 3));
        long dinic = tracedFlow(new Dinic().maxFlowTracked(graph, 0, 3));
        assertEquals(expected, ff);
        assertEquals(expected, ek);
        assertEquals(expected, dinic);
    }

    @SuppressWarnings("unchecked")
    private static long tracedFlow(TracedResult traced) {
        return ((Map<String, Object>) traced.result()).get("maxFlow") instanceof Number n
                ? n.longValue() : -1L;
    }

    private static FlowGraph sampleFlow() {
        FlowGraph g = new FlowGraph(4);
        g.addEdge(0, 1, 3);
        g.addEdge(0, 2, 1);
        g.addEdge(1, 3, 2);
        g.addEdge(2, 3, 3);
        return g;
    }

    @Test
    void vertexCoverTracedMatchesUntraced() {
        UndirectedGraph graph = new UndirectedGraph(4);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        VertexCoverApproximation approx = new VertexCoverApproximation();
        int expected = approx.approximateVertexCover(graph).getCoverSize();
        TracedResult traced = approx.approximateTracked(graph);
        @SuppressWarnings("unchecked")
        int actual = ((Map<String, Object>) traced.result()).get("coverSize")
                instanceof Number n ? n.intValue() : -1;
        assertEquals(expected, actual);
        assertTrue(traced.steps().size() > 0);
    }

    @Test
    void quicksortTracedMatchesUntraced() {
        RandomizedQuickSort q = new RandomizedQuickSort();
        long[] input = {5, 1, 4, 2, 8, 0, 9, 3, 6, 7};
        RandomSource rng = RandomSource.seeded(42);
        long[] expected = q.sort(input.clone(), rng);
        TracedResult traced = q.sortTracked(input.clone(), RandomSource.seeded(42));
        List<?> sorted = traced.result() instanceof Map<?, ?> map
                && map.get("sorted") instanceof List<?> list ? list : List.of();
        assertEquals(expected.length, sorted.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], ((Number) sorted.get(i)).longValue());
        }
    }

    @Test
    void millerRabinTracedMatchesUntraced() {
        long[] tests = {97L, 121L, 2L, 561L, 999983L};
        for (long n : tests) {
            TracedResult deterministic = new MillerRabin().testTracked(n,
                    MillerRabin.Mode.DETERMINISTIC, RandomSource.seeded(1), 1);
            boolean tracedPrime = ((Map<?, ?>) deterministic.result()).get("prime") == Boolean.TRUE;
            assertEquals(new MillerRabin().isDefinitePrime(n), tracedPrime);
        }
    }

    @Test
    void reservoirTracedHasExpectedSize() {
        long[] input = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        TracedResult traced = new ReservoirSampling(3, RandomSource.seeded(7))
                .sampleTracked(input);
        assertEquals(3, ((Map<?, ?>) traced.result()).get("sample") instanceof List<?> l
                ? l.size() : -1);
    }
}