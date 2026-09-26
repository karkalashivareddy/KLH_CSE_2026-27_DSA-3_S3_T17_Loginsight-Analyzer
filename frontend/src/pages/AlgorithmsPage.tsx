import { useCallback, useMemo, useState } from 'react';
import { ExternalLink, Filter, Play, RefreshCw, Search } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { AlgorithmGroup, AlgorithmGroupItem } from '../api/types';
import { Badge, Card, EmptyState, ErrorBox, PageHeader, Spinner, StatCard } from '../components/ui';

export default function AlgorithmsPage() {
  const { data, loading, refreshing, error, reload } = useApi<AlgorithmGroup[]>((signal) => api.algorithmGroups({ signal }));
  const [filter, setFilter] = useState('');
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [runningKey, setRunningKey] = useState<string | null>(null);
  const [runFailure, setRunFailure] = useState<{ key: string; message: string } | null>(null);
  const navigate = useNavigate();
  const query = filter.trim().toLowerCase();
  const groups = useMemo(() => {
    if (!data) return [];
    if (!query) return data;
    return data.map((group) => ({ ...group, algorithms: group.algorithms.filter((algorithm) => `${algorithm.name} ${algorithm.key} ${algorithm.problem} ${algorithm.description}`.toLowerCase().includes(query)) })).filter((group) => group.algorithms.length > 0);
  }, [data, query]);
  const allAlgorithms = data?.flatMap((group) => group.algorithms) ?? [];
  const selected = allAlgorithms.find((algorithm) => algorithm.key === selectedKey) ?? null;
  const matchingCount = groups.reduce((sum, group) => sum + group.algorithms.length, 0);

  const runAlgorithm = useCallback(async (algorithm: AlgorithmGroupItem) => {
    if (!algorithm.defaultInput) return;
    setRunningKey(algorithm.key);
    setRunFailure(null);
    try {
      const record = await api.run(algorithm.key, algorithm.defaultInput);
      navigate(`/runs/${encodeURIComponent(record.runId)}`);
    } catch (failure) {
      setRunFailure({ key: algorithm.key, message: failure instanceof Error ? failure.message : String(failure) });
    } finally {
      setRunningKey(null);
    }
  }, [navigate]);

  return (
    <div className="page">
      <PageHeader eyebrow="Analysis" title="Algorithms" description="The live backend catalogue, grouped by module. Complexity and exposure flags are returned by the API." actions={<><button className="btn btn-sm" type="button" onClick={reload} disabled={refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh</button><Link className="btn btn-sm" to="/benchmarks">Run benchmark <ExternalLink size={13} aria-hidden="true" /></Link></>} />
      {loading ? <Card title="Loading catalogue"><Spinner label="Requesting the algorithm catalogue…" /></Card> : error ? <ErrorBox error={error} retry={reload} /> : !data ? <Card title="Algorithm catalogue"><EmptyState>No algorithm catalogue response is available.</EmptyState></Card> : <>
        <div className="stat-grid stat-grid--small">
          <StatCard label="Modules" value={data.length} color="var(--accent)" />
          <StatCard label="Algorithms" value={allAlgorithms.length} color="var(--mod-flow)" />
          <StatCard label="Traceable" value={allAlgorithms.filter((algorithm) => algorithm.tracked).length} color="var(--ok)" />
          <StatCard label="Exposed endpoints" value={allAlgorithms.filter((algorithm) => Boolean(algorithm.canonicalEndpoint)).length} color="var(--info)" />
        </div>
        <Card title="Filter catalogue" sub="Filtering is applied to the returned API catalogue; no catalogue entries are synthesized.">
          <div className="explorer-filters"><label className="sr-only" htmlFor="algorithm-filter">Filter algorithms</label><input id="algorithm-filter" className="input" type="text" placeholder="Name, key, problem or description…" value={filter} onChange={(event) => setFilter(event.target.value)} /><Filter size={15} aria-hidden="true" /><span className="text-muted">{matchingCount} matching entries</span></div>
        </Card>
        {groups.length === 0 ? <Card title="Catalogue results"><EmptyState><Search size={22} aria-hidden="true" /><strong>No algorithms match this filter.</strong><span>Clear the filter to view the full returned catalogue.</span></EmptyState></Card> : <div className="pattern-layout"><Card title="Catalogue" sub={`${matchingCount} entries across ${groups.length} modules`}><div className="table-scroll"><table className="log-table"><caption className="sr-only">Algorithm catalogue</caption><thead><tr><th>Algorithm</th><th>Module</th><th>Query type</th><th>Time</th><th>Space</th><th>Exposure</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{groups.flatMap((group) => group.algorithms.map((algorithm) => <tr key={algorithm.key} className={selectedKey === algorithm.key ? 'winner-row' : undefined}><td><button className="btn btn-sm" type="button" onClick={() => setSelectedKey(algorithm.key)} aria-label={`Inspect ${algorithm.name}`}><strong>{algorithm.name}</strong><code className="algo-key">{algorithm.key}</code></button></td><td className="muted">{group.module}</td><td><Badge>{algorithm.queryType}</Badge></td><td className="mono">{algorithm.timeComplexity}</td><td className="mono">{algorithm.spaceComplexity}</td><td>{algorithm.tracked && <Badge tone="info">traceable</Badge>} {algorithm.canonicalEndpoint && <Badge tone="good">exposed</Badge>}</td><td><button className="icon-btn" type="button" onClick={() => setSelectedKey(algorithm.key)} aria-label={`Inspect ${algorithm.name}`} title="Inspect algorithm"><Search size={14} aria-hidden="true" /></button></td></tr>))}</tbody></table></div></Card><div>{selected ? <AlgorithmDetail algorithm={selected} onRun={runAlgorithm} running={runningKey === selected.key} runError={runFailure?.key === selected.key ? runFailure.message : null} /> : <Card title="Algorithm detail"><EmptyState><Search size={21} aria-hidden="true" /><strong>Select an algorithm.</strong><span>Its returned problem, endpoint and complexity metadata will appear here.</span></EmptyState></Card>}</div></div>}
      </>}
    </div>
  );
}

function AlgorithmDetail({ algorithm, onRun, running, runError }: {
  algorithm: AlgorithmGroupItem;
  onRun: (algorithm: AlgorithmGroupItem) => void;
  running: boolean;
  runError: string | null;
}) {
  const runnable = algorithm.tracked && Boolean(algorithm.defaultInput);
  return <Card title={algorithm.name} sub={algorithm.key} actions={<>{algorithm.tracked && <Badge tone="info">traceable</Badge>}{algorithm.canonicalEndpoint && <Badge tone="good">exposed</Badge>}{runnable && <button className="btn btn-sm" type="button" onClick={() => onRun(algorithm)} disabled={running} aria-label={`Run ${algorithm.name} with its catalogue input`}><Play size={13} aria-hidden="true" /> {running ? 'Running…' : 'Run'}</button>}</>}><div className="detail-grid"><div className="detail-row"><span>Problem</span><strong>{algorithm.problem}</strong></div><div className="detail-row"><span>Query type</span><strong>{algorithm.queryType}</strong></div><div className="detail-row"><span>Algorithm type</span><strong>{algorithm.algorithmType}</strong></div><div className="detail-row"><span>Time complexity</span><strong>{algorithm.timeComplexity}</strong></div><div className="detail-row"><span>Space complexity</span><strong>{algorithm.spaceComplexity}</strong></div></div>{algorithm.description && <p className="algo-desc">{algorithm.description}</p>}{runError && <ErrorBox error={new Error(runError)} />}{algorithm.canonicalEndpoint && <div className="mt-1"><div className="form-label">Canonical endpoint</div><code className="algo-endpoint">{algorithm.canonicalEndpoint}</code></div>}{algorithm.traceEndpoint && <div className="mt-1"><div className="form-label">Trace endpoint</div><code className="algo-endpoint">{algorithm.traceEndpoint}</code></div>}{runnable && <div className="mt-1"><div className="form-label">Run input (catalogue default)</div><pre className="json-block">{JSON.stringify(algorithm.defaultInput, null, 2)}</pre></div>}{algorithm.tracked && !runnable && <p className="text-muted">The catalogue records no default input for this algorithm, so no run can be created from here.</p>}</Card>;
}
