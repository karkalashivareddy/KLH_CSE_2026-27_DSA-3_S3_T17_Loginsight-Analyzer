import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import type { LogEvent, PatternDto } from '../api/types';
import PatternsPage from './PatternsPage';

const patterns: PatternDto[] = [
  { template: 'request <*> failed', count: 6, example: 'request failed for order 42', level: 'ERROR' }
];

const example: LogEvent = {
  id: 5,
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
  requestId: 'request-5',
  userId: 'user-5',
  message: 'request failed for order 42',
  traceId: null,
  spanId: null,
  url: null,
  source: 'test',
  rawMessage: null,
  attributes: {}
};

describe('Patterns evidence links', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('searches logs for the returned example message instead of the wildcard template', async () => {
    vi.spyOn(api, 'patterns').mockResolvedValue(patterns);
    vi.spyOn(api, 'patternExamples').mockResolvedValue([example]);

    render(<MemoryRouter><PatternsPage /></MemoryRouter>);

    fireEvent.click(await screen.findByRole('button', { name: 'Inspect examples for request <*> failed' }));

    const link = await screen.findByRole('link', { name: 'Search logs for the example message of request <*> failed' });
    expect(link).toHaveAttribute('href', '/logs?q=request%20failed%20for%20order%2042');
    expect(link.getAttribute('href')).not.toContain('%3C');
  });
});
