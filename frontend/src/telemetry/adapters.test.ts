import { describe, expect, it } from 'vitest';
import { measuredBand } from './adapters';
import type { LogEvent, SimulationFrame } from '../api/types';

function event(level: LogEvent['level'], id: number): LogEvent {
  return {
    id,
    timestamp: '2026-01-01T10:00:00Z',
    level,
    service: 'payments',
    host: 'payments-1',
    message: 'generated event',
    responseTime: 120
  } as LogEvent;
}

function frame(events: LogEvent[]): SimulationFrame {
  return {
    sessionId: 'sim-1',
    sequence: 1,
    source: 'live-simulation',
    label: 'generated frame',
    scenarioId: 'checkout-5xx-cascade',
    scenarioTitle: 'Checkout 5xx Cascade',
    seed: 20260412,
    tick: 40,
    tickMillis: 250,
    eventsPerFrame: events.length,
    intensity: 0.5,
    phase: 'degrading',
    totalEvents: 800,
    totalErrors: 120,
    errorRate: 19.44,
    eventsPerSecond: 80,
    averageLatencyMs: 1800,
    p95LatencyMs: 4900,
    baselineP95LatencyMs: 200,
    windowSize: 600,
    windowCapacity: 4000,
    matchedSignatures: [],
    events,
    signals: [],
    evidence: [],
    health: [],
    incidents: [],
    topology: { nodes: [], edges: [], kind: 'declared', label: 'Declared service dependencies' }
  };
}

describe('measuredBand', () => {
  it('reads the current tick rather than the lagging rolling window', () => {
    // A run that has already resolved can still carry a high window error rate and a high rolling
    // p95. The badge must not contradict the resolved incident, so it uses this tick's events.
    const resolved = frame(Array.from({ length: 20 }, (_, i) => event('INFO', i)));
    expect(resolved.errorRate).toBeGreaterThan(10);
    expect(resolved.p95LatencyMs).toBeGreaterThan(resolved.baselineP95LatencyMs * 3);
    expect(measuredBand(resolved)).toBe('healthy');
  });

  it('reports elevated while the current tick is failing', () => {
    const events = [...Array.from({ length: 8 }, (_, i) => event('ERROR', i)), ...Array.from({ length: 12 }, (_, i) => event('INFO', i + 8))];
    expect(measuredBand(frame(events))).toBe('elevated');
  });

  it('counts FATAL as failing, matching the server detector rule', () => {
    const events = [event('FATAL', 1), ...Array.from({ length: 19 }, (_, i) => event('INFO', i + 2))];
    expect(measuredBand(frame(events))).toBe('watch');
  });

  it('is explicit about having no measurement', () => {
    expect(measuredBand(null)).toBe('unknown');
    expect(measuredBand(frame([]))).toBe('unknown');
  });
});
