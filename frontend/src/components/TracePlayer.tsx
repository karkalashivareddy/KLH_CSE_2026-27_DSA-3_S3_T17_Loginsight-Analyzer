import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from 'react';
import { BookOpen, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, ListTree, Pause, Play } from 'lucide-react';
import type { TraceResponse, TraceStep } from '../api/types';
import { formatNanos, moduleAccent } from './format';

export interface TracePlayerProps {
  trace: TraceResponse;
  names?: string[];
  accentId?: string;
  notes?: string;
  resetKey?: string;
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

export default function TracePlayer({ trace, names, accentId, notes, resetKey }: TracePlayerProps) {
  const steps = trace.steps ?? [];
  const [cursor, setCursor] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [speed, setSpeed] = useState(700);
  const [ledgerOpen, setLedgerOpen] = useState(false);
  const timerRef = useRef<number | null>(null);
  const ledgerRef = useRef<HTMLDivElement | null>(null);
  const signatureRef = useRef<string | null>(null);
  const last = Math.max(0, steps.length - 1);
  const boundedCursor = Math.max(0, Math.min(cursor, last));
  const current = steps[boundedCursor] ?? steps[0];
  const accent = accentId ? moduleAccent(accentId) : 'var(--accent)';
  const signature = resetKey ?? `${trace.algorithm}:${trace.category}`;
  const stepHighlightsMemo = useMemo(() => steps.map((step) => stepHighlights(step, names)), [steps, names]);

  useEffect(() => {
    if (signatureRef.current !== signature) {
      signatureRef.current = signature;
      setCursor(0);
      setPlaying(false);
    }
  }, [signature]);

  useEffect(() => {
    setCursor((value) => Math.min(value, last));
  }, [last]);

  useEffect(() => {
    if (!playing || steps.length < 2) return;
    timerRef.current = window.setInterval(() => {
      setCursor((value) => {
        if (value >= last) {
          setPlaying(false);
          return value;
        }
        return value + 1;
      });
    }, speed);
    return () => {
      if (timerRef.current !== null) window.clearInterval(timerRef.current);
      timerRef.current = null;
    };
  }, [last, playing, speed, steps.length]);

  const jump = useCallback((to: number) => setCursor(Math.max(0, Math.min(last, to))), [last]);
  const togglePlayback = useCallback(() => {
    if (steps.length < 2) return;
    if (cursor >= last) {
      setCursor(0);
      setPlaying(true);
      return;
    }
    setPlaying((value) => !value);
  }, [cursor, last, steps.length]);

  const onKeyDown = useCallback((event: KeyboardEvent) => {
    const target = event.target as HTMLElement | null;
    const tag = target?.tagName;
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || tag === 'BUTTON' || target?.isContentEditable === true) return;
    if (event.code === 'Space') {
      event.preventDefault();
      togglePlayback();
    } else if (event.code === 'ArrowRight') {
      event.preventDefault();
      jump(cursor + 1);
    } else if (event.code === 'ArrowLeft') {
      event.preventDefault();
      jump(cursor - 1);
    } else if (event.code === 'Home') {
      event.preventDefault();
      jump(0);
    } else if (event.code === 'End') {
      event.preventDefault();
      jump(last);
    }
  }, [cursor, jump, last, togglePlayback]);

  useEffect(() => {
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onKeyDown]);

  useEffect(() => {
    if (ledgerOpen) ledgerRef.current?.querySelector('.event-row--active')?.scrollIntoView({ block: 'nearest' });
  }, [cursor, ledgerOpen]);

  if (steps.length === 0 || !current) return <div className="empty-state">The trace recorded no observable steps.</div>;

  const highlights = stepHighlightsMemo[boundedCursor] ?? [];
  const stateEntries = Object.entries(current.state ?? {});

  return (
    <div className="trace-player" style={{ '--accent': accent } as CSSProperties}>
      <div className="trace-toolbar">
        <button className="icon-btn trace-control" type="button" onClick={() => jump(0)} disabled={cursor === 0} aria-label="First step" title="First step (Home)"><ChevronsLeft size={16} aria-hidden="true" /></button>
        <button className="icon-btn trace-control" type="button" onClick={() => jump(cursor - 1)} disabled={cursor === 0} aria-label="Previous step" title="Previous step (Left arrow)"><ChevronLeft size={16} aria-hidden="true" /></button>
        <button className="icon-btn trace-control trace-control--primary" type="button" onClick={togglePlayback} disabled={steps.length < 2} aria-label={playing ? 'Pause replay' : 'Play replay'} title="Play or pause (Space)">{playing ? <Pause size={16} aria-hidden="true" /> : <Play size={16} aria-hidden="true" />}</button>
        <button className="icon-btn trace-control" type="button" onClick={() => jump(cursor + 1)} disabled={cursor >= last} aria-label="Next step" title="Next step (Right arrow)"><ChevronRight size={16} aria-hidden="true" /></button>
        <button className="icon-btn trace-control" type="button" onClick={() => jump(last)} disabled={cursor >= last} aria-label="Last step" title="Last step (End)"><ChevronsRight size={16} aria-hidden="true" /></button>
        <input className="range" type="range" min={0} max={last} value={boundedCursor} onChange={(event) => jump(Number(event.target.value))} aria-label="Trace step" />
        <span className="step-count">{boundedCursor + 1} / {steps.length}</span>
        <label className="speed-label">Speed<select className="select-sm" value={speed} onChange={(event) => setSpeed(Number(event.target.value))} aria-label="Replay speed">{SPEEDS.map((option) => <option key={option.ms} value={option.ms}>{option.label}</option>)}</select></label>
        <button className={`btn btn-sm${ledgerOpen ? ' btn-run' : ''}`} type="button" onClick={() => setLedgerOpen((value) => !value)}><ListTree size={14} aria-hidden="true" /> Ledger</button>
      </div>

      <div className="trace-banner">
        <span className="badge">{trace.category}</span>
        <span className="trace-name">{trace.algorithm}</span>
        <span className="trace-time">{formatNanos(trace.executionTimeNanos)}</span>
        <span className="trace-time">{trace.timeComplexity}</span>
        {trace.truncated && <span className="badge badge--warn">Steps truncated</span>}
      </div>

      <div className="trace-banner kbd-hint"><span>Space</span><kbd>play/pause</kbd><span>Arrow keys</span><kbd>step</kbd><span>Home/End</span><kbd>jump</kbd></div>

      <div className="step-card">
        <div className="step-card-head"><span className="step-op">{current.operation}</span><span className="step-idx">Step {current.index}</span></div>
        <p className="step-desc">{current.description}</p>
        {highlights.length > 0 && <div className="step-highlight">{highlights.map((highlight, index) => <span className="highlight-chip" key={`${highlight}-${index}`}>{highlight}</span>)}</div>}
        {stateEntries.length > 0 && <table className="state-table"><caption className="sr-only">Trace operation state</caption><tbody>{stateEntries.map(([key, value]) => <tr key={key}><td className="state-key">{key}</td><td className="state-val"><code>{typeof value === 'string' ? value : JSON.stringify(value)}</code></td></tr>)}</tbody></table>}
      </div>

      {ledgerOpen && <div className="step-card step-card--ledger"><div className="step-card-head"><span className="step-op"><ListTree size={14} aria-hidden="true" /> Operation ledger</span><span className="step-idx">{steps.length} recorded steps</span></div><div className="event-ledger" ref={ledgerRef}>{steps.map((step, index) => <button key={`${step.index}-${index}`} type="button" className={`event-row${index === boundedCursor ? ' event-row--active' : ''}`} onClick={() => jump(index)}><span className="event-idx">{step.index}</span><span><span className="event-op">{step.operation}</span>{' '}<span className="event-note">{step.description}</span></span></button>)}</div></div>}

      {notes && <div className="step-card step-card--notes"><div className="step-card-head"><span className="step-op"><BookOpen size={14} aria-hidden="true" /> Education</span></div><p className="step-desc">{notes}</p></div>}

      <div className="timeline">{steps.map((step, index) => <button key={`${step.index}-${index}`} type="button" className={`timeline-step${index === boundedCursor ? ' timeline-step--active' : ''}`} onClick={() => jump(index)} title={`${step.operation}: ${step.description}`}><span className="timeline-dot" /><span className="timeline-op">{step.operation}</span></button>)}</div>
    </div>
  );
}
