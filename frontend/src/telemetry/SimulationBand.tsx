import { Cpu, FlaskConical, Play, ShieldAlert, Waypoints } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Badge, Card, StatCard, StatusPill } from '../components/ui';
import { useTelemetry } from './TelemetryContext';
import { formatMicros, measuredBand, severityTone, summarize, uniqueAlgorithms } from './adapters';

const MEASURED_TONE: Record<string, 'good' | 'warn' | 'danger' | 'neutral'> = {
  healthy: 'good',
  watch: 'warn',
  elevated: 'danger',
  unknown: 'neutral'
};
import { formatNumber } from '../components/format';

/**
 * Live simulation summary for the overview page.
 *
 * <p>Reads the one shared stream owned by TelemetryProvider, so these are the same numbers the Live
 * Monitor and the incident workbench show. Nothing is computed in the browser.</p>
 */
export function SimulationBand() {
  const { state, statusLabel, isRunning, scenario, frame, health, evidence, incidents, start, openIncidentCount } =
    useTelemetry();

  const totals = summarize(health);
  const algorithms = uniqueAlgorithms(evidence);
  const openIncident = incidents.find((incident) => incident.open) ?? null;
  const measured = evidence.reduce((sum, item) => sum + item.runtimeMicros, 0);

  return (
    <Card
      title="Live simulation"
      sub="Deterministic generated traffic, kept separate from the dataset analysed on this page."
      actions={
        <div className="page-actions">
          {!isRunning ? (
            <button className="btn btn-primary btn-sm" type="button" onClick={() => start()}>
              <Play size={14} aria-hidden="true" /> Start
            </button>
          ) : null}
          <Link className="btn btn-sm" to="/scenario-lab">
            <FlaskConical size={14} aria-hidden="true" /> Scenario Lab
          </Link>
          <Link className="btn btn-sm" to="/live">
            Open monitor
          </Link>
        </div>
      }
    >
      <div className="status-strip">
        <div className="status-strip-item">
          <span className="status-strip-label">Stream</span>
          <StatusPill status={statusLabel} />
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Scenario</span>
          <span>{scenario?.title ?? '-'}</span>
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Seed</span>
          <span className="mono">{frame?.seed ?? scenario?.seed ?? '-'}</span>
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Measured health</span>
          <Badge tone={MEASURED_TONE[measuredBand(frame)] ?? 'neutral'}>{measuredBand(frame)}</Badge>
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Scenario phase</span>
          <Badge tone={frame && frame.phase !== 'healthy' ? 'warn' : 'good'}>{frame?.phase ?? 'idle'}</Badge>
        </div>
        <div className="status-strip-item">
          <span className="status-strip-label">Open incidents</span>
          <span className="mono">{openIncidentCount}</span>
        </div>
      </div>

      {frame ? (
        <>
          <div className="stat-grid">
            <StatCard
              label="Error rate"
              value={`${frame.errorRate.toFixed(2)}%`}
              sub={`${formatNumber(frame.totalErrors)} errors in the window`}
              color="var(--danger)"
            />
            <StatCard
              label="p95 latency"
              value={`${formatNumber(Math.round(frame.p95LatencyMs))} ms`}
              sub={`baseline ${formatNumber(Math.round(frame.baselineP95LatencyMs))} ms`}
              color="var(--warn)"
            />
            <StatCard
              label="Throughput"
              value={`${formatNumber(frame.eventsPerSecond)}/s`}
              sub={`tick ${frame.tick} · ${state === 'complete' ? 'run complete' : 'streaming'}`}
              color="var(--accent)"
            />
            <StatCard
              label="Fleet health"
              value={`${totals.healthy}/${health.length}`}
              sub={`${totals.degraded} degraded · ${totals.critical} critical`}
              color="var(--info)"
            />
          </div>

          <div className="grid-2">
            <Card title="Detected signals" sub="Measured thresholds crossed in the latest frame.">
              {frame.signals.length === 0 ? (
                <p className="inline-note">No signal is above its threshold in this frame.</p>
              ) : (
                <ul className="signal-list">
                  {frame.signals.slice(0, 4).map((signal) => (
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
              sub={`${algorithms.length} algorithms executed for this frame · ${formatMicros(measured)} measured`}
            >
              {evidence.length === 0 ? (
                <p className="inline-note">No algorithm evidence in this frame.</p>
              ) : (
                <ul className="evidence-list">
                  {evidence.slice(0, 4).map((item, index) => (
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
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>

          {openIncident ? (
            <Card
              title={openIncident.title}
              sub={`${openIncident.method} · origin ${openIncident.originService}`}
              actions={<StatusPill status={openIncident.status} />}
            >
              <div className="definition-grid">
                <div>
                  <dt>Error rate</dt>
                  <dd>{openIncident.errorRate.toFixed(2)}%</dd>
                </div>
                <div>
                  <dt>p95 latency</dt>
                  <dd>{Math.round(openIncident.p95LatencyMs)} ms</dd>
                </div>
                <div>
                  <dt>Blast radius</dt>
                  <dd>{openIncident.blastRadius.join(', ')}</dd>
                </div>
                <div>
                  <dt>Signatures</dt>
                  <dd>{openIncident.matchedSignatures.join(', ') || 'none matched'}</dd>
                </div>
              </div>
              <div className="page-actions">
                <Link className="btn btn-sm" to="/incidents/workbench">
                  <Waypoints size={14} aria-hidden="true" /> Investigate
                </Link>
                <Link className="btn btn-sm" to="/incidents">
                  <ShieldAlert size={14} aria-hidden="true" /> Detector windows
                </Link>
              </div>
            </Card>
          ) : null}
        </>
      ) : (
        <p className="inline-note">
          No simulation frame yet. Start a run to measure error rate, latency, throughput and dependency health on
          generated traffic.
        </p>
      )}
    </Card>
  );
}
