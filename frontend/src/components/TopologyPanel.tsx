import { useCallback, useEffect, useId, useMemo, useState, type KeyboardEvent } from 'react';
import { formatNumber } from './format';

export type TopologyMode = '2d' | '3d' | 'depth';

export interface TopologyNode {
  id: string;
  events: number;
  errorRate?: number;
  health?: 'healthy' | 'watch' | 'elevated' | 'unknown';
}

export interface TopologyEdge {
  source: string;
  target: string;
  weight: number;
}

export interface TopologyPanelProps {
  nodes: TopologyNode[];
  edges: TopologyEdge[];
  title?: string;
  description?: string;
  mode?: TopologyMode;
  defaultMode?: TopologyMode;
  onModeChange?: (mode: TopologyMode) => void;
  selectedId?: string | null;
  onSelect?: (id: string | null) => void;
}

const WIDTH = 760;
const HEIGHT = 440;
const PADDING = 76;

type Point = { x: number; y: number };

function normalizeMode(mode: TopologyMode | undefined): '2d' | '3d' {
  return mode === '3d' || mode === 'depth' ? '3d' : '2d';
}

function usePrefersReducedMotion(): boolean {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return;
    const query = window.matchMedia('(prefers-reduced-motion: reduce)');
    const update = () => setReduced(query.matches);
    update();
    if (typeof query.addEventListener === 'function') {
      query.addEventListener('change', update);
      return () => query.removeEventListener('change', update);
    }
    query.addListener(update);
    return () => query.removeListener(update);
  }, []);
  return reduced;
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

function pointOnPath(from: Point, control: Point, to: Point, progress: number): Point {
  const inverse = 1 - progress;
  return {
    x: inverse * inverse * from.x + 2 * inverse * progress * control.x + progress * progress * to.x,
    y: inverse * inverse * from.y + 2 * inverse * progress * control.y + progress * progress * to.y
  };
}

function shortLabel(value: string, length = 14): string {
  return value.length > length ? `${value.slice(0, length - 1)}…` : value;
}

function healthBand(node: TopologyNode): 'healthy' | 'watch' | 'elevated' | 'unknown' {
  return node.health ?? 'unknown';
}

function healthText(node: TopologyNode): string {
  const health = healthBand(node);
  if (health === 'healthy') return 'healthy';
  if (health === 'watch') return 'watch';
  if (health === 'elevated') return 'elevated';
  return 'health unavailable';
}

function rateText(node: TopologyNode): string {
  if (node.errorRate === undefined || !Number.isFinite(node.errorRate)) return 'window error rate unavailable';
  return `${node.errorRate.toFixed(1)}% window error rate`;
}

function shortRateText(node: TopologyNode): string {
  if (node.errorRate === undefined || !Number.isFinite(node.errorRate)) return 'rate unavailable';
  return `${node.errorRate.toFixed(1)}% window`;
}

export function TopologyPanel({ nodes, edges, title = 'Observed service topology', description = 'Node size follows dataset-wide events; node health and error rate follow the selected window. Edge weight follows observed request-trail adjacency.', mode, defaultMode = '2d', onModeChange, selectedId, onSelect }: TopologyPanelProps) {
  const titleId = useId();
  const descriptionId = useId();
  const markerId = useId().replace(/:/g, '');
  const reducedMotion = usePrefersReducedMotion();
  const [internalMode, setInternalMode] = useState<TopologyMode>(defaultMode);
  const [internalSelected, setInternalSelected] = useState<string | null>(selectedId ?? null);
  const [focusedId, setFocusedId] = useState<string | null>(null);
  const activeMode = normalizeMode(mode ?? internalMode);
  const activeSelected = selectedId === undefined ? internalSelected : selectedId;
  const maxEvents = Math.max(1, ...nodes.map((node) => Math.max(0, node.events)));
  const maxWeight = Math.max(1, ...edges.map((edge) => Math.max(0, edge.weight)));

  const positions = useMemo(() => {
    const map = new Map<string, Point>();
    const count = nodes.length;
    const centerX = WIDTH / 2;
    const centerY = HEIGHT / 2;
    const radiusX = WIDTH / 2 - PADDING;
    const radiusY = HEIGHT / 2 - PADDING;
    nodes.forEach((node, index) => {
      if (count === 1) {
        map.set(node.id, { x: centerX, y: centerY });
        return;
      }
      const angle = (Math.PI * 2 * index) / count - Math.PI / 2;
      map.set(node.id, {
        x: centerX + Math.cos(angle) * radiusX,
        y: centerY + Math.sin(angle) * radiusY
      });
    });
    return map;
  }, [nodes]);

  const select = useCallback((id: string | null) => {
    if (selectedId === undefined) setInternalSelected(id);
    onSelect?.(id);
  }, [onSelect, selectedId]);

  const changeMode = (next: TopologyMode) => {
    if (mode === undefined) setInternalMode(next);
    onModeChange?.(next);
  };

  const focusSelected = () => {
    if (activeSelected) setFocusedId(activeSelected);
  };

  const resetView = () => {
    setFocusedId(null);
    select(null);
  };

  const focusPoint = positions.get(focusedId ?? activeSelected ?? '');
  const viewWidth = focusPoint ? Math.min(WIDTH, 420) : WIDTH;
  const viewHeight = focusPoint ? Math.min(HEIGHT, 280) : HEIGHT;
  const viewX = focusPoint ? clamp(focusPoint.x - viewWidth / 2, 0, WIDTH - viewWidth) : 0;
  const viewY = focusPoint ? clamp(focusPoint.y - viewHeight / 2, 0, HEIGHT - viewHeight) : 0;
  const viewBox = `${viewX} ${viewY} ${viewWidth} ${viewHeight}`;
  const stageClass = `topology-stage${activeMode === '3d' ? ' topology-stage--depth' : ''}${reducedMotion ? ' topology-stage--reduced-motion' : ''}`;

  return (
    <section className="topology-panel" aria-label={title} aria-describedby={descriptionId}>
      <div className="topology-toolbar" role="group" aria-label="Topology controls">
        <div className="topology-mode-switch" role="group" aria-label="Topology display mode">
          <button className={`btn btn-sm${activeMode === '2d' ? ' btn-primary' : ''}`} type="button" aria-pressed={activeMode === '2d'} onClick={() => changeMode('2d')}>2D</button>
          <button className={`btn btn-sm${activeMode === '3d' ? ' btn-primary' : ''}`} type="button" aria-pressed={activeMode === '3d'} onClick={() => changeMode('3d')}>3D / depth</button>
        </div>
        <div className="topology-actions">
          <button className="btn btn-sm" type="button" onClick={focusSelected} disabled={!activeSelected}>Focus selected</button>
          <button className="btn btn-sm" type="button" onClick={resetView}>Reset view</button>
        </div>
      </div>
      <p id={descriptionId} className="sr-only">{description} Edge thickness and particles encode observed request-trail weight, not verified infrastructure.</p>
      <div className={stageClass} data-mode={activeMode} data-reduced-motion={reducedMotion ? 'true' : 'false'}>
        <div className="topology-renderer-note">{activeMode === '3d' ? '2.5D / SVG perspective · not WebGL' : '2D SVG renderer · not WebGL'}</div>
        {nodes.length === 0 ? <div className="topology-empty" role="status">No observed service nodes were returned.</div> : <>
          <svg className="topology-svg" viewBox={viewBox} role="img" aria-labelledby={`${titleId}-visual ${descriptionId}-visual`} focusable="false">
            <title id={`${titleId}-visual`}>{title}</title>
            <desc id={`${descriptionId}-visual`}>{description} Selectable service nodes. Edge thickness and particles encode observed request-trail weight, not verified infrastructure.</desc>
            <defs>
              <marker id={markerId} viewBox="0 0 10 10" refX="9" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
                <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--text-muted)" />
              </marker>
            </defs>
            <g className="topology-edges" aria-hidden="true">
              {edges.map((edge, index) => {
                const from = positions.get(edge.source);
                const to = positions.get(edge.target);
                if (!from || !to) return null;
                const weight = Math.max(0, edge.weight);
                const normalized = weight / maxWeight;
                const control = {
                  x: (from.x + to.x) / 2,
                  y: (from.y + to.y) / 2 - Math.min(46, 12 + normalized * 28)
                };
                const width = 1 + normalized * 4;
                const particles = weight > 0 ? Math.min(8, Math.max(1, Math.ceil(normalized * 7))) : 0;
                const pathData = `M ${from.x} ${from.y} Q ${control.x} ${control.y} ${to.x} ${to.y}`;
                const duration = Math.max(1.2, 3.2 - normalized * 1.8);
                return (
                  <g key={`${edge.source}-${edge.target}-${index}`} data-edge-weight={weight} data-particle-count={particles}>
                    <path d={pathData} fill="none" stroke="var(--text-muted)" strokeWidth={width} strokeLinecap="round" opacity={0.3 + normalized * 0.55} markerEnd={`url(#${markerId})`} />
                    {Array.from({ length: particles }, (_, particleIndex) => {
                      const point = pointOnPath(from, control, to, (particleIndex + 1) / (particles + 1));
                      return <circle key={particleIndex} className={`topology-particle${reducedMotion ? ' topology-particle--static' : ''}`} cx={point.x} cy={point.y} r={1.5 + normalized * 1.5} fill="var(--accent-strong)">
                        {!reducedMotion && <animateMotion path={pathData} dur={`${duration}s`} begin={`${(particleIndex * duration) / Math.max(particles, 1)}s`} repeatCount="indefinite" />}
                      </circle>;
                    })}
                  </g>
                );
              })}
            </g>
            <g className="topology-nodes">
              {nodes.map((node) => {
                const point = positions.get(node.id);
                if (!point) return null;
                const normalized = Math.max(0, node.events) / maxEvents;
                const radius = 12 + Math.sqrt(normalized) * 22;
                const selected = activeSelected === node.id;
                const health = healthBand(node);
                const handleKey = (event: KeyboardEvent<SVGGElement>) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    select(node.id);
                  }
                };
                return (
                  <g key={node.id} className={`topology-node topology-node--${health}${selected ? ' topology-node--selected' : ''}`} role="button" tabIndex={0} aria-pressed={selected} aria-label={`${node.id}, ${formatNumber(node.events)} dataset events, ${rateText(node)}, ${healthText(node)}`} onClick={() => select(node.id)} onKeyDown={handleKey} data-node-id={node.id} data-events={node.events} data-health={health} data-radius={radius}>
                    <circle cx={point.x} cy={point.y} r={radius} fill="var(--bg-elevated)" stroke={selected ? 'var(--accent-strong)' : 'var(--accent)'} strokeWidth={selected ? 3 : 2} />
                    <circle className="topology-health-dot" cx={point.x - radius + 7} cy={point.y - radius + 8} r={3.5} fill={`var(--${health === 'unknown' ? 'text-muted' : health === 'healthy' ? 'ok' : health === 'watch' ? 'warn' : 'danger'})`} />
                    <text x={point.x} y={point.y + 4} textAnchor="middle" className="node-label">{shortLabel(node.id)}</text>
                    <text x={point.x} y={point.y + radius + 15} textAnchor="middle" className="node-count">{formatNumber(node.events)} dataset · {shortRateText(node)}</text>
                  </g>
                );
              })}
            </g>
          </svg>
        </>}
      </div>
      <div className="topology-accessible-list">
        <div className="topology-list-heading">Accessible service list · dataset events and selected-window error rate</div>
        <ul aria-label="Accessible service list">
          {nodes.map((node) => <li key={node.id}><button type="button" className={`topology-service-button${activeSelected === node.id ? ' topology-service-button--selected' : ''}`} aria-label={`${node.id}, ${formatNumber(node.events)} dataset events, ${rateText(node)}, ${healthText(node)}`} aria-pressed={activeSelected === node.id} onClick={() => select(node.id)}><span>{node.id}</span><span>{formatNumber(node.events)} dataset events</span><span>{rateText(node)}</span><span>{healthText(node)}</span></button></li>)}
        </ul>
        {edges.length > 0 && <><div className="topology-list-heading topology-list-heading--edges">Observed request-trail adjacency</div><ul aria-label="Observed request-trail adjacency edges">{edges.map((edge, index) => <li className="topology-edge-item" key={`${edge.source}-${edge.target}-${index}`}>{edge.source} → {edge.target} <span>{formatNumber(edge.weight)} observed weight</span></li>)}</ul></>}
      </div>
    </section>
  );
}

export default TopologyPanel;
