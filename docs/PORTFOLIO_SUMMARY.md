# LogInsight Analyzer — Portfolio Project Summary

Three ready-to-paste summaries, all factually derived from the repository (commit `bbf5e45`). No
deployment, users or production usage are claimed.

---

## Version A — One line

> LogInsight Analyzer — a full-stack log analysis & investigation platform whose search, fuzzy
> matching, pattern discovery, incident detection and benchmarking are powered by real, measurable
> DSA algorithms (KMP, edit distance, flow, approximation, randomized, parallel), with 732 passing
> backend tests.

## Version B — Three lines (resume/project section)

> **LogInsight Analyzer — Algorithmic Log Intelligence Platform** · Spring Boot (Java 21) + React/TS
>
> End-to-end log analysis product where every feature maps to an implemented algorithm: KMP-powered
> search with measured duration, Levenshtein "did you mean", heuristic (not ML) pattern extraction,
> and windowed baseline incident detection with inspectable evidence.
>
> Engineered for transparency — 42-algorithm catalogue, single-run matcher benchmarks, and step-level
> trace replay over SSE; 732 tests / 0 failures; zero chart libraries (hand-rolled SVG); in-memory,
> deterministic demo dataset (14,000 events) with honest "not real-time" live replay.

## Version C — Technical paragraph (interviews / project discussions)

> LogInsight Analyzer is a full-stack, in-memory log analysis and investigation platform I built to
> demonstrate that classical algorithm theory can be turned directly into product features. The stack
> is Spring Boot 3.5 / Java 21 serving a typed REST + SSE API to a React 18 + TypeScript + Vite
> frontend with hand-rolled SVG charts. Logs are parsed (JSONL or text), normalized into a domain
> model, and indexed into sorted position lists with time-window lookups; a small query language
> (`level:`, `service:`, `host:`, ranges) is parsed and resolved through index intersections, and
> free-text search is executed by a real KMP matcher over a rendered haystack, reporting the algorithm,
> pattern length, text size and measured duration in the response. Zero-match searches fall back to a
> Levenshtein "did you mean" over the distinct message corpus. Patterns come from heuristic token
> normalization (explicitly labelled not-ML), and incidents are detected by 5-minute windowed baseline
> thresholding (`≥ max(3, 3×baseline)`) with exposeable supporting evidence. The same 42-algorithm DSA
> engine (strings, DP, flow, approximation, randomized, parallel) is exposed through an Algorithm
> Insights catalogue, a four-matcher measured benchmark (winner = smallest measured time, with a
> single-run disclaimer) and run sessions replayed step by step over SSE — 13 algorithms are
> traceable. The product is honest by design: the demo corpus is deterministic and in-memory, the live
> stream is a labelled "demo replay — not real-time", and there is no database, fake telemetry or
> marketing claim attached. Verification: 93 test classes / 732 tests / 0 failures, a clean production
> build, and a headless-Chromium QA pass (59 views, 0 JS/console errors, responsive 1440→375 px).

---

## Facts backing these summaries

| Claim | Source |
| --- | --- |
| 42 algorithms / 6 modules / 36 exposed / 13 traceable / 35 engines | `catalog/AlgorithmCatalog`, `service/CatalogService`, System page |
| Search uses KMP + reports method/algorithm/duration | `search/LogSearchService.java` (`algorithm = "KMP"`, `durationNanos`) |
| "Did you mean" via Levenshtein | `search/LogSearchService.java` → `dsa/dp/editdistance/LevenshteinDistance.java` |
| Patterns heuristic, not ML | `pattern/PatternExtractor.java`; UI label "Heuristic pattern extraction — Not ML" |
| Incidents = 5-min windows, `≥ max(3, 3×baseline)` | `incident/IncidentDetector.java` |
| Benchmark = Naive/KMP/Z/Rabin-Karp, single run, winner | `service/SearchBenchmarkService.java` |
| Demo dataset = seed `20260913L`, 14,000 events | `datasets/DemoDatasetGenerator.java` |
| Live = labelled `demo-replay`, "not real-time" | `service/LiveStreamService.java`, Live page |
| 732 tests / 0 failures / 0 errors (93 suites) | `backend/target/surefire-reports` (run `.\mvnw.cmd -o verify`) |
| QA 59 views, 0 blank/JS/console errors, responsive 1440→375 | `docs/LOGINSIGHT_REBUILD_REPORT.md`, Playwright run |
| No database/state/chart libraries | `README.md` engineering notes, `package.json`/`pom.xml`