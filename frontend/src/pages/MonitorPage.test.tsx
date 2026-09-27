import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import MonitorPage from './MonitorPage';
import { TelemetryProvider } from '../telemetry/TelemetryContext';
import { api } from '../api/client';
import type { SimulationFrame } from '../api/types';

const frame: SimulationFrame = {
  sessionId: 'sim-1',
  sequence: 12,
  source: 'live-simulation',
  label: 'generated frame',
  scenarioId: 'checkout-5xx-cascade',
  scenarioTitle: 'Checkout 5xx cascade',
  seed: 20260412,
  tick: 40,
  tickMillis: 250,
  eventsPerFrame: 18,
  intensity: 0.92,
  phase: 'peak',
  totalEvents: 800,
  totalErrors: 380,
  errorRate: 27.13,
  eventsPerSecond: 72,
  averageLatencyMs: 512,
  p95LatencyMs: 1490,
  baselineP95LatencyMs: 210,
  windowSize: 60,
  windowCapacity: 600,
  matchedSignatures: ['PoolExhaustedException'],
  events: [],
  signals: [
    {
      id: 'sig-1',
      kind: 'latency',
      label: 'Latency regression on payments',
      detail: 'p95 is 7.1x the rolling baseline',
      severity: 'CRITICAL',
      service: 'payments'
    }
  ],
  evidence: [
    {
      algorithm: 'KMP',
      purpose: 'Confirm the strongest signature',
      inputSize: 800,
      inputUnit: 'window characters',
      result: '1 confirmed match',
      runtimeNanos: 96_000,
      runtimeMicros: 96,
      complexity: 'O(n + m)',
      references: ['Knuth-Morris-Pratt 1977']
    }
  ],
  health: [
    {
      service: 'payments',
      label: 'payments',
      tier: 'core',
      events: 400,
      errors: 300,
      errorRate: 75,
      averageLatencyMs: 1200,
      state: 'critical',
      load: 0.99,
      inBlastRadius: true
    }
  ],
  incidents: [
    {
      id: 1,
      scenarioId: 'checkout-5xx-cascade',
      title: 'Checkout 5xx cascade',
      signal: 'latency',
      method: 'KMP + rolling window',
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
      eventCount: 380,
      errorRate: 27.13,
      p95LatencyMs: 1490,
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

describe('MonitorPage', () => {
  beforeEach(() => {
    vi.spyOn(api, 'scenarios').mockResolvedValue([
      {
        id: 'checkout-5xx-cascade',
        title: 'Checkout 5xx cascade',
        summary: 'payments pool exhaustion surfaces as a 5xx cascade',
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

  function renderMonitor() {
    return render(
      <MemoryRouter>
        <TelemetryProvider>
          <MonitorPage />
        </TelemetryProvider>
      </MemoryRouter>
    );
  }

  it('labels the stream as generated rather than captured telemetry', async () => {
    renderMonitor();
    await screen.findAllByText('Checkout 5xx cascade');
    expect(screen.getAllByText('GENERATED').length).toBeGreaterThan(0);
  });

  it('renders measured frame metrics and evidence from the server', async () => {
    renderMonitor();
    await screen.findAllByText('Checkout 5xx cascade');
    expect((await screen.findAllByText('27.13%')).length).toBeGreaterThan(0);
    expect((await screen.findAllByText('KMP')).length).toBeGreaterThan(0);
    expect(await screen.findByText(/p95 is 7.1x the rolling baseline/)).toBeTruthy();
  });

  it('starts a deterministic run on demand', async () => {
    renderMonitor();
    await screen.findAllByText('Checkout 5xx cascade');
    fireEvent.click(screen.getByRole('button', { name: /Start stream/ }));
    await waitFor(() => expect(api.simulationStream).toHaveBeenCalled());
  });

  it('points operators at Scenario Lab instead of implying they can change traffic here', async () => {
    renderMonitor();
    await screen.findAllByText('Checkout 5xx cascade');
    const link = screen.getByRole('link', { name: 'Scenario Lab' });
    expect(link.getAttribute('href')).toBe('/scenario-lab');
  });
});
