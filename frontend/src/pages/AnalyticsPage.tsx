import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { ErrorAnalytics, ObjectApiResponse, TopBucket, TimeBucket, TopDimension } from '../api/types';
import { Card, Spinner, ErrorBox, HBarChart, DependencyGraph, DonutChart, TimeChart } from '../components/ui';

const DIMENSIONS: TopDimension[] = ['service', 'endpoint', 'level', 'ip'];

export default function AnalyticsPage() {
  const [dim, setDim] = useState<TopDimension>('service');

  const deps    = useApi<ObjectApiResponse>(() => api.dependencies());
  const errors  = useApi<ErrorAnalytics>(() => api.errorAnalytics(12));
  const topData = useApi<TopBucket[]>(() => api.top(dim, 8), dim);
  const windows = useApi<TimeBucket[]>(() => api.windows(12));

  const loading = deps.loading || errors.loading || windows.loading;
  const err = deps.error ?? errors.error ?? windows.error;
  if (loading) return <Spinner label="Loading analytics…" />;
  if (err)     return <ErrorBox error={err} retry={() => { deps.reload(); errors.reload(); windows.reload(); }} />;

  const dep = deps.data;
  const errAnalytics = errors.data;
  const timeSeries   = windows.data ?? [];

  const levelEntries = Object.entries(errAnalytics?.patterns ?? {});
  const levelData = levelEntries.map(([msg, count]) => ({
    label: msg.length > 30 ? msg.slice(0, 30) + '…' : msg,
    value: count,
    color: '#e74c3c'
  }));

  return (
    <div className="page">
      <h2 className="page-title">Analytics</h2>

      <div className="chart-grid">
        {/* Dependency Graph */}
        <Card title={`Service Dependency Graph — ${dep?.nodeCount ?? 0} nodes, ${dep?.edgeCount ?? 0} edges`} className="card-wide">
          {dep ? (
            <DependencyGraph nodes={dep.nodes} edges={dep.edges} />
          ) : <Spinner label="Building graph…" />}
        </Card>

        {/* Traffic */}
        <Card title="Traffic Over Time">
          {timeSeries.length > 0 ? (
            <TimeChart data={timeSeries.map((b) => ({ label: b.start.slice(11, 16), value: b.count }))} />
          ) : <Spinner label="Loading…" />}
        </Card>

        {/* Top-K dimension */}
        <Card
          title="Top-K Frequency"
          actions={
            <select className="select-sm" value={dim} onChange={(e) => setDim(e.target.value as TopDimension)}>
              {DIMENSIONS.map((d) => <option key={d} value={d}>{d.charAt(0).toUpperCase() + d.slice(1)}</option>)}
            </select>
          }
        >
          {topData.data ? (
            <HBarChart data={topData.data.map((b) => ({ label: b.service, value: b.count }))} />
          ) : <Spinner label="Loading…" />}
        </Card>

        {/* Error patterns */}
        <Card title="Error / Warning Patterns">
          {levelData.length > 0 ? (
            <>
              <DonutChart data={levelData} size={130} label={`${levelData.length} patterns`} />
            </>
          ) : <div className="empty-state">No error or warning patterns found.</div>}
        </Card>

        {/* Top error alert */}
        {errAnalytics?.topError && (
          <Card title="Most Frequent Error" className="alert-card card-full">
            <p className="error-msg">{errAnalytics.topError}</p>
          </Card>
        )}
      </div>
    </div>
  );
}