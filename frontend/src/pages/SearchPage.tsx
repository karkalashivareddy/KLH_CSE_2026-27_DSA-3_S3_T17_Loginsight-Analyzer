import { useCallback, useEffect, useRef, useState } from 'react';
import { ExternalLink, Search, Sparkles, X } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { LogEvent, LogSearchResponse, SuggestionDto } from '../api/types';
import { Badge, Card, EmptyState, ErrorBox, EventDrawer, LevelBadge, NoDatasetState, PageHeader, Spinner, StatCard } from '../components/ui';
import { formatMillis, formatNanos, formatNumber, formatTs } from '../components/format';

const SUGGESTION_COLORS: Record<string, string> = {
  service: 'var(--severity-info)',
  host: 'var(--severity-trace)',
  endpoint: 'var(--ok)',
  level: 'var(--severity-warn)',
  source: 'var(--warn)',
  status: 'var(--severity-error)',
  query: 'var(--accent)'
};

function isMissingDataset(error: Error): boolean {
  const status = (error as Error & { apiError?: { status?: number } }).apiError?.status;
  return status === 404;
}

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialQuery = searchParams.get('q') ?? '';
  const [input, setInput] = useState(initialQuery);
  const [applied, setApplied] = useState<{ query: string; page: number; size: number } | null>(initialQuery ? { query: initialQuery, page: 1, size: 25 } : null);
  const [suggestions, setSuggestions] = useState<SuggestionDto[]>([]);
  const [suggestionError, setSuggestionError] = useState<string | null>(null);
  const [suggestionLoading, setSuggestionLoading] = useState(false);
  const [focus, setFocus] = useState(false);
  const [selected, setSelected] = useState<LogEvent | null>(null);
  const debounce = useRef<ReturnType<typeof setTimeout> | null>(null);
  const key = applied ? JSON.stringify(applied) : 'not-run';
  const { data, loading, refreshing, error, reload } = useApi<LogSearchResponse | null>(
    (signal) => applied ? api.productSearch({ query: applied.query, page: applied.page, size: applied.size, sort: 'timestamp:desc' }, { signal }) : Promise.resolve(null),
    key
  );

  useEffect(() => {
    const query = searchParams.get('q') ?? '';
    setInput((previous) => previous === query ? previous : query);
    setApplied((previous) => {
      if (query && previous?.query !== query) return { query, page: 1, size: previous?.size ?? 25 };
      if (!query && previous !== null) return null;
      return previous;
    });
  }, [searchParams]);

  useEffect(() => {
    if (debounce.current) clearTimeout(debounce.current);
    const query = input.trim();
    if (!query || query === applied?.query) {
      setSuggestions([]);
      setSuggestionError(null);
      setSuggestionLoading(false);
      return;
    }
    const controller = new AbortController();
    setSuggestionLoading(true);
    debounce.current = setTimeout(() => {
      void api.suggest(query, 10, { signal: controller.signal })
        .then((next) => {
          setSuggestions(next);
          setSuggestionError(null);
        })
        .catch((reason: unknown) => {
          if (controller.signal.aborted) return;
          setSuggestions([]);
          setSuggestionError(reason instanceof Error ? reason.message : 'Suggestions are unavailable.');
        })
        .finally(() => {
          if (!controller.signal.aborted) setSuggestionLoading(false);
        });
    }, 250);
    return () => {
      controller.abort();
      if (debounce.current) clearTimeout(debounce.current);
    };
  }, [applied?.query, input]);

  const submit = useCallback((value?: string) => {
    const query = (value ?? input).trim();
    if (!query) return;
    setInput(query);
    setSuggestions([]);
    setApplied({ query, page: 1, size: applied?.size ?? 25 });
    setSearchParams({ q: query }, { replace: true });
  }, [applied?.size, input, setSearchParams]);

  const clearSearch = () => {
    setInput('');
    setApplied(null);
    setSuggestions([]);
    setSuggestionError(null);
    setSearchParams({}, { replace: true });
  };

  const closeDrawer = useCallback(() => setSelected(null), []);
  const totalPages = Math.max(1, Math.ceil((data?.total ?? 0) / (applied?.size ?? 25)));
  const page = Math.min(applied?.page ?? 1, totalPages);
  const exactHits = data?.matches.reduce((sum, hit) => sum + hit.matchCount, 0) ?? 0;

  return (
    <div className="page">
      <PageHeader eyebrow="Query" title="Search" description="Execute a real matcher over the loaded dataset. Structured terms are interpreted by the backend search service." actions={applied && <button className="btn btn-sm" type="button" onClick={clearSearch}><X size={14} aria-hidden="true" /> Clear</button>} />
      <div className="search-hero">
        <div className="search-box-wrap">
          <label className="sr-only" htmlFor="product-search">Search events</label>
          <input
            id="product-search"
            className="input search-input"
            type="text"
            placeholder="Try a message, service, endpoint or level…"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            onFocus={() => setFocus(true)}
            onBlur={() => window.setTimeout(() => setFocus(false), 150)}
            onKeyDown={(event) => event.key === 'Enter' && submit()}
            aria-expanded={focus && (suggestionLoading || suggestions.length > 0 || Boolean(suggestionError))}
            aria-controls="search-suggestions"
            autoComplete="off"
          />
          <button className="btn btn-run" type="button" onClick={() => submit()} disabled={!input.trim()}><Search size={15} aria-hidden="true" /> Search</button>
          {focus && (suggestionLoading || suggestions.length > 0 || suggestionError) && <div className="suggest-list" id="search-suggestions" role="listbox" aria-label="Search suggestions">{suggestionLoading && <div className="command-empty" role="status">Loading suggestions…</div>}{suggestionError && <div className="command-empty" role="status">{suggestionError}</div>}{suggestions.map((suggestion, index) => <button key={`${suggestion.type}-${suggestion.value}-${index}`} className="suggest-item" type="button" role="option" onMouseDown={(event) => event.preventDefault()} onClick={() => submit(suggestion.value)}><span className="suggest-chip" style={{ background: SUGGESTION_COLORS[suggestion.type] ?? 'var(--severity-unknown)' }}>{suggestion.type}</span><span>{suggestion.label}</span><code className="suggest-value">{suggestion.value}</code></button>)}</div>}
        </div>
        <div className="result-meta"><span>Supported terms: <code>service:</code> <code>host:</code> <code>level:</code> <code>status:</code> <code>trace:</code></span><span>Press Enter to run</span></div>
      </div>

      {!applied ? <Card title="Search"><EmptyState><Search size={23} aria-hidden="true" /><strong>Start with a query.</strong><span>Search returns matches, snippets and the matcher selected by the backend for the loaded dataset.</span><Link className="btn btn-sm" to="/datasets">Open datasets</Link></EmptyState></Card> : loading ? <Card title="Searching"><Spinner label="Running the backend matcher…" /></Card> : error ? isMissingDataset(error) ? <NoDatasetState detail="Search needs a loaded dataset before it can return matches." /> : <ErrorBox error={error} retry={reload} /> : !data ? <Card title="Search"><EmptyState>No search response is available.</EmptyState></Card> : <>
        {data.suggestion && <div className="suggestion-banner" role="status"><Sparkles size={15} aria-hidden="true" /><span>Did you mean</span><button className="btn btn-sm" type="button" onClick={() => submit(data.suggestion!.suggestion)}>{data.suggestion.suggestion}</button><span className="muted">{data.suggestion.similarityPct}% similar · {formatNumber(data.suggestion.matchCount)} matches · {data.suggestion.algorithm} distance {data.suggestion.distance}</span></div>}
        <div className="stat-grid stat-grid--small">
          <StatCard label="Matching events" value={formatNumber(data.total)} color="var(--accent)" />
          <StatCard label="Hits on page" value={formatNumber(exactHits)} color="var(--ok)" />
          <StatCard label="Strategy" value={data.strategy} />
          <StatCard label="Measured duration" value={formatNanos(data.durationNanos)} color="var(--mod-parallel)" />
        </div>
        <Card title="Execution methodology" sub="Fields returned by POST /api/search" actions={refreshing ? <Badge tone="info">Refreshing</Badge> : undefined}>
          <div className="table-scroll"><table className="info-table"><caption className="sr-only">Search execution methodology</caption><tbody><tr><th>Query</th><td><code>{data.query || '—'}</code></td></tr><tr><th>Dataset</th><td>{data.dataset}</td></tr><tr><th>Strategy</th><td>{data.strategy}</td></tr><tr><th>Algorithm</th><td>{data.algorithm}</td></tr><tr><th>Pattern</th><td><code>{data.pattern || '—'}</code></td></tr><tr><th>Pattern length</th><td>{formatNumber(data.patternLength)}</td></tr><tr><th>Haystack</th><td>{formatNumber(data.textSize)} characters</td></tr><tr><th>Duration</th><td>{formatNanos(data.durationNanos)}</td></tr></tbody></table></div>
        </Card>
        <div className="result-meta" aria-live="polite"><span>{formatNumber(data.total)} matching events in {data.dataset}</span><span>Page {data.page} · {data.size} per page</span></div>
        {data.matches.length === 0 ? <Card title="Matches"><EmptyState><Search size={21} aria-hidden="true" /><strong>No events match this query.</strong><span>Try a broader term or use a suggestion from the search response.</span><button className="btn btn-sm" type="button" onClick={clearSearch}>Clear search</button></EmptyState></Card> : <Card title="Matches" sub="Select a row to inspect the normalized event or open its deep link."><div className="table-scroll"><table className="log-table"><caption className="sr-only">Search result events</caption><thead><tr><th>Time</th><th>Level</th><th>Service</th><th>Host</th><th>Message</th><th>HTTP</th><th>Hits</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{data.matches.map((hit) => <tr key={hit.event.id}><td className="ts">{formatTs(hit.event.timestamp)}</td><td><LevelBadge level={hit.event.level} /></td><td><Link to={`/services/${encodeURIComponent(hit.event.service)}`}>{hit.event.service}</Link></td><td className="muted">{hit.event.host}</td><td><Link className="log-cell" to={`/logs/${hit.event.id}`}><span className="log-cell-msg">{hit.event.message}</span>{hit.snippet && <code className="log-snippet">{hit.snippet}</code>}</Link></td><td>{hit.event.httpMethod ? <span className="http-pill">{hit.event.httpMethod} {hit.event.statusCode}</span> : <span className="muted">—</span>}{hit.event.responseTime > 0 && <div className="mono">{formatMillis(hit.event.responseTime)}</div>}</td><td className="num">{hit.matchCount}</td><td><button className="icon-btn" type="button" onClick={() => setSelected(hit.event)} aria-label={`Inspect event ${hit.event.id}`} title="Inspect event"><ExternalLink size={14} aria-hidden="true" /></button></td></tr>)}</tbody></table></div></Card>}
        {data.total > 0 && <div className="pager"><button className="btn btn-sm" type="button" disabled={page <= 1} onClick={() => setApplied({ ...applied, page: page - 1 })}>Previous</button><span className="pager-label">Page {page} of {totalPages}</span><button className="btn btn-sm" type="button" disabled={page >= totalPages} onClick={() => setApplied({ ...applied, page: page + 1 })}>Next</button></div>}
      </>}
      <EventDrawer event={selected} onClose={closeDrawer} />
    </div>
  );
}
