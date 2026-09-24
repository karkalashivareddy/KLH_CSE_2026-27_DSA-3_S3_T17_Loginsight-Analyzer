# LogInsight Analyzer — Demo Script

A guided **5–8 minute** walkthrough of LogInsight Analyzer, designed to show the product naturally
from first click to the algorithm layer underneath. Every value shown on screen is computed from the
loaded dataset or a measured algorithm run — nothing is scripted, faked, or hard-coded in the UI.

Verify against commit `bbf5e45` (see [RELEASE_NOTES_0.1.0.md](RELEASE_NOTES_0.1.0.md)).

---

## 1. Demo objective

LogInsight Analyzer demonstrates how classical data-structure and algorithm theory can be turned
into a working software product: an **intelligent log analysis and investigation platform** where
the DSA engines are genuine, measurable machinery underneath product features such as search,
fuzzy matching, pattern discovery, analytics, incident investigation, benchmarking, and traceable
algorithm execution.

The demo answers three questions:

1. **"Does it work?"** — the product runs, loads a dataset, and produces honest numbers.
2. **"How is it built?"** — REST/SSE full-stack architecture, in-memory dataset, no fabricated metrics.
3. **"Where is the computer science?"** — every feature maps to an implemented, tested algorithm.

---

## 2. Setup (exact commands)

Prerequisites: **Java 21+** and **Node.js 18+**.

```powershell
# Terminal 1 — backend (:8080)
cd backend
.\mvnw.cmd -q -o package        # optional: build the boot jar first
.\mvnw.cmd spring-boot:run      # or: java -jar target\loginsight-analyzer-0.1.0-SNAPSHOT.jar

# Terminal 2 — frontend (:5173, proxies /api -> :8080)
cd frontend
npm install
npm run dev
```

Open **<http://localhost:5173>** in a browser. Vite proxies `/api` to the running backend.

> Tip: if the Command Center already shows data, click **Clear** on the Datasets page first and
> reload — this demo starts from the *first-run* state ("No dataset loaded").

---

## 3. Demo flow (step by step)

### Step 1 — Command Center with no dataset (first-run honesty)

**Where:** `/` (Command Center).

- The screen shows **"No dataset loaded."** with working links to **Datasets** and **Ingestion**.
- **Talking point:** the product refuses to show invented numbers. Every dashboard, count and
  incident derives from a dataset that is actually in memory. This is a deliberate design decision.

### Step 2 — Load the demo dataset

**Where:** Datasets (`/datasets`) → **Load Demo Dataset**.

- A button press generates a **deterministic 14,000-event corpus** (fixed seed `20260913L`,
  ~8 services, hosts, HTTP traffic, error bursts) and loads it in one click.
- The page shows the dataset summary and loading status.
- **Talking point:** the dataset is generated **by the application**, on demand, reproducibly — the
  same corpus every time. File upload and bundled sample files are also available (JSONL / text,
  parser auto-detected, failed lines honestly reported).

### Step 3 — Command Center (live dashboard)

**Where:** `/` after loading.

Show, left to right:

- **Stat cards** — total events, errors, warnings, services, hosts, events/min, active incidents.
- **Timeline** (traffic over time) and **Severity distribution**.
- **Top services**, **HTTP status codes**, **top message patterns**.
- **Activity heatmap** (7×24, hours in UTC) and **recent critical events**.
- Range tabs: `5m / 15m / 1h / 6h / 24h` recompute everything server-side.

**Talking points:** every number is computed live by the backend from the loaded dataset; the heatmap
and charts are hand-rolled SVG — no chart library.

### Step 4 — Log Explorer and an event detail

**Where:** Logs (`/logs`) and click any row (or open a recent critical event).

- Filters: level, service, host, time window, paging, free-text.
- Event detail: timestamp, severity, service, host, source, message, HTTP method/status, raw log,
  trace/span IDs when present, plus per-event attributes.

**Talking point:** the explorer reads from an in-memory index (position lists + time windows), so
filters are exact and instant; the detail page is a plain REST lookup (`GET /api/logs/{id}`).

### Step 5 — Search (the DSA moment begins)

**Where:** Search (`/search`).

Run, in order:

| Query | What it shows |
| --- | --- |
| `level:ERROR` | Structured filter via the index — every event at ERROR level. |
| `service:auth` | Structured filter on the auth service. |
| `level:ERROR service:auth` | Combined filters — positional indices intersected. |
| `connection refused` (plain text) | Substring search executed by the **KMP** matcher over the rendered haystack. |

For a plain-text search the result meta shows the **methodology** — strategy, **algorithm: KMP**,
pattern length, text size and the **measured duration**.

Then trigger the fuzzy path: type a typo such as `paymentd rejected`, submit, and the UI shows a
**"Did you mean?"** suggestion computed with **Levenshtein edit distance** (distance, similarity %,
match count) — not a canned string.

**Talking points:**

- Structured filters use indexed position lists (field → sorted positions, intersections).
- Free text is matched by a real KMP engine: `search/LogSearchService.java` → `dsa/string/KMPMatcher.java`.
- Suggestion fallback: `dsa/dp/editdistance/LevenshteinDistance.java` over distinct messages.
- Typeahead feeds field-filters from the live dataset (`GET /api/search/suggest`).

### Step 6 — Patterns (heuristic, not ML)

**Where:** Patterns (`/patterns`).

- Click a pattern to expand its **sample events**.
- The UI explicitly carries the label **"Heuristic pattern extraction — Not ML"**.

**Talking point:** templates are produced by deterministic token normalization
(`pattern/PatternExtractor.java`): replacing variable values (numbers, UUIDs, IPs) with placeholders,
then grouping by normalized template. No training, no model — which is why the claim is honest.

### Step 7 — Incidents (evidence-based)

**Where:** Incidents (`/incidents`).

- Open an incident — it shows the **method** (rule-based heuristics), the incident **window**,
  **baseline**, **threshold**, affected services, the primary message pattern, and the supporting
  evidence logs.

**Talking points** (exact logic in `incident/IncidentDetector.java`):

- Only ERROR/FATAL events are considered.
- The dataset's time span is bucketed into fixed **5-minute windows**.
- Baseline = average errors per window.
- A window is elevated when its count ≥ `max(3, 3 × baseline)`.
- Consecutive (or one-window-apart) elevated windows are merged into incidents.
- This is **rule-based anomaly grouping, not AI**. The evidence is inspectable.

### Step 8 — Analytics

**Where:** Analytics (`/analytics`) — tabs: `timeline | severity | heatmap | http | hosts`.

- HTTP analysis (status, methods, endpoints with measured latency percentiles), hosts rollup,
  heatmap, per-window time series, top entities, errors, and **service dependencies**.

**Talking point:** analytics are computed by pure-Java analyzers
(`analytics/*.java` — Timeline, Severity, Heatmap, Fleet, Frequency, and a service dependency graph
in `graph/ServiceDependencyGraph.java`).

### Step 9 — Algorithm Insights (reveal the DSA layer)

**Where:** Analysis (`/analysis`), Algorithms (`/analysis/algorithms`), Benchmarks
(`/analysis/benchmarks`).

- The **Algorithm Catalogue** groups **42 algorithms across 6 modules** (M2 Strings, M3 DP, M4 Flow,
  M5 Approximation, M6 Randomized, M6 Parallel) with complexities, endpoints and trace badges.
- Run a **search benchmark**: the same pattern over the same haystack is executed by **Naive, KMP,
  Z-Algorithm and Rabin-Karp**; the winner is the **smallest measured time**, with a note that it's a
  single measured run.
- **Run Sessions** (`/runs`): create a run from the catalogue, then **replay it step-by-step** over
  SSE in the Trace Player (13 traceable algorithms).

**Talking point — the relationship to the product:**

```text
Product problem → Algorithmic technique → Measured execution → Trace/benchmark → Observable result
```

Example: *"find events about connection failures"* is a product problem; the algorithmic technique is
**string matching**; the execution is measured (`KMP`, duration in ns); the benchmark compares four
matchers; the trace player shows each step; the observable result is the hit list in the UI.

### Step 10 — Live (honest streaming)

**Where:** Live (`/live`).

- Click **Start replay**; events stream in batches over Server-Sent Events.
- The UI keeps the disclosure visible at all times: **`source: demo-replay` — "Demo replay stream —
  not real-time"**.

**Talking point:** this is a bounded SSE replay of the loaded dataset
(`service/LiveStreamService.java`), not production telemetry. The distinction is kept on-screen
deliberately.

---

## 4. Talking points per screen

| Screen | What you see | Problem it solves | Backend source | Algorithmic support |
| --- | --- | --- | --- | --- |
| Command Center | totals, timeline, severity, HTTP, patterns, heatmap, critical events | instant health picture of logs | `OverviewService` + `analytics/*` | counting / histogram-style passes, heaps |
| Log Explorer | filters, paging, detail with raw log | navigation of raw events | `LogService`, `LogIndexService` | indexed position lists + binary-search window lookups |
| Search | methodology + KMP results, fuzzy suggestion | fast, transparent search | `LogSearchService` | KMP (substring), Levenshtein (fuzzy) |
| Patterns | templates + samples, "not ML" | recurring message shapes | `PatternExtractor` | token normalization heuristics |
| Incidents | window, baseline, threshold, evidence | anomaly grouping | `IncidentDetector` | windowed baseline thresholding |
| Analytics | tabs incl. HTTP/hosts/heatmap/dependencies | multi-facet analysis | `AnalyticsService` | histogram/aggregation + dependency graph |
| Algorithm Insights | catalogue, benchmark, run session | demonstrate the DSA machinery | `CatalogService`, `SearchBenchmarkService`, `RunService` | all six modules |
| Live | bounded SSE replay, "not real-time" | demonstrate streaming without faking | `LiveStreamService` | — |

---

## 5. DSA explanation (course-module mapping)

Only what genuinely exists in this repository. Product features on the left are driven by the
algorithms below them.

```text
Log search (free text)
→ String matching — KMP  (dsa/string/KMPMatcher.java)
  also Naive / Z / Rabin-Karp available and measured in the search benchmark

Fuzzy search / "did you mean"
→ Dynamic programming — Levenshtein (Wagner-Fischer) (dsa/dp/editdistance/LevenshteinDistance.java)
  Damerau-Levenshtein and weighted edit distance also implemented for the DP module

Pattern / template normalization
→ Heuristic token processing (pattern/PatternExtractor.java) — deliberately not ML

Incident detection
→ Windowed baseline thresholding (incident/IncidentDetector.java) — rule-based heuristics

Analytics (timeline, severity, heatmap, fleet, frequency, dependencies)
→ Aggregation passes + dependency graph (analytics/*, graph/ServiceDependencyGraph.java)

Search benchmark
→ Naive, KMP, Z, Rabin-Karp on the same haystack; winner = smallest measured time

Run sessions / traceable replay
→ 13 traceable algorithms recorded via trace/StepRecorder and replayed over SSE

Algorithm Insights catalogue (42 algorithms, 6 modules)
→ Strings · DP · Flow · Approximation · Randomized · Parallel, each exposed over REST and testable
```

Algorithms that are part of the DSA catalogue but **not wired into a specific product screen**
(flow, approximation, randomized, parallel, most DP variants) are honestly labelled
**"Algorithm engine / analytical capability"** — they are real, exposed, benchmarked and runnable,
but we do not claim they power a product panel they do not drive.

---

## 6. Honest limitations (say these in the demo — do not hide them)

- **Demo dataset:** the 14,000-event corpus is generated deterministically in-memory; it is a
  realistic but synthetic stand-in, not production logs.
- **Live replay is not production streaming:** the Live screen replays the dataset over SSE and says
  so on-screen (`demo-replay`, "not real-time").
- **Patterns are heuristic, not ML:** no training data or models.
- **Incidents are rule-based:** baseline/threshold heuristics over 5-minute windows, with inspectable
  evidence — not a trained detector.
- **Benchmarks are single measured runs:** they show which matcher was fastest on this machine, for
  this haystack/pattern, at this moment — not a universal performance claim.
- **No production claims:** the platform is an academic/portfolio system with an in-memory design and
  a capped run history (64 sessions); it is not deployed to production and does not claim
  observability-scale ingestion.