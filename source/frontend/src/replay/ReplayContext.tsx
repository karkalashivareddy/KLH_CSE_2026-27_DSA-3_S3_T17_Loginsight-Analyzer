import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { api, subscribeDatasetInvalidation } from '../api/client';
import type { LiveBatch, LiveReplayComplete, LiveStartEvent, LiveStatus, LogEvent } from '../api/types';

export type ReplayState = 'idle' | 'starting' | 'streaming' | 'complete' | 'stopped' | 'error';

export interface ReplayOptions {
  batchSize?: number;
  intervalMs?: number;
}

export interface ReplayContextValue {
  state: ReplayState;
  status: ReplayState;
  statusLabel: string;
  liveStatus: LiveStatus | null;
  statusLoading: boolean;
  statusError: Error | null;
  error: Error | null;
  startEvent: LiveStartEvent | null;
  currentDataset: string | null;
  emitted: number;
  total: number;
  progress: number;
  recentEvents: LogEvent[];
  batchSize: number;
  intervalMs: number;
  isRunning: boolean;
  start: (options?: ReplayOptions) => void;
  stop: () => void;
  restart: (options?: ReplayOptions) => void;
  reloadStatus: () => void;
  configure: (options: ReplayOptions) => void;
}

const MAX_EVENTS = 300;
const DEFAULT_OPTIONS: Required<ReplayOptions> = { batchSize: 50, intervalMs: 700 };
const ReplayContext = createContext<ReplayContextValue | null>(null);
const emptyReplayContext: ReplayContextValue = {
  state: 'idle',
  status: 'idle',
  statusLabel: 'Ready',
  liveStatus: null,
  statusLoading: false,
  statusError: null,
  error: null,
  startEvent: null,
  currentDataset: null,
  emitted: 0,
  total: 0,
  progress: 0,
  recentEvents: [],
  batchSize: DEFAULT_OPTIONS.batchSize,
  intervalMs: DEFAULT_OPTIONS.intervalMs,
  isRunning: false,
  start: () => undefined,
  stop: () => undefined,
  restart: () => undefined,
  reloadStatus: () => undefined,
  configure: () => undefined
};

function stateLabel(state: ReplayState): string {
  if (state === 'starting') return 'Connecting';
  if (state === 'streaming') return 'Streaming';
  if (state === 'complete') return 'Complete';
  if (state === 'stopped') return 'Stopped';
  if (state === 'error') return 'Error';
  return 'Ready';
}

function asError(value: unknown): Error {
  return value instanceof Error ? value : new Error(String(value));
}

export function ReplayProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<ReplayState>('idle');
  const [liveStatus, setLiveStatus] = useState<LiveStatus | null>(null);
  const [statusLoading, setStatusLoading] = useState(true);
  const [statusError, setStatusError] = useState<Error | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [startEvent, setStartEvent] = useState<LiveStartEvent | null>(null);
  const [emitted, setEmitted] = useState(0);
  const [total, setTotal] = useState(0);
  const [recentEvents, setRecentEvents] = useState<LogEvent[]>([]);
  const [batchSize, setBatchSize] = useState(DEFAULT_OPTIONS.batchSize);
  const [intervalMs, setIntervalMs] = useState(DEFAULT_OPTIONS.intervalMs);
  const unsubscribeRef = useRef<(() => void) | null>(null);
  const generationRef = useRef(0);
  const optionsRef = useRef<Required<ReplayOptions>>({ ...DEFAULT_OPTIONS });
  const statusControllerRef = useRef<AbortController | null>(null);

  const clearStream = useCallback(() => {
    generationRef.current += 1;
    const unsubscribe = unsubscribeRef.current;
    unsubscribeRef.current = null;
    unsubscribe?.();
  }, []);

  const resetStream = useCallback(() => {
    clearStream();
    setState('idle');
    setError(null);
    setStartEvent(null);
    setEmitted(0);
    setTotal(liveStatus?.total ?? 0);
    setRecentEvents([]);
  }, [clearStream, liveStatus?.total]);

  const stop = useCallback(() => {
    clearStream();
    setState((current) => current === 'starting' || current === 'streaming' ? 'stopped' : current);
  }, [clearStream]);

  const configure = useCallback((next: ReplayOptions) => {
    const resolved = {
      batchSize: next.batchSize ?? optionsRef.current.batchSize,
      intervalMs: next.intervalMs ?? optionsRef.current.intervalMs
    };
    optionsRef.current = resolved;
    setBatchSize(resolved.batchSize);
    setIntervalMs(resolved.intervalMs);
  }, []);

  const start = useCallback((next: ReplayOptions = {}) => {
    if (liveStatus?.enabled === false) {
      setError(new Error('Replay is unavailable until a dataset is loaded.'));
      setState('error');
      return;
    }
    const resolved = {
      batchSize: next.batchSize ?? optionsRef.current.batchSize,
      intervalMs: next.intervalMs ?? optionsRef.current.intervalMs
    };
    optionsRef.current = resolved;
    setBatchSize(resolved.batchSize);
    setIntervalMs(resolved.intervalMs);
    clearStream();
    const generation = generationRef.current;
    let completed = false;
    let failed = false;
    const isCurrent = () => generation === generationRef.current;
    setError(null);
    setStartEvent(null);
    setRecentEvents([]);
    setEmitted(0);
    setTotal(liveStatus?.total ?? 0);
    setState('starting');
    unsubscribeRef.current = api.liveStream({
      onStart: (nextStart: LiveStartEvent) => {
        if (!isCurrent()) return;
        setStartEvent(nextStart);
        setTotal(nextStart.total);
        setState('streaming');
      },
      onBatch: (batch: LiveBatch) => {
        if (!isCurrent()) return;
        setEmitted(batch.emittedCount);
        setTotal(batch.total);
        setRecentEvents((previous) => [...batch.events].reverse().concat(previous).slice(0, MAX_EVENTS));
        setState('streaming');
      },
      onComplete: (complete: LiveReplayComplete) => {
        if (!isCurrent()) return;
        completed = true;
        setEmitted(complete.emitted);
        setTotal(complete.total);
        setState('complete');
      },
      onError: (streamError: Error) => {
        if (!isCurrent()) return;
        failed = true;
        setError(asError(streamError));
        setState('error');
      },
      onClose: () => {
        if (!isCurrent() || completed || failed) return;
        setError(new Error('The replay stream closed before the server reported completion.'));
        setState('error');
      }
    }, resolved);
  }, [clearStream, liveStatus?.enabled, liveStatus?.total]);

  const restart = useCallback((next: ReplayOptions = {}) => start(next), [start]);

  const reloadStatus = useCallback(() => {
    statusControllerRef.current?.abort();
    const controller = new AbortController();
    statusControllerRef.current = controller;
    setStatusLoading(true);
    setStatusError(null);
    void api.liveStatus({ signal: controller.signal })
      .then((next) => {
        if (controller.signal.aborted) return;
        setLiveStatus(next);
        setStatusError(null);
      })
      .catch((reason: unknown) => {
        if (controller.signal.aborted) return;
        setStatusError(asError(reason));
      })
      .finally(() => {
        if (controller.signal.aborted) return;
        setStatusLoading(false);
      });
  }, []);

  useEffect(() => {
    reloadStatus();
    return () => {
      statusControllerRef.current?.abort();
      clearStream();
    };
  }, [clearStream, reloadStatus]);

  useEffect(() => subscribeDatasetInvalidation(() => {
    resetStream();
    reloadStatus();
  }), [reloadStatus, resetStream]);

  const progress = total > 0 ? Math.min(100, Math.max(0, (emitted / total) * 100)) : state === 'complete' ? 100 : 0;
  const currentDataset = startEvent?.dataset ?? liveStatus?.dataset ?? null;
  const isRunning = state === 'starting' || state === 'streaming';
  const value = useMemo<ReplayContextValue>(() => ({
    state,
    status: state,
    statusLabel: stateLabel(state),
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
    batchSize,
    intervalMs,
    isRunning,
    start,
    stop,
    restart,
    reloadStatus,
    configure
  }), [batchSize, currentDataset, emitted, error, intervalMs, isRunning, liveStatus, progress, recentEvents, reloadStatus, restart, start, startEvent, state, statusError, statusLoading, stop, total, configure]);

  return <ReplayContext.Provider value={value}>{children}</ReplayContext.Provider>;
}

export function useReplay(): ReplayContextValue {
  return useContext(ReplayContext) ?? emptyReplayContext;
}
