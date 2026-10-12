import { useCallback, useEffect, useRef, useState } from 'react';
import { ExternalLink, History, Play, RefreshCw, Square } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { RunMetaEvent, RunRecord, RunSummary, TraceResponse, TraceStep } from '../api/types';
import TracePlayer from '../components/TracePlayer';
import { Badge, Card, EmptyState, ErrorBox, PageHeader, Spinner, StatCard, StatusPill } from '../components/ui';
import { formatNanos, formatTs, moduleForAlgorithm } from '../components/format';

function toTrace(run: RunRecord): TraceResponse {
  return { algorithm: run.algorithmName, category: run.category, result: run.result, intermediateData: null, steps: run.steps ?? [], truncated: run.truncated, executionTimeNanos: run.executionTimeNanos, timeComplexity: run.timeComplexity, spaceComplexity: run.spaceComplexity };
}

export default function RunsPage() {
  const { id: routeId } = useParams<{ id?: string }>();
  const navigate = useNavigate();
  const summaries = useApi<RunSummary[]>((signal) => api.runs({ signal }));
  const [selectedId, setSelectedId] = useState<string | null>(routeId ?? null);
  const [run, setRun] = useState<RunRecord | null>(null);
  const [runLoading, setRunLoading] = useState(false);
  const [runError, setRunError] = useState<Error | null>(null);
  const [live, setLive] = useState<TraceResponse | null>(null);
  const [streaming, setStreaming] = useState(false);
  const [streamMeta, setStreamMeta] = useState<RunMetaEvent | null>(null);
  const [streamError, setStreamError] = useState<string | null>(null);
  const [streamGeneration, setStreamGeneration] = useState(0);
  const unsubscribeRef = useRef<(() => void) | null>(null);
  const runAbortRef = useRef<AbortController | null>(null);
  const selectedIdRef = useRef<string | null>(routeId ?? null);
  const streamGenerationRef = useRef(0);

  const open = useCallback(async (id: string) => {
    runAbortRef.current?.abort();
    unsubscribeRef.current?.();
    unsubscribeRef.current = null;
    streamGenerationRef.current += 1;
    const controller = new AbortController();
    runAbortRef.current = controller;
    selectedIdRef.current = id;
    setRun(null);
    setRunError(null);
    setLive(null);
    setStreamMeta(null);
    setStreamError(null);
    setStreamGeneration(streamGenerationRef.current);
    setStreaming(false);
    setRunLoading(true);
    try {
      const record = await api.runGet(id, { signal: controller.signal });
      if (selectedIdRef.current !== id || controller.signal.aborted) return;
      setRun(record);
    } catch (error) {
      if (selectedIdRef.current !== id || controller.signal.aborted || (error instanceof Error && error.name === 'AbortError')) return;
      setRunError(error instanceof Error ? error : new Error(String(error)));
    } finally {
      if (selectedIdRef.current === id && !controller.signal.aborted) setRunLoading(false);
    }
  }, []);

  useEffect(() => {
    selectedIdRef.current = routeId ?? null;
    setSelectedId(routeId ?? null);
  }, [routeId]);

  const list = summaries.data ?? [];

  useEffect(() => {
    if (selectedId === null && list.length > 0) setSelectedId(list[0].runId);
  }, [list, selectedId]);

  useEffect(() => {
    if (selectedId && run?.runId !== selectedId && !runLoading && !runError) void open(selectedId);
  }, [open, run?.runId, runLoading, runError, selectedId]);

  useEffect(() => () => {
    runAbortRef.current?.abort();
    unsubscribeRef.current?.();
  }, []);

  const stream = useCallback((id: string) => {
    unsubscribeRef.current?.();
    const generation = ++streamGenerationRef.current;
    const steps: TraceStep[] = [];
    setStreamGeneration(generation);
    setStreaming(true);
    setStreamError(null);
    setStreamMeta(null);
    setLive({ algorithm: 'Waiting for metadata', category: 'SSE replay', result: null, intermediateData: null, steps: [], truncated: false, executionTimeNanos: 0, timeComplexity: '—', spaceComplexity: '—' });
    const current = () => generation === streamGenerationRef.current && selectedIdRef.current === id;
    unsubscribeRef.current = api.runsStream(id, {
      onMeta: (meta) => {
        if (!current()) return;
        setStreamMeta(meta);
        setLive((previous) => previous ? { ...previous, algorithm: meta.algorithmName, category: meta.category, truncated: meta.truncated, timeComplexity: meta.timeComplexity, spaceComplexity: meta.spaceComplexity, result: meta.result, executionTimeNanos: meta.executionTimeNanos } : previous);
      },
      onStep: (step) => {
        if (!current()) return;
        steps.push(step);
        setLive((previous) => previous ? { ...previous, steps: [...steps] } : previous);
      },
      onComplete: (complete) => {
        if (!current()) return;
        setStreamMeta((previous) => previous ? { ...previous, status: complete.status, executionTimeNanos: complete.executionTimeNanos, stepCount: complete.stepCount, truncated: complete.truncated } : previous);
        setRun((previous) => previous ? { ...previous, status: complete.status as RunRecord['status'], executionTimeNanos: complete.executionTimeNanos, stepCount: complete.stepCount, truncated: complete.truncated } : previous);
        setStreaming(false);
      },
      onError: (error) => {
        if (!current()) return;
        setStreaming(false);
        setStreamError(error.message || 'The SSE connection was lost while streaming this run.');
      },
      onClose: () => {
        if (current()) setStreaming(false);
      }
    });
  }, []);

  const stopStream = useCallback(() => {
    streamGenerationRef.current += 1;
    unsubscribeRef.current?.();
    unsubscribeRef.current = null;
    setStreaming(false);
  }, []);

  const selectRun = (id: string) => {
    setSelectedId(id);
    selectedIdRef.current = id;
    navigate(`/runs/${encodeURIComponent(id)}`);
  };

  if (summaries.loading) return <div className="page"><PageHeader eyebrow="Analysis" title="Run sessions" description="Open a recorded execution or stream its genuine operation ledger over SSE." /><Card title="Loading run history"><Spinner label="Requesting recorded executions…" /></Card></div>;
  if (summaries.error) return <div className="page"><PageHeader eyebrow="Analysis" title="Run sessions" description="Open a recorded execution or stream its genuine operation ledger over SSE." /><ErrorBox error={summaries.error} retry={summaries.reload} /></div>;
  if (list.length === 0 && selectedId === null) return <div className="page"><PageHeader eyebrow="Analysis" title="Run sessions" description="Open a recorded execution or stream its genuine operation ledger over SSE." /><EmptyState><History size={23} aria-hidden="true" /><strong>No recorded runs yet.</strong><span>Run a trace-capable algorithm from the catalogue to create a session.</span><Link className="btn btn-sm" to="/algorithms">Open algorithms</Link></EmptyState></div>;

  const completed = list.filter((summary) => summary.status === 'COMPLETED').length;
  const failed = list.filter((summary) => summary.status === 'FAILED').length;
  const steps = list.reduce((sum, summary) => sum + summary.stepCount, 0);
  const streamKey = `${selectedId ?? 'none'}:${streamGeneration}`;
  const failedStream = streamMeta?.status === 'FAILED';
  const failMessage = streamError ?? (failedStream ? streamMeta?.error ?? run?.error ?? 'The run failed before recording steps.' : run?.status === 'FAILED' ? run.error ?? 'The run failed before recording steps.' : null);

  return (
    <div className="page">
      <PageHeader eyebrow="Analysis" title="Run sessions" description="Open a recorded execution or stream its genuine operation ledger over SSE. The browser only renders server events." actions={<button className="btn btn-sm" type="button" onClick={summaries.reload} disabled={summaries.refreshing}><RefreshCw size={14} aria-hidden="true" /> Refresh history</button>} />
      <div className="stat-grid stat-grid--small"><StatCard label="Recorded sessions" value={list.length} color="var(--accent)" /><StatCard label="Completed" value={completed} color="var(--ok)" /><StatCard label="Failed" value={failed} color="var(--danger)" /><StatCard label="Recorded steps" value={steps} color="var(--mod-parallel)" /><StatCard label="SSE state" value={streaming ? 'Streaming' : 'Idle'} color={streaming ? 'var(--accent)' : 'var(--text-faint)'} /></div>
      <div className="run-layout">
        <Card title={`History (${list.length})`} sub="Newest backend records first" actions={summaries.refreshing ? <Badge tone="info">Refreshing</Badge> : undefined}>
          <div className="run-list">{list.map((summary) => <button key={summary.runId} type="button" className={`run-item${summary.runId === selectedId ? ' run-item--active' : ''}`} aria-pressed={summary.runId === selectedId} onClick={() => selectRun(summary.runId)}><div className="run-item-top"><span className="run-item-name">{summary.algorithmName}</span><StatusPill status={summary.status} /></div><div className="run-item-meta"><span>{formatTs(summary.createdAt)}</span><span>{summary.stepCount} steps</span><span>{summary.executionTimeNanos > 0 ? formatNanos(summary.executionTimeNanos) : '—'}</span></div>{summary.truncated && <span className="badge badge--warn">truncated</span>}</button>)}</div>
        </Card>
        <div>
          {runLoading ? <Card title="Loading session"><Spinner label="Requesting the selected run…" /></Card> : runError ? <ErrorBox error={runError} retry={() => selectedId && void open(selectedId)} /> : run ? <Card title={run.algorithmName} sub={run.runId} actions={<div className="card-actions"><button className="btn btn-sm" type="button" onClick={() => stream(run.runId)} disabled={streaming}><Play size={13} aria-hidden="true" /> {streaming ? 'Streaming…' : 'Stream SSE'}</button><button className="btn btn-sm" type="button" onClick={stopStream} disabled={!streaming}><Square size={13} aria-hidden="true" /> Stop</button><Link className="btn btn-sm" to={`/runs/${encodeURIComponent(run.runId)}`}><ExternalLink size={13} aria-hidden="true" /> Deep link</Link></div>}>
            <div className="lab-meta"><StatusPill status={run.status} /><span className="badge">{run.category}</span><span className="trace-time">{run.timeComplexity} · {run.spaceComplexity}</span><span className="trace-time">{run.stepCount} steps</span><span className="trace-time">{run.executionTimeNanos > 0 ? formatNanos(run.executionTimeNanos) : '—'}</span>{run.truncated && <Badge tone="warn">truncated</Badge>}{streamMeta && <Badge tone="info">stream {streamMeta.status}</Badge>}</div>
            {failMessage && <ErrorBox error={new Error(failMessage)} retry={failedStream ? () => stream(run.runId) : undefined} />}
            {live ? live.steps.length > 0 ? <TracePlayer trace={live} accentId={moduleForAlgorithm(run.algorithm)} resetKey={streamKey} /> : <EmptyState>{streaming ? 'Waiting for streamed steps from the server…' : 'The server stream ended without a recorded step.'}</EmptyState> : <TracePlayer trace={toTrace(run)} accentId={moduleForAlgorithm(run.algorithm)} resetKey={run.runId} />}
            <div className="result-grid"><div className="collapsible"><div className="collapsible-head">Result payload</div><pre className="json-block">{JSON.stringify(run.result, null, 2)}</pre></div><div className="collapsible"><div className="collapsible-head">Input payload</div><pre className="json-block">{JSON.stringify(run.input, null, 2)}</pre></div></div>
          </Card> : <Card title="Run detail"><EmptyState>Select a recorded run from the history.</EmptyState></Card>}
        </div>
      </div>
    </div>
  );
}
