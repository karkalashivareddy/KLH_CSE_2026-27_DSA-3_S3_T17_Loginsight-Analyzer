# 10 - Parallel Algorithms Theory (Module 6B)

> Theory supplement. Current parallel endpoints and benchmark methodology are in [API.md](API.md) and [14-benchmarking.md](14-benchmarking.md).

This document records the theory behind the parallel algorithms implemented in
`backend/src/main/java/com/loginsight/dsa/parallel/`. Every claim is stated honestly: speedups
are empirical, correctness is proven by the equality of the parallel result with the sequential
fold on identical schedules, and work/span numbers are derived from the explicit schedules that
the code actually executes.

Design rules (from `docs/02-architecture.md`): `dsa/parallel` is the only package allowed to use
`java.util.concurrent`; nested classes of `WorkSpanAnalyzer` are the only normal classes here.
`Arrays.sort`, `Collections.sort`, streams and `parallelStream` are never used as hidden algorithm
logic; the parallel sort below is a hand-written mergesort.

---

## 1. The Cost Model

Every task is analysed with the classic **work/span model**:

- **Work (T1)** - total number of unit operations the whole computation performs.
- **Span (T∞)** - length of the longest chain of dependent operations; the time on an ideal
  machine with unlimited processors.
- **Parallelism = T1 / T∞** - the average number of processors the algorithm can actually use.

`WorkSpanAnalyzer` builds an explicit *schedule tree* (`leaf` / `parallelNode` /
`sequentialNode` / `binaryReduceTree`) that mirrors what each algorithm runs, so the reported
work and span are properties of the executed schedule, not of a theoretical idealisation:

- `leaf(name, cost)` - a unit of sequential work: `work = span = cost`.
- `parallelNode(name, cost, children...)` - work = cost + Σ child work; span = cost + max child span.
- `sequentialNode(name, cost, children...)` - work = cost + Σ child work; span = cost + Σ child span.
- `binaryReduceTree(name, n, leafCost, internalCost)` - the balanced binary combine tree used for
  a reduce of `n` leaves: `work = n·leafCost + (n−1)·internalCost` (a full binary tree with `n`
  leaves has exactly `n−1` internal nodes); `span = leafCost + ⌈log₂ n⌉·internalCost`.

---

## 2. Parallel Reduction

`ParallelReduce.reduce` splits the array into `min(parallelism, n)` contiguous chunks, folds each
chunk sequentially, and combines the chunk partials with a balanced binary tree
(`binaryReduceTree`).

- **Work** O(n); **span** O(n/p + log p) for `p` workers — and O(log n) when `p = n` (each element
  is its own leaf), the classic PRAM reduction claim.
- Every operator is **associative** (including the natural wraparound of 64-bit addition), so the
  parallel result equals the sequential left fold for *any* chunking and thread count. This is
  what the tests verify against `reduceSequential`.
- Operators: `SUM`, `COUNT` (elements > 0), `MAX`, `ERROR_COUNT` (elements == error marker) — the
  error-count primitive for a numeric-encoded log column.
- Inputs of at most 1024 elements (`ParallelSupport.SEQUENTIAL_THRESHOLD`) run sequentially; the
  result is identical either way.
- The input is never mutated; workers write only to their own chunk and the partials tree, so the
  computation is race-free without locks.

---

## 3. Parallel Prefix Scan (Blelloch)

`ParallelPrefixScan` implements the classic **Blelloch up-sweep / down-sweep** algorithm over a
power-of-two padded array; the padded tail is masked out before returning.

1. **Up-sweep**: `tree[2^(k+1)·i + 2^k − 1]` receives the sum of the two children beneath it, one
   level at a time. Each level is a set of independent sibling additions executed by the worker
   pool.
2. **Down-sweep**: `tree[2^(k+1)·i + 2^k − 1]` is first set to 0, then every interior node
   `tree[2^(k+1)·(i+1) − 1]` emits its value to its left child and the sum of its own value and its
   left child's previous value to its right child — again level by level, where each level's
   updates are independent.
3. **Read-out**: the exclusive scan sits in `tree[2^k·i + 2^k − 1]` for each level-`k` block.

- **Work** O(n); **span** O(log n) — two sweeps of at most ⌈log₂ n⌉ levels each.
- The exposed `exclusive` scan is validated by construction: `inclusive[i] = exclusive[i] + input[i]`,
  and both equal the sequential fold in tests on power-of-two and non-power-of-two inputs.
- The same worker pool (`ParallelSupport.newPool`) executes every level with an `invokeAll`
  barrier, so a level never starts before the previous level has finished — this guarantees
  determinism: the output is byte-identical for any thread count.

---

## 4. Parallel Mergesort

`ParallelSort.sortParallel` is a bottom-up mergesort whose *schedule is identical to*
`sortSequential`:

- Round `run = 1, 2, 4, … < n`: every pair of runs of length `run` is merged into runs of length
  `2·run`, with the merged result written into the ping-pong buffer (so `sortParallel` and
  `sortSequential` perform exactly the same merges in the same order — the parallel version only
  splits each round's merges across the pool).
- Because parallel and sequential execute the same merge schedule on the same buffers, the two
  outputs are **byte-identical** — tests assert this directly.
- The merge is hand-written and **stable**: ties are decided in input order (merge selects the left
  run's element on equality).
- Each round's merges are independent, giving O(n/p) work per worker and O((n/p)·log n) span
  (plus O(log n) rounds of barriers); the parallelism test verifies `sort == Arrays.sort` on
  4096/8192/50 000 random, reverse-sorted, all-equal and duplicate-heavy inputs.

**Work/span schedule** (`ParallelBenchmark.sortSchedule`): rounds of `ceil(n / 2^run)` merges of
size `2^run`, recorded as a `parallelNode` per round over a `binaryReduceTree`-like merge fan.

---

## 5. Reproducible Benchmarking

`ParallelBenchmark` measures each algorithm on `DEFAULT_SIZES = {10 000, 50 000, 100 000,
500 000}` (or a caller-supplied set), reporting:

- median of 3 runs per size (hand-written insertion sort on the samples — no library shortcut),
- sequential vs parallel elapsed time (nanos), speedup, and throughput (elements/sec),
- work and span of the *actual executed schedule* via `WorkSpanAnalyzer`.

Fixed-seed data (`Random(42)`) plus median-of-3 make successive benchmark runs comparable; every
reported number is presented honestly as empirical (timing is machine-dependent and may show
slowdowns at small n where the per-call pool cost dominates).

---

## 6. Determinism and Pool Management

- There is **no global thread pool**: each call creates a fresh daemon-thread pool
  (`ParallelSupport.newPool`) sized to the requested parallelism and shuts it down in `finally`.
  This keeps results isolated, avoids cross-call races, and makes worker-count independence
  testable: the result of every algorithm is asserted identical at 1, 2, 4, 8 and 16 workers.
- Daemon threads (rather than a cached pool) let the JVM exit cleanly after `mvn verify` and keep
  the always-on web app from leaking threads.
- `requireParallelism` rejects zero/negative worker counts in every API with
  `IllegalArgumentException`.