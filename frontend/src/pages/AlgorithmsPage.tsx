import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import { Link } from 'react-router-dom';
import type { AlgorithmGroup } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState, Badge } from '../components/ui';

/** Algorithm Laboratory: every exposed DSA engine grouped by module (GET /api/analysis/algorithms). */
export default function AlgorithmsPage() {
  const { data, loading, error, reload } = useApi<AlgorithmGroup[]>(() => api.algorithmGroups());
  const [filter, setFilter] = useState('');

  if (loading) return <Spinner label="Loading algorithm catalogue…" />;
  if (error) return <ErrorBox error={error} retry={reload} />;
  if (!data) return <EmptyState>No algorithm catalogue available.</EmptyState>;

  const q = filter.trim().toLowerCase();
  const groups = q
    ? data
        .map((g) => ({ ...g, algorithms: g.algorithms.filter((a) => a.name.toLowerCase().includes(q) || a.key.toLowerCase().includes(q)) }))
        .filter((g) => g.algorithms.length > 0)
    : data;

  return (
    <div className="page">
      <h2 className="page-title">Algorithm Catalogue</h2>

      <Card title="Filter">
        <input
          className="input"
          type="text"
          placeholder="Filter by algorithm name or key…"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
      </Card>

      {groups.length === 0 ? (
        <EmptyState>No algorithms match "{filter}".</EmptyState>
      ) : (
        groups.map((group) => (
          <Card key={group.module} title={group.module}>
            <div className="algo-grid">
              {group.algorithms.map((a) => (
                <div key={a.key} className="algo-card">
                  <div className="algo-head">
                    <h3 className="algo-name">{a.name}</h3>
                    <code className="algo-key">{a.key}</code>
                  </div>
                  <p className="algo-problem">{a.problem}</p>
                  <div className="algo-chips">
                    <Badge>{a.queryType}</Badge>
                    <Badge>{a.algorithmType}</Badge>
                    {a.tracked && <Badge color="var(--accent)">traceable</Badge>}
                  </div>
                  <div className="algo-complexity">
                    <span>Time: {a.timeComplexity}</span>
                    <span>Space: {a.spaceComplexity}</span>
                  </div>
                  {a.description && <p className="algo-desc">{a.description}</p>}
                  {a.canonicalEndpoint && <code className="algo-endpoint">{a.canonicalEndpoint}</code>}
                </div>
              ))}
            </div>
          </Card>
        ))
      )}

      <p className="muted">
        Run recorded executions and replay their traces on the{' '}
        <Link to="/runs">Run Sessions</Link> screen.
      </p>
    </div>
  );
}