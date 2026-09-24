import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { Heatmap as HeatmapDTO, HostRow, HttpStatsDto, OverviewDto } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState, DonutChart, HBarChart, HeatmapGrid, StatCard } from '../components/ui';
import { LEVEL_COLORS, formatNumber, formatNanos } from '../components/format';

const RANGES = ['5m', '15m', '1h', '6h', '24h'] as const;
const TABS = ['timeline', 'severity', 'heatmap', 'http', 'hosts'] as const;
type Tab = (typeof TABS)[number];

/** Analytics: live aggregates computed from the loaded dataset (docs/API.md §7). */
export default function AnalyticsPage() {
  const [tab, setTab] = useState<Tab>('timeline');
  const [range, setRange] = useState<string>('1h');

  const overview = useApi<OverviewDto | null>(() => api.overview(range), `overview-${range}`);
  const heatmap = useApi<HeatmapDTO>(() => api.heatmap());
  const http = useApi<HttpStatsDto>(() => api.httpAnalytics());
  const hosts = useApi<HostRow[]>(() => api.hostAnalytics());

  const ready = tab === 'timeline' ? overview : tab === 'severity' ? overview : tab === 'heatmap' ? heatmap : tab === 'http' ? http : hosts;
  if (ready.loading) return <Spinner label="Loading analytics…" />;

  return (
    <div className="page">
      <h2 className="page-title">Analytics</h2>

      <div className="tab-bar">
        {TABS.map((t) => (
          <button key={t} className={`btn btn-sm${tab === t ? ' btn-primary' : ''}`} onClick={() => setTab(t)}>
            {t}
          </button>
        ))}
      </div>

      {tab === 'timeline' && <TimelineTab overview={overview} range={range} setRange={setRange} />}
      {tab === 'severity' && <SeverityTab overview={overview} />}
      {tab === 'heatmap' && <HeatmapTab heatmap={heatmap} />}
      {tab === 'http' && <HttpTab http={http} />}
      {tab === 'hosts' && <HostsTab hosts={hosts} />}
    </div>
  );
}

function TimelineTab({ overview, range, setRange }: {
  overview: ReturnType<typeof useApi<OverviewDto | null>>;
  range: string;
  setRange: (r: string) => void;
}) {
  const { data, error, reload } = overview;
  if (error) return <ErrorBox error={error} retry={reload} />;
  if (!data) return <NoDataset />;

  const timeline = data.timeline.map((p) => ({ label: p.start, value: p.count }));
  return (
    <>
      <div className="tab-bar">
        {RANGES.map((r) => (
          <button key={r} className={`btn btn-sm${range === r ? ' btn-primary' : ''}`} onClick={() => setRange(r)}>{r}</button>
        ))}
      </div>
      <div className="stat-grid">
        <StatCard label="Events / min" value={String(data.eventsPerMinute)} color="var(--accent)" />
        <StatCard label="Events in range" value={formatNumber(timeline.reduce((n, t) => n + t.value, 0))} />
      </div>
      <Card title={`Traffic series (${range})`}>
        <svg className="chart-svg" viewBox="0 0 600 160" width="100%" preserveAspectRatio="none">
          {timeline.map((p, i) => {
            const max = Math.max(...timeline.map((t) => t.value), 1);
            const gap = 600 / Math.max(1, timeline.length - 1);
            const x = i * gap;
            const y = 12 + (148 * (1 - p.value / max));
            return (
              <g key={i}>
                <circle cx={x} cy={y} r={2.5} fill="var(--accent)" />
                {i > 0 && <line x1={x - gap} y1={12 + (148 * (1 - timeline[i - 1].value / max))} x2={x} y2={y} stroke="var(--accent)" strokeWidth="1.5" />}
              </g>
            );
          })}
        </svg>
        <div className="muted">Buckets from the dataset's own timeline — no fabricated points.</div>
      </Card>
    </>
  );
}

function SeverityTab({ overview }: { overview: ReturnType<typeof useApi<OverviewDto | null>> }) {
  const { data, error, reload } = overview;
  if (error) return <ErrorBox error={error} retry={reload} />;
  if (!data) return <NoDataset />;

  const severity = Object.entries(data.severity)
    .map(([label, value]) => ({ label, value, color: LEVEL_COLORS[label.toUpperCase()] ?? '#666' }));
  return (
    <>
      <div className="grid-2">
        <Card title="Severity Distribution">
          {severity.length > 0 ? <DonutChart data={severity} size={150} /> : <EmptyState>No levels</EmptyState>}
        </Card>
        <Card title="Counts">
          <table className="info-table">
            <tbody>
              {severity.map((s) => (
                <tr key={s.label}><th style={{ color: s.color }}>{s.label}</th><td>{formatNumber(s.value)}</td></tr>
              ))}
            </tbody>
          </table>
        </Card>
      </div>
    </>
  );
}

function HeatmapTab({ heatmap }: { heatmap: ReturnType<typeof useApi<HeatmapDTO>> }) {
  const { data, error, reload } = heatmap;
  if (error) return <ErrorBox error={error} retry={reload} />;
  if (!data) return <EmptyState>No dataset loaded.</EmptyState>;
  return (
    <Card title="Activity Heatmap" sub="7 × 24 grid — hour of day × day of week, UTC">
      <HeatmapGrid days={data.days} columns={data.columns} cells={data.cells} />
    </Card>
  );
}

function HttpTab({ http }: { http: ReturnType<typeof useApi<HttpStatsDto>> }) {
  const { data, error, reload } = http;
  if (error) return <ErrorBox error={error} retry={reload} />;
  if (!data) return <EmptyState>No dataset loaded.</EmptyState>;
  if (data.sampled === 0) return <EmptyState>No HTTP events in the current dataset.</EmptyState>;

  const status = Object.entries(data.statusCodes).map(([label, value]) => ({ label: `HTTP ${label}`, value }));
  const methods = Object.entries(data.methods).map(([label, value]) => ({ label, value }));
  return (
    <>
      <div className="stat-grid">
        <StatCard label="HTTP events sampled" value={formatNumber(data.sampled)} color="var(--accent)" />
        <StatCard label="p50 latency" value={formatNanos(data.latencyP50)} />
        <StatCard label="p95 latency" value={formatNanos(data.latencyP95)} color="#e67e22" />
        <StatCard label="max latency" value={formatNanos(data.latencyMax)} color="#e74c3c" />
      </div>
      <div className="grid-2">
        <Card title="Status Codes"><HBarChart data={status} maxItems={12} /></Card>
        <Card title="Methods"><HBarChart data={methods} maxItems={8} /></Card>
      </div>
      <Card title="Top Endpoints">
        <HBarChart data={data.endpoints.map((e) => ({ label: e.endpoint, value: e.count }))} maxItems={10} />
      </Card>
    </>
  );
}

function HostsTab({ hosts }: { hosts: ReturnType<typeof useApi<HostRow[]>> }) {
  const { data, error, reload } = hosts;
  if (error) return <ErrorBox error={error} retry={reload} />;
  if (!data || data.length === 0) return <EmptyState>No hosts in the current dataset.</EmptyState>;

  return (
    <Card title="Hosts">
      <table className="log-table">
        <thead>
          <tr><th>Host</th><th>Events</th><th>Errors</th><th>Warnings</th><th>Error rate</th></tr>
        </thead>
        <tbody>
          {data.map((h) => (
            <tr key={h.host}>
              <td>{h.host}</td>
              <td>{formatNumber(h.events)}</td>
              <td style={{ color: h.errors > 0 ? '#e74c3c' : undefined }}>{formatNumber(h.errors)}</td>
              <td>{formatNumber(h.warnings)}</td>
              <td>{h.errorRate.toFixed(2)}%</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}

function NoDataset() {
  return (
    <EmptyState>
      <strong>No dataset loaded.</strong>
      <span>Load the demo dataset or import a file to see analytics.</span>
    </EmptyState>
  );
}