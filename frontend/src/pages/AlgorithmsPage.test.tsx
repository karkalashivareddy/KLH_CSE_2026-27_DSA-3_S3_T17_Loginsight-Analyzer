import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/client';
import type { AlgorithmGroup, RunRecord } from '../api/types';
import AlgorithmsPage from './AlgorithmsPage';

const kmp: AlgorithmGroup['algorithms'][number] = {
  key: 'kmp',
  name: 'KMP Pattern Search',
  problem: 'Linear-time pattern search with a failure function',
  queryType: 'PATTERN_SEARCH',
  algorithmType: 'KMP',
  canonicalEndpoint: '/api/search/kmp',
  traceEndpoint: '/api/trace/search/kmp',
  timeComplexity: 'O(n+m)',
  spaceComplexity: 'O(m) LPS',
  tracked: true,
  defaultInput: { text: 'ABABABCABABABC', pattern: 'ABABC' },
  description: 'Knuth-Morris-Pratt with a hand-built LPS failure function.'
};

const perfectHash: AlgorithmGroup['algorithms'][number] = {
  key: 'perfect_hash',
  name: 'Two-Level Perfect Hashing',
  problem: 'Worst-case O(1) membership',
  queryType: 'HASH',
  algorithmType: 'UNIVERSAL_HASH',
  canonicalEndpoint: null,
  traceEndpoint: null,
  timeComplexity: 'O(1) worst case',
  spaceComplexity: 'O(n)',
  tracked: false,
  defaultInput: null,
  description: 'FKS-style two-level scheme with expected-linear build.'
};

const groups: AlgorithmGroup[] = [{ module: 'Strings', algorithms: [kmp, perfectHash] }];

const run: RunRecord = {
  runId: 'run-42',
  algorithm: 'kmp',
  algorithmName: 'KMP Pattern Search',
  category: 'Strings',
  status: 'COMPLETED',
  createdAt: '2026-09-25T10:00:00Z',
  completedAt: '2026-09-25T10:00:01Z',
  stepCount: 9,
  executionTimeNanos: 1000,
  truncated: false,
  timeComplexity: 'O(n+m)',
  spaceComplexity: 'O(m) LPS',
  input: kmp.defaultInput ?? {},
  result: { matchCount: 1 },
  steps: [],
  error: null
};

function RunSessionStub() {
  const { id } = useParams<{ id: string }>();
  return <div>Run session {id}</div>;
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/algorithms']}>
      <Routes>
        <Route path="/algorithms" element={<AlgorithmsPage />} />
        <Route path="/runs/:id" element={<RunSessionStub />} />
      </Routes>
    </MemoryRouter>
  );
}

async function selectAlgorithm(name: string) {
  const [inspect] = await screen.findAllByRole('button', { name: `Inspect ${name}` });
  fireEvent.click(inspect);
}

describe('Algorithm Lab run path', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('posts the catalogue default input to the runs API and opens the created session', async () => {
    const runMock = vi.spyOn(api, 'run').mockResolvedValue(run);
    vi.spyOn(api, 'algorithmGroups').mockResolvedValue(groups);

    renderPage();

    await selectAlgorithm('KMP Pattern Search');
    fireEvent.click(screen.getByRole('button', { name: 'Run KMP Pattern Search with its catalogue input' }));

    expect(runMock).toHaveBeenCalledWith('kmp', kmp.defaultInput);
    expect(await screen.findByText('Run session run-42')).toBeInTheDocument();
  });

  it('shows the catalogue run input and offers no run control without one', async () => {
    const runMock = vi.spyOn(api, 'run').mockResolvedValue(run);
    vi.spyOn(api, 'algorithmGroups').mockResolvedValue(groups);

    renderPage();

    await selectAlgorithm('KMP Pattern Search');
    expect(screen.getByText('Run input (catalogue default)')).toBeInTheDocument();
    expect(screen.getByText(/"pattern": "ABABC"/)).toBeInTheDocument();

    await selectAlgorithm('Two-Level Perfect Hashing');
    expect(screen.queryByRole('button', { name: 'Run Two-Level Perfect Hashing with its catalogue input' })).not.toBeInTheDocument();
    expect(runMock).not.toHaveBeenCalled();
  });

  it('surfaces a rejected run instead of navigating', async () => {
    vi.spyOn(api, 'run').mockRejectedValue(new Error('kmp is not trace-instrumented'));
    vi.spyOn(api, 'algorithmGroups').mockResolvedValue(groups);

    renderPage();

    await selectAlgorithm('KMP Pattern Search');
    fireEvent.click(screen.getByRole('button', { name: 'Run KMP Pattern Search with its catalogue input' }));

    expect(await screen.findByText('kmp is not trace-instrumented')).toBeInTheDocument();
    expect(screen.queryByText('Run session run-42')).not.toBeInTheDocument();
  });
});
