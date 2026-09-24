import { Link, useParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { ServiceDetail, ServiceStatsDto } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState, LevelBadge, StatCard } from '../components/ui';
import { formatNanos, formatNumber, formatTs } from '../components/format';

/** Service fleet health (docs/API.md §8): per-service rollups + drill-down detail. */
export default function ServicesPage() {
  const { name } = useParams();
  if (name) return <ServiceDetailView name={name} />;
  return <FleetView />;
}

function FleetView() {
  const { data, loading, error, reload } = useApi<ServiceStatsDto[]>(() => api.services(50));

  if (loading) return <Spinner label="Loading services…" />;
  if (error) return <ErrorBox error={error} retry={reload} />;
  if (!data) return <EmptyState>No dataset loaded.</EmptyState>;

  return (
    <div className="page">
      <h2 className="page-title">Services</h2>
      {data.length === 0 ? (
        <EmptyState>No services found in the current dataset.</EmptyState>
      ) : (
        <div className="service-grid">
          {data.map((s) => {
            const worst = s.errors > 0 ? '#e74c3c' : s.warnings > 0 ? '#f1c40f' : '#3498db';
            return (
              <Link key={s.name} className="service-card" to={`/services/${encodeURIComponent(s.name)}`}>
                <div className="service-card-head">
                  <h3 className="service-name">{s.name}</h3>
                  <span className="service-rate" style={{ color: worst }}>{s.eventRate.toFixed(2)}% errors</span>
                </div>
                <div className="service-card-stats">
                  <StatCard label="Events" value={formatNumber(s.events)} color="var(--accent)" />
                  <StatCard label="Errors" value={formatNumber(s.errors)} color="#e74c3c" />
                  <StatCard label="Hosts" value={String(s.hosts)} color="#9b59b6" />
                </div>
                <div className="service-card-foot">
                  <span>{s.latestAt ? `latest ${formatTs(s.latestAt)}` : 'no timestamp'}</span>
                  <span>warnings {formatNumber(s.warnings)}</span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}

function ServiceDetailView({ name }: { name: string }) {
  const { data, loading, error, reload } = useApi<ServiceDetail>(() => api.serviceDetail(name), name);

  if (loading) return <Spinner label={`Loading ${name}…`} />;
  if (error) return <ErrorBox error={error} retry={reload} />;
  if (!data) return <EmptyState>Service not found.</EmptyState>;

  const summary = data.summary;
  const activity = data.activity.map((p) => ({ label: p.start, value: p.count }));

  return (
    <div className="page">
      <h2 className="page-title">
        <Link className="btn btn-sm" to="/services">← Services</Link>
        <span className="title-inline">{summary.name}</span>
      </h2>

      <div className="stat-grid">
        <StatCard label="Events" value={formatNumber(summary.events)} color="var(--accent)" />
        <StatCard label="Errors" value={formatNumber(summary.errors)} color="#e74c3c" />
        <StatCard label="Warnings" value={formatNumber(summary.warnings)} color="#f1c40f" />
        <StatCard label="Hosts" value={String(summary.hosts)} color="#9b59b6" />
        <StatCard label="Error rate" value={`${summary.eventRate.toFixed(2)}%`} />
      </div>

      <div className="grid-2">
        <Card title="24h Activity">
          <svg className="chart-svg" viewBox="0 0 600 140" width="100%" preserveAspectRatio="none">
            {activity.map((p, i) => {
              const max = Math.max(...activity.map((t) => t.value), 1);
              const gap = 600 / Math.max(1, activity.length - 1);
              const x = i * gap;
              const y = 12 + (118 * (1 - p.value / max));
              return (
                <g key={i}>
                  <circle cx={x} cy={y} r={2.5} fill="var(--accent)" />
                  {i > 0 && <line x1={x - gap} y1={12 + (118 * (1 - activity[i - 1].value / max))} x2={x} y2={y} stroke="var(--accent)" strokeWidth="1.5" />}
                </g>
              );
            })}
          </svg>
        </Card>
        <Card title="Severity">
          <table className="info-table">
            <tbody>
              {Object.entries(summary.severity).map(([k, v]) => (
                <tr key={k}><th>{k}</th><td>{formatNumber(v)}</td></tr>
              ))}
            </tbody>
          </table>
        </Card>
      </div>

      <Card title="Recent Events" sub={`${formatNumber(data.totalEvents)} total for ${summary.name}`}>
        {data.recentEvents.length === 0 ? (
          <EmptyState>No events recorded.</EmptyState>
        ) : (
          <div className="log-list">
            {data.recentEvents.map((e) => (
              <Link key={e.id} className="log-item" to={`/logs/${e.id}`}>
                <div className="log-item-top">
                  <LevelBadge level={e.level} />
                  <span className="log-item-host">{e.host}</span>
                  <span className="log-item-time">{formatTs(e.timestamp)}</span>
                </div>
                <div className="log-item-msg">{e.message}</div>
                <div className="log-item-meta">
                  {e.httpMethod && <span className="http-pill">{e.httpMethod} {e.statusCode}</span>}
                  {e.responseTime > 0 && <span>{formatNanos(e.responseTime)}</span>}
                </div>
              </Link>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}