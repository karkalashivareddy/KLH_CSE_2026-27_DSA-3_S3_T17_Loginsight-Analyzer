import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ScenarioLabPage from './ScenarioLabPage';
import { TelemetryProvider } from '../telemetry/TelemetryContext';
import { api } from '../api/client';
import type { Scenario, SimulationFrame, SimulationStatus } from '../api/types';

const scenario: Scenario = {
  id: 'checkout-5xx-cascade',
  title: 'Checkout 5xx cascade',
  summary: 'payments pool exhaustion surfaces as a 5xx cascade across checkout.',
  seed: 20260412,
  durationSeconds: 60,
  onsetSeconds: 3,
  peakSeconds: 10,
  recoverySeconds: 42,
  affectedServices: ['payments', 'orders', 'api-gateway'],
  errorSignatures: ['PoolExhaustedException', 'HTTP 502 upstream reset'],
  expectedSignal: 'error-burst',
  expectedIncident: 'Checkout 5xx cascade',
  severity: 'CRITICAL',
  source: 'live-simulation',
  label: 'Deterministic simulation'
};

const status: SimulationStatus = {
  enabled: true,
  source: 'live-simulation',
  label: 'Deterministic simulation',
  scenarios: 7,
  activeSessions: 0,
  maxConcurrentStreams: 24,
  speed: { min: 0.25, max: 8, default: 1 },
  intervalMs: { min: 40, max: 5000, default: 250 },
  maxFrames: { min: 1, max: 20000, default: 1200 },
  determinism: 'scenario + seed + tick',
  persistence: 'session'
};

const frame: SimulationFrame = {
  sessionId: 'sim-1',
  sequence: 1,
  source: 'live-simulation',
  label: 'generated frame',
  scenarioId: scenario.id,
  scenarioTitle: scenario.title,
  seed: scenario.seed,
  tick: 400,
  tickMillis: 250,
  eventsPerFrame: 12,
  intensity: 0.9,
  phase: 'peak',
  totalEvents: 980,
  totalErrors: 210,
  errorRate: 21.4,
  eventsPerSecond: 48,
  averageLatencyMs: 420,
  p95LatencyMs: 1180,
  baselineP95LatencyMs: 220,
  windowSize: 60,
  windowCapacity: 600,
  matchedSignatures: ['PoolExhaustedException'],
  events: [
    {
      id: 1,
      timestamp: '2026-01-01T10:00:00Z',
      severityNumber: 17,
      level: 'ERROR',
      service: 'payments',
      host: 'payments-1',
      ipAddress: '10.0.0.1',
      httpMethod: 'POST',
      endpoint: '/pay',
      statusCode: 502,
      responseTime: 1180,
      requestId: 'r-1',
      userId: 'u-1',
      message: 'PoolExhaustedException: connection pool exhausted',
      traceId: 't-1',
      spanId: 's-1',
      url: null,
      source: 'live-simulation',
      rawMessage: 'PoolExhaustedException: connection pool exhausted',
      attributes: { pool: 'payments' }
    }
  ],
  signals: [
    {
      id: 'sig-1',
      kind: 'error-burst',
      label: 'Error burst on payments',
      detail: '21.4% of the rolling window is failing',
      severity: 'CRITICAL',
      service: 'payments'
    }
  ],
  evidence: [
    {
      algorithm: 'Aho-Corasick',
      purpose: 'Multi-signature error scan',
      inputSize: 980,
      inputUnit: 'window characters',
      result: '1 signature matched',
      runtimeNanos: 412_000,
      runtimeMicros: 412,
      complexity: 'O(n + m)',
      references: ['Gusfield 1997']
    }
  ],
  health: [
    {
      service: 'payments',
      label: 'payments',
      tier: 'core',
      events: 300,
      errors: 180,
      errorRate: 60,
      averageLatencyMs: 900,
      state: 'critical',
      load: 0.98,
      inBlastRadius: true
    },
    {
      service: 'orders',
      label: 'orders',
      tier: 'core',
      events: 400,
      errors: 20,
      errorRate: 5,
      averageLatencyMs: 300,
      state: 'degraded',
      load: 0.7,
      inBlastRadius: true
    }
  ],
  incidents: [
    {
      id: 1,
      scenarioId: scenario.id,
      title: 'Checkout 5xx cascade',
      signal: 'error-burst',
      method: 'Aho-Corasick + rolling window',
      severity: 'CRITICAL',
      status: 'INVESTIGATING',
      open: true,
      originService: 'payments',
      affectedServices: ['orders'],
      blastRadius: ['orders', 'api-gateway'],
      detectedAt: '2026-01-01T10:01:00Z',
      start: '2026-01-01T10:00:40Z',
      end: null,
      lastUpdatedAt: '2026-01-01T10:01:00Z',
      eventCount: 210,
      errorRate: 21.4,
      p95LatencyMs: 1180,
      matchedSignatures: ['PoolExhaustedException'],
      evidence: [],
      timeline: [{ at: '2026-01-01T10:01:00Z', status: 'INVESTIGATING', label: 'Investigating' }],
      persistence: 'session'
    }
  ],
  topology: {
    nodes: [
      { id: 'api-gateway', label: 'api-gateway', tier: 'edge' },
      { id: 'orders', label: 'orders', tier: 'core' },
      { id: 'payments', label: 'payments', tier: 'core' }
    ],
    edges: [
      { source: 'api-gateway', target: 'orders' },
      { source: 'orders', target: 'payments' }
    ],
    kind: 'declared',
    label: 'Declared service dependencies'
  }
};

describe('ScenarioLabPage', () => {
  beforeEach(() => {
    vi.spyOn(api, 'scenarios').mockResolvedValue([scenario]);
    vi.spyOn(api, 'simulationStatus').mockResolvedValue(status);
    vi.spyOn(api, 'simulationSample').mockResolvedValue(frame);
    vi.spyOn(api, 'simulationStream').mockReturnValue(() => undefined);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    window.localStorage.clear();
  });

  function renderLab() {
    return render(
      <TelemetryProvider>
        <ScenarioLabPage />
      </TelemetryProvider>
    );
  }

  it('lists scenarios from the backend and marks the persisted selection', async () => {
    renderLab();
    await screen.findAllByText('Checkout 5xx cascade');
    const card = screen.getByRole('button', { name: /Checkout 5xx cascade/ });
    expect(card).toHaveAttribute('aria-pressed', 'true');
  });

  it('previews a real server frame before any stream starts', async () => {
    renderLab();
    await screen.findAllByText('Checkout 5xx cascade');
    await waitFor(() => expect(api.simulationSample).toHaveBeenCalled());
    expect(api.simulationStream).not.toHaveBeenCalled();
    // Everything below is measured output from the server, never a client-side fixture.
    expect((await screen.findAllByText('21.40%')).length).toBeGreaterThan(0);
    expect((await screen.findAllByText('Aho-Corasick')).length).toBeGreaterThan(0);
    await screen.findByText(/Multi-signature error scan/);
    await screen.findByText(/window characters/);
    expect((await screen.findAllByText(/blast radius/i)).length).toBeGreaterThan(0);
  });

  it('opens a deterministic run through the client and reports the session', async () => {
    renderLab();
    await screen.findAllByText('Checkout 5xx cascade');
    fireEvent.click(screen.getByRole('button', { name: /Start run/ }));
    await waitFor(() =>
      expect(api.simulationStream).toHaveBeenCalledWith(
        expect.objectContaining({ onStart: expect.any(Function) }),
        expect.objectContaining({ scenario: scenario.id, seed: scenario.seed })
      )
    );
  });

  it('keeps the two data sources separate in its copy', async () => {
    renderLab();
    await screen.findByText(/Deterministic simulation/);
    expect(screen.getByText(/no page mixes the two sources/i)).toBeTruthy();
  });
});
