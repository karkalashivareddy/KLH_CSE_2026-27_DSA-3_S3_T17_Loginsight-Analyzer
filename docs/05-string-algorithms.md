# 05 - String Algorithms (theory supplement)

This file documents the string-algorithm topics that are **conceptual only** in this project, stated
honestly alongside the implemented machinery.

## Implemented (real engines, trace-instrumented)

| Algorithm | Key | Canonical endpoint | Trace endpoint |
|---|---|---|---|
| Naive search | `naive` | `POST /api/search/naive` | `POST /api/trace/search/naive` |
| KMP | `kmp` | `POST /api/search/kmp` | `POST /api/trace/search/kmp` |
| Z-algorithm | `z` | `POST /api/search/z` | `POST /api/trace/search/z` |
| Rabin-Karp | `rabinkarp` | `POST /api/search/rabin-karp` | `POST /api/trace/search/rabin-karp` |
| Aho-Corasick (multi-pattern) | `aho_corasick` | `POST /api/search/multi` | library-only |
| Suffix array + LCP construction / search | `suffixarray`, `kasai_lcp` | `POST /api/string/suffix/build`, `POST /api/string/suffix/search` | library-only |

The four search engines are cross-verified so `Naive == KMP == Z == Rabin-Karp` match sets
(`docs/13-testing.md`). Add suffix-array interval search to that same chain where the pattern appears
inside the corpus.

## Conceptual (documented, not implemented)

- **SA-IS O(n) suffix construction.** The suffix array is currently built with an O(n log n)
  sorting-based construction which is correct and adequate for the corpus sizes used in the demo.
  Refining it to the linear-time SA-IS / skew algorithm is a roadmap item, not a gap in correctness.
- **Real-time streaming KMP state machines.** The engines operate on an in-memory corpus; a
  character-stream interface would let KMP persist its `lps`/cursor state across chunks.

## Lab map

Frontend: Laboratory group **String Algorithms** (4 traceable). TextHack "Pattern Search" executes
the KMP engine directly.