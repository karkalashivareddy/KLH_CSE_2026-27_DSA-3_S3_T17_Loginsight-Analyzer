import { useEffect, useMemo, useState } from 'react';
import { Cpu, Network, RefreshCw, ShieldCheck, Square, Waypoints } from 'lucide-react';
import { Badge, Card, EmptyState, PageHeader, StatusPill } from '../components/ui';
import TopologyPanel, { type TopologyMode } from '../components/TopologyPanel';
import { useTelemetry } from '../telemetry/TelemetryContext';
import {
  blastRadiusServices,
  formatMicros,
  healthBand,
  severityTone,
  sortIncidents,
  toTopology
} from '../telemetry/adapters';
import { eventKey, formatNumber } from '../components/format';

const NEXT_STATUS: Record<string, { next: string; label: string; hint: string } | null> = {
  DETECTED: { next: 'INVESTIGATING', label: 'Investigate', hint: 'Start correlating services and events.' },
  INVESTIGATING: { next: 'ACKNOWLEDGED', label: 'Acknowledge', hint: 'An operator owns this incident now.' },
  ACKNOWLEDGED: { next: 'MITIGATED', label: 'Mark mitigated', hint: 'The corrective action is believed to be working.' },
  MITIGATED: { next: 'RESOLVED', label: 'Resolve', hint: 'Measured metrics returned to a healthy level.' },
  RESOLVED: null
};

export default function IncidentWorkbenchPage() {
  const {
    statusLabel,
    isRunning,
    scenario,
    sessionId,
    frame,
    events,
    evidence,
    health,
    incidents,
    topology,
    start,
    stop,
    advanceIncident,
    setIncidentStatus,
    refreshIncidents,
    error
  } = useTelemetry();

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [mode, setMode] = useState<TopologyMode>('2d');

  // The stream carries incidents in every frame, but this surface must survive a reload, a finished
  // stream or a run started on another screen. Ask the server what the session actually holds.
  useEffect(() => {
    if (!sessionId) return;
    void refreshIncidents().catch(() => undefined);
  }, [refreshIncidents, sessionId]);


  const ordered = useMemo(() => sortIncidents(incidents), [incidents]);
  const selected = useMemo(
    () => ordered.find((incident) => incident.id === selectedId) ?? ordered[0] ?? null,
    [ordered, selectedId]
  );

  useEffect(() => {
    if (selected && selectedId !== selected.id) setSelectedId(selected.id);
  }, [selected, selectedId]);

  const { nodes, edges } = useMemo(() => toTopology(topology, health), [topology, health]);

  const relatedEvents = useMemo(() => {
    if (!selected) return [];
    const services = new Set(blastRadiusServices(selected));
    return events.filter((event) => services.has(event.service)).slice(0, 30);
  }, [events, selected]);

  const healthRows = useMemo(
    () => (selected ? health.filter((service) => blastRadiusServices(selected).includes(service.service)) : health),
    [health, selected]
  );

  const action = selected ? NEXT_STATUS[selected.status] ?? null : null;

  return (
    <div className="page experience-page">
      <PageHeader
        eyebrow="INVESTIGATE / SIMULATION"
        title="Incident workbench"
        description="Investigate incidents detected by the deterministic simulation: measured evidence, blast radius, correlated events and an explicit operator lifecycle."
        actions={
          <div className="page-actions">
            {!isRunning ? (
              <button className="btn btn-primary btn-sm" type="button" onClick={() => start()}>
                Start a run
              </button>
            ) : (
              <button className="btn btn-sm" type="button" onClick={() => stop()}>
                <Square size={14} aria-hidden="true" /> Stop
              </button>
            )}
            <button
              className="btn btn-sm"
              type="button"
              onClick={() => {
                setSelectedId(null);
                setMode('2d');
              }}
            >
              <RefreshCw size={14} aria-hidden="true" /> Reset view
            </button>
          </div>
        }
      />

      <div className="page">
        <div className="status-strip">
          <div className="status-strip-item">
            <span className="status-strip-label">Stream</span>
            <StatusPill status={statusLabel} />
          </div>
          <div className="status-strip-item">
            <span className="status-strip-label">Scenario</span>
            <span>{scenario?.title ?? '—'}</span>
          </div>
          <div className="status-strip-item">
            <span className="status-strip-label">Session</span>
            <span className="mono">{sessionId ? sessionId.slice(0, 8) : '—'}</span>
          </div>
          <div className="status-strip-item">
            <span className="status-strip-label">Open</span>
            <span className="mono">{incidents.filter((incident) => incident.open).length}</span>
          </div>
        </div>
      </div>

      {error ? (
        <div className="page">
          <Card title="Stream error">
            <p className="inline-note">{error.message}</p>
          </Card>
        </div>
      ) : null}

      {ordered.length === 0 ? (
        <div className="page">
          <Card title="No incident candidate">
            <EmptyState>
              Nothing has crossed a detection threshold yet. Start a run in <strong>Scenario Lab</strong> with an
              aggressive scenario to see a candidate, its evidence and the lifecycle.
            </EmptyState>
          </Card>
        </div>
      ) : (
        <div className="page workbench">
          <aside className="workbench-list" aria-label="Incident list">
            {ordered.map((incident) => (
              <button
                key={incident.id}
                type="button"
                className={`workbench-item${selected?.id === incident.id ? ' is-selected' : ''}${incident.open ? ' is-open' : ''}`}
                onClick={() => setSelectedId(incident.id)}
                aria-pressed={selected?.id === incident.id}
              >
                <span className="workbench-item-head">
                  <span className="workbench-item-title">{incident.title}</span>
                  <StatusPill status={incident.status} />
                </span>
                <span className="workbench-item-meta">
                  <Badge tone={severityTone(incident.severity)}>{incident.severity}</Badge>
                  <span className="mono">{incident.errorRate.toFixed(1)}%</span>
                  <span className="mono">{Math.round(incident.p95LatencyMs)} ms</span>
                </span>
                <span className="workbench-item-origin">origin {incident.originService}</span>
              </button>
            ))}
          </aside>

          <section className="workbench-detail" aria-label="Incident detail">
            {!selected ? <EmptyState>Select an incident.</EmptyState> : null}
            {selected ? (
              <>
                <Card
                  title={selected.title}
                  sub={`${selected.method} · ${selected.signal}`}
                  actions={<StatusPill status={selected.status} />}
                >
                  <div className="definition-grid">
                    <div><dt>Severity</dt><dd>{selected.severity}</dd></div>
                    <div><dt>Error rate</dt><dd>{selected.errorRate.toFixed(2)}%</dd></div>
                    <div><dt>p95 latency</dt><dd>{Math.round(selected.p95LatencyMs)} ms</dd></div>
                    <div><dt>Events in window</dt><dd>{formatNumber(selected.eventCount)}</dd></div>
                    <div><dt>Origin</dt><dd>{selected.originService}</dd></div>
                    <div><dt>Detected</dt><dd>{new Date(selected.detectedAt).toLocaleTimeString()}</dd></div>
                  </div>

                  <div className="page-actions">
                    {action ? (
                      <button
                        className="btn btn-primary btn-sm"
                        type="button"
                        onClick={() => setIncidentStatus(selected.id, action.next as never)}
                        title={action.hint}
                      >
                        <ShieldCheck size={14} aria-hidden="true" /> {action.label}
                      </button>
                    ) : (
                      <Badge tone="good">Lifecycle complete</Badge>
                    )}
                    <button
                      className="btn btn-sm"
                      type="button"
                      onClick={() => advanceIncident(selected.id)}
                      disabled={!selected.open || isRunning}
                      title="Let the detector move the incident to the next measured state."
                    >
                      Advance automatically
                    </button>
                  </div>
                  <p className="inline-note">
                    {action ? action.hint : 'The incident is resolved and stays in this session history.'}
                    Lifecycle changes are stored for the session only, never in the dataset.
                  </p>
                </Card>

                <Card title="Timeline" sub="Every status change with its timestamp.">
                  <ol className="timeline">
                    {selected.timeline.map((entry) => (
                      <li key={`${entry.status}-${entry.at}`} className="timeline-item">
                        <span className="timeline-dot" aria-hidden="true" />
                        <span className="timeline-status">{entry.label}</span>
                        <span className="timeline-time">{new Date(entry.at).toLocaleTimeString()}</span>
                      </li>
                    ))}
                  </ol>
                </Card>

                <Card
                  title="Algorithm evidence"
                  sub="Produced by the running engines while this incident was open."
                >
                  {selected.evidence.length === 0 ? (
                    <EmptyState icon={false}>No evidence recorded for this incident yet.</EmptyState>
                  ) : (
                    <ul className="evidence-list">
                      {selected.evidence.map((item, index) => (
                        <li key={`${item.algorithm}-${index}`} className="evidence-item">
                          <span className="evidence-item-head">
                            <Cpu size={14} aria-hidden="true" />
                            <span className="evidence-item-algorithm">{item.algorithm}</span>
                            <span className="evidence-item-runtime">{formatMicros(item.runtimeMicros)}</span>
                          </span>
                          <span className="evidence-item-purpose">{item.purpose}</span>
                          <span className="evidence-item-input">
                            {formatNumber(item.inputSize)} {item.inputUnit} → {item.result}
                          </span>
                          <span className="evidence-item-complexity">{item.complexity}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </Card>
              </>
            ) : null}
          </section>

          <aside className="workbench-context" aria-label="Incident context">
            <Card title="Blast radius" sub="Upstream callers derived from the declared dependency graph.">
              {selected ? (
                <>
                  <p className="inline-note">
                    <Waypoints size={14} aria-hidden="true" /> Traversal starts at {selected.originService} and walks
                    callers upstream. It is a graph traversal over declared dependencies, not a learned probability.
                  </p>
                  <div className="health-strip">
                    {healthRows.map((service) => (
                      <div key={service.service} className={`health-chip health-${service.state}`}>
                        <span className="health-chip-name">
                          {service.label}
                          <Badge tone={healthBand(service.state) === 'elevated' ? 'danger' : healthBand(service.state) === 'watch' ? 'warn' : 'good'}>
                            {service.state}
                          </Badge>
                        </span>
                        <span className="health-chip-metrics">
                          {service.errorRate.toFixed(1)}% · {Math.round(service.averageLatencyMs)} ms
                        </span>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <EmptyState icon={false}>Select an incident.</EmptyState>
              )}
            </Card>

            <Card title="Topology" sub="Blast radius highlighted in the dependency graph.">
              <TopologyPanel
                nodes={nodes}
                edges={edges}
                graphKind="declared"
                mode={mode}
                onModeChange={setMode}
                incidentServiceIds={selected ? blastRadiusServices(selected) : []}
                incidentLabel={selected ? `Blast radius of ${selected.title}` : undefined}
              />
            </Card>

            <Card title="Correlated events" sub={`${relatedEvents.length} client-buffered events from the blast radius.`}>
              {relatedEvents.length === 0 ? (
                <EmptyState icon={false}>No buffered events for these services yet.</EmptyState>
              ) : (
                <div className="table-scroll">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th scope="col">Time</th>
                        <th scope="col">Level</th>
                        <th scope="col">Service</th>
                        <th scope="col">Message</th>
                      </tr>
                    </thead>
                    <tbody>
                      {relatedEvents.map((event) => (
                        <tr key={eventKey(event)}>
                          <td className="mono">{new Date(event.timestamp).toLocaleTimeString()}</td>
                          <td>
                            <Badge tone={event.level === 'ERROR' ? 'danger' : event.level === 'WARN' ? 'warn' : 'info'}>
                              {event.level}
                            </Badge>
                          </td>
                          <td>{event.service}</td>
                          <td className="table-message">{event.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>

            <Card title="Current frame evidence" sub="Algorithms executed on the latest delivered frame.">
              {evidence.length === 0 ? (
                <EmptyState icon={false}>No frame evidence yet.</EmptyState>
              ) : (
                <ul className="evidence-list">
                  {evidence.slice(0, 4).map((item, index) => (
                    <li key={`${item.algorithm}-${index}`} className="evidence-item">
                      <span className="evidence-item-head">
                        <Network size={14} aria-hidden="true" />
                        <span className="evidence-item-algorithm">{item.algorithm}</span>
                        <span className="evidence-item-runtime">{formatMicros(item.runtimeMicros)}</span>
                      </span>
                      <span className="evidence-item-input">{item.result}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </aside>
        </div>
      )}

      {frame ? null : (
        <div className="page">
          <Card title="No live frame">
            <EmptyState>The workbench reads the same stream as the Live Monitor, so both always agree.</EmptyState>
          </Card>
        </div>
      )}
    </div>
  );
}
