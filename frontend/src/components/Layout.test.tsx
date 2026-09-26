import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { api } from '../api/client';
import type { LiveStatus, SystemStatus } from '../api/types';
import Layout from './Layout';

const systemStatus: SystemStatus = {
  status: 'UP',
  service: 'loginsight',
  timestamp: '2026-01-01T00:00:00Z',
  uptimeMillis: 1_000,
  datasetLoaded: true,
  datasetName: 'sample',
  datasetSize: 10,
  engines: 4
};

const liveStatus: LiveStatus = {
  enabled: true,
  dataset: 'sample',
  total: 10,
  source: 'sample',
  label: 'Ready'
};

describe('application shell', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('exposes skip navigation, route breadcrumbs, and active navigation', async () => {
    vi.spyOn(api, 'systemStatus').mockResolvedValue(systemStatus);
    vi.spyOn(api, 'liveStatus').mockResolvedValue(liveStatus);

    render(
      <MemoryRouter initialEntries={['/logs/42']}>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/logs/:id" element={<div>Event detail</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByRole('link', { name: 'Skip to main content' })).toHaveAttribute('href', '#main-content');
    const workspaceNavigation = screen.getByRole('navigation', { name: 'Workspace sections' });
    expect(workspaceNavigation).toBeInTheDocument();
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content');
    expect(within(workspaceNavigation).getByRole('link', { name: 'Log Explorer' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByText('Event #42')).toBeInTheDocument();
    await screen.findByText('Connected');
  });

  it('does not claim no dataset or standby while status is still loading', () => {
    vi.spyOn(api, 'systemStatus').mockReturnValue(new Promise<SystemStatus>(() => undefined));
    vi.spyOn(api, 'liveStatus').mockReturnValue(new Promise<LiveStatus>(() => undefined));

    render(
      <MemoryRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<div>Command Center</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByRole('link', { name: 'Dataset: Checking' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Demo replay: Checking' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Dataset: No dataset' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Demo replay: Standby' })).toBeNull();
    expect(screen.getAllByText('Checking status')).toHaveLength(2);
    expect(screen.getByText('Demo replay channel checking')).toBeInTheDocument();
  });
});
