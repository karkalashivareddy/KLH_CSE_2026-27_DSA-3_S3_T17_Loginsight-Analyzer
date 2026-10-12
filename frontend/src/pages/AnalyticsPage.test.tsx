import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import type { Heatmap, HostRow, HttpStatsDto, OverviewDto } from '../api/types';
import AnalyticsPage from './AnalyticsPage';

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
  activeIncidents: 1,
  timeline: [{ start: '2026-09-25T09:00:00Z', end: '2026-09-25T09:30:00Z', count: 50 }],
  range: '1h',
  severity: { ERROR: 6, INFO: 94 },
  topServices: [],
  topPatterns: [],
  recentCritical: [],
  heatmap: { days: ['Mon'], columns: 2, cells: [[1, 2]] },
  statusCodes: { '200': 100 }
};

const heatmap: Heatmap = { days: ['Mon'], columns: 2, cells: [[1, 2]] };
const http: HttpStatsDto = { statusCodes: { '200': 10 }, methods: { GET: 10 }, endpoints: [{ endpoint: '/events', count: 10 }], latencyP50: 4, latencyP95: 9, latencyMax: 12, sampled: 10 };
const hosts: HostRow[] = [{ host: 'host-1', events: 10, errors: 1, warnings: 1, errorRate: 10 }];

function mockSources() {
  vi.spyOn(api, 'overview').mockResolvedValue(overview);
  vi.spyOn(api, 'heatmap').mockResolvedValue(heatmap);
  vi.spyOn(api, 'httpAnalytics').mockResolvedValue(http);
  vi.spyOn(api, 'hostAnalytics').mockResolvedValue(hosts);
}

describe('Analytics tabs', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('labels each tab panel with the selected tab and keeps the tablist association', async () => {
    mockSources();
    render(<MemoryRouter><AnalyticsPage /></MemoryRouter>);

    const tablist = screen.getByRole('tablist', { name: 'Analytics views' });
    expect(tablist).toBeInTheDocument();
    const timelineTab = screen.getByRole('tab', { name: 'Timeline' });
    expect(timelineTab).toHaveAttribute('aria-selected', 'true');
    expect(timelineTab).toHaveAttribute('aria-controls', 'analytics-panel-timeline');
    const panel = screen.getByRole('tabpanel', { name: 'Timeline' });
    expect(panel).toHaveAttribute('id', 'analytics-panel-timeline');
    expect(panel).toHaveAttribute('aria-labelledby', timelineTab.getAttribute('id'));

    fireEvent.click(screen.getByRole('tab', { name: 'HTTP' }));

    const httpTab = screen.getByRole('tab', { name: 'HTTP' });
    expect(httpTab).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: 'Timeline' })).toHaveAttribute('aria-selected', 'false');
    const httpPanel = screen.getByRole('tabpanel', { name: 'HTTP' });
    expect(httpPanel).toHaveAttribute('id', 'analytics-panel-http');
    expect(httpPanel).toHaveAttribute('aria-labelledby', httpTab.getAttribute('id'));
    expect(await screen.findByText('Top HTTP endpoints')).toBeInTheDocument();
  });
});
