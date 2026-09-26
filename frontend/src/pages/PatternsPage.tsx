import { useEffect, useState } from 'react';
import { ExternalLink, RefreshCw, Workflow } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { LogEvent, PatternDto } from '../api/types';
import { Badge, Card, EmptyState, ErrorBox, EventDrawer, LevelBadge, NoDatasetState, PageHeader, Spinner, StatCard } from '../components/ui';
import { formatMillis, formatNumber, formatTs } from '../components/format';

const LEVELS = ['all', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE', 'FATAL'] as const;

function isMissingDataset(error: Error): boolean {
  const status = (error as Error & { apiError?: { status?: number } }).apiError?.status;
  return status === 404;
}

export default function PatternsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialLevel = searchParams.get('level')?.toUpperCase() ?? 'ALL';
  const [level, setLevel] = useState<string>(LEVELS.includes(initialLevel as (typeof LEVELS)[number]) ? initialLevel : 'all');
  const [selected, setSelected] = useState<PatternDto | null>(null);
  const [drawerEvent, setDrawerEvent] = useState<LogEvent | null>(null);
  const patterns = useApi<PatternDto[]>((signal) => api.patterns(level, 100, { signal }), `patterns-${level}`);
  const examples = useApi<LogEvent[]>((signal) => selected ? api.patternExamples(selected.template, 50, { signal }) : Promise.resolve([]), selected?.template ?? '');

  useEffect(() => {
    const candidate = searchParams.get('level')?.toUpperCase() ?? 'ALL';
    if (LEVELS.includes(candidate as (typeof LEVELS)[number])) setLevel(candidate);
  }, [searchParams]);

  const chooseLevel = (value: string) => {
    setLevel(value);
    setSelected(null);
    setSearchParams(value === 'all' ? {} : { level: value }, { replace: true });
  };

  const list = patterns.data ?? [];
  const occurrences = list.reduce((sum, pattern) => sum + pattern.count, 0);
  const largest = list.reduce((max, pattern) => Math.max(max, pattern.count), 0);

  return (
    <div className="page">
      <PageHeader eyebrow="Investigate" title="Patterns" description="Recurring message structures extracted by the backend heuristic token normalizer." actions={<button className="btn btn-sm" type="button" onClick={patterns.reload} disabled={patterns.refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh</button>} />
      <div className="tab-bar" role="group" aria-label="Pattern severity filter">{LEVELS.map((value) => <button key={value} className={`btn btn-sm${level === value ? ' btn-primary' : ''}`} type="button" aria-pressed={level === value} onClick={() => chooseLevel(value)}>{value}</button>)}</div>
      {patterns.loading ? <Card title="Loading patterns"><Spinner label="Extracting recurring structures…" /></Card> : patterns.error ? isMissingDataset(patterns.error) ? <NoDatasetState detail="Load a dataset before extracting message patterns." /> : <ErrorBox error={patterns.error} retry={patterns.reload} /> : list.length === 0 ? <Card title="Patterns"><EmptyState><Workflow size={22} aria-hidden="true" /><strong>No patterns at this level.</strong><span>The backend extractor returned no recurring structures for the current dataset and filter.</span></EmptyState></Card> : <>
        <div className="stat-grid stat-grid--small">
          <StatCard label="Templates returned" value={formatNumber(list.length)} color="var(--accent)" />
          <StatCard label="Occurrences represented" value={formatNumber(occurrences)} />
          <StatCard label="Largest template" value={formatNumber(largest)} color="var(--mod-flow)" />
          <StatCard label="Method" value="Heuristic" sub="not ML" color="var(--warn)" />
        </div>
        <div className="pattern-layout">
          <Card title={`Recurring structures (${list.length})`} sub="Select a template to request its supporting events" actions={patterns.refreshing ? <Badge tone="info">Refreshing</Badge> : undefined}>
            <div className="table-scroll"><table className="log-table"><caption className="sr-only">Recurring message patterns</caption><thead><tr><th>Level</th><th>Template</th><th>Count</th><th>Example</th></tr></thead><tbody>{list.map((pattern) => <tr key={`${pattern.template}-${pattern.count}`} className={selected?.template === pattern.template ? 'winner-row' : undefined}><td><LevelBadge level={pattern.level} /></td><td><button className="btn btn-sm" type="button" onClick={() => setSelected(pattern)} aria-label={`Inspect examples for ${pattern.template}`}><code className="pattern-template">{pattern.template}</code></button></td><td className="num">{formatNumber(pattern.count)}</td><td className="muted">{pattern.example}</td></tr>)}</tbody></table></div>
          </Card>
          <div>
            {selected ? <Card title="Pattern evidence" sub={selected.example} actions={<><Badge tone="info">{selected.level}</Badge><Link className="btn btn-sm" to={`/logs?q=${encodeURIComponent(selected.example || selected.template)}`} aria-label={`Search logs for the example message of ${selected.template}`}><ExternalLink size={13} aria-hidden="true" /> Matching logs</Link></>}>
              <div className="incident-summary"><span>Template occurrences: {formatNumber(selected.count)}</span><span>Normalization: heuristic token replacement</span></div>
              {examples.loading ? <Spinner label="Loading supporting events…" /> : examples.error ? <ErrorBox error={examples.error} retry={examples.reload} /> : !examples.data || examples.data.length === 0 ? <EmptyState>No supporting events were returned for this template.</EmptyState> : <div className="log-list">{examples.data.map((event) => <div className="log-item" key={event.id}><div className="log-item-top"><LevelBadge level={event.level} /><span className="log-item-service">{event.service}</span><span className="log-item-host">{event.host}</span><span className="log-item-time">{formatTs(event.timestamp)}</span></div><Link className="log-item-msg" to={`/logs/${event.id}`}>{event.message}</Link><div className="log-item-meta">{event.httpMethod && <span className="http-pill">{event.httpMethod} {event.statusCode}</span>}{event.responseTime > 0 && <span>{formatMillis(event.responseTime)}</span>}<button className="icon-btn" type="button" onClick={() => setDrawerEvent(event)} aria-label={`Inspect event ${event.id}`} title="Inspect event"><ExternalLink size={14} aria-hidden="true" /></button></div></div>)}</div>}
            </Card> : <Card title="Pattern evidence"><EmptyState><Workflow size={21} aria-hidden="true" /><strong>Select a template.</strong><span>Evidence events are requested only for the selected pattern.</span></EmptyState></Card>}
          </div>
        </div>
      </>}
      <EventDrawer event={drawerEvent} onClose={() => setDrawerEvent(null)} />
    </div>
  );
}
