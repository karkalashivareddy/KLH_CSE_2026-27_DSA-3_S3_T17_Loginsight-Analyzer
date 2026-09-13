# 14 - Benchmarking Methodology

## 1. Principles

1. **Never fabricate.** Every number displayed comes from an actual execution on the machine it was
   generated on; the environment is recorded alongside the numbers.
2. **Measure after correctness.** No benchmark is meaningful on an algorithm that fails its tests;
   the benchmark suite only runs against the tested implementations.
3. **Optimise only after measuring.** Baseline numbers are collected first; any later change is
   justified by a measured difference, not intuition.
4. **Interactive bounds.** Benchmark runs are capped (NFR-8) so a live demo stays responsive;
   large runs (1M) are opt-in.

## 2. Metrics

| Metric | Definition |
|---|---|
| `executionTimeNanos` | wall-clock `System.nanoTime()` around the algorithm body (excludes JSON serialization and IO) |
| `inputSize` | algorithm-specific: chars scanned (string), cells (DP), `V*E` traversed (flow), N (stream/sort) |
| `throughputPerSec` | `inputSize / (executionTimeNanos / 1e9)` |
| `resultSize` | size of the produced result (match count, flow value, alignment length) |
| `memoryEstimateBytes` | sized sum of primary arrays/structures allocated by the algorithm (approximation, not JVM heap) |
| `speedup` | `sequentialNanos / parallelNanos` (parallel suite) |
| `work` / `span` | schedule-derived values from `WorkSpanAnalyzer`; `parallelism = work / span` |

## 3. Protocol

- **Warm-up:** the JVM is warmed with 2 non-measured runs before timing starts (JIT).
- **Repetitions:** 3..7 measured runs per (algorithm, size) point; **median** reported; best/worst
  kept in the payload for transparency.
- **Ordering:** sizes run small-to-large to limit GC pressure bias; each point runs on a fresh input
  (no reuse across algorithms for search benchmarks).
- **Environment recording:** `javaVersion`, `cores`, `os`, `heap` (if set) included in every
  benchmark response (`docs/12` §10).
- **Parallel runs** record thread-pool size = `Runtime.getRuntime().availableProcessors()`.
- **Time budget:** a `BenchmarkController` request carries its own `repetitions` and `sizes`;
  the dispatcher refuses sizes beyond the active cap.

## 4. Benchmark Scenarios

### S1 String engines - naive vs KMP vs Z vs Rabin-Karp
- Sizes: `10_000`, `50_000`, `100_000`, `500_000`, (`1_000_000` opt-in).
- Corpus: synthetic repeated log messages + random-text control, pattern length fixed (~10-30).
- Report table: per algorithm per size `{ executionTimeNanos, throughputPerSec, matches }`.
- Expected storyline (measured, not assumed): naive degrades on repeated text; KMP/Z/RK remain
  linear; RK's constant factor vs KMP shown honestly.

### S2 Suffix array construction
- Sizes: `1_000`, `5_000`, `10_000`, `50_000` characters (doubling construction).
- Report: build time vs `n log^2 n` trend; LCP time separately (expected O(n)).

### S3 Flow engines - FF vs Edmonds-Karp vs Dinic
- Graph families: chain, layered, dense random (V=8..64, E up to ~5*V), integer capacities.
- Report: max-flow value equality assertion in-run + augm./phase counts + time.
- Budget capped so worst-case FF (exponential on bad input) is demonstrated only with tiny graphs.

### S4 Parallel suite - reduce / scan / sort
- Sizes: `100_000`, `1_000_000`, (5M opt-in).
- Report: sequential vs parallel nanos, speedup, work/span, parallelism; threshold effect at the
  fallback cut-off is shown for small n.

### S5 DP spot checks
- Levenshtein on 5k x 5k texts (matrix ~ O(25e6)) and TSP on n=18..20 (documented 2^n explosion
  rather than run to completion) - evidence for why the app warns about n.

## 5. Where Results Live

- Live: `POST /api/benchmark/run` returns `BenchmarkResultDto` shown by the Benchmark Lab.
- Persistent: `docs/14` appendix updated **only** after the final measured run on the demo machine
  (Phase 14), each row annotated with the environment that produced it.
- No pre-baked numbers exist in the codebase; the benchmark table is empty until measured.

## 6. Honesty Anchors

- An algorithm is only listed in a scenario if its class and test exist in source (`docs/04`)
  - the benchmark runner validates this before executing.
- Complexity strings shown next to benchmark rows are the **declared** complexities from the class
  javadoc; observed scaling curves are not reinterpreted to match them.
- Outliers (GC pauses) are kept visible (best/worst recorded) so reviewers can audit the median.