# Benchmarking Methodology

The repository has two different benchmark paths. They must not be described as one averaged benchmark suite.

## Search matcher comparison

`GET /api/analysis/benchmarks/search?pattern=...` builds one rendered haystack from the current dataset and runs:

- Naive
- KMP
- Z-Algorithm
- Rabin-Karp

It measures one execution per matcher, sorts the rows by that measured duration and reports the smallest as the winner. It does not fabricate averages, confidence intervals or universal performance claims. The result is specific to the active dataset, pattern, JVM and host.

## Parallel benchmark endpoint

`POST /api/benchmark/run` delegates to `ParallelBenchmark` for `dataset`/`reduce`, `scan` or `sort` scenarios.

- Each requested size is bounded by the shared validator.
- Data is generated from fixed seed 42 for the benchmark input.
- Each sequential/parallel operation has one warm-up and then the requested timed repetitions.
- The median timed sample is reported.
- `speedup = sequentialNanos / parallelNanos`.
- Work and span come from the explicit schedule model; they are not inferred from a marketing curve.
- The service accepts 1–20 repetitions and at most 12 sizes, each at most 2,000,000.

The default API sweep is `1_000`, `10_000`, `100_000`; the underlying parallel benchmark helper also defines a larger default set for direct callers.

## Direct parallel laboratory endpoints

`POST /api/parallel/reduce`, `/scan` and `/sort` run the corresponding engine and report the actual result, verification witness, sequential/parallel measurements and schedule fields. Their exact repetition behavior belongs to the engine endpoint; clients should read the returned payload rather than infer it from the search benchmark.

## Interpretation and limits

Timings depend on hardware, JVM warm-up, worker count, input size, garbage collection and concurrent load. A speedup below 1 is a valid observation. No benchmark result is stored as a product metric, and no result is presented as a production SLA.

Correctness is established separately by JUnit tests and cross-checks in [13-testing.md](13-testing.md).
