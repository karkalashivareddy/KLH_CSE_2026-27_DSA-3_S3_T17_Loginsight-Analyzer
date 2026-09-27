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

  it('keeps supporting routes under an accessible More control', async () => {
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
    expect(within(navigation).getByRole('link', { name: 'Overview' })).toBeInTheDocument();
    expect(within(navigation).getByRole('link', { name: 'Detector Windows' })).toBeInTheDocument();
  expect(within(navigation).getByRole('link', { name: 'Incident Workbench' })).toBeInTheDocument();
  expect(within(navigation).getByRole('link', { name: 'Scenario Lab' })).toBeInTheDocument();
  expect(within(navigation).getByRole('link', { name: 'Live Monitor' })).toBeInTheDocument();
    expect(within(navigation).queryByRole('link', { name: 'Benchmarks' })).toBeNull();
    fireEvent.click(within(navigation).getByRole('button', { name: /^More/ }));
    expect(within(navigation).getByRole('link', { name: 'Benchmarks' })).toBeInTheDocument();
    expect(within(navigation).getByRole('link', { name: 'Datasets' })).toBeInTheDocument();
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
