# Datasets and Replay

LogInsight keeps at most one dataset in backend memory. Product screens do not show substitute metrics when the dataset is empty; dataset-backed endpoints return a not-found response and the UI presents a first-run state.

## Sources

### Demo Dataset

`POST /api/datasets/demo` and `POST /api/ingestion/demo` call `DemoDatasetGenerator`.

- 14,000 events.
- Deterministic content seed `20260913L`.
- Time range anchored to the current hour so recent ranges have data.
- Eight demo services: `api-gateway`, `auth-service`, `user-service`, `payment-service`, `order-service`, `notification-service`, `database` and `cache`.
- Structured HTTP fields, hosts, IPs, request/user IDs, trace/span IDs, response times, raw lines and source metadata.
- Correlated error bursts give the heuristic incident detector meaningful windows.

The corpus is synthetic demo data. It is not a production log capture and is labelled in the UI and API.

### Bundled samples

`GET /api/datasets` lists committed files from the configured `sample-data` directory. `POST /api/datasets/{name}` loads one by plain file name.

| File | Approximate size | Intended use |
|---|---:|---|
| `logs-small.txt` | 1,000 lines | Small dashboard and test corpus |
| `logs-medium.txt` | 10,000 lines | General search and analytics |
| `logs-large.txt` | 100,000 lines | Larger in-memory search and performance experiments |
| `logs-json.jsonl` | 5,000 lines | JSONL parser path |
| `logs-multi-service.txt` | 8,000 lines | Cross-service request trails |
| `logs-malformed.txt` | 400 lines | Deliberate partial-import failures |

The committed files are the source of truth for parser tests. The sample service resolves only a normalized child of the configured directory and rejects traversal attempts.

### Upload

`POST /api/datasets` accepts multipart fields:

- `file`: required raw file.
- `name`: optional dataset name; the original filename is used when omitted.

The parser auto-detects JSONL when the first non-blank line starts with `{`; otherwise it parses the canonical 11-field text format:

```text
timestamp | LEVEL | SERVICE | ipAddress | HTTP_METHOD | endpoint | statusCode | responseTime | requestId | userId | message
```

Text parsing records malformed lines individually. JSONL requires `timestamp` and `message`; optional fields remain nullable and unknown top-level fields are retained as attributes. A successful load reports `size`, `totalLines`, `failedLines` and `loadedAt`.

The upload ceiling is 64 MB at both the Spring multipart boundary and the service read boundary. A stream that is too large or produces no records is rejected.

## Dataset lifecycle

```text
no dataset
   ├─ POST /api/datasets/demo
   ├─ POST /api/datasets/{bundled-name}
   └─ POST /api/datasets multipart upload
          ↓
     one current Dataset
          ├─ product analysis and search
          ├─ bounded SSE replay (Demo Replay)
          └─ DELETE /api/datasets → no dataset
```

A backend restart clears the active dataset. There is no persistence, multi-dataset selection, retention policy or external collector.

## Demo Replay is not live ingestion

These documents call this surface **Demo Replay**. Its route is `/live`, and the sidebar entry and page heading are currently labelled `Live Replay`; that label is a navigation name, not a real-time claim.

`GET /api/live` replays the loaded events oldest-first in batches. The backend snapshots the current event list at subscribe time and sorts it by `(timestamp, id)`, so ordering does not depend on ingestion order. `GET /api/live/status` reports the source and label. The stream emits `start`, `batch` and `replay-complete` events, and the UI retains the `demo-replay` / “not real-time” disclosure. Replay controls accept `batchSize=1..200` and `intervalMs=100..60000`; invalid values return the normal 400 envelope.

The replay is bounded by the current event list and is finite: it ends with `replay-complete` rather than continuing to wait, and the emitter has a 6-hour ceiling. It does not connect to Kafka, a log collector, a WebSocket, or a remote telemetry source. Even when the current dataset came from an upload, the application still exposes this as a replay surface rather than claiming live ingestion.

On the client, `frontend/src/replay/ReplayContext.tsx` owns the single SSE subscription. `ReplayProvider` is mounted once in `App.tsx` above the router, so the Command Center replay card and the Demo Replay page read the same state, progress and 300-event buffer rather than each opening a connection. The provider resets the stream when a dataset change is broadcast.

## Related documentation

- [API reference](API.md)
- [Architecture](ARCHITECTURE.md)
- [Command Center](COMMAND_CENTER.md)
- [Current implementation audit](IMPLEMENTATION_AUDIT.md)
- [Sample-data format notes](../sample-data/README.md)
