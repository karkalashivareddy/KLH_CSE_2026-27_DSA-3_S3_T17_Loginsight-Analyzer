import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { ModuleInfo, SystemStatus } from '../api/types';
import { Card, StatCard, Spinner, ErrorBox } from '../components/ui';
import { formatDuration, formatTs } from '../components/format';

export default function SystemPage() {
  const { data, loading, error, reload } = useApi<SystemStatus>(() => api.systemStatus());
  const modules = useApi<ModuleInfo[]>(() => api.modules());

  if (loading) return <Spinner label="Loading system status…" />;
  if (error) return <ErrorBox error={error} retry={reload} />;

  const sys = data!;

  return (
    <div className="page">
      <h2 className="page-title">System</h2>

      <div className="stat-grid">
        <StatCard label="Service"    value={sys.service}    color="var(--accent)" />
        <StatCard label="Status"     value={sys.status}     color="#38c172" />
        <StatCard label="Uptime"     value={formatDuration(sys.uptimeMillis)} />
        <StatCard label="Engines"    value={String(sys.engines)} />
        <StatCard label="Modules"    value={String(modules.data?.length ?? '…')} sub={`${modules.data?.reduce((n, m) => n + m.trackableCount, 0) ?? '…'} traceable algorithms`} color="var(--mod-dp)" />
        <StatCard label="Dataset"    value={sys.datasetLoaded ? (sys.datasetName ?? '—') : 'None'} sub={sys.datasetLoaded ? `${sys.datasetSize} events` : 'No dataset loaded'} color={sys.datasetLoaded ? '#38c172' : '#95a5a6'} />
        <StatCard label="Timestamp"  value={formatTs(sys.timestamp)} />
      </div>

      <Card title="Build Information" className="build-info">
        <table className="info-table">
          <tbody>
            <tr><th>Frontend</th><td>React 18 + TypeScript + Vite</td></tr>
            <tr><th>Backend</th><td>Spring Boot 3.5 · Java 21 · In-Memory</td></tr>
            <tr><th>Algorithm Engines</th><td>{sys.engines} registered (real DSA execution)</td></tr>
            <tr><th>DSA Engine Catalogue</th><td>{modules.data?.length ?? 0} modules · {modules.data?.reduce((n, m) => n + m.algorithmCount, 0) ?? '…'} algorithms catalogued</td></tr>
            <tr><th>Trace Capable</th><td>13 algorithms with real-time step recording + SSE replay</td></tr>
            <tr><th>Dataset</th><td>{sys.datasetLoaded ? `${sys.datasetName} — ${sys.datasetSize} events` : 'Not loaded'}</td></tr>
          </tbody>
        </table>
      </Card>
    </div>
  );
}