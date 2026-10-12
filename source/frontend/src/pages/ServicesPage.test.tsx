import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import ServicesPage from './ServicesPage';
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
  signals: [],
  evidence: [],
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
  incidents: [],
  topology: { nodes: [], edges: [], kind: 'declared', label: 'Declared service dependencies' }
};

function missingDataset(): Error {
  return Object.assign(new Error('No dataset'), { apiError: { status: 404 } });
}

describe('ServicesPage generated fleet', () => {
  beforeEach(() => {
    vi.spyOn(api, 'services').mockRejectedValue(missingDataset());
    vi.spyOn(api, 'dependencies').mockRejectedValue(missingDataset());
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

  function renderServices() {
    return render(
      <MemoryRouter>
        <TelemetryProvider>
          <ServicesPage />
        </TelemetryProvider>
      </MemoryRouter>
    );
  }

  it('shows generated service health even when no dataset is loaded', async () => {
    renderServices();
    expect(await screen.findByText('Generated service health')).toBeTruthy();
    expect(screen.getAllByText('GENERATED').length).toBeGreaterThan(0);
    expect(await screen.findByText('elevated')).toBeTruthy();
  });

  it('keeps the generated fleet separate from the dataset service map', async () => {
    renderServices();
    await screen.findByText('Generated service health');
    expect(screen.getByText(/Load a dataset before requesting the service fleet/)).toBeTruthy();
    expect(screen.getByText(/independent of any loaded dataset/)).toBeTruthy();
  });
});
