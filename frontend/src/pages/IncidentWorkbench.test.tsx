import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import IncidentWorkbenchPage from './IncidentWorkbenchPage';
import { TelemetryProvider } from '../telemetry/TelemetryContext';
import { api } from '../api/client';
import type { SimulationFrame, SimulationIncident } from '../api/types';

function incident(overrides: Partial<SimulationIncident> = {}): SimulationIncident {
  return {
    id: 1,
    scenarioId: 'checkout-5xx-cascade',
    title: 'Checkout 5xx cascade',
    signal: 'error-burst',
    method: 'Aho-Corasick + rolling window',
    severity: 'CRITICAL',
    status: 'DETECTED',
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
    timeline: [{ at: '2026-01-01T10:00:40Z', status: 'DETECTED', label: 'Detected' }],
    persistence: 'session',
    ...overrides
  };
}

const frame: SimulationFrame = {
  sessionId: 'sim-1',
  sequence: 5,
  source: 'live-simulation',
  label: 'generated frame',
  scenarioId: 'checkout-5xx-cascade',
  scenarioTitle: 'Checkout 5xx cascade',
  seed: 20260412,
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
  events: [],
  signals: [],
  evidence: [],
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
    }
  ],
  incidents: [incident()],
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

describe('IncidentWorkbenchPage', () => {
  beforeEach(() => {
    vi.spyOn(api, 'scenarios').mockResolvedValue([
      {
        id: 'checkout-5xx-cascade',
        title: 'Checkout 5xx cascade',
        summary: 'payments pool exhaustion',
        seed: 20260412,
        durationSeconds: 60,
        onsetSeconds: 3,
        peakSeconds: 10,
        recoverySeconds: 42,
        affectedServices: ['payments'],
        errorSignatures: ['PoolExhaustedException'],
        expectedSignal: 'error-burst',
        expectedIncident: 'Checkout 5xx cascade',
        severity: 'CRITICAL',
        source: 'live-simulation',
        label: 'Deterministic simulation'
      }
    ]);
    vi.spyOn(api, 'simulationStatus').mockResolvedValue({
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
    });
    vi.spyOn(api, 'simulationSample').mockResolvedValue(frame);
    vi.spyOn(api, 'simulationStream').mockReturnValue(() => undefined);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    window.localStorage.clear();
  });

  function renderWorkbench() {
    return render(
      <TelemetryProvider>
        <IncidentWorkbenchPage />
      </TelemetryProvider>
    );
  }

  it('shows the detected incident with its measured evidence and blast radius', async () => {
    renderWorkbench();
    await screen.findAllByText('Checkout 5xx cascade');
    expect((await screen.findAllByText('Aho-Corasick')).length).toBeGreaterThan(0);
    expect(screen.getByText(/Traversal starts at payments/)).toBeTruthy();
    expect(screen.getByText(/not a learned probability/)).toBeTruthy();
  });

  it('advances the lifecycle with the server contract, not a local guess', async () => {
    const transition = vi
      .spyOn(api, 'transitionIncident')
      .mockResolvedValue(incident({ status: 'INVESTIGATING', open: true }));
    renderWorkbench();
    await screen.findAllByText('Checkout 5xx cascade');
    fireEvent.click(await screen.findByRole('button', { name: /Investigate/ }));
    await waitFor(() =>
      expect(transition).toHaveBeenCalledWith(1, { sessionId: expect.any(String), status: 'INVESTIGATING' })
    );
  });

  it('offers automatic advancement while the incident is open', async () => {
    renderWorkbench();
    await screen.findAllByText('Checkout 5xx cascade');
    const button = await screen.findByRole('button', { name: /Advance automatically/ });
    expect(button.hasAttribute('disabled')).toBe(false);
  });

  it('states that lifecycle changes are session scoped', async () => {
    renderWorkbench();
    await screen.findAllByText('Checkout 5xx cascade');
    expect(screen.getByText(/stored for the session only, never in the dataset/)).toBeTruthy();
  });
});
