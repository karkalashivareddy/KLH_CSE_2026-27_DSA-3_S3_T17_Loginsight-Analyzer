import { useEffect, useMemo, useRef, useState } from 'react';
import { ChevronLeft, ExternalLink, FlaskConical, Network, RefreshCw, Server } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { LogEvent, ObjectApiResponse, ServiceDetail, ServiceStatsDto } from '../api/types';
import { Badge, Card, EmptyState, ErrorBox, EventDrawer, LevelBadge, NoDatasetState, PageHeader, Spinner, StatCard, TimeChart } from '../components/ui';
import { TopologyPanel, type TopologyMode } from '../components/TopologyPanel';
import { formatMillis, formatNumber, formatTs } from '../components/format';
import { useTelemetry } from '../telemetry/TelemetryContext';
import { healthBand, frameTitle } from '../telemetry/adapters';

function isMissingDataset(error: Error): boolean {
  const status = (error as Error & { apiError?: { status?: number } }).apiError?.status;
  return status === 404;
}

function serviceTone(service: ServiceStatsDto): 'good' | 'warn' | 'danger' {
  if (service.errors > 0) return 'danger';
  if (service.warnings > 0) return 'warn';
  return 'good';
}

const PHASE_TONE: Record<string, 'good' | 'info' | 'warn' | 'danger'> = {
  healthy: 'good',
  onset: 'info',
  degrading: 'warn',
  peak: 'danger',
};

export default function ServicesPage() {
  const { id } = useParams<{ id?: string }>();
  return id ? <ServiceDetailView name={id} /> : <FleetView />;
}

function FleetView() {
  const { data, loading, refreshing, error, reload } = useApi<ServiceStatsDto[]>((signal) => api.services(50, { signal }));
  const dependencies = useApi<ObjectApiResponse>((signal) => api.dependencies({ signal }), 'service-map');
  const [mode, setMode] = useState<TopologyMode>('2d');
  const [selectedName, setSelectedName] = useState<string | null>(null);
  const initializedSelection = useRef(false);
  const { frame, health, isRunning, start } = useTelemetry();
  const list = data ?? [];
  const topologyNodes = useMemo(() => (dependencies.data?.nodes ?? []).map((node) => {
    const service = list.find((candidate) => candidate.name === node.id);
    const rate = service?.eventRate;
    const health = rate === undefined ? 'unknown' : rate < 5 ? 'healthy' : rate < 10 ? 'watch' : 'elevated';
    return { ...node, errorRate: rate, health } as const;
  }), [dependencies.data, list]);
  const selectedService = list.find((service) => service.name === selectedName) ?? null;
  useEffect(() => {
    if (initializedSelection.current || list.length === 0) return;
    initializedSelection.current = true;
    const mostErrors = [...list].sort((left, right) => right.errors - left.errors || right.events - left.events)[0];
    if (mostErrors) setSelectedName(mostErrors.name);
  }, [list]);
  const events = list.reduce((sum, service) => sum + service.events, 0);
  const errors = list.reduce((sum, service) => sum + service.errors, 0);
  const warnings = list.reduce((sum, service) => sum + service.warnings, 0);
  const hosts = list.reduce((sum, service) => sum + service.hosts, 0);

  return (
    <div className="page experience-page services-page">
      <PageHeader eyebrow="OBSERVE / SERVICE MAP" title="Services" description="Explore observed request-trail relationships and the service rollups returned for the selected source." actions={<><button className="btn btn-sm" type="button" onClick={() => { reload(); dependencies.reload(); }} disabled={refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh</button><Link className="btn btn-primary btn-sm" to="/live"><FlaskConical size={14} aria-hidden="true" /> Generated fleet</Link></>} />
      <Card className="simulation-fleet" title="Generated service health" sub="Measured health from the deterministic simulation, independent of any loaded dataset." actions={<span className="sim-source-tag">GENERATED</span>}>
        {frame === null ? (
          <EmptyState>
            <FlaskConical size={22} aria-hidden="true" />
            <strong>No simulation frame yet.</strong>
            <span>Start a scenario to measure generated service health, or choose one in the lab.</span>
            <div className="control-row">
              <button className="btn btn-primary btn-sm" type="button" onClick={() => start()}>Start default scenario</button>
              <Link className="btn btn-sm" to="/scenario-lab">Open Scenario Lab</Link>
            </div>
          </EmptyState>
        ) : (
          <>
            <div className="control-row">
              <span className="mono">{frameTitle(frame, 'Generated run')}</span>
              <Badge tone={PHASE_TONE[frame.phase] ?? 'neutral'}>{frame.phase}</Badge>
              <span className="muted">{formatNumber(frame.windowSize)} events in window</span>
              {isRunning ? <Badge tone="info">Streaming</Badge> : null}
            </div>
            <div className="health-strip">
              {health.map((entry) => (
                <div key={entry.service} className="health-chip">
                  <span className="health-name">{entry.label || entry.service}</span>
                  <span className={`health-state health-${healthBand(entry.state)}`}>{healthBand(entry.state)}</span>
                  <span className="health-metric">{formatNumber(entry.events)} ev · {entry.errors} err · {formatMillis(entry.averageLatencyMs)}</span>
                </div>
              ))}
            </div>
            <p className="control-note">Health bands are measured thresholds over the rolling window. Service structure comes from the declared dependency graph, not from observed traffic.</p>
          </>
        )}
      </Card>
      {loading ? <Card title="Loading services"><Spinner label="Requesting service rollups…" /></Card> : error ? isMissingDataset(error) ? <NoDatasetState detail="Load a dataset before requesting the service fleet." /> : <ErrorBox error={error} retry={reload} /> : list.length === 0 ? <Card title="Services"><EmptyState><Server size={23} aria-hidden="true" /><strong>No services found.</strong><span>The loaded dataset contains no service records.</span></EmptyState></Card> : <>
        <section className="service-topology-layout" aria-label="Observed service relationship map">
          <Card className="service-map-card" title="Observed service map" sub="Nodes and request-trail edges are returned from the current dataset. Relationships do not verify deployed infrastructure." actions={<span className="service-map-count"><Network size={14} aria-hidden="true" />{topologyNodes.length} services · {dependencies.data?.edgeCount ?? 0} observed edges</span>}>
            {dependencies.loading ? <Spinner label="Loading service relationships" /> : dependencies.error ? <ErrorBox error={dependencies.error} retry={dependencies.reload} /> : dependencies.data ? <TopologyPanel nodes={topologyNodes} edges={dependencies.data.edges} mode={mode} onModeChange={setMode} selectedId={selectedName} onSelect={setSelectedName} description="Observed service adjacency from log request trails. Node size represents dataset events; edge weight represents observed adjacency." /> : <EmptyState>No relationship graph was returned.</EmptyState>}
          </Card>
          <aside className="service-inspector" aria-live="polite">
            {selectedService ? <><div className="service-inspector-kicker">SELECTED SERVICE</div><div className="service-inspector-title"><span className="service-inspector-pulse" /><h2>{selectedService.name}</h2></div><span className={`service-health-tag service-health-tag--${serviceTone(selectedService)}`}>{serviceTone(selectedService) === 'good' ? 'Healthy signal' : serviceTone(selectedService) === 'warn' ? 'Warnings observed' : 'Errors observed'}</span><div className="service-inspector-metrics"><div><span>Events</span><strong>{formatNumber(selectedService.events)}</strong></div><div><span>Errors</span><strong>{formatNumber(selectedService.errors)}</strong></div><div><span>Error rate</span><strong>{selectedService.eventRate.toFixed(2)}%</strong></div><div><span>Hosts</span><strong>{formatNumber(selectedService.hosts)}</strong></div></div><p className="service-inspector-note">Service rollup values are returned for the currently loaded source.</p><div className="service-inspector-actions"><Link to={`/services/${encodeURIComponent(selectedService.name)}`}>Open details <ExternalLink size={14} aria-hidden="true" /></Link><Link to={`/logs?q=${encodeURIComponent(`service:${selectedService.name}`)}`}>Filter logs <ExternalLink size={14} aria-hidden="true" /></Link></div></> : <div className="service-inspector-empty"><span className="service-inspector-glyph"><Network size={21} aria-hidden="true" /></span><span className="eyebrow">SERVICE INSPECTOR</span><h2>Select a node</h2><p>Choose a service in the map or fleet to inspect its activity and open the related logs.</p></div>}
          </aside>
        </section>
        <div className="stat-grid stat-grid--small">
          <StatCard label="Services" value={formatNumber(list.length)} color="var(--accent)" />
          <StatCard label="Events represented" value={formatNumber(events)} />
          <StatCard label="Errors" value={formatNumber(errors)} color="var(--severity-error)" />
          <StatCard label="Warnings" value={formatNumber(warnings)} color="var(--severity-warn)" />
          <StatCard label="Service-host pairs" value={formatNumber(hosts)} color="var(--mod-dp)" />
        </div>
        <Card title="Service fleet" sub="Counts and rates returned by the backend" actions={refreshing ? <Badge tone="info">Refreshing</Badge> : undefined}>
          <div className="table-scroll"><table className="log-table"><caption className="sr-only">Service fleet analytics</caption><thead><tr><th>Service</th><th>Events</th><th>Errors</th><th>Warnings</th><th>Error rate</th><th>Hosts</th><th>Latest event</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{list.map((service) => <tr key={service.name}><td><Link to={`/services/${encodeURIComponent(service.name)}`}><strong>{service.name}</strong></Link></td><td className="num">{formatNumber(service.events)}</td><td className="num" style={{ color: service.errors > 0 ? 'var(--severity-error)' : undefined }}>{formatNumber(service.errors)}</td><td className="num">{formatNumber(service.warnings)}</td><td className="num"><Badge tone={serviceTone(service)}>{service.eventRate.toFixed(2)}%</Badge></td><td className="num">{formatNumber(service.hosts)}</td><td className="ts">{service.latestAt ? formatTs(service.latestAt) : '—'}</td><td><Link className="btn btn-sm" to={`/logs?q=${encodeURIComponent(`service:${service.name}`)}`}><ExternalLink size={13} aria-hidden="true" /> Events</Link></td></tr>)}</tbody></table></div>
        </Card>
      </>}
    </div>
  );
}

function ServiceDetailView({ name }: { name: string }) {
  const [drawerEvent, setDrawerEvent] = useState<LogEvent | null>(null);
  const { data, loading, refreshing, error, reload } = useApi<ServiceDetail>((signal) => api.serviceDetail(name, 50, { signal }), name);

  return (
    <div className="page">
      <PageHeader eyebrow="Service detail" title={name} description={data ? <>{formatNumber(data.summary.events)} events across {formatNumber(data.summary.hosts)} hosts.</> : 'Service rollup and recent event evidence returned by the backend.'} actions={<><button className="btn btn-sm" type="button" onClick={reload} disabled={refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh</button><Link className="btn btn-sm" to="/services"><ChevronLeft size={14} aria-hidden="true" /> Services</Link><Link className="btn btn-sm" to={`/logs?q=${encodeURIComponent(`service:${name}`)}`}><ExternalLink size={13} aria-hidden="true" /> Events</Link></>} />
      {loading ? <Card title="Loading service"><Spinner label={`Requesting ${name} rollup…`} /></Card> : error ? isMissingDataset(error) ? <NoDatasetState detail="Load a dataset before opening a service detail view." /> : <ErrorBox error={error} retry={reload} /> : !data ? <Card title="Service detail"><EmptyState>Service data is not available for this link.</EmptyState></Card> : <>
        <div className="stat-grid">
          <StatCard label="Events" value={formatNumber(data.summary.events)} color="var(--accent)" />
          <StatCard label="Errors" value={formatNumber(data.summary.errors)} color="var(--severity-error)" />
          <StatCard label="Warnings" value={formatNumber(data.summary.warnings)} color="var(--severity-warn)" />
          <StatCard label="Hosts" value={formatNumber(data.summary.hosts)} color="var(--mod-dp)" />
          <StatCard label="Error rate" value={`${data.summary.eventRate.toFixed(2)}%`} color={data.summary.errors > 0 ? 'var(--danger)' : 'var(--ok)'} />
          <StatCard label="Events represented" value={formatNumber(data.totalEvents)} />
        </div>
        <div className="grid-2">
          <Card title="24 hour activity" sub="Backend time buckets"><TimeChart data={data.activity.map((point) => ({ label: point.start, value: point.count }))} height={180} label={`Observed activity for ${name}`} /></Card>
          <Card title="Severity" sub="Observed levels"><div className="table-scroll"><table className="info-table"><caption className="sr-only">Service severity distribution</caption><thead><tr><th>Level</th><th>Events</th><th>Share</th></tr></thead><tbody>{Object.entries(data.summary.severity).map(([level, value]) => <tr key={level}><td><LevelBadge level={level} /></td><td className="num">{formatNumber(value)}</td><td className="num">{data.summary.events > 0 ? `${((value / data.summary.events) * 100).toFixed(2)}%` : '—'}</td></tr>)}</tbody></table></div></Card>
        </div>
        <Card title="Recent events" sub={`${formatNumber(data.recentEvents.length)} shown · ${formatNumber(data.totalEvents)} total for ${data.summary.name}`} actions={<Link className="btn btn-sm" to={`/logs?q=${encodeURIComponent(`service:${data.summary.name}`)}`}>Open filtered explorer <ExternalLink size={13} aria-hidden="true" /></Link>}>
          {data.recentEvents.length === 0 ? <EmptyState>No events are recorded for this service.</EmptyState> : <div className="table-scroll"><table className="log-table"><caption className="sr-only">Recent events for service</caption><thead><tr><th>Time</th><th>Level</th><th>Host</th><th>HTTP</th><th>Message</th><th>Latency</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{data.recentEvents.map((event) => <tr key={event.id}><td className="ts">{formatTs(event.timestamp)}</td><td><LevelBadge level={event.level} /></td><td className="muted">{event.host}</td><td>{event.httpMethod ? <span className="http-pill">{event.httpMethod} {event.statusCode}</span> : '—'}</td><td><Link className="log-cell" to={`/logs/${event.id}`}><span className="log-cell-msg">{event.message}</span></Link></td><td className="num">{event.responseTime > 0 ? formatMillis(event.responseTime) : '—'}</td><td><button className="icon-btn" type="button" onClick={() => setDrawerEvent(event)} aria-label={`Inspect event ${event.id}`} title="Inspect event"><ExternalLink size={14} aria-hidden="true" /></button></td></tr>)}</tbody></table></div>}
        </Card>
        <div className="quick-actions"><Link className="btn btn-sm" to={`/search?q=${encodeURIComponent(`service:${data.summary.name}`)}`}>Search service events</Link></div>
      </>}
      <EventDrawer event={drawerEvent} onClose={() => setDrawerEvent(null)} />
    </div>
  );
}
