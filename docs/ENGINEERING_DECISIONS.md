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

## Freeze the `dsa` java.util usage instead of refactoring it

DSA-3 forbids delegating core algorithm logic in `dsa/**` to `java.util`
collections. The engines already imported a bounded set of `java.util` types
before the rebuild, so a blanket refactor carried more regression risk than the
course rule was worth. `EngineScopeGuardTest` therefore records the exact allowed
`java.util` imports per file in a frozen manifest and fails the build on any new
one, while always allowing removals so the ledger can only shrink.
`java.util.concurrent` (parallel) and `java.util.Random` (randomized) are
course-licensed and are listed in that manifest. The honest framing is a
course-scope guard, not a claim that `dsa` contains no `java.util` type.

## Label the dataset replay for what it is

The SSE stream at `GET /api/live` replays the in-memory event list oldest-first
and then ends. Rather than presenting it as live capture, the backend reports
`source: "demo-replay"`, the UI prints a "not real-time" disclosure, and the
documentation calls the surface Demo Replay even though the route and the
navigation label still read `/live` and `Live Replay`. One `ReplayProvider`
subscription backs both the Command Center card and the replay page, so the two
screens cannot disagree about progress.

## Keep window-scoped metrics and dataset-wide topology separate

`GET /api/overview` reports a selected window anchored on the newest event
timestamp, and every window-scoped count is computed over that slice.
`GET /api/analytics/dependencies` is different: it folds the whole current
dataset by `requestId`. The Command Center states that split in the UI rather
than implying the graph is a view of the selected window, and the health bands
are labelled as fixed error-rate thresholds rather than a health model.

