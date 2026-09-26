import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import type { LiveBatch, LiveStartEvent, LogEvent } from '../api/types';
import { ReplayProvider } from '../replay/ReplayContext';
import LivePage from './LivePage';

const event: LogEvent = {
  id: 21,
  timestamp: '2026-09-25T10:00:00Z',
  severityNumber: 9,
  level: 'ERROR',
  service: 'api',
  host: 'host-1',
  ipAddress: '127.0.0.1',
  httpMethod: 'GET',
  endpoint: '/events',
  statusCode: 500,
  responseTime: 4,
  requestId: 'request-21',
  userId: 'user-21',
  message: 'request failed',
  traceId: null,
  spanId: null,
  url: null,
  source: 'test',
  rawMessage: null,
  attributes: {}
};

const startEvent: LiveStartEvent = {
  source: 'demo-replay',
  dataset: 'sample',
  total: 1,
  batchSize: 100,
  paceMs: 1500,
  label: 'demo-replay'
};

function renderPage() {
  return render(<MemoryRouter><ReplayProvider><LivePage /></ReplayProvider></MemoryRouter>);
}

describe('Live Replay', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('discloses the demo replay surface and uses the shared status label', async () => {
    vi.spyOn(api, 'liveStatus').mockResolvedValue({ enabled: true, dataset: 'sample', total: 1, source: 'sample', label: 'Ready' });
    vi.spyOn(api, 'liveStream').mockReturnValue(vi.fn());

    renderPage();

    expect(await screen.findByText('DEMO REPLAY')).toBeInTheDocument();
    expect(screen.getByLabelText('Demo replay of the loaded dataset, not live production telemetry')).toBeInTheDocument();
    expect(screen.getByText(/Demo replay: a bounded SSE replay of the loaded dataset/)).toBeInTheDocument();
    expect(screen.getByText('Replay state: Ready')).toBeInTheDocument();
  });

  it('starts and replays again with the selected batch size and pace', async () => {
    vi.spyOn(api, 'liveStatus').mockResolvedValue({ enabled: true, dataset: 'sample', total: 1, source: 'sample', label: 'Ready' });
    let handlers: Parameters<typeof api.liveStream>[0] | undefined;
    const liveStream = vi.spyOn(api, 'liveStream').mockImplementation((next) => {
      handlers = next;
      return vi.fn();
    });

    renderPage();
    await waitFor(() => expect(screen.getByRole('button', { name: /Start replay/ })).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText('Batch size'), { target: { value: '100' } });
    fireEvent.change(screen.getByLabelText('Pace'), { target: { value: '1500' } });
    fireEvent.click(screen.getByRole('button', { name: /Start replay/ }));

    expect(liveStream).toHaveBeenCalledTimes(1);
    expect(liveStream.mock.calls[0]?.[1]).toEqual({ batchSize: 100, intervalMs: 1500 });

    handlers?.onStart?.(startEvent);
    handlers?.onBatch?.({ sequence: 1, source: 'demo-replay', dataset: 'sample', emittedCount: 1, total: 1, events: [event] } satisfies LiveBatch);
    handlers?.onComplete?.({ emitted: 1, total: 1 });

    const replayAgain = await screen.findByRole('button', { name: /Replay again/ });
    fireEvent.click(replayAgain);

    expect(liveStream).toHaveBeenCalledTimes(2);
    expect(liveStream.mock.calls[1]?.[1]).toEqual({ batchSize: 100, intervalMs: 1500 });
  });
});
