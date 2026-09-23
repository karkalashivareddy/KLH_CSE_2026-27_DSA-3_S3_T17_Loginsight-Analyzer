import { Link } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { DatasetStats, ModuleInfo, RunSummary, SystemStatus, DatasetSummary } from '../api/types';
import { Card, StatCard, Spinner, ErrorBox, EmptyState, LevelBadge, TimeChart, BarChart } from '../components/ui';
import { formatDuration, formatNanos, formatNumber, formatTs, moduleAccent, moduleVar, moduleGlow, normaliseBuckets } from '../components/format';

/**
 * Command Center: system health, the six algorithm modules (real counts from the catalogue),
 * recent run sessions, and the live dataset overview. Nothing is fabricated.
 */
export default function OverviewPage() {
  const status  = useApi<SystemStatus>(() => api.systemStatus());
  const stats   = useApi<DatasetStats>(() => api.stats());
  const probe   = useApi<DatasetSummary>(() => api.datasetProbe());
  const modules = useApi<ModuleInfo[]>(() => api.modules());
  const runs    = useApi<RunSummary[]>(() => api.runs());

  if (status.loading) return <Spinner label="Loading overview…" />;
  if (status.error)  return <ErrorBox error={status.error} retry={status.reload} />;

  const sys = status.data;
  const ds  = probe.data;
  const st  = stats.data;
  const recentRuns = (runs.data ?? []).slice(0, 6);

  return (
    <div className="page">
      {/* Hero */}
      <div className="hero-banner">
        <div>
          <h2 className="hero-title">Command Center</h2>
          <p className="hero-sub">
            DSA-3 advanced algorithms on real log intelligence · trace over animation · zero fabricated data
          </p>
        </div>
        <div className="quick-actions">
          <Link className="btn btn-run" to="/text-hack">⌗ TextHack</Link>
          <Link className="btn" to="/labs">⚙ Laboratory</Link>
          <Link className="btn" to="/benchmarks">▤ Benchmarks</Link>
        </div>
      </div>

      {/* Status banner */}
      <div className="banner-row">
        <div className="status-banner status-banner--up">
          <span className="status-dot" />
          <span>Service {sys?.status ?? '…'} — {sys?.service}</span>
        </div>
        <span className="banner-meta">Engines: {sys?.engines ?? '…'}</span>
        <span className="banner-meta">Uptime: {sys ? formatDuration(sys.uptimeMillis) : '…'}</span>
      </div>

      {/* Module cards */}
      {modules.loading ? (
        <Spinner label="Loading modules…" />
      ) : modules.data && modules.data.length > 0 ? (
        <div className="module-grid">
          {modules.data.map((mod) => (
            <Link
              key={mod.id}
              to={`/labs/${mod.id}`}
              className="module-card"
              style={{
                ['--mod-color' as never]: moduleAccent(mod.id),
                ['--glow-mod' as never]: moduleGlow(mod.id)
              }}
            >
              <div className="module-card-head">
                <h3 className="module-card-title">{mod.title}</h3>
                <span className="chip chip--accent">{moduleVar(mod.id)}</span>
              </div>
              <p>{mod.description}</p>
              <div className="module-card-meta">
                <span className="chip">{mod.algorithmCount} algorithms</span>
                <span className="chip">{mod.exposedCount} exposed</span>
                <span className="chip">{mod.trackableCount} traceable</span>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <EmptyState>Module catalogue unavailable — is the backend running?</EmptyState>
      )}

      {/* Recent runs */}
      {recentRuns.length > 0 && (
        <Card
          title="Recent Run Sessions"
          actions={<Link className="btn btn-sm" to="/runs">Open Run Sessions ›</Link>}
        >
          <div className="run-list" style={{ maxHeight: 'none' }}>
            {recentRuns.map((s) => (
              <Link key={s.runId} className="run-item" to="/runs">
                <div className="run-item-top">
                  <span className="run-item-name">{s.algorithmName}</span>
                  <span className={`status-pill status-pill--${s.status}`}>{s.status}</span>
                </div>
                <div className="run-item-meta">
                  <span>{formatTs(s.createdAt)}</span>
                  <span>{s.stepCount} steps</span>
                  <span>{s.executionTimeNanos !== 0 ? formatNanos(s.executionTimeNanos) : '—'}</span>
                </div>
              </Link>
            ))}
          </div>
        </Card>
      )}

      {/* Dataset overview */}
      {!ds?.loaded ? (
        <EmptyState>
          No dataset loaded yet. Head to <strong>Datasets</strong> to import one.
        </EmptyState>
      ) : (
        <>
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

          {st?.topError && (
            <Card title="Most Frequent Error" className="alert-card">
              <p className="error-msg">{st.topError}</p>
            </Card>
          )}

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