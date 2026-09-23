# Research — Comparable Algorithm Visualization & Learning Systems

How do serious algorithm-visualization products make execution legible? This
document extracts engineering *principles* from the systems studied during the
TextHack rebuild. Nothing is copied (no branding, layouts, code, or text); the
principles below shaped our own decisions and are recorded for traceability.

---

## 1. iFlow — Interactive Max-Flow / Min-Cut Visualizer (cf. arXiv 2411.10484)

**What it is.** An interactive Ford-Fulkerson family visualizer. Students choose
augmenting paths, set flow amounts, build residual graphs, and validate cuts.

**Patterns we took.**
- **Stage separation** (Create Graph → Iterate → Finalize) mapped onto teaching.
- **Residual + original capacity on the same edge** ("flow/capacity"), with the
  option to toggle what is shown.
- **History ledger**: every augmenting path and its pushed flow is kept as a
  record — the analogue of our run history and step ledger.
- Feedback on mistakes + an auto-complete mode for demos.

**What we did not copy.** Hand-driven execution requiring the student to perform
the algorithm (we replay *real backend executions*), the exact graph-layout
scheme, or its visual styling.

**Applies to TextHack as.** The Flow lab shows capacity + flow + residual state
from genuine trace events, keeps an augmenting-path log, and exposes min-cut
from the real result.

## 2. dpvis — DP Array Trace Visualization (cf. arXiv 2411.07705)

**What it is.** A Python library that animates dynamic programs frame-by-frame by
tracking every `READ`/`WRITE`/`MAX/MIN` on a DP array, then grouping operations
so each frame ends at a `WRITE`.

**Patterns we took.**
- **Cell highlighting by access type** (read / written / arg-extremum).
- **Traceback visualization** as an annotated final path through the table.
- Frames are derived from actual operations, never invented — identical to our
  "visualisation is a representation of computation" rule.

**We did not copy.** The Python annotation approach (our DP matrices are
produced by Java engines and shipped as trace/intermediate data).

**Applies to TextHack as.** The DP lab renders real matrices, animates the
active cell and dependency candidates, and can overlay the reconstruction path
when the trace exposes it (Levenshtein edit script).

## 3. AlgoFlow / Algorithm-Visualiser / visual-cs — Frame-Generator SPA family

**What they are.** Modern SPA visualizers whose algorithm implementations are
**frame generators**: each returns a flat, immutable array of full state
snapshots; the UI plays the frames deterministically (O(1) seek, no re-execution).

**Patterns we took.**
- **Playback as a pure function of (steps, index)** — seeking never re-runs the
  algorithm. Our `TraceStudio` replays a fixed, immutable step list with O(1)
  jump.
- **Frame/step budget with keyframe-preserving clamping** to keep the UI
  responsive on huge traces. Our backend caps at 400 steps and flags truncation;
  the UI virtualizes the timeline.
- **Category-routed visualizers** (string → cells + tables, graph → SVG nodes/
  edges, matrix → grid) — exactly the module routing in our labs.
- Keyboard playback controls and speed steppers.

**We did not copy.** Browser-only generation (our frames come from a real Java
execution), the glassmorphism styling, the search/compare UX, or any layout.

## 4. VisuAlgo MaxFlow — graph-state-synchronized execution

**What it is.** Class-room demonstration of Ford-Fulkerson/Edmonds-Karp/Dinic on
an edge-capacity graph with residuals shown from the first animation frame.

**Patterns we took.**
- **First frame = residual graph** (back edges included from t=0), so residual
  semantics are visible from the start.
- Source/sink drawn at fixed positions (left/right) to anchor the animation.
- Integer capacities to guarantee termination discussion (educational honesty).
- Keyboard shortcuts (Space play/pause, arrows step, +/- speed).

**We did not copy.** Its graph-drawing rule or any screenshots/code; we render
the residual state produced by our own traced engines over user-supplied graphs.

## 5. General product principles distilled

1. **Trace over animation.** Every on-screen transition must correspond to a
   real recorded event; animation is a rendering concern, not a truth source.
2. **Seekable immutable steps.** Store full state snapshots; playback is a
   deterministic read; never recompute to scrub.
3. **Budgets + honest truncation.** Bounded traces, flagged truncation, and
   virtualized rendering keep large runs usable without lying about completeness.
4. **Ledger/history.** Augmenting paths, DP cells, witness bases — record them;
   history and auditability increase perceived (and real) rigor.
5. **Keyboard-first control.** Space/arrows/speed apply to every replay surface.
6. **Category/lab routing.** Strings, DP, flow, etc. get purpose-built renderers
   rather than one generic "animation box".
7. **Honesty labels.** Theoretical vs observed performance, probable-prime vs
   proven-prime, exact vs approximate, recorded vs streamed — always labelled.

## 6. What we deliberately avoided

- Fake real-time (timers alone), fake benchmark numbers, and animation that
  implies a computation that never occurred.
- Copying any UI, brand, illustrations, or copyrighted materials from the
  systems above.