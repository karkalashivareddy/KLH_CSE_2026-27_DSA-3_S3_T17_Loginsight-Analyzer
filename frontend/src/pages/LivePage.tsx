import { useCallback, useState } from 'react';
import { Database, Pause, Play, Radio, RefreshCw, RotateCcw } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useReplay } from '../replay/ReplayContext';
import type { LogEvent } from '../api/types';
import { Badge, Card, EmptyState, ErrorBox, EventDrawer, LevelBadge, PageHeader, Spinner, StatusPill } from '../components/ui';
import { formatNumber, formatTs } from '../components/format';

export default function LivePage() {
  const {
    state: streamState,
    statusLabel,
    liveStatus,
    statusLoading,
    statusError,
    error,
    startEvent,
    currentDataset,
    emitted,
    total,
    progress,
    recentEvents,
    isRunning,
    start,
    stop,
    restart,
    reloadStatus
  } = useReplay();
  const [batchSize, setBatchSize] = useState(50);
  const [intervalMs, setIntervalMs] = useState(700);
  const [selected, setSelected] = useState<LogEvent | null>(null);
  const startReplay = useCallback(() => start({ batchSize, intervalMs }), [batchSize, intervalMs, start]);
  const restartReplay = useCallback(() => restart({ batchSize, intervalMs }), [batchSize, intervalMs, restart]);

  if (statusLoading) return <Spinner label="Loading stream status…" />;
  if (statusError) return <ErrorBox error={statusError} retry={reloadStatus} />;
  if (!liveStatus) return <EmptyState>No live status is available.</EmptyState>;

  const progressTitle = streamState === 'starting' ? 'Connecting to replay' : streamState === 'streaming' ? 'Replay in progress' : streamState === 'complete' ? 'Replay complete' : streamState === 'stopped' ? 'Replay stopped' : streamState === 'error' ? 'Replay error' : 'Replay progress';
  const hasStreamState = Boolean(startEvent || isRunning || streamState === 'complete' || emitted > 0);

  return (
    <div className="page">
      <PageHeader
        eyebrow="Observe"
        title="Live Replay"
        description="Demo replay: a bounded SSE replay of the loaded dataset. This is not a real-time capture feed."
        actions={<button className="btn btn-sm" type="button" onClick={reloadStatus} disabled={statusLoading}><RefreshCw size={14} aria-hidden="true" /> Status</button>}
      />
      <Card title="Stream source" sub={startEvent?.label ?? liveStatus.label}>
        <div className="stream-source">
          <Badge tone="info" label="Demo replay of the loaded dataset, not live production telemetry">DEMO REPLAY</Badge>
          <StatusPill status={liveStatus.enabled ? 'ENABLED' : 'DISABLED'} />
          <strong>{currentDataset ?? 'No dataset'}</strong>
          <span>{formatNumber(total || liveStatus.total)} replayable events</span>
          <span className="stream-label">source: {startEvent?.source ?? liveStatus.source ?? '—'}</span>
          <span className="text-muted" aria-live="polite">Replay state: {statusLabel}</span>
        </div>
        <div className="explorer-filters stream-controls">
          <label className="form-field">
            <span className="form-label">Batch size</span>
            <select className="select" value={batchSize} onChange={(event) => setBatchSize(Number(event.target.value))} disabled={isRunning}>
              <option value={10}>10</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
              <option value={200}>200</option>
            </select>
          </label>
          <label className="form-field">
            <span className="form-label">Pace</span>
            <select className="select" value={intervalMs} onChange={(event) => setIntervalMs(Number(event.target.value))} disabled={isRunning}>
              <option value={100}>100 ms</option>
              <option value={300}>300 ms</option>
              <option value={700}>700 ms</option>
              <option value={1500}>1500 ms</option>
            </select>
          </label>
          {!isRunning ? <button className="btn btn-run" type="button" onClick={streamState === 'complete' ? restartReplay : startReplay} disabled={!liveStatus.enabled}><Play size={14} aria-hidden="true" />{streamState === 'complete' ? 'Replay again' : 'Start replay'}</button> : <button className="btn btn-danger" type="button" onClick={stop}><Pause size={14} aria-hidden="true" /> Stop</button>}
          {streamState === 'stopped' && <button className="btn btn-sm" type="button" onClick={restartReplay}><RotateCcw size={13} aria-hidden="true" /> Restart</button>}
        </div>
      </Card>

      {error && <ErrorBox error={error} retry={startReplay} />}
      {hasStreamState && <Card title={progressTitle}>
        <div className="progress-track" role="progressbar" aria-label="Replay progress" aria-valuenow={emitted} aria-valuemin={0} aria-valuemax={Math.max(total, 1)}>
          <div className="progress-fill" style={{ width: `${progress}%` }} />
        </div>
        <div className="progress-meta"><span>{formatNumber(emitted)} / {formatNumber(total)} events</span><span>batch {startEvent?.batchSize ?? batchSize} · {startEvent?.paceMs ?? intervalMs} ms pace</span></div>
      </Card>}

      {recentEvents.length > 0 ? <Card title="Streamed events" sub={startEvent?.label ?? liveStatus.label}>
        <div className="log-list">
          {recentEvents.map((event) => <div className="log-item" key={`${event.id}-${event.timestamp}`}>
            <div className="log-item-top"><LevelBadge level={event.level} /><span className="log-item-service">{event.service}</span><span className="log-item-host">{event.host}</span><span className="log-item-time">{formatTs(event.timestamp)}</span></div>
            <div className="log-item-msg">{event.message}</div>
            <div className="log-item-meta"><Link className="btn btn-sm" to={`/logs/${event.id}`}>Open event</Link><button className="btn btn-sm" type="button" onClick={() => setSelected(event)}>Inspect</button></div>
          </div>)}
        </div>
      </Card> : !liveStatus.enabled ? <EmptyState><Database size={22} aria-hidden="true" /><strong>No replay source is loaded.</strong><span>Load a dataset before starting the bounded replay.</span></EmptyState> : streamState === 'starting' ? <EmptyState><Spinner label="Connecting to the replay stream…" /></EmptyState> : streamState === 'streaming' ? <EmptyState><Spinner label="Waiting for replayed events…" /></EmptyState> : streamState === 'stopped' ? <EmptyState><Radio size={22} aria-hidden="true" /><strong>Replay stopped.</strong><span>Start the replay again when you are ready.</span></EmptyState> : streamState === 'complete' ? <EmptyState><Radio size={22} aria-hidden="true" /><strong>Replay completed without events.</strong></EmptyState> : streamState === 'error' ? null : <EmptyState><Radio size={22} aria-hidden="true" /><strong>Replay is ready.</strong><span>Start the bounded stream to observe events from the loaded dataset.</span></EmptyState>}

      <EventDrawer event={selected} onClose={() => setSelected(null)} />
    </div>
  );
}
