# LogInsight Analyzer

**Intelligent Log Analysis & Investigation Platform** — a full-stack system that turns classical
data-structure and algorithm theory into a working log-analysis product: search, fuzzy matching,
pattern discovery, analytics, incident investigation, benchmarking, and traceable algorithm
execution.

> Academic/portfolio project (DSA-3), not a deployed production platform. Every number on screen is
> computed live from a loaded dataset or a measured algorithm run.

---

## What is this?

LogInsight Analyzer is an algorithmically engineered log analysis and investigation platform. You
load (or generate) a log dataset, then investigate it through a React dashboard backed by a Spring
Boot API — and because the DSA engines are genuine, you can inspect **which** algorithm ran, on
**what** haystack, and **how long it measured**, down to step-by-step replay.

## Why does it exist?

To demonstrate how classical algorithms solve realistic software-engineering problems:

| Engineering problem | Algorithmic solution |
| --- | --- |
| find events matching a phrase | KMP string matching |
| suggest a correction on a miss | Levenshtein edit distance |
| discover recurring message shapes | heuristic token normalization |
| spot error surges | windowed baseline thresholding |
| compare matchers | measured benchmark (Naive/KMP/Z/Rabin-Karp) |
| explain an algorithm run | step recording + SSE replay |

## Tech stack (exactly what is used)

| Layer | Technologies |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5, Spring Web (REST + SSE), in-memory |
| Frontend | React 18, TypeScript, Vite, React Router — no chart/state libraries, hand-rolled SVG charts |
| Data | in-memory dataset (demo generator, bundled samples, file upload); no external database |
| Quality | 732 JUnit tests (0 failures), Maven Wrapper, JaCoCo, GitHub Actions verify |

## Architecture (short)

```mermaid
flowchart LR
    U[Browser] --> F[React + TypeScript + Vite]
    F -->|REST /api| B[Spring Boot API]
    F -->|SSE /api/live · /runs/{id}/events| B
    B --> D[DatasetService · in-memory dataset]
    B --> I[LogIndex · position lists]
    B --> S[Search · KMP + Levenshtein]
    B --> P[PatternExtractor · IncidentDetector]
    B --> A[DSA engines · 42 algorithms / 6 modules]
    B --> L[LiveStreamService · demo replay]
```

Full labelled diagram: [docs/ARCHITECTURE_DIAGRAM.md](docs/ARCHITECTURE_DIAGRAM.md).

## Features

- **Command Center** (`GET /api/overview`) — events/errors/services/hosts, traffic timeline,
  severity, HTTP status, top patterns, 7×24 heatmap, recent critical events; every value derived
  live from the loaded dataset.
- **Log Explorer** — structured filters, time window, paging, per-event detail (timestamp, severity,
  service, host, source, message, raw log, trace/span IDs, attributes).
- **Search** — `level:ERROR`, `service:auth`, combined structured queries; free text executed by a
  real **KMP** engine with measured duration; **"Did you mean?"** via **Levenshtein** on zero hits;
  live typeahead.
- **Analytics** — timeline / severity / heatmap / HTTP / hosts tabs; service dependencies.
- **Patterns** — heuristic message templates with sample events, labelled "not ML".
- **Incidents** — 5-minute windowed baseline thresholding (`≥ max(3, 3×baseline)`) with exposeable
  supporting evidence.
- **Services** — per-service rollups and 24-hour drill-down.
- **Live** — bounded SSE replay of the dataset, labelled `demo-replay` / "not real-time".
- **Datasets / Ingestion** — one-click deterministic demo corpus (14,000 events, seeded), file
  upload with honest parse summaries, Clear.
- **Algorithm Insights** — catalogue of **42 algorithms across 6 modules** (36 exposed, 13
  traceable), measured search benchmark, and run sessions replayed step-by-step over SSE.

## Running locally

Prerequisites: **Java 21+** and **Node.js 18+**.

```powershell
# Terminal 1 — backend (http://localhost:8080)
cd backend
.\mvnw.cmd spring-boot:run            # ./mvnw spring-boot:run on Linux/macOS

# Terminal 2 — frontend (http://localhost:5173, proxies /api -> 8080)
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173> → **Datasets** → **Load Demo Dataset** and every screen populates with
honest numbers. A narrated 5–8 minute demo is in [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md).

## Testing (verified commands)

```powershell
cd backend
.\mvnw.cmd -o verify                  # 732 tests / 0 failures / 0 errors

cd ..\frontend
npm run build                         # tsc + vite production build
```

Current verification: **93 test classes · 732 tests · 0 failures · 0 errors** and a clean
TypeScript + Vite build; GitHub Actions runs both on push/PR.

## Algorithmic foundation

Concise mapping (full detail: [docs/DSA_PRODUCT_MAPPING.md](docs/DSA_PRODUCT_MAPPING.md)):

- **M2 String algorithms** — Naive, KMP, Z, Rabin-Karp (search + benchmark), Aho-Corasick,
  Suffix Array/Kasai.
- **M3 Dynamic programming** — Levenshtein/Damerau/Weighted edit distance (fuzzy search),
  alignment, tree/interval/bitmask/SOS DP.
- **M4 Network flow** — Ford-Fulkerson, Edmonds-Karp, Dinic, min-cost, min-cut, matching.
- **M5 Approximation** — vertex cover (2-approx, kernelized, bounded), set cover, matching, FPTAS.
- **M6 Randomized** — randomized quicksort, Miller-Rabin, reservoir sampling, hashing.
- **M6 Parallel** — parallel sort/reduce/prefix-scan with work-span analysis.

Algorithms that power product screens are marked **product feature**; the rest are honestly labelled
**algorithm engine / analytical capability** (real, exposed, benchmarked and runnable via the
catalogue).

## Engineering characteristics

- Deterministic, reproducible demo dataset (seed `20260913L`).
- Measured — not simulated — algorithm execution and benchmarks (single-run disclaimers on screen).
- Traceable execution: 13 algorithms record steps replayed over SSE.
- REST + SSE contract documented in [docs/API.md](docs/API.md); controllers map 1:1.
- Structured query parser combined with indexed position-list intersections.
- 732 executing tests including brute-force cross-checks for the DSA engine.
- Responsive React UI verified headlessly at 1440 → 375 px.

Honest limits (by design): in-memory single dataset, run history capped at 64, live = labelled demo
replay, patterns/incidents are heuristics (not AI/ML), single-measured-run benchmarks, no
production deployment.

## Documentation

- [Demo script](docs/DEMO_SCRIPT.md) · [Project walkthrough](docs/PROJECT_WALKTHROUGH.md)
- [Architecture diagram](docs/ARCHITECTURE_DIAGRAM.md) · [API](docs/API.md)
- [DSA → product mapping](docs/DSA_PRODUCT_MAPPING.md) · [Algorithms](docs/ALGORITHMS.md)
- [Dataset](docs/DATASET.md) · [UI/UX](docs/UI-UX.md) · [Release notes](docs/RELEASE_NOTES_0.1.0.md)
- [Portfolio summary](docs/PORTFOLIO_SUMMARY.md) · [Interview guide](docs/INTERVIEW_GUIDE.md)
- [LogInsight rebuild report](docs/LOGINSIGHT_REBUILD_REPORT.md)
- Historical academic records (kept for compatibility): [Rebuild baseline](docs/REBUILD_BASELINE.md),
  [Course map](docs/COURSE_MAP.md), [Final rebuild report](docs/FINAL_REBUILD_REPORT.md)

## Author

**Karkala Shiva Reddy** — <https://github.com/karkalashivareddy>