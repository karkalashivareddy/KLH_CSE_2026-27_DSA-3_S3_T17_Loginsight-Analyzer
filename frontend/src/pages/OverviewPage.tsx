import { useState } from 'react';
import { Activity, ArrowUpRight, Database, ExternalLink, RefreshCw, Search, ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useApi, type UseApiResult } from '../hooks/useApi';
import { api } from '../api/client';
import type { IncidentDto, LogEvent, ObjectApiResponse, OverviewDto, ServiceStatsDto, SystemStatus } from '../api/types';
import { TopologyPanel, type TopologyMode } from '../components/TopologyPanel';
import { useReplay } from '../replay/ReplayContext';
import {
  Badge,
  Card,
  DonutChart,
  EmptyState,
  ErrorBox,
  EventDrawer,
  HBarChart,
  HeatmapGrid,
  LevelBadge,
  NoDatasetState,
  PageHeader,
  Spinner,
  StatCard,
  StatusPill,
  TimeChart
} from '../components/ui';
import { LEVEL_COLORS, formatMillis, formatNumber, formatTs } from '../components/format';

const RANGES = ['5m', '15m', '1h', '6h', '24h'] as const;
const HEALTHY_ERROR_RATE_MAX = 5;
const WATCH_ERROR_RATE_MAX = 10;

type HealthBand = 'healthy' | 'watch' | 'elevated' | 'unknown';

function scopeText(data: OverviewDto): string {
  return data.scope || `${data.range} selected window`;
}

function windowText(data: OverviewDto): string {
  if (!data.windowStart || !data.windowEnd) return scopeText(data);
  return `${formatTs(data.windowStart)} – ${formatTs(data.windowEnd)}`;
}

function coverageText(data: OverviewDto): { value: string; detail: string } {
  if (!Number.isFinite(data.datasetEvents) || data.datasetEvents <= 0) return { value: '—', detail: 'Full dataset total unavailable' };
  const percentage = (data.events / data.datasetEvents) * 100;
  return { value: `${percentage.toFixed(1)}%`, detail: `${formatNumber(data.events)} of ${formatNumber(data.datasetEvents)} full-dataset events` };
}

function healthBand(rate: number): HealthBand {
  if (!Number.isFinite(rate)) return 'unknown';
  if (rate < HEALTHY_ERROR_RATE_MAX) return 'healthy';
  if (rate < WATCH_ERROR_RATE_MAX) return 'watch';
  return 'elevated';
}

function healthLabel(band: HealthBand): string {
  if (band === 'healthy') return 'Healthy';
  if (band === 'watch') return 'Watch';
  if (band === 'unknown') return 'Unavailable';
  return 'Elevated';
}

function incidentTime(value: string): number {
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function incidentInWindow(incident: IncidentDto, data: OverviewDto): boolean {
  const start = incidentTime(data.windowStart);
  const end = incidentTime(data.windowEnd);
  if (!start || !end) return true;
  const incidentStart = incidentTime(incident.start);
  const incidentEnd = incidentTime(incident.end);
  return incidentEnd >= start && incidentStart <= end;
}

function latestIncident(incidents: IncidentDto[]): IncidentDto | null {
  return incidents.reduce<IncidentDto | null>((latest, incident) => {
    if (!latest || incidentTime(incident.start) > incidentTime(latest.start)) return incident;
    return latest;
  }, null);
}

function investigationFor(incidents: IncidentDto[], data: OverviewDto): IncidentDto | null {
  const scoped = incidents.filter((incident) => incidentInWindow(incident, data));
  const active = scoped.find((incident) => ['ACTIVE', 'OPEN'].includes(incident.status.toUpperCase()));
  const hasWindow = incidentTime(data.windowStart) > 0 && incidentTime(data.windowEnd) > 0;
  return active ?? latestIncident(scoped) ?? (hasWindow ? null : latestIncident(incidents));
}

export default function OverviewPage() {
  const [range, setRange] = useState<string>('1h');
  const [selected, setSelected] = useState<LogEvent | null>(null);
  const [selectedService, setSelectedService] = useState<string | null>(null);
  const [topologyMode, setTopologyMode] = useState<TopologyMode>('2d');
  const overview = useApi<OverviewDto | null>((signal) => api.overview(range, { signal }), `overview-${range}`);
  const dependencies = useApi<ObjectApiResponse>((signal) => api.dependencies({ signal }), 'dependencies');
  const incidents = useApi<IncidentDto[]>((signal) => api.incidents(20, { signal }), 'incidents');
  const health = useApi<SystemStatus>((signal) => api.systemStatus({ signal }));
  const replay = useReplay();
  const data = overview.data;
  const coverage = data ? coverageText(data) : { value: '—', detail: '' };
  const investigation = data ? investigationFor(incidents.data ?? [], data) : null;
  const severityData = data ? Object.entries(data.severity).map(([label, value]) => ({ label, value, color: LEVEL_COLORS[label.toUpperCase()] ?? 'var(--severity-unknown)' })) : [];
  const timeline = data?.timeline.map((point) => ({ label: formatTs(point.start), value: point.count })) ?? [];
  const statusClasses = data ? httpClassBreakdown(data.statusCodes) : [];
  const topologyNodes = data && dependencies.data
    ? dependencies.data.nodes.map((node) => {
      const service = data.topServices.find((candidate) => candidate.name === node.id);
      return {
        ...node,
        errorRate: service?.eventRate,
        health: service ? healthBand(service.eventRate) : 'unknown' as const
      };
    })
    : [];
  const topologyEdges = dependencies.data?.edges ?? [];
  const refreshing = overview.refreshing || dependencies.refreshing || incidents.refreshing || health.refreshing;
  const refreshAll = () => {
    overview.reload();
    dependencies.reload();
    incidents.reload();
    health.reload();
  };

  return (
    <div className="page">
      <PageHeader
        eyebrow="Command Center"
        title="Command Center"
        description={data ? <><strong>{data.dataset}</strong> · {scopeText(data)} · {windowText(data)} · backend <StatusPill status={health.data?.status ?? data.systemStatus} /></> : 'A selected-window operational view built only from returned dataset and runtime data.'}
        actions={<><button className="btn btn-sm" type="button" onClick={refreshAll} disabled={refreshing}><RefreshCw size={14} aria-hidden="true" />{refreshing ? 'Refreshing' : 'Refresh'}</button><Link className="btn btn-sm" to="/datasets">Dataset <ArrowUpRight size={14} aria-hidden="true" /></Link></>}
      />

      <div className="tab-bar command-range-bar" role="group" aria-label="Command Center selected-window range">
        {RANGES.map((value) => <button key={value} className={`btn btn-sm${range === value ? ' btn-primary' : ''}`} type="button" aria-pressed={range === value} onClick={() => { setRange(value); setSelectedService(null); }}>{value}</button>)}
        {overview.lastUpdated && <span className="text-muted">Updated {formatTs(new Date(overview.lastUpdated).toISOString())}</span>}
      </div>

      {overview.loading ? <Card title="Loading selected window"><Spinner label="Requesting the selected-window snapshot…" /></Card> : overview.error ? <ErrorBox error={overview.error} retry={overview.reload} /> : !data ? <NoDatasetState detail="Load a bundled sample or upload a log file before opening Command Center." /> : <>
        <section className="command-metric-strip" aria-label="Command Center selected-window metrics">
          <StatCard label="Selected-window observed rate" value={`${data.eventsPerMinute.toFixed(2)} / min`} sub={`${formatNumber(data.events)} observed events · ${scopeText(data)}`} color="var(--accent)" />
          <StatCard label="Detected incident windows" value={formatNumber(data.activeIncidents)} sub="Heuristic windows returned for this scope" color="var(--warn)" />
          <StatCard label="Dataset coverage" value={coverage.value} sub={coverage.detail} color="var(--mod-flow)" />
          <StatCard label="Selected-window events" value={formatNumber(data.events)} sub={`${formatNumber(data.warnings)} warnings · ${formatNumber(data.errors)} ERROR/FATAL`} color="var(--mod-dp)" />
        </section>

        <section className="command-hero-grid" aria-label="Command Center topology and active investigation">
          <Card className="command-topology-card" title="Observed request-trail topology" sub={`Node size follows dataset-wide events; health band and error rate follow the ${scopeText(data)}. Edge weight follows observed request-trail adjacency, not verified infrastructure.`} actions={<Link className="btn btn-sm" to="/services">Service fleet <ArrowUpRight size={13} aria-hidden="true" /></Link>}>
            {dependencies.loading ? <Spinner label="Requesting observed service adjacency…" /> : dependencies.error ? <ErrorBox error={dependencies.error} retry={dependencies.reload} /> : dependencies.data ? <><TopologyPanel nodes={topologyNodes} edges={topologyEdges} mode={topologyMode} onModeChange={setTopologyMode} selectedId={selectedService} onSelect={setSelectedService} description={`Node size follows dataset-wide events; health band and error rate follow the ${scopeText(data)}. Edge weight follows observed request-trail adjacency, not verified infrastructure.`} />{selectedService && <div className="topology-selection-summary"><span>Selected service: <strong>{selectedService}</strong></span><Link className="btn btn-sm" to={`/services/${encodeURIComponent(selectedService)}`}>Open service <ArrowUpRight size={13} aria-hidden="true" /></Link></div>}</> : <EmptyState>No dependency response is available.</EmptyState>}
          </Card>
          <InvestigationPanel result={incidents} incident={investigation} scope={data} />
        </section>

        <div className="command-pipeline-grid">
          <Card title="Pipeline story" sub="Follow the returned evidence from source loading to event investigation.">
            <ol className="command-pipeline-story">
              <li><Link to="/ingestion"><Database size={15} aria-hidden="true" /><span><strong>Load</strong><small>{data.dataset}</small></span></Link></li>
              <li><Link to="/analytics"><Activity size={15} aria-hidden="true" /><span><strong>Observe</strong><small>{formatNumber(data.events)} selected-window events</small></span></Link></li>
              <li><Link to="/incidents"><ShieldAlert size={15} aria-hidden="true" /><span><strong>Detect</strong><small>{formatNumber(data.activeIncidents)} heuristic windows</small></span></Link></li>
              <li><Link to="/logs"><Search size={15} aria-hidden="true" /><span><strong>Investigate</strong><small>{formatNumber(data.recentCritical.length)} critical events returned</small></span></Link></li>
            </ol>
          </Card>
          <Card title="Shared replay context" sub="One bounded SSE subscription is shared with Live Replay; this is not real-time capture." actions={<Link className="btn btn-sm" to="/live">Open live replay <ArrowUpRight size={13} aria-hidden="true" /></Link>}>
            <div className="replay-context-summary">
              <div className="replay-context-status"><Badge tone="info" label="Demo replay of the loaded dataset, not live production telemetry">DEMO REPLAY</Badge><StatusPill status={replay.statusLabel} /><strong>{replay.currentDataset ?? data.dataset}</strong></div>
              <div className="progress-track" role="progressbar" aria-label="Shared replay progress" aria-valuenow={replay.emitted} aria-valuemin={0} aria-valuemax={Math.max(replay.total, 1)}><div className="progress-fill" style={{ width: `${replay.progress}%` }} /></div>
              <div className="progress-meta"><span>{formatNumber(replay.emitted)} / {formatNumber(replay.total)} events</span><span>{replay.liveStatus?.source ?? '—'}</span></div>
              <div className="quick-actions"><button className="btn btn-sm" type="button" onClick={() => { if (replay.isRunning) replay.stop(); else if (replay.state === 'complete') replay.restart(); else replay.start(); }} disabled={!replay.liveStatus || replay.liveStatus.enabled === false}>{replay.isRunning ? 'Stop replay' : replay.state === 'complete' ? 'Replay again' : 'Start replay'}</button><Link className="btn btn-sm" to="/live">Controls</Link></div>
              {replay.error && <div className="replay-inline-error" role="alert">{replay.error.message}</div>}
            </div>
          </Card>
        </div>

        <Card title={`Observed timeline · ${data.range}`} sub={`${scopeText(data)} · ${windowText(data)}`}>
          <TimeChart data={timeline} height={210} label={`Observed events for ${data.range}`} />
        </Card>

        <div className="command-detail-grid">
          <ServiceHealthMatrix fallback={data.topServices} scope={scopeText(data)} />
          <Card title="Pattern intelligence" sub="Heuristic token normalization; counts and examples are returned data, not machine-learning output." actions={<Link className="btn btn-sm" to="/patterns">All patterns <ArrowUpRight size={13} aria-hidden="true" /></Link>}>
            {data.topPatterns.length === 0 ? <EmptyState>No recurring patterns were returned for this scope.</EmptyState> : <div className="table-scroll"><table className="log-table"><caption className="sr-only">Pattern intelligence for the selected window</caption><thead><tr><th>Level</th><th>Template</th><th>Count</th><th>Share</th><th>Example</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{data.topPatterns.map((pattern) => <tr key={`${pattern.template}-${pattern.level}`}><td><LevelBadge level={pattern.level} /></td><td><Link to={`/patterns?level=${encodeURIComponent(pattern.level || 'all')}`}><code className="pattern-template">{pattern.template}</code></Link></td><td className="num">{formatNumber(pattern.count)}</td><td className="num">{data.events > 0 ? `${((pattern.count / data.events) * 100).toFixed(2)}%` : '—'}</td><td className="muted">{pattern.example}</td><td><Link className="btn btn-sm" to={`/logs?q=${encodeURIComponent(pattern.example || pattern.template)}`} aria-label={`Search logs for the example message of ${pattern.template}`}><Search size={13} aria-hidden="true" /> Logs</Link></td></tr>)}</tbody></table></div>}
          </Card>
        </div>

        <div className="command-detail-grid">
          <Card title="HTTP class breakdown" sub="Observed response classes from the selected-window status-code counts; latency is not inferred.">
            {statusClasses.length === 0 ? <EmptyState>No HTTP status codes were returned for this scope.</EmptyState> : <div className="http-class-chart" aria-label="Observed HTTP response classes"><HBarChart data={statusClasses} maxItems={10} /><div className="chart-footnote">Classes aggregate the returned status codes; exact code values remain available in Analytics.</div></div>}
          </Card>
          <Card title="Window context" sub="Selected-window severity and activity distribution.">
            <div className="window-context-grid"><div><DonutChart data={severityData} size={145} label={`Severity for ${data.range}`} /></div><div className="table-scroll"><HeatmapGrid days={data.heatmap.days} columns={data.heatmap.columns} cells={data.heatmap.cells} /></div></div>
          </Card>
        </div>

        <Card title="Recent critical events" sub={`Newest ERROR/FATAL events returned for ${scopeText(data)}`} actions={<Link className="btn btn-sm" to="/logs">Open explorer <ArrowUpRight size={14} aria-hidden="true" /></Link>}>
          {data.recentCritical.length === 0 ? <EmptyState>No critical events were returned in this scope.</EmptyState> : <div className="log-list">{data.recentCritical.map((event) => <div className="log-item" key={event.id}><div className="log-item-top"><LevelBadge level={event.level} /><span className="log-item-service">{event.service}</span><span className="log-item-host">{event.host}</span><span className="log-item-time">{formatTs(event.timestamp)}</span></div><Link className="log-item-msg" to={`/logs/${event.id}`}>{event.message}</Link><div className="log-item-meta">{event.httpMethod && <span className="http-pill">{event.httpMethod} {event.statusCode}</span>}{event.responseTime > 0 && <span>{formatMillis(event.responseTime)}</span>}<Link className="btn btn-sm" to={`/services/${encodeURIComponent(event.service)}`}>Service</Link><button className="icon-btn" type="button" onClick={() => setSelected(event)} aria-label={`Inspect event ${event.id}`} title="Inspect event"><ExternalLink size={14} aria-hidden="true" /></button></div></div>)}</div>}
        </Card>
      </>}

      <EventDrawer event={selected} onClose={() => setSelected(null)} />
    </div>
  );
}

function InvestigationPanel({ result, incident, scope }: { result: UseApiResult<IncidentDto[]>; incident: IncidentDto | null; scope: OverviewDto }) {
  return <Card className="command-investigation-card" title="Latest detected investigation" sub={`Dataset-wide detector results filtered to the selected window: ${scopeText(scope)} · ${windowText(scope)}. Heuristic window evidence; no causal attribution is inferred.`} actions={<Link className="btn btn-sm" to="/incidents">All incidents <ArrowUpRight size={13} aria-hidden="true" /></Link>}>
    {result.loading ? <Spinner label="Requesting detected investigation windows…" /> : result.error ? <ErrorBox error={result.error} retry={result.reload} /> : !incident ? <EmptyState><ShieldAlert size={22} aria-hidden="true" /><strong>No detected windows returned.</strong><span>The detector returned no incident for this scope. This is not evidence of system health.</span><Link className="btn btn-sm" to="/logs">Inspect events</Link></EmptyState> : <>
      <div className="investigation-status-row"><StatusPill status={incident.status} /><span className="incident-id">Incident #{incident.id}</span><span className="incident-count">{formatNumber(incident.eventCount)} evidence events</span></div>
      <div className="detail-grid investigation-details"><div className="detail-row"><span>Window start</span><strong>{formatTs(incident.start)}</strong></div><div className="detail-row"><span>Window end</span><strong>{formatTs(incident.end)}</strong></div><div className="detail-row"><span>Services named by detector</span><strong>{incident.services.length > 0 ? incident.services.join(', ') : '—'}</strong></div><div className="detail-row"><span>Detector method</span><strong>{incident.method}</strong></div></div>
      <div className="investigation-pattern"><span>Primary returned pattern</span><code>{incident.primaryPattern}</code></div>
      <div className="quick-actions"><Link className="btn btn-sm btn-run" to={`/incidents/${incident.id}`}><ShieldAlert size={13} aria-hidden="true" /> Open evidence</Link>{incident.services.slice(0, 3).map((service) => <Link className="btn btn-sm" key={service} to={`/services/${encodeURIComponent(service)}`}>{service}</Link>)}</div>
      <p className="method-note">The window and pattern are returned detector output. They do not establish why the events occurred.</p>
    </>}
  </Card>;
}

function ServiceHealthMatrix({ fallback, scope }: { fallback: ServiceStatsDto[]; scope: string }) {
  return <Card title="Service health matrix" sub={`Heuristic error-rate bands only: healthy <${HEALTHY_ERROR_RATE_MAX}%, watch ${HEALTHY_ERROR_RATE_MAX}–<${WATCH_ERROR_RATE_MAX}%, elevated ≥${WATCH_ERROR_RATE_MAX}%. Selected-window scope: ${scope}.`}>
    {fallback.length === 0 ? <EmptyState>No service rollups were returned for this scope.</EmptyState> : <div className="table-scroll"><table className="log-table"><caption className="sr-only">Service health matrix derived from selected-window error rates</caption><thead><tr><th>Service</th><th>Events</th><th>Errors</th><th>Error rate</th><th>Heuristic band</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{fallback.map((service) => { const band = healthBand(service.eventRate); return <tr key={service.name}><td><Link to={`/services/${encodeURIComponent(service.name)}`}><strong>{service.name}</strong></Link></td><td className="num">{formatNumber(service.events)}</td><td className="num">{formatNumber(service.errors)}</td><td className="num">{Number.isFinite(service.eventRate) ? `${service.eventRate.toFixed(2)}%` : '—'}</td><td><BadgeHollow band={band} /></td><td><Link className="btn btn-sm" to={`/logs?q=${encodeURIComponent(`service:${service.name}`)}`}><ExternalLink size={13} aria-hidden="true" /> Events</Link></td></tr>; })}</tbody></table></div>}
  </Card>;
}

function BadgeHollow({ band }: { band: HealthBand }) {
  return <span className={`health-band health-band--${band}`} role="img" aria-label={`Heuristic health band: ${healthLabel(band)}`}>{healthLabel(band)}</span>;
}

function httpClassBreakdown(statusCodes: Record<string, number>): Array<{ label: string; value: number }> {
  const grouped = new Map<string, number>();
  Object.entries(statusCodes).forEach(([code, count]) => {
    const numeric = Number(code);
    const label = Number.isInteger(numeric) && numeric >= 100 && numeric <= 599 ? `${Math.floor(numeric / 100)}xx` : `HTTP ${code}`;
    grouped.set(label, (grouped.get(label) ?? 0) + count);
  });
  return [...grouped.entries()].sort((left, right) => left[0].localeCompare(right[0])).map(([label, value]) => ({ label, value }));
}
