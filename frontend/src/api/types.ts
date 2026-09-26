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

/** GET /api/logs (slice) and GET /api/logs/{id} (backend LogEventDto). */
export interface LogEvent {
  id: number;
  timestamp: string;
  severityNumber: number;
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
  traceId: string | null;
  spanId: string | null;
  url: string | null;
  source: string | null;
  rawMessage: string | null;
  attributes: Record<string, string>;
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
  highlighted: number[] | null;
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

/* ── RESTART · TextHack laboratory surface (backend docs/REBUILD_BASELINE Phase 2-4) ───────── */

/** GET /api/modules — module framing with computed counts (backend ModuleInfo). */
export interface ModuleInfo {
  id: string;
  label: string;
  title: string;
  description: string;
  accent: string;
  algorithmCount: number;
  exposedCount: number;
  trackableCount: number;
  algorithms: AlgorithmInfo[];
}

/** GET /api/algorithms and GET /api/algorithms/{key} (backend AlgorithmInfo). */
export interface AlgorithmInfo {
  key: string;
  name: string;
  moduleId: string;
  moduleLabel: string;
  problem: string;
  queryType: string;
  algorithmType: string;
  canonicalEndpoint: string | null;
  traceEndpoint: string | null;
  timeComplexity: string;
  spaceComplexity: string;
  tracked: boolean;
  exposed: boolean;
  defaultInput: Record<string, unknown> | null;
  description: string;
}

/** POST /api/text-hack/query response (backend TextHackResponseDto). */
export interface TextHackResponse {
  queryClass: string;
  label: string;
  moduleId: string;
  moduleLabel: string;
  description: string;
  recommended: AlgorithmInfo[];
  executed: AlgorithmResult | null;
  traceAlgorithmKey: string | null;
}

/** POST /api/runs response — the full recorded run (backend RunRecord). */
export interface RunRecord {
  runId: string;
  algorithm: string;
  algorithmName: string;
  category: string;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  completedAt: string | null;
  stepCount: number;
  executionTimeNanos: number;
  truncated: boolean;
  timeComplexity: string;
  spaceComplexity: string;
  input: Record<string, unknown>;
  result: unknown;
  steps: TraceStep[];
  error: string | null;
}

/** GET /api/runs — summaries (backend RunSummaryDto). */
export interface RunSummary {
  runId: string;
  algorithm: string;
  algorithmName: string;
  category: string;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  completedAt: string | null;
  stepCount: number;
  executionTimeNanos: number;
  truncated: boolean;
  timeComplexity: string;
  spaceComplexity: string;
}

/** SSE events emitted by GET /api/runs/{id}/events. */
export interface RunMetaEvent {
  runId: string;
  algorithm: string;
  algorithmName: string;
  category: string;
  status: string;
  stepCount: number;
  truncated: boolean;
  timeComplexity: string;
  spaceComplexity: string;
  executionTimeNanos: number;
  result: unknown;
  error: string | null;
}

export interface RunCompleteEvent {
  runId: string;
  stepCount: number;
  executionTimeNanos: number;
  truncated: boolean;
  status: string;
}

/** Course map row input: a module with its exercise/feature matrix statuses. */
export type LabStatus = 'implemented' | 'exposed' | 'traceable' | 'extra';

/* ── LogInsight Analyzer product surface (backend dto/response, docs/API.md) ───────────────── */

export interface DatasetResult {
  loaded: boolean | string;
  datasetName: string | null;
  size: number;
  totalLines: number;
  failedLines: number;
  loadedAt: string | null;
  note?: string;
  error?: string;
}

/** GET /api/ingestion/status */
export interface IngestionStatus {
  dataset: DatasetSummary;
  parser: string;
  streaming: string;
  availableSamples: string[];
  sampleCount: number;
}

/** Body of POST /api/search and the query string shape of GET /api/logs/explore. */
export interface LogSearchRequest {
  query: string;
  from?: string | null;
  to?: string | null;
  page: number;
  size: number;
  sort: string;
}

/** POST /api/search · GET /api/logs/explore response (backend LogSearchResponse). */
export interface LogSearchResponse {
  query: string;
  strategy: string;
  algorithm: string;
  pattern: string;
  patternLength: number;
  textSize: number;
  durationNanos: number;
  total: number;
  page: number;
  size: number;
  sort: string;
  dataset: string;
  matches: SearchHit[];
  suggestion: FuzzySuggestion | null;
}

export interface SearchHit {
  event: LogEvent;
  snippet: string;
  matchCount: number;
}

export interface FuzzySuggestion {
  suggestion: string;
  similarityPct: number;
  matchCount: number;
  distance: number;
  algorithm: string;
}

export type SuggestionType = 'service' | 'host' | 'endpoint' | 'level' | 'source' | 'status' | 'query';

/** GET /api/search/suggest — a clickable typeahead candidate. */
export interface SuggestionDto {
  type: SuggestionType;
  label: string;
  value: string;
}

/** GET /api/patterns · GET /api/patterns/examples (backend PatternDto). */
export interface PatternDto {
  template: string;
  count: number;
  example: string;
  level: string;
}

/** GET /api/incidents (backend IncidentDto) — heuristic detection, never ML. */
export interface IncidentDto {
  id: number;
  start: string;
  end: string;
  services: string[];
  eventCount: number;
  primaryPattern: string;
  status: string;
  method: string;
}

/** GET /api/incidents/{id} — the incident plus its supporting evidence. */
export interface IncidentDetail {
  incident: IncidentDto;
  events: LogEvent[];
  total: number;
}

/** GET /api/services (backend ServiceStatsDto). */
export interface ServiceStatsDto {
  name: string;
  events: number;
  errors: number;
  warnings: number;
  eventRate: number;
  hosts: number;
  latestAt: string | null;
  severity: Record<string, number>;
}

/** GET /api/services/{name} (backend ServiceStatsDto.ServiceDetail). */
export interface ServiceDetail {
  summary: ServiceStatsDto;
  recentEvents: LogEvent[];
  totalEvents: number;
  activity: Array<{ start: string; end: string; count: number }>;
}

/** GET /api/analytics/http (backend HttpStatsDto). */
export interface HttpStatsDto {
  statusCodes: Record<string, number>;
  methods: Record<string, number>;
  endpoints: Array<{ endpoint: string; count: number }>;
  latencyP50: number;
  latencyP95: number;
  latencyMax: number;
  sampled: number;
}

/** GET /api/analytics/hosts row. */
export interface HostRow {
  host: string;
  events: number;
  errors: number;
  warnings: number;
  errorRate: number;
}

/** GET /api/analytics/heatmap (backend HeatmapAnalyzer.Heatmap). */
export interface Heatmap {
  days: string[];
  columns: number;
  cells: number[][];
}

/** GET /api/overview (backend OverviewDto) — 404 when no dataset is loaded. */
export interface OverviewDto {
  dataset: string;
  datasetEvents: number;
  windowStart: string;
  windowEnd: string;
  scope: string;
  systemStatus: string;
  events: number;
  errors: number;
  warnings: number;
  services: number;
  hosts: number;
  eventsPerMinute: number;
  activeIncidents: number;
  timeline: Array<{ start: string; end: string; count: number }>;
  range: string;
  severity: Record<string, number>;
  topServices: ServiceStatsDto[];
  topPatterns: PatternDto[];
  recentCritical: LogEvent[];
  heatmap: Heatmap;
  statusCodes: Record<string, number>;
}

/** SSE event named `start` on GET /api/live. */
export interface LiveStartEvent {
  source: string;
  dataset: string;
  total: number;
  batchSize: number;
  paceMs: number;
  label: string;
}

/** SSE event named `batch` on GET /api/live. */
export interface LiveBatch {
  sequence: number;
  source: string;
  dataset: string;
  emittedCount: number;
  total: number;
  events: LogEvent[];
}

/** SSE event named `replay-complete` on GET /api/live. */
export interface LiveReplayComplete {
  emitted: number;
  total: number;
}

/** GET /api/live/status */
export interface LiveStatus {
  enabled: boolean;
  dataset: string | null;
  total: number;
  source: string | null;
  label: string;
}

/** GET /api/analysis/algorithms — one algorithm entry (subset of AlgorithmInfo). */
export interface AlgorithmGroupItem {
  key: string;
  name: string;
  problem: string;
  queryType: string;
  algorithmType: string;
  canonicalEndpoint: string | null;
  traceEndpoint: string | null;
  timeComplexity: string;
  spaceComplexity: string;
  tracked: boolean;
  defaultInput: Record<string, unknown> | null;
  description: string;
}

export interface AlgorithmGroup {
  module: string;
  algorithms: AlgorithmGroupItem[];
}

/** GET /api/analysis/benchmarks/search — one measured execution per matcher. */
export interface SearchBenchmarkRow {
  algorithm: string;
  matchCount: number;
  timeNanos: number;
  timeMs: number;
  timeComplexity: string;
  spaceComplexity: string;
}

export interface SearchBenchmarkResponse {
  problem: string;
  dataset: string;
  pattern: string;
  textLength: number;
  textPreview: string;
  results: SearchBenchmarkRow[];
  winner: string;
  note: string;
}