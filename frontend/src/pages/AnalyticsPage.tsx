import { useState } from 'react';
import { BarChart3, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useApi, type UseApiResult } from '../hooks/useApi';
import { api } from '../api/client';
import type { Heatmap as HeatmapDTO, HostRow, HttpStatsDto, OverviewDto } from '../api/types';
import { Badge, Card, DonutChart, EmptyState, ErrorBox, HBarChart, HeatmapGrid, LevelBadge, NoDatasetState, PageHeader, Spinner, StatCard, TimeChart } from '../components/ui';
import { LEVEL_COLORS, formatMillis, formatNumber, formatTs } from '../components/format';

const RANGES = ['5m', '15m', '1h', '6h', '24h'] as const;
const TABS = [
  { id: 'timeline', label: 'Timeline' },
  { id: 'severity', label: 'Severity' },
  { id: 'heatmap', label: 'Activity heatmap' },
  { id: 'http', label: 'HTTP' },
  { id: 'hosts', label: 'Hosts' }
] as const;
type Tab = (typeof TABS)[number]['id'];

function isMissingDataset(error: Error): boolean {
  const status = (error as Error & { apiError?: { status?: number } }).apiError?.status;
  return status === 404;
}

function sourceState(result: UseApiResult<unknown>): { label: string; tone: 'good' | 'warn' | 'danger' | 'info' } {
  if (result.loading) return { label: 'Loading', tone: 'info' };
  if (result.error) return { label: isMissingDataset(result.error) ? 'No dataset' : 'Unavailable', tone: 'danger' };
  if (result.data === null) return { label: 'No response', tone: 'warn' };
  return { label: 'Ready', tone: 'good' };
}

export default function AnalyticsPage() {
  const [tab, setTab] = useState<Tab>('timeline');
  const [range, setRange] = useState('1h');
  const overview = useApi<OverviewDto | null>((signal) => api.overview(range, { signal }), `overview-${range}`);
  const heatmap = useApi<HeatmapDTO>((signal) => api.heatmap({ signal }));
  const http = useApi<HttpStatsDto>((signal) => api.httpAnalytics({ signal }));
  const hosts = useApi<HostRow[]>((signal) => api.hostAnalytics(25, { signal }));
  const active = tab === 'timeline' || tab === 'severity' ? overview : tab === 'heatmap' ? heatmap : tab === 'http' ? http : hosts;

  return (
    <div className="page">
      <PageHeader eyebrow="Observe" title="Analytics" description="Traffic, severity, HTTP and host aggregates computed by the backend from the loaded dataset." actions={<button className="btn btn-sm" type="button" onClick={active.reload} disabled={active.refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh view</button>} />
      <div className="tab-bar" role="tablist" aria-label="Analytics views">
        {TABS.map((value) => <button key={value.id} id={`analytics-tab-${value.id}`} className={`btn btn-sm${tab === value.id ? ' btn-primary' : ''}`} type="button" role="tab" aria-selected={tab === value.id} aria-controls={`analytics-panel-${value.id}`} tabIndex={tab === value.id ? 0 : -1} onClick={() => setTab(value.id)}>{value.label}</button>)}
      </div>
      <div id={`analytics-panel-${tab}`} role="tabpanel" aria-labelledby={`analytics-tab-${tab}`} tabIndex={0}>
        {active.loading ? <Card title="Loading analytics"><Spinner label="Requesting the selected analytics view…" /></Card> : active.error ? isMissingDataset(active.error) ? <NoDatasetState detail="Load a dataset before requesting analytics aggregates." /> : <ErrorBox error={active.error} retry={active.reload} /> : tab === 'timeline' ? <TimelineTab overview={overview} range={range} setRange={setRange} /> : tab === 'severity' ? <SeverityTab overview={overview} /> : tab === 'heatmap' ? <HeatmapTab heatmap={heatmap} /> : tab === 'http' ? <HttpTab http={http} /> : <HostsTab hosts={hosts} />}
      </div>
      <SourceCoverage overview={overview} heatmap={heatmap} http={http} hosts={hosts} />
    </div>
  );
}

function SourceCoverage({ overview, heatmap, http, hosts }: { overview: UseApiResult<OverviewDto | null>; heatmap: UseApiResult<HeatmapDTO>; http: UseApiResult<HttpStatsDto>; hosts: UseApiResult<HostRow[]> }) {
  const sources = [
    { label: 'Overview timeline and severity', result: overview as UseApiResult<unknown>, reload: overview.reload },
    { label: 'Activity heatmap', result: heatmap as UseApiResult<unknown>, reload: heatmap.reload },
    { label: 'HTTP analytics', result: http as UseApiResult<unknown>, reload: http.reload },
    { label: 'Host analytics', result: hosts as UseApiResult<unknown>, reload: hosts.reload }
  ];
  return <Card title="Data source coverage" sub="Independent endpoint states; an unavailable source does not replace the active view."><div className="table-scroll"><table className="info-table"><caption className="sr-only">Analytics source coverage</caption><thead><tr><th>Source</th><th>State</th><th>Action</th></tr></thead><tbody>{sources.map((source) => { const state = sourceState(source.result); return <tr key={source.label}><td>{source.label}</td><td><Badge tone={state.tone}>{state.label}</Badge></td><td><button className="btn btn-sm" type="button" onClick={source.reload} disabled={source.result.refreshing}>{source.result.refreshing ? 'Retrying…' : 'Retry'}</button></td></tr>; })}</tbody></table></div></Card>;
}

function TimelineTab({ overview, range, setRange }: { overview: UseApiResult<OverviewDto | null>; range: string; setRange: (value: string) => void }) {
  const { data, error, reload } = overview;
  if (error) return isMissingDataset(error) ? <NoDatasetState detail="Load a dataset before requesting the traffic timeline." /> : <ErrorBox error={error} retry={reload} />;
  if (!data) return <NoDatasetState />;
  const timeline = data.timeline.map((point) => ({ label: formatTs(point.start), value: point.count }));
  const rangeEvents = data.timeline.reduce((sum, point) => sum + point.count, 0);
  return <><div className="tab-bar" role="group" aria-label="Timeline range">{RANGES.map((value) => <button key={value} className={`btn btn-sm${range === value ? ' btn-primary' : ''}`} type="button" aria-pressed={range === value} onClick={() => setRange(value)}>{value}</button>)}</div><div className="stat-grid"><StatCard label="Events / min" value={data.eventsPerMinute.toFixed(2)} color="var(--accent)" /><StatCard label="Events in returned buckets" value={formatNumber(rangeEvents)} /><StatCard label="Dataset" value={data.dataset} /><StatCard label="Selected range" value={data.range} /></div><Card title="Traffic series" sub="Buckets returned by the overview endpoint"><TimeChart data={timeline} height={200} label="Observed traffic timeline" /></Card></>;
}

function SeverityTab({ overview }: { overview: UseApiResult<OverviewDto | null> }) {
  const { data, error, reload } = overview;
  if (error) return isMissingDataset(error) ? <NoDatasetState detail="Load a dataset before requesting severity aggregates." /> : <ErrorBox error={error} retry={reload} />;
  if (!data) return <NoDatasetState />;
  const severity = Object.entries(data.severity).map(([label, value]) => ({ label, value, color: LEVEL_COLORS[label.toUpperCase()] ?? 'var(--severity-unknown)' }));
  return <><div className="grid-2"><Card title="Severity distribution" sub="Normalized levels returned by the overview endpoint"><DonutChart data={severity} size={160} /></Card><Card title="Severity counts" sub="Select a level to open the matching event stream."><div className="table-scroll"><table className="log-table"><caption className="sr-only">Severity counts</caption><thead><tr><th>Level</th><th>Events</th><th>Share</th></tr></thead><tbody>{severity.map((entry) => <tr key={entry.label}><td><Link to={`/logs?q=${encodeURIComponent(`level:${entry.label}`)}`}><LevelBadge level={entry.label} /></Link></td><td className="num">{formatNumber(entry.value)}</td><td className="num">{data.events > 0 ? `${((entry.value / data.events) * 100).toFixed(2)}%` : '—'}</td></tr>)}</tbody></table></div></Card></div></>;
}

function HeatmapTab({ heatmap }: { heatmap: UseApiResult<HeatmapDTO> }) {
  const { data, error, reload } = heatmap;
  if (error) return isMissingDataset(error) ? <NoDatasetState detail="Load a dataset before requesting the activity heatmap." /> : <ErrorBox error={error} retry={reload} />;
  if (!data) return <NoDatasetState />;
  return <Card title="Activity heatmap" sub="Day of week by UTC hour; zero cells are preserved by the API."><div className="table-scroll"><HeatmapGrid days={data.days} columns={data.columns} cells={data.cells} /></div></Card>;
}

function latencyText(value: number): string {
  return value > 0 ? formatMillis(value) : '—';
}

function HttpTab({ http }: { http: UseApiResult<HttpStatsDto> }) {
  const { data, error, reload } = http;
  if (error) return isMissingDataset(error) ? <NoDatasetState detail="Load a dataset before requesting HTTP analytics." /> : <ErrorBox error={error} retry={reload} />;
  if (!data) return <NoDatasetState />;
  if (data.sampled === 0) return <EmptyState><BarChart3 size={22} aria-hidden="true" /><strong>No HTTP events are present.</strong><span>The endpoint returned no events carrying an HTTP method or status code.</span></EmptyState>;
  const status = Object.entries(data.statusCodes).map(([label, value]) => ({ label: `HTTP ${label}`, value }));
  const methods = Object.entries(data.methods).map(([label, value]) => ({ label, value }));
  return <><div className="stat-grid"><StatCard label="HTTP events sampled" value={formatNumber(data.sampled)} color="var(--accent)" /><StatCard label="p50 latency" value={latencyText(data.latencyP50)} /><StatCard label="p95 latency" value={latencyText(data.latencyP95)} color="var(--warn)" /><StatCard label="max latency" value={latencyText(data.latencyMax)} color="var(--danger)" /></div><div className="grid-2"><Card title="Status codes" sub="Observed response classes"><HBarChart data={status} maxItems={12} /></Card><Card title="Methods" sub="Observed request methods"><HBarChart data={methods} maxItems={8} /></Card></div><Card title="Top endpoints" sub="Endpoint counts returned by the backend"><div className="table-scroll"><table className="log-table"><caption className="sr-only">Top HTTP endpoints</caption><thead><tr><th>Endpoint</th><th>Events</th><th>Share of HTTP sample</th></tr></thead><tbody>{data.endpoints.map((endpoint) => <tr key={endpoint.endpoint}><td><code>{endpoint.endpoint}</code></td><td className="num">{formatNumber(endpoint.count)}</td><td className="num">{data.sampled > 0 ? `${((endpoint.count / data.sampled) * 100).toFixed(2)}%` : '—'}</td></tr>)}</tbody></table></div></Card></>;
}

function HostsTab({ hosts }: { hosts: UseApiResult<HostRow[]> }) {
  const { data, error, reload } = hosts;
  if (error) return isMissingDataset(error) ? <NoDatasetState detail="Load a dataset before requesting host analytics." /> : <ErrorBox error={error} retry={reload} />;
  if (!data || data.length === 0) return <EmptyState>No hosts are present in the current dataset.</EmptyState>;
  return <Card title="Hosts" sub="Error rate is calculated by the backend"><div className="table-scroll"><table className="log-table"><caption className="sr-only">Host analytics</caption><thead><tr><th>Host</th><th>Events</th><th>Errors</th><th>Warnings</th><th>Error rate</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{data.map((host) => <tr key={host.host}><td><code>{host.host}</code></td><td className="num">{formatNumber(host.events)}</td><td className="num" style={{ color: host.errors > 0 ? 'var(--severity-error)' : undefined }}>{formatNumber(host.errors)}</td><td className="num">{formatNumber(host.warnings)}</td><td className="num">{host.errorRate.toFixed(2)}%</td><td><Link className="btn btn-sm" to={`/logs?q=${encodeURIComponent(`host:${host.host}`)}`}>Events</Link></td></tr>)}</tbody></table></div></Card>;
}
