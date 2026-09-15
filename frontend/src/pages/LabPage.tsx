import { useCallback, useEffect, useMemo, useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { TraceCatalogEntry, TraceResponse } from '../api/types';
import { Card, Spinner, ErrorBox } from '../components/ui';
import TracePlayer from '../components/TracePlayer';

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
  const { data: catalog, loading: catLoading, error: catError } = useApi<TraceCatalogEntry[]>(() => api.traceCatalog());

  const [selected, setSelected] = useState<TraceCatalogEntry | null>(null);
  const [inputText, setInputText] = useState('');
  const [trace, setTrace] = useState<TraceResponse | null>(null);
  const [running, setRunning] = useState(false);
  const [runError, setRunError] = useState<Error | null>(null);

  useEffect(() => {
    if (catalog && catalog.length > 0 && !selected) {
      const first = catalog[0];
      setSelected(first);
      setInputText(pretty(first.defaultInput));
      setTrace(null);
      setRunError(null);
    }
  }, [catalog, selected]);

  const groups = useMemo(() => {
    const map = new Map<string, TraceCatalogEntry[]>();
    for (const entry of catalog ?? []) {
      const list = map.get(entry.category) ?? [];
      list.push(entry);
      map.set(entry.category, list);
    }
    return Array.from(map.entries());
  }, [catalog]);

  const pick = useCallback((entry: TraceCatalogEntry) => {
    setSelected(entry);
    setInputText(pretty(entry.defaultInput));
    setTrace(null);
    setRunError(null);
  }, []);

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
    try {
      const response = await api.traceRun(selected.endpoint, parsed);
      setTrace(response);
    } catch (e) {
      setRunError(e instanceof Error ? e : new Error(String(e)));
    } finally {
      setRunning(false);
    }
  }, [selected, inputText]);

  if (catLoading) return <Spinner label="Loading algorithm catalogue…" />;
  if (catError)    return <ErrorBox error={catError} />;

  const names = trace ? namesFrom(trace.result) : undefined;

  return (
    <div className="page">
      <h2 className="page-title">Algorithm Laboratory</h2>
      <p className="lab-intro">
        Pick an instrumented algorithm, edit its input, then replay the real steps the algorithm
        recorded during execution.
      </p>

      <div className="lab-layout">
        {/* Catalogue sidebar */}
        <Card className="lab-catalog">
          {groups.map(([category, entries]) => (
            <div key={category} className="catalog-group">
              <div className="catalog-category">{category}</div>
              {entries.map((entry) => (
                <button
                  key={entry.key}
                  className={`catalog-entry${selected?.key === entry.key ? ' catalog-entry--active' : ''}`}
                  onClick={() => pick(entry)}
                  title={entry.description}
                >
                  {entry.name}
                </button>
              ))}
            </div>
          ))}
        </Card>

        {/* Workspace */}
        <div className="lab-workspace">
          {selected && (
            <Card title={selected.name} className="lab-input-card">
              <div className="lab-meta">
                <span className="badge">{selected.category}</span>
                <span className="trace-time">{selected.timeComplexity}</span>
                <span className="trace-time">{selected.spaceComplexity}</span>
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

              {trace && (
                <div className="lab-results">
                  <TracePlayer trace={trace} names={names} />

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
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}