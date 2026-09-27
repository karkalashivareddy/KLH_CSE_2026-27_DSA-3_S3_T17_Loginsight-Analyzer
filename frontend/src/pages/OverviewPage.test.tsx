import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import type { IncidentDto, LogEvent, ObjectApiResponse, OverviewDto, ServiceStatsDto, SystemStatus } from '../api/types';
import { ReplayProvider } from '../replay/ReplayContext';
import OverviewPage from './OverviewPage';

const event: LogEvent = { id: 11, timestamp: '2026-09-25T10:05:00Z', severityNumber: 9, level: 'ERROR', service: 'api', host: 'host-1', ipAddress: '127.0.0.1', httpMethod: 'GET', endpoint: '/events', statusCode: 500, responseTime: 4, requestId: 'request-11', userId: 'user-11', message: 'request failed', traceId: null, spanId: null, url: null, source: 'test', rawMessage: null, attributes: {} };
const service: ServiceStatsDto = { name: 'api', events: 100, errors: 6, warnings: 2, eventRate: 6, hosts: 1, latestAt: '2026-09-25T10:05:00Z', severity: { ERROR: 6, INFO: 94 } };
const incident: IncidentDto = { id: 3, start: '2026-09-25T10:00:00Z', end: '2026-09-25T10:05:00Z', services: ['api'], eventCount: 6, primaryPattern: 'request <*> failed', status: 'OPEN', method: 'Heuristic: 5-minute error-rate threshold' };
const overview: OverviewDto = { dataset: 'sample', datasetEvents: 400, windowStart: '2026-09-25T09:00:00Z', windowEnd: '2026-09-25T10:00:00Z', scope: 'selected window', systemStatus: 'Operational', events: 100, errors: 6, warnings: 2, services: 1, hosts: 1, eventsPerMinute: 60, activeIncidents: 2, timeline: [{ start: '2026-09-25T09:00:00Z', end: '2026-09-25T09:30:00Z', count: 50 }, { start: '2026-09-25T09:30:00Z', end: '2026-09-25T10:00:00Z', count: 50 }], range: '1h', severity: { ERROR: 6, INFO: 94 }, topServices: [service], topPatterns: [{ template: 'request <*> failed', count: 6, example: 'request failed', level: 'ERROR' }], recentCritical: [event], heatmap: { days: ['Mon'], columns: 2, cells: [[1, 2]] }, statusCodes: { '200': 90, '500': 6, '503': 4 } };
const dependencies: ObjectApiResponse = { nodes: [{ id: 'api', events: 100, outDegree: 1, inDegree: 0 }], edges: [{ source: 'api', target: 'auth', weight: 8 }], nodeCount: 1, edgeCount: 1 };
const health: SystemStatus = { status: 'UP', service: 'loginsight', timestamp: '2026-09-25T10:05:00Z', uptimeMillis: 1000, datasetLoaded: true, datasetName: 'sample', datasetSize: 400, engines: 4 };

function renderPage() { return render(<MemoryRouter><ReplayProvider><OverviewPage /></ReplayProvider></MemoryRouter>); }

describe('System overview', () => {
  afterEach(() => { cleanup(); vi.restoreAllMocks(); });

  it('prioritizes selected-window signals, observed relationships and investigation evidence', async () => {
    vi.spyOn(api, 'overview').mockResolvedValue(overview);
    vi.spyOn(api, 'dependencies').mockResolvedValue(dependencies);
    vi.spyOn(api, 'services').mockResolvedValue([service]);
    vi.spyOn(api, 'incidents').mockResolvedValue([incident]);
    vi.spyOn(api, 'systemStatus').mockResolvedValue(health);
    vi.spyOn(api, 'liveStatus').mockResolvedValue({ enabled: true, dataset: 'sample', total: 400, source: 'sample', label: 'Ready' });
    renderPage();

    expect(await screen.findByRole('heading', { name: 'System overview' })).toBeInTheDocument();
    expect(screen.getByText('Events observed')).toBeInTheDocument();
    expect(screen.getByText('Observed rate')).toBeInTheDocument();
    expect(screen.getByText('6.00%')).toBeInTheDocument();
    expect(screen.getByText('60.0 / min')).toBeInTheDocument();
    expect(screen.getByText('Incident windows')).toBeInTheDocument();
    expect(screen.getByText('Service relationships')).toBeInTheDocument();
    expect(screen.getByText('Investigation')).toBeInTheDocument();
    expect(screen.getByText('Services named by detector')).toBeInTheDocument();
    expect(screen.getByText(/does not establish a root cause/)).toBeInTheDocument();
    expect(screen.getByText('Event signals')).toBeInTheDocument();
    expect(screen.getByText('Activity rhythm')).toBeInTheDocument();
    expect(screen.getByText('Recurring patterns')).toBeInTheDocument();
  });

  it('labels the available SSE channel as bounded dataset replay and keeps pattern log links', async () => {
    vi.spyOn(api, 'overview').mockResolvedValue(overview);
    vi.spyOn(api, 'dependencies').mockResolvedValue(dependencies);
    vi.spyOn(api, 'services').mockResolvedValue([service]);
    vi.spyOn(api, 'incidents').mockResolvedValue([incident]);
    vi.spyOn(api, 'systemStatus').mockResolvedValue(health);
    vi.spyOn(api, 'liveStatus').mockResolvedValue({ enabled: true, dataset: 'sample', total: 400, source: 'demo-replay', label: 'Ready' });
    renderPage();

    expect(await screen.findByText('REPLAY READY')).toBeInTheDocument();
    expect(screen.getByLabelText('Bounded SSE replay of this dataset; not live production telemetry')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /request <\*> failed/ })).toHaveAttribute('href', '/logs?q=request%20failed');
  });

  it('shows source selection actions instead of presenting empty metrics', async () => {
    vi.spyOn(api, 'overview').mockResolvedValue(null);
    vi.spyOn(api, 'dependencies').mockResolvedValue({ nodes: [], edges: [], nodeCount: 0, edgeCount: 0 });
    vi.spyOn(api, 'services').mockResolvedValue([]);
    vi.spyOn(api, 'incidents').mockResolvedValue([]);
    vi.spyOn(api, 'systemStatus').mockResolvedValue({ ...health, datasetLoaded: false, datasetName: null, datasetSize: 0 });
    vi.spyOn(api, 'liveStatus').mockResolvedValue({ enabled: false, dataset: null, total: 0, source: null, label: 'Standby' });
    renderPage();

    expect(await screen.findByText('No investigation data yet')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Choose a source' })).toHaveLength(2);
    expect(screen.getByRole('link', { name: 'Upload logs' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open dataset replay' })).toBeInTheDocument();
  });
});
