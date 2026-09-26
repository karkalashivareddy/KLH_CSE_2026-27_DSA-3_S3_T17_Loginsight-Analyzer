import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import type { IncidentDto, LogEvent, ObjectApiResponse, OverviewDto, ServiceStatsDto, SystemStatus } from '../api/types';
import { ReplayProvider } from '../replay/ReplayContext';
import OverviewPage from './OverviewPage';

const event: LogEvent = {
  id: 11,
  timestamp: '2026-09-25T10:05:00Z',
  severityNumber: 9,
  level: 'ERROR',
  service: 'api',
  host: 'host-1',
  ipAddress: '127.0.0.1',
  httpMethod: 'GET',
  endpoint: '/events',
  statusCode: 500,
  responseTime: 4,
  requestId: 'request-11',
  userId: 'user-11',
  message: 'request failed',
  traceId: null,
  spanId: null,
  url: null,
  source: 'test',
  rawMessage: null,
  attributes: {}
};

const service: ServiceStatsDto = {
  name: 'api',
  events: 100,
  errors: 6,
  warnings: 2,
  eventRate: 6,
  hosts: 1,
  latestAt: '2026-09-25T10:05:00Z',
  severity: { ERROR: 6, INFO: 94 }
};

const incident: IncidentDto = {
  id: 3,
  start: '2026-09-25T10:00:00Z',
  end: '2026-09-25T10:05:00Z',
  services: ['api'],
  eventCount: 6,
  primaryPattern: 'request <*> failed',
  status: 'OPEN',
  method: 'Heuristic: 5-minute error-rate threshold'
};

const overview: OverviewDto = {
  dataset: 'sample',
  datasetEvents: 400,
  windowStart: '2026-09-25T09:00:00Z',
  windowEnd: '2026-09-25T10:00:00Z',
  scope: 'selected window',
  systemStatus: 'Operational',
  events: 100,
  errors: 6,
  warnings: 2,
  services: 1,
  hosts: 1,
  eventsPerMinute: 60,
  activeIncidents: 2,
  timeline: [{ start: '2026-09-25T09:00:00Z', end: '2026-09-25T09:30:00Z', count: 50 }, { start: '2026-09-25T09:30:00Z', end: '2026-09-25T10:00:00Z', count: 50 }],
  range: '1h',
  severity: { ERROR: 6, INFO: 94 },
  topServices: [service],
  topPatterns: [{ template: 'request <*> failed', count: 6, example: 'request failed', level: 'ERROR' }],
  recentCritical: [event],
  heatmap: { days: ['Mon'], columns: 2, cells: [[1, 2]] },
  statusCodes: { '200': 90, '500': 6, '503': 4 }
};

const dependencies: ObjectApiResponse = {
  nodes: [{ id: 'api', events: 100, outDegree: 1, inDegree: 0 }],
  edges: [{ source: 'api', target: 'auth', weight: 8 }],
  nodeCount: 1,
  edgeCount: 1
};

const health: SystemStatus = {
  status: 'UP',
  service: 'loginsight',
  timestamp: '2026-09-25T10:05:00Z',
  uptimeMillis: 1000,
  datasetLoaded: true,
  datasetName: 'sample',
  datasetSize: 400,
  engines: 4
};

function renderPage() {
  return render(<MemoryRouter><ReplayProvider><OverviewPage /></ReplayProvider></MemoryRouter>);
}

describe('Command Center', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('shows explicit selected-window metrics and investigation states', async () => {
    vi.spyOn(api, 'overview').mockResolvedValue(overview);
    vi.spyOn(api, 'dependencies').mockResolvedValue(dependencies);
    vi.spyOn(api, 'services').mockResolvedValue([service]);
    vi.spyOn(api, 'incidents').mockResolvedValue([incident]);
    vi.spyOn(api, 'systemStatus').mockResolvedValue(health);
    vi.spyOn(api, 'liveStatus').mockResolvedValue({ enabled: true, dataset: 'sample', total: 400, source: 'sample', label: 'Ready' });

    renderPage();

    expect(await screen.findByText('Selected-window observed rate')).toBeInTheDocument();
    expect(screen.getByText('Detected incident windows')).toBeInTheDocument();
    expect(screen.getByText('Dataset coverage')).toBeInTheDocument();
    expect(screen.getByText('25.0%')).toBeInTheDocument();
    expect(screen.getByText('60.00 / min')).toBeInTheDocument();
    expect(screen.getByText('Latest detected investigation')).toBeInTheDocument();
    expect(screen.queryByText('Active detected investigation')).toBeNull();
    expect(screen.getByText(/Dataset-wide detector results filtered to the selected window: selected window/)).toBeInTheDocument();
    expect(screen.getByText(/no causal attribution is inferred/)).toBeInTheDocument();
    expect(screen.getByText('Observed request-trail topology')).toBeInTheDocument();
    expect(screen.getAllByText(/Node size follows dataset-wide events; health band and error rate follow the selected window/).length).toBeGreaterThan(0);
    expect(screen.getByText('Service health matrix')).toBeInTheDocument();
    expect(screen.getByText('Pattern intelligence')).toBeInTheDocument();
    expect(screen.getByText('HTTP class breakdown')).toBeInTheDocument();
    expect(screen.getByText('Recent critical events')).toBeInTheDocument();
  });

  it('labels window severity counts, replay and pattern links honestly', async () => {
    vi.spyOn(api, 'overview').mockResolvedValue(overview);
    vi.spyOn(api, 'dependencies').mockResolvedValue(dependencies);
    vi.spyOn(api, 'services').mockResolvedValue([service]);
    vi.spyOn(api, 'incidents').mockResolvedValue([incident]);
    vi.spyOn(api, 'systemStatus').mockResolvedValue(health);
    vi.spyOn(api, 'liveStatus').mockResolvedValue({ enabled: true, dataset: 'sample', total: 400, source: 'demo-replay', label: 'Ready' });

    renderPage();

    expect(await screen.findByText('2 warnings · 6 ERROR/FATAL')).toBeInTheDocument();
    expect(screen.queryByText('2 warnings · 6 errors')).toBeNull();
    expect(screen.getAllByText('DEMO REPLAY').length).toBeGreaterThan(0);
    expect(screen.getByLabelText('Demo replay of the loaded dataset, not live production telemetry')).toBeInTheDocument();
    const logsLink = screen.getByRole('link', { name: 'Search logs for the example message of request <*> failed' });
    expect(logsLink).toHaveAttribute('href', '/logs?q=request%20failed');
  });

  it('keeps the no-dataset state explicit', async () => {
    vi.spyOn(api, 'overview').mockResolvedValue(null);
    vi.spyOn(api, 'dependencies').mockResolvedValue({ nodes: [], edges: [], nodeCount: 0, edgeCount: 0 });
    vi.spyOn(api, 'services').mockResolvedValue([]);
    vi.spyOn(api, 'incidents').mockResolvedValue([]);
    vi.spyOn(api, 'systemStatus').mockResolvedValue({ ...health, datasetLoaded: false, datasetName: null, datasetSize: 0 });
    vi.spyOn(api, 'liveStatus').mockResolvedValue({ enabled: false, dataset: null, total: 0, source: null, label: 'Standby' });

    renderPage();

    expect(await screen.findByText('No dataset loaded.')).toBeInTheDocument();
    expect(screen.getByText('Open datasets')).toBeInTheDocument();
  });
});
