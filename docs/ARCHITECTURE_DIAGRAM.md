# LogInsight Analyzer — Architecture Diagram

Mermaid rendering of the implemented architecture (commit `bbf5e45`). Every node corresponds to a
real package or service in the repository; see [PROJECT_WALKTHROUGH.md](PROJECT_WALKTHROUGH.md) and
[API.md](API.md) for the matching detail.

```mermaid
flowchart TB
    subgraph UI["LogInsight UI"]
        PAGES["React 18 + TS + Vite pages<br/>Overview · Logs · Search · Analytics · Patterns ·<br/>Incidents · Services · Live · Datasets · Ingestion ·<br/>Analysis · Algorithms · Benchmarks · Runs · System · Docs"]
        UI_KIT["UI kit components<br/>hand-rolled SVG charts · TracePlayer · useApi"]
    end

    API["Spring Boot API (REST + SSE)<br/>REST controllers map 1:1 to docs/API.md"]

    subgraph CORE["Backend services / domain"]
        DS["DatasetService<br/>one in-memory dataset"]
        PARSER["Parser + normalisation<br/>Json/Text parsers · LogEvent"]
        INDEX["LogIndexService + LogIndex<br/>position lists · time windows · intersect"]
        SEARCH["LogSearchService + SearchQueryParser<br/>structured filters · KMP free text · Levenshtein suggest"]
        AN["Analytics / Overview services<br/>Timeline · Severity · Heatmap · Fleet · Frequency"]
        PAT["PatternExtractor<br/>heuristic token templates"]
        INC["IncidentDetector<br/>windowed baseline thresholding"]
        SVC["Services + dependency graph"]
        LIVE["LiveStreamService<br/>bounded SSE replay"]
        RUN["RunService + RunStore + StepRecorder<br/>run sessions · SSE step replay"]
    end

    subgraph DSA["DSA Engine (com.loginsight.dsa)"]
        S[String · M2<br/>Naive · KMP · Z · Rabin-Karp ·<br/>Aho-Corasick · SuffixArray/Kasai]
        DP[DP · M3<br/>Levenshtein · Damerau · Weighted ·<br/>alignment · tree · interval · bitmask · SOS]
        FLOW[Flow · M4<br/>Ford-Fulkerson · Edmonds-Karp · Dinic ·<br/>Min-Cost · Min-Cut · Bipartite]
        APX[Approximation · M5<br/>Vertex Cover · Kernelization ·<br/>Set Cover · Max Matching · FPTAS]
        RND[Randomized · M6<br/>Quicksort · Miller-Rabin ·<br/>Reservoir · Perfect Hash]
        PAR[Parallel · M6<br/>Sort · Reduce · Prefix Scan<br/>work-span analysis]
    end

    DS --> PARSER
    PARSER --> INDEX
    DS --> INDEX
    INDEX --> SEARCH
    SEARCH --> AN
    SEARCH --> PAT
    PAT --> INC
    INDEX --> SVC
    SVC --> AN
    DS --> LIVE
    DS --> RUN
    SEARCH --> S
    S --> DP
    AN --> DP

    API --> DS
    API --> SEARCH
    API --> AN
    API --> INC
    API --> PAT
    API --> SVC
    API --> LIVE
    API --> RUN

    API --> DSA
    RUN --> DSA
    DSA --> API

    PAGES -->|"REST  /api/*"| API
    LIVE -->|"SSE  /api/live"| PAGES
    RUN -->|"SSE  /api/runs/{id}/events"| PAGES
    API -->|"JSON"| PAGES
```

## Flow of one request

```mermaid
sequenceDiagram
    participant B as Browser
    participant V as Vite /api proxy
    participant C as Spring Controller
    participant S as Service
    participant D as Dataset (in-memory)
    participant A as DSA engine

    B->>V: GET /api/search (query)
    V->>C: proxy to :8080
    C->>S: LogSearchService.search()
    S->>D: current dataset
    S->>A: KMP over rendered haystack (measured ns)
    A-->>S: positions + stats
    S-->>C: LogSearchResponse (strategy, algorithm, duration, hits)
    C-->>B: JSON
```

## Where data lives

```mermaid
flowchart LR
    GEN[DemoDatasetGenerator<br/>seed 20260913L · 14k events] --> DS
    F[File upload / sample-data] --> P[Parser]
    P --> DS[DatasetService — current dataset]
    DS --> IDX[LogIndex]
    IDX --> SV[Search / Pattern / Incident / Analytics]
    RUN[RunStore — run history, cap 64] --> SSE
    STREAM[LiveStreamService] -->|SSE| SSE[Demo replay — not real-time]
```

## Verified numbers used by the UI

- **35** engines registered · **42** catalogue algorithms · **36** exposed · **13** traceable.
- **6** catalogue modules: M2 Strings (9), M3 DP (12), M4 Flow (6), M5 Approximation (7),
  M6 Randomized (5), M6 Parallel (3).
- Demo corpus: **14,000** deterministic events across **~8 services**.
- Test gate: **732 tests / 0 failures / 0 errors**.