import { useMemo, useState } from 'react';
import { Activity, Cpu, FlaskConical, Pause, Play, Radio, RotateCcw, Shuffle, Square } from 'lucide-react';
import { PageHeader, Card, StatCard, Badge, EmptyState, ErrorBox, Spinner, SectionTitle, StatusPill, BarChart, TimeChart } from '../components/ui';
import TopologyPanel, { type TopologyMode } from '../components/TopologyPanel';
import { useTelemetry } from '../telemetry/TelemetryContext';
import {
  blastRadiusServices,
  errorRateSeries,
  formatMicros,
  healthTone,
  measuredBand,
  latencySeries,
  severityTone,
  sortIncidents,
  summarize,
  throughputSeries,
  toTopology,
  totalMeasuredRuntimeMicros,
  uniqueAlgorithms
} from '../telemetry/adapters';
import { eventKey, formatNumber } from '../components/format';

const SPEED_OPTIONS = [0.25, 0.5, 1, 2, 4, 8];

const MEASURED_TONE: Record<string, 'good' | 'warn' | 'danger' | 'neutral'> = {
  healthy: 'good',
  watch: 'warn',
  elevated: 'danger',
  unknown: 'neutral'
};

function formatSeconds(value: number): string {
  if (!Number.isFinite(value)) return 'unavailable';
  if (value >= 60) {
    const minutes = Math.floor(value / 60);
    const seconds = Math.round(value % 60);
    return `${minutes}m ${seconds}s`;
  }
  return `${value}s`;
}

export default function ScenarioLabPage() {
  const {
    state,
    statusLabel,
    error,
    isRunning,
    scenarios,
    scenariosLoading,
    scenariosError,
    simulationStatus,
    scenario,
    scenarioId,
    seed,
    speed,
    startEvent,
    sessionId,
    completeEvent,
    frame,
    samples,
    events,
    signals,
    evidence,
    health,
    incidents,
    topology,
    openIncidentCount,
    selectScenario,
    setSpeed,
    loadPreview,
    start,
    stop,
    restart,
    reseed,
    advanceIncident,
    setIncidentStatus,
    reloadScenarios
  } = useTelemetry();

  const [mode, setMode] = useState<TopologyMode>('2d');
  const [selectedService, setSelectedService] = useState<string | null>(null);

  const { nodes, edges } = useMemo(() => toTopology(topology, health), [topology, health]);
  const totals = useMemo(() => summarize(health), [health]);
  const orderedIncidents = useMemo(() => sortIncidents(incidents), [incidents]);
  const errorSeries = useMemo(() => errorRateSeries(samples), [samples]);
  const latencyChart = useMemo(() => latencySeries(samples), [samples]);
  const throughputChart = useMemo(() => throughputSeries(samples), [samples]);
  const algorithms = useMemo(() => uniqueAlgorithms(evidence), [evidence]);
  const measuredRuntime = useMemo(() => totalMeasuredRuntimeMicros(evidence), [evidence]);
  const serviceBars = useMemo(
    () =>
      [...health]
        .sort((left, right) => right.events - left.events)
        .slice(0, 8)
        .map((service) => ({ label: service.label, value: service.events })),
    [health]
  );

  const elapsedSeconds = frame ? frame.tick * 0.25 : 0;
  const progress = scenario ? Math.min(100, (elapsedSeconds / Math.max(1, scenario.durationSeconds)) * 100) : 0;
  const phaseLabel = frame?.phase ?? (isRunning ? 'starting' : 'idle');

  return (
    <div className="page">
      <PageHeader
        eyebrow="Deterministic simulation"
        title="Scenario Lab"
        description="Drive reproducible incident scenarios on the server, then inspect exactly which algorithms produced the evidence."
        actions={
          <div className="page-actions">
            {isRunning ? (
              <button type="button" className="btn" onClick={stop}>
                <Square size={15} aria-hidden="true" /> Stop
              </button>
            ) : (
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => start()}
                disabled={scenariosLoading || scenarios.length === 0}
              >
                <Play size={15} aria-hidden="true" /> {state === 'complete' || state === 'stopped' ? 'Run again' : 'Start run'}
              </button>
            )}
            <button type="button" className="btn" onClick={loadPreview} disabled={isRunning || !scenario}>
              <Activity size={15} aria-hidden="true" /> Preview peak
            </button>
            <button type="button" className="btn" onClick={restart} disabled={scenariosLoading || scenarios.length === 0}>
              <RotateCcw size={15} aria-hidden="true" /> Replay
            </button>
            <button type="button" className="btn" onClick={reseed} disabled={isRunning}>
              <Shuffle size={15} aria-hidden="true" /> New seed
            </button>
          </div>
        }
      />

      {scenariosError ? <ErrorBox error={scenariosError} retry={reloadScenarios} /> : null}
      {error && state === 'error' ? <ErrorBox error={error} retry={() => start()} /> : null}

      <div className="status-strip">
        <div className="status-strip-item">
          <span className="status-strip-label">Stream</span>
          <StatusPill status={statusLabel} />
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Session</span>
          <span className="mono">{sessionId ? sessionId.slice(0, 8) : 'Ã¢â‚¬â€'}</span>
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Seed</span>
          <span className="mono">{seed ?? scenario?.seed ?? 'Ã¢â‚¬â€'}</span>
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Tick</span>
          <span className="mono">{frame?.tick ?? 0}</span>
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Measured health</span>
          <Badge tone={MEASURED_TONE[measuredBand(frame)] ?? 'neutral'}>{measuredBand(frame)}</Badge>
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Scenario phase</span>
          <Badge tone={frame && frame.phase !== 'healthy' ? 'warn' : 'good'}>{phaseLabel}</Badge>
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Open incidents</span>
          <span className="mono">{openIncidentCount}</span>
        </div>
      </div>

      <Card title="Scenario" sub="Seven curated failure modes with a fixed progression.">
        {scenariosLoading ? <Spinner label="Loading scenarios" /> : null}
        {!scenariosLoading && scenarios.length === 0 ? (
          <EmptyState>No scenarios were returned by the backend.</EmptyState>
        ) : null}
        {scenarios.length > 0 ? (
          <div className="scenario-grid">
            {scenarios.map((item) => {
              const active = item.id === scenarioId;
              return (
                <button
                  key={item.id}
                  type="button"
                  className={`scenario-card${active ? ' is-active' : ''}`}
                  onClick={() => selectScenario(item.id)}
                  aria-pressed={active}
                >
                  <span className="scenario-card-head">
                    <span className="scenario-card-title">{item.title}</span>
                    <Badge tone={severityTone(item.severity)}>{item.severity}</Badge>
                  </span>
                  <span className="scenario-card-summary">{item.summary}</span>
                  <span className="scenario-card-meta">
                    <span>{formatSeconds(item.durationSeconds)} run</span>
                    <span>seed {item.seed}</span>
                    <span>{item.affectedServices.length} services</span>
                  </span>
                  <span className="scenario-card-signatures">
                    {item.errorSignatures.slice(0, 3).map((signature) => (
                      <code key={signature}>{signature}</code>
                    ))}
                  </span>
                </button>
              );
            })}
          </div>
        ) : null}
      </Card>

      {scenario ? (
        <Card
          title={scenario.title}
          sub={scenario.summary}
          actions={
            <div className="page-actions">
              <Badge tone="info">{scenario.expectedSignal}</Badge>
              <Badge tone="neutral">session scoped</Badge>
            </div>
          }
        >
          <dl className="definition-grid">
            <div>
              <dt>Expected signal</dt>
              <dd>{scenario.expectedSignal}</dd>
            </div>
            <div>
              <dt>Expected incident</dt>
              <dd>{scenario.expectedIncident}</dd>
            </div>
            <div>
              <dt>Onset</dt>
              <dd>{formatSeconds(scenario.onsetSeconds)}</dd>
            </div>
            <div>
              <dt>Peak</dt>
              <dd>{formatSeconds(scenario.peakSeconds)}</dd>
            </div>
            <div>
              <dt>Recovery</dt>
              <dd>{formatSeconds(scenario.recoverySeconds)}</dd>
            </div>
            <div>
              <dt>Affected services</dt>
              <dd>{scenario.affectedServices.join(', ')}</dd>
            </div>
          </dl>
          <div className="progress-track" role="progressbar" aria-valuenow={Math.round(progress)} aria-valuemin={0} aria-valuemax={100} aria-label="Scenario progression">
            <div className="progress-fill" style={{ width: `${progress}%` }} />
          </div>
          <div className="control-row">
            <label className="control">
              <span className="control-label">Delivery speed</span>
              <select
                className="control-input"
                value={speed}
                onChange={(event) => setSpeed(Number(event.target.value))}
                disabled={isRunning}
              >
                {SPEED_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {option}Ãƒâ€”
                  </option>
                ))}
              </select>
            </label>
            <p className="control-note">
              <Radio size={14} aria-hidden="true" /> Speed changes delivery pace only. Content stays identical for a given scenario and seed.
            </p>
          </div>
          {completeEvent ? (
            <p className="inline-note">
              Run complete Ã‚Â· {formatNumber(completeEvent.frames)} frames emitted over {formatNumber(completeEvent.ticks)} ticks (
              {completeEvent.reason === 'maxFrames' ? 'frame budget reached' : 'scenario finished'}).
            </p>
          ) : null}
        </Card>
      ) : null}

      {frame ? (
        <>
          <div className="stat-grid">
            <StatCard label="Error rate" value={`${frame.errorRate.toFixed(2)}%`} sub={`${formatNumber(frame.totalErrors)} errors in window`} color="rose" />
            <StatCard label="p95 latency" value={`${formatNumber(Math.round(frame.p95LatencyMs))} ms`} sub={`avg ${formatNumber(Math.round(frame.averageLatencyMs))} ms`} color="amber" />
            <StatCard label="Throughput" value={`${formatNumber(frame.eventsPerSecond)}/s`} sub={`${formatNumber(frame.eventsPerFrame)} events this frame`} color="teal" />
            <StatCard label="Window events" value={formatNumber(frame.totalEvents)} sub={`${frame.events.length} streamed in this frame`} color="blue" />
          </div>

          <div className="grid-2">
            <Card title="Error rate over the run" sub="Measured from the live rolling window.">
              <TimeChart data={errorSeries} label="Error rate percent" valueLabel="% errors" />
            </Card>
            <Card title="p95 latency over the run" sub="Same window, latency percentile.">
              <TimeChart data={latencyChart} label="p95 latency milliseconds" valueLabel="ms" />
            </Card>
          </div>

          <div className="grid-2">
            <Card title="Throughput" sub="Events per second derived from this tick.">
              <TimeChart data={throughputChart} label="Events per second" valueLabel="events/s" />
            </Card>
            <Card title="Busiest services" sub="Window event volume per service.">
              <BarChart data={serviceBars} label="Events per service" />
            </Card>
          </div>

          <Card
            title="Dependency topology"
            sub="Declared service dependencies with measured window health. Select a node for detail."
          >
            <TopologyPanel
              nodes={nodes}
              edges={edges}
              graphKind="declared"
              mode={mode}
              onModeChange={setMode}
              selectedId={selectedService}
              onSelect={setSelectedService}
              incidentServiceIds={blastRadiusServices(orderedIncidents.find((incident) => incident.open) ?? orderedIncidents[0] ?? null)}
              incidentLabel="Blast radius of the most severe open incident"
            />
            <div className="fleet-summary">
              <Badge tone="good">{totals.healthy} healthy</Badge>
              <Badge tone="warn">{totals.degraded} degraded</Badge>
              <Badge tone="danger">{totals.critical} critical</Badge>
              <Badge tone="neutral">peak load {Math.round(totals.maxLoad * 100)}%</Badge>
            </div>
            <div className="health-strip">
              {health.map((service) => (
                <button
                  key={service.service}
                  type="button"
                  className={`health-chip health-${service.state}${selectedService === service.service ? ' is-selected' : ''}`}
                  onClick={() => setSelectedService(selectedService === service.service ? null : service.service)}
                >
                  <span className="health-chip-name">
                    {service.label}
                    <Badge tone={healthTone(service.state)}>{service.state}</Badge>
                  </span>
                  <span className="health-chip-metrics">
                    {service.errorRate.toFixed(1)}% Ã‚Â· {Math.round(service.averageLatencyMs)} ms Ã‚Â· load {Math.round(service.load)}
                  </span>
                  {service.inBlastRadius ? <span className="health-chip-flag">blast radius</span> : null}
                </button>
              ))}
            </div>
          </Card>

          <div className="grid-2">
            <Card title="Detected signals" sub="Measured signals raised during this tick.">
              {signals.length === 0 ? (
                <EmptyState icon={false}>No signal crossed its threshold in this tick.</EmptyState>
              ) : (
                <ul className="signal-list">
                  {signals.map((signal) => (
                    <li key={signal.id} className="signal-item">
                      <span className="signal-item-head">
                        <Badge tone={severityTone(signal.severity)}>{signal.severity}</Badge>
                        <span className="signal-item-label">{signal.label}</span>
                        <span className="signal-item-service">{signal.service}</span>
                      </span>
                      <span className="signal-item-detail">{signal.detail}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card
              title="Algorithm evidence"
              sub={`${algorithms.length} distinct algorithms executed on this frame Ã‚Â· ${formatMicros(measuredRuntime)} total measured`}
            >
              {evidence.length === 0 ? (
                <EmptyState icon={false}>No algorithm evidence for this frame yet.</EmptyState>
              ) : (
                <ul className="evidence-list">
                  {evidence.map((item, index) => (
                    <li key={`${item.algorithm}-${index}`} className="evidence-item">
                      <span className="evidence-item-head">
                        <Cpu size={14} aria-hidden="true" />
                        <span className="evidence-item-algorithm">{item.algorithm}</span>
                        <span className="evidence-item-runtime">{formatMicros(item.runtimeMicros)}</span>
                      </span>
                      <span className="evidence-item-purpose">{item.purpose}</span>
                      <span className="evidence-item-input">
                        {formatNumber(item.inputSize)} {item.inputUnit} Ã¢â€ â€™ {item.result}
                      </span>
                      <span className="evidence-item-complexity">{item.complexity}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>

          <Card title="Incidents" sub="Session-scoped candidates with an explicit operator lifecycle.">
            {orderedIncidents.length === 0 ? (
              <EmptyState icon={false}>No incident candidate yet. Detection waits for measured thresholds.</EmptyState>
            ) : (
              <div className="incident-grid">
                {orderedIncidents.map((incident) => (
                  <article key={incident.id} className={`incident-card${incident.open ? ' is-open' : ''}`}>
                    <header className="incident-card-head">
                      <span className="incident-card-title">{incident.title}</span>
                      <StatusPill status={incident.status} />
                    </header>
                    <p className="incident-card-signal">{incident.signal}</p>
                    <dl className="incident-card-grid">
                      <div>
                        <dt>Method</dt>
                        <dd>{incident.method}</dd>
                      </div>
                      <div>
                        <dt>Origin</dt>
                        <dd>{incident.originService}</dd>
                      </div>
                      <div>
                        <dt>Error rate</dt>
                        <dd>{incident.errorRate.toFixed(2)}%</dd>
                      </div>
                      <div>
                        <dt>p95</dt>
                        <dd>{Math.round(incident.p95LatencyMs)} ms</dd>
                      </div>
                      <div>
                        <dt>Events</dt>
                        <dd>{formatNumber(incident.eventCount)}</dd>
                      </div>
                      <div>
                        <dt>Blast radius</dt>
                        <dd>{incident.blastRadius.join(', ')}</dd>
                      </div>
                    </dl>
                    {incident.matchedSignatures.length > 0 ? (
                      <p className="incident-card-signatures">
                        {incident.matchedSignatures.map((signature) => (
                          <code key={signature}>{signature}</code>
                        ))}
                      </p>
                    ) : null}
                    <ol className="timeline">
                      {incident.timeline.map((entry) => (
                        <li key={`${entry.status}-${entry.at}`} className="timeline-item">
                          <span className="timeline-dot" aria-hidden="true" />
                          <span className="timeline-status">{entry.label}</span>
                          <span className="timeline-time">{new Date(entry.at).toLocaleTimeString()}</span>
                        </li>
                      ))}
                    </ol>
                    <div className="page-actions">
                      <button type="button" className="btn btn-sm" onClick={() => advanceIncident(incident.id)} disabled={!incident.open || isRunning}>
                        <Pause size={13} aria-hidden="true" /> Advance
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm"
                        onClick={() => setIncidentStatus(incident.id, 'ACKNOWLEDGED')}
                        disabled={!incident.open || incident.status === 'ACKNOWLEDGED'}
                      >
                        Acknowledge
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm"
                        onClick={() => setIncidentStatus(incident.id, 'RESOLVED')}
                        disabled={incident.status === 'RESOLVED'}
                      >
                        Resolve
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </Card>

          <Card title="Streamed events" sub={`${events.length} generated events retained in the client buffer.`} actions={<Badge tone="neutral">generated Ã‚Â· not captured</Badge>}>
            {events.length === 0 ? (
              <EmptyState icon={false}>No events received yet.</EmptyState>
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
                    {events.slice(0, 40).map((event) => (
                      <tr key={eventKey(event)}>
                        <td className="mono">{new Date(event.timestamp).toLocaleTimeString()}</td>
                        <td>
                          <Badge tone={event.level === 'ERROR' ? 'danger' : event.level === 'WARN' ? 'warn' : 'info'}>{event.level}</Badge>
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
        </>
      ) : (
        <Card title="No frame yet">
          <EmptyState>
            Start a run to generate deterministic traffic, then the measured window, signals, evidence and incident lifecycle appear here.
          </EmptyState>
        </Card>
      )}

      <Card title="Runtime" sub="Server-side executor and dataset availability.">
        <div className="definition-grid">
          <div>
            <dt>Active streams</dt>
            <dd>{simulationStatus?.activeSessions ?? 0}</dd>
          </div>
          <div>
            <dt>Max concurrent streams</dt>
            <dd>{simulationStatus?.maxConcurrentStreams ?? 0}</dd>
          </div>
          <div>
            <dt>Delivery speed</dt>
            <dd>{simulationStatus ? `${simulationStatus.speed.min}x Ã¢â‚¬â€œ ${simulationStatus.speed.max}x` : 'Ã¢â‚¬â€'}</dd>
          </div>
          <div>
            <dt>Logical tick</dt>
            <dd>{startEvent ? `${startEvent.tickMillis} ms` : 'Ã¢â‚¬â€'}</dd>
          </div>
          <div>
            <dt>Determinism</dt>
            <dd>{simulationStatus?.determinism ?? 'Ã¢â‚¬â€'}</dd>
          </div>
          <div>
            <dt>Persistence</dt>
            <dd>{simulationStatus?.persistence ?? 'Ã¢â‚¬â€'}</dd>
          </div>
        </div>
        <p className="inline-note">
          <FlaskConical size={14} aria-hidden="true" /> These numbers describe the simulation engine only. Dataset-backed pages keep using their own
          endpoints, and no page mixes the two sources.
        </p>
      </Card>

      <SectionTitle sub="Reproducibility is a contract, not a coincidence.">
        <Activity size={16} aria-hidden="true" /> Determinism model
      </SectionTitle>
      <ol className="pipeline-steps">
        <li>Raw generated event with a deterministic seed stream.</li>
        <li>Rolling window aggregation for error rate, throughput and latency.</li>
        <li>Aho-Corasick scan for every configured error signature.</li>
        <li>KMP confirmation of the strongest signature.</li>
        <li>Signal thresholds open an incident; sustained recovery closes it.</li>
        <li>Evidence carries measured runtime, input size and complexity.</li>
      </ol>
      {startEvent ? (
        <p className="inline-note">
          {startEvent.label} Ã‚Â· session {startEvent.sessionId.slice(0, 8)} Ã‚Â· seed {startEvent.seed} Ã‚Â· {startEvent.paceMs} ms per frame at{' '}
          {startEvent.speed}x.
        </p>
      ) : null}
    </div>
  );
}
