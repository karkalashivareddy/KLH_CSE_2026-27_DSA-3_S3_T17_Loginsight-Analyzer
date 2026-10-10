import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GuidedDemoProvider, useGuidedDemo } from './GuidedDemo';

function Launcher() {
  const demo = useGuidedDemo();
  return <button type="button" onClick={demo.start}>Start demo</button>;
}

function response(body: unknown): Response {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}

describe('guided product demo', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('uses the active dataset, executes backend search, and exits with Escape', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input);
      if (url.includes('/datasets/current')) return response({ loaded: true, datasetName: 'sample.jsonl', size: 42 });
      if (url.endsWith('/api/search')) return response({
        total: 3, algorithm: 'KMP', durationNanos: 2500, query: 'level:ERROR', strategy: 'INDEX_AND_KMP',
        pattern: 'ERROR', patternLength: 5, textSize: 42, page: 1, size: 5, sort: 'timestamp:desc', dataset: 'sample.jsonl', matches: [], suggestion: null
      });
      throw new Error(`Unexpected guided demo request: ${url}`);
    });

    render(<MemoryRouter initialEntries={['/']}><GuidedDemoProvider><Launcher /><Routes><Route path="*" element={<div>Product route</div>} /></Routes></GuidedDemoProvider></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: 'Start demo' }));
    expect(await screen.findByText(/sample\.jsonl · 42 parsed events/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Next step/ }));
    expect(await screen.findByText(/3 matching events · KMP/)).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith('/api/search', expect.objectContaining({ method: 'POST', signal: expect.any(AbortSignal), cache: 'no-store', body: expect.stringContaining('level:ERROR') }));

    fireEvent.keyDown(document, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('complementary', { name: 'Guided product demonstration' })).not.toBeInTheDocument());
  });
});
