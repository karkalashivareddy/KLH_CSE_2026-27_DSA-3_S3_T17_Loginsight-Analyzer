import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
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
    cleanup();
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
    expect(within(workspaceNavigation).getByRole('link', { name: 'Logs' })).toHaveAttribute('aria-current', 'page');
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

    expect(screen.getByRole('link', { name: 'Source: Checking' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Replay: Checking' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Source: No dataset' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Replay: Standby' })).toBeNull();
    expect(screen.getByText(/Backend checking/)).toBeInTheDocument();
  });

it('keeps the primary workflow compact and specialist routes under Advanced', async () => {
    vi.spyOn(api, 'systemStatus').mockResolvedValue(systemStatus);
    vi.spyOn(api, 'liveStatus').mockResolvedValue(liveStatus);

    render(
      <MemoryRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<div>Home</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    const navigation = screen.getByRole('navigation', { name: 'Workspace sections' });
    // The five daily destinations are visible without opening anything.
    for (const label of ['Overview', 'Logs', 'Incidents', 'Services', 'Analytics']) {
      expect(within(navigation).getByRole('link', { name: label })).toBeInTheDocument();
    }
    // Specialist tools stay reachable, just collapsed.
    expect(within(navigation).queryByRole('link', { name: 'Benchmarks' })).toBeNull();
    expect(within(navigation).queryByRole('link', { name: 'Algorithm Lab' })).toBeNull();

    fireEvent.click(within(navigation).getByRole('button', { name: /^Advanced/ }));
    for (const label of ['Incident Workbench', 'Live Monitor', 'Dataset Replay', 'Scenario Lab', 'Patterns', 'Algorithm Lab', 'Algorithmic Search', 'Algorithms', 'Benchmarks', 'Run Sessions', 'Datasets', 'Ingestion', 'System', 'Documentation']) {
      expect(within(navigation).getByRole('link', { name: label }), `${label} should be reachable under Advanced`).toBeInTheDocument();
    }
    // Every route is still declared; grouping must not drop one.
    expect(within(navigation).getAllByRole('link')).toHaveLength(19);
  });

  it('opens the command palette with Ctrl+K and restores focus after Escape', async () => {
    vi.spyOn(api, 'systemStatus').mockResolvedValue(systemStatus);
    vi.spyOn(api, 'liveStatus').mockResolvedValue(liveStatus);

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<div>Home</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    const trigger = screen.getByRole('button', { name: 'Open command palette' });
    trigger.focus();
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
    const dialog = screen.getByRole('dialog', { name: 'Move through LogInsight' });
    expect(within(dialog).getByRole('option', { name: /Overview/ })).toBeInTheDocument();
    fireEvent.keyDown(document, { key: 'Escape' });

    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Move through LogInsight' })).toBeNull());
    await waitFor(() => expect(trigger).toHaveFocus());
  });
});
