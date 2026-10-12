# 03 — Proposed Project Structure

> Historical proposed tree. The current repository layout is represented by [ARCHITECTURE.md](ARCHITECTURE.md); this file is retained for phase traceability.

## Full proposed tree for `LogInsight Analyzer`

Phase 0 creates this document and the documentation/scaffolding directories only. Backend and
frontend directories are created from Phase 1 onward and filled phase by phase. Empty directories
under Git are tracked with `.gitkeep` where they must exist before content lands.

---

## 1. Repository Root

```
Loginsight-Analyzer/
├── README.md                 # professional project readme (rewritten in final docs phase)
├── .gitignore                # Phase 0 (updated)
├── docs/                     # Phase 0..final documentation
├── sample-data/              # Phase 2 datasets (spec in sample-data/README.md)
├── backend/                  # Phase 1+ Spring Boot + Maven Wrapper
└── frontend/                 # Phase 1+ React (Vite) + TypeScript
```

---

## 2. `backend/`

```
backend/
├── pom.xml
├── mvnw                      # Maven wrapper script (Phase 1, committed)
├── mvnw.cmd
├── .mvn/wrapper/
│   └── maven-wrapper.properties
├── src/main/java/com/loginsight/
│   ├── LogInsightApplication.java
│   ├── config/
│   │   ├── WebConfig.java            # CORS for dev origin, static resources mapping
│   │   └── AppProperties.java        # dataset limits, sample paths, cores
│   ├── controller/
│   │   ├── HealthController.java
│   │   ├── LogController.java        # upload/import/list/stats/clear
│   │   ├── SearchController.java     # naive/kmp/z/rabin-karp/aho-corasick
│   │   ├── StringAlgorithmController.java   # suffix build/search/lcp
│   │   ├── DpController.java         # similarity + playground DP endpoints
│   │   ├── FlowController.java       # graph + ff/ek/dinic/min-cut/matching/min-cost
│   │   ├── ApproximationController.java
│   │   ├── RandomizedController.java
│   │   ├── ParallelController.java
│   │   ├── BenchmarkController.java
│   │   └── PlaygroundController.java # explicit-input mode, delegates to dispatcher
│   ├── dto/
│   │   ├── request/                  # SearchRequest, FuzzySearchRequest,
│   │   │                             # AlignmentRequest, FlowRequest, MinCutRequest,
│   │   │                             # MatchingRequest, VertexCoverRequest, SetCoverRequest,
│   │   │                             # IncidentCoverageRequest, MillerRabinRequest,
│   │   │                             # ReservoirRequest, HashRequest, QuicksortRequest,
│   │   │                             # ParallelRequest, BenchmarkRequest, PlaygroundRequest
│   │   └── response/                 # AlgorithmResultDto, DatasetStatsDto, LogEventDto,
│   │                                 # ErrorResponseDto, BenchmarkResultDto
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── InvalidQueryException.java
│   │   ├── InvalidLogException.java
│   │   ├── AlgorithmExecutionException.java
│   │   └── DatasetException.java
│   ├── model/
│   │   ├── LogEvent.java             # id, timestamp, level, service, host, ipAddress,
│   │   │                             # httpMethod, endpoint, statusCode, responseTime,
│   │   │                             # requestId, userId, message, rawMessage
│   │   ├── LogLevel.java             # INFO DEBUG WARN ERROR (enum)
│   │   ├── HttpMethod.java           # GET POST PUT DELETE PATCH (enum)
│   │   ├── ServiceNode.java
│   │   ├── LogQuery.java
│   │   ├── QueryType.java            # PATTERN_SEARCH, FUZZY_SEARCH, MULTI_PATTERN_SEARCH,
│   │   │                             # DOCUMENT_SIMILARITY, SEQUENCE_ALIGNMENT,
│   │   │                             # SUFFIX_ANALYSIS, SERVICE_FLOW, MIN_CUT,
│   │   │                             # APPROXIMATE_COVER, PRIMALITY_TEST, STREAM_SAMPLE,
│   │   │                             # BENCHMARK, + DP/bitmask/tree/sos/matching variants
│   │   ├── AlgorithmType.java        # NAIVE, KMP, Z, RABIN_KARP, AHO_CORASICK, SUFFIX_ARRAY,
│   │   │                             # KASAI_LCP, LEVENSHTEIN, DAMERAU_LEVENSHTEIN,
│   │   │                             # WEIGHTED_EDIT_DISTANCE, NEEDLEMAN_WUNSCH, SMITH_WATERMAN,
│   │   │                             # MATRIX_CHAIN, BITMASK_TSP, HAMILTONIAN_PATH,
│   │   │                             # TREE_DIAMETER, REROOTING_DP, SOS_DP, FORD_FULKERSON,
│   │   │                             # EDMONDS_KARP, DINIC, MIN_CUT, BIPARTITE_MATCHING,
│   │   │                             # MIN_COST_MAX_FLOW, VERTEX_COVER, MAXIMAL_MATCHING,
│   │   │                             # SET_COVER, MILLER_RABIN, RESERVOIR_SAMPLING,
│   │   │                             # UNIVERSAL_HASH, RANDOMIZED_QUICKSORT, PARALLEL_REDUCE,
│   │   │                             # PARALLEL_PREFIX_SCAN, PARALLEL_SORT, BENCHMARK
│   │   ├── AlgorithmMetrics.java     # inputSize, executionTimeNanos, resultSize,
│   │   │                             # memoryEstimateBytes, throughput, speedup
│   │   └── QueryResult.java
│   ├── parser/
│   │   ├── LogParser.java            # SPI (parse stream → records + errors)
│   │   ├── ParsedLog.java
│   │   ├── LogParserFactory.java     # detects text vs jsonl
│   │   ├── TextLogParser.java
│   │   ├── JsonLogParser.java        # JSON Lines
│   │   └── ParserException.java
│   ├── service/
│   │   ├── DatasetService.java       # upload/importSample/clear/stats/events(session)
│   │   ├── LogService.java
│   │   ├── AnalyticsService.java     # orchestrates analytics/* (charts data)
│   │   ├── StringSearchService.java
│   │   ├── SimilarityService.java
│   │   ├── FlowService.java          # builds graph from dataset, runs flow engines
│   │   ├── ApproximationService.java
│   │   ├── RandomizedService.java
│   │   ├── ParallelService.java
│   │   └── BenchmarkService.java
│   ├── analytics/
│   │   ├── FrequencyAnalyzer.java
│   │   ├── TopKAnalyzer.java
│   │   ├── TimeWindowAnalyzer.java
│   │   ├── ServiceGraphBuilder.java  # log events → FlowGraph (not hard-coded)
│   │   ├── ErrorAnalyzer.java
│   │   └── AnalyticsResult.java
│   ├── query/
│   │   ├── QueryEngine.java
│   │   ├── QueryDispatcher.java
│   │   ├── QueryContext.java
│   │   ├── QueryResult.java
│   │   ├── QueryValidator.java
│   │   └── strategies/
│   │       ├── NaiveStrategy.java
│   │       ├── KmpStrategy.java
│   │       ├── ZStrategy.java
│   │       ├── RabinKarpStrategy.java
│   │       ├── AhoCorasickStrategy.java
│   │       ├── FuzzySearchStrategy.java
│   │       ├── NeedlemanWunschStrategy.java
│   │       ├── SmithWatermanStrategy.java
│   │       ├── DinicStrategy.java
│   │       ├── FordFulkersonStrategy.java
│   │       ├── EdmondsKarpStrategy.java
│   │       ├── MinCutStrategy.java
│   │       ├── VertexCoverStrategy.java
│   │       ├── MillerRabinStrategy.java
│   │       ├── ReservoirStrategy.java
│   │       └── ParallelStrategy.java  # + matrix-chain/tsp/sos/tree playground strategies
│   └── dsa/
│       ├── common/                    # hand-written structures used to keep algorithms transparent
│       │   ├── CustomArrayList.java
│       │   ├── CustomStack.java
│       │   ├── CustomQueue.java
│       │   ├── CustomDeque.java
│       │   ├── CustomTrie.java        # used by Aho-Corasick
│       │   └── IntArrayQueue.java     # primitive BFS queue for flow (optional)
│       ├── string/
│       │   ├── NaiveMatcher.java
│       │   ├── KMPMatcher.java
│       │   ├── ZAlgorithm.java
│       │   ├── RabinKarpMatcher.java
│       │   ├── aho/
│       │   │   ├── AhoNode.java
│       │   │   ├── PatternMatch.java
│       │   │   └── AhoCorasick.java
│       │   └── suffix/
│       │       ├── SuffixArray.java
│       │       └── KasaiLCP.java
│       ├── dp/
│       │   ├── editdistance/
│       │   │   ├── LevenshteinDistance.java
│       │   │   ├── DamerauLevenshteinDistance.java
│       │   │   └── WeightedEditDistance.java
│       │   ├── alignment/
│       │   │   ├── AlignmentResult.java
│       │   │   ├── NeedlemanWunsch.java
│       │   │   └── SmithWaterman.java
│       │   ├── interval/
│       │   │   └── MatrixChainMultiplication.java
│       │   ├── bitmask/
│       │   │   ├── BitmaskTSP.java
│       │   │   └── HamiltonianPath.java
│       │   ├── tree/
│       │   │   ├── TreeDiameterDP.java
│       │   │   └── RerootingDP.java
│       │   └── sos/
│       │       └── SOSDP.java
│       ├── graph/
│       │   ├── ServiceDependencyGraph.java   # analytics-facing adjacency (adjacency list)
│       │   └── GraphIo.java                  # small DOT/JSON export helpers (visualization)
│       ├── flow/
│       │   ├── Edge.java
│       │   ├── FlowGraph.java
│       │   ├── FlowResult.java
│       │   ├── FordFulkerson.java
│       │   ├── EdmondsKarp.java
│       │   ├── Dinic.java
│       │   ├── MinCut.java
│       │   ├── BipartiteMatching.java
│       │   └── MinCostMaxFlow.java
│       ├── approximation/
│       │   ├── ApproximationResult.java
│       │   ├── MaximalMatching.java
│       │   ├── VertexCoverApproximation.java
│       │   └── SetCoverDemo.java
│       ├── randomized/
│       │   ├── RandomizedResult.java
│       │   ├── RandomizedQuickSort.java
│       │   ├── MillerRabin.java
│       │   ├── UniversalHashFamily.java
│       │   ├── RandomizedHash.java
│       │   └── ReservoirSampling.java
│       └── parallel/
│           ├── BenchmarkResult.java
│           ├── WorkSpanAnalyzer.java
│           ├── ParallelReduce.java
│           ├── ParallelPrefixScan.java
│           ├── ParallelSort.java
│           └── ParallelBenchmark.java
├── src/main/resources/
│   ├── application.properties
│   └── sample/                  # optional embedded copies of sample-data
└── src/test/java/com/loginsight/
    ├── dsa/                     # algorithm unit tests (mirror tree, see docs/13)
    │   ├── string/   (NaiveMatcherTest, KMPMatcherTest, ZAlgorithmTest,
    │   │              RabinKarpTest, AhoCorasickTest, SuffixArrayTest, KasaiLCPTest)
    │   ├── dp/       (LevenshteinTest, DamerauTest, NeedlemanWunschTest,
    │   │              SmithWatermanTest, MatrixChainTest, BitmaskTSPTest,
    │   │              HamiltonianPathTest, TreeDpTest, SosDpTest)
    │   ├── flow/     (FordFulkersonTest, EdmondsKarpTest, DinicTest, MinCutTest,
    │   │              BipartiteMatchingTest, MinCostMaxFlowTest,
    │   │              FlowCrossCheckTest)
    │   ├── approximation/ (VertexCoverApproximationTest, SetCoverDemoTest)
    │   ├── randomized/     (MillerRabinTest, ReservoirSamplingTest, RandomHashTest,
    │   │                    RandomizedQuicksortTest)
    │   └── parallel/       (ParallelReduceTest, ParallelPrefixScanTest,
    │                        ParallelSortTest, ParallelCrossCheckTest)
    ├── parser/    (TextLogParserTest, JsonLogParserTest, LogParserFactoryTest)
    ├── service/   (DatasetServiceTest, ServiceGraphBuilderTest)
    ├── query/     (QueryDispatcherTest, QueryValidatorTest)
    └── controller/ (SearchControllerTest, FlowControllerTest, …  MockMvc)
```

---

## 3. `frontend/`

```
frontend/
├── package.json
├── tsconfig.json               # + tsconfig.node.json
├── vite.config.ts              # /api proxy to :8080
├── index.html
└── src/
    ├── main.tsx
    ├── App.tsx                 # router + layout shell
    ├── api/
    │   ├── client.ts           # fetch wrapper, error normalization
    │   ├── logsApi.ts
    │   ├── searchApi.ts
    │   ├── similarityApi.ts
    │   ├── suffixApi.ts
    │   ├── dpApi.ts
    │   ├── flowApi.ts
    │   ├── approximationApi.ts
    │   ├── randomizedApi.ts
    │   ├── parallelApi.ts
    │   └── benchmarkApi.ts
    ├── components/
    │   ├── layout/   (TopBar, SideNav, StatusBar, AppShell)
    │   ├── dashboard/ (StatCard, LogsOverTimeChart, ErrorsOverTimeChart,
    │   │               ServiceDistributionChart, StatusCodeChart)
    │   ├── logs/     (UploadPanel, DatasetPanel, LogTable, LogFilterBar, LogPagination)
    │   ├── algorithms/ (AlgorithmTabs, ComplexityBadge, MetricsPanel, IntermediatePanel,
    │   │                AlgorithmSelector, InputPanel)
    │   ├── charts/   (recharts wrappers, BenchmarkBarChart, SpeedupChart)
    │   ├── graph/    (FlowGraphView, EdgeList, CutVisualizer, MatchingVisualizer, TrieView)
    │   ├── dsa/      (DpMatrixView, AlignmentView, LpsView, ZArrayView, SuffixArrayView,
    │   │              HashWindowView, AugmentationLog)
    │   └── common/   (Button, Input, Select, Spinner, EmptyState, ErrorBanner, CodeBlock)
    ├── pages/
    │   ├── Dashboard.tsx
    │   ├── LogExplorer.tsx
    │   ├── StringAlgorithms.tsx
    │   ├── SimilarityLab.tsx
    │   ├── SuffixLab.tsx
    │   ├── NetworkFlow.tsx
    │   ├── Approximation.tsx
    │   ├── RandomizedLab.tsx
    │   ├── ParallelLab.tsx
    │   ├── BenchmarkLab.tsx
    │   └── DSAPlayground.tsx
    ├── hooks/      (useDataset, useApi, useAlgorithms, useBenchmark)
    ├── types/      (api.d.ts, algorithms.d.ts, flow.d.ts, dataset.d.ts)
    ├── constants/  (paths.ts, algorithmCatalog.ts, complexity.ts)
    ├── utils/      (format.ts, time.ts, matrix.ts)
    └── styles/     (global.css, tokens.css, layout.css, pages/*.css)
```

---

## 4. `docs/`

```
docs/
├── 01-requirements.md        ✔ Phase 0
├── 02-architecture.md        ✔ Phase 0
├── 03-project-structure.md   ✔ Phase 0
├── 04-dsa-mapping.md         ✔ Phase 0
├── 05-string-algorithms.md   (Phase 15)
├── 06-dynamic-programming.md (Phase 15)
├── 07-network-flow.md        (Phase 15)
├── 08-approximation.md       (Phase 15)
├── 09-randomized.md          (Phase 15)
├── 10-parallel.md            (Phase 15)
├── 11-complexity-analysis.md (Phase 15)
├── 12-api-documentation.md   ✔ Phase 0
├── 13-testing.md             ✔ Phase 0
├── 14-benchmarking.md        ✔ Phase 0
├── 15-user-guide.md          (Phase 15)
├── 16-viva-questions.md      (Phase 15)
└── 17-limitations.md         (Phase 15)
```

---

## 5. `sample-data/`

```
sample-data/
├── README.md                 # format specification (Phase 0)
├── logs-small.txt            # ~1,000 lines   (Phase 2)
├── logs-medium.txt           # ~10,000 lines  (Phase 2)
├── logs-large.txt            # ~100,000 lines (Phase 2)
├── logs-malformed.txt        # deliberate format errors (Phase 2)
├── logs-json.jsonl           # JSON Lines variant (Phase 2)
└── logs-multi-service.txt    # richer service/chaining (Phase 2)
```

Dataset formats and generation rules: `sample-data/README.md`.

---

## 6. Directory Creation Policy

- Phase 1 creates `backend/` (+ Maven Wrapper) and `frontend/`; both must build.
- Algorithm packages are created *as their phase lands*: string ⇒ Phase 3, dp ⇒ Phase 4,
  flow ⇒ Phase 5, approximation ⇒ Phase 6, randomized ⇒ Phase 7, parallel ⇒ Phase 8.
- `docs/05..11` and `docs/15..17` are authored in the final documentation phase to reflect the
  implementations actually present.
- No directory is created purely for decoration; each maps to at least one tested class or file.