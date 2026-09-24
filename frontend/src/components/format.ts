const compactFormatter = new Intl.NumberFormat('en-US', { maximumFractionDigits: 1 });

/** 12345 → "12.3k", 987 → "987" */
export function formatNumber(n: number): string {
  if (Math.abs(n) >= 1e6) return `${compactFormatter.format(n / 1e6)}M`;
  if (Math.abs(n) >= 1e3) return `${compactFormatter.format(n / 1e3)}k`;
  return String(n);
}

/** Nanoseconds → human-readable duration (e.g. "1.2 ms", "43 µs", "82 ns"). */
export function formatNanos(nanos: number): string {
  if (nanos < 1_000) return `${nanos} ns`;
  if (nanos < 1_000_000) return `${(nanos / 1_000).toFixed(1)} µs`;
  if (nanos < 1_000_000_000) return `${(nanos / 1e6).toFixed(1)} ms`;
  return `${(nanos / 1e9).toFixed(2)} s`;
}

/** ISO timestamp → short display string (e.g. "15 Sep 12:34:05"). */
export function formatTs(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  });
}

/** 123456 ms → "2m 3.4s". */
export function formatDuration(ms: number): string {
  if (ms < 1000) return `${Math.round(ms)} ms`;
  const s = ms / 1000;
  if (s < 60) return `${s.toFixed(1)}s`;
  const m = Math.floor(s / 60);
  return `${m}m ${(s - 60 * m).toFixed(1)}s`;
}

/** Format bytes as human-readable. */
export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Normalise log level to a CSS class key. */
export function levelClass(level: string | null): string {
  return (level ?? 'UNKNOWN').toUpperCase();
}

/** Badge colour mapping for log levels. */
export const LEVEL_COLORS: Record<string, string> = {
  FATAL: '#c0392b',
  ERROR: '#e74c3c',
  WARN:  '#f1c40f',
  INFO:  '#3498db',
  DEBUG: '#95a5a6',
  TRACE: '#9b59b6'
};

/** Derive a bar-chart normalised percentage list. */
export function normaliseBuckets<T extends { service: string; count: number }>(items: T[]): Array<{ label: string; value: number; pct: number }> {
  const max = Math.max(...items.map((b) => b.count), 1);
  return items.map((b) => ({
    label: b.service,
    value: b.count,
    pct: Math.round((b.count / max) * 100)
  }));
}

/* ── Module identity helpers (TextHack laboratory) ─────────────────────────────────────── */

/** Module id → CSS accent variable name. Falls back to the global accent. */
export function moduleAccent(moduleId: string): string {
  switch (moduleId) {
    case 'strings':     return 'var(--mod-strings)';
    case 'dp':          return 'var(--mod-dp)';
    case 'flow':        return 'var(--mod-flow)';
    case 'approximation': return 'var(--mod-approx)';
    case 'randomized':  return 'var(--mod-random)';
    case 'parallel':    return 'var(--mod-parallel)';
    default:            return 'var(--accent)';
  }
}

/** Module id → glow (soft background) variable name for tinted panes. */
export function moduleGlow(moduleId: string): string {
  switch (moduleId) {
    case 'strings':     return 'var(--glow-strings)';
    case 'dp':          return 'var(--glow-dp)';
    case 'flow':        return 'var(--glow-flow)';
    case 'approximation': return 'var(--glow-approx)';
    case 'randomized':  return 'var(--glow-random)';
    case 'parallel':    return 'var(--glow-parallel)';
    default:            return 'var(--accent-dim)';
  }
}

/** Resolve a module id to its CSS custom-property name, e.g. "strings" → "mod-strings". */
export function moduleVar(moduleId: string): string {
  const map: Record<string, string> = {
    strings: 'mod-strings',
    dp: 'mod-dp',
    flow: 'mod-flow',
    approximation: 'mod-approx',
    randomized: 'mod-random',
    parallel: 'mod-parallel'
  };
  return map[moduleId] ?? 'accent';
}

/** Short label for module ids used by the backend. */
export function moduleLabel(moduleId: string): string {
  const map: Record<string, string> = {
    strings: 'String Algorithms',
    dp: 'Dynamic Programming',
    flow: 'Graph & Flow',
    approximation: 'Approximation',
    randomized: 'Randomized',
    parallel: 'Parallel'
  };
  return map[moduleId] ?? moduleId;
}

/** Algorithm key → module id (mirrors the backend AlgorithmCatalog). */
export function moduleForAlgorithm(key: string): string {
  const map: Record<string, string> = {
    naive: 'strings', kmp: 'strings', z: 'strings', rabinkarp: 'strings',
    aho_corasick: 'strings', suffix_array: 'strings', suffix_search: 'strings',
    kasai_lcp: 'strings', fuzzy_search: 'strings',
    levenshtein: 'dp', damerau: 'dp', weighted_edit: 'dp',
    global_alignment: 'dp', local_alignment: 'dp', matrixchain: 'dp',
    optimal_bst: 'dp', bitmask_tsp: 'dp', hamiltonian: 'dp',
    tree_diameter: 'dp', rerooting: 'dp', sos: 'dp',
    fordfulkerson: 'flow', edmondskarp: 'flow', dinic: 'flow', min_cut: 'flow',
    bipartite_matching: 'flow', min_cost_max_flow: 'flow',
    vertexcover: 'approximation', set_cover: 'approximation',
    incident_cover: 'approximation', bounded_vertex_cover: 'approximation',
    vertex_cover_kernelization: 'approximation', knapsack_fptas: 'approximation',
    vc_is_reduction: 'approximation',
    quicksort: 'randomized', millerrabin: 'randomized', reservoir: 'randomized',
    universal_hash: 'randomized', perfect_hash: 'randomized',
    parallel_reduce: 'parallel', parallel_scan: 'parallel', parallel_sort: 'parallel'
  };
  return map[key] ?? '';
}