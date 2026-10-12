import { Activity, BookOpen, Database, RefreshCw, Server, Workflow } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { ModuleInfo, SystemStatus } from '../api/types';
import { Badge, Card, ErrorBox, PageHeader, Spinner, StatCard, StatusPill } from '../components/ui';
import { formatDuration, formatNumber, formatTs } from '../components/format';

export default function SystemPage() {
  const status = useApi<SystemStatus>((signal) => api.systemStatus({ signal }));
  const modules = useApi<ModuleInfo[]>((signal) => api.modules({ signal }));

  return (
    <div className="page">
      <PageHeader eyebrow="Reference" title="System" description="Runtime health and algorithm registry information returned by the backend." actions={<><button className="btn btn-sm" type="button" onClick={() => { status.reload(); modules.reload(); }} disabled={status.refreshing || modules.refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh</button><Link className="btn btn-sm" to="/docs"><BookOpen size={13} aria-hidden="true" /> API guide</Link></>} />
      {status.loading ? <Card title="Loading system status"><Spinner label="Requesting runtime health…" /></Card> : status.error ? <ErrorBox error={status.error} retry={status.reload} /> : !status.data ? <ErrorBox error={new Error('System status response was empty.')} retry={status.reload} /> : <SystemContent system={status.data} modules={modules} />}
    </div>
  );
}

function SystemContent({ system, modules }: { system: SystemStatus; modules: ReturnType<typeof useApi<ModuleInfo[]>> }) {
  const moduleData = modules.data ?? [];
  const trackable = moduleData.reduce((sum, module) => sum + module.trackableCount, 0);
  const catalogued = moduleData.reduce((sum, module) => sum + module.algorithmCount, 0);
  const exposed = moduleData.reduce((sum, module) => sum + module.exposedCount, 0);
  return <>
    <div className="stat-grid">
      <StatCard label="Service" value={system.service} color="var(--accent)" />
      <StatCard label="Status" value={<StatusPill status={system.status} />} color={system.status === 'UP' ? 'var(--ok)' : 'var(--warn)'} />
      <StatCard label="Uptime" value={formatDuration(system.uptimeMillis)} />
      <StatCard label="Registered engines" value={formatNumber(system.engines)} color="var(--mod-parallel)" />
      <StatCard label="Modules" value={moduleData.length > 0 ? formatNumber(moduleData.length) : '—'} sub={moduleData.length > 0 ? `${formatNumber(trackable)} traceable algorithms` : 'Module response unavailable'} color="var(--mod-dp)" />
      <StatCard label="Dataset" value={system.datasetLoaded ? system.datasetName ?? 'Loaded' : 'None'} sub={system.datasetLoaded ? `${formatNumber(system.datasetSize)} events` : 'No dataset loaded'} color={system.datasetLoaded ? 'var(--ok)' : 'var(--text-faint)'} />
      <StatCard label="Status timestamp" value={formatTs(system.timestamp)} />
    </div>
    {modules.error && <ErrorBox error={modules.error} retry={modules.reload} />}
    {modules.loading && !modules.error && <div className="status-banner" role="status"><Activity size={14} aria-hidden="true" /> Loading module registry independently of runtime health…</div>}
    <div className="grid-2">
      <Card title="Runtime status" sub="Values are returned by the health status endpoint.">
        <div className="table-scroll"><table className="info-table"><caption className="sr-only">Runtime status</caption><tbody><tr><th><Activity size={14} aria-hidden="true" /> Service</th><td>{system.service}</td></tr><tr><th><Server size={14} aria-hidden="true" /> Status</th><td><StatusPill status={system.status} /></td></tr><tr><th><Workflow size={14} aria-hidden="true" /> Catalogue</th><td>{moduleData.length > 0 ? `${formatNumber(moduleData.length)} modules · ${formatNumber(catalogued)} algorithms` : 'Unavailable'}</td></tr><tr><th><Database size={14} aria-hidden="true" /> Dataset</th><td>{system.datasetLoaded ? `${system.datasetName ?? 'Loaded'} · ${formatNumber(system.datasetSize)} events` : 'Not loaded'}</td></tr></tbody></table></div>
      </Card>
      <Card title="Dataset and engine context" sub="Operational context returned by the backend.">
        <div className="table-scroll"><table className="info-table"><caption className="sr-only">Dataset and engine context</caption><tbody><tr><th>Dataset state</th><td>{system.datasetLoaded ? <Badge tone="good">Loaded</Badge> : <Badge>No dataset</Badge>}</td></tr><tr><th>Engine count</th><td>{formatNumber(system.engines)}</td></tr><tr><th>Exposed algorithms</th><td>{moduleData.length > 0 ? formatNumber(exposed) : '—'}</td></tr><tr><th>Traceable algorithms</th><td>{moduleData.length > 0 ? formatNumber(trackable) : '—'}</td></tr><tr><th>Health timestamp</th><td>{formatTs(system.timestamp)}</td></tr></tbody></table></div>
      </Card>
    </div>
    <Card title="Module registry" sub={moduleData.length > 0 ? 'Backend module counts and catalogue exposure' : 'No module response is currently available'} actions={modules.refreshing ? <Badge tone="info">Refreshing</Badge> : undefined}>
      {moduleData.length === 0 ? <Card title="Module data"><div className="muted">The module endpoint returned no rows.</div></Card> : <div className="table-scroll"><table className="log-table"><caption className="sr-only">Registered algorithm modules</caption><thead><tr><th>Module</th><th>Algorithms</th><th>Exposed</th><th>Traceable</th><th>Description</th></tr></thead><tbody>{moduleData.map((module) => <tr key={module.id}><td><strong>{module.title}</strong><div className="muted mono">{module.id}</div></td><td className="num">{formatNumber(module.algorithmCount)}</td><td className="num">{formatNumber(module.exposedCount)}</td><td className="num">{formatNumber(module.trackableCount)}</td><td className="muted">{module.description}</td></tr>)}</tbody></table></div>}
    </Card>
    <div className="quick-actions"><Link className="btn btn-sm" to="/algorithms">Open algorithm catalogue</Link><Link className="btn btn-sm" to="/docs">Read API documentation</Link></div>
  </>;
}
