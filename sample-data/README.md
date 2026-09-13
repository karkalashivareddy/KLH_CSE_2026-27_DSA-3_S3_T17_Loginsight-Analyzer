# Sample Data

Format specification for the datasets under `sample-data/`. Files are created in **Phase 2**
(log engine); this document defines their exact shape so the parser and datasets evolve in lockstep.

## 1. Canonical Text Log Format

```
<timestamp> | <LEVEL> | <SERVICE> | <ipAddress> | <HTTP_METHOD> | <endpoint> | <statusCode> | <responseTime> | <requestId> | <userId> | <message>
```

Example line:

```
2026-09-13T10:00:01Z | ERROR | AUTH | 10.0.0.1 | POST | /login | 401 | 45 | req-001 | user-101 | authentication failed
```

Field rules:

| Field | Type / rule |
|---|---|
| timestamp | ISO-8601 UTC `yyyy-MM-dd'T'HH:mm:ss'Z'`; second or millisecond precision |
| LEVEL | one of `INFO DEBUG WARN ERROR` (enum `LogLevel`) |
| SERVICE | one of `API_GATEWAY AUTH USER PAYMENT INVENTORY ORDER DATABASE NOTIFICATION` |
| ipAddress | dotted-quad IPv4 |
| HTTP_METHOD | `GET POST PUT DELETE PATCH` (enum `HttpMethod`) |
| endpoint | path string, e.g. `/login`, `/api/orders/{id}` |
| statusCode | 3-digit int |
| responseTime | int milliseconds |
| requestId | `req-\d{3,5}` |
| userId | `user-\d{3,5}` or `-` (anonymous) |
| message | free text; may contain spaces and colons |

Record separator: newline. The `|` delimiter is ` | ` (space-pipe-space). `userId` uses `-` for
anonymous. The message field is the final field and is not allowed to contain the `|` sequence.

## 2. JSON Lines Format (`logs-json.jsonl`)

One JSON object per line. All fields optional except `timestamp` and `message`:

```json
{"timestamp":"2026-09-13T10:00:01Z","level":"ERROR","service":"AUTH","host":"api-1","ipAddress":"10.0.0.1","httpMethod":"POST","endpoint":"/login","statusCode":401,"responseTime":45,"requestId":"req-001","userId":"user-101","message":"authentication failed"}
```

Host is only present in JSON variant (adds realism for hosts/unique hosts analytics) and is optional
in the domain model (`LogEvent.host`).

## 3. File Inventory (created Phase 2)

| File | Approx. lines | Purpose |
|---|---|---|
| `logs-small.txt` | 1,000 | demo + tests; dashboard load |
| `logs-medium.txt` | 10,000 | default demo dataset; string search on real corpus |
| `logs-large.txt` | 100,000 | suffix array + performance + export demo |
| `logs-malformed.txt` | ~400 (mix) | contains bad timestamps, missing fields, bad levels, stray pipes; exercises ParserException handling and partial import |
| `logs-json.jsonl` | 5,000 | JSON Lines parser path |
| `logs-multi-service.txt` | 8,000 | trace-chaining (requestId spans services) for sequence alignment + service graph depth |

## 4. Generation Rules

- Sizes stay approximate; each file is generated once by a helper (JUnit `@Tag("gen")` test or a
  small CLI util in Phase 2) and committed as a static artifact so the repo is self-contained.
- `logs-malformed.txt` deliberately breaks ~15% of lines so partial-import behaviour is visible in
  the UI (line numbers + reasons reported).
- Service mixtures and error rates are controlled to make every chart meaningful (e.g. AUTH and
  DATABASE carry the ERROR dominated traffic; 401 cluster around AUTH).
- One template message per ~3 ERROR messages reused heavily so KMP/Aho-Corasick/fuzzy/suffix
  features have realistic repetition to find.

## 5. Ownership

Every file listed in `docs/03` §5 is generated once by `SampleDataGenerator` — a deterministic
(JUnit `@Tag("gen")`) helper in `backend/src/test/java/com/loginsight/support/`. The generator uses a
fixed seed (`20260913L`) and **regenerates a file only when it is missing or empty**, so the files
are committed as static, reproducible artifacts and the repo stays self-contained.

Format policy enforced by the parser tests (`SampleDataParsingTest`):

- Valid text files must parse with zero failures; `logs-malformed.txt` must fail on roughly 15% of
  lines while every remaining line parses cleanly.
- `logs-malformed.txt` deliberately contains bad timestamps, missing fields, bad levels, and stray
  ` | ` sequences inside the message field so partial-import behaviour is visible in the UI
  (line numbers + reasons reported).
- The generator never writes a bare `|` into the message column of a valid line.