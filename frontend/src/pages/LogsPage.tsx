import { useCallback, useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight, Clock3, ExternalLink, Filter, Search, Sparkles } from 'lucide-react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { LogEvent, LogSearchResponse } from '../api/types';
import {
  Badge,
  Card,
  EmptyState,
  ErrorBox,
  EventDrawer,
  LevelBadge,
  NoDatasetState,
  PageHeader,
  Spinner,
  StatCard,
} from '../components/ui';
import { formatMillis, formatNanos, formatNumber, formatTs } from '../components/format';

function toIso(value: string): string | null {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}

function toLocalInput(value: string | null): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
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
  const initialFrom = searchParams.get('from');
  const initialTo = searchParams.get('to');
  const [query, setQuery] = useState(initialQuery);
  const [from, setFrom] = useState(toLocalInput(initialFrom));
  const [to, setTo] = useState(toLocalInput(initialTo));
  const [showTimeBounds, setShowTimeBounds] = useState(Boolean(initialFrom || initialTo));
  const [size, setSize] = useState(25);
  const [sort, setSort] = useState('timestamp:desc');
  const [applied, setApplied] = useState({ query: initialQuery, from: initialFrom, to: initialTo, page: 1, size: 25, sort: 'timestamp:desc' });
  const [selected, setSelected] = useState<LogEvent | null>(null);
  const key = JSON.stringify(applied);
  const { data, loading, refreshing, error, reload } = useApi<LogSearchResponse>((signal) => api.explore({ query: applied.query, from: applied.from, to: applied.to, page: applied.page, size: applied.size, sort: applied.sort }, { signal }), key);
  const closeDrawer = useCallback(() => setSelected(null), []);

  useEffect(() => {
    const nextQuery = searchParams.get('q') ?? '';
    const nextFrom = searchParams.get('from');
    const nextTo = searchParams.get('to');
    if (nextQuery === applied.query && nextFrom === applied.from && nextTo === applied.to) return;
    setQuery(nextQuery);
    setFrom(toLocalInput(nextFrom));
    setTo(toLocalInput(nextTo));
    setShowTimeBounds(Boolean(nextFrom || nextTo));
    setApplied((previous) => ({ ...previous, query: nextQuery, from: nextFrom, to: nextTo, page: 1 }));
  }, [applied.from, applied.query, applied.to, searchParams]);

  const submit = () => {
    const next = { query: query.trim(), from: toIso(from), to: toIso(to), page: 1, size, sort };
    setApplied(next);
    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete('q');
    nextParams.delete('from');
    nextParams.delete('to');
    if (next.query) nextParams.set('q', next.query);
    if (next.from) nextParams.set('from', next.from);
    if (next.to) nextParams.set('to', next.to);
    setSearchParams(nextParams, { replace: true });
  };

  const clearFilters = () => {
    setQuery('');
    setFrom('');
    setTo('');
    setShowTimeBounds(false);
    setSize(25);
    setSort('timestamp:desc');
    setApplied({ query: '', from: null, to: null, page: 1, size: 25, sort: 'timestamp:desc' });
    setSearchParams({}, { replace: true });
  };

  const totalPages = Math.max(1, Math.ceil((data?.total ?? 0) / applied.size));
  const page = Math.min(applied.page, totalPages);
  const exactHits = data?.matches.reduce((sum, hit) => sum + hit.matchCount, 0) ?? 0;

  const addLevel = (level: string) => {
    const term = `level:${level}`;
    if (query.split(/\s+/).includes(term)) return;
    setQuery((value) => `${value.trim()} ${term}`.trim());
  };

  return (
    <div className="page logs-page">
      <PageHeader eyebrow="Investigate · Events" title="Log explorer" description="Search normalized events, narrow the time window, then follow an event into its trace or service." actions={<><button className="btn btn-sm" type="button" onClick={reload} disabled={refreshing} aria-label="Refresh log results">↻ {refreshing ? 'Refreshing' : 'Refresh'}</button><button className="btn btn-sm" type="button" onClick={clearFilters}>Clear query</button></>} />
      <section className="log-search-workspace" aria-label="Log search">
        <div className="log-query-row">
          <Search size={18} aria-hidden="true" />
          <label className="sr-only" htmlFor="log-query">Search logs</label>
          <input id="log-query" className="log-query-input" type="search" autoComplete="off" placeholder={'Search messages or fields: level:ERROR service:payment-service "connection refused"'} value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && submit()} />
          <button className="btn btn-primary log-submit" type="button" onClick={submit}><Search size={15} aria-hidden="true" /> Search</button>
        </div>
        <div className="log-query-help"><span>Free text uses KMP against indexed event text; field filters are parsed by the backend.</span><span className="log-query-syntax"><code>level</code><code>service</code><code>host</code><code>status</code><code>trace</code><code>request</code></span></div>
        <div className="log-search-tools">
          <div className="log-quick-filters" role="group" aria-label="Add severity filter">
            <span><Filter size={13} aria-hidden="true" /> Add filter</span>
            {['ERROR', 'WARN', 'INFO'].map((level) => <button key={level} type="button" className={`filter-chip filter-chip--${level.toLowerCase()}`} onClick={() => addLevel(level)} aria-label={`Add level ${level} filter`}>{level}</button>)}
          </div>
          <div className="log-search-options">
            <button className={`text-control${showTimeBounds ? ' text-control--active' : ''}`} type="button" aria-expanded={showTimeBounds} onClick={() => setShowTimeBounds((value) => !value)}><Clock3 size={14} aria-hidden="true" /> Time bounds</button>
            <label className="sr-only" htmlFor="log-sort">Sort order</label><select id="log-sort" className="select log-sort" value={sort} onChange={(event) => setSort(event.target.value)} aria-label="Sort order"><option value="timestamp:desc">Newest first</option><option value="timestamp:asc">Oldest first</option></select>
            <label className="sr-only" htmlFor="log-size">Rows per page</label><select id="log-size" className="select log-size" value={size} onChange={(event) => setSize(Number(event.target.value))} aria-label="Rows per page"><option value={10}>10 rows</option><option value={25}>25 rows</option><option value={50}>50 rows</option><option value={100}>100 rows</option></select>
          </div>
        </div>
        {showTimeBounds && <div className="log-time-bounds"><div><label htmlFor="log-from">From</label><input id="log-from" className="input" type="datetime-local" value={from} onChange={(event) => setFrom(event.target.value)} /></div><div><label htmlFor="log-to">To</label><input id="log-to" className="input" type="datetime-local" value={to} onChange={(event) => setTo(event.target.value)} /></div><p>Bounds use each event’s timestamp before matching.</p></div>}
      </section>

      {loading ? <Card title="Searching the event index"><Spinner label="Requesting matching events" /></Card> : error ? isMissingDataset(error) ? <NoDatasetState detail="The event explorer needs a loaded source before it can return matching records." /> : <ErrorBox error={error} retry={reload} /> : !data ? <Card title="Search results"><EmptyState>No search response is available.</EmptyState></Card> : <>
        <section className="log-result-summary" aria-live="polite">
          <div className="log-result-count"><span>Matching events</span><strong>{formatNumber(data.total)}</strong><small>{data.dataset} · page {page} of {totalPages}</small></div>
          <div className="log-result-measure"><span><Sparkles size={13} aria-hidden="true" /> Search execution</span><strong>{data.algorithm ?? 'Indexed filters'}</strong><small>{data.strategy} · {refreshing ? 'refreshing' : `${formatNanos(data.durationNanos)} measured`}</small></div>
          <div className="log-result-hits"><span>Text matches on this page</span><strong>{formatNumber(exactHits)}</strong><small>{data.matches.length} event records returned</small></div>
          <div className="log-result-actions"><button className="text-control" type="button" onClick={reload} disabled={refreshing}>Refresh results</button></div>
        </section>
        {data.matches.length === 0 ? <Card title="No events matched"><EmptyState><Search size={21} aria-hidden="true" /><strong>Try a wider search.</strong><span>Remove a field filter, adjust the time bounds, or clear the query to return to the full event stream.</span><button className="btn btn-sm" type="button" onClick={clearFilters}>Clear query and time bounds</button></EmptyState></Card> : <Card className="log-results-card" title="Event stream" sub={`${data.size} records per page · ordered ${data.sort === 'timestamp:asc' ? 'oldest first' : 'newest first'}`}>
          <div className="log-results-list" role="list" aria-label="Filtered log events">{data.matches.map((hit) => <article className="log-result-row" role="listitem" key={hit.event.id}>
            <div className="log-row-time"><time dateTime={hit.event.timestamp}>{formatTs(hit.event.timestamp)}</time><span>#{hit.event.id}</span></div>
            <div className="log-row-service"><LevelBadge level={hit.event.level} /><Link to={`/services/${encodeURIComponent(hit.event.service)}`}>{hit.event.service}</Link><small>{hit.event.host}</small></div>
            <div className="log-row-content"><button type="button" className="log-row-message" onClick={() => setSelected(hit.event)} aria-label={`Inspect event ${hit.event.id}: ${hit.event.message}`}>{hit.event.message}</button>{hit.snippet && <code className="log-snippet">{hit.snippet}</code>}</div>
            <div className="log-row-context">{hit.event.httpMethod && <Badge tone={hit.event.statusCode >= 500 ? 'danger' : hit.event.statusCode >= 400 ? 'warn' : 'good'}>{hit.event.httpMethod} {hit.event.statusCode}</Badge>}{hit.event.responseTime > 0 && <span>{formatMillis(hit.event.responseTime)}</span>}{hit.event.traceId && <Link to={`/logs?q=${encodeURIComponent(`trace:${hit.event.traceId}`)}`} title={`Search trace ${hit.event.traceId}`}>Trace ↗</Link>}{hit.matchCount > 0 && <small>{hit.matchCount} hits</small>}</div>
            <button className="icon-btn log-row-open" type="button" onClick={() => setSelected(hit.event)} aria-label={`Open event ${hit.event.id} details`} title="Open event details"><ExternalLink size={14} aria-hidden="true" /></button>
          </article>)}</div>
        </Card>}
        {data.matches.length > 0 && <nav className="log-pager" aria-label="Log results pages"><span>Showing <strong>{formatNumber((page - 1) * applied.size + 1)}–{formatNumber(Math.min(page * applied.size, data.total))}</strong> of {formatNumber(data.total)}</span><div><button className="btn btn-sm" type="button" disabled={page <= 1} onClick={() => setApplied({ ...applied, page: page - 1 })}><ChevronLeft size={14} aria-hidden="true" /> Previous</button><button className="btn btn-sm" type="button" disabled={page >= totalPages} onClick={() => setApplied({ ...applied, page: page + 1 })}>Next <ChevronRight size={14} aria-hidden="true" /></button></div></nav>}
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
