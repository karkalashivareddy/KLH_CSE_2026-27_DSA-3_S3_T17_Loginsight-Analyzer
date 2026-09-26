import type {
  AlgorithmGroup,
  ApiError,
  AlgorithmInfo,
  AlgorithmResult,
  BenchmarkRequest,
  BenchmarkRow,
  DatasetName,
  DatasetResult,
  DatasetStats,
  DatasetSummary,
  ErrorAnalytics,
  Health,
  Heatmap,
  HostRow,
  HttpStatsDto,
  IngestionStatus,
  IncidentDetail,
  IncidentDto,
  LiveBatch,
  LiveReplayComplete,
  LiveStartEvent,
  LiveStatus,
  LogEvent,
  LogSearchRequest,
  LogSearchResponse,
  ModuleInfo,
  ObjectApiResponse,
  OverviewDto,
  PatternDto,
  RunCompleteEvent,
  RunRecord,
  RunSummary,
  SearchBenchmarkResponse,
  ServiceDetail,
  ServiceStatsDto,
  SuggestionDto,
  SystemStatus,
  TextHackResponse,
  TimeBucket,
  TopBucket,
  TopDimension,
  TraceCatalogEntry,
  TraceResponse,
  RunMetaEvent,
  TraceStep
} from './types';

export interface RequestOptions {
  signal?: AbortSignal;
  timeoutMs?: number;
}

export interface SseEvent {
  event: string;
  data: string;
  id?: string;
}

function isSsePayload(value: unknown): boolean {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

const BASE = '/api';
const DEFAULT_TIMEOUT_MS = 15_000;
const DATASET_INVALIDATION_EVENT = 'loginsight:dataset-invalidated';

type ApiErrorCarrier = Error & { apiError?: ApiError };

function isFormData(value: BodyInit | null | undefined): value is FormData {
  if (typeof FormData === 'undefined' || value === null || value === undefined) return false;
  return value instanceof FormData || Object.prototype.toString.call(value) === '[object FormData]';
}

function bounded(value: number | undefined, min: number, max: number, fallback: number): number {
  if (value === undefined || !Number.isFinite(value)) return fallback;
  return Math.max(min, Math.min(max, Math.round(value)));
}

function pathFor(path: string): string {
  const clean = path.startsWith('/api') ? path.slice(3) || '/' : path;
  return `${BASE}${clean.startsWith('/') ? clean : `/${clean}`}`;
}

function errorStatus(error: unknown): number | undefined {
  if (!error || typeof error !== 'object') return undefined;
  const value = (error as ApiErrorCarrier).apiError?.status;
  return typeof value === 'number' ? value : undefined;
}

function abortError(): Error {
  const error = new Error('Request cancelled');
  error.name = 'AbortError';
  return error;
}

function parseApiErrorBody(value: unknown, response: Response, path: string): ApiError {
  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>;
    const message = typeof record.message === 'string'
      ? record.message
      : typeof record.error === 'string'
        ? record.error
        : `Request failed (${response.status})`;
    return {
      status: typeof record.status === 'number' ? record.status : response.status,
      error: typeof record.error === 'string' ? record.error : response.statusText || 'Request failed',
      message,
      timestamp: typeof record.timestamp === 'string' ? record.timestamp : new Date().toISOString(),
      path: typeof record.path === 'string' ? record.path : path
    };
  }
  if (typeof value === 'string' && value.trim()) {
    return {
      status: response.status,
      error: response.statusText || 'Request failed',
      message: value,
      timestamp: new Date().toISOString(),
      path
    };
  }
  return {
    status: response.status,
    error: response.statusText || 'Request failed',
    message: `Request failed (${response.status})`,
    timestamp: new Date().toISOString(),
    path
  };
}

function makeApiError(body: unknown, response: Response, path: string): ApiErrorCarrier {
  const apiError = parseApiErrorBody(body, response, path);
  const error = new Error(apiError.message) as ApiErrorCarrier;
  error.apiError = apiError;
  return error;
}

async function responseBody(response: Response): Promise<unknown> {
  if (typeof response.text === 'function') {
    const text = await response.text();
    if (typeof text !== 'string') {
      const legacyResponse = response as Response & { json?: () => Promise<unknown> };
      return legacyResponse.json ? legacyResponse.json() : undefined;
    }
    if (!text.trim()) return undefined;
    try {
      return JSON.parse(text) as unknown;
    } catch {
      return text;
    }
  }
  const legacyResponse = response as Response & { json?: () => Promise<unknown> };
  return legacyResponse.json ? legacyResponse.json() : undefined;
}

function requestTimeout(value: number | undefined): number {
  if (value === undefined || !Number.isFinite(value) || value <= 0) return DEFAULT_TIMEOUT_MS;
  return Math.max(1, Math.ceil(value));
}

async function request<T>(path: string, init: RequestInit = {}, options: RequestOptions = {}): Promise<T> {
  const url = pathFor(path);
  const headers = new Headers(init.headers);
  if (isFormData(init.body)) headers.delete('Content-Type');
  else if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const controller = new AbortController();
  const upstreamSignals = [options.signal, init.signal].filter((signal): signal is AbortSignal => signal !== null && signal !== undefined);
  let timedOut = false;
  const abortFromUpstream = () => controller.abort();
  if (upstreamSignals.some((signal) => signal.aborted)) controller.abort();
  else upstreamSignals.forEach((signal) => signal.addEventListener('abort', abortFromUpstream, { once: true }));
  const timeoutMs = requestTimeout(options.timeoutMs);
  let timeout: ReturnType<typeof setTimeout> | undefined;
  const timeoutPromise = new Promise<never>((_, reject) => {
    timeout = setTimeout(() => {
      timedOut = true;
      controller.abort();
      reject(new Error('timeout'));
    }, timeoutMs);
  });

  try {
    const response = await Promise.race([
      fetch(url, {
        ...init,
        headers,
        signal: controller.signal,
        cache: 'no-store'
      }),
      timeoutPromise
    ]);
    if (response.status === 204 || response.status === 205) return undefined as T;
    const body = await Promise.race([responseBody(response), timeoutPromise]);
    if (upstreamSignals.some((signal) => signal.aborted)) throw abortError();
    if (!response.ok) throw makeApiError(body, response, path);
    if (body === undefined) return undefined as T;
    return body as T;
  } catch (error) {
    if (timedOut) {
      const timeoutError = new Error(`Request timed out after ${timeoutMs} ms`) as ApiErrorCarrier;
      timeoutError.apiError = {
        status: 408,
        error: 'Request timeout',
        message: timeoutError.message,
        timestamp: new Date().toISOString(),
        path
      };
      throw timeoutError;
    }
    if (upstreamSignals.some((signal) => signal.aborted) || (error instanceof Error && error.name === 'AbortError')) throw abortError();
    throw error;
  } finally {
    if (timeout !== undefined) clearTimeout(timeout);
    upstreamSignals.forEach((signal) => signal.removeEventListener('abort', abortFromUpstream));
  }
}

async function optional<T>(path: string, init?: RequestInit, options?: RequestOptions): Promise<T | null> {
  try {
    return await request<T>(path, init, options);
  } catch (error) {
    if (errorStatus(error) === 404) return null;
    throw error;
  }
}

async function post<T>(path: string, payload?: unknown, options?: RequestOptions): Promise<T> {
  const init: RequestInit = { method: 'POST' };
  if (payload !== undefined) init.body = JSON.stringify(payload);
  return request<T>(path, init, options);
}

function announceDatasetMutation(): void {
  if (typeof window !== 'undefined') window.dispatchEvent(new Event(DATASET_INVALIDATION_EVENT));
}

export function subscribeDatasetInvalidation(listener: () => void): () => void {
  if (typeof window === 'undefined') return () => undefined;
  window.addEventListener(DATASET_INVALIDATION_EVENT, listener);
  return () => window.removeEventListener(DATASET_INVALIDATION_EVENT, listener);
}

export class SseParser {
  private buffer = '';
  private eventName = 'message';
  private data: string[] = [];
  private lastId: string | undefined;
  private firstLine = true;

  push(chunk: string): SseEvent[] {
    this.buffer += chunk;
    return this.drain(false);
  }

  flush(): SseEvent[] {
    return this.drain(true);
  }

  private drain(final: boolean): SseEvent[] {
    const events: SseEvent[] = [];
    for (;;) {
      const match = /\r\n|\n|\r/.exec(this.buffer);
      if (!match || (match[0] === '\r' && match.index === this.buffer.length - 1 && !final)) break;
      const line = this.buffer.slice(0, match.index);
      const delimiterLength = match[0] === '\r\n' ? 2 : 1;
      this.buffer = this.buffer.slice(match.index + delimiterLength);
      this.consumeLine(line, events);
    }
    if (final && this.buffer.length > 0) {
      const line = this.buffer;
      this.buffer = '';
      this.consumeLine(line, events);
    }
    if (final) {
      const event = this.dispatch();
      if (event) events.push(event);
    }
    return events;
  }

  private consumeLine(line: string, events: SseEvent[]): void {
    const normalized = this.firstLine ? line.replace(/^\uFEFF/, '') : line;
    this.firstLine = false;
    if (normalized === '') {
      const event = this.dispatch();
      if (event) events.push(event);
      return;
    }
    if (normalized.startsWith(':')) return;
    const separator = normalized.indexOf(':');
    const field = separator === -1 ? normalized : normalized.slice(0, separator);
    let value = separator === -1 ? '' : normalized.slice(separator + 1);
    if (value.startsWith(' ')) value = value.slice(1);
    if (field === 'data') this.data.push(value);
    else if (field === 'event') this.eventName = value || 'message';
    else if (field === 'id' && !value.includes('\u0000')) this.lastId = value;
  }

  private dispatch(): SseEvent | null {
    if (this.data.length === 0) {
      this.eventName = 'message';
      return null;
    }
    const event: SseEvent = {
      event: this.eventName,
      data: this.data.join('\n'),
      ...(this.lastId === undefined ? {} : { id: this.lastId })
    };
    this.data = [];
    this.eventName = 'message';
    return event;
  }
}

async function consumeSse(response: Response, onEvent: (event: SseEvent) => void): Promise<void> {
  if (!response.body) throw new Error('The server returned an empty event stream');
  const reader = response.body.getReader();
  let ended = false;
  try {
    const decoder = new TextDecoder();
    const parser = new SseParser();
    const dispatch = (events: SseEvent[]) => events.forEach(onEvent);
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      dispatch(parser.push(decoder.decode(value, { stream: true })));
    }
    dispatch(parser.push(decoder.decode()));
    dispatch(parser.flush());
    ended = true;
  } finally {
    if (!ended) {
      try {
        await reader.cancel();
      } catch {
      }
    }
    try {
      reader.releaseLock();
    } catch {
    }
  }
}

async function streamError(response: Response, path: string): Promise<ApiErrorCarrier> {
  const body = await responseBody(response);
  return makeApiError(body, response, path);
}

type StreamCallbacks = {
  onError?: (error: Error) => void;
  onClose?: () => void;
};

function finishStream(controller: AbortController, callbacks: StreamCallbacks, state: { closed: boolean }, error?: unknown): void {
  if (state.closed) return;
  state.closed = true;
  if (error !== undefined && !controller.signal.aborted) {
    try {
      callbacks.onError?.(error instanceof Error ? error : new Error(String(error)));
    } catch {
    }
  }
  try {
    callbacks.onClose?.();
  } catch {
  }
}

class Api {
  health = (options?: RequestOptions): Promise<Health> => request<Health>('/health', {}, options);
  systemStatus = (options?: RequestOptions): Promise<SystemStatus> => request<SystemStatus>('/health/status', {}, options);
  datasetProbe = (options?: RequestOptions): Promise<DatasetSummary> => request<DatasetSummary>('/health/dataset', {}, options);

  datasets = (options?: RequestOptions): Promise<DatasetName[]> => request<DatasetName[]>('/datasets', {}, options);

  loadDataset = async (name: string, options?: RequestOptions): Promise<DatasetSummary> => {
    const result = await post<DatasetSummary>(`/datasets/${encodeURIComponent(name)}`, undefined, options);
    announceDatasetMutation();
    return result;
  };

  clearDataset = async (options?: RequestOptions): Promise<DatasetSummary> => {
    const result = await request<DatasetSummary>('/datasets', { method: 'DELETE' }, options);
    announceDatasetMutation();
    return result;
  };

  currentDataset = (options?: RequestOptions): Promise<DatasetSummary | null> => optional<DatasetSummary>('/datasets/current', undefined, options);

  logs = (limit = 100, offset = 0, options?: RequestOptions): Promise<LogEvent[]> =>
    request<LogEvent[]>(`/logs?limit=${bounded(limit, 1, 1_000, 100)}&offset=${bounded(offset, 0, 1_000_000, 0)}`, {}, options);
  stats = (options?: RequestOptions): Promise<DatasetStats> => request<DatasetStats>('/logs/stats', {}, options);

  dependencies = (options?: RequestOptions): Promise<ObjectApiResponse> => request<ObjectApiResponse>('/analytics/dependencies', {}, options);
  errorAnalytics = (limit = 10, options?: RequestOptions): Promise<ErrorAnalytics> => request<ErrorAnalytics>(`/analytics/errors?limit=${bounded(limit, 1, 100, 10)}`, {}, options);
  top = (dimension: TopDimension, limit = 6, options?: RequestOptions): Promise<TopBucket[]> =>
    request<TopBucket[]>(`/analytics/top?dimension=${dimension}&limit=${bounded(limit, 1, 50, 6)}`, {}, options);
  windows = (buckets = 12, options?: RequestOptions): Promise<TimeBucket[]> =>
    request<TimeBucket[]>(`/analytics/windows?buckets=${bounded(buckets, 1, 100, 12)}`, {}, options);

  traceCatalog = (options?: RequestOptions): Promise<TraceCatalogEntry[]> => request<TraceCatalogEntry[]>('/trace/catalog', {}, options);
  traceRun = (endpoint: string, input: unknown, options?: RequestOptions): Promise<TraceResponse> =>
    post<TraceResponse>(endpoint.replace(/^\/api/, ''), input, options);

  execute = (endpoint: string, payload: unknown, options?: RequestOptions): Promise<unknown> =>
    post<unknown>(endpoint.replace(/^\/api/, ''), payload, options);

  search = (path: '/search/naive' | '/search/kmp' | '/search/z' | '/search/rabin-karp', payload: unknown, options?: RequestOptions): Promise<AlgorithmResult> =>
    post<AlgorithmResult>(path, payload, options);

  benchmark = (payload: BenchmarkRequest, options?: RequestOptions): Promise<BenchmarkRow[]> => post<BenchmarkRow[]>('/benchmark/run', payload, options);

  overview = (range = '1h', options?: RequestOptions): Promise<OverviewDto | null> =>
    optional<OverviewDto>(`/overview?range=${encodeURIComponent(range)}`, undefined, options);

  explore = (params: LogSearchRequest, options?: RequestOptions): Promise<LogSearchResponse> => {
    const qs = new URLSearchParams();
    if (params.query) qs.set('q', params.query);
    if (params.from) qs.set('from', params.from);
    if (params.to) qs.set('to', params.to);
    qs.set('page', String(bounded(params.page, 1, 100_000, 1)));
    qs.set('size', String(bounded(params.size, 1, 200, 25)));
    qs.set('sort', params.sort || 'timestamp:desc');
    return request<LogSearchResponse>(`/logs/explore?${qs.toString()}`, {}, options);
  };

  logById = (id: number, options?: RequestOptions): Promise<LogEvent | null> =>
    optional<LogEvent>(`/logs/${encodeURIComponent(String(id))}`, undefined, options);

  productSearch = (payload: LogSearchRequest, options?: RequestOptions): Promise<LogSearchResponse> =>
    post<LogSearchResponse>('/search', payload, options);

  suggest = (q: string, limit = 10, options?: RequestOptions): Promise<SuggestionDto[]> =>
    request<SuggestionDto[]>(`/search/suggest?q=${encodeURIComponent(q)}&limit=${bounded(limit, 1, 25, 10)}`, {}, options);

  patterns = (level = 'all', limit = 50, options?: RequestOptions): Promise<PatternDto[]> =>
    request<PatternDto[]>(`/patterns?level=${encodeURIComponent(level)}&limit=${bounded(limit, 1, 200, 50)}`, {}, options);

  patternExamples = (template: string, limit = 50, options?: RequestOptions): Promise<LogEvent[]> =>
    request<LogEvent[]>(`/patterns/examples?template=${encodeURIComponent(template)}&limit=${bounded(limit, 1, 200, 50)}`, {}, options);

  incidents = (limit = 20, options?: RequestOptions): Promise<IncidentDto[]> =>
    request<IncidentDto[]>(`/incidents?limit=${bounded(limit, 1, 100, 20)}`, {}, options);
  incidentCount = (options?: RequestOptions): Promise<number> => request<number>('/incidents/count', {}, options);
  incidentDetail = (id: number, logs = 100, options?: RequestOptions): Promise<IncidentDetail> =>
    request<IncidentDetail>(`/incidents/${encodeURIComponent(String(id))}?logs=${bounded(logs, 1, 500, 100)}`, {}, options);
  incidentLogs = (id: number, limit = 100, offset = 0, options?: RequestOptions): Promise<LogEvent[]> =>
    request<LogEvent[]>(`/incidents/${encodeURIComponent(String(id))}/logs?limit=${bounded(limit, 1, 500, 100)}&offset=${bounded(offset, 0, 100_000, 0)}`, {}, options);

  services = (limit = 50, options?: RequestOptions): Promise<ServiceStatsDto[]> =>
    request<ServiceStatsDto[]>(`/services?limit=${bounded(limit, 1, 200, 50)}`, {}, options);
  serviceDetail = (name: string, recent = 50, options?: RequestOptions): Promise<ServiceDetail> =>
    request<ServiceDetail>(`/services/${encodeURIComponent(name)}?recent=${bounded(recent, 1, 500, 50)}`, {}, options);

  httpAnalytics = (options?: RequestOptions): Promise<HttpStatsDto> => request<HttpStatsDto>('/analytics/http', {}, options);
  hostAnalytics = (limit = 25, options?: RequestOptions): Promise<HostRow[]> =>
    request<HostRow[]>(`/analytics/hosts?limit=${bounded(limit, 1, 200, 25)}`, {}, options);
  heatmap = (options?: RequestOptions): Promise<Heatmap> => request<Heatmap>('/analytics/heatmap', {}, options);

  liveStatus = (options?: RequestOptions): Promise<LiveStatus> => request<LiveStatus>('/live/status', {}, options);

  loadDemo = async (options?: RequestOptions): Promise<DatasetResult> => {
    const result = await post<DatasetResult>('/datasets/demo', undefined, options);
    announceDatasetMutation();
    return result;
  };
  ingestionDemo = async (options?: RequestOptions): Promise<DatasetResult> => {
    const result = await post<DatasetResult>('/ingestion/demo', undefined, options);
    announceDatasetMutation();
    return result;
  };
  ingestionStatus = (options?: RequestOptions): Promise<IngestionStatus> => request<IngestionStatus>('/ingestion/status', {}, options);

  uploadDataset = async (file: File, name?: string, options?: RequestOptions): Promise<DatasetResult> => {
    const form = new FormData();
    form.append('file', file);
    if (name) form.append('name', name);
    const result = await request<DatasetResult>('/datasets', { method: 'POST', body: form }, options);
    announceDatasetMutation();
    return result;
  };

  algorithmGroups = (options?: RequestOptions): Promise<AlgorithmGroup[]> => request<AlgorithmGroup[]>('/analysis/algorithms', {}, options);
  searchBenchmark = (pattern: string, options?: RequestOptions): Promise<SearchBenchmarkResponse> =>
    request<SearchBenchmarkResponse>(`/analysis/benchmarks/search?pattern=${encodeURIComponent(pattern)}`, {}, options);

  liveStream(
    handlers: {
      onStart?: (start: LiveStartEvent) => void;
      onBatch?: (batch: LiveBatch) => void;
      onComplete?: (complete: LiveReplayComplete) => void;
      onError?: (error: Error) => void;
      onClose?: () => void;
    },
    opts?: { batchSize?: number; intervalMs?: number }
  ): () => void {
    const controller = new AbortController();
    const batchSize = bounded(opts?.batchSize, 1, 200, 50);
    const intervalMs = bounded(opts?.intervalMs, 100, 60_000, 700);
    const state = { closed: false };
    void (async () => {
      try {
        const query = new URLSearchParams({ batchSize: String(batchSize), intervalMs: String(intervalMs) });
        const response = await fetch(`${BASE}/live?${query.toString()}`, {
          headers: { Accept: 'text/event-stream' },
          signal: controller.signal,
          cache: 'no-store'
        });
        if (!response.ok) throw await streamError(response, '/live');
        await consumeSse(response, (event) => {
          if (!event.data || (event.event !== 'start' && event.event !== 'batch' && event.event !== 'replay-complete')) return;
          let parsed: unknown;
          try {
            parsed = JSON.parse(event.data) as unknown;
          } catch {
            throw new Error(`Malformed ${event.event} event from the server`);
          }
          if (event.event === 'start') {
            if (!isSsePayload(parsed)) throw new Error(`Malformed ${event.event} event from the server`);
            handlers.onStart?.(parsed as LiveStartEvent);
          } else if (event.event === 'batch') {
            if (!isSsePayload(parsed)) throw new Error(`Malformed ${event.event} event from the server`);
            handlers.onBatch?.(parsed as LiveBatch);
          } else if (event.event === 'replay-complete') {
            if (!isSsePayload(parsed)) throw new Error(`Malformed ${event.event} event from the server`);
            handlers.onComplete?.(parsed as LiveReplayComplete);
          }
        });
        finishStream(controller, handlers, state);
      } catch (error) {
        finishStream(controller, handlers, state, error);
      }
    })();
    return () => {
      controller.abort();
      finishStream(controller, handlers, state);
    };
  }

  modules = (options?: RequestOptions): Promise<ModuleInfo[]> => request<ModuleInfo[]>('/modules', {}, options);
  module = (id: string, options?: RequestOptions): Promise<ModuleInfo | null> =>
    optional<ModuleInfo>(`/modules/${encodeURIComponent(id)}`, undefined, options);
  algorithms = (options?: RequestOptions): Promise<AlgorithmInfo[]> => request<AlgorithmInfo[]>('/algorithms', {}, options);
  algorithm = (key: string, options?: RequestOptions): Promise<AlgorithmInfo | null> =>
    optional<AlgorithmInfo>(`/algorithms/${encodeURIComponent(key)}`, undefined, options);

  textHack = (queryClass: string, input: Record<string, unknown>, options?: RequestOptions): Promise<TextHackResponse> =>
    post<TextHackResponse>('/text-hack/query', { queryClass, input }, options);

  run = (algorithm: string, input: Record<string, unknown>, options?: RequestOptions): Promise<RunRecord> =>
    post<RunRecord>('/runs', { algorithm, input }, options);
  runs = (options?: RequestOptions): Promise<RunSummary[]> => request<RunSummary[]>('/runs', {}, options);
  runGet = (id: string, options?: RequestOptions): Promise<RunRecord> => request<RunRecord>(`/runs/${encodeURIComponent(id)}`, {}, options);
  runResult = (id: string, options?: RequestOptions): Promise<unknown> => request<unknown>(`/runs/${encodeURIComponent(id)}/result`, {}, options);

  runsStream(
    id: string,
    handlers: {
      onMeta?: (meta: RunMetaEvent) => void;
      onStep?: (step: TraceStep, index: number) => void;
      onComplete?: (complete: RunCompleteEvent) => void;
      onError?: (error: Error) => void;
      onClose?: () => void;
    }
  ): () => void {
    const controller = new AbortController();
    let stepIndex = 0;
    const state = { closed: false };
    void (async () => {
      try {
        const response = await fetch(`${BASE}/runs/${encodeURIComponent(id)}/events`, {
          headers: { Accept: 'text/event-stream' },
          signal: controller.signal,
          cache: 'no-store'
        });
        if (!response.ok) throw await streamError(response, `/runs/${id}/events`);
        await consumeSse(response, (event) => {
          if (!event.data || (event.event !== 'meta' && event.event !== 'step' && event.event !== 'complete')) return;
          let parsed: unknown;
          try {
            parsed = JSON.parse(event.data) as unknown;
          } catch {
            throw new Error(`Malformed ${event.event} event from the server`);
          }
          if (event.event === 'meta') {
            if (!isSsePayload(parsed)) throw new Error(`Malformed ${event.event} event from the server`);
            handlers.onMeta?.(parsed as RunMetaEvent);
          } else if (event.event === 'step') {
            if (!isSsePayload(parsed)) throw new Error(`Malformed ${event.event} event from the server`);
            handlers.onStep?.(parsed as TraceStep, stepIndex++);
          } else if (event.event === 'complete') {
            if (!isSsePayload(parsed)) throw new Error(`Malformed ${event.event} event from the server`);
            handlers.onComplete?.(parsed as RunCompleteEvent);
          }
        });
        finishStream(controller, handlers, state);
      } catch (error) {
        finishStream(controller, handlers, state, error);
      }
    })();
    return () => {
      controller.abort();
      finishStream(controller, handlers, state);
    };
  }
}

export const api = new Api();

export const SEARCH_PATHS = ['/search/naive', '/search/kmp', '/search/z', '/search/rabin-karp'] as const;
export type SearchPath = (typeof SEARCH_PATHS)[number];
