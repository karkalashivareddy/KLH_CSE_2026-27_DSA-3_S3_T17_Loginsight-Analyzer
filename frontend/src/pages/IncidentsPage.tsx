import { useEffect, useState } from 'react';
import { ExternalLink, RefreshCw, ShieldAlert } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { IncidentDetail, IncidentDto, LogEvent } from '../api/types';
import { Badge, Card, EmptyState, ErrorBox, EventDrawer, LevelBadge, NoDatasetState, PageHeader, Spinner, StatCard, StatusPill } from '../components/ui';
import { formatMillis, formatNumber, formatTs } from '../components/format';

function isMissingDataset(error: Error): boolean {
  const status = (error as Error & { apiError?: { status?: number } }).apiError?.status;
  return status === 404;
}

export default function IncidentsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const routeId = id === undefined ? null : Number(id);
  const validRouteId = routeId !== null && Number.isSafeInteger(routeId) && routeId >= 0 ? routeId : null;
  const invalidRoute = id !== undefined && validRouteId === null;
  const [selectedId, setSelectedId] = useState<number | null>(validRouteId);
  const [drawerEvent, setDrawerEvent] = useState<LogEvent | null>(null);
  const incidents = useApi<IncidentDto[]>((signal) => api.incidents(20, { signal }));
  const detail = useApi<IncidentDetail | null>((signal) => selectedId === null ? Promise.resolve(null) : api.incidentDetail(selectedId, 100, { signal }), selectedId === null ? '' : String(selectedId));

  useEffect(() => {
    if (validRouteId !== null) setSelectedId(validRouteId);
  }, [validRouteId]);

  const list = incidents.data ?? [];

  useEffect(() => {
    if (invalidRoute || selectedId !== null || list.length === 0) return;
    setSelectedId(list[0].id);
  }, [invalidRoute, list, selectedId]);

  const select = (incident: IncidentDto) => {
    setSelectedId(incident.id);
    navigate(`/incidents/${incident.id}`);
  };

  const evidence = list.reduce((sum, incident) => sum + incident.eventCount, 0);
  const services = new Set(list.flatMap((incident) => incident.services));
  const active = list.filter((incident) => ['ACTIVE', 'OPEN'].includes(incident.status.toUpperCase())).length;
  const latest = list.reduce<string | null>((current, incident) => {
    if (!current) return incident.start;
    return new Date(incident.start).getTime() > new Date(current).getTime() ? incident.start : current;
  }, null);
  const selectedSummary = list.find((incident) => incident.id === selectedId) ?? null;

  if (invalidRoute) return <div className="page"><PageHeader eyebrow="Investigate" title="Invalid incident link" /><EmptyState><strong>Incident identifiers must be non-negative integers.</strong><Link className="btn btn-sm" to="/incidents">Open incidents</Link></EmptyState></div>;

  return (
    <div className="page">
      <PageHeader eyebrow="Investigate" title="Incidents" description="Heuristic elevated-error windows derived from the current dataset baseline." actions={<button className="btn btn-sm" type="button" onClick={incidents.reload} disabled={incidents.refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh</button>} />
      {incidents.loading ? <Card title="Detecting incidents"><Spinner label="Requesting heuristic incident windows…" /></Card> : incidents.error ? isMissingDataset(incidents.error) ? <NoDatasetState detail="Load a dataset before running incident detection." /> : <ErrorBox error={incidents.error} retry={incidents.reload} /> : list.length === 0 ? <Card title="Incidents"><EmptyState><ShieldAlert size={23} aria-hidden="true" /><strong>No elevated-error incidents detected.</strong><span>The detector found no windows above its dataset-derived threshold. This is an empty result, not evidence of system health.</span><Link className="btn btn-sm" to="/logs">Inspect all events</Link></EmptyState></Card> : <>
        <div className="stat-grid stat-grid--small">
          <StatCard label="Returned windows" value={formatNumber(list.length)} color="var(--accent)" />
          <StatCard label="Evidence events" value={formatNumber(evidence)} color="var(--severity-error)" />
          <StatCard label="Affected services" value={formatNumber(services.size)} color="var(--mod-flow)" />
          <StatCard label="Active or open" value={formatNumber(active)} color="var(--warn)" />
          <StatCard label="Latest start" value={latest ? formatTs(latest) : '—'} />
        </div>
        <div className="incident-layout">
          <Card title={`Detected incidents (${list.length})`} sub="Five-minute windows merged by the backend detector" actions={incidents.refreshing ? <Badge tone="info">Refreshing</Badge> : undefined}>
            <div className="incident-list">{list.map((incident) => <button key={incident.id} type="button" className={`incident-item${selectedId === incident.id ? ' incident-item--active' : ''}`} aria-pressed={selectedId === incident.id} onClick={() => select(incident)}><div className="incident-item-top"><span className="incident-id">#{incident.id}</span><StatusPill status={incident.status} /><span className="incident-count">{formatNumber(incident.eventCount)} events</span></div><div className="incident-item-time">{formatTs(incident.start)} → {formatTs(incident.end)}</div><div className="incident-item-services">{incident.services.join(', ')}</div><div className="incident-item-pattern">{incident.primaryPattern}</div></button>)}</div>
          </Card>
          <div>
            {selectedId === null ? <Card title="Incident evidence"><EmptyState>Select a returned window to request its supporting events.</EmptyState></Card> : <Card title={selectedSummary ? `Incident #${selectedSummary.id}` : `Incident #${selectedId}`} sub={detail.data?.incident.method ?? selectedSummary?.method ?? 'Loading evidence'} actions={<Link className="btn btn-sm" to={`/incidents/${selectedId}`}><ExternalLink size={13} aria-hidden="true" /> Deep link</Link>}>
              {detail.loading ? <Spinner label="Loading incident evidence…" /> : detail.error ? isMissingDataset(detail.error) ? <NoDatasetState detail="The incident evidence is no longer available in the current dataset." /> : <ErrorBox error={detail.error} retry={detail.reload} /> : detail.data ? <IncidentDetailPanel detail={detail.data} onInspect={setDrawerEvent} /> : <EmptyState>No evidence is available for this incident.</EmptyState>}
            </Card>}
          </div>
        </div>
      </>}
      <EventDrawer event={drawerEvent} onClose={() => setDrawerEvent(null)} />
    </div>
  );
}

function IncidentDetailPanel({ detail, onInspect }: { detail: IncidentDetail; onInspect: (event: LogEvent) => void }) {
  const incident = detail.incident;
  return <>
    <div className="stat-grid stat-grid--small">
      <StatCard label="Status" value={<StatusPill status={incident.status} />} />
      <StatCard label="Evidence returned" value={`${formatNumber(detail.events.length)} / ${formatNumber(detail.total)}`} />
      <StatCard label="Window start" value={formatTs(incident.start)} />
      <StatCard label="Window end" value={formatTs(incident.end)} />
    </div>
    <div className="incident-summary"><StatusPill status={incident.status} /><span>Services: {incident.services.join(', ')}</span><span>Method: {incident.method}</span></div>
    <Card title="Primary pattern" sub="Returned detector evidence"><code className="log-detail-message">{incident.primaryPattern}</code></Card>
    <Card title="Supporting events" sub={detail.events.length < detail.total ? `Showing ${formatNumber(detail.events.length)} of ${formatNumber(detail.total)} evidence events` : 'Evidence events returned by the detail endpoint'}>
      {detail.events.length === 0 ? <EmptyState>No evidence events were returned.</EmptyState> : <div className="log-list">{detail.events.map((event) => <div className="log-item" key={event.id}><div className="log-item-top"><LevelBadge level={event.level} /><span className="log-item-service">{event.service}</span><span className="log-item-host">{event.host}</span><span className="log-item-time">{formatTs(event.timestamp)}</span></div><Link className="log-item-msg" to={`/logs/${event.id}`}>{event.message}</Link><div className="log-item-meta">{event.httpMethod && <span className="http-pill">{event.httpMethod} {event.statusCode}</span>}{event.responseTime > 0 && <span>{formatMillis(event.responseTime)}</span>}<button className="icon-btn" type="button" onClick={() => onInspect(event)} aria-label={`Inspect event ${event.id}`} title="Inspect event"><ExternalLink size={14} aria-hidden="true" /></button></div></div>)}</div>}
    </Card>
  </>;
}
