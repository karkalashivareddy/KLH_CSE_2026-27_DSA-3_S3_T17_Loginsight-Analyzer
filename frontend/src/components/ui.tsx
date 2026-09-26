import { isValidElement, useEffect, useId, useRef, type CSSProperties, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, Database, ExternalLink, Inbox, X } from 'lucide-react';
import { LEVEL_COLORS, formatMillis, formatNumber, formatTs } from './format';
import type { LogEvent } from '../api/types';

export function Card({ title, sub, children, className = '', actions }: {
  title?: string;
  sub?: ReactNode;
  children: ReactNode;
  className?: string;
  actions?: ReactNode;
}) {
  const titleId = useId();
  return (
    <section className={`card ${className}`} aria-labelledby={title ? titleId : undefined}>
      {(title || sub || actions) && (
        <div className="card-header">
          <div className="card-title-block">
            {title && <h2 className="card-title" id={titleId}>{title}</h2>}
            {sub && <div className="card-sub">{sub}</div>}
          </div>
          {actions && <div className="card-actions">{actions}</div>}
        </div>
      )}
      <div className="card-body">{children}</div>
    </section>
  );
}

export function PageHeader({ title, description, actions, eyebrow }: {
  title: string;
  description?: ReactNode;
  actions?: ReactNode;
  eyebrow?: string;
}) {
  return (
    <div className="page-header">
      <div>
        {eyebrow && <div className="eyebrow">{eyebrow}</div>}
        <h1 className="page-title">{title}</h1>
        {description && <p className="page-description">{description}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}

export function StatCard({ label, value, sub, color }: {
  label: string;
  value: ReactNode;
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
    <div className="spinner-wrap" role="status" aria-live="polite">
      <div className="spinner" aria-hidden="true" />
      <span className="spinner-label">{label}</span>
    </div>
  );
}

export function ErrorBox({ error, retry }: { error: Error; retry?: () => void }) {
  const details = (error as Error & { apiError?: { status?: number; path?: string } }).apiError;
  return (
    <div className="error-box" role="alert">
      <div className="error-title"><AlertCircle size={15} aria-hidden="true" /> Request error</div>
      <div className="error-msg">{error.message}</div>
      {details && <div className="error-meta">{details.status ? `HTTP ${details.status}` : 'Request failed'}{details.path ? ` · ${details.path}` : ''}</div>}
      {retry && <button className="btn btn-sm" type="button" onClick={retry}>Retry</button>}
    </div>
  );
}

export function EmptyState({ children, icon = true }: { children: ReactNode; icon?: boolean }) {
  return (
    <div className="empty-state">
      {icon && <Inbox size={22} aria-hidden="true" />}
      {children}
    </div>
  );
}

export function NoDatasetState({ detail = 'Choose a source before requesting dataset-backed investigation data.' }: { detail?: string }) {
  return (
    <EmptyState>
      <Database size={23} aria-hidden="true" />
      <strong>No dataset loaded.</strong>
      <span>{detail}</span>
      <Link className="btn btn-sm" to="/datasets">Open datasets</Link>
    </EmptyState>
  );
}

function nodeText(value: ReactNode): string {
  if (typeof value === 'string' || typeof value === 'number') return String(value);
  if (Array.isArray(value)) return value.map(nodeText).join('');
  if (isValidElement(value)) return nodeText(value.props.children as ReactNode);
  return '';
}

export function Badge({ children, color, tone, label }: { children: ReactNode; color?: string; tone?: 'neutral' | 'good' | 'warn' | 'danger' | 'info'; label?: string }) {
  const style: CSSProperties | undefined = color ? { backgroundColor: color, borderColor: color, color: '#101416' } : undefined;
  const accessibleLabel = label ?? nodeText(children) ?? '';
  return <span className={`badge${tone ? ` badge--${tone}` : ''}`} style={style} role="img" aria-label={accessibleLabel || undefined}>{children}</span>;
}

export function LevelBadge({ level }: { level: string | null }) {
  const tag = (level ?? 'UNKNOWN').toUpperCase();
  return <Badge color={LEVEL_COLORS[tag] ?? 'var(--severity-unknown)'} label={`Severity ${tag}`}>{tag}</Badge>;
}

export function StatusPill({ status }: { status: string | undefined }) {
  const normalized = (status ?? 'UNKNOWN').toUpperCase();
  const tone = normalized === 'UP' || normalized === 'COMPLETED' || normalized === 'COMPLETE' || normalized === 'ENABLED' || normalized === 'RESOLVED'
    ? 'good'
    : normalized === 'FAILED' || normalized === 'ERROR' || normalized === 'ACTIVE' || normalized === 'OPEN'
      ? 'danger'
      : normalized === 'RUNNING' || normalized === 'STREAMING' || normalized === 'CONNECTING' || normalized === 'QUEUED' || normalized === 'PARTIAL'
        ? normalized === 'PARTIAL' ? 'warn' : 'info'
        : normalized === 'DEGRADED'
          ? 'warn'
          : 'neutral';
  return <span className={`status-pill status-pill--${tone}`} role="status" aria-live="polite" aria-label={`Status ${normalized}`}><span className="status-pill-dot" aria-hidden="true" />{normalized}</span>;
}

export function SectionTitle({ children, sub }: { children: ReactNode; sub?: ReactNode }) {
  return (
    <div className="section-header">
      <h2 className="section-title">{children}</h2>
      {sub && <div className="section-sub">{sub}</div>}
    </div>
  );
}

export function EventDrawer({ event, onClose }: { event: LogEvent | null; onClose: () => void }) {
  const drawerRef = useRef<HTMLElement>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  const onCloseRef = useRef(onClose);

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    if (!event || typeof document === 'undefined') return;
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    document.body.classList.add('drawer-open');
    closeRef.current?.focus();
    const onKeyDown = (keyboardEvent: KeyboardEvent) => {
      if (keyboardEvent.key === 'Escape') {
        keyboardEvent.preventDefault();
        onCloseRef.current();
        return;
      }
      if (keyboardEvent.key !== 'Tab' || !drawerRef.current) return;
      const focusable = [...drawerRef.current.querySelectorAll<HTMLElement>('a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')];
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const activeElement = document.activeElement;
      if (!(activeElement instanceof HTMLElement) || !drawerRef.current.contains(activeElement)) {
        keyboardEvent.preventDefault();
        (keyboardEvent.shiftKey ? last : first).focus();
        return;
      }
      if (keyboardEvent.shiftKey && activeElement === first) {
        keyboardEvent.preventDefault();
        last.focus();
      } else if (!keyboardEvent.shiftKey && activeElement === last) {
        keyboardEvent.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', onKeyDown, true);
    return () => {
      document.removeEventListener('keydown', onKeyDown, true);
      document.body.style.overflow = previousOverflow;
      document.body.classList.remove('drawer-open');
      previousFocus?.focus();
    };
  }, [event]);

  if (!event) return null;
  return (
    <div className="drawer-overlay" role="presentation" onMouseDown={(mouseEvent) => mouseEvent.target === mouseEvent.currentTarget && onClose()}>
      <aside ref={drawerRef} className="event-drawer" role="dialog" aria-modal="true" aria-labelledby="event-drawer-title" aria-describedby="event-drawer-summary">
        <div className="drawer-header">
          <div>
            <div className="eyebrow">Event detail</div>
            <h2 id="event-drawer-title">#{event.id} · {event.service}</h2>
          </div>
          <button ref={closeRef} className="icon-btn" type="button" onClick={onClose} aria-label="Close event detail"><X size={18} aria-hidden="true" /></button>
        </div>
        <div className="drawer-body">
          <div className="drawer-summary" id="event-drawer-summary">
            <LevelBadge level={event.level} />
            <span>{formatTs(event.timestamp)}</span>
            <span>{event.host}</span>
          </div>
          <div className="drawer-message">{event.message}</div>
          <table className="info-table">
            <caption className="sr-only">Event fields</caption>
            <tbody>
              <tr><th>Timestamp</th><td>{formatTs(event.timestamp)}</td></tr>
              <tr><th>Service</th><td>{event.service}</td></tr>
              <tr><th>Host</th><td>{event.host}</td></tr>
              <tr><th>IP address</th><td>{event.ipAddress}</td></tr>
              <tr><th>Request ID</th><td>{event.requestId || '—'}</td></tr>
              <tr><th>User ID</th><td>{event.userId || '—'}</td></tr>
              {event.httpMethod && <tr><th>HTTP</th><td>{event.httpMethod} {event.endpoint} {event.statusCode || ''}</td></tr>}
              {event.responseTime > 0 && <tr><th>Response time</th><td>{formatMillis(event.responseTime)}</td></tr>}
              {event.traceId && <tr><th>Trace ID</th><td><code>{event.traceId}</code></td></tr>}
              {event.spanId && <tr><th>Span ID</th><td><code>{event.spanId}</code></td></tr>}
            </tbody>
          </table>
          {event.rawMessage && <pre className="raw-message">{event.rawMessage}</pre>}
          {Object.keys(event.attributes ?? {}).length > 0 && (
            <table className="info-table">
              <caption className="sr-only">Event attributes</caption>
              <thead><tr><th>Attribute</th><th>Value</th></tr></thead>
              <tbody>{Object.entries(event.attributes).map(([key, value]) => <tr key={key}><td>{key}</td><td>{value}</td></tr>)}</tbody>
            </table>
          )}
        </div>
        <div className="drawer-footer">
          <Link className="btn btn-sm" to={`/logs/${event.id}`} onClick={onClose}>Open full detail <ExternalLink size={14} aria-hidden="true" /></Link>
          <span className="text-muted">Event ID {event.id}</span>
        </div>
      </aside>
    </div>
  );
}

export function HeatmapGrid({ days, columns, cells }: { days: string[]; columns: number; cells: number[][] }) {
  const legendId = useId();
  const max = Math.max(1, ...cells.flat());
  return (
    <div className="heatmap-wrap">
      <div className="heatmap" role="grid" aria-label="Activity heatmap by weekday and UTC hour" aria-describedby={legendId}>
        {days.map((day, row) => (
          <div className="heatmap-row" key={day} role="row" aria-rowindex={row + 1}>
            <span className="heatmap-day" role="rowheader" aria-colindex={1} aria-label={`Row ${day}`}>{day}</span>
            <div className="heatmap-cells">
              {(cells[row] ?? []).slice(0, columns).map((value, col) => {
                const alpha = value === 0 ? 0.04 : 0.16 + (value / max) * 0.84;
                return <span key={col} className="heatmap-cell" style={{ backgroundColor: `rgba(83, 169, 255, ${alpha})` }} title={`${day} ${String(col).padStart(2, '0')}:00 UTC — ${formatNumber(value)} events`} role="gridcell" aria-colindex={col + 2} aria-label={`${day} ${col}:00 UTC, ${value} events`} />;
              })}
            </div>
          </div>
        ))}
      </div>
      <div className="heatmap-legend" id={legendId}><span>UTC hour</span><span>0</span><span className="heatmap-legend-scale" aria-hidden="true" /><span>{Math.max(0, columns - 1)}</span></div>
    </div>
  );
}

export function BarChart({ data, height = 160, label }: { data: Array<{ label: string; value: number; pct?: number }>; height?: number; label?: string }) {
  if (data.length === 0) return <EmptyState>No data</EmptyState>;
  const barGap = 8;
  const barCount = data.length;
  const barWidth = Math.max(20, Math.floor((600 - barGap * (barCount - 1)) / barCount));
  const svgWidth = barCount * (barWidth + barGap) - barGap + 80;
  const maxVal = Math.max(...data.map((entry) => entry.value), 1);
  return (
    <div className="chart-wrap">
      {label && <div className="chart-label">{label}</div>}
      <svg className="chart-svg" viewBox={`0 0 ${svgWidth} ${height + 40}`} width="100%" preserveAspectRatio="xMinYMin meet" role="img" aria-label={label ?? 'Bar chart'}>
        {data.map((entry, index) => {
          const barHeight = Math.round((entry.value / maxVal) * height);
          const x = index * (barWidth + barGap) + 40;
          const y = height - barHeight;
          return (
            <g key={`${entry.label}-${index}`}>
              <rect x={x} y={y} width={barWidth} height={barHeight} fill="var(--accent)" rx="2" />
              <text x={x + barWidth / 2} y={height + 15} textAnchor="middle" className="chart-x-label">{entry.label.length > 10 ? `${entry.label.slice(0, 10)}…` : entry.label}</text>
              <text x={x + barWidth / 2} y={Math.max(10, y - 6)} textAnchor="middle" className="chart-val">{formatNumber(entry.value)}</text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

export function HBarChart({ data, maxItems = 10, label = 'Horizontal bar chart' }: { data: Array<{ label: string; value: number }>; maxItems?: number; label?: string }) {
  const rows = data.slice(0, maxItems);
  if (rows.length === 0) return <EmptyState>No data</EmptyState>;
  const maxVal = Math.max(...rows.map((row) => row.value), 1);
  return (
    <div className="hbar-chart" role="list" aria-label={label}>
      {rows.map((row) => (
        <div className="hbar-row" key={row.label} role="listitem" aria-label={`${row.label}: ${formatNumber(row.value)}`}>
          <span className="hbar-label" title={row.label}>{row.label}</span>
          <div className="hbar-track" aria-hidden="true"><div className="hbar-fill" style={{ width: `${(row.value / maxVal) * 100}%` }} /></div>
          <span className="hbar-value">{formatNumber(row.value)}</span>
        </div>
      ))}
    </div>
  );
}

export function TimeChart({ data, height = 140, label = 'Time series chart', valueLabel = 'events' }: { data: Array<{ label: string; value: number }>; height?: number; label?: string; valueLabel?: string }) {
  const chartId = useId();
  if (data.length === 0) return <EmptyState>No data</EmptyState>;
  const safeData = data.map((entry) => ({ ...entry, value: Number.isFinite(entry.value) ? Math.max(0, entry.value) : 0 }));
  const maxVal = Math.max(...safeData.map((entry) => entry.value), 1);
  const width = 640;
  const gap = safeData.length === 1 ? 0 : width / (safeData.length - 1);
  const padTop = 18;
  const padBottom = 34;
  const padLeft = 38;
  const chartWidth = width - padLeft;
  const chartHeight = Math.max(40, height - padTop - padBottom);
  const baseline = padTop + chartHeight;
  const svgHeight = Math.max(height, baseline + 32);
  const points = safeData.map((entry, index) => `${padLeft + index * gap},${padTop + chartHeight * (1 - entry.value / maxVal)}`).join(' ');
  const areaPoints = `${padLeft},${baseline} ${points} ${padLeft + chartWidth},${baseline}`;
  const labelIndexes = [...new Set([0, Math.floor((safeData.length - 1) / 2), safeData.length - 1])];
  const displayLabel = (value: string) => value.length > 18 ? `${value.slice(0, 17)}…` : value;
  return (
    <div className="chart-wrap">
      <svg className="chart-svg time-chart-svg" viewBox={`0 0 ${width} ${svgHeight}`} width="100%" preserveAspectRatio="xMidYMid meet" role="img" aria-label={label}>
        <title id={`${chartId}-title`}>{label}</title>
        <desc id={`${chartId}-description`}>{safeData.length} observed buckets. Maximum {formatNumber(maxVal)} {valueLabel}.</desc>
        <line x1={padLeft} x2={width} y1={padTop} y2={padTop} className="chart-grid-line" />
        <line x1={padLeft} x2={width} y1={baseline} y2={baseline} className="chart-grid-line" />
        <text x={padLeft - 8} y={padTop + 4} textAnchor="end" className="chart-y-label">{formatNumber(maxVal)}</text>
        <text x={padLeft - 8} y={baseline + 4} textAnchor="end" className="chart-y-label">0</text>
        <polygon points={areaPoints} fill="var(--accent)" opacity="0.12" />
        <polyline points={points} fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" />
        {safeData.map((entry, index) => {
          const x = padLeft + index * gap;
          const y = padTop + chartHeight * (1 - entry.value / maxVal);
          return <circle key={`${entry.label}-${index}`} cx={x} cy={y} r="3" fill="var(--accent)" aria-label={`${entry.label}: ${formatNumber(entry.value)} ${valueLabel}`} />;
        })}
        {labelIndexes.map((index) => {
          const x = padLeft + index * gap;
          const anchor = index === 0 ? 'start' : index === safeData.length - 1 ? 'end' : 'middle';
          return <text key={`label-${index}`} x={x} y={svgHeight - 7} textAnchor={anchor} className="chart-x-label">{displayLabel(safeData[index].label)}</text>;
        })}
      </svg>
    </div>
  );
}

export function DonutChart({ data, size = 120, label }: { data: Array<{ label: string; value: number; color: string }>; size?: number; label?: string }) {
  const total = data.reduce((sum, entry) => sum + entry.value, 0);
  if (total === 0) return <EmptyState>No data</EmptyState>;
  const radius = (size - 12) / 2;
  const center = size / 2;
  const circumference = 2 * Math.PI * radius;
  let offset = 0;
  return (
    <div className="donut-wrap">
      {label && <div className="chart-label">{label}</div>}
      <svg viewBox={`0 0 ${size} ${size}`} width={size} height={size} className="donut-svg" role="img" aria-label={label ?? 'Distribution chart'}>
        {data.map((entry) => {
          const portion = entry.value / total;
          const dashLength = circumference * portion;
          const dashOffset = -circumference * offset;
          offset += portion;
          return <circle key={entry.label} cx={center} cy={center} r={radius} fill="none" stroke={entry.color} strokeWidth="10" strokeDasharray={`${dashLength} ${circumference - dashLength}`} strokeDashoffset={dashOffset} />;
        })}
        <text x={center} y={center + 5} textAnchor="middle" className="donut-total">{formatNumber(total)}</text>
      </svg>
      <div className="donut-legend">{data.map((entry) => <span className="legend-item" key={entry.label}><span className="legend-dot" style={{ backgroundColor: entry.color }} />{entry.label} ({formatNumber(entry.value)})</span>)}</div>
    </div>
  );
}

export function DependencyGraph({ nodes, edges }: { nodes: Array<{ id: string; events: number }>; edges: Array<{ source: string; target: string; weight: number }> }) {
  const markerId = `arrow-${useId().replace(/:/g, '')}`;
  if (nodes.length === 0) return <EmptyState>No nodes</EmptyState>;
  const size = Math.max(320, nodes.length * 52);
  const center = size / 2;
  const radius = size * 0.36;
  const positions = new Map<string, { x: number; y: number }>();
  nodes.forEach((node, index) => {
    const angle = (2 * Math.PI * index) / nodes.length - Math.PI / 2;
    positions.set(node.id, { x: center + radius * Math.cos(angle), y: center + radius * Math.sin(angle) });
  });
  const maxEvents = Math.max(...nodes.map((node) => node.events), 1);
  const maxWeight = Math.max(...edges.map((edge) => edge.weight), 1);
  return (
    <svg className="graph-svg" viewBox={`0 0 ${size} ${size + 20}`} width="100%" preserveAspectRatio="xMidYMid meet" role="img" aria-label="Service dependency graph">
      <defs><marker id={markerId} viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="var(--text-muted)" /></marker></defs>
      {edges.map((edge, index) => {
        const from = positions.get(edge.source);
        const to = positions.get(edge.target);
        if (!from || !to) return null;
        const opacity = 0.25 + (edge.weight / maxWeight) * 0.75;
        const midX = (from.x + to.x) / 2;
        const midY = (from.y + to.y) / 2 - 10;
        return <g key={`${edge.source}-${edge.target}-${index}`}><path d={`M${from.x},${from.y} Q${midX},${midY} ${to.x},${to.y}`} fill="none" stroke="var(--text-muted)" strokeWidth={1 + (edge.weight / maxWeight) * 3} opacity={opacity} markerEnd={`url(#${markerId})`} /><text x={midX} y={midY - 4} textAnchor="middle" className="edge-label" fontSize="10" opacity={opacity}>{edge.weight}</text></g>;
      })}
      {nodes.map((node) => {
        const point = positions.get(node.id);
        if (!point) return null;
        const nodeRadius = 12 + (node.events / maxEvents) * 12;
        return <g key={node.id}><circle cx={point.x} cy={point.y} r={nodeRadius} fill="var(--bg-elevated)" stroke="var(--accent)" strokeWidth="2" /><text x={point.x} y={point.y + 4} textAnchor="middle" className="node-label" fontSize="11">{node.id}</text><text x={point.x} y={point.y + nodeRadius + 14} textAnchor="middle" className="node-count" fontSize="9">{formatNumber(node.events)}</text></g>;
      })}
    </svg>
  );
}
