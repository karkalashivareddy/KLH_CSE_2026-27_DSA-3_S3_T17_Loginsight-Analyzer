import { useCallback, useEffect, useRef, useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { RunRecord, RunSummary, TraceResponse } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState } from '../components/ui';
import TracePlayer from '../components/TracePlayer';
import { formatNanos, formatTs, moduleForAlgorithm } from '../components/format';

function toTrace(run: RunRecord): TraceResponse {
  return {
    algorithm: run.algorithmName,
    category: run.category,
    result: run.result,
    intermediateData: null,
    steps: run.steps ?? [],
    truncated: run.truncated,
    executionTimeNanos: run.executionTimeNanos,
    timeComplexity: run.timeComplexity,
    spaceComplexity: run.spaceComplexity
  };
}

export default function RunsPage() {
  const { data: summaries, loading, error, reload } = useApi<RunSummary[]>(() => api.runs());

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [run, setRun] = useState<RunRecord | null>(null);
  const [runLoading, setRunLoading] = useState(false);
  const [live, setLive] = useState<TraceResponse | null>(null);
  const [streaming, setStreaming] = useState(false);
  const [streamMeta, setStreamMeta] = useState<Record<string, unknown> | null>(null);
  const [streamError, setStreamError] = useState<string | null>(null);
  const unsubscribeRef = useRef<(() => void) | null>(null);
  const selectedIdRef = useRef<string | null>(null);

  const open = useCallback(async (id: string) => {
    selectedIdRef.current = id;
    setSelectedId(id);
    setLive(null);
    setStreaming(false);
    setStreamMeta(null);
    setStreamError(null);
    unsubscribeRef.current?.();
    unsubscribeRef.current = null;
    setRunLoading(true);
    try {
      const record = await api.runGet(id);
      if (selectedIdRef.current !== id) return; // a newer selection superseded this request
      setRun(record);
    } catch (e) {
      if (selectedIdRef.current !== id) return;
      setRun(null);
      console.error(e);
    } finally {
      if (selectedIdRef.current === id) setRunLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!summaries || summaries.length === 0) return;
    if (!selectedId) {
      selectedIdRef.current = summaries[0].runId;
      setSelectedId(summaries[0].runId);
    }
  }, [summaries, selectedId]);

  useEffect(() => {
    if (selectedId && !run && !runLoading) void open(selectedId);
  }, [selectedId, run, runLoading, open]);

  useEffect(() => () => unsubscribeRef.current?.(), []);

  const stream = useCallback((id: string) => {
    unsubscribeRef.current?.();
    const steps: import('../api/types').TraceStep[] = [];
    setStreaming(true);
    setLive({ algorithm: '…', category: '…', result: null, intermediateData: null, steps: [], truncated: false, executionTimeNanos: 0, timeComplexity: '…', spaceComplexity: '…' });
    setStreamMeta(null);
    setStreamError(null);
    setStreamError(null);
    unsubscribeRef.current = api.runsStream(id, {
      onMeta: (meta) => {
        setStreamMeta(meta as unknown as Record<string, unknown>);
        setLive((prev) => prev ? {
          ...prev,
          algorithm: meta.algorithmName,
          category: meta.category,
          truncated: meta.truncated,
          timeComplexity: meta.timeComplexity,
          spaceComplexity: meta.spaceComplexity,
          result: meta.result
        } : prev);
      },
      onStep: (step) => {
        steps.push(step);
        setLive((prev) => prev ? { ...prev, steps: [...steps] } : prev);
      },
      onComplete: (complete) => {
        setStreamMeta((prev) => ({ ...prev, status: complete.status, executionTimeNanos: complete.executionTimeNanos }));
        setRun((prev) => prev ? {
          ...prev,
          status: complete.status as RunRecord['status'],
          executionTimeNanos: complete.executionTimeNanos,
          stepCount: complete.stepCount
        } : prev);
        setStreaming(false);
      },
      onError: (e) => {
        setStreaming(false);
        setStreamError(e.message || 'Connection to the server was lost while streaming this run.');
        console.error('SSE error', e);
      },
      onClose: () => setStreaming(false)
    });
  }, []);

  const stopStream = useCallback(() => {
    unsubscribeRef.current?.();
    unsubscribeRef.current = null;
    setStreaming(false);
    setStreamError(null);
  }, []);

  if (loading) return <Spinner label="Loading run history…" />;
  if (error) return <ErrorBox error={error} retry={reload} />;

  const list = summaries ?? [];

  const accentId = moduleForAlgorithm(run?.algorithm ?? '');
  const failedStream = streamMeta !== null && streamMeta.status === 'FAILED';
  const failMessage =
    streamError ??
    (failedStream
      ? String(streamMeta.error ?? run?.error ?? 'This run failed before any steps were recorded.')
      : run?.status === 'FAILED'
        ? (run.error ?? 'This run failed before any steps were recorded.')
        : null);

  return (
    <div className="page">
      <h2 className="page-title">Run Sessions</h2>
      <p className="lab-intro">
        Every run below is a real recorded execution. Open a session to replay its steps, or stream the
        same trace live over SSE from the backend recorder.
      </p>

      {list.length === 0 ? (
        <EmptyState>
          No recorded runs yet. Run an algorithm in the <strong>Laboratory</strong> or through{' '}
          <strong>TextHack</strong> to create the first session.
        </EmptyState>
      ) : (
        <div className="run-layout">
          {/* Run list */}
          <Card>
            <div className="run-list">
              {list.map((s) => (
                <button
                  key={s.runId}
                  className={`run-item${s.runId === selectedId ? ' run-item--active' : ''}`}
                  onClick={() => void open(s.runId)}
                >
                  <div className="run-item-top">
                    <span className="run-item-name">{s.algorithmName}</span>
                    <span className={`status-pill status-pill--${s.status}`}>{s.status}</span>
                  </div>
                  <div className="run-item-meta">
                    <span>{formatTs(s.createdAt)}</span>
                    <span>{s.stepCount} steps</span>
                    <span>{s.executionTimeNanos !== 0 ? formatNanos(s.executionTimeNanos) : '—'}</span>
                  </div>
                  {s.truncated && <span className="badge badge--truncated">truncated</span>}
                </button>
              ))}
            </div>
          </Card>

          {/* Detail + replay */}
          <div>
            {runLoading ? (
              <Spinner label="Loading session…" />
            ) : run ? (
              <Card
                title={`${run.algorithmName}`}
                actions={
                  <div className="card-actions">
                    <button className="btn btn-sm" onClick={() => stream(run.runId)} disabled={streaming}>
                      {streaming ? 'Streaming…' : 'Stream live (SSE)'}
                    </button>
                    <button className="btn btn-sm" onClick={stopStream} disabled={!streaming}>
                      Stop
                    </button>
                  </div>
                }
              >
                <div className="lab-meta">
                  <span className="badge">{run.category}</span>
                  <span className="trace-time">{run.timeComplexity} · {run.spaceComplexity}</span>
                  <span className="badge">id {run.runId}</span>
                  {streamMeta && (
                    <span className="badge">{streamMeta.status as string}</span>
                  )}
                </div>

                {failMessage ? (
                  <ErrorBox
                    error={new Error(failMessage)}
                    retry={failedStream ? () => stream(run.runId) : undefined}
                  />
                ) : live && live.steps.length > 0 ? (
                  <TracePlayer trace={live} accentId={accentId} />
                ) : !live ? (
                  <TracePlayer trace={toTrace(run)} accentId={accentId} />
                ) : (
                  <div className="empty-state">
                    {streaming ? 'Waiting for streamed steps…' : 'The stream recorded no steps for this run.'}
                  </div>
                )}

                <div className="result-grid">
                  <div className="collapsible">
                    <div className="collapsible-head"><span>Result</span></div>
                    <pre className="json-block">{JSON.stringify(run.result, null, 2)}</pre>
                  </div>
                  <div className="collapsible">
                    <div className="collapsible-head"><span>Input</span></div>
                    <pre className="json-block">{JSON.stringify(run.input, null, 2)}</pre>
                  </div>
                </div>
              </Card>
            ) : (
              <EmptyState>Select a run from the list.</EmptyState>
            )}
          </div>
        </div>
      )}
    </div>
  );
}