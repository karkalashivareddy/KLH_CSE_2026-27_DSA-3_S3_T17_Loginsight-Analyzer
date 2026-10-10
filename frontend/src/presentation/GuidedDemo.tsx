import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Activity, ArrowLeft, ArrowRight, Check, CircleHelp, Database, ExternalLink, GitBranch, LoaderCircle, ShieldAlert, Sparkles, X } from 'lucide-react';
import { api } from '../api/client';
import type { TraceCatalogEntry } from '../api/types';

type DemoStep = {
  title: string;
  concept: string;
  route: string;
  operationLabel: string;
  operation: (signal: AbortSignal) => Promise<string>;
};

const steps: DemoStep[] = [
  {
    title: 'The investigation problem', route: '/', operationLabel: 'Product context',
    concept: 'Logs are high-volume and loosely structured. LogInsight turns them into searchable events, scoped summaries, observed service relationships, and evidence an operator can inspect.',
    operation: async () => 'Product explanation · the tour uses backend responses for every measured result that follows.'
  },
  {
    title: 'Choose the source', route: '/datasets', operationLabel: 'Dataset API',
    concept: 'Every result starts with the active backend dataset. If none is loaded, this step installs the reproducible demo corpus through the dataset endpoint.',
    operation: async (signal) => {
      const active = await api.currentDataset({ signal });
      if (active?.loaded) return `${active.datasetName ?? 'Active dataset'} · ${active.size ?? 0} parsed events already loaded`;
      const loaded = await api.loadDemo({ signal });
      return `${loaded.datasetName ?? 'Demo dataset'} · ${loaded.size} parsed events · ${loaded.failedLines ?? 0} rejected lines · loaded by the backend`;
    }
  },
  {
    title: 'Inspect parsing evidence', route: '/ingestion', operationLabel: 'Dataset summary API',
    concept: 'The ingestion result reports accepted records and rejected lines. A successful HTTP response does not conceal parser failures; partial parsing is visible in the dataset summary.',
    operation: async (signal) => {
      const dataset = await api.currentDataset({ signal });
      if (!dataset?.loaded) throw new Error('No dataset is active. Retry the source step before inspecting parser counts.');
      const lines = dataset.totalLines === undefined ? 'line total unavailable' : `${dataset.totalLines} input lines`;
      const rejected = dataset.failedLines === undefined ? 'rejected-line count unavailable' : `${dataset.failedLines} rejected lines`;
      return `${dataset.datasetName ?? 'Active dataset'} · ${dataset.size ?? 0} accepted events · ${lines} · ${rejected}`;
    }
  },
  {
    title: 'Search matching records', route: '/search', operationLabel: 'Structured search API',
    concept: 'The query runs against the active dataset. Structured filters use indexed fields and free text uses the backend search strategy; the response supplies the match count and timing.',
    operation: async (signal) => {
      const result = await api.productSearch({ query: 'level:ERROR', page: 1, size: 5, sort: 'timestamp:desc' }, { signal });
      return `${result.total.toLocaleString()} matching ERROR events · ${result.algorithm ?? 'structured index filters'} · ${result.durationNanos.toLocaleString()} ns reported · ${result.matches.length} records in this page`;
    }
  },
  {
    title: 'Read a selected window', route: '/analytics', operationLabel: 'Overview analytics API',
    concept: 'Time scope matters. This response reports a trailing one-hour window anchored to the dataset’s newest timestamp, separate from the full dataset size.',
    operation: async (signal) => {
      const result = await api.overview('1h', { signal });
      if (!result) throw new Error('The backend returned no overview. Confirm a dataset is loaded and retry.');
      return `${result.events.toLocaleString()} events in ${result.range} · ${result.errors.toLocaleString()} errors · ${result.datasetEvents.toLocaleString()} total dataset events · scope ${result.scope ?? 'not supplied'}`;
    }
  },
  {
    title: 'Trace the search algorithm', route: '/algorithms', operationLabel: 'KMP trace endpoint',
    concept: 'The catalogue entry is not presented as execution evidence. The tour selects the registered KMP trace descriptor and submits its actual default input to the backend trace endpoint.',
    operation: async (signal) => {
      const catalog = await api.traceCatalog({ signal });
      const kmp = catalog.find((entry: TraceCatalogEntry) => entry.key.toLowerCase() === 'kmp');
      if (!kmp) throw new Error('The runtime does not expose the KMP trace descriptor. Inspect the algorithm catalogue for available engines.');
      const trace = await api.traceRun(kmp.endpoint, kmp.defaultInput, { signal });
      const output = typeof trace.result === 'string' ? trace.result : JSON.stringify(trace.result);
      return `${trace.algorithm} ran · ${trace.steps.length} trace states · ${trace.timeComplexity} time · ${trace.executionTimeNanos.toLocaleString()} ns for this single trace · result ${output.slice(0, 100)}`;
    }
  },
  {
    title: 'Explore observed relationships', route: '/services', operationLabel: 'Dependency analysis API',
    concept: 'The graph is derived from request-trail adjacency in log records. Its edges are observed associations, not declarations of infrastructure and not proof of causality.',
    operation: async (signal) => {
      const graph = await api.dependencies({ signal });
      return `${graph.nodeCount} returned service nodes · ${graph.edgeCount} observed request-trail edges`;
    }
  },
  {
    title: 'Inspect detector evidence', route: '/incidents', operationLabel: 'Incident detector API',
    concept: 'Detector windows group error evidence heuristically. They help an operator investigate a time range; the result does not establish a definitive root cause or persisted incident lifecycle.',
    operation: async (signal) => {
      const incidents = await api.incidents(20, { signal });
      const evidenceCount = incidents.reduce((sum, incident) => sum + (incident.eventCount ?? 0), 0);
      return `${incidents.length} detector windows returned · ${evidenceCount.toLocaleString()} ERROR/FATAL events represented by the returned windows`;
    }
  },
  {
    title: 'What the evidence supports', route: '/', operationLabel: 'Final backend snapshot',
    concept: 'The application demonstrated dataset parsing, a real query, a scoped aggregate, a KMP trace, observed graph relationships, and heuristic detector output. It does not claim an external production stream or automatic root cause.',
    operation: async (signal) => {
      const [dataset, overview] = await Promise.all([api.currentDataset({ signal }), api.overview('1h', { signal })]);
      if (!dataset?.loaded || !overview) throw new Error('The final summary could not be read. Retry this step or exit and return to the Command Center.');
      return `${dataset.datasetName ?? 'Active dataset'} · ${dataset.size ?? 0} parsed events · ${overview.events.toLocaleString()} events in selected ${overview.range} · simulation and dataset replay remain separate modes`;
    }
  }
];

type GuidedDemoContextValue = { start: () => void };
const GuidedDemoContext = createContext<GuidedDemoContextValue | null>(null);

export function GuidedDemoProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const [started, setStarted] = useState(false);
  const [index, setIndex] = useState(0);
  const [evidence, setEvidence] = useState<Record<number, string>>({});
  const [result, setResult] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();
  const panelRef = useRef<HTMLElement>(null);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const opener = useRef<HTMLElement | null>(null);
  const step = steps[index];

  const start = useCallback(() => {
    opener.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    setIndex(0);
    setEvidence({});
    setResult('');
    setError('');
    setStarted(false);
    setOpen(true);
  }, []);

  const begin = useCallback(() => {
    setStarted(true);
    setIndex(0);
    setEvidence({});
    setResult('');
    setError('');
    navigate(steps[0].route);
  }, [navigate]);

  const close = useCallback(() => {
    setOpen(false);
    setStarted(false);
  }, []);

  const restart = useCallback(() => {
    setStarted(false);
    setIndex(0);
    setEvidence({});
    setResult('');
    setError('');
  }, []);

  const move = useCallback((next: number) => {
    const bounded = Math.max(0, Math.min(steps.length - 1, next));
    setIndex(bounded);
    setResult('');
    setError('');
    navigate(steps[bounded].route);
  }, [navigate]);

  useEffect(() => {
    if (!open) {
      document.body.classList.remove('guided-demo-open');
      delete document.body.dataset.guidedStep;
      return;
    }
    document.body.classList.add('guided-demo-open');
    const focusTimer = window.setTimeout(() => panelRef.current?.querySelector<HTMLElement>('button:not(:disabled)')?.focus(), 0);
    return () => {
      window.clearTimeout(focusTimer);
      document.body.classList.remove('guided-demo-open');
      delete document.body.dataset.guidedStep;
    };
  }, [open]);

  useEffect(() => {
    if (!open || started) return;
    const timer = window.setTimeout(() => panelRef.current?.querySelector<HTMLElement>('.guided-demo-begin')?.focus(), 0);
    return () => window.clearTimeout(timer);
  }, [open, started]);

  useEffect(() => {
    if (open && started) headingRef.current?.focus();
  }, [open, started, index]);

  useEffect(() => {
    if (!open) {
      if (opener.current?.isConnected) opener.current.focus();
      opener.current = null;
      return;
    }
  }, [open]);

  useEffect(() => {
    if (!open || !started) return;
    document.body.dataset.guidedStep = String(index);
    const controller = new AbortController();
    let active = true;
    setBusy(true);
    setError('');
    step.operation(controller.signal).then((value) => {
      if (!active) return;
      setResult(value);
      setEvidence((current) => ({ ...current, [index]: value }));
    }).catch((cause: unknown) => {
      if (!active || (cause instanceof Error && cause.name === 'AbortError')) return;
      setError(cause instanceof Error ? cause.message : 'The backend request failed. Retry this step.');
    }).finally(() => { if (active) setBusy(false); });
    return () => { active = false; controller.abort(); };
  }, [open, started, index, step, attempt]);

  useEffect(() => {
    if (open && started && location.pathname !== step.route) navigate(step.route, { replace: true });
  }, [open, started, location.pathname, navigate, step.route]);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        close();
        return;
      }
      if (started && event.key === 'ArrowRight' && !busy && index < steps.length - 1) { event.preventDefault(); move(index + 1); }
      else if (started && event.key === 'ArrowLeft' && !busy && index > 0) { event.preventDefault(); move(index - 1); }
      if (event.key === 'Tab' && panelRef.current) {
        const focusable = [...panelRef.current.querySelectorAll<HTMLElement>('button:not(:disabled), a[href], [tabindex]:not([tabindex="-1"])')];
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
      }
    };
    document.addEventListener('keydown', onKeyDown, true);
    return () => document.removeEventListener('keydown', onKeyDown, true);
  }, [open, started, busy, index, move, close]);

  const context = useMemo(() => ({ start }), [start]);
  const completed = Object.keys(evidence).length;
  return <GuidedDemoContext.Provider value={context}>{children}{open && <div className="guided-demo-backdrop" onMouseDown={(event) => event.target === event.currentTarget && close()}>
    <section ref={panelRef} className={`guided-demo${started ? ' guided-demo--running' : ' guided-demo--intro'}`} role="dialog" aria-modal="true" aria-labelledby="guided-demo-title" aria-describedby="guided-demo-description">
      <header className="guided-demo-top"><span><CircleHelp size={15} aria-hidden="true" /> Instructor presentation <span className="guided-demo-source-tag"><Database size={12} aria-hidden="true" /> Backend evidence</span></span><button className="icon-btn" type="button" onClick={close} aria-label="Exit guided presentation"><X size={17} /></button></header>
      {!started ? <div className="guided-demo-intro">
        <div className="guided-demo-intro-art" aria-hidden="true"><div className="tour-orbit tour-orbit--outer" /><div className="tour-orbit tour-orbit--inner" /><span><Activity size={28} /></span><i /><b /></div>
        <p className="guided-demo-kicker">SIGNAL ATLAS · PRODUCT WALKTHROUGH</p>
        <h2 id="guided-demo-title">From raw events<br />to useful evidence.</h2>
        <p id="guided-demo-description">A guided tour of the real dataset, search, analytics, algorithm trace, observed topology, and detector evidence. Each result below comes from a backend operation.</p>
        <div className="guided-demo-intro-meta"><span><Sparkles size={14} aria-hidden="true" /> 9 concise steps</span><span><Database size={14} aria-hidden="true" /> Deterministic local data</span><span><ShieldAlert size={14} aria-hidden="true" /> No external stream claim</span></div>
        <div className="guided-demo-intro-actions"><button className="btn btn-primary guided-demo-begin" type="button" onClick={begin}>Begin walkthrough <ArrowRight size={15} aria-hidden="true" /></button><button className="btn btn-quiet" type="button" onClick={close}>Return to workspace</button></div>
      </div> : <>
        <div className="guided-demo-progress-wrap"><div className="guided-demo-progress-copy"><span>STEP {index + 1} / {steps.length}</span><span>{completed} evidenced</span></div><div className="guided-demo-progress" role="progressbar" aria-label="Presentation progress" aria-valuemin={1} aria-valuemax={steps.length} aria-valuenow={index + 1}><span style={{ transform: `scaleX(${(index + 1) / steps.length})` }} /></div></div>
        <div className="guided-demo-content">
          <div className="guided-demo-narrative">
            <p className="guided-demo-count"><span>{step.operationLabel}</span><span>·</span><span>{step.route}</span></p>
            <h2 ref={headingRef} id="guided-demo-title" tabIndex={-1} aria-live="polite">{step.title}</h2>
            <p id="guided-demo-description" className="guided-demo-copy">{step.concept}</p>
            <div className="guided-demo-callout"><GitBranch size={15} aria-hidden="true" /><span>Follow this step in the product workspace. You can exit at any time and continue investigating normally.</span></div>
            <div className="guided-demo-operation" aria-live="polite" aria-atomic="true">
              <div className="guided-demo-operation-heading">{busy ? <><LoaderCircle className="guided-demo-spinner" size={15} aria-hidden="true" /> Request in progress</> : error ? <><ShieldAlert size={15} aria-hidden="true" /> Operation failed</> : result ? <><Check size={15} aria-hidden="true" /> Backend response</> : <><Database size={15} aria-hidden="true" /> Waiting to run</>}</div>
              {busy ? <p>Requesting evidence from the active backend…</p> : error ? <><p className="guided-demo-error">{error}</p><button className="btn btn-sm" type="button" onClick={() => setAttempt((value) => value + 1)}>Retry this step</button></> : result ? <><p className="guided-demo-result">{result}</p><span className="guided-demo-provenance">Response returned by {step.operationLabel}</span></> : <p>Advance to run this operation.</p>}
            </div>
            <div className="guided-demo-links"><Link to={step.route} onClick={close}>Open this full product view <ExternalLink size={13} aria-hidden="true" /></Link></div>
          </div>
          <aside className="guided-demo-rail" aria-label="Demonstration outline">
            <div className="guided-demo-rail-head"><span>INVESTIGATION PATH</span><span>{completed}/{steps.length}</span></div>
            <ol>{steps.map((item, at) => <li key={item.title} className={`${at === index ? 'is-current' : ''}${evidence[at] ? ' is-complete' : ''}`}><span className="guided-demo-step-marker">{evidence[at] ? <Check size={12} aria-hidden="true" /> : String(at + 1).padStart(2, '0')}</span><span><strong>{item.title}</strong><small>{evidence[at] ? 'Evidence captured' : at === index ? 'Current step' : 'Upcoming'}</small></span></li>)}</ol>
            <div className="guided-demo-rail-note"><strong>Evidence boundary</strong><span>Heuristics show correlated records. Observed graph edges do not prove infrastructure or causality.</span></div>
          </aside>
        </div>
        <footer className="guided-demo-actions"><button className="btn btn-sm" type="button" onClick={() => move(index - 1)} disabled={index === 0 || busy}><ArrowLeft size={14} aria-hidden="true" /> Previous</button><span className="guided-demo-shortcuts">← → navigate <kbd>Esc</kbd> exit</span><div><button className="btn btn-sm btn-quiet" type="button" onClick={restart}>Restart</button><button className="btn btn-primary btn-sm" type="button" onClick={() => index === steps.length - 1 ? close() : move(index + 1)} disabled={busy}>{index === steps.length - 1 ? 'Finish tour' : 'Next step'} {index < steps.length - 1 && <ArrowRight size={14} aria-hidden="true" />}</button></div></footer>
      </>}
    </section>
  </div>}</GuidedDemoContext.Provider>;
}

export function useGuidedDemo(): GuidedDemoContextValue {
  const value = useContext(GuidedDemoContext);
  if (!value) throw new Error('useGuidedDemo must be used inside GuidedDemoProvider');
  return value;
}
