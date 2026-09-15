import { type CSSProperties, type ReactNode } from 'react';
import { LEVEL_COLORS } from './format';

/* ────────────────────────────────────────────────────────────────────────────────────────── */

export function Card({ title, children, className = '', actions }: {
  title?: string;
  children: ReactNode;
  className?: string;
  actions?: ReactNode;
}) {
  return (
    <div className={`card ${className}`}>
      {(title || actions) && (
        <div className="card-header">
          {title && <h2 className="card-title">{title}</h2>}
          {actions && <div className="card-actions">{actions}</div>}
        </div>
      )}
      <div className="card-body">{children}</div>
    </div>
  );
}

export function StatCard({ label, value, sub, color }: {
  label: string;
  value: string | number;
  sub?: string;
  color?: string;
}) {
  return (
    <div className="stat-card" style={color ? { borderLeftColor: color } : undefined}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {sub && <div className="stat-sub">{sub}</div>}
    </div>
  );
}

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="spinner-wrap">
      <div className="spinner" />
      <span className="spinner-label">{label}</span>
    </div>
  );
}

export function ErrorBox({ error, retry }: { error: Error; retry?: () => void }) {
  return (
    <div className="error-box">
      <div className="error-title">Error</div>
      <div className="error-msg">{error.message}</div>
      {retry && (
        <button className="btn" onClick={retry}>Retry</button>
      )}
    </div>
  );
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <div className="empty-state">{children}</div>;
}

export function Badge({ children, color }: { children: ReactNode; color?: string }) {
  const style: CSSProperties = color
    ? { background: color, color: '#fff', borderColor: color }
    : {};
  return (
    <span className="badge" style={style}>{children}</span>
  );
}

export function LevelBadge({ level }: { level: string | null }) {
  const tag = (level ?? 'UNKNOWN').toUpperCase();
  const color = LEVEL_COLORS[tag] ?? '#666';
  return <Badge color={color}>{tag}</Badge>;
}

export function SectionTitle({ children, sub }: { children: ReactNode; sub?: ReactNode }) {
  return (
    <div className="section-header">
      <h2 className="section-title">{children}</h2>
      {sub && <div className="section-sub">{sub}</div>}
    </div>
  );
}

/* ────────────────────────────────────────────────────────────────────────────────────────── */
/*  SVG Charts (hand-rolled, no external chart library)                                      */
/* ────────────────────────────────────────────────────────────────────────────────────────── */

/** Vertical bar chart. `data` should be ordered; bars fill available width. */
export function BarChart({ data, height = 160, label }: {
  data: Array<{ label: string; value: number; pct?: number }>;
  height?: number;
  label?: string;
}) {
  if (data.length === 0) return <EmptyState>No data</EmptyState>;
  const barGap = 6;
  const barCount = data.length;
  const barWidth = Math.max(20, Math.floor((600 - barGap * (barCount - 1)) / barCount));
  const svgWidth = barCount * (barWidth + barGap) - barGap + 80;
  const maxVal = Math.max(...data.map((d) => d.value), 1);

  return (
    <div className="chart-wrap">
      {label && <div className="chart-label">{label}</div>}
      <svg className="chart-svg" viewBox={`0 0 ${svgWidth} ${height + 40}`} width="100%" preserveAspectRatio="xMinYMin meet">
        {data.map((d, i) => {
          const barH = Math.round((d.value / maxVal) * height);
          const x = i * (barWidth + barGap) + 40;
          const y = height - barH;
          return (
            <g key={d.label}>
              <rect x={x} y={y} width={barWidth} height={barH} fill="var(--accent)" rx={3} />
              <text x={x + barWidth / 2} y={height + 14} textAnchor="middle" className="chart-x-label">
                {d.label.length > 8 ? d.label.slice(0, 8) + '…' : d.label}
              </text>
              <text x={x + barWidth / 2} y={y - 5} textAnchor="middle" className="chart-val">{d.value}</text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

/** Horizontal bar chart (ranked list). */
export function HBarChart({ data, maxItems = 10 }: {
  data: Array<{ label: string; value: number }>;
  maxItems?: number;
}) {
  const rows = data.slice(0, maxItems);
  if (rows.length === 0) return <EmptyState>No data</EmptyState>;
  const maxVal = Math.max(...rows.map((r) => r.value), 1);

  return (
    <div className="hbar-chart">
      {rows.map((row) => (
        <div key={row.label} className="hbar-row">
          <span className="hbar-label">{row.label}</span>
          <div className="hbar-track">
            <div className="hbar-fill" style={{ width: `${(row.value / maxVal) * 100}%` }} />
          </div>
          <span className="hbar-value">{row.value}</span>
        </div>
      ))}
    </div>
  );
}

/** Simple area/line chart for time-window buckets. */
export function TimeChart({ data, height = 140 }: {
  data: Array<{ label: string; value: number }>;
  height?: number;
}) {
  if (data.length === 0) return <EmptyState>No data</EmptyState>;
  const maxVal = Math.max(...data.map((d) => d.value), 1);
  const w = 600;
  const gap = w / (data.length - 1 || 1);
  const padTop = 12;
  const padBot = 28;

  const points = data.map((d, i) => {
    const x = i * gap;
    const y = padTop + (height - padTop - padBot) * (1 - d.value / maxVal);
    return `${x},${y}`;
  }).join(' ');

  const areaPoints = points + ` ${w},${height - padBot} 0,${height - padBot}`;

  return (
    <div className="chart-wrap">
      <svg className="chart-svg" viewBox={`0 0 ${w} ${height}`} width="100%" preserveAspectRatio="none">
        <polygon points={areaPoints} fill="var(--accent)" opacity="0.15" />
        <polyline points={points} fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinejoin="round" />
        {data.map((d, i) => {
          const x = i * gap;
          const y = padTop + (height - padTop - padBot) * (1 - d.value / maxVal);
          return (
            <g key={i}>
              <circle cx={x} cy={y} r={3} fill="var(--accent)" />
              <text x={x} y={height - 4} textAnchor="middle" className="chart-x-label" fontSize="9">
                {d.label.length > 6 ? d.label.slice(0, 6) + '…' : d.label}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

/** Donut chart for status distribution. */
export function DonutChart({ data, size = 120, label }: {
  data: Array<{ label: string; value: number; color: string }>;
  size?: number;
  label?: string;
}) {
  const total = data.reduce((s, d) => s + d.value, 0);
  if (total === 0) return <EmptyState>No data</EmptyState>;
  const r = (size - 12) / 2;
  const cx = size / 2;
  const cy = size / 2;
  const circumference = 2 * Math.PI * r;

  let offset = 0;

  return (
    <div className="donut-wrap">
      {label && <div className="chart-label">{label}</div>}
      <svg viewBox={`0 0 ${size} ${size}`} width={size} height={size} className="donut-svg">
        {data.map((d) => {
          const pct = d.value / total;
          const dashLen = circumference * pct;
          const dashOff = -circumference * offset;
          offset += pct;
          return (
            <circle
              key={d.label}
              cx={cx}
              cy={cy}
              r={r}
              fill="none"
              stroke={d.color}
              strokeWidth={10}
              strokeDasharray={`${dashLen} ${circumference - dashLen}`}
              strokeDashoffset={dashOff}
            />
          );
        })}
        <text x={cx} y={cy + 4} textAnchor="middle" className="donut-total" fontSize="16">{total}</text>
      </svg>
      <div className="donut-legend">
        {data.map((d) => (
          <span key={d.label} className="legend-item">
            <span className="legend-dot" style={{ background: d.color }} />
            {d.label} ({d.value})
          </span>
        ))}
      </div>
    </div>
  );
}

/** Dependency graph visualisation (force-directed layout with node circles + weighted edges). */
export function DependencyGraph({ nodes, edges }: {
  nodes: Array<{ id: string; events: number }>;
  edges: Array<{ source: string; target: string; weight: number }>;
}) {
  if (nodes.length === 0) return <EmptyState>No nodes</EmptyState>;

  const size = Math.max(280, nodes.length * 50);
  const cx = size / 2;
  const cy = size / 2;
  const r = size * 0.36;

  const pos = new Map<string, { x: number; y: number }>();
  nodes.forEach((n, i) => {
    const angle = (2 * Math.PI * i) / nodes.length - Math.PI / 2;
    pos.set(n.id, { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) });
  });

  const maxEvents = Math.max(...nodes.map((n) => n.events), 1);
  const maxWeight = Math.max(...edges.map((e) => e.weight), 1);

  return (
    <svg className="graph-svg" viewBox={`0 0 ${size} ${size + 20}`} width="100%" preserveAspectRatio="xMidYMid meet">
      {edges.map((e, i) => {
        const a = pos.get(e.source);
        const b = pos.get(e.target);
        if (!a || !b) return null;
        const opacity = 0.25 + (e.weight / maxWeight) * 0.75;
        const sw = 1 + (e.weight / maxWeight) * 3;
        const mx = (a.x + b.x) / 2;
        const my = (a.y + b.y) / 2 - 10;
        return (
          <g key={i}>
            <path
              d={`M${a.x},${a.y} Q${mx},${my} ${b.x},${b.y}`}
              fill="none"
              stroke="var(--text-muted)"
              strokeWidth={sw}
              opacity={opacity}
              markerEnd="url(#arrow)"
            />
            <text x={mx} y={my - 4} textAnchor="middle" className="edge-label" fontSize="10" opacity={opacity}>{e.weight}</text>
          </g>
        );
      })}
      <defs>
        <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth={6} markerHeight={6} orient="auto-start-reverse">
          <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--text-muted)" />
        </marker>
      </defs>
      {nodes.map((n) => {
        const p = pos.get(n.id)!;
        const nr = 12 + (n.events / maxEvents) * 12;
        return (
          <g key={n.id}>
            <circle cx={p.x} cy={p.y} r={nr} fill="var(--bg)" stroke="var(--accent)" strokeWidth={2} />
            <text x={p.x} y={p.y + 4} textAnchor="middle" className="node-label" fontSize="11">{n.id}</text>
            <text x={p.x} y={p.y + nr + 14} textAnchor="middle" className="node-count" fontSize="9">{n.events}</text>
          </g>
        );
      })}
    </svg>
  );
}