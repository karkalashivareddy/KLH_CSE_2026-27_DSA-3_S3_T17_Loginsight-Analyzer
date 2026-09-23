import type {
  ApiError,
  AlgorithmInfo,
  AlgorithmResult,
  BenchmarkRequest,
  BenchmarkRow,
  DatasetName,
  DatasetStats,
  DatasetSummary,
  ErrorAnalytics,
  Health,
  LogEvent,
  ModuleInfo,
  ObjectApiResponse,
  RunCompleteEvent,
  RunRecord,
  RunSummary,
  SystemStatus,
  TextHackResponse,
  TimeBucket,
  TopBucket,
  TopDimension,
  TraceCatalogEntry,
  TraceResponse
} from './types';

/**
 * Central API client. Every method maps to exactly one backend endpoint; the response payload is
 * parsed and returned typed, and failures carry the documented {@link ApiError} shape surfaced by
 * GlobalExceptionHandler.
 */

const BASE = '/api';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init
  });

  if (!response.ok) {
    let body: ApiError | null = null;
    try {
      const parsed: unknown = await response.json();
      if (parsed && typeof parsed === 'object' && 'status' in (parsed as Record<string, unknown>)) {
        body = parsed as ApiError;
      }
    } catch {
      // non-JSON error body; fall back to HTTP status text below
    }
    const error = new Error(body?.message ?? `Request failed (${response.status})`);
    (error as Error & { apiError?: ApiError }).apiError =
      body ??
      ({
        status: response.status,
        error: response.statusText,
        message: `Request failed (${response.status})`,
        timestamp: new Date().toISOString(),
        path
      } satisfies ApiError);
    throw error;
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function post<T>(path: string, payload?: unknown): Promise<T> {
  return request<T>(path, { method: 'POST', body: payload === undefined ? undefined : JSON.stringify(payload) });
}

class Api {
  health = (): Promise<Health> => request<Health>('/health');
  systemStatus = (): Promise<SystemStatus> => request<SystemStatus>('/health/status');
  datasetProbe = (): Promise<DatasetSummary> => request<DatasetSummary>('/health/dataset');

  datasets = (): Promise<DatasetName[]> => request<DatasetName[]>('/datasets');
  loadDataset = (name: string): Promise<DatasetSummary> => post<DatasetSummary>(`/datasets/${encodeURIComponent(name)}`);
  clearDataset = (): Promise<DatasetSummary> => request<DatasetSummary>('/datasets', { method: 'DELETE' });
  currentDataset = (): Promise<DatasetSummary> => request<DatasetSummary>('/datasets/current');

  logs = (limit = 100, offset = 0): Promise<LogEvent[]> =>
    request<LogEvent[]>(`/logs?limit=${limit}&offset=${offset}`);
  stats = (): Promise<DatasetStats> => request<DatasetStats>('/logs/stats');

  dependencies = (): Promise<ObjectApiResponse> => request<ObjectApiResponse>('/analytics/dependencies');
  errorAnalytics = (limit = 10): Promise<ErrorAnalytics> => request<ErrorAnalytics>(`/analytics/errors?limit=${limit}`);
  top = (dimension: TopDimension, limit = 6): Promise<TopBucket[]> =>
    request<TopBucket[]>(`/analytics/top?dimension=${dimension}&limit=${limit}`);
  windows = (buckets = 12): Promise<TimeBucket[]> => request<TimeBucket[]>(`/analytics/windows?buckets=${buckets}`);

  traceCatalog = (): Promise<TraceCatalogEntry[]> => request<TraceCatalogEntry[]>('/trace/catalog');
  traceRun = (endpoint: string, input: unknown): Promise<TraceResponse> =>
    post<TraceResponse>(endpoint.replace(/^\/api/, ''), input);

  /** Generic POST against an arbitrary backend path (used for canonical lab execution). */
  execute = (endpoint: string, payload: unknown): Promise<unknown> =>
    post<unknown>(endpoint.replace(/^\/api/, ''), payload);

  search = (path: '/search/naive' | '/search/kmp' | '/search/z' | '/search/rabin-karp', payload: unknown): Promise<AlgorithmResult> =>
    post<AlgorithmResult>(path, payload);

  benchmark = (payload: BenchmarkRequest): Promise<BenchmarkRow[]> => post<BenchmarkRow[]>('/benchmark/run', payload);

  /* ── Phase 2-4 surface: catalog, TextHack, run & replay ─────────────────────────── */

  modules = (): Promise<ModuleInfo[]> => request<ModuleInfo[]>('/modules');
  module = (id: string): Promise<ModuleInfo | null> =>
    request<ModuleInfo>(`/modules/${encodeURIComponent(id)}`).catch(() => null);
  algorithms = (): Promise<AlgorithmInfo[]> => request<AlgorithmInfo[]>('/algorithms');
  algorithm = (key: string): Promise<AlgorithmInfo | null> =>
    request<AlgorithmInfo>(`/algorithms/${encodeURIComponent(key)}`).catch(() => null);

  textHack = (queryClass: string, input: Record<string, unknown>): Promise<TextHackResponse> =>
    post<TextHackResponse>('/text-hack/query', { queryClass, input });

  run = (algorithm: string, input: Record<string, unknown>): Promise<RunRecord> =>
    post<RunRecord>('/runs', { algorithm, input });
  runs = (): Promise<RunSummary[]> => request<RunSummary[]>('/runs');
  runGet = (id: string): Promise<RunRecord> => request<RunRecord>(`/runs/${encodeURIComponent(id)}`);
  runResult = (id: string): Promise<unknown> => request<unknown>(`/runs/${encodeURIComponent(id)}/result`);

  /**
   * Subscribe to the SSE replay stream for a recorded run. Returns an unsubscribe function.
   * Events: `meta` (RunMetaEvent), `step` (TraceStep), `complete` (RunCompleteEvent).
   */
  runsStream(
    id: string,
    handlers: {
      onMeta?: (meta: import('./types').RunMetaEvent) => void;
      onStep?: (step: import('./types').TraceStep, index: number) => void;
      onComplete?: (complete: RunCompleteEvent) => void;
      onError?: (error: Error) => void;
      onClose?: () => void;
    }
  ): () => void {
    const controller = new AbortController();
    void (async () => {
      try {
        const response = await fetch(`${BASE}/runs/${encodeURIComponent(id)}/events`, {
          headers: { Accept: 'text/event-stream' },
          signal: controller.signal
        });
        if (!response.ok || !response.body) {
          throw new Error(`Replay stream failed (${response.status})`);
        }
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let stepIndex = 0;
        for (;;) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          let sep = buffer.indexOf('\n\n');
          while (sep !== -1) {
            const raw = buffer.slice(0, sep);
            buffer = buffer.slice(sep + 2);
            let event = 'message';
            const data: string[] = [];
            for (const line of raw.split('\n')) {
              if (line.startsWith('event:')) event = line.slice(6).trim();
              else if (line.startsWith('data:')) data.push(line.slice(5).trimStart());
              else if (line === '') continue;
            }
            const payload = data.join('\n');
            if (!payload) {
              sep = buffer.indexOf('\n\n');
              continue;
            }
            let parsed: unknown;
            try {
              parsed = JSON.parse(payload);
            } catch {
              parsed = null;
            }
            if (event === 'meta' && parsed && handlers.onMeta) {
              handlers.onMeta(parsed as import('./types').RunMetaEvent);
            } else if (event === 'step' && parsed && handlers.onStep) {
              handlers.onStep(parsed as import('./types').TraceStep, stepIndex++);
            } else if (event === 'complete' && parsed && handlers.onComplete) {
              handlers.onComplete(parsed as RunCompleteEvent);
            }
            sep = buffer.indexOf('\n\n');
          }
        }
        handlers.onClose?.();
      } catch (e) {
        if (!controller.signal.aborted) {
          handlers.onError?.(e instanceof Error ? e : new Error(String(e)));
        }
        handlers.onClose?.();
      }
    })();
    return () => controller.abort();
  }
}

export const api = new Api();

/** Endpoint paths the search family supports. */
export const SEARCH_PATHS = ['/search/naive', '/search/kmp', '/search/z', '/search/rabin-karp'] as const;
export type SearchPath = (typeof SEARCH_PATHS)[number];