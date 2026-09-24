import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { LogEvent, PatternDto } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState, LevelBadge, Badge } from '../components/ui';
import { formatNumber, formatNanos, formatTs } from '../components/format';

const LEVELS = ['all', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE', 'FATAL'] as const;

/** Message patterns: heuristic token normalisation of the loaded dataset (docs/API.md §5). */
export default function PatternsPage() {
  const [level, setLevel] = useState<string>('all');
  const [selected, setSelected] = useState<PatternDto | null>(null);

  const patterns = useApi<PatternDto[]>(() => api.patterns(level, 100), `patterns-${level}`);
  const examples = useApi<LogEvent[]>(
    () => (selected ? api.patternExamples(selected.template, 50) : Promise.resolve([])),
    selected?.template ?? ''
  );

  return (
    <div className="page">
      <h2 className="page-title">Patterns</h2>

      <div className="tab-bar">
        {LEVELS.map((l) => (
          <button
            key={l}
            className={`btn btn-sm${level === l ? ' btn-primary' : ''}`}
            onClick={() => { setLevel(l); setSelected(null); }}
          >
            {l}
          </button>
        ))}
      </div>

      {patterns.loading ? (
        <Spinner label="Extracting patterns…" />
      ) : patterns.error ? (
        <ErrorBox error={patterns.error} retry={patterns.reload} />
      ) : !patterns.data ? (
        <EmptyState>No dataset loaded.</EmptyState>
      ) : patterns.data.length === 0 ? (
        <EmptyState>No patterns at this level.</EmptyState>
      ) : (
        <div className="pattern-layout">
          <Card title={`Recurring structures (${patterns.data.length})`} sub="heuristic token patterns — not ML" className="pattern-list-card">
            <div className="pattern-list">
              {patterns.data.map((p) => (
                <button
                  key={`${p.template}-${p.count}`}
                  className={`pattern-item pattern-item--btn${selected?.template === p.template ? ' pattern-item--active' : ''}`}
                  onClick={() => setSelected(p)}
                >
                  <LevelBadge level={p.level} />
                  <code className="pattern-template">{p.template}</code>
                  <span className="pattern-count">{formatNumber(p.count)}</span>
                </button>
              ))}
            </div>
          </Card>

          {selected && (
            <Card title="Sample Events" sub={`example: ${selected.example}`}>
              {examples.loading ? (
                <Spinner label="Loading examples…" />
              ) : examples.error ? (
                <ErrorBox error={examples.error} retry={examples.reload} />
              ) : examples.data && examples.data.length > 0 ? (
                <div className="log-list">
                  {examples.data.map((e) => (
                    <Link key={e.id} className="log-item" to={`/logs/${e.id}`}>
                      <div className="log-item-top">
                        <LevelBadge level={e.level} />
                        <span className="log-item-service">{e.service}</span>
                        <span className="log-item-host">{e.host}</span>
                        <span className="log-item-time">{formatTs(e.timestamp)}</span>
                      </div>
                      <div className="log-item-msg">{e.message}</div>
                      <div className="log-item-meta">
                        {e.httpMethod && <Badge>{e.httpMethod}</Badge>}
                        {e.statusCode > 0 && <Badge>{e.statusCode}</Badge>}
                        {e.responseTime > 0 && <span>{formatNanos(e.responseTime)}</span>}
                      </div>
                    </Link>
                  ))}
                </div>
              ) : (
                <EmptyState>No example events.</EmptyState>
              )}
            </Card>
          )}
        </div>
      )}
    </div>
  );
}