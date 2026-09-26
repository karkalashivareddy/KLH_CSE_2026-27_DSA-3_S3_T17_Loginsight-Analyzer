import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import type { LiveBatch, LiveStartEvent, LogEvent } from '../api/types';
import { ReplayProvider, useReplay } from './ReplayContext';

const event: LogEvent = {
  id: 7,
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
  requestId: 'request-7',
  userId: 'user-7',
  message: 'failed request',
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
  batchSize: 1,
  paceMs: 100,
  label: 'demo-replay'
};

function Probe() {
  const replay = useReplay();
  return <>
    <output data-testid="state">{replay.state}</output>
    <output data-testid="dataset">{replay.currentDataset ?? 'none'}</output>
    <output data-testid="progress">{replay.emitted}/{replay.total}</output>
    <output data-testid="recent">{replay.recentEvents.length}</output>
    <button type="button" onClick={() => replay.start({ batchSize: 1, intervalMs: 100 })}>Start</button>
    <button type="button" onClick={replay.stop}>Stop</button>
    <button type="button" onClick={() => replay.restart()}>Restart</button>
    <button type="button" onClick={() => replay.restart({ batchSize: 2, intervalMs: 300 })}>Restart with options</button>
    <output data-testid="options">{replay.batchSize}/{replay.intervalMs}</output>
  </>;
}

describe('ReplayContext', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('owns one SSE subscription and exposes replay progress and events', async () => {
    vi.spyOn(api, 'liveStatus').mockResolvedValue({ enabled: true, dataset: 'sample', total: 1, source: 'sample', label: 'Ready' });
    let handlers: Parameters<typeof api.liveStream>[0] | undefined;
    const unsubscribe = vi.fn();
    const liveStream = vi.spyOn(api, 'liveStream').mockImplementation((next) => {
      handlers = next;
      return unsubscribe;
    });

    render(<ReplayProvider><Probe /></ReplayProvider>);
    await waitFor(() => expect(screen.getByTestId('dataset')).toHaveTextContent('sample'));

    fireEvent.click(screen.getByRole('button', { name: 'Start' }));
    expect(liveStream).toHaveBeenCalledOnce();
    expect(screen.getByTestId('state')).toHaveTextContent('starting');

    act(() => {
      handlers?.onStart?.(startEvent);
      handlers?.onBatch?.({ sequence: 1, source: 'demo-replay', dataset: 'sample', emittedCount: 1, total: 1, events: [event] } satisfies LiveBatch);
    });
    expect(screen.getByTestId('state')).toHaveTextContent('streaming');
    expect(screen.getByTestId('dataset')).toHaveTextContent('sample');
    expect(screen.getByTestId('progress')).toHaveTextContent('1/1');
    expect(screen.getByTestId('recent')).toHaveTextContent('1');

    act(() => handlers?.onComplete?.({ emitted: 1, total: 1 }));
    expect(screen.getByTestId('state')).toHaveTextContent('complete');

    fireEvent.click(screen.getByRole('button', { name: 'Stop' }));
    expect(unsubscribe).toHaveBeenCalledOnce();
    expect(screen.getByTestId('state')).toHaveTextContent('complete');
  });

  it('restarts a finished replay with the options it is given', async () => {
    vi.spyOn(api, 'liveStatus').mockResolvedValue({ enabled: true, dataset: 'sample', total: 1, source: 'sample', label: 'Ready' });
    const liveStream = vi.spyOn(api, 'liveStream').mockReturnValue(vi.fn());

    render(<ReplayProvider><Probe /></ReplayProvider>);
    await waitFor(() => expect(screen.getByTestId('dataset')).toHaveTextContent('sample'));

    fireEvent.click(screen.getByRole('button', { name: 'Start' }));
    fireEvent.click(screen.getByRole('button', { name: 'Restart with options' }));

    expect(liveStream).toHaveBeenCalledTimes(2);
    expect(liveStream.mock.calls[1]?.[1]).toEqual({ batchSize: 2, intervalMs: 300 });
    expect(screen.getByTestId('options')).toHaveTextContent('2/300');
  });
});
