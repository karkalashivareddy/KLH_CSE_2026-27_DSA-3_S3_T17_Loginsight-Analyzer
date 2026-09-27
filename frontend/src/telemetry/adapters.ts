import type {
  ServiceHealth,
  ServiceHealthState,
  SimulationEvidence,
  SimulationFrame,
  SimulationIncident,
  SimulationTopologyPayload
} from '../api/types';
import type { TopologyEdge, TopologyNode } from '../components/TopologyPanel';
import type { TelemetrySample } from './TelemetryContext';

export type HealthBand = 'healthy' | 'watch' | 'elevated' | 'unknown';

/**
 * Health of the most recent frame, derived only from values the server computed.
 *
 * <p>The scenario {@code phase} describes where the generator is on its intensity curve, so it can
 * read {@code healthy} while the rolling window still holds the tail of a failure. Every rolling
 * aggregate on the frame — window error rate and p95 — lags the same way, which is why a run that
 * has already {@code RESOLVED} its incident can still report a high window rate and a high p95.
 *
 * <p>This badge therefore answers the operator's question — is the system failing right now — from
 * the current tick only: the error share of {@code frame.events}, using the same
 * {@code ERROR || FATAL} rule as the server's detector. Latency is deliberately not folded in here;
 * it is reported in its own KPI tile and as a separate signal, where a lagging p95 is labelled as
 * the rolling window it is. No score, probability or confidence is implied.
 */
export function measuredBand(frame: SimulationFrame | null | undefined): HealthBand {
  if (!frame) return 'unknown';
  const events = frame.events ?? [];
  if (events.length === 0) return 'unknown';
  const failing = events.filter((event) => event.level === 'ERROR' || event.level === 'FATAL').length;
  const errorShare = failing / events.length;
  if (errorShare >= 0.1) return 'elevated';
  if (errorShare >= 0.02) return 'watch';
  return 'healthy';
}

export function healthBand(state: ServiceHealthState | undefined): HealthBand {
  if (state === 'healthy') return 'healthy';
  if (state === 'degraded') return 'watch';
  if (state === 'critical') return 'elevated';
  return 'unknown';
}

export function healthTone(state: ServiceHealthState | undefined): 'good' | 'warn' | 'danger' | 'neutral' {
  if (state === 'healthy') return 'good';
  if (state === 'degraded') return 'warn';
  if (state === 'critical') return 'danger';
  return 'neutral';
}

export function severityTone(severity: string | undefined): 'good' | 'warn' | 'danger' | 'info' | 'neutral' {
  const value = (severity ?? '').toLowerCase();
  if (value.includes('critical') || value.includes('fatal')) return 'danger';
  if (value.includes('warn') || value.includes('error') || value.includes('elevated')) return 'warn';
  if (value.includes('info') || value.includes('low')) return 'info';
  return 'neutral';
}

/**
 * Joins the declared topology (structure only) with measured window health.
 *
 * <p>Positions and edges come from the declared dependency catalogue; event volume, error rate and
 * health band come from the current rolling window. Nothing here is invented for layout.</p>
 */
export function toTopology(
  topology: SimulationTopologyPayload | null,
  health: ServiceHealth[]
): { nodes: TopologyNode[]; edges: TopologyEdge[] } {
  if (!topology) return { nodes: [], edges: [] };
  const byService = new Map(health.map((item) => [item.service, item]));
  const nodes: TopologyNode[] = topology.nodes.map((node) => {
    const measured = byService.get(node.id);
    return {
      id: node.id,
      events: measured?.events ?? 0,
      errorRate: measured?.errorRate,
      health: healthBand(measured?.state)
    };
  });
  const edges: TopologyEdge[] = topology.edges.map((edge) => {
    const source = byService.get(edge.source)?.events ?? 0;
    const target = byService.get(edge.target)?.events ?? 0;
    return { source: edge.source, target: edge.target, weight: source + target };
  });
  return { nodes, edges };
}

export function blastRadiusServices(incident: SimulationIncident | null | undefined): string[] {
  if (!incident) return [];
  return [incident.originService, ...incident.affectedServices].filter(
    (service, index, all) => all.indexOf(service) === index
  );
}

export interface FrameTotals {
  events: number;
  errors: number;
  warnings: number;
  healthy: number;
  degraded: number;
  critical: number;
  maxLoad: number;
}

export function summarize(health: ServiceHealth[]): FrameTotals {
  return health.reduce<FrameTotals>(
    (totals, service) => {
      totals.events += service.events;
      totals.errors += service.errors;
      totals.warnings += Math.max(0, service.events - service.errors);
      if (service.state === 'healthy') totals.healthy += 1;
      if (service.state === 'degraded') totals.degraded += 1;
      if (service.state === 'critical') totals.critical += 1;
      totals.maxLoad = Math.max(totals.maxLoad, service.load);
      return totals;
    },
    { events: 0, errors: 0, warnings: 0, healthy: 0, degraded: 0, critical: 0, maxLoad: 0 }
  );
}

export function errorRateSeries(samples: TelemetrySample[]): Array<{ label: string; value: number }> {
  return samples.map((sample) => ({ label: String(sample.tick), value: sample.errorRate }));
}

export function latencySeries(samples: TelemetrySample[]): Array<{ label: string; value: number }> {
  return samples.map((sample) => ({ label: String(sample.tick), value: sample.p95LatencyMs }));
}

export function throughputSeries(samples: TelemetrySample[]): Array<{ label: string; value: number }> {
  return samples.map((sample) => ({ label: String(sample.tick), value: sample.eventsPerSecond }));
}

export function evidenceByAlgorithm(evidence: SimulationEvidence[]): Map<string, SimulationEvidence[]> {
  const grouped = new Map<string, SimulationEvidence[]>();
  for (const item of evidence) {
    const bucket = grouped.get(item.algorithm);
    if (bucket) bucket.push(item);
    else grouped.set(item.algorithm, [item]);
  }
  return grouped;
}

export function totalMeasuredRuntimeMicros(evidence: SimulationEvidence[]): number {
  return evidence.reduce((sum, item) => sum + (Number.isFinite(item.runtimeMicros) ? item.runtimeMicros : 0), 0);
}

export function uniqueAlgorithms(evidence: SimulationEvidence[]): string[] {
  return Array.from(new Set(evidence.map((item) => item.algorithm)));
}

export function frameTitle(frame: SimulationFrame | null, fallback: string): string {
  if (!frame) return fallback;
  return `Tick ${frame.tick} · ${frame.phase}`;
}

export function formatMicros(micros: number): string {
  if (!Number.isFinite(micros)) return 'unavailable';
  if (micros >= 1000) return `${(micros / 1000).toFixed(2)} ms`;
  return `${micros.toFixed(1)} µs`;
}

export function sortIncidents(incidents: SimulationIncident[]): SimulationIncident[] {
  const order: Record<string, number> = {
    DETECTED: 0,
    INVESTIGATING: 1,
    ACKNOWLEDGED: 2,
    MITIGATED: 3,
    RESOLVED: 4
  };
  return [...incidents].sort((left, right) => {
    const status = (order[left.status] ?? 9) - (order[right.status] ?? 9);
    if (status !== 0) return status;
    if (left.errorRate !== right.errorRate) return right.errorRate - left.errorRate;
    return left.id - right.id;
  });
}
