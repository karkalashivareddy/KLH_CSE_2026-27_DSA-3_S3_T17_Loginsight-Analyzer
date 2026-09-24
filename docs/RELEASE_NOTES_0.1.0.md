# LogInsight Analyzer 0.1.0

## Release status

**Verified local release.** This release has not been deployed publicly; it is a self-contained
academic/portfolio build verified locally (backend test suite + frontend production build + browser
QA). See [README.md](../README.md) for how to run it.

## Product transformation

The repository evolved from the academic **"TextHack — Advanced Algorithms Laboratory"** presentation
into a product-facing surface — **LogInsight Analyzer, an Intelligent Log Analysis & Investigation
Platform**:

- Removed TextHack-, Laboratory- and Course-Map-branded pages; replaced with product pages:
  Command Center, Log Explorer, Search, Analytics, Patterns, Incidents, Services, Live, Datasets,
  Ingestion, Analysis, Algorithms, Benchmarks, Run Sessions, System, Docs.
- DSA algorithms (unchanged in substance) are now the **engineering machinery underneath** the
  product: exposed via search/fuzzy/patterns/incidents and, transparently, via the Algorithm Insights
  catalogue, measured benchmarks and step replay.
- Historical academic records remain in `docs/` (e.g. `REBUILD_BASELINE.md`, `COURSE_MAP.md`,
  `FINAL_REBUILD_REPORT.md`) for compatibility; they are not part of the current product surface.

## Major capabilities

- **Command Center** — dashboard computed live from the loaded dataset (totals, timeline, severity,
  HTTP, patterns, 7×24 heatmap, recent critical events; 5 time ranges).
- **Log Explorer** — structured filters, time window, paging, and per-event detail with raw log and
  trace/span IDs.
- **Search** — query language (`level:`, `service:`, `host:`, `source:`, `status:`, `trace:`,
  ranges); free text executed by a real KMP engine with measured duration; Levenshtein
  "did you mean" on zero hits; dataset-driven typeahead.
- **Patterns** — heuristic token-normalized message templates with sample events, labelled "not ML".
- **Incidents** — 5-minute windowed baseline thresholding (`≥ max(3, 3×baseline)`) with inspectable
  supporting evidence.
- **Analytics** — timeline / severity / heatmap / HTTP / hosts tabs plus service dependencies.
- **Services** — rollups and per-service 24-hour drill-down.
- **Live** — bounded SSE replay of the dataset, labelled `demo-replay` / "not real-time".
- **Datasets / Ingestion** — deterministic demo corpus (14,000 events, seed `20260913L`), bundled
  samples, file upload with honest parse results, Clear.
- **Algorithm Insights** — catalogue of 42 algorithms across 6 modules (36 exposed, 13 traceable);
  four-matcher measured search benchmark (Naive/KMP/Z/Rabin-Karp, winner = smallest measured time);
  run sessions recorded and replayed step-by-step over SSE.

## Verification

| Check | Result |
| --- | --- |
| Backend `.\mvnw.cmd -o verify` | 93 test classes · **732 tests · 0 failures · 0 errors** |
| Frontend `npm run build` (tsc + vite) | clean production build (54 modules) |
| Browser QA (Playwright/Chromium) | 59 views · 0 blank routes · 0 JS errors · 0 console errors · 0 network 404/500 |
| Responsive | no horizontal overflow at 1440 / 1280 / 1024 / 768 / 480 / 375 px |
| API contract | exercised against the live app (overview, logs, search, patterns, incidents, services, analytics, live, datasets, ingestion, benchmarks, runs/SSE) |

## Known limitations

- In-memory, single-current-dataset design; run history capped at 64 sessions; no persistence.
- Live screen is a labelled **demo replay**, not production telemetry.
- Patterns and incidents are deterministic heuristics with exposed evidence — not trained models.
- Benchmarks are single measured runs; the winner is per-run on the host machine.
- No database, auth, durability, or observability-scale ingestion is claimed.
- Trace replay lives on the Run Sessions page (in-page selection); no deep-link `:runId` route yet.

## Commit

`bbf5e45` — `feat: finalize LogInsight Analyzer product`