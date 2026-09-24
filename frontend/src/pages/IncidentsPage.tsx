import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { IncidentDetail, IncidentDto } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState, LevelBadge, Badge } from '../components/ui';
import { formatNumber, formatNanos, formatTs } from '../components/format';

/**
 * Incident investigation (docs/API.md §6). Detection is the documented heuristic: 5-minute
 * elevated-error windows against the dataset's own baseline are merged into incidents. Every
 * incident exposes its supporting logs so a claim can be verified — nothing is fabricated.
 */
export default function IncidentsPage() {
  const [selected, setSelected] = useState<IncidentDto | null>(null);
  const incidents = useApi<IncidentDto[]>(() => api.incidents(20));
  const detail = useApi<IncidentDetail | null>(
    () => selected ? api.incidentDetail(selected.id, 100) : Promise.resolve(null),
    selected?.id ? String(selected.id) : '',
  );

  if (incidents.loading) return <Spinner label="Detecting incidents…" />;
  if (incidents.error) return <ErrorBox error={incidents.error} retry={incidents.reload} />;

  const list = incidents.data;
  if (!list) return <EmptyState>No dataset loaded.</EmptyState>;
  if (list.length === 0) {
    return (
      <div className="page">
        <h2 className="page-title">Incidents</h2>
        <EmptyState>
          <strong>No elevated-error incidents detected.</strong>
          <span>The heuristic threshold is derived from this dataset's own baseline.</span>
        </EmptyState>
      </div>
    );
  }

  return (
    <div className="page">
      <h2 className="page-title">Incidents</h2>

      <div className="incident-layout">
        <Card title={`Detected incidents (${list.length})`} sub="heuristic 5-minute elevated-error windows">
          <div className="incident-list">
            {list.map((inc) => (
              <button
                key={inc.id}
                className={`incident-item${selected?.id === inc.id ? ' incident-item--active' : ''}`}
                onClick={() => setSelected(inc)}
              >
                <div className="incident-item-top">
                  <span className="incident-id">#{inc.id}</span>
                  <span className="incident-status">{inc.status}</span>
                  <span className="incident-count">{formatNumber(inc.eventCount)} events</span>
                </div>
                <div className="incident-item-time">{formatTs(inc.start)} → {formatTs(inc.end)}</div>
                <div className="incident-item-services">{inc.services.join(', ')}</div>
                <div className="incident-item-pattern">⇢ {inc.primaryPattern}</div>
              </button>
            ))}
          </div>
        </Card>

        {selected && (
          <Card title={`Incident #${selected.id}`} sub={selected.method}>
            {detail.loading ? (
              <Spinner label="Loading incident evidence…" />
            ) : detail.error ? (
              <ErrorBox error={detail.error} retry={detail.reload} />
            ) : detail.data ? (
              <>
                <div className="incident-summary">
                  <span>Window: {formatTs(detail.data.incident.start)} → {formatTs(detail.data.incident.end)}</span>
                  <span>Services: {detail.data.incident.services.join(', ')}</span>
                  <span>Total evidence: {formatNumber(detail.data.total)}</span>
                </div>
                <div className="log-list">
                  {detail.data.events.map((e) => (
                    <Link key={e.id} className="log-item" to={`/logs/${e.id}`}>
                      <div className="log-item-top">
                        <LevelBadge level={e.level} />
                        <span className="log-item-service">{e.service}</span>
                        <span className="log-item-host">{e.host}</span>
                        <span className="log-item-time">{formatTs(e.timestamp)}</span>
                      </div>
                      <div className="log-item-msg">{e.message}</div>
                      <div className="log-item-meta">
                        {e.httpMethod && <Badge>{e.httpMethod}</Badge>}
                        {e.statusCode > 0 && <Badge>{e.statusCode}</Badge>}
                        {e.responseTime > 0 && <span>{formatNanos(e.responseTime)}</span>}
                      </div>
                    </Link>
                  ))}
                </div>
              </>
            ) : (
              <EmptyState>No evidence.</EmptyState>
            )}
          </Card>
        )}
      </div>
    </div>
  );
}