import { useCallback, useState, type ReactNode } from 'react';
import { Activity, Clock3, Database, ExternalLink, Pause, Play, Radio, RefreshCw, RotateCcw, Wifi } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useReplay } from '../replay/ReplayContext';
import type { LogEvent } from '../api/types';
import { Badge, EmptyState, ErrorBox, EventDrawer, LevelBadge, PageHeader } from '../components/ui';
import { formatNumber, formatTs } from '../components/format';

export default function LivePage() {
  const {
    state: streamState, statusLabel, liveStatus, statusLoading, statusError, error, startEvent,
    currentDataset, emitted, total, progress, recentEvents, isRunning, start, stop, restart, reloadStatus
  } = useReplay();
  const [batchSize, setBatchSize] = useState(50);
  const [intervalMs, setIntervalMs] = useState(700);
  const [selected, setSelected] = useState<LogEvent | null>(null);
  const startReplay = useCallback(() => start({ batchSize, intervalMs }), [batchSize, intervalMs, start]);
  const restartReplay = useCallback(() => restart({ batchSize, intervalMs }), [batchSize, intervalMs, restart]);

  if (statusLoading) return <div className="page experience-page"><div className="experience-loading"><div className="skeleton-line skeleton-line--wide" /><div className="skeleton-hero" /></div></div>;
  if (statusError) return <div className="page experience-page"><ErrorBox error={statusError} retry={reloadStatus} /></div>;
  if (!liveStatus) return <div className="page experience-page"><EmptyState>No replay status is available from the backend.</EmptyState></div>;

  const hasStreamState = Boolean(startEvent || isRunning || streamState === 'complete' || emitted > 0);
  const stateText = isRunning ? 'REPLAYING' : streamState === 'complete' ? 'COMPLETE' : streamState === 'stopped' ? 'STOPPED' : streamState === 'error' ? 'ERROR' : 'READY';

  return (
    <div className="page experience-page live-page">
      <PageHeader eyebrow="OBSERVE / REPLAY" title="Dataset replay" description="Demo replay: a bounded SSE replay of the loaded dataset. This is not a real-time capture feed." actions={<button className="btn btn-sm" type="button" onClick={reloadStatus} disabled={statusLoading}><RefreshCw size={14} aria-hidden="true" /> Refresh status</button>} />

      <section className={`live-hero live-hero--${streamState}`} data-state={streamState}>
        <div className="live-hero-grid" aria-hidden="true"><span /><span /><span /><span /><span /></div>
        <div className="live-hero-copy">
          <div className="live-label"><span className={isRunning ? 'live-indicator live-indicator--on' : 'live-indicator'} /><Badge tone="info" label="Demo replay of the loaded dataset, not live production telemetry">DEMO REPLAY</Badge><span className="live-state-name">{stateText}</span></div>
          <h2>{isRunning ? 'Events are moving through the replay stream.' : streamState === 'complete' ? 'Replay reached the end of this dataset.' : 'Step through the data at your pace.'}</h2>
          <p>{currentDataset ? `${currentDataset} · ${formatNumber(total || liveStatus.total)} events available` : 'Load a source before starting the replay.'}</p>
        </div>
        <div className="live-hero-status" role="status" aria-live="polite">
          <div><Wifi size={16} aria-hidden="true" /><span>Replay channel</span><strong>{statusLabel}</strong></div>
          <div><Database size={16} aria-hidden="true" /><span>Source</span><strong>{startEvent?.source ?? liveStatus.source ?? '—'}</strong></div>
        </div>
      </section>

      <section className="live-metric-strip" aria-label="Replay progress metrics">
        <ReplayMetric icon={<Activity size={16} aria-hidden="true" />} label="Events emitted" value={formatNumber(emitted)} detail="Received by this browser" />
        <ReplayMetric icon={<Database size={16} aria-hidden="true" />} label="Events available" value={formatNumber(total || liveStatus.total)} detail="Bounded source size" />
        <ReplayMetric icon={<Clock3 size={16} aria-hidden="true" />} label="Batch size" value={String(startEvent?.batchSize ?? batchSize)} detail="Events per server batch" />
        <ReplayMetric icon={<Radio size={16} aria-hidden="true" />} label="Replay pace" value={`${startEvent?.paceMs ?? intervalMs} ms`} detail="Configured batch interval" />
      </section>

      {error && <ErrorBox error={error} retry={startReplay} />}

      <section className="live-workspace" aria-label="Replay controls and event stream">
        <div className="live-stream-panel">
          <div className="live-section-heading"><div><span className="eyebrow">STREAM OUTPUT</span><h2>Events</h2></div><span className="live-count">{formatNumber(recentEvents.length)} recent</span></div>
          {recentEvents.length ? <div className="live-event-list" aria-live="polite">{recentEvents.map((event) => <article className="live-event-row" key={`${event.id}-${event.timestamp}`}>
            <time className="live-event-time">{formatTs(event.timestamp)}</time><div className="live-event-severity"><LevelBadge level={event.level} /></div><Link className="live-event-service" to={`/services/${encodeURIComponent(event.service)}`}>{event.service}</Link><div className="live-event-main"><p>{event.message}</p><div>{event.httpMethod && <span>{event.httpMethod} {event.statusCode}</span>}<span>{event.host}</span>{event.requestId && <code>{event.requestId}</code>}</div></div><button className="icon-btn" type="button" onClick={() => setSelected(event)} aria-label={`Inspect event ${event.id}`}><ExternalLink size={14} aria-hidden="true" /></button>
          </article>)}</div> : <div className="live-empty-stream"><div className="live-empty-signal"><Activity size={22} aria-hidden="true" /></div><strong>{isRunning ? 'Waiting for the first batch' : 'The event stream is quiet'}</strong><span>{isRunning ? 'The replay connection is open; events appear when the backend emits a batch.' : liveStatus.enabled ? 'Start a bounded replay from the controls to inspect events as they arrive.' : 'Load a dataset before opening a replay stream.'}</span>{!liveStatus.enabled && <Link className="btn btn-primary" to="/datasets">Choose a source</Link>}</div>}
        </div>

        <aside className="live-control-panel">
          <div className="live-section-heading"><div><span className="eyebrow">CONTROL</span><h2>Replay settings</h2></div><Radio size={17} aria-hidden="true" /></div>
          <div className="replay-source-summary"><span>ACTIVE SOURCE</span><strong>{currentDataset ?? 'No source loaded'}</strong><small>{startEvent?.label ?? liveStatus.label}</small></div>
          <label className="replay-control-field"><span>Batch size</span><select value={batchSize} onChange={(event) => setBatchSize(Number(event.target.value))} disabled={isRunning}><option value={10}>10 events</option><option value={25}>25 events</option><option value={50}>50 events</option><option value={100}>100 events</option><option value={200}>200 events</option></select></label>
          <label className="replay-control-field"><span>Interval between batches</span><select value={intervalMs} onChange={(event) => setIntervalMs(Number(event.target.value))} disabled={isRunning}><option value={100}>100 ms</option><option value={300}>300 ms</option><option value={700}>700 ms</option><option value={1500}>1,500 ms</option></select></label>
          <div className="replay-control-actions">{!isRunning ? <button className="btn btn-primary" type="button" onClick={streamState === 'complete' ? restartReplay : startReplay} disabled={!liveStatus.enabled}><Play size={14} aria-hidden="true" />{streamState === 'complete' ? 'Replay again' : 'Start replay'}</button> : <button className="btn btn-danger" type="button" onClick={stop}><Pause size={14} aria-hidden="true" /> Stop replay</button>}{streamState === 'stopped' && <button className="btn" type="button" onClick={restartReplay}><RotateCcw size={14} aria-hidden="true" /> Restart</button>}</div>
          {hasStreamState && <div className="replay-progress-block"><div className="replay-progress-heading"><span>{streamState === 'complete' ? 'Replay complete' : isRunning ? 'Replay in progress' : 'Replay progress'}</span><strong>{progress.toFixed(0)}%</strong></div><div className="progress-track" role="progressbar" aria-label="Replay progress" aria-valuenow={emitted} aria-valuemin={0} aria-valuemax={Math.max(total, 1)}><div className="progress-fill" style={{ width: `${progress}%` }} /></div><div className="progress-meta"><span>{formatNumber(emitted)} emitted</span><span>{formatNumber(Math.max(0, total - emitted))} remaining</span></div></div>}
          <p className="live-honesty-note">This stream replays records already in the selected dataset. It does not generate new telemetry or represent a production connection.</p>
        </aside>
      </section>
      <EventDrawer event={selected} onClose={() => setSelected(null)} />
    </div>
  );
}

function ReplayMetric({ icon, label, value, detail }: { icon: ReactNode; label: string; value: string; detail: string }) {
  return <div className="live-metric"><span className="live-metric-icon">{icon}</span><span className="live-metric-label">{label}</span><strong>{value}</strong><small>{detail}</small></div>;
}
