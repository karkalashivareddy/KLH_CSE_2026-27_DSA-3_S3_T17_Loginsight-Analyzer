/**
 * Wire contracts mirroring the backend DTOs (backend/dto/...). Kept as plain interfaces so the
 * dashboard renders real API responses only — every field maps 1:1 to a documented endpoint.
 */

/** GET /api/health */
export interface Health {
  status: string;
  service: string;
  timestamp: string;
}

/** GET /api/health/status */
export interface SystemStatus extends Health {
  uptimeMillis: number;
  datasetLoaded: boolean;
  datasetName: string | null;
  datasetSize: number;
  engines: number;
}

/** GET /api/health/dataset and GET /api/datasets/current (404 body when not loaded) */
export interface DatasetSummary {
  loaded: boolean;
  datasetName?: string;
  size?: number;
  totalLines?: number;
  failedLines?: number;
  loadedAt?: string;
  error?: string;
}

/** GET /api/datasets */
export type DatasetName = string;

/** GET /api/logs (slice) */
export interface LogEvent {
  id: number;
  timestamp: string;
  level: string | null;
  service: string;
  host: string;
  ipAddress: string;
  httpMethod: string | null;
  endpoint: string;
  statusCode: number;
  responseTime: number;
  requestId: string;
  userId: string;
  message: string;
}

/** GET /api/logs/stats */
export interface DatasetStats {
  totalLogs: number;
  errors: number;
  warnings: number;
  services: number;
  uniqueIps: number;
  topError: string | null;
  avgResponseTimeMs: number;
  requestsPerMinute: number;
  levels: Record<string, number>;
  topServices: TopBucket[];
  statusCodes: Record<string, number>;
  logsOverTime: TimeBucket[];
}

export interface TopBucket {
  service: string;
  count: number;
}

/** GET /api/analytics/windows */
export interface TimeBucket {
  start: string;
  end: string;
  count: number;
}

/** Object API response */
export interface ObjectApiResponse {
  nodes: GraphNode[];
  edges: GraphEdge[];
  nodeCount: number;
  edgeCount: number;
}

export interface GraphNode {
  id: string;
  events: number;
  outDegree: number;
  inDegree: number;
}

export interface GraphEdge {
  source: string;
  target: string;
  weight: number;
}

/** GET /api/analytics/errors */
export interface ErrorAnalytics {
  topError: string | null;
  patterns: Record<string, number>;
}

/** GET /api/analytics/top */
export type TopDimension = 'service' | 'endpoint' | 'level' | 'ip';

/** POST /api/benchmark/run */
export interface BenchmarkRow {
  algorithm: string;
  inputSize: number;
  executionTimeNanos: number;
  throughputPerSec: number;
  resultSize: number;
  sequentialNanos: number | null;
  parallelNanos: number | null;
  speedup: number | null;
  work: number | null;
  span: number | null;
  parallelism: number | null;
}

export interface BenchmarkRequest {
  scenario: string;
  sizes: number[];
  repetitions: number;
  parallelism: number;
}

/** GET /api/trace/catalog */
export interface TraceCatalogEntry {
  key: string;
  category: string;
  name: string;
  endpoint: string;
  defaultInput: Record<string, unknown>;
  timeComplexity: string;
  spaceComplexity: string;
  description: string;
}

/** POST /api/trace/{...} */
export interface TraceStep {
  index: number;
  operation: string;
  description: string;
  state: Record<string, unknown>;
  highlighted: string[] | null;
  metrics: Record<string, unknown> | null;
}

export interface TraceResponse {
  algorithm: string;
  category: string;
  result: unknown;
  intermediateData: unknown;
  steps: TraceStep[];
  truncated: boolean;
  executionTimeNanos: number;
  timeComplexity: string;
  spaceComplexity: string;
}

/** POST /api/search/... / /api/dp/... / /api/flow/... etc. (canonical envelope) */
export interface AlgorithmResult {
  algorithm: string;
  queryType: string;
  inputSize: number;
  pattern: string | null;
  result: unknown;
  intermediateData: unknown;
  executionTimeNanos: number;
  memoryEstimateBytes: number;
  timeComplexity: string;
  spaceComplexity: string;
  notes: string | null;
}

/** Global error envelope (backend GlobalExceptionHandler -> ErrorResponseDto) */
export interface ApiError {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  path: string;
}