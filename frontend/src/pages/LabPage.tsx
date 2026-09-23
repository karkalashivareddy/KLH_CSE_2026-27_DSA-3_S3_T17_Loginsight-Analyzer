import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { AlgorithmInfo, TraceResponse } from '../api/types';
import { Card, Spinner, ErrorBox } from '../components/ui';
import TracePlayer from '../components/TracePlayer';
import { moduleAccent, moduleLabel, formatNanos } from '../components/format';

function pretty(object: unknown): string {
  return JSON.stringify(object, null, 2);
}

/** Extract service names from a flow/cover result payload so vertex ids render as names. */
function namesFrom(result: unknown): string[] | undefined {
  if (result && typeof result === 'object' && 'names' in result) {
    const names = (result as { names: unknown }).names;
    if (Array.isArray(names)) return names.map(String);
  }
  return undefined;
}

function Collapsible({ title, children, open = false }: {
  title: string;
  children: React.ReactNode;
  open?: boolean;
}) {
  const [isOpen, setIsOpen] = useState(open);
  return (
    <div className="collapsible">
      <button className="collapsible-head" onClick={() => setIsOpen((o) => !o)}>
        <span>{title}</span>
        <span>{isOpen ? '▾' : '▸'}</span>
      </button>
      {isOpen && <div className="collapsible-body">{children}</div>}
    </div>
  );
}

export default function LabPage() {
  const { key, module } = useParams<{ key?: string; module?: string }>();
  const navigate = useNavigate();

  const { data: catalog, loading: catLoading, error: catError } =
    useApi<AlgorithmInfo[]>(() => api.algorithms());
  const trackable = useMemo(() => (catalog ?? []).filter((a) => a.tracked), [catalog]);

  const [selected, setSelected] = useState<AlgorithmInfo | null>(null);
  const [inputText, setInputText] = useState('');
  const [trace, setTrace] = useState<TraceResponse | null>(null);
  const [plainResult, setPlainResult] = useState<unknown>(null);
  const [running, setRunning] = useState(false);
  const [saving, setSaving] = useState(false);
  const [runError, setRunError] = useState<Error | null>(null);

  const pick = useCallback((entry: AlgorithmInfo) => {
    setSelected(entry);
    setInputText(pretty(entry.defaultInput ?? {}));
    setTrace(null);
    setPlainResult(null);
    setRunError(null);
    navigate(`/labs/${entry.key}`, { replace: true });
  }, [navigate]);

  useEffect(() => {
    if (!catalog || catalog.length === 0) return;
    const target = key ? catalog.find((a) => a.key === key) : undefined;
    const first = target ?? trackable[0] ?? catalog[0];
    if (first) pick(first);
  }, [catalog, key]); // eslint-disable-line react-hooks/exhaustive-deps

  const groups = useMemo(() => {
    const map = new Map<string, AlgorithmInfo[]>();
    for (const entry of catalog ?? []) {
      if (module && entry.moduleId !== module) continue;
      const list = map.get(entry.moduleId) ?? [];
      list.push(entry);
      map.set(entry.moduleId, list);
    }
    return Array.from(map.entries());
  }, [catalog, module]);

  const run = useCallback(async () => {
    if (!selected) return;
    let parsed: unknown;
    try {
      parsed = JSON.parse(inputText);
    } catch (e) {
      setRunError(e instanceof Error ? new Error(`Invalid JSON: ${e.message}`) : new Error('Invalid JSON'));
      return;
    }
    setRunning(true);
    setRunError(null);
    setTrace(null);
    setPlainResult(null);
    try {
      if (selected.tracked && selected.traceEndpoint) {
        const response = await api.traceRun(selected.traceEndpoint, parsed);
        setTrace(response);
      } else if (selected.canonicalEndpoint) {
        setPlainResult(await api.execute(selected.canonicalEndpoint, parsed));
      }
    } catch (e) {
      setRunError(e instanceof Error ? e : new Error(String(e)));
    } finally {
      setRunning(false);
    }
  }, [selected, inputText]);

  const saveAsRun = useCallback(async () => {
    if (!selected) return;
    let parsed: unknown;
    try {
      parsed = JSON.parse(inputText);
    } catch (e) {
      setRunError(e instanceof Error ? new Error(`Invalid JSON: ${e.message}`) : new Error('Invalid JSON'));
      return;
    }
    setSaving(true);
    try {
      await api.run(selected.key, parsed as Record<string, unknown>);
      navigate('/runs');
    } catch (e) {
      setRunError(e instanceof Error ? e : new Error(String(e)));
    } finally {
      setSaving(false);
    }
  }, [selected, inputText, navigate]);

  if (catLoading) return <Spinner label="Loading algorithm catalogue…" />;
  if (catError)    return <ErrorBox error={catError} />;

  const names = trace ? namesFrom(trace.result) : undefined;

  return (
    <div className="page">
      <h2 className="page-title">Algorithm Laboratory</h2>
      <p className="lab-intro">
        Pick an instrumented algorithm, edit its input, then replay the <strong>real steps</strong> the
        algorithm recorded during execution. Save any execution as a replay session.
        {module && (
          <>
            {' '}Filtered to <strong style={{ color: moduleAccent(module) }}>{moduleLabel(module)}</strong>.{' '}
            <Link to="/labs">Show all modules</Link>
          </>
        )}
      </p>

      <div className="lab-layout">
        {/* Catalogue sidebar grouped by module */}
        <Card className="lab-catalog">
          {groups.map(([moduleId, entries]) => (
            <div key={moduleId} className="catalog-group">
              <div
                className="catalog-category"
                style={{ color: moduleAccent(moduleId), fontWeight: 650 }}
              >
                {moduleLabel(moduleId)} ({entries.length})
              </div>
              {entries.map((entry) => (
                <button
                  key={entry.key}
                  className={`catalog-entry${selected?.key === entry.key ? ' catalog-entry--active' : ''}`}
                  onClick={() => pick(entry)}
                  title={entry.description}
                >
                  {entry.name}
                  {!entry.exposed && <span className="badge" style={{ marginLeft: '0.4rem' }}>lib</span>}
                </button>
              ))}
            </div>
          ))}
        </Card>

        {/* Workspace */}
        <div className="lab-workspace">
          {selected && (
            <Card
              className="lab-input-card"
              title={selected.name}
              actions={
                <button className="btn btn-sm" onClick={saveAsRun} disabled={saving}>
                  {saving ? 'Saving…' : 'Save as run ›'}
                </button>
              }
            >
              <div className="lab-meta">
                <span className="badge">{moduleLabel(selected.moduleId)}</span>
                <span className="trace-time">{selected.timeComplexity}</span>
                <span className="trace-time">{selected.spaceComplexity}</span>
                {selected.tracked
                  ? <span className="badge">live trace</span>
                  : <span className="badge">library only</span>}
              </div>
              <p className="lab-desc">{selected.description}</p>

              <div className="lab-controls">
                <textarea
                  className="input-json"
                  spellCheck={false}
                  value={inputText}
                  onChange={(e) => setInputText(e.target.value)}
                  rows={Math.min(10, inputText.split('\n').length + 1)}
                />
                <button className="btn btn-run" onClick={run} disabled={running}>
                  {running ? 'Running…' : 'Run ›'}
                </button>
              </div>

              {runError && <ErrorBox error={runError} />}

              {!selected.tracked && !trace && (
                <p className="lab-desc" style={{ marginTop: '0.8rem' }}>
                  This algorithm is not trace-instrumented. Run it to inspect its output, or open it from
                  the Command Center to build a recorded session.
                </p>
              )}

              {trace && (
                <div className="lab-results" style={{ marginTop: '1rem' }}>
                  <TracePlayer
                    trace={trace}
                    names={names}
                    accentId={selected.moduleId}
                    notes={`${selected.name} — module ${moduleLabel(selected.moduleId)} · ${formatNanos(trace.executionTimeNanos)} wall-time. ${selected.description}`}
                  />

                  <div className="result-grid">
                    <Collapsible title="Result" open>
                      <pre className="json-block">{pretty(trace.result)}</pre>
                    </Collapsible>
                    {trace.intermediateData != null && (
                      <Collapsible title="Intermediate Data">
                        <pre className="json-block">
                          {pretty(trace.intermediateData).slice(0, 4000)}
                          {pretty(trace.intermediateData).length > 4000 ? '\n… (truncated for display)' : ''}
                        </pre>
                      </Collapsible>
                    )}
                  </div>
                </div>
              )}

              {plainResult !== null && (
                <div className="lab-results" style={{ marginTop: '1rem' }}>
                  <Collapsible title="Result" open>
                    <pre className="json-block">{pretty(plainResult)}</pre>
                  </Collapsible>
                </div>
              )}
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}