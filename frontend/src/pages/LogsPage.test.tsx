import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import type { LogEvent, LogSearchResponse } from '../api/types';
import LogsPage from './LogsPage';

const event: LogEvent = {
  id: 42,
  timestamp: '2026-09-26T10:00:00Z',
  severityNumber: 17,
  level: 'ERROR',
  service: 'payment-service',
  host: 'payment-1',
  ipAddress: '127.0.0.1',
  httpMethod: 'POST',
  endpoint: '/charge',
  statusCode: 504,
  responseTime: 482,
  requestId: 'req-42',
  userId: '',
  message: 'payment timeout',
  traceId: 'trace-42',
  spanId: 'span-42',
  url: null,
  source: 'test',
  rawMessage: null,
  attributes: {}
};

const response: LogSearchResponse = {
  query: 'level:ERROR',
  strategy: 'Structured filters only',
  algorithm: '',
  pattern: '',
  patternLength: 0,
  textSize: 1,
  durationNanos: 1_200_000,
  total: 1,
  page: 1,
  size: 25,
  sort: 'timestamp:desc',
  dataset: 'sample',
  matches: [{ event, snippet: '', matchCount: 0 }],
  suggestion: null
};

describe('Log explorer', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('applies backend severity syntax and opens the selected event drawer', async () => {
    const explore = vi.spyOn(api, 'explore').mockResolvedValue(response);
    render(<MemoryRouter><LogsPage /></MemoryRouter>);

    expect(await screen.findByText('payment timeout')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Add level ERROR filter' }));
    fireEvent.click(screen.getByRole('button', { name: /^Search$/ }));

    await waitFor(() => expect(explore).toHaveBeenLastCalledWith(expect.objectContaining({ query: 'level:ERROR', page: 1 }), expect.anything()));
    fireEvent.click(screen.getByRole('button', { name: 'Inspect event 42: payment timeout' }));
    expect(await screen.findByRole('heading', { name: '#42 · payment-service' })).toBeInTheDocument();
    expect(screen.getByText('trace-42')).toBeInTheDocument();
  });
});
