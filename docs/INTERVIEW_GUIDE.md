# LogInsight Analyzer — Interview Guide

Answers below describe the current `rebuild/loginsight-v4` implementation. Historical rebuild counts and browser-QA figures are not current claims.

## Product

**What is it?**
A full-stack, single-dataset log investigation workspace. Users load demo, bundled or uploaded logs and then explore, search, analyse, group patterns, inspect incidents, benchmark matchers and replay recorded algorithm steps.

**What is deliberately not real-time?**
Demo Replay (route `/live`, navigation label `Live Replay`) emits a bounded SSE replay of the current in-memory event list with `source: demo-replay` and a “not real-time” label. There is no external collector, WebSocket path, retention system or production telemetry claim.

## Architecture

**Why same-origin `/api`?**
The browser uses relative `/api` paths. Vite proxies them to port 8080 in development, and Nginx proxies `/api/` to the backend service in the Compose deployment. This keeps the frontend client independent of the deployment hostname.

**Where is state?**
`DatasetService` owns one volatile current dataset. The index, analytics, search, patterns, incidents and replay read that state. There is no database or durable upload store.

**How are errors handled?**
`GlobalExceptionHandler` returns `{status,error,message,timestamp,path}` without stack traces. Dataset absence is 404, validation is 400, and unexpected failures are sanitized 500 responses. A few presence/load endpoints intentionally use small custom 404 bodies documented in [API.md](API.md).

## Search and algorithms

**What powers product search?**
`LogIndex` resolves structured fields and half-open time windows. Free text is matched by KMP over the rendered candidate text, and the response reports the algorithm and measured duration.

**What happens on a miss?**
A bounded Levenshtein pass over distinct messages can return a suggestion with distance, similarity and match count.

**How large is the algorithm surface?**
The catalogue has 42 entries across six modules, 35 registered query engines, 36 reachable entries and 13 traceable algorithms. Library-only entries are marked honestly. Algorithms not wired to a product panel are described as engines or laboratory capabilities, not hidden claims.

**Are benchmarks universal?**
No. The search benchmark runs one measured execution per matcher over the active haystack. Parallel benchmark rows use measured sequential/parallel timings and schedule-derived work/span. Host, JVM, input and scheduling affect results.

## Data and investigation

**Where does demo data come from?**
`DemoDatasetGenerator` creates 14,000 deterministic synthetic events with seed `20260913L`, re-anchored to the current hour. Bundled samples and uploads are separate sources.

**Are patterns ML?**
No. Token normalization replaces variable-looking values and groups messages. Incidents are rule-based five-minute elevated-error windows. Both expose method/evidence in the UI and API.

**What is durable?**
Nothing at runtime. The dataset, run history and trace records disappear when the backend restarts; run history is capped at 64 and traces at 400 steps.

## Trace and accessibility

**What is a trace?**
A recorded step from a real instrumented execution, containing operation, description, state, highlights and metrics. Run SSE emits `meta`, `step` and `complete`; the frontend only renders those records.

**What accessibility work exists?**
The shell has a skip link, semantic landmarks, labelled controls, focus-visible styles, keyboard command-palette/trace controls, Escape dialog handling, reduced-motion CSS and responsive breakpoints. The Command Center topology adds `role="button"` nodes with `Enter`/`Space` activation, per-node `aria-label`s carrying the health band, and paired accessible service and edge lists that mirror the SVG. Focused Layout and TopologyPanel tests verify key landmarks and rendering. No full axe, screen-reader or browser QA is claimed.

## Deployment and limitations

**How is it deployed?**
Compose builds a non-root Java backend and non-root Nginx frontend. Nginx serves the SPA, proxies `/api/` with SSE buffering disabled and falls back to `index.html`. Both services have healthchecks. The audit validated Compose syntax but could not build images because the Docker daemon was unavailable; that limitation still holds, so the images remain unbuilt.

**What would production require?**
Authentication/authorization, durable storage, secret management, TLS termination, rate limits, a real ingestion pipeline, shared run state and operational monitoring. Those are limitations, not implemented features. There is also no WebGL/3D engine, WebSocket transport, research integration or trained ML model. The Command Center topology's "3D / depth" mode is a 2.5D CSS transform over a flat SVG, not a 3D renderer, and the panel says so in both modes.

**How is the `dsa` package held to the course rule?**
`EngineScopeGuardTest` freezes a per-file manifest of the `java.util` imports that already existed in `backend/src/main/java/com/loginsight/dsa`, walks every file in that package, and fails the build on any new `java.util` import. `java.util.concurrent` (parallel) and `java.util.Random` (randomized) are course-licensed and are listed; deleting recorded usage is always allowed, so the ledger can only shrink. Non-`dsa` packages are unrestricted.

## Honest-claim traps

These are the places where a portfolio project usually overstates itself. The current implementation is explicit about each one.

- **The topology is not infrastructure.** Edges are `requestId` co-occurrence counted in the loaded logs, over the full dataset rather than the selected window. A missing edge is not proof of a missing call.
- **`eventsPerMinute` is a window rate.** It divides window events by the nominal range width, not by the observed span of the events, so a clustered window reads low.
- **`OverviewDto.systemStatus` is a hardcoded string.** Real runtime status is `/api/health/status`; the UI prefers it and keeps the DTO field only as a pre-resolution fallback.
- **Health bands are error-rate thresholds.** Healthy <5%, watch 5–<10%, elevated ≥10%. Not a health model, not an SLI evaluation.
- **"Live Replay" is a navigation label.** The stream is a finite, oldest-first replay of the loaded dataset labelled `demo-replay`; these documents call it Demo Replay.
- **A detected window is not a cause.** The incident method is a five-minute baseline heuristic, and the UI states the window does not explain the events.
- **Catalogue exposure is not product usage.** 42 entries, 35 engines and 13 traceable algorithms do not mean 42 panels; several entries are library-only.
