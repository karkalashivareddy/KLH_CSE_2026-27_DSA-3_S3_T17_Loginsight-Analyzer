# Algorithms

LogInsight is a product shell around the DSA-3 engines. This document names the algorithms that
power each product feature, how they are invoked, and their complexity bounds. The full 42-entry
catalogue is served by `GET /api/analysis/algorithms`; the code lives in `com.loginsight.dsa`
(classic engines), `com.loginsight.search`, `com.loginsight.pattern`, `com.loginsight.incident`
and `com.loginsight.analytics`.

## Search (product search)

Endpoint `POST /api/search` · explorer `GET /api/logs/explore`. After `SearchQueryParser` resolves
structured filters, the query's free-text term is matched with a DSA string matcher over the
**lowercased rendered haystack** (`QueryContext.renderDataset(events)`).

| Matcher | Class | Pattern/lps/preprocess | Time | Space |
| --- | --- | --- | --- | --- |
| Naive | `NaiveMatcher` | none | O(n·m) | O(1) |
| KMP (default) | `KMPMatcher` | prefix function π | O(n + m) | O(m) |
| Z-Algorithm | `ZAlgorithm` | Z-array | O(n + m) | O(n) |
| Rabin-Karp | `RabinKarpMatcher` | rolling hash | O(n + m) avg, O(n·m) worst | O(1) |

Invocation details are reported back in `LogSearchResponse.strategy/algorithm/pattern/
patternLength/textSize/durationNanos`, and the measured benchmark (`GET
/api/analysis/benchmarks/search`) runs all four over one shared haystack.

Complexities are real from the catalogue: Naive `"O(n * m)"`, KMP `"O(n + m)"`,
Z `"O(n + m)"`, Rabin-Karp `"O(n + m) avg, O(n*m) worst"`.

## Fuzzy "did you mean"

When a search misses, `LogSearchService` finds the closest dictionary term via **Levenshtein
edit distance** (`com.loginsight.dsa.dp.LevenshteinDistance`). The response carries the suggested
term, `similarityPct`, distance, and match count of the corrected term so the suggestion is
evidenced. Typeahead suggestions (`GET /api/search/suggest`) come from dataset dictionaries
(service/host/endpoint/level/source/status) and common template terms.

## Pattern discovery

`PatternExtractor.normalizeMessage(message)` is a **rule/token heuristic** (not ML):

- tokens that are numeric, contain only hex chars (len ≥ 8), look like hashes, IPs, or contain
  `#`, `{`, `}`, `$` are replaced by `<*>`;
- punctuation is stripped from remaining static tokens to keep like-shapes equal;
- `response:`-style static text survives so templates remain readable.

Templates are counted and ranked; the UI labels the result "heuristic token patterns — not ML".

## Incident detection

`IncidentDetector`:

1. considers only ERROR/FATAL events;
2. buckets them into fixed **5-minute windows** over the span (cap 5000 windows);
3. marks a window elevated when its error volume **> max(3, ceil(3 × baselineRate))** where
   baseline is the dataset's own average error volume per window;
4. merges consecutive (or one-window-apart) elevated windows into an incident, carrying the window
   bounds, affected services and the top pattern as evidence.

The response's `method` string states *"Heuristic: …"*; supporting logs are available for every
incident so the claim can be verified.

## Analytics

- `TimelineAnalyzer` — constant-size range buckets (`5m/15m/1h/6h/24h`) with determin-istic sample
  counts; `Range.bucketCount()` drives the dashboard.
- `SeverityAnalyzer` — zero-filled TRACE→FATAL histogram.
- `HeatmapAnalyzer` — 7 (day-of-week) × 24 (UTC hour) grid, zeros included, via calendar fields.
- `FleetAnalyzer` — service/host rollups with error rates; HTTP stats with median/max and **p50/p95
  measured latency percentiles** over events carrying response times.
- `TopKFrequentAnalyzer` — heap-based top-K over service/endpoint/level/ip.
- `Graph` — service-dependency graph from request trails (`ServiceGraphBuilder`).
- `SearchBenchmarkService` — one measured run per matcher; winner = fastest measured.

## Catalogue (42 entries)

`GET /api/analysis/algorithms` exposes, grouped by module: **String** (Naive, KMP, Z, Rabin-Karp,
Aho-Corasick, suffix array/build/search, Kasai LCP, fuzzy search), **Dynamic Programming**
(Levenshtein, Damerau, weighted edit, global/local alignment, matrix chain, optimal BST, TSP,
Hamiltonian, tree DP, rerooting, SOS), **Graph & Flow** (Ford-Fulkerson, Edmonds-Karp, Dinic,
min-cut, bipartite matching, min-cost flow), **Approximation** (VC 2-approx, incident cover, greedy
set cover, bounded VC, kernelization, knapsack FPTAS, VC⇄IS reduction), **Randomized** (quicksort,
Miller-Rabin, reservoir, universal/FKS hashing), **Parallel** (reduce, scan, merge-sort).
`AlgorithmInfo` exposes `canonicalEndpoint`, `traceEndpoint`, `timeComplexity`,
`spaceComplexity`, `tracked`, `description`.

## Integrity

- Complexity strings shown anywhere come from `AlgorithmCatalog` — never hardcoded in the UI.
- Timings (search duration, benchmark) are measured at run time; nothing is averaged or sampled
  fictitiously.
- Probabilistic results stay labelled (e.g. `PROBABLY_PRIME`); approximation algorithms report
  their ratio/bound; traceable algorithms replay **recorded** steps.