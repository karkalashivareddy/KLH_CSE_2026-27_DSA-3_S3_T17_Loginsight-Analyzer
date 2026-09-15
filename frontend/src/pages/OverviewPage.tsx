import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { DatasetStats, SystemStatus, DatasetSummary } from '../api/types';
import { Card, StatCard, Spinner, ErrorBox, EmptyState, LevelBadge, TimeChart, BarChart } from '../components/ui';
import { formatDuration, formatNumber, normaliseBuckets } from '../components/format';

/**
 * Dashboard overview: live health badge, dataset summary, top-level stat cards and mini charts.
 * Every value is computed from real backend data — nothing is fabricated.
 */
export default function OverviewPage() {
  const status = useApi<SystemStatus>(() => api.systemStatus());
  const stats   = useApi<DatasetStats>(() => api.stats());
  const probe   = useApi<DatasetSummary>(() => api.datasetProbe());

  if (status.loading) return <Spinner label="Loading overview…" />;
  if (status.error)  return <ErrorBox error={status.error} retry={status.reload} />;

  const sys = status.data;
  const ds  = probe.data;
  const st  = stats.data;

  return (
    <div className="page">
      <h2 className="page-title">Overview</h2>

      {/* Status banner */}
      <div className="banner-row">
        <div className="status-banner status-banner--up">
          <span className="status-dot" />
          <span>Service {sys?.status ?? '…'} — {sys?.service}</span>
        </div>
        <span className="banner-meta">Engines: {sys?.engines ?? '…'}</span>
        <span className="banner-meta">Uptime: {sys ? formatDuration(sys.uptimeMillis) : '…'}</span>
      </div>

      {!ds?.loaded ? (
        <EmptyState>
          No dataset loaded yet. Head to <strong>Datasets</strong> to import one.
        </EmptyState>
      ) : (
        <>
          {/* Stat cards */}
          <div className="stat-grid">
            <StatCard label="Total Logs" value={formatNumber(st?.totalLogs ?? 0)} color="var(--accent)" />
            <StatCard label="Errors"     value={formatNumber(st?.errors ?? 0)}     color="#e74c3c" />
            <StatCard label="Warnings"   value={formatNumber(st?.warnings ?? 0)}   color="#f1c40f" />
            <StatCard label="Services"   value={formatNumber(st?.services ?? 0)}   color="#38c172" />
            <StatCard label="Unique IPs" value={formatNumber(st?.uniqueIps ?? 0)}  color="#9b59b6" />
            <StatCard label="Avg Response" value={`${(st?.avgResponseTimeMs ?? 0).toFixed(0)} ms`} color="#3498db" />
            <StatCard label="Req/min"    value={`${(st?.requestsPerMinute ?? 0).toFixed(1)}`} />
            <StatCard label="Dataset"    value={ds.datasetName ?? '—'} sub={ds.size != null ? `${formatNumber(ds.size)} events` : undefined} />
          </div>

          {/* Top error alert */}
          {st?.topError && (
            <Card title="Most Frequent Error" className="alert-card">
              <p className="error-msg">{st.topError}</p>
            </Card>
          )}

          {/* Charts row */}
          <div className="chart-grid">
            <Card title="Traffic Over Time">
              {st ? (
                <TimeChart data={st.logsOverTime.map((b) => ({ label: b.start.slice(11, 16), value: b.count }))} />
              ) : <Spinner label="Loading traffic…" />}
            </Card>

            <Card title="Log Levels">
              {st ? (() => {
                const entries = Object.entries(st.levels);
                const total = entries.reduce((s, [, v]) => s + v, 0);
                if (total === 0) return <EmptyState>No levels</EmptyState>;
                return (
                  <div className="level-list">
                    {entries.map(([lvl, cnt]) => (
                      <div key={lvl} className="level-row">
                        <LevelBadge level={lvl} />
                        <span className="level-bar-wrap">
                          <span className="level-bar" style={{ width: `${(cnt / total) * 100}%` }} />
                        </span>
                        <span className="level-count">{cnt}</span>
                      </div>
                    ))}
                  </div>
                );
              })() : <Spinner label="Loading levels…" />}
            </Card>

            <Card title="Top Services">
              {st ? (
                <BarChart
                  data={normaliseBuckets(st.topServices)}
                  label={`${st.topServices.length} services`}
                />
              ) : <Spinner label="Loading services…" />}
            </Card>
          </div>
        </>
      )}
    </div>
  );
}