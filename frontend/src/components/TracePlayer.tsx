import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { TraceResponse, TraceStep } from '../api/types';
import { formatNanos, moduleAccent } from './format';

/**
 * TraceStudio — step-by-step replay of a real algorithm trace. Every step shown here was recorded
 * by the backend while the genuine algorithm executed; the player never invents animation state.
 *
 * Keyboard: Space = play/pause · ←/→ = step back/forward · Home/End = first/last step
 */

export interface TracePlayerProps {
  trace: TraceResponse;
  names?: string[];
  accentId?: string;
  notes?: string;
}

function stepHighlights(step: TraceStep, names?: string[]): string[] {
  if (!step.highlighted) return [];
  const tracked = step.state?.tracked as (string | number)[] | undefined;
  return step.highlighted.map((id) => {
    if (tracked && id >= 0 && id < tracked.length && tracked[id] !== undefined) return String(tracked[id]);
    if (names && id >= 0 && id < names.length) return names[id];
    return String(id);
  });
}

const SPEEDS = [
  { label: 'Slow', ms: 1500 },
  { label: 'Normal', ms: 700 },
  { label: 'Fast', ms: 300 }
];

export default function TracePlayer({ trace, names, accentId, notes }: TracePlayerProps) {
  const steps = trace.steps;
  const [cursor, setCursor] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [speed, setSpeed] = useState(700);
  const [ledgerOpen, setLedgerOpen] = useState(false);
  const timerRef = useRef<number | null>(null);
  const ledgerRef = useRef<HTMLDivElement | null>(null);

  const last = Math.max(0, steps.length - 1);
  const current = steps[Math.min(cursor, last)];
  const accent = accentId ? moduleAccent(accentId) : 'var(--accent)';

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

  const jump = useCallback((to: number) => setCursor(Math.max(0, Math.min(last, to))), [last]);

  const onKeyDown = useCallback(
    (e: KeyboardEvent) => {
      const t = e.target as HTMLElement | null;
      const tag = t?.tagName;
      if (
        tag === 'INPUT' ||
        tag === 'TEXTAREA' ||
        tag === 'SELECT' ||
        t?.isContentEditable === true
      ) {
        return; // never steal keys while the user is typing (Lab JSON editor, TextHack, Run input)
      }
      if (e.code === 'Space') {
        e.preventDefault();
        setPlaying((p) => (cursor >= last ? false : !p));
      } else if (e.code === 'ArrowRight') {
        e.preventDefault();
        jump(cursor + 1);
      } else if (e.code === 'ArrowLeft') {
        e.preventDefault();
        jump(cursor - 1);
      } else if (e.code === 'Home') {
        e.preventDefault();
        jump(0);
      } else if (e.code === 'End') {
        e.preventDefault();
        jump(last);
      }
    },
    [cursor, jump, last]
  );

  useEffect(() => {
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onKeyDown]);

  useEffect(() => {
    if (ledgerOpen && ledgerRef.current) {
      const active = ledgerRef.current.querySelector('.event-row--active');
      active?.scrollIntoView({ block: 'nearest' });
    }
  }, [cursor, ledgerOpen]);

  if (steps.length === 0) {
    return <div className="empty-state">The trace recorded no observable steps.</div>;
  }

  const highlights = stepHighlightsMemo[cursor] ?? [];
  const stateEntries = Object.entries(current.state ?? {});

  return (
    <div className="trace-player" style={{ ['--accent' as never]: accent }}>
      <div className="trace-toolbar">
        <button className="btn btn-sm" onClick={() => jump(0)} disabled={cursor === 0} title="First step (Home)">⏮</button>
        <button className="btn btn-sm" onClick={() => jump(cursor - 1)} disabled={cursor === 0} title="Previous step (←)">◀</button>
        <button
          className="btn btn-sm"
          onClick={() => setPlaying((p) => !p)}
          disabled={cursor >= last}
          title="Play/Pause (Space)"
        >
          {playing ? '❚❚' : '▶'}
        </button>
        <button className="btn btn-sm" onClick={() => jump(cursor + 1)} disabled={cursor >= last} title="Next step (→)">▶</button>
        <button className="btn btn-sm" onClick={() => jump(last)} disabled={cursor >= last} title="Last step (End)">⏭</button>

        <input
          className="range"
          type="range"
          min={0}
          max={last}
          value={cursor}
          onChange={(e) => jump(Number(e.target.value))}
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
            {SPEEDS.map((s) => (
              <option key={s.ms} value={s.ms}>{s.label}</option>
            ))}
          </select>
        </label>

        <button
          className={`btn btn-sm${ledgerOpen ? ' btn-run' : ''}`}
          onClick={() => setLedgerOpen((o) => !o)}
          title="Toggle the operation ledger"
        >
          Ledger
        </button>
      </div>

      <div className="trace-banner">
        <span className="badge">{trace.category}</span>
        <span className="trace-name">{trace.algorithm}</span>
        <span className="trace-time">{formatNanos(trace.executionTimeNanos)}</span>
        <span className="trace-time">{trace.timeComplexity}</span>
        {trace.truncated && <span className="badge badge--truncated">Steps truncated</span>}
      </div>

      <div className="trace-banner kbd-hint">
        <span>Space</span><kbd>play/pause</kbd>
        <span>←/→</span><kbd>step</kbd>
        <span>Home/End</span><kbd>jump</kbd>
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

      {/* Event ledger (variable inspector with jump-to-step) */}
      {ledgerOpen && (
        <div className="step-card" style={{ marginTop: '0.8rem' }}>
          <div className="step-card-head">
            <span className="step-op">Operation ledger</span>
            <span className="step-idx">{steps.length} recorded steps</span>
          </div>
          <div className="event-ledger" ref={ledgerRef}>
            {steps.map((step, i) => (
              <button
                key={step.index}
                className={`event-row${i === cursor ? ' event-row--active' : ''}`}
                onClick={() => jump(i)}
                style={{ border: 'none', background: 'transparent', textAlign: 'left', cursor: 'pointer' }}
              >
                <span className="event-idx">{step.index}</span>
                <span>
                  <span className="event-op">{step.operation}</span>{' '}
                  <span className="event-note">{step.description}</span>
                </span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Education panel */}
      {notes && (
        <div className="step-card" style={{ marginTop: '0.8rem' }}>
          <div className="step-card-head">
            <span className="step-op">Education</span>
          </div>
          <p className="step-desc" style={{ marginBottom: 0 }}>{notes}</p>
        </div>
      )}

      {/* Timeline */}
      <div className="timeline">
        {steps.map((step, i) => (
          <button
            key={step.index}
            className={`timeline-step${i === cursor ? ' timeline-step--active' : ''}`}
            onClick={() => jump(i)}
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