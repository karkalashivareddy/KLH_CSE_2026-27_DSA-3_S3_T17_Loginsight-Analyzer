# LogInsight Analyzer — Interview Guide

Questions and answers based **only** on this repository (commit `bbf5e45`). Say what is true, and
own the limitations openly.

---

## Product

**Q: What is LogInsight Analyzer?**
> An algorithmically engineered log analysis and investigation platform. You load (or generate) a log
> dataset, and the product lets you search, analyse, find patterns, detect incidents, benchmark and
> step-replay algorithms. Logs → parsing → indexing → algorithmic matching → analytics → pattern
> discovery → incident investigation → evidence. Every number derives from the loaded dataset or a
> measured run.

**Q: Why build a log analysis system?**
> Logs are a realistic, relatable engineering domain with concrete sub-problems — fast substring
> search, fuzzy/typo matching, recurring-message grouping and anomaly detection — each of which maps
> cleanly onto classical DSA (string matching, edit distance, heuristics). It lets me demonstrate
> curriculum algorithms inside a real product surface rather than as isolated snippets.

**Q: What problem does it solve?**
> Engineers investigating logs need to find events, see what's recurring, and spot anomalies without
> guessing. LogInsight makes the *method transparent*: it reports which algorithm ran, on what
> haystack, how long it took, and exposes the evidence behind each claim (e.g. supporting logs for an
> incident).

## Architecture

**Q: Why Spring Boot?**
> Mature, testable Java web stack: `@RestController` maps 1:1 to endpoints, `@Service` beans model the
> domain, dependency injection makes services testable, and it ships SSE support natively. Also it
> pairs with the Java-implemented DSA engine.

**Q: Why React + TypeScript?**
> Typed, component-based UI that mirrors the typed backend DTOs; Vite gives fast dev with `/api`
> proxying. I deliberately avoided chart/state libraries — charts are hand-rolled SVG.

**Q: Why REST?**
> Standard, inspectable contract for request/response operations (search, analytics, incidents,
> datasets). The full contract is in `docs/API.md`; controllers map 1:1 to it.

**Q: Why SSE (Server-Sent Events)?**
> The live stream and run replay are one-way server→client pushes with natural HTTP semantics and
> easy reconnection — exactly what a *bounded replay* and *step replay* need, without the overhead of
> WebSockets. Two SSE endpoints: `/api/live` and `/api/runs/{id}/events`.

**Q: Why no database?**
> The product is a deterministic, reproducible portfolio system. Keeping the dataset in memory makes
> it self-contained, fast, and honest (no pretending to be a durable store). Documented limitation:
> not multi-tenant or durable.

**Q: How does the frontend communicate with the backend?**
> A typed client (`frontend/src/api/client.ts`) calls REST via Vite's `/api` proxy during dev; SSE is
> consumed by small parser helpers feeding the live page and `TracePlayer`. Wire types mirror the
> backend DTOs.

## Search

**Q: How is structured search parsed?**
> `SearchQueryParser` (backend `search/`) tokenises the query string into fields — `level:`, `service:`,
> `host:`, `source:`, `status:`, `trace:` — plus optional time ranges and sort order, producing a
> `SearchQuery` value object.

**Q: How does filtering work?**
> `LogIndex` keeps sorted position lists per field (severity, service, host, source, status, trace).
> Each filter resolves to a position list; lists are intersected (`LogIndex.intersect`), then the
> surviving positions are sorted by the requested field and paginated.

**Q: How are DSA matchers integrated?**
> For free text, the candidate events are rendered into one lowercase haystack and matched **once**
> by `KMPMatcher`. Match positions are mapped back to source lines, giving real per-event hit counts.
> The response reports `algorithm: "KMP"`, pattern length, text size and measured duration.

**Q: How does fuzzy search work?**
> Only as a *suggestion path*: when a free-text search returns zero hits, `LogSearchService` computes
> the Levenshtein distance from the query to each distinct message (up to 4,000), picks the nearest,
> and shows "Did you mean?" with distance, similarity % and match count if within threshold
> (`ceil(0.35 × max length)`, minimum 2).

**Q: Why Levenshtein?**
> It's the classic edit-distance DP (`O(n·m)`, Wagner-Fischer) and matches the "typo in a log phrase"
> use case well. Damerau-Levenshtein and a weighted variant also exist in the DP module and could be
> swapped in.

## Patterns

**Q: How are templates extracted?**
> `PatternExtractor` performs deterministic token normalization: variable tokens (numbers, UUIDs,
> IPs, hex, times) are replaced with placeholders, forming a message template; events group by
> template with per-level counts and sample events.

**Q: Why heuristic rather than ML?**
> Two reasons: deterministic and explainable output, and honesty — the project explicitly avoids
> claiming trained-model intelligence it doesn't have. The UI carries the label "Heuristic pattern
> extraction — Not ML".

## Incidents

**Q: How are incidents detected?**
> `IncidentDetector` operates only on ERROR/FATAL events, buckets them into fixed 5-minute windows
> over the dataset's time span, and computes a baseline (average errors per window). A window is
> "elevated" when its count ≥ `max(3, 3 × baseline)`. Consecutive or one-window-apart elevated
> windows merge into incidents.

**Q: What is the baseline?**
> The average number of ERROR/FATAL events per 5-minute window across the whole dataset time span.

**Q: How is the threshold calculated?**
> `count ≥ max(3, 3 × baseline)` — so a quiet dataset needs a burst of at least 3 in a window, and a
> busy one needs roughly triple its average.

**Q: How is evidence exposed?**
> Each incident details its window, baseline, threshold, affected services and primary message
> pattern, and the API exposes the supporting logs (`/api/incidents/{id}/logs`) so a user can verify
> the call. The UI labels detection as rule-based heuristics, not AI.

## Benchmarking

**Q: How is a benchmark performed?**
> `SearchBenchmarkService` runs **Naive, KMP, Z-Algorithm and Rabin-Karp** over the exact same
> haystack and pattern, one measured execution each, and returns per-matcher times.

**Q: What does "winner" mean?**
> The matcher with the smallest measured wall-clock time for that single run on this machine.

**Q: Why is a single measured run not a universal performance claim?**
> Times depend on hardware, JVM warm-up, haystack size and pattern; one run can't establish
> asymptotic superiority in practice. The UI therefore labels it "measured once" and the docs say the
> same.

## Trace engine

**Q: What is a trace event?**
> A recorded step: a named operation (e.g. an LPS init, a match attempt, a DP-cell fill) with an
> index, description, and a state snapshot (variables/table) captured by `StepRecorder` at
> `dsa/trace`. 13 algorithms are marked traceable.

**Q: How does replay work?**
> A run is created (`POST /api/runs`), executed with the recorder attached, stored in `RunStore` (cap
> 64), and its steps are streamed over SSE (`GET /api/runs/{id}/events`). The frontend `TracePlayer`
> renders steps, state tables, a step timeline and play/pause/step/speed controls with keyboard
> shortcuts (which never steal focus from inputs).

**Q: How are algorithm steps represented?**
> A step typically has: op name, sequence number, description of the conceptual action, operation
> chips/highlights, and a state table (variables or DP cells). Representation lives in
> `trace/AlgorithmStep` / `trace/StepRecorder`.

## DSA

For each implemented module (all in `backend/src/main/java/com/loginsight/dsa/`):

- **M2 Strings** — `NaiveMatcher`, `KMPMatcher`, `ZAlgorithm`, `RabinKarpMatcher` (all used by the
  benchmark; KMP drives product search), plus `AhoCorasick` and Suffix Array/Kasai-LCP.
  KMP: precompute prefix (LPS) array, avoid backtracking — `O(n+m)`. Z: Z-array of longest prefix
  match at each position. Rabin-Karp: rolling hash, `O(n+m)` expected.
- **M3 DP** — `LevenshteinDistance` (`O(n·m)` DP table, used in search suggestions), `Damerau`,
  `WeightedEditDistance`, `NeedlemanWunsch`/`SmithWaterman` alignment, tree/interval/bitmask(TSP)/
  SOS DP. Explain the subproblem + recurrence (+ matrix-chain traceable).
- **M4 Flow** — `FordFulkerson`, `EdmondsKarp`, `Dinic` (level graph + blocking flow), `MinCostMaxFlow`,
  `MinCut`, `BipartiteMatching`. Traceable 3: fordfulkerson, edmondskarp, dinic.
- **M5 Approximation** — vertex cover 2-approx (maximal matching), kernelization, bounded/FPT,
  set cover, maximal matching, Knapsack FPTAS, independent-set reduction. Traceable 1: vertexcover.
- **M6 Randomized** — `RandomizedQuickSort` (pivot randomness → expected `O(n log n)`), `MillerRabin`
  (probable primes), `ReservoirSampling` (stream uniform sample), perfect/randomized hashing.
  Traceable 3: quicksort, millerrabin, reservoir.
- **M6 Parallel** — `ParallelSort`, `ParallelReduce`, `ParallelPrefixScan` with `WorkSpanAnalyzer`
  (work/span) and benchmarks; no traceable entries in this module.

If asked "which algorithms power product features?": KMP (search), Levenshtein (fuzzy suggestion),
the four matchers (benchmark); the rest are exposed catalogue engines — I say that explicitly rather
than overclaiming.

Cross-checks: each module has tests that validate DSA results against brute force (e.g.
`StringCrossCheckTest`, `FlowCrossCheckTest`, `ParallelCrossCheckTest`).

## Testing

**Q: How many backend tests?**
> 93 test classes / **732 tests / 0 failures / 0 errors** via `.\mvnw.cmd -o verify`.

**Q: What does the suite cover?**
> Every DSA module (including brute-force cross-checks), search parser/service, index, pattern,
> incident, analytics, datasets/generator, both parsers, all controllers, catalog/query facade,
> trace cross-check, exceptions, and a scope-guard asserting the `dsa` package avoids `java.util.*`
> in algorithm code.

**Q: What frontend verification was performed?**
> No JS test runner is configured; verification is `npm run build` (tsc + vite) plus a headless
> Playwright pass: 59 views, 0 blank routes, 0 JS errors, 0 console errors, no network 404/500, and
> responsive checks at 1440/1280/1024/768/480/375 px (recorded in `LOGINSIGHT_REBUILD_REPORT.md`).

## Limitations

Honest answers to volunteer when asked "what would you do differently?":

- In-memory single-dataset design and capped run history (64) — appropriate for a deterministic
  portfolio product, not durable/multi-tenant.
- Live screen replays the loaded dataset over SSE (`demo-replay`, "not real-time") — actual ingestion
  from external streams isn't implemented.
- Patterns and incidents are deterministic heuristics with exposed evidence — no trained models.
- Benchmarks are single measured runs; winner is per-run, not a universal claim.
- No persistence, auth, or observability-scale ingestion claims.

## Suggested next steps (interview answers)

"With more time I'd add durable storage for run history, incremental ingestion into `LogIndex`,
exportable incident evidence bundles, and — as a clearly-separated future step — a classifier trained
on the labelled pattern templates, keeping the heuristic baseline intact."