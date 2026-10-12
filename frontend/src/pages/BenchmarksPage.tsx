import { useState } from 'react';
import { Gauge, RefreshCw, Timer } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { SearchBenchmarkResponse } from '../api/types';
import { Badge, Card, EmptyState, ErrorBox, NoDatasetState, PageHeader, Spinner, StatCard } from '../components/ui';
import { formatNanos, formatNumber } from '../components/format';

function isMissingDataset(error: Error): boolean {
  const status = (error as Error & { apiError?: { status?: number } }).apiError?.status;
  return status === 404;
}

export default function BenchmarksPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialPattern = searchParams.get('pattern') ?? '';
  const [input, setInput] = useState(initialPattern);
  const [pattern, setPattern] = useState<string | null>(initialPattern || null);
  const { data, loading, refreshing, error, reload } = useApi<SearchBenchmarkResponse | null>((signal) => pattern ? api.searchBenchmark(pattern, { signal }) : Promise.resolve(null), pattern ?? '');

  const run = () => {
    const next = input.trim();
    if (!next) return;
    setPattern(next);
    setSearchParams({ pattern: next }, { replace: true });
  };

  return (
    <div className="page">
      <PageHeader eyebrow="Analysis" title="Benchmarks" description="One real execution per matcher over the same dataset haystack. Winner is the smallest measured duration." actions={data && <button className="btn btn-sm" type="button" onClick={reload} disabled={refreshing}><RefreshCw size={14} aria-hidden="true" /> Run again</button>} />
      <Card title="Benchmark setup" sub="The pattern is sent to the backend; no timing is generated in the browser.">
        <div className="explorer-filters"><label className="sr-only" htmlFor="benchmark-pattern">Pattern</label><input id="benchmark-pattern" className="input" type="text" placeholder="Pattern in the loaded dataset…" value={input} onChange={(event) => setInput(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && run()} /><button className="btn btn-run" type="button" onClick={run} disabled={!input.trim()}><Timer size={14} aria-hidden="true" /> Run benchmark</button><Link className="btn" to={pattern ? `/search?q=${encodeURIComponent(pattern)}` : '/search'}>Product search</Link></div>
      </Card>
      {!pattern ? <EmptyState><Gauge size={23} aria-hidden="true" /><strong>Enter a pattern to run a measured comparison.</strong><span>All matcher results will come from one backend request over the loaded dataset haystack.</span><Link className="btn btn-sm" to="/datasets">Open datasets</Link></EmptyState> : loading ? <Card title="Running benchmark"><Spinner label="Running each matcher on the backend…" /></Card> : error ? isMissingDataset(error) ? <NoDatasetState detail="Load a dataset before running a benchmark." /> : <ErrorBox error={error} retry={reload} /> : !data ? <Card title="Benchmark"><EmptyState>No benchmark response is available.</EmptyState></Card> : data.results.length === 0 ? <Card title="Measurements"><EmptyState>No matcher measurements were returned for this pattern.</EmptyState></Card> : <>
        <div className="stat-grid stat-grid--small">
          <StatCard label="Fastest matcher" value={data.winner} color="var(--ok)" />
          <StatCard label="Pattern length" value={formatNumber(data.pattern.length)} />
          <StatCard label="Haystack" value={`${formatNumber(data.textLength)} chars`} color="var(--accent)" />
          <StatCard label="Dataset" value={data.dataset} />
        </div>
        <Card title="Benchmark context" sub="All fields are returned by the measured benchmark endpoint.">
          <div className="table-scroll"><table className="info-table"><caption className="sr-only">Benchmark context</caption><tbody><tr><th>Problem</th><td>{data.problem}</td></tr><tr><th>Dataset</th><td>{data.dataset}</td></tr><tr><th>Pattern</th><td><code>{data.pattern}</code></td></tr><tr><th>Haystack length</th><td>{formatNumber(data.textLength)} characters</td></tr><tr><th>Methodology</th><td>{data.note}</td></tr></tbody></table></div>
          <p className="muted">Returned haystack preview:</p><code className="log-snippet">{data.textPreview || 'No preview returned'}</code>
        </Card>
        <Card title="Measurements" sub="One measured execution per matcher" actions={refreshing ? <Badge tone="info">Refreshing</Badge> : <Badge tone="good">Measured server-side</Badge>}>
          <div className="table-scroll"><table className="log-table"><caption className="sr-only">Measured matcher benchmark results</caption><thead><tr><th>Algorithm</th><th>Matches</th><th>Duration</th><th>Reported ms</th><th>Time complexity</th><th>Space complexity</th></tr></thead><tbody>{data.results.map((row) => <tr key={row.algorithm} className={row.algorithm === data.winner ? 'winner-row' : undefined}><td>{row.algorithm === data.winner && <span className="winner-tag">WINNER</span>}<strong>{row.algorithm}</strong></td><td className="num">{formatNumber(row.matchCount)}</td><td className="num">{formatNanos(row.timeNanos)}</td><td className="num">{row.timeMs.toFixed(3)} ms</td><td className="mono">{row.timeComplexity}</td><td className="mono">{row.spaceComplexity}</td></tr>)}</tbody></table></div>
        </Card>
      </>}
    </div>
  );
}
