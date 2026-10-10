import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { GuidedDemoProvider, useGuidedDemo } from './GuidedDemo';

function Launcher() {
  const demo = useGuidedDemo();
  return <button type="button" onClick={demo.start}>Start demo</button>;
}

function response(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

function renderDemo() {
  return render(<MemoryRouter initialEntries={['/']}><GuidedDemoProvider><Launcher /><Routes><Route path="*" element={<div>Product route</div>} /></Routes></GuidedDemoProvider></MemoryRouter>);
}

describe('guided product demo', () => {
  beforeEach(() => vi.restoreAllMocks());
  afterEach(() => cleanup());

  it('walks real backend evidence through search and exits with Escape', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input);
      if (url.includes('/datasets/current')) return response({ loaded: true, datasetName: 'sample.jsonl', size: 42, totalLines: 45, failedLines: 3 });
      if (url.endsWith('/api/search')) return response({
        total: 3, algorithm: 'KMP', durationNanos: 2500, query: 'level:ERROR', strategy: 'INDEX_AND_KMP',
        pattern: 'ERROR', patternLength: 5, textSize: 42, page: 1, size: 5, sort: 'timestamp:desc', dataset: 'sample.jsonl', matches: [], suggestion: null
      });
      throw new Error(`Unexpected guided demo request: ${url}`);
    });

    renderDemo();
    fireEvent.click(screen.getByRole('button', { name: 'Start demo' }));
    expect(screen.getByRole('dialog', { name: /From raw events/ })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Begin walkthrough/ }));
    expect(await screen.findByText(/Product explanation/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Next step/ }));
    expect(await screen.findByText(/sample\.jsonl · 42 parsed events/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Next step/ }));
    expect(await screen.findByText(/45 input lines · 3 rejected lines/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Next step/ }));
    expect(await screen.findByText(/3 matching ERROR events · KMP/)).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith('/api/search', expect.objectContaining({ method: 'POST', signal: expect.any(AbortSignal), cache: 'no-store', body: expect.stringContaining('level:ERROR') }));

    fireEvent.keyDown(document, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('dialog', { name: /From raw events|The investigation problem/ })).not.toBeInTheDocument());
  });

  it('surfaces API errors, retries the actual request, and keeps exit available', async () => {
    let searchAttempts = 0;
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input);
      if (url.includes('/datasets/current')) return response({ loaded: true, datasetName: 'sample.jsonl', size: 42 });
      if (url.endsWith('/api/search')) {
        searchAttempts += 1;
        if (searchAttempts === 1) return response({ message: 'Search unavailable' }, 503);
        return response({ total: 0, algorithm: 'KMP', durationNanos: 10, query: 'level:ERROR', strategy: 'INDEX_AND_KMP', pattern: 'ERROR', patternLength: 5, textSize: 42, page: 1, size: 5, sort: 'timestamp:desc', dataset: 'sample.jsonl', matches: [], suggestion: null });
      }
      throw new Error(`Unexpected guided demo request: ${url}`);
    });

    renderDemo();
    fireEvent.click(screen.getByRole('button', { name: 'Start demo' }));
    fireEvent.click(screen.getByRole('button', { name: /Begin walkthrough/ }));
    await screen.findByText(/Product explanation/);
    fireEvent.click(screen.getByRole('button', { name: /Next step/ }));
    await screen.findByText(/sample\.jsonl · 42 parsed events/);
    fireEvent.click(screen.getByRole('button', { name: /Next step/ }));
    await screen.findByText(/42 accepted events/);
    fireEvent.click(screen.getByRole('button', { name: /Next step/ }));
    expect(await screen.findByText(/Search unavailable/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Retry this step/ }));
    expect(await screen.findByText(/0 matching ERROR events · KMP/)).toBeInTheDocument();
    expect(searchAttempts).toBe(2);
    fireEvent.keyDown(document, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('dialog', { name: /From raw events|The investigation problem/ })).not.toBeInTheDocument());
  });
});
