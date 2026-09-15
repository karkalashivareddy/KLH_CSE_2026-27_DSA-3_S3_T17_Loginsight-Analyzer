/**
 * Parallel algorithms engine (DSA-3 Module 6B).
 *
 * <p>This is the <b>only</b> package in {@code com.loginsight.dsa} allowed to use
 * {@code java.util.concurrent} (docs/02 §8.2).  Parallelism is expressed as an <b>explicit
 * schedule</b>: every round/level partitions disjoint work ranges across up to {@code parallelism}
 * worker tasks and waits with an implicit barrier, so the algorithms are race-free by
 * construction.  There is no hidden global pool; every top-level call owns a fresh daemon
 * {@link java.util.concurrent.ExecutorService} that is shut down afterwards.</p>
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@link com.loginsight.dsa.parallel.ParallelReduce} – parallel reduction (SUM, COUNT,
 *       MAX, ERROR_COUNT): chunked workers + balanced tree combine; work O(n),
 *       span O(n/p + log p).</li>
 *   <li>{@link com.loginsight.dsa.parallel.ParallelPrefixScan} – Blelloch inclusive/exclusive
 *       prefix scan over power-of-two padded work arrays; work O(n), span O(log n).</li>
 *   <li>{@link com.loginsight.dsa.parallel.ParallelSort} – round-based bottom-up parallel
 *       merge sort with ping-pong buffers and a hand-written stable merge; work O(n log n).</li>
 *   <li>{@link com.loginsight.dsa.parallel.WorkSpanAnalyzer} – derives work, span and
 *       parallelism (work/span) from an explicit task schedule tree.</li>
 *   <li>{@link com.loginsight.dsa.parallel.ParallelBenchmark} – median-of-n sequential vs
 *       parallel timing across sizes, with schedule-derived work/span in every result.</li>
 *   <li>{@link com.loginsight.dsa.parallel.BenchmarkResult} – immutable single-row benchmark
 *       measurement (size, nanos, speedup, throughput, work, span).</li>
 * </ul>
 *
 * <p>Correctness is deterministic and independent of worker count: the operators are
 * associative, the merge/scan schedules are order-stable, and the parallel and sequential
 * variants produce identical results — verified by {@code ParallelCrossCheckTest}.</p>
 *
 * <p>Phase 1 skeleton created; implemented and tested in Phase 8.</p>
 */
package com.loginsight.dsa.parallel;