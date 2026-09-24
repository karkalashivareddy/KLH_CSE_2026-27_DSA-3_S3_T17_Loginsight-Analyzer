import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { LogSearchResponse, SuggestionDto } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState, LevelBadge, Badge } from '../components/ui';
import { formatNumber, formatNanos, formatTs } from '../components/format';

const SUGGESTION_COLORS: Record<string, string> = {
  service: '#3498db', host: '#9b59b6', endpoint: '#2ecc71', level: '#f1c40f',
  source: '#e67e22', status: '#e74c3c', query: 'var(--accent)', UNKNOWN: '#666'
};

/**
 * Product search (POST /api/search). The DSA engine executes the pattern over the dataset
 * haystack; the methodology panel reports strategy, algorithm, pattern length, text size and the
 * measured duration. A Levenshtein "did you mean" suggestion appears when a search misses.
 */
export default function SearchPage() {
  const [input, setInput] = useState('');
  const [applied, setApplied] = useState({ query: '', page: 1, size: 25 });
  const [suggestions, setSuggestions] = useState<SuggestionDto[]>([]);
  const [focus, setFocus] = useState(false);
  const debounce = useRef<ReturnType<typeof setTimeout> | null>(null);

  const key = JSON.stringify(applied);
  const { data, loading, error, reload } = useApi<LogSearchResponse>(
    () => api.productSearch({ query: applied.query, page: applied.page, size: applied.size, sort: 'timestamp:desc' }),
    key
  );

  useEffect(() => {
    if (debounce.current) clearTimeout(debounce.current);
    const q = input.trim();
    if (!q || q === applied.query) {
      setSuggestions([]);
      return;
    }
    debounce.current = setTimeout(async () => {
      try {
        setSuggestions(await api.suggest(q, 10));
      } catch {
        setSuggestions([]);
      }
    }, 250);
    return () => {
      if (debounce.current) clearTimeout(debounce.current);
    };
  }, [input, applied.query]);

  const submit = useCallback((query?: string) => {
    const q = (query ?? input).trim();
    if (!q) return;
    setSuggestions([]);
    setApplied({ query: q, page: 1, size: applied.size });
  }, [input, applied.size]);

  const totalPages = Math.max(1, Math.ceil((data?.total ?? 0) / applied.size));
  const page = Math.min(applied.page, totalPages);

  return (
    <div className="page">
      <h2 className="page-title">Search</h2>

      <div className="search-hero">
        <div className="search-box-wrap">
          <input
            className="input search-input"
            type="text"
            placeholder="Search the dataset… try payment, timeout, service:auth level:error"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onFocus={() => setFocus(true)}
            onBlur={() => setTimeout(() => setFocus(false), 150)}
            onKeyDown={(e) => e.key === 'Enter' && submit()}
          />
          <button className="btn btn-run" onClick={() => submit()}>Search</button>

          {focus && suggestions.length > 0 && (
            <div className="suggest-list">
              {suggestions.map((s, i) => (
                <button
                  key={`${s.type}-${s.value}-${i}`}
                  className="suggest-item"
                  onMouseDown={() => { setInput(s.value); submit(s.value); }}
                >
                  <span className="suggest-chip" style={{ background: SUGGESTION_COLORS[s.type] ?? SUGGESTION_COLORS.UNKNOWN }}>
                    {s.type}
                  </span>
                  <span>{s.label}</span>
                  <code className="suggest-value">{s.value}</code>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {loading ? (
        <Spinner label="Searching the dataset…" />
      ) : error ? (
        <ErrorBox error={error} retry={reload} />
      ) : !data ? (
        <EmptyState>
          <strong>No dataset loaded.</strong>
          <span>Load the demo dataset or import a file before searching.</span>
          <Link className="btn" to="/datasets">Open Datasets</Link>
        </EmptyState>
      ) : (
        <>
          {data.suggestion && (
            <div className="suggestion-banner">
              <span>Did you mean</span>
              <button className="btn btn-sm" onClick={() => { setInput(data.suggestion!.suggestion); submit(data.suggestion!.suggestion); }}>
                {data.suggestion.suggestion}
              </button>
              <span className="muted">
                {data.suggestion.similarityPct}% similar · {formatNumber(data.suggestion.matchCount)} matches · Levenshtein ({data.suggestion.distance})
              </span>
            </div>
          )}

          <Card title="Execution Methodology">
            <div className="stat-grid stat-grid--small">
              <Badge>strategy: {data.strategy}</Badge>
              <Badge>algorithm: {data.algorithm}</Badge>
              <Badge>pattern: {data.pattern ? `"${data.pattern}"` : '—'}</Badge>
              <Badge>pattern length: {data.patternLength}</Badge>
              <Badge>text size: {formatNumber(data.textSize)} chars</Badge>
              <Badge>measured: {formatNanos(data.durationNanos)}</Badge>
            </div>
          </Card>

          <div className="result-meta">
            <span>{formatNumber(data.total)} matching events in {data.dataset}</span>
            <span>page {data.page} · {data.size} per page</span>
          </div>

          {data.matches.length === 0 ? (
            <EmptyState>No matches for this query.</EmptyState>
          ) : (
            <Card title="Results">
              <div className="search-results">
                {data.matches.map((hit) => (
                  <div key={hit.event.id} className="search-result">
                    <div className="search-result-head">
                      <LevelBadge level={hit.event.level} />
                      <span className="search-result-time">{formatTs(hit.event.timestamp)}</span>
                      <span className="search-result-service">{hit.event.service}</span>
                      <span className="search-result-host">{hit.event.host}</span>
                      <span className="search-result-hits">{hit.matchCount} hit{hit.matchCount === 1 ? '' : 's'}</span>
                    </div>
                    <Link className="search-result-msg" to={`/logs/${hit.event.id}`}>{hit.event.message}</Link>
                    {hit.snippet && <code className="log-snippet">{hit.snippet}</code>}
                    <div className="search-result-meta">
                      {hit.event.httpMethod && <Badge>{hit.event.httpMethod}</Badge>}
                      {hit.event.statusCode > 0 && <Badge>{hit.event.statusCode}</Badge>}
                      {hit.event.responseTime > 0 && <span>{formatNanos(hit.event.responseTime)}</span>}
                      {hit.event.requestId && <span className="muted">{hit.event.requestId}</span>}
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}

          {data.total > 0 && (
            <div className="pager">
              <button className="btn btn-sm" disabled={page <= 1} onClick={() => setApplied({ ...applied, page: page - 1 })}>‹ Prev</button>
              <span className="pager-label">Page {page} of {totalPages}</span>
              <button className="btn btn-sm" disabled={page >= totalPages} onClick={() => setApplied({ ...applied, page: page + 1 })}>Next ›</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}