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