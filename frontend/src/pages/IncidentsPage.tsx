import { useEffect, useState } from 'react';
import { ExternalLink, RefreshCw, ShieldAlert } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import type { IncidentDetail, IncidentDto, LogEvent, ObjectApiResponse } from '../api/types';
import { useApi } from '../hooks/useApi';
import { TopologyPanel, type TopologyMode } from '../components/TopologyPanel';
import { Badge, Card, EmptyState, ErrorBox, EventDrawer, LevelBadge, NoDatasetState, PageHeader, Spinner, StatusPill } from '../components/ui';
import { formatMillis, formatNumber, formatTs } from '../components/format';

function isMissingDataset(error: Error): boolean {
  return (error as Error & { apiError?: { status?: number } }).apiError?.status === 404;
}

export default function IncidentsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const routeId = id === undefined ? null : Number(id);
  const validRouteId = routeId !== null && Number.isSafeInteger(routeId) && routeId >= 0 ? routeId : null;
  const invalidRoute = id !== undefined && validRouteId === null;
  const [selectedId, setSelectedId] = useState<number | null>(validRouteId);
  const [drawerEvent, setDrawerEvent] = useState<LogEvent | null>(null);
  const [topologyMode, setTopologyMode] = useState<TopologyMode>('2d');
  const incidents = useApi<IncidentDto[]>((signal) => api.incidents(20, { signal }));
  const dependencies = useApi<ObjectApiResponse>((signal) => api.dependencies({ signal }), 'incident-service-map');
  const detail = useApi<IncidentDetail | null>((signal) => selectedId === null ? Promise.resolve(null) : api.incidentDetail(selectedId, 100, { signal }), selectedId === null ? '' : String(selectedId));
  useEffect(() => { if (validRouteId !== null) setSelectedId(validRouteId); }, [validRouteId]);

  const list = incidents.data ?? [];
  useEffect(() => {
    if (invalidRoute || selectedId !== null || list.length === 0) return;
    setSelectedId(list[0].id);
  }, [invalidRoute, list, selectedId]);

  const select = (incident: IncidentDto) => { setSelectedId(incident.id); navigate(`/incidents/${incident.id}`); };
  const evidenceCount = list.reduce((sum, incident) => sum + incident.eventCount, 0);
  const serviceCount = new Set(list.flatMap((incident) => incident.services)).size;
  const selectedSummary = list.find((incident) => incident.id === selectedId) ?? null;
  const refresh = () => { incidents.reload(); dependencies.reload(); };

  if (invalidRoute) return <div className="page experience-page"><PageHeader eyebrow="INVESTIGATE" title="Incident not found" /><EmptyState><strong>Incident identifiers must be non-negative integers.</strong><Link className="btn btn-sm" to="/incidents">Open investigations</Link></EmptyState></div>;

  return (
    <div className="page experience-page incidents-page">
      <PageHeader eyebrow="INVESTIGATE / DETECTOR WINDOWS" title="Incident investigations" description="Inspect elevated-error windows, the services named by the detector, and their supporting events." actions={<button className="btn btn-sm" type="button" onClick={refresh} disabled={incidents.refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh</button>} />
      {incidents.loading ? <div className="experience-loading"><div className="skeleton-line skeleton-line--wide" /><div className="skeleton-hero" /></div>
        : incidents.error ? isMissingDataset(incidents.error) ? <NoDatasetState detail="Load a source before requesting heuristic incident windows." /> : <ErrorBox error={incidents.error} retry={refresh} />
          : list.length === 0 ? <Card className="incident-empty"><EmptyState><ShieldAlert size={24} aria-hidden="true" /><strong>No elevated-error windows returned</strong><span>The detector found no windows above its dataset-derived threshold. This empty result is not evidence of system health.</span><Link className="btn btn-primary" to="/logs">Inspect event logs</Link></EmptyState></Card>
            : <>
              <div className="incident-overview-strip"><div><span>DETECTOR WINDOWS</span><strong>{formatNumber(list.length)}</strong></div><div><span>SUPPORTING EVENTS</span><strong>{formatNumber(evidenceCount)}</strong></div><div><span>SERVICES NAMED</span><strong>{formatNumber(serviceCount)}</strong></div><p>Rule-based grouping of ERROR/FATAL volume. This view makes no root-cause or remediation claim.</p></div>
              <div className="incident-workspace">
                <Card className="incident-list-pane" title={`Detected windows · ${list.length}`} sub="Select a five-minute error window to inspect its evidence." actions={incidents.refreshing ? <Badge tone="info">Updating</Badge> : undefined}>
                  <div className="incident-list">{list.map((incident) => <button key={incident.id} type="button" className={`incident-item${selectedId === incident.id ? ' incident-item--active' : ''}`} aria-pressed={selectedId === incident.id} onClick={() => select(incident)}><div className="incident-item-top"><span className="incident-id">#{incident.id}</span><StatusPill status={incident.status} /><span className="incident-count">{formatNumber(incident.eventCount)} events</span></div><div className="incident-item-time">{formatTs(incident.start)} → {formatTs(incident.end)}</div><div className="incident-item-services">{incident.services.join(', ') || 'No service names returned'}</div><div className="incident-item-pattern">{incident.primaryPattern || 'Elevated error volume'}</div></button>)}</div>
                </Card>

                <section className="incident-investigation-pane" aria-label="Selected incident investigation">
                  {selectedId === null ? <Card title="Incident investigation"><EmptyState>Select a detector window to load its supporting events.</EmptyState></Card> : <Card className="incident-investigation-card" title={selectedSummary ? `Incident #${selectedSummary.id}` : `Incident #${selectedId}`} sub={detail.data?.incident.method ?? selectedSummary?.method ?? 'Loading detector evidence'} actions={<Link className="btn btn-sm" to={`/incidents/${selectedId}`}><ExternalLink size={13} aria-hidden="true" /> Investigation link</Link>}>
                    {detail.loading ? <Spinner label="Loading incident evidence…" /> : detail.error ? isMissingDataset(detail.error) ? <NoDatasetState detail="This incident evidence is not available in the selected source." /> : <ErrorBox error={detail.error} retry={detail.reload} /> : detail.data ? <IncidentDetailPanel detail={detail.data} dependencies={dependencies.data} dependenciesLoading={dependencies.loading} dependenciesError={dependencies.error} onRetryDependencies={dependencies.reload} mode={topologyMode} onModeChange={setTopologyMode} onInspect={setDrawerEvent} /> : <EmptyState>No supporting evidence is available for this window.</EmptyState>}
                  </Card>}
                </section>
              </div>
            </>}
      <EventDrawer event={drawerEvent} onClose={() => setDrawerEvent(null)} />
    </div>
  );
}

function IncidentDetailPanel({ detail, dependencies, dependenciesLoading, dependenciesError, onRetryDependencies, mode, onModeChange, onInspect }: { detail: IncidentDetail; dependencies?: ObjectApiResponse | null; dependenciesLoading: boolean; dependenciesError: Error | null; onRetryDependencies: () => void; mode: TopologyMode; onModeChange: (mode: TopologyMode) => void; onInspect: (event: LogEvent) => void }) {
  const incident = detail.incident;
  return <>
    <div className="incident-detail-heading"><StatusPill status={incident.status} /><span className="incident-detail-source">HEURISTIC ERROR WINDOW</span><span className="incident-detail-evidence-count">{formatNumber(detail.total)} events</span></div>
    <div className="incident-time-rail" aria-label="Incident detector window"><div><span className="incident-rail-dot" /><small>WINDOW START</small><strong>{formatTs(incident.start)}</strong></div><span className="incident-rail-line" aria-hidden="true" /><div><span className="incident-rail-dot incident-rail-dot--end" /><small>WINDOW END</small><strong>{formatTs(incident.end)}</strong></div></div>
    <section className="incident-evidence-block"><div className="incident-block-heading"><span className="eyebrow">SIGNATURE</span><h3>Primary pattern</h3></div><code className="incident-pattern-code">{incident.primaryPattern || 'No primary message template returned.'}</code></section>
    <section className="incident-evidence-block"><div className="incident-block-heading"><span className="eyebrow">OBSERVED RELATIONSHIPS</span><h3>Service window context</h3><p>Services named in the detector window are highlighted on the observed request-trail graph. This does not establish upstream causality.</p></div>
      {dependenciesLoading ? <Spinner label="Loading observed service map" /> : dependenciesError ? <ErrorBox error={dependenciesError} retry={onRetryDependencies} /> : dependencies ? <TopologyPanel nodes={dependencies.nodes} edges={dependencies.edges} mode={mode} onModeChange={onModeChange} incidentServiceIds={incident.services} incidentLabel={`Named by incident #${incident.id}`} title="Incident service context" description="Observed request-trail adjacency from the selected source. Highlighted service names were returned in this incident window; edges are not causal claims." /> : <EmptyState>No observed dependency graph is available.</EmptyState>}
    </section>
    <section className="incident-evidence-block"><div className="incident-block-heading"><span className="eyebrow">SUPPORTING LOGS</span><h3>Events in this window</h3><p>{detail.events.length < detail.total ? `Showing ${formatNumber(detail.events.length)} of ${formatNumber(detail.total)} evidence events.` : 'Events returned by the incident detail endpoint.'}</p></div>
      {detail.events.length === 0 ? <EmptyState>No evidence events were returned.</EmptyState> : <div className="incident-event-list">{detail.events.map((event) => <article className="incident-event" key={event.id}><div className="log-item-top"><LevelBadge level={event.level} /><span className="log-item-service">{event.service}</span><span className="log-item-host">{event.host}</span><time className="log-item-time">{formatTs(event.timestamp)}</time></div><Link className="log-item-msg" to={`/logs/${event.id}`}>{event.message}</Link><div className="log-item-meta">{event.httpMethod && <span className="http-pill">{event.httpMethod} {event.statusCode}</span>}{event.responseTime > 0 && <span>{formatMillis(event.responseTime)}</span>}<button className="icon-btn" type="button" onClick={() => onInspect(event)} aria-label={`Inspect event ${event.id}`}><ExternalLink size={14} aria-hidden="true" /></button></div></article>)}</div>}
    </section>
    <p className="incident-method-caveat">The detector groups error volume using a five-minute, dataset-derived threshold. It does not provide root-cause attribution, confidence scores, acknowledgement, or remediation state.</p>
  </>;
}
