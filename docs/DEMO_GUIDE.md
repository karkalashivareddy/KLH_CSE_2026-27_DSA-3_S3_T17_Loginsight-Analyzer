# LogInsight Demo Guide

A short, honest walkthrough of the current product. Every number shown in the demo is produced by the
backend: the frontend renders measured frames and never substitutes browser-generated telemetry.

This guide has two parts. **Part A** is the headline live demonstration: healthy → degrading →
detection → incident → investigation → recovery → resolved. **Part B** is the dataset-side tour.

---

## 1. Start the stack

Local development:

```powershell
# terminal 1 — backend on http://localhost:8080
cd backend
.\mvnw.cmd spring-boot:run
```

```powershell
# terminal 2 — frontend on http://localhost:5173
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` to port 8080, so the browser uses same-origin API
paths. Wait for `Started LogInsightApplication` in the backend terminal before opening the UI.

### Guided product demo

From the Command Center, choose **Start guided demo**. It visits the existing overview, search,
analytics, services, incidents and algorithms routes. Each step calls its corresponding backend API
and displays returned evidence. If no dataset is active, the first step loads the deterministic
backend demo corpus; if a dataset is already active, it keeps that source. Search, time buckets,
observed request-trail topology, heuristic incident windows and trace catalogue data are fetched from
the server. Failures remain visible with a retry action. Use Previous/Next or the arrow keys; Escape
exits. The tour describes inferred topology and heuristic incidents with those limitations intact.

This feature has a focused Vitest interaction test. It has not been exercised in a real browser in
this revision.

Docker evaluation:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
```

Open `http://localhost:8088`. Nginx serves the SPA and proxies `/api/` to the backend. Container image
builds and runtime smoke tests have **not** been executed on this branch — no Docker daemon is
available in the audit environment, so only `docker compose config` has been validated.

---

## 2. Part A — the live demonstration

### 2.1 Overview (`/`)

Open `/`. The page opens on a selected window over the currently loaded dataset. If no dataset is
loaded the shell says so and offers source selection instead of inventing metrics. Confirm the header
pill is reading `GET /api/health/status`, and that it reports `datasetLoaded` honestly.

### 2.2 Scenario Lab (`/scenario-lab`)

Open **Scenario Lab**. Seven scenarios are returned by `GET /api/scenarios`, each declaring its origin
service, affected services, expected signal, expected incident, seed and speed. Select
**Checkout 5xx Cascade** (`checkout-5xx-cascade`) — it is the default, it is `CRITICAL`, and it names
three affected services, so the blast radius traversal is visible. Leave the seed at its declared value
so the run is reproducible.

### 2.3 Run the scenario

Press **Run**. Before starting the stream the page previews one real server frame through
`GET /api/simulation/sample`, so the panel shows measured output rather than placeholders.

Watch the phase label move through:

| Phase | What the backend is doing |
|---|---|
| `healthy` | intensity 0 — baseline event mix, error rate near zero |
| `onset` | intensity rising, first error signatures appearing |
| `degrading` | intensity ≥ 0.35 — the detector's opening threshold |
| `peak` | intensity ≥ 0.85 — peak error rate and latency reached |
| `healthy` again | cooldown tail, after `durationTicks` |

Timing is deterministic and derived from `TICK_MILLIS = 250 ms`. For `checkout-5xx-cascade`
(duration 240 ticks, onset at tick 12, recovery at tick 40, plus an 80-tick cooldown tail):

- failure becomes measurable at about **tick 12** (~3 s at speed 1),
- the run reaches its peak between about **tick 40 and tick 200**,
- recovery begins at about **tick 40** (~10 s), and
- the whole run is **320 ticks ≈ 80 s at speed 1**, or ≈40 s at speed 2.

Raise the speed control if the evaluator is waiting; speed changes delivery pace only and never changes
event content.

### 2.4 Show the measured change

As intensity rises, from server frames only:

- **error rate** climbs toward the scenario's declared peak error rate,
- **p95 latency** rises away from the measured `baselineP95LatencyMs`,
- **throughput** (`eventsPerSecond`) follows the scenario intensity curve,
- the **service health** chips change band, and the services named in the scenario show it,
- the **signature** appears in the evidence panel once Aho-Corasick matches a declared signature and
  KMP re-counts it.

Nothing here is computed in the browser. The frontend derives its display from the frame the server
sent.

### 2.5 Incident and evidence

Once a primary signal, a confirmed signature and intensity ≥ 0.35 coincide after the onset phase, the
backend opens an incident. On the page you should see the incident card, the measured evidence rows and
the services named. Open **Incident Workbench** (`/incidents/workbench`) — this is the flagship
investigation surface, in three zones:

- **Left** — incident navigation.
- **Centre** — severity, lifecycle, timeline, signals, algorithm evidence and the supporting events.
- **Right** — origin service, affected services, blast radius, topology and health.

Every lifecycle control on this page calls the backend; the page does not move a state locally.

### 2.6 Blast radius and topology

Open the topology in the right zone. The simulation surface is labelled **DECLARED DEPENDENCIES** —
the graph is the scenario's declared dependency graph, not observed infrastructure. The blast radius is
a graph traversal (BFS reachability) from the origin service over those declared edges. Say "declared
dependency reachability" out loud; do not say root cause.

### 2.7 Lifecycle, recovery and resolution

The incident progresses `DETECTED` → `INVESTIGATING` (after 8 sustained signalling windows) →
`MITIGATED` (once the scenario has passed recovery and intensity is decaying) → `RESOLVED`. Two things
are worth demonstrating:

- an illegal transition is rejected by the backend with `409`, not silently accepted;
- `RESOLVED` is **measurement-driven**: it requires intensity back at the healthy threshold *and* the
  measured per-tick error rate at or below 1%, so the green state is a genuine observation.

The Workbench timeline persists for the session. Lifecycle state is session-scoped and in-memory; it is
not persisted across a backend restart.

### 2.8 Stop and reset

Press **Stop** to end the stream, then **Reset** to clear the session. Confirm that the incident,
stream, events, chart and topology all clear, and that starting another scenario produces a fresh
session with its own ids. Repeat the run with a different scenario and a different seed to show the
content changes.

---

## 3. Part B — dataset-side tour

1. **Load data.** Open **Datasets** or **Ingestion** and choose the deterministic Demo Dataset, a
   bundled sample from `sample-data/`, or upload text/JSONL. The parser reports successful and failed
   line counts. Dataset state is in-memory: a restart clears it.
2. **Command Center window.** Switch between `5m` and `24h`. Point out the printed
   `windowStart`–`windowEnd` and the `selected-window` scope, and the coverage card comparing window
   events against the unfiltered dataset total. The rate label divides by the nominal range width, so a
   clustered window reads low by design.
3. **Topology honesty.** On the dataset topology, state that edges come from `requestId`
   co-occurrence in the loaded logs — labelled **OBSERVED DEPENDENCIES** — not from a service registry
   or an APM agent, and that a missing edge is not proof of a missing call. Toggle the 3D WebGL view;
   nodes and edges come from the same backend response as the SVG view, and the 2D view plus an
   accessible service list remain available.
4. **Health bands.** Read the thresholds printed in the service health subtitle: healthy <5%,
   watch 5–<10%, elevated ≥10%. These are error-rate thresholds, not a health model.
5. **Search and patterns.** Open **Logs** and **Algorithmic Search**. Use `level:ERROR`,
   `service:...` and free text. KMP runs the product search; the zero-hit "Did you mean?" suggestion is
   Levenshtein. Patterns are token heuristics, not ML.
6. **Dataset incidents.** Open **Detector Windows** (`/incidents`, alias `/investigate/:id`). These are
   dataset-derived elevated-error windows with inspectable evidence, separate from the simulation
   incident lifecycle.
7. **Algorithm Lab.** Open **Algorithm Lab** (`/analysis`), **Algorithms** (`/algorithms`) and
   **Benchmarks** (`/benchmarks`). The catalogue is read from `GET /api/analysis/algorithms`; filter it,
   inspect a row and show its complexity plus `traceable` / `exposed` badges. Benchmarks measure one
   run per matcher over the same loaded haystack and are host-dependent.
8. **Run sessions.** Open **Run Sessions** (`/runs`). Runs are created server-side through
   `POST /api/runs`; play or stream a recorded run's steps over SSE. Run history is capped at 64 and
   trace steps at 400.
9. **Dataset Replay.** Open **Dataset Replay** (`/replay`) and start the bounded SSE stream. The source
   stays `demo-replay`, events arrive oldest-first, and the page carries a "not real-time" disclosure.
   Note that this is **not** the Live Monitor: `/live` is the generated simulation.
10. **System.** Open **System** for health, dataset state and registered engine counts. The full error
    and limit contract is in [API.md](API.md).

---

## 4. Closing limitations

State plainly that the system is single-process and in-memory, has no authentication or durable
storage, does not ingest an external live stream, and uses no WebSocket transport, no trained ML model
and no research integration. Simulation traffic is generated; dataset traffic is loaded. Blast radius
is dependency reachability, not confirmed causality. The 3D topology is an optional WebGL renderer over
the same backend data and requires device WebGL support, with an SVG fallback.

Docker image builds and container smoke tests have not been run on this branch. See
[COMMAND_CENTER.md](COMMAND_CENTER.md), [DEPLOYMENT.md](DEPLOYMENT.md) and
[IMPLEMENTATION_AUDIT.md](IMPLEMENTATION_AUDIT.md) for the audited contract and validation limits.
