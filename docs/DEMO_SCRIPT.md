# LogInsight Analyzer Demo Script

A short, honest walkthrough of the current shell. The demo uses real backend output; it does not generate browser-side replacement data.

## Setup

Local development:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` to port 8080.

Docker evaluation:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
```

Open `http://localhost:8088`. Nginx serves the SPA and proxies `/api/` to the backend.

## Walkthrough

1. **First run**: open `/`. The shell shows no dataset rather than invented metrics. Use Datasets or Ingestion to continue.
2. **Load data**: choose the deterministic Demo Dataset, a bundled sample or upload text/JSONL. Show the returned event, total-line and failed-line counts.
3. **Command Center**: the page opens on the selected window. Point at the range buttons, the printed `windowStart`–`windowEnd` and the `selected-window` scope, and the coverage card showing window events against the unfiltered dataset total. Switch between `5m` and `24h` and show that the rate label says "Selected-window observed rate" — it divides by the nominal range width, so a clustered window reads low.
4. **Topology honesty**: on "Observed request-trail topology", state that edges come from `requestId` co-occurrence in the loaded logs, not from a service registry or APM agent, and that the graph covers the full dataset rather than the selected window. Toggle `3D / depth` and point at the renderer note: "2.5D / SVG perspective · not WebGL". There is no WebGL context. Show that particle counts and edge widths follow the observed weight, and that reduced-motion renders static particles. Select a node and open that service.
5. **Health bands**: in the service health matrix, read the thresholds printed in the subtitle — healthy <5%, watch 5–<10%, elevated ≥10% — and say plainly that these are error-rate thresholds, not a health model.
6. **Health vs compatibility status**: the header pill reads `GET /api/health/status`. `OverviewDto.systemStatus` is a hardcoded `"Operational"` compatibility field and is only a transient fallback before the health request resolves.
7. **Investigate**: open Logs and Search. Use `level:ERROR`, `service:...` and free text. Point out KMP, measured duration, snippets and the Levenshtein zero-hit suggestion.
8. **Explain findings**: open Patterns and Incidents. State that patterns are token heuristics, not ML, and incidents are five-minute baseline groups with inspectable evidence. Incident evidence is also reachable at `/investigate/{id}`.
9. **Pipeline story**: back on the Command Center, walk the Load → Observe → Detect → Investigate strip and open each destination to show it is the same returned data.
10. **Inspect algorithms**: open Algorithms and Benchmarks. The catalogue has 42 entries across six modules and is read from `GET /api/analysis/algorithms`; filter it, inspect a row and show its complexity, `traceable`/`exposed` badges and canonical/trace endpoint paths. The search benchmark measures one run per matcher over the same loaded haystack.
11. **Replay a run**: runs are created server-side through `POST /api/runs` and listed in Run Sessions. Open a recorded run, then play or stream its recorded steps over SSE. Mention the 64-run and 400-step caps and that the browser only renders server-recorded steps.
12. **Replay data**: open Demo Replay (route `/live`, navigation label `Live Replay`) and start the bounded SSE stream. The source remains `demo-replay` and events arrive oldest-first. Note that the Command Center replay card shows the same progress because both screens share one `ReplayProvider` subscription, and that it is not real-time capture.
13. **Check system**: open System for health, dataset state and engine count. The full error and limit contract is in [API.md](API.md).

## Closing limitations

State plainly that the system is single-process and in-memory, has no authentication or durable storage, does not ingest an external live stream, and does not use WebSockets, WebGL/3D rendering, a trained ML model or a research integration. The topology depth mode is a 2.5D CSS transform over a flat SVG, not a 3D engine.

Docker image builds and container smoke tests have not been run on this branch: no Docker daemon is available in the audit environment, so only `docker compose config` has been validated.

See [COMMAND_CENTER.md](COMMAND_CENTER.md), [DEPLOYMENT.md](DEPLOYMENT.md) and [IMPLEMENTATION_AUDIT.md](IMPLEMENTATION_AUDIT.md) for the audited contract and validation limits.
