import { useCallback, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { TextHackResponse } from '../api/types';
import { Card, Spinner, ErrorBox } from '../components/ui';
import { formatNanos, moduleAccent, moduleGlow, moduleLabel } from '../components/format';

interface QueryClassDef {
  id: string;
  label: string;
  moduleId: string;
  description: string;
  template: Record<string, unknown>;
}

const QUERY_CLASSES: QueryClassDef[] = [
  {
    id: 'PATTERN_SEARCH',
    label: 'Pattern Search',
    moduleId: 'strings',
    description: 'Find a substring inside a text using the genuine KMP engine (or open it in the lab for Z, Rabin-Karp, Naive).',
    template: { pattern: 'quick', text: 'The quick brown fox jumps over the lazy dog. The quick fox is swift.', scope: 'EXPLICIT' }
  },
  {
    id: 'FUZZY_MATCH',
    label: 'Fuzzy Match',
    moduleId: 'strings',
    description: 'Approximate string match against the live log dataset by Levenshtein edit distance.',
    template: { query: 'error', maxDistance: 2 }
  },
  {
    id: 'DOCUMENT_SIMILARITY',
    label: 'Document Similarity',
    moduleId: 'dp',
    description: 'Global alignment (Needleman-Wunsch) between two documents; reports the real identity score.',
    template: {
      a: 'sev06 login-failed retry dial config upload',
      b: 'sev06 login-failed retry dial backup upload'
    }
  },
  {
    id: 'CITATION_FLOW',
    label: 'Dependency Flow',
    moduleId: 'flow',
    description: 'Max flow across a service dependency graph with Dinic, using recorded residual augmentations.',
    template: {
      source: 'gateway',
      sink: 'db',
      nodes: ['gateway', 'auth', 'search', 'cache', 'db'],
      edges: [
        { from: 'gateway', to: 'auth', capacity: 3 },
        { from: 'gateway', to: 'search', capacity: 2 },
        { from: 'auth', to: 'db', capacity: 2 },
        { from: 'search', to: 'cache', capacity: 2 },
        { from: 'cache', to: 'db', capacity: 3 }
      ]
    }
  },
  {
    id: 'PROJECT_SCHEDULING',
    label: 'Project Scheduling',
    moduleId: 'approximation',
    description: 'Vertex Cover 2-approximation over task dependencies — which tasks must be scheduled?',
    template: {
      nodes: ['A', 'B', 'C', 'D'],
      edges: [['A', 'B'], ['B', 'C'], ['C', 'D']]
    }
  },
  {
    id: 'PRIME_TESTING',
    label: 'Prime Testing',
    moduleId: 'randomized',
    description: 'Miller-Rabin primality check with random bases (probable primes, never fabricated).',
    template: { n: 2147483647, rounds: 20 }
  }
];

export default function TextHackPage() {
  const navigate = useNavigate();
  const [active, setActive] = useState<QueryClassDef>(QUERY_CLASSES[0]);
  const [inputText, setInputText] = useState(() => JSON.stringify(QUERY_CLASSES[0].template, null, 2));
  const [response, setResponse] = useState<TextHackResponse | null>(null);
  const [running, setRunning] = useState(false);
  const [runError, setRunError] = useState<Error | null>(null);

  const { loading: modLoading, error: modError } = useApi(() => api.modules());

  const selectClass = useCallback((qc: QueryClassDef) => {
    setActive(qc);
    setInputText(JSON.stringify(qc.template, null, 2));
    setResponse(null);
    setRunError(null);
  }, []);

  const run = useCallback(async () => {
    let parsed: Record<string, unknown>;
    try {
      parsed = JSON.parse(inputText) as Record<string, unknown>;
    } catch (e) {
      setRunError(e instanceof Error ? new Error(`Invalid JSON: ${e.message}`) : new Error('Invalid JSON'));
      return;
    }
    setRunning(true);
    setRunError(null);
    try {
      const res = await api.textHack(active.id, parsed);
      setResponse(res);
    } catch (e) {
      setRunError(e instanceof Error ? e : new Error(String(e)));
    } finally {
      setRunning(false);
    }
  }, [active.id, inputText]);

  const saveAsRun = useCallback(async () => {
    if (!response?.traceAlgorithmKey) return;
    let parsed: Record<string, unknown>;
    try {
      parsed = JSON.parse(inputText) as Record<string, unknown>;
    } catch (e) {
      setRunError(e instanceof Error ? new Error(`Invalid JSON: ${e.message}`) : new Error('Invalid JSON'));
      return;
    }
    try {
      await api.run(response.traceAlgorithmKey, parsed);
      navigate('/runs');
    } catch (e) {
      setRunError(e instanceof Error ? e : new Error(String(e)));
    }
  }, [response, inputText, navigate]);

  if (modLoading) return <Spinner label="Loading TextHack console…" />;
  if (modError) return <ErrorBox error={modError} />;

  const accent = moduleAccent(active.moduleId);
  const glow = moduleGlow(active.moduleId);

  return (
    <div className="page">
      <h2 className="page-title">TextHack Console</h2>
      <p className="lab-intro">
        Ask one of six fixed algorithm questions. Every answer is produced by a real engine running in
        the backend — nothing is simulated.
      </p>

      <div className="th-layout">
        {/* Query-class selection */}
        <div className="th-query-list">
          {QUERY_CLASSES.map((qc) => (
            <button
              key={qc.id}
              className={`th-query${qc.id === active.id ? ' th-query--active' : ''}`}
              style={{ ['--mod-color' as never]: moduleAccent(qc.moduleId) }}
              onClick={() => selectClass(qc)}
              aria-pressed={qc.id === active.id}
            >
              <span className="th-query-name">{qc.label}</span>
              <span className="th-query-desc">{moduleLabel(qc.moduleId)}</span>
            </button>
          ))}
        </div>

        {/* Console */}
        <div className="th-console">
          <Card
            title={active.label}
            actions={
              <span className="badge" style={{ borderColor: accent, color: accent }}>{moduleLabel(active.moduleId)}</span>
            }
          >
            <div className="th-field">
              <label htmlFor="th-input">Input (JSON)</label>
              <textarea
                id="th-input"
                className="input-json"
                spellCheck={false}
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                rows={Math.min(12, inputText.split('\n').length + 1)}
              />
            </div>
            <p className="lab-desc" style={{ marginTop: 0 }}>{active.description}</p>
            <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'center', flexWrap: 'wrap' }}>
              <button className="btn btn-run" onClick={run} disabled={running}>
                {running ? 'Running…' : `Run ${active.label} ›`}
              </button>
              {response?.traceAlgorithmKey && (
                <button className="btn" onClick={saveAsRun}>Save as run session</button>
              )}
            </div>

            {runError && <div style={{ marginTop: '0.9rem' }}><ErrorBox error={runError} /></div>}

            {response && (
              <div style={{ marginTop: '1.1rem' }}>
                <div
                  className="step-card"
                  style={{ background: `linear-gradient(160deg, ${glow} 0%, transparent 60%), var(--bg-elevated)` }}
                >
                  <div className="step-card-head">
                    <span className="step-op">{response.label}</span>
                    <span style={{ color: accent }}>
                      {response.executed
                        ? `${response.executed.executionTimeNanos != null ? formatNanos(response.executed.executionTimeNanos) : ''}`.trim()
                        : ''}
                    </span>
                  </div>

                  <p className="step-desc">{response.description}</p>

                  {response.executed && (
                    <div className="state-table-wrap">
                      <table className="state-table">
                        <tbody>
                          <tr>
                            <td className="state-key">engine</td>
                            <td className="state-val"><code>{response.executed.algorithm}</code></td>
                          </tr>
                          <tr>
                            <td className="state-key">input size</td>
                            <td className="state-val"><code>{response.executed.inputSize ?? '—'}</code></td>
                          </tr>
                          <tr>
                            <td className="state-key">result</td>
                            <td className="state-val">
                              <code>{JSON.stringify(response.executed.result, null, 2)}</code>
                            </td>
                          </tr>
                          <tr>
                            <td className="state-key">complexity</td>
                            <td className="state-val">
                              <code>{response.executed.timeComplexity ?? '—'} · {response.executed.spaceComplexity ?? '—'}</code>
                            </td>
                          </tr>
                          {typeof (response.executed.result as Record<string, unknown>)?.similarityPercent === 'number' && (
                            <tr>
                              <td className="state-key">similarity</td>
                              <td className="state-val">
                                <code>
                                  {(response.executed.result as Record<string, unknown>).similarityPercent as number}
                                  {'% '}({String((response.executed.result as Record<string, unknown>).identityMatches)} identity matches)
                                </code>
                              </td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  )}

                  {response.recommended.length > 0 && (
                    <div className="step-highlight" style={{ marginTop: '0.8rem' }}>
                      {response.recommended.map((rec) => (
                        <Link
                          key={rec.key}
                          className="highlight-chip"
                          to={`/labs/${rec.key}`}
                          style={{ textDecoration: 'none' }}
                        >
                          {rec.tracked ? '▶ ' : ''}{rec.name}
                        </Link>
                      ))}
                    </div>
                  )}

                  {response.traceAlgorithmKey && (
                    <Link className="btn btn-sm" to={`/labs/${response.traceAlgorithmKey}`} style={{ marginTop: '0.6rem' }}>
                      Open in Laboratory
                    </Link>
                  )}
                </div>
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}