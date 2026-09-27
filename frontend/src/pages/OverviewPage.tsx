import { useMemo, useState, type ReactNode } from 'react';
import { Activity, ArrowUpRight, Database, ExternalLink, Radio, RefreshCw, Search, ShieldAlert, Timer } from 'lucide-react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { IncidentDto, LogEvent, ObjectApiResponse, OverviewDto, SystemStatus } from '../api/types';
import { useApi, type UseApiResult } from '../hooks/useApi';
import { TopologyPanel, type TopologyMode } from '../components/TopologyPanel';
import { useReplay } from '../replay/ReplayContext';
import { SimulationBand } from '../telemetry/SimulationBand';
import { Badge, Card, EmptyState, ErrorBox, EventDrawer, LevelBadge, NoDatasetState, PageHeader, Spinner, StatusPill, TimeChart } from '../components/ui';
import { formatMillis, formatNumber, formatTs } from '../components/format';

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

function healthBand(rate: number): HealthBand {
  if (!Number.isFinite(rate)) return 'unknown';
  if (rate < HEALTHY_ERROR_RATE_MAX) return 'healthy';
  if (rate < WATCH_ERROR_RATE_MAX) return 'watch';
  return 'elevated';
}

function incidentTime(value: string): number {
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function incidentInWindow(incident: IncidentDto, data: OverviewDto): boolean {
  const start = incidentTime(data.windowStart);
  const end = incidentTime(data.windowEnd);
  if (!start || !end) return true;
  return incidentTime(incident.end) >= start && incidentTime(incident.start) <= end;
}

function investigationFor(incidents: IncidentDto[], data: OverviewDto): IncidentDto | null {
  const scoped = incidents.filter((incident) => incidentInWindow(incident, data));
  return scoped.find((incident) => ['ACTIVE', 'OPEN'].includes(incident.status.toUpperCase()))
    ?? scoped.reduce<IncidentDto | null>((latest, incident) => !latest || incidentTime(incident.start) > incidentTime(latest.start) ? incident : latest, null);
}

export default function OverviewPage() {
  const [range, setRange] = useState<string>('1h');
  const [selectedEvent, setSelectedEvent] = useState<LogEvent | null>(null);
  const [selectedService, setSelectedService] = useState<string | null>(null);
  const [topologyMode, setTopologyMode] = useState<TopologyMode>('2d');
  const overview = useApi<OverviewDto | null>((signal) => api.overview(range, { signal }), `overview-${range}`);
  const dependencies = useApi<ObjectApiResponse>((signal) => api.dependencies({ signal }), 'dependencies');
  const incidents = useApi<IncidentDto[]>((signal) => api.incidents(20, { signal }), 'incidents');
  const health = useApi<SystemStatus>((signal) => api.systemStatus({ signal }));
  const replay = useReplay();
  const data = overview.data;
  const investigation = data ? investigationFor(incidents.data ?? [], data) : null;
  const topologyNodes = useMemo(() => data && dependencies.data
    ? dependencies.data.nodes.map((node) => {
      const service = data.topServices.find((candidate) => candidate.name === node.id);
      return { ...node, errorRate: service?.eventRate, health: service ? healthBand(service.eventRate) : 'unknown' as const };
    })
    : [], [data, dependencies.data]);
  const topologyEdges = useMemo(() => dependencies.data?.edges ?? [], [dependencies.data]);
  const refreshAll = () => { overview.reload(); dependencies.reload(); incidents.reload(); health.reload(); };
  const errorShare = data && data.events > 0 ? (data.errors / data.events) * 100 : null;

  return (
    <div className="page experience-page overview-page">
      <PageHeader
        eyebrow="LOGINSIGHT / OBSERVE"
        title="System overview"
        description={data ? <><strong>{data.dataset}</strong> · {scopeText(data)} · {windowText(data)}</> : 'A workspace for reading operational signals, following service relationships and inspecting evidence.'}
        actions={<><button className="btn btn-sm" type="button" onClick={refreshAll} disabled={overview.refreshing || dependencies.refreshing || incidents.refreshing || health.refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh</button><Link className="btn btn-sm" to="/replay"><Radio size={14} aria-hidden="true" /> Dataset replay</Link><Link className="btn btn-primary" to="/live"><Radio size={14} aria-hidden="true" /> Live monitor</Link></>}
      />

      <div className="page">
        <SimulationBand />
      </div>

      <section className="overview-intro" aria-label="LogInsight overview">
        <div className="overview-intro-copy">
          <div className="overview-kicker"><span className="signal-mark" aria-hidden="true"><Activity size={15} /></span> SIGNALS · IMPACT · EVIDENCE</div>
          <h2>See the signal.<br /><span>Follow the evidence.</span></h2>
          <p>Move from observed activity to the logs and algorithms behind an investigation.</p>
        </div>
        <div className="overview-source-card">
          <div className="source-card-heading"><span className="source-pulse" aria-hidden="true" /><span>{data ? 'SOURCE IN VIEW' : 'WORKSPACE READY'}</span></div>
          {data ? <><strong>{data.dataset}</strong><span>{scopeText(data)} · {formatNumber(data.datasetEvents)} dataset events</span><div className="source-card-actions"><Link to="/datasets">Change source <ArrowUpRight size={13} aria-hidden="true" /></Link><Link to="/live">Open dataset replay</Link></div>{replay.liveStatus?.enabled && <Badge tone="info" label="Bounded SSE replay of this dataset; not live production telemetry">REPLAY {replay.statusLabel.toUpperCase()}</Badge>}</> : <><strong>No investigation data yet</strong><span>Choose a source to populate this workspace.</span><div className="source-card-actions"><Link to="/live">Replay data</Link><Link to="/datasets">Choose a source</Link></div></>}
        </div>
      </section>

      <div className="overview-window-bar">
        <div className="window-label"><span className="window-label-dot" /> OBSERVED WINDOW</div>
        <div className="window-switch" role="group" aria-label="Observed time window">
          {RANGES.map((value) => <button key={value} type="button" aria-pressed={range === value} className={range === value ? 'is-active' : ''} onClick={() => { setRange(value); setSelectedService(null); }}>{value}</button>)}
        </div>
        {overview.lastUpdated && <span className="window-updated">Updated {formatTs(new Date(overview.lastUpdated).toISOString())}</span>}
      </div>

      {overview.loading ? <div className="experience-loading"><div className="skeleton-line skeleton-line--wide" /><div className="skeleton-grid">{Array.from({ length: 4 }, (_, index) => <div key={index} className="skeleton-metric" />)}</div><div className="skeleton-hero" /></div>
        : overview.error ? <ErrorBox error={overview.error} retry={overview.reload} />
          : !data ? <NoDatasetState detail="Start with a bounded replay, load a bundled sample, or upload logs. Metrics remain empty until a source is loaded." />
            : <>
              <section className="overview-metrics" aria-label="Selected-window signals">
                <Metric label="Events observed" value={formatNumber(data.events)} note={`${scopeText(data)} · ${formatNumber(data.datasetEvents)} in source`} tone="cyan" icon={<Activity size={16} aria-hidden="true" />} />
                <Metric label="Observed rate" value={`${data.eventsPerMinute.toFixed(1)} / min`} note="Backend selected-window aggregate" tone="blue" icon={<Timer size={16} aria-hidden="true" />} />
                <Metric label="Error share" value={errorShare === null ? '—' : `${errorShare.toFixed(2)}%`} note={`${formatNumber(data.errors)} ERROR/FATAL of ${formatNumber(data.events)} events`} tone={errorShare !== null && errorShare >= 5 ? 'amber' : 'green'} icon={<ShieldAlert size={16} aria-hidden="true" />} />
                <Metric label="Incident windows" value={formatNumber(data.activeIncidents)} note="Heuristic windows returned for scope" tone="violet" icon={<Database size={16} aria-hidden="true" />} />
              </section>

              <section className="overview-main-grid" aria-label="System signals and current investigation">
                <Card className="overview-topology-surface" title="Service relationships" sub="Observed request-trail adjacency. This view reflects logs and does not verify infrastructure or prove causality." actions={<Link className="text-action" to="/services">Open service view <ArrowUpRight size={13} aria-hidden="true" /></Link>}>
                  {dependencies.loading ? <Spinner label="Loading observed service relationships" /> : dependencies.error ? <ErrorBox error={dependencies.error} retry={dependencies.reload} /> : dependencies.data ? <TopologyPanel nodes={topologyNodes} edges={topologyEdges} mode={topologyMode} onModeChange={setTopologyMode} selectedId={selectedService} onSelect={setSelectedService} incidentServiceIds={investigation?.services ?? []} incidentLabel={investigation ? `Services named by incident #${investigation.id} detector window` : undefined} description={`Service sizes follow dataset events. Health and error share follow ${scopeText(data)}. Connections follow request identifiers observed in the log records.`} /> : <EmptyState>No dependency view was returned for this source.</EmptyState>}
                  {selectedService && <div className="selection-strip"><span>Selected <strong>{selectedService}</strong></span><Link to={`/services/${encodeURIComponent(selectedService)}`}>Inspect service <ArrowUpRight size={13} aria-hidden="true" /></Link></div>}
                </Card>

                <InvestigationCard result={incidents} incident={investigation} />
              </section>

              <section className="overview-lower-grid" aria-label="Recent signal context">
                <Card title="Event signals" sub={`Recent ERROR/FATAL events returned for ${scopeText(data)}.`} actions={<Link className="text-action" to="/logs">Open logs <ArrowUpRight size={13} aria-hidden="true" /></Link>}>
                  {data.recentCritical.length === 0 ? <EmptyState>No critical events were returned for this selected window.</EmptyState> : <div className="signal-event-list">{data.recentCritical.slice(0, 6).map((event) => <article className="signal-event" key={event.id}>
                    <div className="signal-event-marker" aria-hidden="true" /><div className="signal-event-copy"><div className="signal-event-meta"><LevelBadge level={event.level} /><Link to={`/services/${encodeURIComponent(event.service)}`}>{event.service}</Link><time>{formatTs(event.timestamp)}</time></div><button type="button" className="signal-event-message" onClick={() => setSelectedEvent(event)}>{event.message}</button><div className="signal-event-foot">{event.httpMethod && <span>{event.httpMethod} {event.statusCode}</span>}{event.responseTime > 0 && <span>{formatMillis(event.responseTime)}</span>}<span>{event.host}</span></div></div><button className="icon-btn" type="button" onClick={() => setSelectedEvent(event)} aria-label={`Inspect event ${event.id}`}><ExternalLink size={14} aria-hidden="true" /></button>
                  </article>)}</div>}
                </Card>

                <div className="overview-context-column">
                  <Card title="Activity rhythm" sub={`${data.range} · ${windowText(data)}`}><TimeChart data={data.timeline.map((point) => ({ label: formatTs(point.start), value: point.count }))} height={170} label={`Observed event count over ${data.range}`} /></Card>
                  <Card title="Recurring patterns" sub="Normalized message templates returned by the backend.">
                    {data.topPatterns.length === 0 ? <EmptyState>No recurring patterns were returned.</EmptyState> : <div className="overview-pattern-list">{data.topPatterns.slice(0, 4).map((pattern) => <Link key={`${pattern.template}-${pattern.level}`} to={`/logs?q=${encodeURIComponent(pattern.example || pattern.template)}`}><span className="pattern-level">{pattern.level || 'UNKNOWN'}</span><code>{pattern.template}</code><strong>{formatNumber(pattern.count)}</strong><Search size={14} aria-hidden="true" /></Link>)}</div>}
                  </Card>
                </div>
              </section>
            </>}
      <EventDrawer event={selectedEvent} onClose={() => setSelectedEvent(null)} />
    </div>
  );
}

function Metric({ label, value, note, tone, icon }: { label: string; value: string; note: string; tone: 'cyan' | 'blue' | 'green' | 'amber' | 'violet'; icon: ReactNode }) {
  return <div className={`overview-metric overview-metric--${tone}`}><span className="overview-metric-icon">{icon}</span><div className="overview-metric-label">{label}</div><strong>{value}</strong><small>{note}</small></div>;
}

function InvestigationCard({ result, incident }: { result: UseApiResult<IncidentDto[]>; incident: IncidentDto | null }) {
  return <Card className="investigation-feature" title="Investigation" sub="Detector output for the selected window." actions={<Link className="text-action" to="/incidents">All incidents <ArrowUpRight size={13} aria-hidden="true" /></Link>}>
    {result.loading ? <Spinner label="Requesting incident windows" /> : result.error ? <ErrorBox error={result.error} retry={result.reload} /> : !incident ? <EmptyState><ShieldAlert size={22} aria-hidden="true" /><strong>No detector window in this view</strong><span>This is an empty result, not evidence of system health.</span><Link className="btn btn-sm" to="/logs">Inspect logs</Link></EmptyState> : <>
      <div className="investigation-feature-top"><span className="investigation-index">#{incident.id}</span><StatusPill status={incident.status} /></div>
      <h3>{incident.primaryPattern || 'Elevated error window'}</h3>
      <p className="investigation-feature-time">{formatTs(incident.start)} <span>to</span> {formatTs(incident.end)}</p>
      <div className="investigation-stat"><strong>{formatNumber(incident.eventCount)}</strong><span>ERROR/FATAL events in detector window</span></div>
      <div className="investigation-service-list"><span>Services named by detector</span>{incident.services.length ? incident.services.map((service) => <Link key={service} to={`/services/${encodeURIComponent(service)}`}>{service}<ArrowUpRight size={12} aria-hidden="true" /></Link>) : <small>No service names returned</small>}</div>
      <div className="investigation-method"><span>Method</span><p>{incident.method}</p></div>
      <Link className="investigation-open" to={`/incidents/${incident.id}`}>Open investigation <ArrowUpRight size={15} aria-hidden="true" /></Link>
      <p className="investigation-caveat">This is heuristic grouping of observed errors. It does not establish a root cause.</p>
    </>}
  </Card>;
}
