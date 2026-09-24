import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import { Link } from 'react-router-dom';
import type { SearchBenchmarkResponse } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState } from '../components/ui';
import { formatNumber } from '../components/format';

/**
 * Search benchmark (GET /api/analysis/benchmarks/search). Each of the four string matchers runs
 * once over the exact same dataset haystack; every time is measured on that run and the winner is
 * simply the fastest measurement — no fabricated averages.
 */
export default function BenchmarksPage() {
  const [input, setInput] = useState('');
  const [pattern, setPattern] = useState<string | null>(null);
  const { data, loading, error, reload } = useApi<SearchBenchmarkResponse | null>(
    () => pattern ? api.searchBenchmark(pattern) : Promise.resolve(null),
    pattern ?? ''
  );

  const run = () => {
    const p = input.trim();
    if (p) setPattern(p);
  };

  return (
    <div className="page">
      <h2 className="page-title">Search Benchmarks</h2>

      <Card title="Benchmark Setup" sub="one measured execution per matcher">
        <div className="explorer-filters">
          <input
            className="input"
            type="text"
            placeholder="Pattern to search for in the dataset haystack… e.g. payment"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && run()}
          />
          <button className="btn btn-run" onClick={run} disabled={!input.trim()}>Run benchmark</button>
          <Link className="btn" to="/search">Try product search instead ›</Link>
        </div>
      </Card>

      {loading ? (
        <Spinner label="Running matchers…" />
      ) : error ? (
        <ErrorBox error={error} retry={reload} />
      ) : !data ? (
        <EmptyState>Enter a pattern and run the benchmark — all four matchers execute over the loaded dataset.</EmptyState>
      ) : (
        <>
          <Card title="Benchmark Context">
            <table className="info-table">
              <tbody>
                <tr><th>Problem</th><td>{data.problem}</td></tr>
                <tr><th>Dataset</th><td>{data.dataset}</td></tr>
                <tr><th>Pattern</th><td><code>"{data.pattern}"</code></td></tr>
                <tr><th>Haystack length</th><td>{formatNumber(data.textLength)} characters</td></tr>
              </tbody>
            </table>
            <p className="muted">Haystack preview: <code className="log-snippet">{data.textPreview}</code></p>
          </Card>

          <Card title={`Measurements — winner: ${data.winner}`}>
            <table className="log-table">
              <thead>
                <tr><th>Algorithm</th><th>Match count</th><th>Time (ms)</th><th>Time complexity</th><th>Space complexity</th></tr>
              </thead>
              <tbody>
                {data.results.map((r) => (
                  <tr key={r.algorithm} className={r.algorithm === data.winner ? 'winner-row' : ''}>
                    <td>
                      {r.algorithm === data.winner && <span className="winner-tag">WINNER</span>}
                      {r.algorithm}
                    </td>
                    <td>{r.matchCount}</td>
                    <td>{r.timeMs.toFixed(3)}</td>
                    <td>{r.timeComplexity}</td>
                    <td>{r.spaceComplexity}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="note-line">{data.note}</div>
          </Card>
        </>
      )}
    </div>
  );
}