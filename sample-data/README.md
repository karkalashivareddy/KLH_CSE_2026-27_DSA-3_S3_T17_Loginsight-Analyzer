# Sample Data

The files in this directory are committed, parser-tested inputs for LogInsight Analyzer. They are static examples; the application’s **Demo Dataset** is generated separately in memory and is not one of these files.

## Canonical text format

```text
<timestamp> | <LEVEL> | <SERVICE> | <ipAddress> | <HTTP_METHOD> | <endpoint> | <statusCode> | <responseTime> | <requestId> | <userId> | <message>
```

The parser expects exactly 11 fields separated by ` | `. `userId` is `-` for anonymous. The message is the final field and must not contain the delimiter sequence. Blank lines are skipped; malformed non-blank lines are recorded as parse failures.

## JSON Lines format

`logs-json.jsonl` contains one JSON object per line. `timestamp` and `message` are required. Other canonical fields are optional; `host` and unknown top-level fields are retained by the domain model/attribute bag.

```json
{"timestamp":"2026-09-13T10:00:01Z","level":"ERROR","service":"AUTH","host":"api-1","message":"authentication failed"}
```

## Inventory

| File | Approximate lines | Purpose |
|---|---:|---|
| `logs-small.txt` | 1,000 | Small dashboard and parser fixture |
| `logs-medium.txt` | 10,000 | General search and analytics |
| `logs-large.txt` | 100,000 | Larger in-memory experiments |
| `logs-malformed.txt` | 400 | Deliberate partial-import failures |
| `logs-json.jsonl` | 5,000 | JSONL parser path |
| `logs-multi-service.txt` | 8,000 | Cross-service request trails |

The exact parser outcomes are covered by `SampleDataParsingTest`; use the current API summary (`size`, `totalLines`, `failedLines`) rather than assuming a file is failure-free.

## Loading

The backend lists these files through `GET /api/datasets` and loads one with `POST /api/datasets/{name}`. The configured directory is `loginsight.sample-data.dir`; the Docker image sets it to `/app/sample-data`. A file can also be uploaded through `POST /api/datasets` as multipart form data.

The current runtime holds one dataset in memory. Restarting the backend clears it. See [../docs/DATASET.md](../docs/DATASET.md) for lifecycle, parser details and the explicit distinction between demo data and SSE replay.
