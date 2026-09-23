# TRACE_ENGINE — how "trace over animation" works

Design rule from `docs/RESEARCH.md`: **the frontend never animates what the algorithm did — it
replays what the algorithm recorded.** This document ties the recorder, the wire format, and the
player together.

## 1. Recording (backend)

Trace-instrumented algorithms dispatch through `QueryDispatcher` with a `StepRecorder` attached
(see `com.loginsight` packages `query.step`/`dsa.*`). While the *real* algorithm runs it calls
`recorder.record(operation, description, state, highlighted)` at every meaningful transition:

- String engines record `COMPARE`, `MATCH`, `MISMATCH`, `LPS_STEP`, `LPS_INIT`.
- DP engines record DP-table cell fills and matrix-chain split decisions.
- Flow engines record `AUGMENT` with the augmenting path, bottleneck, and residual edge updates.
- Randomized engines record pivot choices (quicksort), witnesses (Miller-Rabin), and
  fill/replace decisions (reservoir).

The recorder bounds the step list; when the ceiling is hit it sets `truncated=true`. `truncated` is
**never guessed** — it is set by the recorder at the moment it stops.

Wire step shape (shared by `/api/trace/...` responses and run-session events):

```json
{
  "index": 3,
  "operation": "MATCH",
  "description": "Pattern fully matched ending at index 1; record start 1 and fall back to lps[0]=0.",
  "state": { "phase": "search", "i": 2, "j": 1, "start": 1, "lps": [0], "matchCount": 1, "positions": "[1]" },
  "highlighted": [1],
  "metrics": { "comparisons": 3, "fallbacks": 0 }
}
```

`highlighted` indexes into `state.tracked` (when present) or into the lab's `names` array, so the
UI can render human labels instead of raw indices.

## 2. Delivery

- **Laboratory**: `POST /api/trace/{...}` returns the whole `TraceResponse` at once.
- **Run Sessions** (Phases 4+): `POST /api/runs` executes the algorithm synchronously and stores the
  run in the bounded in-memory `RunStore` (max 64). `GET /api/runs/{id}` returns the whole record;
  `GET /api/runs/{id}/events` streams the same steps over SSE (`meta` → `step`×n → `complete`).
  Frontend consumption: `api.runsStream()` in `frontend/src/api/client.ts` parses SSE framing with a
  fetch `ReadableStream` reader (works behind the Vite proxy; EventSource cannot POST).

## 3. Playback (frontend)

`frontend/src/components/TracePlayer.tsx` is the **TraceStudio** component:

- Rendering is a pure function of `cursor` over the immutable recorded steps — seeking is free.
- Keyboard: `Space` play/pause, `←`/`→` step, `Home`/`End` jump (respects `prefers-reduced-motion`).
- The **operation ledger** lists every recorded op with jump-to-step; the **education panel** shows
  the catalog description + complexity.
- Every step renders `{operation, description, state}` tuples from the recorder — nothing synthesized.

## 4. Honest labels

- Estimated structures (e.g. memory) come from the engine's own metrics; theoretical complexity
  strings come from the catalogue, never fabricated per run.
- Probabilistic algorithms (Miller-Rabin) say `PROBABLY_PRIME`; approximation algorithms report their
  ratio and lower bound; truncated replays carry a `badge--truncated` marker.