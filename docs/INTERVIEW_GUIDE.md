# Interview Guide

These questions are grounded in the current source tree and are useful when
discussing LogInsight in a technical interview.

## Product and architecture

- What happens when a bundled log file is ingested?
  - Explain parsing, normalization, in-memory services, analytics, and the
    React client consuming JSON responses.
- Why is there no database?
  - The current project is a reproducible algorithm laboratory; persistence and
    bounded retention are future concerns, not hidden capabilities.
- How does the frontend reach the backend locally?
  - Vite proxies `/api` requests to the Spring Boot service.

## Algorithms

- Which algorithms are implemented in each family?
  - Use the README matrix and the package layout as the source of truth.
- How do you explain complexity for a selected endpoint?
  - Identify the engine, input sizes, data structures, and whether the trace or
    benchmark adds extra work.
- How do you know an algorithm implementation is correct?
  - Discuss focused tests, cross-checks, edge cases, and independently verified
    results where those tests exist.

## Tracing and benchmarking

- How does trace playback work?
  - The backend records execution steps through `StepRecorder`; the frontend
    replays the returned trace catalog.
- What does a benchmark result mean?
  - It compares the requested implementations on the supplied input and host;
    it is not a general performance guarantee.

## Scaling and limitations

- What would change for production-scale log ingestion?
  - Add durable storage, streaming ingestion, authentication, retention limits,
    resource quotas, and bounded trace generation.
- What is intentionally not claimed by this repository?
  - No external database, no multi-instance durability, and no fixed coverage or
    performance number without a fresh run.

