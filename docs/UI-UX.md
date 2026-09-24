# UI/UX

LogInsight presents a dark, control-room styled dashboard: the operator inspects a live in-memory
log dataset through product screens, with the DSA engine work surfaced honestly wherever it ran.

## Shell

- Left **sidebar** with the brand mark **LI** and grouped navigation:
  Command Center, Log Explorer, Search, Analytics, Patterns, Incidents, Services, Live Stream,
  Datasets, Ingestion, Analysis (hub + Algorithms + Search Benchmarks), Run Sessions, System, Docs.
- Header shows the product name and the currently loaded dataset (name + event count) from
  `GET /api/health/status` — or "No dataset loaded".
- Narrow viewports collapse grids and the sidebar icon rail via media queries.

## Screens

| Route | Screen | Loads |
| --- | --- | --- |
| `/` | Power stats, bar/line/donut charts, top patterns, recent critical events, heatmap | `GET /api/overview` |
| `/logs`, `/logs/:id` | explorer table (filters, paging) + full event detail | `/api/logs/explore`, `/api/logs/{id}` |
| `/search` | search hero with typeahead, methodology panel, results, "did you mean" | `/api/search`, `/api/search/suggest` |
| `/analytics` | tabs: timeline / severity / heatmap / http / hosts | `/api/analysis+` |
| `/patterns` | template list + level filter + per-template sample events | `/api/patterns[/examples]` |
| `/incidents` | incident list + supporting evidence | `/api/incidents` |
| `/services`, `/services/:name` | fleet cards + drill-down detail with 24h series | `/api/services` |
| `/live` | SSE replay with labelled source and progress | `/api/live` (SSE), `/api/live/status` |
| `/datasets` | current dataset, demo load, upload, samples, clear | `/api/datasets…` |
| `/ingestion` | parser/streaming status + demo + upload | `/api/ingestion/status` |
| `/analysis` | hub linking catalogue, benchmark, run sessions | — |
| `/analysis/algorithms` | module-grouped catalogue with complexity + endpoints | `/api/analysis/algorithms` |
| `/analysis/benchmarks` | measured four-matcher benchmark with winner | `/api/analysis/benchmarks/search` |
| `/runs` | recorded runs + SSE step replay (`TracePlayer`) | `/api/runs…` |
| `/system` | status + build info | `/api/health/status`, `/api/modules` |
| `/docs` | documentation | — |

## Honesty language (required everywhere)

- **No fake data**: when nothing is loaded, screens show a first-run state ("No dataset loaded…")
  with a pointer to Datasets — never a fabricated dashboard.
- **Demo is labelled**: "Demo Dataset" and the live stream label *"Demo replay stream — not
  real-time"* remain visible.
- **Not ML**: patterns say "heuristic token patterns — not ML"; incidents expose their `method`
  string and supporting logs; search shows its `strategy`/`algorithm` and measured duration.
- **Measured** benchmark note: "Measured once per matcher over the loaded dataset haystack — no
  fabricated averages."

## Palette (spec `#0B0F14` family)

- Background `#0B0F17`, panels `#111823` / `#182131`, borders `#222C3D`, hover `#202A3E`.
- Text `#E6EDF3`, muted `#8B98A5`, accent `#4EA1FF`.
- Severity: INFO cyan (`#3498DB`), WARN amber (`#F1C40F`), ERROR red (`#E74C3C`),
  FATAL deep red (`#C0392B`), DEBUG/T RACE greys/purple.
- Hand-rolled SVG charts (bar, horizontal bar, line, donut, heatmap) — no chart library.

## Interactions

- Explorer/search/brokers use debounced typeahead, `Enter` to commit, pagination (Prev/Next with
  current page) and sort/size selects.
- Pattern template and incident items are clickable to reveal sample/evidence events; log rows deep
  link to the event detail (raw line, wire attributes, trace/span ids).
- Live stream: Start/Stop replay with progress fill; completed state tells the operator to replay
  again.