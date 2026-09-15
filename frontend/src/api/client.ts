import type {
  ApiError,
  AlgorithmResult,
  BenchmarkRequest,
  BenchmarkRow,
  DatasetName,
  DatasetStats,
  DatasetSummary,
  ErrorAnalytics,
  Health,
  LogEvent,
  ObjectApiResponse,
  SystemStatus,
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

  search = (path: '/search/naive' | '/search/kmp' | '/search/z' | '/search/rabin-karp', payload: unknown): Promise<AlgorithmResult> =>
    post<AlgorithmResult>(path, payload);

  benchmark = (payload: BenchmarkRequest): Promise<BenchmarkRow[]> => post<BenchmarkRow[]>('/benchmark/run', payload);
}

export const api = new Api();

/** Endpoint paths the search family supports. */
export const SEARCH_PATHS = ['/search/naive', '/search/kmp', '/search/z', '/search/rabin-karp'] as const;
export type SearchPath = (typeof SEARCH_PATHS)[number];