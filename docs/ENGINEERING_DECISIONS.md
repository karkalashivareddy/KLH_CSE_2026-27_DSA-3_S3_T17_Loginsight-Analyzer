# Engineering Decisions

## Keep the runtime self-contained

LogInsight uses in-memory services and bundled text/JSONL datasets instead of
an external database. This makes the algorithm laboratory reproducible from a
checkout and keeps the data path visible during experiments.

The trade-off is that runtime state is not durable and the current design is
not intended for multi-instance deployment or unbounded log retention.

## Separate algorithms from transport

REST controllers dispatch requests to query engines and algorithm packages.
This keeps HTTP concerns separate from the implementations being studied and
makes the same engine testable without a browser.

## Record traces at execution time

Selected engines use `StepRecorder` to produce trace data for the Algorithm
Laboratory. The UI replays those records; it does not reconstruct an algorithm
run from synthetic steps.

The trade-off is additional per-run memory and serialization work in exchange
for inspectable execution.

## Cross-check algorithm families independently

The test suite includes focused tests and cross-checks for string, dynamic
programming, graph/flow, randomized, approximation, and parallel modules.
This is more useful for an algorithm laboratory than relying only on controller
smoke tests.

## Keep benchmarking explicit

Benchmark requests are separate from ordinary analysis requests. The backend
exposes sequential-versus-parallel comparisons without presenting benchmark
numbers as universal performance claims; results depend on the current machine
and input dataset.

