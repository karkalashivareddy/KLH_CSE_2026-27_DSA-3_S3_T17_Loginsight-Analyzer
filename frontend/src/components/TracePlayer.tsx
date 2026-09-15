import { useEffect, useMemo, useRef, useState } from 'react';
import type { TraceResponse, TraceStep } from '../api/types';
import { formatNanos } from './format';

/**
 * Step-by-step replay of a real algorithm trace. Every step shown here was recorded by the backend
 * while the genuine algorithm executed — the player never invents animation state.
 */

function stepHighlights(step: TraceStep, names?: string[]): string[] {
  if (!step.highlighted) return [];
  const tracked = step.state?.tracked as (string | number)[] | undefined;
  return step.highlighted.map((id) => {
    if (tracked && id >= 0 && id < tracked.length && tracked[id] !== undefined) return String(tracked[id]);
    if (names && id >= 0 && id < names.length) return names[id];
    return String(id);
  });
}

export default function TracePlayer({ trace, names }: { trace: TraceResponse; names?: string[] }) {
  const steps = trace.steps;
  const [cursor, setCursor] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [speed, setSpeed] = useState(700);
  const timerRef = useRef<number | null>(null);

  const last = Math.max(0, steps.length - 1);
  const current = steps[Math.min(cursor, last)];

  const stepHighlightsMemo = useMemo(
    () => steps.map((s) => stepHighlights(s, names)),
    [steps, names]
  );

  useEffect(() => {
    setCursor(0);
    setPlaying(false);
  }, [trace]);

  useEffect(() => {
    if (!playing) return;
    timerRef.current = window.setInterval(() => {
      setCursor((c) => {
        if (c >= steps.length - 1) {
          setPlaying(false);
          return c;
        }
        return c + 1;
      });
    }, speed);
    return () => {
      if (timerRef.current !== null) window.clearInterval(timerRef.current);
      timerRef.current = null;
    };
  }, [playing, speed, steps.length]);

  if (steps.length === 0) {
    return <div className="empty-state">The trace recorded no observable steps.</div>;
  }

  const highlights = stepHighlightsMemo[cursor] ?? [];
  const stateEntries = Object.entries(current.state ?? {});

  return (
    <div className="trace-player">
      <div className="trace-toolbar">
        <button className="btn btn-sm" onClick={() => setCursor(Math.max(0, cursor - 1))} disabled={cursor === 0}>◀</button>
        <button
          className="btn btn-sm"
          onClick={() => setPlaying((p) => !p)}
          disabled={cursor >= last}
        >
          {playing ? '❚❚' : '▶'}
        </button>
        <button className="btn btn-sm" onClick={() => setCursor(Math.min(last, cursor + 1))} disabled={cursor >= last}>▶▶</button>

        <input
          className="range"
          type="range"
          min={0}
          max={last}
          value={cursor}
          onChange={(e) => setCursor(Number(e.target.value))}
          aria-label="Step"
        />

        <span className="step-count">{cursor + 1} / {steps.length}</span>

        <label className="speed-label">
          Speed
          <select
            className="select-sm"
            value={speed}
            onChange={(e) => setSpeed(Number(e.target.value))}
          >
            <option value={1500}>Slow</option>
            <option value={700}>Normal</option>
            <option value={300}>Fast</option>
          </select>
        </label>
      </div>

      <div className="trace-banner">
        <span className="badge">{trace.category}</span>
        <span className="trace-name">{trace.algorithm}</span>
        <span className="trace-time">{formatNanos(trace.executionTimeNanos)}</span>
        <span className="trace-time">{trace.timeComplexity}</span>
        {trace.truncated && <span className="badge badge--truncated">Steps truncated</span>}
      </div>

      {/* Current step */}
      <div className="step-card">
        <div className="step-card-head">
          <span className="step-op">{current.operation}</span>
          <span className="step-idx">Step {current.index}</span>
        </div>
        <p className="step-desc">{current.description}</p>

        {highlights.length > 0 && (
          <div className="step-highlight">
            {highlights.map((h, i) => (
              <span key={i} className="highlight-chip">{h}</span>
            ))}
          </div>
        )}

        {stateEntries.length > 0 && (
          <table className="state-table">
            <tbody>
              {stateEntries.map(([key, value]) => (
                <tr key={key}>
                  <td className="state-key">{key}</td>
                  <td className="state-val"><code>{typeof value === 'string' ? value : JSON.stringify(value)}</code></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Timeline */}
      <div className="timeline">
        {steps.map((step, i) => (
          <button
            key={step.index}
            className={`timeline-step${i === cursor ? ' timeline-step--active' : ''}`}
            onClick={() => setCursor(i)}
            title={`${step.operation}: ${step.description}`}
          >
            <span className="timeline-dot" />
            <span className="timeline-op">{step.operation}</span>
          </button>
        ))}
      </div>
    </div>
  );
}