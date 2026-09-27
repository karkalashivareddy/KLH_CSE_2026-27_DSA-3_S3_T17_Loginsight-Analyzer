import { useMemo, useState } from 'react';
import { Activity, Cpu, Pause, Play, Radio, Square, TrendingUp, Zap } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Badge, Card, EmptyState, PageHeader, StatCard, StatusPill, TimeChart } from '../components/ui';
import TopologyPanel, { type TopologyMode } from '../components/TopologyPanel';
import { useTelemetry } from '../telemetry/TelemetryContext';
import {
  errorRateSeries,
  formatMicros,
  latencySeries,
  severityTone,
  measuredBand,
  summarize,
  throughputSeries,
  toTopology,
  uniqueAlgorithms
} from '../telemetry/adapters';
import { formatNumber } from '../components/format';

export default function MonitorPage() {
  const {
    state,
    statusLabel,
    error,
    isRunning,
    scenario,
    scenarioId,
    scenarios,
    seed,
    speed,
    sessionId,
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
    start,
    stop,
    restart
  } = useTelemetry();

  const [mode, setMode] = useState<TopologyMode>('2d');
  const [selectedService, setSelectedService] = useState<string | null>(null);

  const { nodes, edges } = useMemo(() => toTopology(topology, health), [topology, health]);
  const totals = useMemo(() => summarize(health), [health]);
  const errors = useMemo(() => errorRateSeries(samples), [samples]);
  const latencies = useMemo(() => latencySeries(samples), [samples]);
  const throughput = useMemo(() => throughputSeries(samples), [samples]);
  const algorithms = useMemo(() => uniqueAlgorithms(evidence), [evidence]);
  const openIncident = incidents.find((incident) => incident.open) ?? null;

  return (
    <div className="page experience-page live-page">
      <PageHeader
        eyebrow="OBSERVE / SIMULATION"
        title="Live Monitor"
        description="Deterministic generated traffic with measured error rate, latency, throughput, dependency health and detected incidents."
        actions={
          <div className="page-actions">
            {isRunning ? (
              <button className="btn btn-sm" type="button" onClick={stop}>
                <Square size={14} aria-hidden="true" /> Stop
              </button>
            ) : (
              <button className="btn btn-primary btn-sm" type="button" onClick={() => start()} disabled={scenarios.length === 0}>
                <Play size={14} aria-hidden="true" /> Start stream
              </button>
            )}
            <button className="btn btn-sm" type="button" onClick={restart} disabled={scenarios.length === 0}>
              <Radio size={14} aria-hidden="true" /> Replay
            </button>
          </div>
        }
      />

      <section className={`live-hero live-hero--${state}`} data-state={state}>
        <div className="live-hero-grid" aria-hidden="true"><span /><span /><span /><span /><span /></div>
        <div className="live-hero-copy">
          <div className="live-label">
            <span className={isRunning ? 'live-indicator live-indicator--on' : 'live-indicator'} />
            <Badge tone="info" label="Deterministic generated traffic, not captured production telemetry">GENERATED</Badge>
            <span className="live-state-name">{isRunning ? 'STREAMING' : state.toUpperCase()}</span>
          </div>
          <h2>
            {isRunning
              ? `${scenario?.title ?? 'Simulation'} is ${frame?.phase ?? 'starting'}, measured ${measuredBand(frame)}.`
              : frame
                ? `Preview frame loaded. Measured ${measuredBand(frame)}; start a run to watch the incident form.`
                : 'Start a deterministic run to watch the incident form.'}
          </h2>
          <p>
            {scenario ? `${scenario.title} · seed ${seed ?? scenario.seed}` : 'No scenario selected.'}
            {sessionId ? ` · session ${sessionId.slice(0, 8)}` : ''}
          </p>
        </div>
        <div className="live-hero-status" role="status" aria-live="polite">
          <div><Activity size={16} aria-hidden="true" /><span>Channel</span><strong>{statusLabel}</strong></div>
          <div><Zap size={16} aria-hidden="true" /><span>Tick</span><strong>{frame?.tick ?? 0}</strong></div>
          <div><TrendingUp size={16} aria-hidden="true" /><span>Open incidents</span><strong>{openIncidentCount}</strong></div>
        </div>
      </section>

      {error ? (
        <div className="page">
          <Card title="Stream error">
            <p className="inline-note">{error.message}</p>
            <button className="btn btn-sm" type="button" onClick={() => start()}><Pause size={14} aria-hidden="true" /> Retry</button>
          </Card>
        </div>
      ) : null}

      <div className="page">
        <div className="control-row">
          <label className="control">
            <span className="control-label">Scenario</span>
            <select
              className="control-input"
              value={scenarioId ?? ''}
              onChange={(event) => selectScenario(event.target.value)}
              disabled={isRunning}
            >
              {scenarios.map((item) => (
                <option key={item.id} value={item.id}>{item.title}</option>
              ))}
            </select>
          </label>
          <label className="control">
            <span className="control-label">Delivery speed</span>
            <select
              className="control-input"
              value={speed}
              onChange={(event) => setSpeed(Number(event.target.value))}
              disabled={isRunning}
            >
              {[0.25, 0.5, 1, 2, 4, 8].map((option) => (
                <option key={option} value={option}>{option}×</option>
              ))}
            </select>
          </label>
          <p className="control-note">
            <Cpu size={14} aria-hidden="true" /> Content is fixed by scenario and seed; only delivery pace changes.
            Need to change the failure? Use <Link to="/scenario-lab">Scenario Lab</Link>.
          </p>
        </div>
      </div>

      {frame ? (
        <>
          <div className="page">
            <div className="stat-grid">
              <StatCard label="Error rate" value={`${frame.errorRate.toFixed(2)}%`} sub={`baseline p95 ${Math.round(frame.baselineP95LatencyMs)} ms`} color="var(--danger)" />
              <StatCard label="p95 latency" value={`${formatNumber(Math.round(frame.p95LatencyMs))} ms`} sub={`avg ${formatNumber(Math.round(frame.averageLatencyMs))} ms`} color="var(--warn)" />
              <StatCard label="Throughput" value={`${formatNumber(frame.eventsPerSecond)}/s`} sub={`${formatNumber(frame.totalEvents)} events in window`} color="var(--accent)" />
              <StatCard label="Fleet health" value={`${totals.healthy}/${health.length}`} sub={`${totals.degraded} degraded · ${totals.critical} critical`} color="var(--info)" />
            </div>
          </div>

          <div className="page grid-2">
            <Card title="Error rate" sub="Live rolling-window measurement.">
              <TimeChart data={errors} label="Error rate percent" valueLabel="% errors" />
            </Card>
            <Card title="p95 latency" sub="Live rolling-window measurement.">
              <TimeChart data={latencies} label="p95 latency milliseconds" valueLabel="ms" />
            </Card>
          </div>

          <div className="page">
            <Card title="Throughput" sub="Events per second, derived from each delivered frame.">
              <TimeChart data={throughput} label="Events per second" valueLabel="events/s" />
            </Card>
          </div>

          <div className="page">
            <Card
              title="Live dependency topology"
              sub="Declared structure with the current window health. Switch between the accessible 2D map and the real 3D view."
            >
              <TopologyPanel
                nodes={nodes}
                edges={edges}
                graphKind="declared"
                mode={mode}
                onModeChange={setMode}
                selectedId={selectedService}
                onSelect={setSelectedService}
                incidentServiceIds={openIncident ? openIncident.blastRadius : []}
                incidentLabel={openIncident ? `Blast radius of ${openIncident.title}` : undefined}
              />
            </Card>
          </div>

          <div className="page grid-2">
            <Card title="Signals" sub="Thresholds crossed in the latest frame.">
              {signals.length === 0 ? (
                <EmptyState icon={false}>No signal is above its threshold right now.</EmptyState>
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
            <Card title="Algorithm evidence" sub={`${algorithms.length} algorithms actually executed for this frame.`}>
              {evidence.length === 0 ? (
                <EmptyState icon={false}>No algorithm evidence in this frame.</EmptyState>
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
                      <span className="evidence-item-input">{formatNumber(item.inputSize)} {item.inputUnit} → {item.result}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>

          <div className="page">
            <Card
              title="Event stream"
              sub={`${events.length} generated events held in the client buffer.`}
              actions={<Badge tone="neutral">generated · not captured</Badge>}
            >
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
                        <th scope="col">Status</th>
                        <th scope="col">Message</th>
                      </tr>
                    </thead>
                    <tbody>
                      {events.slice(0, 60).map((event) => (
                        <tr key={event.id}>
                          <td className="mono">{new Date(event.timestamp).toLocaleTimeString()}</td>
                          <td><Badge tone={event.level === 'ERROR' ? 'danger' : event.level === 'WARN' ? 'warn' : 'info'}>{event.level}</Badge></td>
                          <td>{event.service}</td>
                          <td className="mono">{event.statusCode}</td>
                          <td className="table-message">{event.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>
          </div>
        </>
      ) : (
        <div className="page">
          <Card title="Waiting for the first frame">
            <EmptyState>Start the stream to receive deterministic frames, measured windows and detected signals.</EmptyState>
          </Card>
        </div>
      )}

      <div className="page">
        <Card title="Incidents in this run" sub="Candidates are opened by measured thresholds, never by hand.">
          {incidents.length === 0 ? (
            <EmptyState icon={false}>No incident candidate yet.</EmptyState>
          ) : (
            <div className="incident-grid">
              {incidents.map((incident) => (
                <article key={incident.id} className={`incident-card${incident.open ? ' is-open' : ''}`}>
                  <header className="incident-card-head">
                    <span className="incident-card-title">{incident.title}</span>
                    <StatusPill status={incident.status} />
                  </header>
                  <p className="incident-card-signal">{incident.signal} · {incident.method}</p>
                  <p className="incident-card-signal">
                    {incident.errorRate.toFixed(2)}% errors · {Math.round(incident.p95LatencyMs)} ms p95 ·{' '}
                    {formatNumber(incident.eventCount)} events
                  </p>
                </article>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
