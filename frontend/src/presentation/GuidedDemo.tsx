import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Check, CircleHelp, X } from 'lucide-react';
import { api } from '../api/client';

type DemoResult = { title: string; detail: string; route: string; operation: (signal: AbortSignal) => Promise<string> };

const steps: DemoResult[] = [
  {
    title: 'Observe the source', route: '/',
    detail: 'Start from a dataset owned by the backend. Its provenance and parser counts are the boundary for every later claim.',
    operation: async (signal) => {
      const current = await api.currentDataset({ signal });
      if (current?.loaded) return `${current.datasetName ?? 'Active dataset'} · ${current.size ?? 0} parsed events`;
      const loaded = await api.loadDemo({ signal });
      return `${loaded.datasetName ?? 'Deterministic demo dataset'} · ${loaded.size ?? 0} parsed events loaded by the backend`;
    }
  },
  {
    title: 'Search evidence', route: '/search',
    detail: 'Run a real structured query against the active dataset. The response reports the matching algorithm and execution time.',
    operation: async (signal) => {
      const result = await api.productSearch({ query: 'level:ERROR', page: 1, size: 5, sort: 'timestamp:desc' }, { signal });
      return `${result.total} matching events · ${result.algorithm ?? 'structured index filters'} · ${result.durationNanos.toLocaleString()} ns reported`;
    }
  },
  {
    title: 'Analyze the window', route: '/analytics',
    detail: 'Inspect backend-computed time buckets. These values summarize the loaded dataset; they are not external live traffic.',
    operation: async (signal) => {
      const result = await api.windows(12, { signal });
      const events = result.reduce((sum, bucket) => sum + (bucket.count ?? 0), 0);
      return `${result.length} returned time buckets · ${events.toLocaleString()} bucketed events`;
    }
  },
  {
    title: 'Explore observed relationships', route: '/services',
    detail: 'Open the service map built from request-trail adjacency. These edges are inferred from logs, not declared infrastructure dependencies.',
    operation: async (signal) => {
      const graph = await api.dependencies({ signal });
      return `${graph.nodeCount} services · ${graph.edgeCount} observed request-trail edges`;
    }
  },
  {
    title: 'Investigate detector evidence', route: '/incidents',
    detail: 'Review heuristic detector windows and their supporting records. Grouping is evidence for an operator, not an automatic root-cause claim.',
    operation: async (signal) => {
      const incidents = await api.incidents(20, { signal });
      return `${incidents.length} detector windows returned from the active dataset`;
    }
  },
  {
    title: 'Connect the algorithms', route: '/algorithms',
    detail: 'Compare product-integrated algorithms with the wider academic catalogue. Catalogue presence alone does not mean a product screen invokes an engine.',
    operation: async (signal) => {
      const catalog = await api.traceCatalog({ signal });
      return `${catalog.length} trace catalogue operations are exposed by this runtime`;
    }
  },
  {
    title: 'Close with measured context', route: '/',
    detail: 'Return to the command center and connect the selected-window summary with the same backend dataset used throughout this walkthrough.',
    operation: async (signal) => {
      const overview = await api.overview('1h', { signal });
      if (!overview) return 'No dataset summary was returned. Load a source to inspect measured signals.';
      return `${overview.events.toLocaleString()} events in ${overview.range} · ${overview.errors.toLocaleString()} errors · source ${overview.dataset}`;
    }
  }
];

type GuidedDemoContextValue = { start: () => void };
const GuidedDemoContext = createContext<GuidedDemoContextValue | null>(null);

export function GuidedDemoProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const [index, setIndex] = useState(0);
  const [evidence, setEvidence] = useState<string[]>([]);
  const [result, setResult] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();
  const step = steps[index];
  const opener = useRef<HTMLElement | null>(null);

  const start = useCallback(() => {
    opener.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    setIndex(0);
    setEvidence([]);
    setResult('');
    setError('');
    setOpen(true);
    navigate(steps[0].route);
  }, [navigate]);

  const close = useCallback(() => {
    setOpen(false);
    window.setTimeout(() => {
      if (opener.current?.isConnected) opener.current.focus();
      else document.querySelector<HTMLElement>('.guided-demo-launch')?.focus();
    }, 0);
  }, []);

  const move = useCallback((next: number) => {
    const bounded = Math.max(0, Math.min(steps.length - 1, next));
    setIndex(bounded);
    setResult('');
    setError('');
    navigate(steps[bounded].route);
  }, [navigate]);

  useEffect(() => {
    if (!open) return;
    const controller = new AbortController();
    let active = true;
    setBusy(true);
    setError('');
    step.operation(controller.signal).then((value) => {
      if (!active) return;
      setResult(value);
      setEvidence((current) => current[index] === value ? current : current.map((item, at) => at === index ? value : item).concat(current[index] === undefined ? [value] : []));
    }).catch((cause: unknown) => {
      if (!active || (cause instanceof Error && cause.name === 'AbortError')) return;
      setError(cause instanceof Error ? cause.message : 'The backend request failed. Retry this step.');
    }).finally(() => { if (active) setBusy(false); });
    return () => { active = false; controller.abort(); };
  }, [open, index, step, attempt]);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { event.preventDefault(); close(); }
      else if (event.key === 'ArrowRight' && index < steps.length - 1) move(index + 1);
      else if (event.key === 'ArrowLeft' && index > 0) move(index - 1);
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open, index, move, close]);

  useEffect(() => {
    if (open && location.pathname !== step.route) navigate(step.route, { replace: true });
  }, [open, location.pathname, navigate, step.route]);

  const context = useMemo(() => ({ start }), [start]);
  return <GuidedDemoContext.Provider value={context}>{children}{open && <aside className="guided-demo" aria-label="Guided product demonstration">
    <div className="guided-demo-top"><span><CircleHelp size={15} aria-hidden="true" /> Guided product tour</span><button className="icon-btn" type="button" onClick={close} aria-label="Exit guided demo"><X size={16} /></button></div>
    <div className="guided-demo-progress" role="progressbar" aria-label="Tour progress" aria-valuemin={1} aria-valuemax={steps.length} aria-valuenow={index + 1}><span style={{ transform: `scaleX(${(index + 1) / steps.length})` }} /></div>
    <p className="guided-demo-count">STEP {index + 1} OF {steps.length} <span>·</span> {step.route}</p>
    <h2 aria-live="polite">{step.title}</h2><p className="guided-demo-copy">{step.detail}</p>
    <div className="guided-demo-evidence" aria-live="polite">
      {busy ? <span className="guided-demo-pending">Requesting evidence from the backend…</span> : error ? <><strong>Step request failed</strong><span>{error}</span><button className="btn btn-sm" type="button" onClick={() => setAttempt((value) => value + 1)}>Retry this step</button></> : result ? <><strong><Check size={14} aria-hidden="true" /> Backend response</strong><span>{result}</span></> : null}
    </div>
    {evidence.length > 0 && <details className="guided-demo-history"><summary>{evidence.length} completed operation{evidence.length === 1 ? '' : 's'}</summary><ol>{evidence.map((item, at) => <li key={`${at}-${item}`}><span>{steps[at].title}</span>{item}</li>)}</ol></details>}
    <div className="guided-demo-actions"><button className="btn btn-sm" type="button" onClick={() => move(index - 1)} disabled={index === 0 || busy}><ArrowLeft size={14} /> Previous</button><button className="btn btn-primary btn-sm" type="button" onClick={() => index === steps.length - 1 ? close() : move(index + 1)} disabled={busy}>{index === steps.length - 1 ? 'Finish tour' : 'Next step'} {index < steps.length - 1 && <ArrowRight size={14} />}</button></div>
    <p className="guided-demo-shortcuts">Use ← → to move · Esc to exit</p>
  </aside>}</GuidedDemoContext.Provider>;
}

export function useGuidedDemo(): GuidedDemoContextValue {
  const value = useContext(GuidedDemoContext);
  if (!value) throw new Error('useGuidedDemo must be used inside GuidedDemoProvider');
  return value;
}
