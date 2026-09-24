import { useCallback, useEffect, useRef, useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import { Link } from 'react-router-dom';
import type { LiveBatch, LiveReplayComplete, LiveStartEvent, LiveStatus, LogEvent } from '../api/types';
import { Card, Spinner, ErrorBox, EmptyState, LevelBadge, Badge } from '../components/ui';
import { formatNumber, formatTs } from '../components/format';

const MAX_EVENTS = 300;

/**
 * Live stream (docs/API.md §9). The backend owns a static dataset, so the stream is an honest
 * replay — events are emitted oldest-first at a bounded pace and labelled "demo replay stream —
 * not real-time". The UI keeps that label visible at all times.
 */
export default function LivePage() {
  const status = useApi<LiveStatus>(() => api.liveStatus());
  const [running, setRunning] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [start, setStart] = useState<LiveStartEvent | null>(null);
  const [events, setEvents] = useState<LogEvent[]>([]);
  const [emitted, setEmitted] = useState(0);
  const [total, setTotal] = useState(0);
  const unsubscribeRef = useRef<(() => void) | null>(null);

  const stop = useCallback(() => {
    unsubscribeRef.current?.();
    unsubscribeRef.current = null;
    setRunning(false);
  }, []);

  useEffect(() => () => stop(), [stop]);

  const startStream = useCallback(() => {
    setError(null);
    setDone(false);
    setEvents([]);
    setEmitted(0);
    setTotal(status.data?.total ?? 0);
    setRunning(true);
    unsubscribeRef.current = api.liveStream({
      onStart: (s: LiveStartEvent) => {
        setStart(s);
        setTotal(s.total);
      },
      onBatch: (b: LiveBatch) => {
        setEmitted(b.emittedCount);
        setEvents((prev) => [...b.events.reverse(), ...prev].slice(0, MAX_EVENTS));
      },
      onComplete: (c: LiveReplayComplete) => {
        setEmitted(c.emitted);
        setTotal(c.total);
        setDone(true);
        setRunning(false);
      },
      onError: (e: Error) => {
        setError(e.message);
        setRunning(false);
      },
      onClose: () => setRunning(false)
    });
  }, [status.data]);

  if (status.loading) return <Spinner label="Loading stream status…" />;
  if (status.error) return <ErrorBox error={status.error} retry={status.reload} />;

  const paused = (status.data?.enabled ?? false) && !running && !done && error === null;

  return (
    <div className="page">
      <h2 className="page-title">Live Stream</h2>

      <Card title="Stream Source">
        <div className="stream-source">
          <span className={`status-pill status-pill--${status.data?.enabled ? 'COMPLETED' : 'FAILED'}`}>
            {status.data?.enabled ? 'enabled' : 'disabled'}
          </span>
          <span><strong>{status.data?.dataset ?? '—'}</strong> · {formatNumber(status.data?.total ?? 0)} events replayable</span>
          <span className="stream-label">{status.data?.label ?? '—'}</span>
        </div>
        <div className="quick-actions" style={{ marginTop: '0.6rem' }}>
          {!running && !done && (
            <button className="btn btn-run" onClick={startStream} disabled={!status.data?.enabled}>
              ▶ Start replay
            </button>
          )}
          {running && <button className="btn btn-danger" onClick={stop}>■ Stop</button>}
          {done && paused && <span className="muted" style={{ alignSelf: 'center' }}>Replay complete — press Start to replay again.</span>}
        </div>
      </Card>

      {error && <div className="error-box"><div className="error-msg">{error}</div></div>}

      {start && (
        <Card title={`Replay progress${running ? ' (streaming)' : done ? ' (complete)' : ''}`}>
          <div className="progress-track">
            <div
              className="progress-fill"
              style={{ width: `${total > 0 ? (emitted / total) * 100 : 0}%` }}
            />
          </div>
          <div className="progress-meta">
            <span>{formatNumber(emitted)} / {formatNumber(total)} events</span>
            <span className="muted">batch {start.batchSize} · {start.paceMs} ms pace</span>
          </div>
        </Card>
      )}

      {events.length > 0 && (
        <Card title="Streamed Events" sub={start?.label ?? (status.data?.label ?? '')}>
          <div className="log-list">
            {events.map((e) => (
              <Link key={`${e.id}-${e.timestamp}`} className="log-item" to={`/logs/${e.id}`}>
                <div className="log-item-top">
                  <LevelBadge level={e.level} />
                  <span className="log-item-service">{e.service}</span>
                  <span className="log-item-host">{e.host}</span>
                  <span className="log-item-time">{formatTs(e.timestamp)}</span>
                </div>
                <div className="log-item-msg">{e.message}</div>
                <div className="log-item-meta">
                  {e.httpMethod && <Badge>{e.httpMethod}</Badge>}
                  {e.statusCode > 0 && <Badge>{e.statusCode}</Badge>}
                </div>
              </Link>
            ))}
          </div>
        </Card>
      )}

      {events.length === 0 && !running && !done && (
        <EmptyState>No events streamed yet — press <strong>Start replay</strong> to begin.</EmptyState>
      )}
    </div>
  );
}