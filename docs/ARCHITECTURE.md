# Architecture

LogInsight Analyzer is a three-layer application: a typed React SPA, a Spring Boot REST/SSE API,
and a pure-Java analysis/algorithm layer over one in-memory log dataset.

```
┌──────────────────────────────┐
│  React SPA (Vite, :5173)    │
│  pages → api/client.ts (TS) │  api/types.ts mirrors backend DTOs
│  components → ui.tsx, charts │  hand-rolled SVG charts, no chart libs
│  hooks/useApi.ts (fetch)    │  SSE parsed manually in client.ts
└──────────────┬───────────────┘
               │  /api (Vite proxy)
┌──────────────▼───────────────┐
│  Spring Boot API (:8080)     │
│  controllers → services      │
│  GlobalExceptionHandler      │
└──────────────┬───────────────┘
               │  pure-Java layer, in-memory only
┌──────────────▼───────────────┐
│  DatasetService (1 dataset)  │
│  LogIndex · LogSearchService │
│  PatternExtractor            │
│  IncidentDetector            │
│  Analytics analyzers         │
│  DemoDatasetGenerator        │
└──────────────────────────────┘
```

## 1. Web layer (`frontend/src`)

- **`api/types.ts`** — one interface per backend DTO (LogEventDto, LogSearchResponse, PatternDto,
  IncidentDto, ServiceStatsDto, HttpStatsDto, OverviewDto, LiveBatch, SuggestionDto, DatasetResult,
  AlgorithmGroup, SearchBenchmarkResponse, …).
- **`api/client.ts`** — one method per endpoint; JSON errors are normalised through the documented
  `ApiError` envelope; two fetch-reader-based **SSE clients** (run replay and the live stream).
- **`pages/`** — screen per feature; `hooks/useApi` handles load/error/reload with a serialisable
  cache key so filters produce fresh requests without manual plumbing.
- **`styles/global.css`** — dark control-room palette (`#0B0F17` background family), card/sidebar
  layout, table, chart, pattern, incident and heatmap styles.

## 2. API layer (`backend/src/main/java/com/loginsight/controller`, `service`, `dto`)

Controllers are thin: they map to a service and return DTO records. `DatasetException` → 404 body,
`IllegalArgumentException`/`InvalidQueryException` → 400, other runtime errors → 500 via
`GlobalExceptionHandler` producing the `ApiError` shape (`status/error/message/timestamp/path`).

| Concern | Controllers / services |
| --- | --- |
| Dashboard | `OverviewController` → `OverviewService` |
| Logs | `LogController` → `LogService`, `LogSearchService` |
| Search | `SearchController` → `LogSearchService`, legacy `SearchService` |
| Analytics | `AnalyticsController` → `FleetAnalyzer`, `HeatmapAnalyzer`, … |
| Patterns | `PatternsController` → `PatternExtractor` |
| Incidents | `IncidentsController` → `IncidentsService`, `IncidentDetector` |
| Services | `ServicesController` → `FleetAnalyzer`, `TimelineAnalyzer` |
| Live | `LiveController` → `LiveStreamService` (SseEmitter) |
| Datasets | `DatasetController`, `IngestionController` → `DatasetService` |
| Catalogue / benchmark | `InsightsController` → `AlgorithmCatalog`, `SearchBenchmarkService` |
| Runs / trace / labs | `RunController`, `TraceController`, `TextHackController`, `CatalogController`, module controllers |

## 3. Analysis layer

All analysis is **pure Java over the current dataset snapshot**. Readers iterate the in-memory
`List<LogEvent>`; `LogIndex` additionally keeps per-field sorted position lists for ranged lookups.

- **`index/LogIndex`** — field positions sorted by timestamp; `inTimeWindow(from, to)` is a
  half-open binary search `[from, to)`. Built per-dataset-instance and cached by `LogIndexService`
  keyed on the dataset instance (no manual invalidation).
- **`search/LogSearchService`** — parses a query (`level:`, `service:`, `host:`, `source:`,
  `status:`, `trace:`, `request:`, `message:`, other → free text), filters via the index, then runs
  the chosen DSA matcher (KMP by default; the strategy/algorithm is reported to the client) over the
  lowercased rendered haystack. Renders per-match snippets, computes an exact match count, and on a
  miss produces a Levenshtein "did you mean" suggestion. `suggest()` returns typed typeahead
  candidates.
- **`pattern/PatternExtractor`** — rule-based token normalisation (`normalizeMessage`): numeric,
  hex-ish, hash, IP, `#`/`{`/`}`/`$` tokens become `<*>`; punctuation stripped from static tokens.
  Counts occurrences and keeps a representative example and level per template.
- **`incident/IncidentDetector`** — fixed 5-minute windows over the span (capped); each window's
  ERROR/FATAL volume compared to a baseline-derived threshold
  `max(3, ceil(3 × baselineRate))`; adjacent/one-window-apart elevated windows are merged.
- **`analytics/`** — `TimelineAnalyzer` (range bucket counts), `SeverityAnalyzer`, `HeatmapAnalyzer`
  (7×24 UTC grid), `FleetAnalyzer` (service/http/host rollups and measured latency percentiles).
- **`datasets/DemoDatasetGenerator`** — deterministic 14,000-event corpus (seed fixed), 8 services,
  correlated error bursts, timestamps re-anchored to the current hour; `generate()` is cached.
- **`service/SearchBenchmarkService`** — four matchers over one shared
  `QueryContext.renderDataset` haystack; a single measured run per matcher; the winner is simply
  the smallest measured time.

## 4. Data life-cycle

1. App starts with no dataset. `DatasetService.currentDataset()` is empty; product endpoints answer
   404 so the UI shows first-run states.
2. User loads the demo (`POST /api/datasets/demo`), a bundled sample (`POST /api/datasets/{name}`)
   or uploads a file (`POST /api/datasets`, multipart). `TextLogParser`/`JsonLogParser` parse each
   line, recording failures; the dataset instance + its summary are stored.
3. Analysis is recomputed per request/cached keyed on dataset instance; the live stream and run
   sessions read whatever dataset/run is current.

## 5. Integrity rules

- No fabricated numbers: all counts, latencies, percentiles, benchmark timings and incidents come
  from the loaded data. Demo data is explicitly labelled; the live stream is `demo-replay` /
  "not real-time".
- `java.util.*` is only forbidden inside the `com.loginsight.dsa` package (scope-guard tested).
- Every DSA complexity string exposed by the API is a real bound from `AlgorithmCatalog`.