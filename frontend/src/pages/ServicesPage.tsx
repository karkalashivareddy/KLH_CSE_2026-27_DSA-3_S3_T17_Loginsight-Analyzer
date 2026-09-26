import { useState } from 'react';
import { ChevronLeft, ExternalLink, RefreshCw, Server } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { LogEvent, ServiceDetail, ServiceStatsDto } from '../api/types';
import { Badge, Card, EmptyState, ErrorBox, EventDrawer, LevelBadge, NoDatasetState, PageHeader, Spinner, StatCard, TimeChart } from '../components/ui';
import { formatMillis, formatNumber, formatTs } from '../components/format';

function isMissingDataset(error: Error): boolean {
  const status = (error as Error & { apiError?: { status?: number } }).apiError?.status;
  return status === 404;
}

function serviceTone(service: ServiceStatsDto): 'good' | 'warn' | 'danger' {
  if (service.errors > 0) return 'danger';
  if (service.warnings > 0) return 'warn';
  return 'good';
}

export default function ServicesPage() {
  const { id } = useParams<{ id?: string }>();
  return id ? <ServiceDetailView name={id} /> : <FleetView />;
}

function FleetView() {
  const { data, loading, refreshing, error, reload } = useApi<ServiceStatsDto[]>((signal) => api.services(50, { signal }));
  const list = data ?? [];
  const events = list.reduce((sum, service) => sum + service.events, 0);
  const errors = list.reduce((sum, service) => sum + service.errors, 0);
  const warnings = list.reduce((sum, service) => sum + service.warnings, 0);
  const hosts = list.reduce((sum, service) => sum + service.hosts, 0);

  return (
    <div className="page">
      <PageHeader eyebrow="Observe" title="Services" description="Per-service rollups computed from the current dataset." actions={<button className="btn btn-sm" type="button" onClick={reload} disabled={refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh</button>} />
      {loading ? <Card title="Loading services"><Spinner label="Requesting service rollups…" /></Card> : error ? isMissingDataset(error) ? <NoDatasetState detail="Load a dataset before requesting the service fleet." /> : <ErrorBox error={error} retry={reload} /> : list.length === 0 ? <Card title="Services"><EmptyState><Server size={23} aria-hidden="true" /><strong>No services found.</strong><span>The loaded dataset contains no service records.</span></EmptyState></Card> : <>
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
