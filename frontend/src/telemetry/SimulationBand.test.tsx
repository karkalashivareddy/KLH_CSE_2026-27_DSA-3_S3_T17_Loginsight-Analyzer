import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { SimulationBand } from './SimulationBand';
import { TelemetryProvider } from './TelemetryContext';
import { api } from '../api/client';
import type { SimulationFrame } from '../api/types';

const frame: SimulationFrame = {
  sessionId: 'sim-1',
  sequence: 3,
  source: 'live-simulation',
  label: 'generated frame',
  scenarioId: 'checkout-5xx-cascade',
  scenarioTitle: 'Checkout 5xx cascade',
  seed: 20260412,
  tick: 300,
  tickMillis: 250,
  eventsPerFrame: 14,
  intensity: 0.95,
  phase: 'peak',
  totalEvents: 1200,
  totalErrors: 240,
  errorRate: 20,
  eventsPerSecond: 56,
  averageLatencyMs: 380,
  p95LatencyMs: 990,
  baselineP95LatencyMs: 200,
  windowSize: 60,
  windowCapacity: 600,
  matchedSignatures: ['PoolExhaustedException'],
  events: [],
  signals: [
    {
      id: 'sig-1',
      kind: 'error-burst',
      label: 'Error burst on payments',
      detail: '20.00% of the window is failing',
      severity: 'CRITICAL',
      service: 'payments'
    }
  ],
  evidence: [
    {
      algorithm: 'KMP',
      purpose: 'Confirm the strongest signature',
      inputSize: 1200,
      inputUnit: 'window characters',
      result: '1 confirmed match',
      runtimeNanos: 88_000,
      runtimeMicros: 88,
      complexity: 'O(n + m)',
      references: ['Knuth-Morris-Pratt 1977']
    }
  ],
  health: [
    {
      service: 'payments',
      label: 'payments',
      tier: 'core',
      events: 300,
      errors: 200,
      errorRate: 66.6,
      averageLatencyMs: 800,
      state: 'critical',
      load: 0.97,
      inBlastRadius: true
    },
    {
      service: 'orders',
      label: 'orders',
      tier: 'core',
      events: 500,
      errors: 20,
      errorRate: 4,
      averageLatencyMs: 220,
      state: 'healthy',
      load: 0.5,
      inBlastRadius: true
    }
  ],
  incidents: [
    {
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
      eventCount: 240,
      errorRate: 20,
      p95LatencyMs: 990,
      matchedSignatures: ['PoolExhaustedException'],
      evidence: [],
      timeline: [{ at: '2026-01-01T10:01:00Z', status: 'DETECTED', label: 'Detected' }],
      persistence: 'session'
    }
  ],
  topology: { nodes: [], edges: [], kind: 'declared', label: 'Declared service dependencies' }
};

describe('SimulationBand', () => {
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
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    window.localStorage.clear();
  });

  it('shows measured simulation metrics from the shared stream', async () => {
    render(
      <MemoryRouter>
        <TelemetryProvider>
          <SimulationBand />
        </TelemetryProvider>
      </MemoryRouter>
    );
    expect((await screen.findAllByText('20.00%')).length).toBeGreaterThan(0);
    expect(screen.getByText('1/2')).toBeTruthy();
    expect(screen.getByText(/20.00% of the window is failing/)).toBeTruthy();
  });

  it('separates the open incident from the dataset sections and links to the workbench', async () => {
    render(
      <MemoryRouter>
        <TelemetryProvider>
          <SimulationBand />
        </TelemetryProvider>
      </MemoryRouter>
    );
    // Wait for the assertion target itself. The scenario title and the open incident arrive from
    // two independent async calls, so matching the title can resolve before the incident has
    // rendered its workbench link. Waiting on the link keeps this deterministic under load.
    const link = await screen.findByRole('link', { name: /Investigate/ });
    expect(link.getAttribute('href')).toBe('/incidents/workbench');
    expect(await screen.findByText(/kept separate from the dataset/)).toBeTruthy();
  });
});
