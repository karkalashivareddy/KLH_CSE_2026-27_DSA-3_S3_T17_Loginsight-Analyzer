import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { OverviewDto } from '../api/types';
import {
  Card, StatCard, Spinner, ErrorBox, EmptyState, LevelBadge,
  TimeChart, DonutChart, HBarChart, HeatmapGrid, Badge
} from '../components/ui';
import { LEVEL_COLORS, formatNumber, formatTs, formatNanos } from '../components/format';

const RANGES = ['5m', '15m', '1h', '6h', '24h'] as const;

/**
 * Command Center: a live dashboard derived entirely from the loaded dataset via GET /api/overview.
 * When nothing is loaded the first-run state points at the demo dataset — no numbers are fabricated.
 */
export default function OverviewPage() {
  const [range, setRange] = useState<string>('1h');
  const { data, loading, error, reload } = useApi<OverviewDto | null>(() => api.overview(range), range);

  if (loading) return <Spinner label="Loading overview…" />;
  if (error) return <ErrorBox error={error} retry={reload} />;

  if (!data) {
    return (
      <div className="page">
        <h2 className="page-title">Command Center</h2>
        <EmptyState>
          <strong>No dataset loaded.</strong>
          <span>Load the demo dataset to explore the analyzer, or head to Datasets to import a file.</span>
          <div className="quick-actions">
            <Link className="btn btn-run" to="/datasets">Open Datasets</Link>
            <Link className="btn" to="/ingestion">Ingestion</Link>
          </div>
        </EmptyState>
      </div>
    );
  }

  const severityData = Object.entries(data.severity)
    .map(([label, value]) => ({ label, value, color: LEVEL_COLORS[label.toUpperCase()] ?? '#666' }));
  const statusData = Object.entries(data.statusCodes)
    .map(([label, value]) => ({ label, value, color: '#4a9' }));
  const timeline = data.timeline.map((p) => ({ label: formatTs(p.start), value: p.count }));

  return (
    <div className="page">
      <div className="hero-banner">
        <div>
          <h2 className="hero-title">Command Center</h2>
          <p className="hero-sub">
            {data.dataset} · {data.systemStatus} · every number computed live from the loaded dataset
          </p>
        </div>
        <div className="range-tabs">
          {RANGES.map((r) => (
            <button key={r} className={`btn btn-sm${range === r ? ' btn-primary' : ''}`} onClick={() => setRange(r)}>
              {r}
            </button>
          ))}
        </div>
      </div>

      <div className="stat-grid">
        <StatCard label="Events"        value={formatNumber(data.events)}      color="var(--accent)" />
        <StatCard label="Errors"        value={formatNumber(data.errors)}      color="#e74c3c" />
        <StatCard label="Warnings"      value={formatNumber(data.warnings)}    color="#f1c40f" />
        <StatCard label="Services"      value={formatNumber(data.services)}    color="#3498db" />
        <StatCard label="Hosts"         value={formatNumber(data.hosts)}       color="#9b59b6" />
        <StatCard label="Events / min"  value={String(data.eventsPerMinute)}   color="#2ecc71" />
        <StatCard label="Active Incidents" value={formatNumber(data.activeIncidents)} color="#e67e22" sub="heuristic detection" />
      </div>

      <div className="grid-2">
        <Card title={`Timeline (${data.range})`}>
          <TimeChart data={timeline} height={160} />
        </Card>
        <Card title="Severity Distribution">
          {severityData.length > 0 ? <DonutChart data={severityData} size={140} /> : <EmptyState>No levels</EmptyState>}
        </Card>
      </div>

      <div className="grid-2">
        <Card title="Top Services">
          <HBarChart data={data.topServices.map((s) => ({ label: s.name, value: s.events }))} maxItems={8} />
        </Card>
        <Card title="HTTP Status Codes">
          {statusData.length > 0 ? <HBarChart data={statusData} maxItems={10} /> : <EmptyState>No HTTP events</EmptyState>}
        </Card>
      </div>

      <Card title="Top Message Patterns" sub="heuristic token pattern extraction">
        {data.topPatterns.length > 0 ? (
          <div className="pattern-list">
            {data.topPatterns.map((p) => (
              <div key={p.template} className="pattern-item">
                <LevelBadge level={p.level} />
                <code className="pattern-template">{p.template}</code>
                <span className="pattern-count">{formatNumber(p.count)}</span>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState>No patterns found</EmptyState>
        )}
      </Card>

      <Card title="Activity Heatmap" sub="7 × 24 grid, hours in UTC">
        <HeatmapGrid days={data.heatmap.days} columns={data.heatmap.columns} cells={data.heatmap.cells} />
      </Card>

      <Card title="Recent Critical Events" actions={<Link className="btn btn-sm" to="/logs">Open Log Explorer ›</Link>}>
        {data.recentCritical.length > 0 ? (
          <div className="log-list">
            {data.recentCritical.map((e) => (
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
          <EmptyState>No critical events in range</EmptyState>
        )}
      </Card>
    </div>
  );
}