package com.loginsight.dsa.parallel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Cross-check and evidence tests (docs/04 Module 6B "Verification"):
 * parallel reduce/scan/sort must equal their sequential counterparts on shared datasets —
 * including inputs below the sequential threshold — and the work/span analyzer plus benchmark
 * must produce sane, deterministic evidence.
 */
class ParallelCrossCheckTest {

    private static final int[] SHARED_SIZES = {
            0, 1, 2, 15, 100, 1024, 1025, 4096, 10_000, 50_001
    };

    private static long[] data(int n, long seed) {
        long[] a = new long[n];
        java.util.Random rng = new java.util.Random(seed);
        for (int i = 0; i < n; i++) {
            a[i] = rng.nextLong(100);
        }
        return a;
    }

    // --- reduce == sequential on shared datasets incl. n below threshold ---

    @Test
    void reduceEqualsSequentialOnSharedDatasets() {
        for (int n : SHARED_SIZES) {
            long[] a = data(n, 100 + n);
            assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.SUM),
                    ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, 4),
                    "SUM n=" + n);
            if (n == 0) {
                assertThrows(IllegalArgumentException.class,
                        () -> ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.MAX));
            } else {
                assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.MAX),
                        ParallelReduce.reduce(a, ParallelReduce.ReduceOp.MAX, 4),
                        "MAX n=" + n);
            }
            assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.COUNT),
                    ParallelReduce.reduce(a, ParallelReduce.ReduceOp.COUNT, 4),
                    "COUNT n=" + n);
            assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.ERROR_COUNT, 42),
                    ParallelReduce.reduce(a, ParallelReduce.ReduceOp.ERROR_COUNT, 42, 4),
                    "ERROR_COUNT n=" + n);
        }
    }

    @Test
    void reduceEqualsSequentialBelowThreshold() {
        long[] a = data(128, 7L);
        assertEquals(ParallelReduce.reduceSequential(a, ParallelReduce.ReduceOp.SUM),
                ParallelReduce.reduce(a, ParallelReduce.ReduceOp.SUM, 8));
    }

    // --- scan == sequential on shared datasets incl. n below threshold ---

    @Test
    void scanEqualsSequentialOnSharedDatasets() {
        for (int n : SHARED_SIZES) {
            long[] a = data(n, 200 + n);
            assertArrayEquals(ParallelPrefixScan.inclusiveSequential(a),
                    ParallelPrefixScan.inclusive(a, 4), "inclusive n=" + n);
            assertArrayEquals(ParallelPrefixScan.exclusiveSequential(a),
                    ParallelPrefixScan.exclusive(a, 4), "exclusive n=" + n);
        }
    }

    @Test
    void scanBelowThresholdFallsBack() {
        long[] a = data(900, 8L);
        assertArrayEquals(ParallelPrefixScan.exclusiveSequential(a),
                ParallelPrefixScan.exclusive(a, 16));
    }

    // --- sort == sequential on shared datasets incl. n below threshold ---

    @Test
    void sortEqualsSequentialOnSharedDatasets() {
        for (int n : SHARED_SIZES) {
            long[] a = data(n, 300 + n);
            assertArrayEquals(ParallelSort.sortSequential(a), ParallelSort.sortParallel(a, 4),
                    "sort n=" + n);
        }
    }

    @Test
    void sortEqualsGoldStandardOracle() {
        long[] a = data(8192, 9L);
        long[] oracle = a.clone();
        java.util.Arrays.sort(oracle);
        assertArrayEquals(oracle, ParallelSort.sortParallel(a, 8));
    }

    // --- WorkSpanAnalyzer: schedule-derived work/span ---

    @Test
    void leafWorkAndSpan() {
        WorkSpanAnalyzer.Task t = WorkSpanAnalyzer.Task.leaf("load", 5);
        assertEquals(5, WorkSpanAnalyzer.work(t));
        assertEquals(5, WorkSpanAnalyzer.span(t));
        assertEquals(1.0, WorkSpanAnalyzer.analyze(t).getParallelism());
        assertEquals("load", WorkSpanAnalyzer.analyze(t).getCriticalPath());
    }

    @Test
    void parallelChildrenTakeMaxSpan() {
        WorkSpanAnalyzer.Task t = WorkSpanAnalyzer.Task.parallelNode("p", 2,
                WorkSpanAnalyzer.Task.leaf("a", 5), WorkSpanAnalyzer.Task.leaf("b", 3));
        assertEquals(10, WorkSpanAnalyzer.work(t));   // 2 + 5 + 3
        assertEquals(7, WorkSpanAnalyzer.span(t));    // 2 + max(5, 3)
        assertEquals(10.0 / 7.0, WorkSpanAnalyzer.analyze(t).getParallelism());
    }

    @Test
    void sequentialChildrenSumSpans() {
        WorkSpanAnalyzer.Task t = WorkSpanAnalyzer.Task.sequentialNode("s", 1,
                WorkSpanAnalyzer.Task.leaf("a", 5), WorkSpanAnalyzer.Task.leaf("b", 3));
        assertEquals(9, WorkSpanAnalyzer.work(t));    // 1 + 5 + 3
        assertEquals(9, WorkSpanAnalyzer.span(t));    // 1 + 5 + 3
    }

    @Test
    void binaryReduceTreeWorkIsTwoNMinusOne() {
        WorkSpanAnalyzer.Task t = WorkSpanAnalyzer.Task.binaryReduceTree("reduce", 8, 1, 1);
        assertEquals(15, WorkSpanAnalyzer.work(t));   // 8 leaves + 7 internal
        assertEquals(4, WorkSpanAnalyzer.span(t));    // 1 + log2(8)
        WorkSpanAnalyzer.WorkSpanResult r = WorkSpanAnalyzer.analyze(t);
        assertTrue(r.getParallelism() > 1.0);
        assertTrue(r.getCriticalPath().startsWith("reduce"));
    }

    @Test
    void blellochScanScheduleWorkSpan() {
        // upSweep + downSweep as sequential phases over a power-of-two reduce tree.
        WorkSpanAnalyzer.Task up =
                WorkSpanAnalyzer.Task.binaryReduceTree("upSweep", 8, 1, 1);
        WorkSpanAnalyzer.Task down =
                WorkSpanAnalyzer.Task.binaryReduceTree("downSweep", 8, 1, 1);
        WorkSpanAnalyzer.Task scan =
                WorkSpanAnalyzer.Task.sequentialNode("Blelloch", 0, up, down);
        WorkSpanAnalyzer.WorkSpanResult r = WorkSpanAnalyzer.analyze(scan);
        assertEquals(30, r.getWork());                // 2 * (2*8 - 1)
        assertEquals(8, r.getSpan());                 // 2 * (1 + log2(8))
        assertTrue(r.getCriticalPath().contains("Blelloch"));
        assertTrue(r.getCriticalPath().contains("upSweep"));
    }

    @Test
    void analyzerValidation() {
        assertThrows(IllegalArgumentException.class, () -> WorkSpanAnalyzer.analyze(null));
        assertThrows(IllegalArgumentException.class,
                () -> WorkSpanAnalyzer.Task.leaf(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> WorkSpanAnalyzer.Task.leaf("a", -1));
        assertThrows(IllegalArgumentException.class,
                () -> WorkSpanAnalyzer.Task.binaryReduceTree("r", 0, 1, 1));
    }

    // --- Benchmark smoke: measured numbers are sane and deterministic ---

    @Test
    void benchmarkReduceProducesSaneRows() {
        List<BenchmarkResult> rows =
                ParallelBenchmark.benchmarkReduce(new int[]{2000, 4000, 8000},
                        ParallelReduce.ReduceOp.SUM, 4, 3);
        assertEquals(3, rows.size());
        int prevSize = 0;
        for (BenchmarkResult r : rows) {
            assertTrue(r.getInputSize() > prevSize, "rows sorted by size");
            prevSize = r.getInputSize();
            assertTrue(r.getSequentialNanos() > 0);
            assertTrue(r.getParallelNanos() > 0);
            assertTrue(r.getSpeedup() > 0);
            assertTrue(r.getThroughputPerSecond() > 0);
            assertEquals(4, r.getParallelism());
            assertEquals(2L * r.getInputSize() - 1, r.getWork(), "reduce work = 2n-1");
            assertTrue(r.getSpan() > 0);
            assertFalse(r.toString().isBlank());
        }
    }

    @Test
    void benchmarkScanProducesSaneRows() {
        List<BenchmarkResult> rows =
                ParallelBenchmark.benchmarkScan(new int[]{2000, 8000}, 4, 3);
        assertEquals(2, rows.size());
        for (BenchmarkResult r : rows) {
            assertTrue(r.getSequentialNanos() > 0);
            assertTrue(r.getParallelNanos() > 0);
            assertTrue(r.getSpeedup() > 0);
            assertTrue(r.getWork() > 0);
            assertTrue(r.getSpan() > 0);
        }
    }

    @Test
    void benchmarkSortProducesSaneRows() {
        List<BenchmarkResult> rows =
                ParallelBenchmark.benchmarkSort(new int[]{2000, 8000}, 4, 3);
        assertEquals(2, rows.size());
        for (BenchmarkResult r : rows) {
            assertTrue(r.getParallelNanos() > 0);
            assertTrue(r.getSpeedup() >= 0);
            assertTrue(r.getWork() > 0);
            assertTrue(r.getSpan() > 0);
        }
    }

    @Test
    void benchmarkValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> ParallelBenchmark.benchmarkReduce(new int[0], ParallelReduce.ReduceOp.SUM, 4, 3));
        assertThrows(IllegalArgumentException.class,
                () -> ParallelBenchmark.benchmarkReduce(new int[]{10}, ParallelReduce.ReduceOp.SUM, 0, 3));
        assertThrows(IllegalArgumentException.class,
                () -> ParallelBenchmark.benchmarkReduce(new int[]{10}, ParallelReduce.ReduceOp.SUM, 4, 0));
    }

    @Test
    void benchmarkSortsAnUnorderedSweepAndSizesTheBufferFromTheMaximum() {
        List<BenchmarkResult> rows =
                ParallelBenchmark.benchmarkSort(new int[]{8000, 2000, 4000}, 4, 1);
        assertEquals(3, rows.size());
        assertEquals(2000, rows.get(0).getInputSize());
        assertEquals(4000, rows.get(1).getInputSize());
        assertEquals(8000, rows.get(2).getInputSize());
    }

    @Test
    void benchmarkReduceHandlesAnUnorderedSweep() {
        List<BenchmarkResult> rows =
                ParallelBenchmark.benchmarkReduce(new int[]{4000, 2000}, ParallelReduce.ReduceOp.SUM,
                        4, 1);
        assertEquals(2, rows.size());
        assertEquals(2000, rows.get(0).getInputSize());
        assertEquals(4000, rows.get(1).getInputSize());
    }

    @Test
    void benchmarkScanHandlesAnUnorderedSweep() {
        List<BenchmarkResult> rows = ParallelBenchmark.benchmarkScan(new int[]{4000, 2000}, 4, 1);
        assertEquals(2, rows.size());
        assertEquals(2000, rows.get(0).getInputSize());
        assertEquals(4000, rows.get(1).getInputSize());
    }
}