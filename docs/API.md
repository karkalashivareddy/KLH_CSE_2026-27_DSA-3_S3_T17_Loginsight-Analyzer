# API Reference

Base URL: `http://localhost:8080/api` (the Vite dev server proxies `/api` → `:8080`). All error
responses use one envelope produced by `GlobalExceptionHandler`:

```json
{ "status": 404, "error": "Not Found", "message": "No dataset loaded", "timestamp": "…", "path": "/api/overview" }
```

Semantics: `404 DatasetException` (no dataset / unknown id or service or incident), `400`
`InvalidQueryException` / validation, `500` unexpected. Sections 1–10 cover the LogInsight product
surface; sections 11–14 are the DSA laboratory/run surface.

## 1. Health

| Method | Endpoint | Response |
| --- | --- | --- |
| `GET` | `/health` | `{ status, service, timestamp }` |
| `GET` | `/health/status` | extends health with `uptimeMillis, datasetLoaded, datasetName, datasetSize, engines` |

## 2. Overview (dashboard)

`GET /overview?range=1h` — range one of `5m | 15m | 1h | 6h | 24h` (default `1h`). **404 when no
dataset is loaded.**

```json
{
  "dataset": "Demo Dataset", "systemStatus": "operational", "range": "1h",
  "events": 14000, "errors": 301, "warnings": 705, "services": 8, "hosts": 6,
  "eventsPerMinute": 210.0, "activeIncidents": 3,
  "timeline": [{ "start": "…", "end": "…", "count": 42 }],
  "severity": { "INFO": 12800, "WARN": 705, "ERROR": 298, "FATAL": 3 },
  "topServices": [/* ServiceStatsDto, §8 */],
  "topPatterns": [/* PatternDto, §5 */],
  "recentCritical": [/* LogEventDto */],
  "heatmap": { "days": ["Mon","Tue","Wed","Thu","Fri","Sat","Sun"], "columns": 24, "cells": [[…]] },
  "statusCodes": { "200": 12000, "404": 42 }
}
```

## 3. Logs, explorer and product search

`GET /logs?limit=100&offset=0` returns `LogEventDto[]` (newest slice). `GET /logs/stats` returns
dataset statistics. `GET /logs/{id}` returns one event or 404.

**Explorer** — `GET /logs/explore?q=&from=&to=&page=1&size=25&sort=timestamp:desc` (ISO-8601
`from`/`to`, `size ≤ 200`) returns the same `LogSearchResponse` as product search:

```json
{
  "query": "payment", "strategy": "KMP", "algorithm": "KMP", "pattern": "payment",
  "patternLength": 7, "textSize": 1234567, "durationNanos": 812000,
  "total": 14, "page": 1, "size": 25, "sort": "timestamp:desc", "dataset": "Demo Dataset",
  "matches": [
    {
      "event": { "id": 1, "timestamp": "…", "severityNumber": 1, "level": "ERROR",
                 "service": "payment-service", "host": "host-1", "ipAddress": "10.0.0.1",
                 "httpMethod": "POST", "endpoint": "/payments", "statusCode": 502,
                 "responseTime": 2400000, "requestId": "req-…", "userId": "user-…",
                 "message": "Connection timeout for payment 9900", "traceId": "…", "spanId": "…",
                 "url": "…", "source": "…", "rawMessage": "…", "attributes": {} },
      "snippet": "…Connection timeout for payment 9900…", "matchCount": 2
    }
  ],
  "suggestion": null
}
```

**Product search** — `POST /search` with body

```json
{ "query": "service:auth level:error", "from": null, "to": null, "page": 1, "size": 25, "sort": "timestamp:desc" }
```

returns the object above. Query grammar: `level:`, `service:`, `host:`, `source:`, `status:`,
`trace:`, `request:`, `message:` prefixes; an unknown prefix is treated as free text. When a search
misses, `suggestion` is populated:

```json
{ "suggestion": "payment", "similarityPct": 92, "matchCount": 14, "distance": 1, "algorithm": "Levenshtein" }
```

**Typeahead** — `GET /search/suggest?q=pay&limit=12` →

```json
[{ "type": "query", "label": "payment", "value": "payment" },
 { "type": "service", "label": "payment-service", "value": "service:payment-service" }]
```

`type ∈ service | host | endpoint | level | source | status | query`.

## 4. Datasets and ingestion

| Method | Endpoint | Notes |
| --- | --- | --- |
| `GET` | `/datasets` | `string[]` of bundled samples |
| `POST` | `/datasets/demo` | loads deterministic demo; returns `DatasetResult` |
| `POST` | `/datasets?file=&name=` | multipart upload; `file` required, `name` optional |
| `POST` | `/datasets/{name}` | loads a bundled sample; 404 body when unknown |
| `GET` | `/datasets/current` | `{ loaded:boolean, datasetName?, size?, totalLines?, failedLines?, loadedAt? }`; 404 body when empty |
| `DELETE` | `/datasets` | clears dataset |
| `GET` | `/ingestion/status` | `{ dataset, parser, streaming, availableSamples, sampleCount }` |
| `POST` | `/ingestion/demo` | same result as `/datasets/demo` |

`DatasetResult`:

```json
{ "loaded": "true", "datasetName": "Demo Dataset", "size": 14000,
  "totalLines": 14000, "failedLines": 0, "loadedAt": "…", "note": "…" }
```

## 5. Patterns

`GET /patterns?level=all&limit=50` → `PatternDto[]`; `GET /patterns/examples?template=<t>&limit=50`
→ `LogEventDto[]` for that template.

```json
{ "template": "Connection timeout for payment <*>", "count": 9,
  "example": "Connection timeout for payment 9900", "level": "ERROR" }
```

Templates are produced by heuristic token normalisation — the UI labels them "heuristic, not ML".

## 6. Incidents

| Method | Endpoint | Notes |
| --- | --- | --- |
| `GET` | `/incidents?limit=20` | `IncidentDto[]`, newest first |
| `GET` | `/incidents/count` | `long` active incidents |
| `GET` | `/incidents/{id}?logs=100` | `IncidentDetail` |
| `GET` | `/incidents/{id}/logs?limit=&offset=` | evidence events in the window |

```json
{ "id": 3, "start": "…", "end": "…", "services": ["payment-service"],
  "eventCount": 42, "primaryPattern": "Connection timeout for payment <*>",
  "status": "ACTIVE", "method": "Heuristic: 5-min windows vs dataset baseline" }
```

## 7. Analytics

| Method | Endpoint | Response |
| --- | --- | --- |
| `GET` | `/analytics/http` | `HttpStatsDto` |
| `GET` | `/analytics/hosts?limit=25` | `[{ host, events, errors, warnings, errorRate }]` |
| `GET` | `/analytics/heatmap` | `{ days, columns, cells }` (7×24, zeros included) |
| `GET` | `/analytics/windows?buckets=` | time-window traffic series |
| `GET` | `/analytics/top?dimension=&limit=` | top-K of service/endpoint/level/ip |
| `GET` | `/analytics/errors?limit=` | top error patterns |
| `GET` | `/analytics/dependencies` | service dependency graph (nodes/edges) |

`HttpStatsDto`:

```json
{ "statusCodes": { "200": 12000 }, "methods": { "POST": 3000 },
  "endpoints": [{ "endpoint": "/payments", "count": 900 }],
  "latencyP50": 240000, "latencyP95": 1200000, "latencyMax": 9000000, "sampled": 13000 }
```

## 8. Services

`GET /services?limit=50` → `ServiceStatsDto[]`:

```json
{ "name": "payment-service", "events": 3200, "errors": 88, "warnings": 210,
  "eventRate": 2.75, "hosts": 3, "latestAt": "…", "severity": { "INFO": 2800, "WARN": 210, "ERROR": 88 } }
```

`GET /services/{name}?recent=50` → `{ summary, recentEvents, totalEvents, activity }` where
`activity` is a 24-point `{ start, end, count }` series.

## 9. Live stream

`GET /live?batchSize=50&intervalMs=700` — SSE with named events:

- `start`: `{ source: "demo-replay", dataset, total, batchSize, paceMs, label }`
- `batch`: `{ sequence, source, dataset, emittedCount, total, events: LogEventDto[] }`
- `replay-complete`: `{ emitted, total }` then the stream closes.

`GET /live/status` → `{ enabled, dataset, total, source, label }`. The source is always labelled
honestly (`demo-replay`, "Demo replay stream — not real-time").

## 10. Analysis (catalogue + benchmark)

`GET /analysis/algorithms` → exposed algorithms grouped by module:

```json
[{ "module": "String Algorithms",
   "algorithms": [{ "key": "kmp", "name": "Knuth-Morris-Pratt", "problem": "PATTERN_SEARCH",
                    "queryType": "MATCH", "algorithmType": "single-pattern",
                    "canonicalEndpoint": "POST /api/search/kmp", "traceEndpoint": "POST /api/trace/search/kmp",
                    "timeComplexity": "O(n + m)", "spaceComplexity": "O(m)", "tracked": true,
                    "description": "…" }] }]
```

`GET /analysis/benchmarks/search?pattern=payment` → one **measured** run per matcher:

```json
{ "problem": "PATTERN_SEARCH", "dataset": "Demo Dataset", "pattern": "payment",
  "textLength": 1234567, "textPreview": "…",
  "results": [
    { "algorithm": "KMP", "matchCount": 2, "timeNanos": 812000, "timeMs": 0.812,
      "timeComplexity": "O(n + m)", "spaceComplexity": "O(m)" }
  ],
  "winner": "KMP",
  "note": "Measured once per matcher over the loaded dataset haystack — no fabricated averages." }
```

## 11. DSA laboratory endpoints

Classic single-engine endpoints remain exposed: `POST /search/{naive|kmp|z|rabin-karp}`,
`POST /search/multi`, `POST /string/suffix/build|search`, `POST /fuzzy/search`,
`POST /dp/{…}`, `POST /flow/{…}`, `POST /approx/{…}`, `POST /random/{…}`,
`POST /parallel/{reduce|scan|sort}`, `POST /benchmark/run`, `GET /trace/catalog`,
`POST /trace/{category}/{algorithm}`.

## 12. Run sessions (recorded traces)

`POST /runs { algorithm, input }` → recorded `RunRecord`. `GET /runs` → summaries.
`GET /runs/{id}` → full record. `GET /runs/{id}/result` → result. `GET /runs/{id}/events`
→ SSE replay (`meta` → `step`×n → `complete`). History is capped at 64 runs.

## 13. Catalogue

`GET /modules`, `GET /modules/{id}`, `GET /algorithms`, `GET /algorithms/{key}` and
`POST /text-hack/query` provide the 42-algorithm catalogue framing used by the Analysis screens.