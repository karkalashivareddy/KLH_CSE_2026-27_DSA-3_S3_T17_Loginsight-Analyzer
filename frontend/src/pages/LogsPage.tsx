import { useCallback, useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight, ExternalLink, Filter, RefreshCw, Search } from 'lucide-react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { LogEvent, LogSearchResponse } from '../api/types';
import {
  Card,
  EmptyState,
  ErrorBox,
  EventDrawer,
  LevelBadge,
  NoDatasetState,
  PageHeader,
  Spinner,
  StatCard
} from '../components/ui';
import { formatMillis, formatNanos, formatNumber, formatTs } from '../components/format';

function toIso(value: string): string | null {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}

function isMissingDataset(error: Error): boolean {
  const status = (error as Error & { apiError?: { status?: number } }).apiError?.status;
  return status === 404;
}

function LogDetail({ id }: { id: number }) {
  const { data, loading, error, reload } = useApi<LogEvent | null>((signal) => api.logById(id, { signal }), String(id));

  return (
    <div className="page">
      <PageHeader
        eyebrow="Log detail"
        title={`Event #${id}`}
        description={data ? <><LevelBadge level={data.level} /> · {data.service} · {formatTs(data.timestamp)}</> : 'A single normalized event returned by the event detail endpoint.'}
        actions={<Link className="btn btn-sm" to="/logs"><ChevronLeft size={14} aria-hidden="true" /> Explorer</Link>}
      />
      {loading ? <Card title="Loading event"><Spinner label="Requesting event detail…" /></Card> : error ? <ErrorBox error={error} retry={reload} /> : !data ? <Card title="Event unavailable"><EmptyState><strong>Event {id} is not present in the current dataset.</strong><span>Return to the explorer or load a dataset that contains this event.</span><Link className="btn btn-sm" to="/logs">Open explorer</Link></EmptyState></Card> : <>
        <div className="stat-grid">
          <StatCard label="Severity" value={<LevelBadge level={data.level} />} color="var(--severity-error)" />
          <StatCard label="Response time" value={data.responseTime > 0 ? formatMillis(data.responseTime) : '—'} color="var(--accent)" />
          <StatCard label="HTTP status" value={data.statusCode > 0 ? data.statusCode : '—'} color={data.statusCode >= 500 ? 'var(--danger)' : data.statusCode >= 400 ? 'var(--warn)' : 'var(--ok)'} />
          <StatCard label="Service" value={data.service} color="var(--mod-flow)" />
          <StatCard label="Host" value={data.host} color="var(--mod-dp)" />
          <StatCard label="Request ID" value={data.requestId || '—'} />
        </div>
        <Card title="Event summary" actions={<Link className="btn btn-sm" to={`/search?q=${encodeURIComponent(`service:${data.service}`)}`}><Search size={13} aria-hidden="true" /> Search service</Link>}>
          <div className="detail-grid">
            <div className="detail-row"><span>Service</span><strong>{data.service}</strong></div>
            <div className="detail-row"><span>Host</span><strong>{data.host}</strong></div>
            <div className="detail-row"><span>IP address</span><strong>{data.ipAddress}</strong></div>
            <div className="detail-row"><span>Request ID</span><strong>{data.requestId || '—'}</strong></div>
            <div className="detail-row"><span>User ID</span><strong>{data.userId || '—'}</strong></div>
            <div className="detail-row"><span>Trace ID</span><strong>{data.traceId ?? '—'}</strong></div>
            <div className="detail-row"><span>Span ID</span><strong>{data.spanId ?? '—'}</strong></div>
            <div className="detail-row"><span>Source</span><strong>{data.source ?? '—'}</strong></div>
            <div className="detail-row"><span>URL</span><strong>{data.url ?? '—'}</strong></div>
            {data.httpMethod && <div className="detail-row"><span>HTTP</span><strong>{data.httpMethod} {data.endpoint}</strong></div>}
            {data.statusCode > 0 && <div className="detail-row"><span>Status</span><strong>{data.statusCode}</strong></div>}
            {data.responseTime > 0 && <div className="detail-row"><span>Response time</span><strong>{formatMillis(data.responseTime)}</strong></div>}
          </div>
        </Card>
        <Card title="Message"><div className="log-detail-message">{data.message}</div></Card>
        <div className="quick-actions"><Link className="btn btn-sm" to={`/services/${encodeURIComponent(data.service)}`}>Open service</Link><Link className="btn btn-sm" to={`/logs?q=${encodeURIComponent(data.message)}`}>Search message</Link></div>
        {data.rawMessage && <Card title="Raw line"><pre className="raw-message">{data.rawMessage}</pre></Card>}
        {Object.keys(data.attributes ?? {}).length > 0 && <Card title="Attributes"><div className="table-scroll"><table className="info-table"><caption className="sr-only">Event attributes</caption><thead><tr><th>Key</th><th>Value</th></tr></thead><tbody>{Object.entries(data.attributes).map(([key, value]) => <tr key={key}><td className="mono">{key}</td><td>{value}</td></tr>)}</tbody></table></div></Card>}
      </>}
    </div>
  );
}

function LogExplorer() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialQuery = searchParams.get('q') ?? '';
  const [query, setQuery] = useState(initialQuery);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [size, setSize] = useState(25);
  const [sort, setSort] = useState('timestamp:desc');
  const [applied, setApplied] = useState({ query: initialQuery, from: null as string | null, to: null as string | null, page: 1, size: 25, sort: 'timestamp:desc' });
  const [selected, setSelected] = useState<LogEvent | null>(null);
  const key = JSON.stringify(applied);
  const { data, loading, refreshing, error, reload } = useApi<LogSearchResponse>((signal) => api.explore({ query: applied.query, from: applied.from, to: applied.to, page: applied.page, size: applied.size, sort: applied.sort }, { signal }), key);
  const closeDrawer = useCallback(() => setSelected(null), []);

  useEffect(() => {
    const nextQuery = searchParams.get('q') ?? '';
    if (nextQuery === applied.query) return;
    setQuery(nextQuery);
    setApplied((previous) => ({ ...previous, query: nextQuery, page: 1 }));
  }, [applied.query, searchParams]);

  const submit = () => {
    const next = { query: query.trim(), from: toIso(from), to: toIso(to), page: 1, size, sort };
    setApplied(next);
    const nextParams = new URLSearchParams();
    if (next.query) nextParams.set('q', next.query);
    if (next.from) nextParams.set('from', next.from);
    if (next.to) nextParams.set('to', next.to);
    setSearchParams(nextParams, { replace: true });
  };

  const clearFilters = () => {
    setQuery('');
    setFrom('');
    setTo('');
    setSize(25);
    setSort('timestamp:desc');
    setApplied({ query: '', from: null, to: null, page: 1, size: 25, sort: 'timestamp:desc' });
    setSearchParams({}, { replace: true });
  };

  const totalPages = Math.max(1, Math.ceil((data?.total ?? 0) / applied.size));
  const page = Math.min(applied.page, totalPages);
  const exactHits = data?.matches.reduce((sum, hit) => sum + hit.matchCount, 0) ?? 0;

  return (
    <div className="page">
      <PageHeader eyebrow="Observe" title="Log Explorer" description="Filter, page and inspect normalized events through the structured explorer API." actions={<><button className="btn btn-sm" type="button" onClick={reload} disabled={refreshing}><RefreshCw size={14} aria-hidden="true" />{refreshing ? 'Refreshing' : 'Refresh'}</button><button className="btn btn-sm" type="button" onClick={clearFilters}><Filter size={14} aria-hidden="true" /> Clear filters</button></>} />
      <Card title="Explorer filters" sub="Filters are sent to the backend; the table is not filtered in the browser.">
        <div className="explorer-filters">
          <label className="sr-only" htmlFor="log-query">Event query</label>
          <input id="log-query" className="input" type="text" placeholder="Message, service, endpoint or level…" value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && submit()} />
          <label className="sr-only" htmlFor="log-from">From timestamp</label>
          <input id="log-from" className="input" type="datetime-local" value={from} onChange={(event) => setFrom(event.target.value)} />
          <label className="sr-only" htmlFor="log-to">To timestamp</label>
          <input id="log-to" className="input" type="datetime-local" value={to} onChange={(event) => setTo(event.target.value)} />
          <label className="sr-only" htmlFor="log-sort">Sort order</label>
          <select id="log-sort" className="select" value={sort} onChange={(event) => setSort(event.target.value)}><option value="timestamp:desc">Newest first</option><option value="timestamp:asc">Oldest first</option></select>
          <label className="sr-only" htmlFor="log-size">Page size</label>
          <select id="log-size" className="select" value={size} onChange={(event) => setSize(Number(event.target.value))}><option value={10}>10 / page</option><option value={25}>25 / page</option><option value={50}>50 / page</option><option value={100}>100 / page</option></select>
          <button className="btn btn-run" type="button" onClick={submit}><Filter size={14} aria-hidden="true" /> Apply</button>
        </div>
      </Card>

      {loading ? <Card title="Loading events"><Spinner label="Requesting the explorer response…" /></Card> : error ? isMissingDataset(error) ? <NoDatasetState detail="The explorer needs a loaded dataset before it can return events." /> : <ErrorBox error={error} retry={reload} /> : !data ? <Card title="Events"><EmptyState>No explorer response is available.</EmptyState></Card> : data.matches.length === 0 ? <Card title="Events"><EmptyState><Search size={21} aria-hidden="true" /><strong>No matching events.</strong><span>Try a broader query or load a dataset with more records.</span><button className="btn btn-sm" type="button" onClick={clearFilters}>Clear filters</button></EmptyState></Card> : <>
        <div className="stat-grid stat-grid--small">
          <StatCard label="Matching events" value={formatNumber(data.total)} color="var(--accent)" />
          <StatCard label="Exact hits on page" value={formatNumber(exactHits)} color="var(--ok)" />
          <StatCard label="Dataset" value={data.dataset} />
          <StatCard label="Page" value={`${data.page} / ${totalPages}`} />
        </div>
        <div className="result-meta" aria-live="polite"><span>{formatNumber(data.total)} events · {data.strategy} · {data.algorithm}</span><span>{refreshing ? 'Refreshing…' : `${formatNanos(data.durationNanos)} measured`}</span></div>
        <Card title="Events" sub={`Page ${data.page} · ${data.size} records per page`} actions={<span className="badge">{data.sort}</span>}>
          <div className="table-scroll"><table className="log-table"><caption className="sr-only">Filtered log events</caption><thead><tr><th>Time</th><th>Level</th><th>Service</th><th>Host</th><th>HTTP</th><th>Message</th><th>Hits</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{data.matches.map((hit) => <tr key={hit.event.id}><td className="ts">{formatTs(hit.event.timestamp)}</td><td><LevelBadge level={hit.event.level} /></td><td><Link to={`/services/${encodeURIComponent(hit.event.service)}`}>{hit.event.service}</Link></td><td className="muted">{hit.event.host}</td><td>{hit.event.httpMethod ? <span className="http-pill">{hit.event.httpMethod} {hit.event.statusCode}</span> : <span className="muted">—</span>}</td><td><Link className="log-cell" to={`/logs/${hit.event.id}`}><span className="log-cell-msg">{hit.event.message}</span>{hit.snippet && <code className="log-snippet">{hit.snippet}</code>}</Link></td><td className="num">{hit.matchCount}</td><td><button className="icon-btn" type="button" onClick={() => setSelected(hit.event)} aria-label={`Inspect event ${hit.event.id}`} title="Inspect event"><ExternalLink size={15} aria-hidden="true" /></button></td></tr>)}</tbody></table></div>
        </Card>
        <div className="pager"><button className="btn btn-sm" type="button" disabled={page <= 1} onClick={() => setApplied({ ...applied, page: page - 1 })}><ChevronLeft size={14} aria-hidden="true" /> Previous</button><span className="pager-label">Page {page} of {totalPages}</span><button className="btn btn-sm" type="button" disabled={page >= totalPages} onClick={() => setApplied({ ...applied, page: page + 1 })}>Next <ChevronRight size={14} aria-hidden="true" /></button></div>
      </>}
      <EventDrawer event={selected} onClose={closeDrawer} />
    </div>
  );
}

export default function LogsPage() {
  const { id } = useParams();
  const numericId = id === undefined ? null : Number(id);
  if (numericId !== null && Number.isSafeInteger(numericId) && numericId >= 0) return <LogDetail id={numericId} />;
  if (id !== undefined) return <div className="page"><PageHeader title="Invalid event link" /><EmptyState>Event identifiers must be non-negative integers.</EmptyState></div>;
  return <LogExplorer />;
}
