# LogInsight Analyzer — DSA-3 Advanced Algorithmic Log Intelligence

KLH CSE 2026-27 DSA-3 Semester 3, Team T17

## Project Summary

LogInsight Analyzer is a full-stack algorithmic log intelligence platform that applies real DSA algorithms to real log data. Every algorithm result, every trace step, and every analytics metric is computed from genuine execution — no fabricated data, no fake animations.

---

## Architecture

| Layer | Stack |
|-------|-------|
| **Frontend** | React 18 · TypeScript · Vite (port 5173, dev proxy to :8080) |
| **Backend** | Spring Boot 3.5.16 · Java 21 · In-memory (no JPA / no DB) |
| **Algorithms** | 35 registered engines across 6 DSA categories |
| **Tracing** | 13 instrumented algorithms with real-time step recording |

---

## Running the Application

### Prerequisites
- Java 21+ (`java -version`)
- Node.js 18+ (`node --version`)

### Start the Backend
```bash
cd backend
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```
Backend starts on http://localhost:8080

### Start the Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend starts on http://localhost:5173 and proxies `/api` to the backend.

### Run All Tests
```bash
cd backend
./mvnw clean verify             # 669 tests, JaCoCo coverage report
```

---

## Algorithm Categories

### String Search (7 algorithms)
Naive Pattern Search · KMP · Z-Algorithm · Rabin-Karp · Aho-Corasick Multi-Pattern · Suffix Array Build/Search · Fuzzy Edit-Distance Search

### Dynamic Programming (12 algorithms)
Levenshtein · Damerau · Weighted Edit · Needleman-Wunsch Global Alignment · Smith-Waterman Local Alignment · Matrix Chain Ordering · Optimal BST · Bitmask TSP · Hamiltonian Path · Tree Diameter DP · Rerooting DP · Sum-Over-Subsets DP

### Graph & Flow (6 algorithms)
Ford-Fulkerson Max Flow · Edmonds-Karp Max Flow · Dinic Max Flow · Min-Cut · Bipartite Matching · Min-Cost Flow

### Approximation Algorithms (3 algorithms)
Vertex Cover 2-Approximation · Incident-on-Call Cover · Greedy Set Cover

### Randomized Algorithms (4 algorithms)
Randomized QuickSort · Miller-Rabin Primality Test · Reservoir Sampling · Universal Hashing

### Parallel Algorithms (3 algorithms)
Parallel Prefix Sum (Scan) · Parallel Merge-Sort · Parallel Reduce

---

## Trace / Algorithm Laboratory

13 of the above algorithms are instrumented with a `StepRecorder` that captures every observable operation during execution. The Algorithm Laboratory (frontend `/lab` page) replays these genuine steps in sequence — never fabricated animation state.

| Category | Traceable Algorithms |
|----------|---------------------|
| Strings | Naive, KMP, Z-Algorithm, Rabin-Karp |
| Dynamic Programming | Levenshtein, Matrix Chain Ordering |
| Graph & Flow | Ford-Fulkerson, Edmonds-Karp, Dinic |
| Approximation | Vertex Cover 2-Approximation |
| Randomized | QuickSort, Miller-Rabin, Reservoir Sampling |

Trace API: `GET /api/trace/catalog` + `POST /api/trace/{category}/{algorithm}`

---

## REST API Surface

| Endpoint Group | Path | Methods |
|---------------|------|---------|
| Health | `/api/health`, `/api/health/status`, `/api/health/dataset` | GET |
| Datasets | `/api/datasets`, `/api/datasets/{name}`, `/api/datasets/current` | GET, POST, DELETE |
| Logs | `/api/logs`, `/api/logs/first`, `/api/logs/stats` | GET |
| Analytics | `/api/analytics/dependencies`, `/api/analytics/errors`, `/api/analytics/top`, `/api/analytics/windows` | GET |
| String Search | `/api/search/{naive,kmp,z,rabin-karp}`, `/api/search/multi`, `/api/fuzzy/search` | POST |
| DP | `/api/dp/{levenshtein,damerau,weighted-edit,global,local,matrix-chain,obst,tsp,hamiltonian,tree,rerooting,sos}` | POST |
| Flow | `/api/flow`, `/api/flow/{edmonds-karp,dinic,min-cut,matching,min-cost}` | POST |
| Approximation | `/api/approx/{vertex-cover,incident-cover,set-cover}` | POST |
| Randomized | `/api/random/{prime,sample,hash,quicksort}` | POST |
| Trace | `/api/trace/catalog`, `/api/trace/{search,dp,flow,approx,random}/{algorithm}` | GET, POST |
| Benchmark | `/api/benchmark/run` | POST |
| Parallel | `/api/parallel/{reduce,scan,sort}` | POST |

All endpoints are documented in `docs/12-api-documentation.md`.

---

## Testing & Coverage

| Metric | Value |
|--------|-------|
| **Total tests** | 669 (0 failures, 0 errors) |
| **Instruction coverage** | 92.7% |
| **Branch coverage** | 81.0% |
| **Line coverage** | 93.4% |

Coverage targets:
- Backend overall: ≥ 90% lines ✓
- Algorithm packages: ≥ 95% ✓
- Critical services/controllers: ≥ 90% ✓

Cross-check tests (`TraceCrossCheckTest`) verify that traced algorithm variants produce identical results to the original untraced implementations.

---

## Frontend Pages

| Page | Description |
|------|-------------|
| `/` (Overview) | Live health, dataset stats, traffic chart, service distribution, log levels |
| `/logs` | Paginated log event table with level badges, timestamps, endpoints |
| `/analytics` | Service dependency graph (SVG), traffic time series, top-K frequency, error patterns |
| `/datasets` | List available samples, load/clear dataset, current status |
| `/lab` | Algorithm Laboratory: select algorithm, edit input JSON, replay real steps with playback controls |
| `/benchmarks` | Configure and run sequential-vs-parallel benchmark sweeps |
| `/system` | Service status, uptime, engine count, build information |
| `/docs` | Architecture overview, algorithm catalog, API endpoint reference |

---

## Design Principles

1. **No fake data** — every algorithm runs for real; every trace step was recorded during genuine execution
2. **No fabricated animations** — the lab replays actual `AlgorithmStep` records from the backend
3. **Existing methods untouched** — traced variants added alongside originals; existing code never broken
4. **One commit per phase** — clean Git history, no squashing, no history rewrites
5. **Test-first verification** — all changes verified with `./mvnw clean verify` before commit

---

## Project Structure

```
loginsight-analyzer/
├── backend/
│   ├── src/main/java/com/loginsight/
│   │   ├── dsa/                    # Algorithm implementations (string, dp, flow, approximation, randomized, parallel)
│   │   ├── trace/                  # StepRecorder, AlgorithmStep, TraceCatalog, TracedResult
│   │   ├── controller/             # REST controllers (Health, Search, Dp, Flow, Trace, Analytics, etc.)
│   │   ├── service/                # Business services (Log, Dataset, Search, Trace, Benchmark, etc.)
│   │   ├── query/engine/           # Query engines (per-algorithm result builders)
│   │   ├── analytics/              # ErrorPatternAnalyzer, TimeWindowAnalyzer
│   │   ├── graph/                  # ServiceGraphBuilder, ServiceDependencyGraph, TopKFrequentAnalyzer
│   │   ├── dto/                    # Request/Response DTOs
│   │   └── exception/              # GlobalExceptionHandler, custom exceptions
│   └── src/test/                   # 669 tests (controllers, services, cross-check, analytics, graph)
├── frontend/
│   ├── src/
│   │   ├── api/                    # client.ts (typed API client), types.ts (wire contracts)
│   │   ├── components/             # Layout, UI components, TracePlayer, charts (hand-rolled SVG)
│   │   ├── hooks/                  # useApi (data-fetching hook)
│   │   ├── pages/                  # 8 routed pages
│   │   └── styles/                 # global.css (dark-theme design system)
│   └── package.json
└── docs/                           # 10 design/architecture documents
```

---

## License

Academic project — KLH University, CSE Department, 2026-27.
