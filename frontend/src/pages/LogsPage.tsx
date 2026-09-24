import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { LogEvent, LogSearchResponse } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState, LevelBadge } from '../components/ui';
import { formatNumber, formatNanos, formatTs } from '../components/format';

function toIso(value: string): string | null {
  return value ? new Date(value).toISOString() : null;
}

function LogDetail({ id }: { id: number }) {
  const { data, loading, error, reload } = useApi<LogEvent | null>(() => api.logById(id), String(id));

  if (loading) return <Spinner label="Loading event…" />;
  if (error) return <ErrorBox error={error} retry={reload} />;
  if (!data) return <EmptyState>Event {id} not found in the current dataset.</EmptyState>;

  const e = data;
  return (
    <div className="page">
      <h2 className="page-title">
        <Link className="btn btn-sm" to="/logs">← Explorer</Link>
        <span className="title-inline">Event #{e.id}</span>
        <LevelBadge level={e.level} />
      </h2>

      <Card title="Summary">
        <div className="detail-grid">
          <div className="detail-row"><span>Timestamp</span><strong>{formatTs(e.timestamp)}</strong></div>
          <div className="detail-row"><span>Service</span><strong>{e.service}</strong></div>
          <div className="detail-row"><span>Host</span><strong>{e.host}</strong></div>
          <div className="detail-row"><span>IP</span><strong>{e.ipAddress}</strong></div>
          <div className="detail-row"><span>Request ID</span><strong>{e.requestId}</strong></div>
          <div className="detail-row"><span>User ID</span><strong>{e.userId}</strong></div>
          <div className="detail-row"><span>Trace ID</span><strong>{e.traceId ?? '—'}</strong></div>
          <div className="detail-row"><span>Span ID</span><strong>{e.spanId ?? '—'}</strong></div>
          <div className="detail-row"><span>Source</span><strong>{e.source ?? '—'}</strong></div>
          <div className="detail-row"><span>URL</span><strong>{e.url ?? '—'}</strong></div>
          {e.httpMethod && <div className="detail-row"><span>HTTP</span><strong>{e.httpMethod} {e.endpoint}</strong></div>}
          {e.statusCode > 0 && <div className="detail-row"><span>Status</span><strong>{e.statusCode}</strong></div>}
          {e.responseTime > 0 && <div className="detail-row"><span>Response time</span><strong>{formatNanos(e.responseTime)}</strong></div>}
        </div>
      </Card>

      <Card title="Message">
        <div className="log-detail-message">{e.message}</div>
      </Card>

      {e.rawMessage && (
        <Card title="Raw line">
          <pre className="raw-message">{e.rawMessage}</pre>
        </Card>
      )}

      {Object.keys(e.attributes ?? {}).length > 0 && (
        <Card title="Attributes">
          <table className="info-table">
            <thead><tr><th>Key</th><th>Value</th></tr></thead>
            <tbody>
              {Object.entries(e.attributes).map(([k, v]) => (
                <tr key={k}><td>{k}</td><td>{v}</td></tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  );
}

/** Log Explorer: structured filters, paging and free-text search (GET /api/logs/explore). */
function LogExplorer() {
  const [query, setQuery] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [size, setSize] = useState(25);
  const [sort, setSort] = useState('timestamp:desc');
  const [applied, setApplied] = useState({ query: '', from: '' as string | null, to: '' as string | null, page: 1, size: 25, sort: 'timestamp:desc' });

  const key = JSON.stringify({ ...applied });
  const { data, loading, error, reload } = useApi<LogSearchResponse>(
    () => api.explore({
      query: applied.query,
      from: applied.from,
      to: applied.to,
      page: applied.page,
      size: applied.size,
      sort: applied.sort
    }),
    key
  );

  const submit = () => setApplied({ query, from: toIso(from), to: toIso(to), page: 1, size, sort });

  const totalPages = Math.max(1, Math.ceil((data?.total ?? 0) / applied.size));
  const page = Math.min(applied.page, totalPages);

  return (
    <div className="page">
      <h2 className="page-title">Log Explorer</h2>

      <Card title="Filters">
        <div className="explorer-filters">
          <input
            className="input"
            type="text"
            placeholder="Free-text search… e.g. service:payment level:error"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && submit()}
          />
          <input className="input" type="datetime-local" value={from} onChange={(e) => setFrom(e.target.value)} aria-label="From" />
          <input className="input" type="datetime-local" value={to} onChange={(e) => setTo(e.target.value)} aria-label="To" />
          <select className="select" value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort">
            <option value="timestamp:desc">Newest first</option>
            <option value="timestamp:asc">Oldest first</option>
          </select>
          <select className="select" value={size} onChange={(e) => setSize(Number(e.target.value))} aria-label="Page size">
            {[10, 25, 50, 100].map((n) => <option key={n} value={n}>{n} / page</option>)}
          </select>
          <button className="btn btn-run" onClick={submit}>Apply</button>
        </div>
      </Card>

      {loading ? (
        <Spinner label="Searching…" />
      ) : error ? (
        <ErrorBox error={error} retry={reload} />
      ) : !data ? (
        <EmptyState>No dataset loaded — use the demo dataset or import a file first.</EmptyState>
      ) : data.matches.length === 0 ? (
        <EmptyState>
          <strong>No matches.</strong>
          <span>No events in the current dataset match these filters.</span>
        </EmptyState>
      ) : (
        <>
          <div className="result-meta">
            <span>{formatNumber(data.total)} events · dataset {data.dataset}</span>
            <span>exact hit count {formatNumber(data.matches.reduce((n, m) => n + m.matchCount, 0))}</span>
          </div>

          <Card title="Events">
            <table className="log-table">
              <thead>
                <tr>
                  <th>Time</th><th>Level</th><th>Service</th><th>Host</th><th>HTTP</th><th>Message</th><th>Hits</th>
                </tr>
              </thead>
              <tbody>
                {data.matches.map((hit) => (
                  <tr key={hit.event.id}>
                    <td className="mono">{formatTs(hit.event.timestamp)}</td>
                    <td><LevelBadge level={hit.event.level} /></td>
                    <td>{hit.event.service}</td>
                    <td className="muted">{hit.event.host}</td>
                    <td>
                      {hit.event.httpMethod ? (
                        <span className="http-pill">{hit.event.httpMethod} {hit.event.statusCode}</span>
                      ) : <span className="muted">—</span>}
                    </td>
                    <td>
                      <Link className="log-cell" to={`/logs/${hit.event.id}`} title={hit.event.message}>
                        <span className="log-cell-msg">{hit.event.message}</span>
                        {hit.snippet && <code className="log-snippet">{hit.snippet}</code>}
                      </Link>
                    </td>
                    <td>{hit.matchCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>

          <div className="pager">
            <button className="btn btn-sm" disabled={page <= 1} onClick={() => setApplied({ ...applied, page: page - 1 })}>‹ Prev</button>
            <span className="pager-label">Page {page} of {totalPages}</span>
            <button className="btn btn-sm" disabled={page >= totalPages} onClick={() => setApplied({ ...applied, page: page + 1 })}>Next ›</button>
          </div>
        </>
      )}
    </div>
  );
}

export default function LogsPage() {
  const { id } = useParams();
  if (id) return <LogDetail id={Number(id)} />;
  return <LogExplorer />;
}