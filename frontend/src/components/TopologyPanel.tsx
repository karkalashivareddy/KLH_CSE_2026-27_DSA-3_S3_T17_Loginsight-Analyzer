import { lazy, Suspense, useCallback, useEffect, useId, useMemo, useRef, useState, type KeyboardEvent, type PointerEvent, type WheelEvent } from 'react';
import { formatNumber } from './format';

const Topology3D = lazy(() => import('./Topology3D'));

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
  graphKind?: 'observed' | 'declared';
  incidentServiceIds?: string[];
  incidentLabel?: string;
}

const WIDTH = 760;
const HEIGHT = 440;
const PADDING = 76;
const DEFAULT_VISIBLE_EDGE_LIMIT = 18;
const MAX_GRAPH_NODES = 250;
const MAX_GRAPH_EDGES = 2_500;
const MAX_3D_EDGE_COUNT = 500;
const MIN_VIEW_WIDTH = 230;
const MIN_VIEW_HEIGHT = 135;

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

export function TopologyPanel({ nodes, edges, graphKind = 'observed', title = graphKind === 'declared' ? 'Declared service topology' : 'Observed service topology', description = graphKind === 'declared' ? 'Node size follows simulated events in the selected window; node health and error rate follow the selected window. Edge weight follows the declared scenario dependency graph.' : 'Node size follows dataset-wide events; node health and error rate follow the selected window. Edge weight follows observed request-trail adjacency.', mode, defaultMode = '2d', onModeChange, selectedId, onSelect, incidentServiceIds = [], incidentLabel }: TopologyPanelProps) {
  const titleId = useId();
  const descriptionId = useId();
  const markerId = useId().replace(/:/g, '');
  const stageRef = useRef<HTMLDivElement>(null);
  const reducedMotion = usePrefersReducedMotion();
  const [internalMode, setInternalMode] = useState<TopologyMode>(defaultMode);
  const [internalSelected, setInternalSelected] = useState<string | null>(selectedId ?? null);
  const [showAllEdges, setShowAllEdges] = useState(false);
  const [focusToken, setFocusToken] = useState(0);
  const [fitToken, setFitToken] = useState(0);
  const [resetToken, setResetToken] = useState(0);
  const [viewport, setViewport] = useState({ x: 0, y: 0, width: WIDTH, height: HEIGHT });
  const panStart = useRef<{ pointerId: number; clientX: number; clientY: number; x: number; y: number } | null>(null);
  const activeMode = normalizeMode(mode ?? internalMode);
  const activeSelected = selectedId === undefined ? internalSelected : selectedId;
  const graphNodeResult = useMemo(() => {
    const unique = new Map<string, TopologyNode>();
    for (const node of nodes) {
      if (!node || typeof node.id !== 'string' || !node.id.trim() || !Number.isFinite(node.events)) continue;
      if (unique.has(node.id)) continue;
      if (unique.size >= MAX_GRAPH_NODES) break;
      unique.set(node.id, { ...node, events: Math.max(0, node.events) });
    }
    const items = [...unique.values()];
    let omittedRecords = 0;
    if (items.length >= MAX_GRAPH_NODES) {
      const included = new Set(items.map((node) => node.id));
      omittedRecords = nodes.filter((node) => node && typeof node.id === 'string' && !included.has(node.id) && Number.isFinite(node.events)).length;
    }
    return { items, omittedRecords };
  }, [nodes]);
  const graphNodes = graphNodeResult.items;
  const graphEdgeResult = useMemo(() => {
    const nodeIds = new Set(graphNodes.map((node) => node.id));
    const unique = new Map<string, TopologyEdge>();
    let omittedRecords = 0;
    for (const edge of edges) {
      if (!edge || typeof edge.source !== 'string' || typeof edge.target !== 'string' || !Number.isFinite(edge.weight)) continue;
      if (!nodeIds.has(edge.source) || !nodeIds.has(edge.target)) continue;
      const key = `${edge.source}\u0000${edge.target}`;
      const weight = Math.max(0, edge.weight);
      const previous = unique.get(key);
      if (!previous && unique.size >= MAX_GRAPH_EDGES) { omittedRecords += 1; continue; }
      unique.set(key, { source: edge.source, target: edge.target, weight: Math.min(Number.MAX_SAFE_INTEGER, (previous?.weight ?? 0) + weight) });
    }
    const items = [...unique.values()].sort((left, right) => left.source.localeCompare(right.source) || left.target.localeCompare(right.target));
    return { items, omittedRecords };
  }, [edges, graphNodes]);
  const graphEdges = graphEdgeResult.items;
  const maxEvents = Math.max(1, ...graphNodes.map((node) => Math.max(0, node.events)));
  const maxWeight = Math.max(1, ...graphEdges.map((edge) => Math.max(0, edge.weight)));
  const incidentServices = useMemo(() => new Set(incidentServiceIds), [incidentServiceIds]);
  const visualEdges = useMemo(() => {
    if (showAllEdges || graphEdges.length <= DEFAULT_VISIBLE_EDGE_LIMIT) return graphEdges;
    return [...graphEdges]
      .sort((left, right) => Math.max(0, right.weight) - Math.max(0, left.weight) || left.source.localeCompare(right.source) || left.target.localeCompare(right.target))
      .slice(0, DEFAULT_VISIBLE_EDGE_LIMIT);
  }, [graphEdges, showAllEdges]);
  const depthEdges = useMemo(() => visualEdges.length <= MAX_3D_EDGE_COUNT ? visualEdges : [...visualEdges]
    .sort((left, right) => right.weight - left.weight || left.source.localeCompare(right.source) || left.target.localeCompare(right.target))
    .slice(0, MAX_3D_EDGE_COUNT), [visualEdges]);
  const displayedEdges = activeMode === '3d' ? depthEdges : visualEdges;
  const edgeScope = `${displayedEdges.length} of ${graphEdges.length} ${graphKind === 'declared' ? 'declared dependencies' : 'observed dependencies'} shown`;
  const stageDescription = graphEdges.length > displayedEdges.length
    ? `${description} The visualization shows ${displayedEdges.length} of ${graphEdges.length} returned edges${activeMode === '3d' && displayedEdges.length === MAX_3D_EDGE_COUNT ? ', capped to the strongest edges for 3D rendering' : ''}; the accessible adjacency list below includes the full returned graph subset.`
    : `${description} The visualization shows all ${graphEdges.length} returned edges.`;
  const weightSource = graphKind === 'declared' ? 'simulated window volume' : 'observed request-trail weight';
  const eventsUnit = graphKind === 'declared' ? 'events in window' : 'dataset events';
  const adjacencyHeading = graphKind === 'declared' ? 'Declared dependency adjacency' : 'Observed request-trail adjacency';
  const edgeWeightUnit = graphKind === 'declared' ? 'declared window weight' : 'observed weight';
  const rendererNote = graphKind === 'declared' ? 'DECLARED DEPENDENCIES' : 'OBSERVED DEPENDENCIES';
  const emptyState = graphKind === 'declared' ? 'No declared service nodes were returned.' : 'No observed service nodes were returned.';

  const positions = useMemo(() => {
    const map = new Map<string, Point>();
    const count = graphNodes.length;
    const centerX = WIDTH / 2;
    const centerY = HEIGHT / 2;
    const radiusX = WIDTH / 2 - PADDING;
    const radiusY = HEIGHT / 2 - PADDING;
    graphNodes.forEach((node, index) => {
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
  }, [graphNodes]);

  const select = useCallback((id: string | null) => {
    if (selectedId === undefined) setInternalSelected(id);
    onSelect?.(id);
  }, [onSelect, selectedId]);

  const changeMode = (next: TopologyMode) => {
    if (mode === undefined) setInternalMode(next);
    onModeChange?.(next);
  };

  const focusSelected = () => {
    if (!activeSelected) return;
    setFocusToken((value) => value + 1);
    const point = positions.get(activeSelected);
    if (point) {
      const width = 420;
      const height = 280;
      setViewport({ x: clamp(point.x - width / 2, 0, WIDTH - width), y: clamp(point.y - height / 2, 0, HEIGHT - height), width, height });
    }
  };

  const resetView = () => {
    setResetToken((value) => value + 1);
    setViewport({ x: 0, y: 0, width: WIDTH, height: HEIGHT });
    select(null);
  };

  const viewBox = `${viewport.x} ${viewport.y} ${viewport.width} ${viewport.height}`;
  const panTo = (x: number, y: number, width = viewport.width, height = viewport.height) => setViewport({
    width, height,
    x: clamp(x, 0, WIDTH - width),
    y: clamp(y, 0, HEIGHT - height)
  });
  const zoom = (factor: number, anchorX = viewport.x + viewport.width / 2, anchorY = viewport.y + viewport.height / 2) => {
    const width = clamp(viewport.width * factor, MIN_VIEW_WIDTH, WIDTH);
    const height = clamp(viewport.height * factor, MIN_VIEW_HEIGHT, HEIGHT);
    const ratioX = (anchorX - viewport.x) / viewport.width;
    const ratioY = (anchorY - viewport.y) / viewport.height;
    panTo(anchorX - ratioX * width, anchorY - ratioY * height, width, height);
  };
  const onWheel = (event: WheelEvent<SVGSVGElement>) => {
    event.preventDefault();
    const rect = event.currentTarget.getBoundingClientRect();
    const anchorX = viewport.x + ((event.clientX - rect.left) / Math.max(rect.width, 1)) * viewport.width;
    const anchorY = viewport.y + ((event.clientY - rect.top) / Math.max(rect.height, 1)) * viewport.height;
    zoom(event.deltaY < 0 ? 0.88 : 1.14, anchorX, anchorY);
  };
  const beginPan = (event: PointerEvent<SVGSVGElement>) => {
    if (event.button !== 0 || (event.target as Element).closest('.topology-node')) return;
    panStart.current = { pointerId: event.pointerId, clientX: event.clientX, clientY: event.clientY, x: viewport.x, y: viewport.y };
    event.currentTarget.setPointerCapture(event.pointerId);
  };
  const movePan = (event: PointerEvent<SVGSVGElement>) => {
    const start = panStart.current;
    if (!start || start.pointerId !== event.pointerId) return;
    const rect = event.currentTarget.getBoundingClientRect();
    panTo(start.x - ((event.clientX - start.clientX) / Math.max(rect.width, 1)) * viewport.width,
      start.y - ((event.clientY - start.clientY) / Math.max(rect.height, 1)) * viewport.height);
  };
  const endPan = (event: PointerEvent<SVGSVGElement>) => {
    if (panStart.current?.pointerId === event.pointerId) panStart.current = null;
  };
  const fitView = () => {
    setViewport({ x: 0, y: 0, width: WIDTH, height: HEIGHT });
    setFitToken((value) => value + 1);
  };
  const stageClass = `topology-stage${activeMode === '3d' ? ' topology-stage--depth' : ''}${reducedMotion ? ' topology-stage--reduced-motion' : ''}`;

  return (
    <section className="topology-panel" aria-label={title} aria-describedby={descriptionId}>
      <div className="topology-toolbar" role="group" aria-label="Topology controls">
        <div className="topology-mode-switch" role="group" aria-label="Topology display mode">
          <button className={`btn btn-sm${activeMode === '2d' ? ' btn-primary' : ''}`} type="button" aria-pressed={activeMode === '2d'} onClick={() => changeMode('2d')}>2D</button>
          <button className={`btn btn-sm${activeMode === '3d' ? ' btn-primary' : ''}`} type="button" aria-pressed={activeMode === '3d'} onClick={() => changeMode('3d')}>3D WebGL</button>
          {graphEdges.length > DEFAULT_VISIBLE_EDGE_LIMIT && <button className="btn btn-sm topology-edge-toggle" type="button" aria-pressed={showAllEdges} onClick={() => setShowAllEdges((value) => !value)}>{showAllEdges ? `Show strongest ${DEFAULT_VISIBLE_EDGE_LIMIT}` : `Show all ${graphEdges.length} edges`}</button>}
        </div>
        <div className="topology-actions">
          <button className="btn btn-sm" type="button" onClick={() => zoom(.82)} aria-label="Zoom in">+</button>
          <button className="btn btn-sm" type="button" onClick={() => zoom(1.22)} aria-label="Zoom out">−</button>
          <button className="btn btn-sm" type="button" onClick={fitView}>Fit to view</button>
          <button className="btn btn-sm" type="button" onClick={focusSelected} disabled={!activeSelected}>Focus selected</button>
          <button className="btn btn-sm" type="button" onClick={resetView}>Reset view</button>
          {activeSelected && <button className="btn btn-sm btn-quiet" type="button" onClick={() => select(null)}>Clear selection</button>}
        </div>
      </div>
      {incidentLabel && incidentServices.size > 0 && <div className="topology-incident-context" role="note"><span className="topology-incident-symbol" aria-hidden="true" />{incidentLabel}; highlighted nodes are names returned in the same detector window, not a causal path.</div>}
      {(graphNodeResult.omittedRecords > 0 || graphEdgeResult.omittedRecords > 0) && <p className="topology-budget-note" role="status">Renderer safety limits applied: {graphNodeResult.omittedRecords > 0 && `${graphNodeResult.omittedRecords} additional node records omitted`}{graphNodeResult.omittedRecords > 0 && graphEdgeResult.omittedRecords > 0 ? ' · ' : ''}{graphEdgeResult.omittedRecords > 0 && `${graphEdgeResult.omittedRecords} additional edge records omitted`}. The accessible list contains the rendered graph subset.</p>}
      <p id={descriptionId} className="sr-only">{description} Edge thickness and static markers encode {weightSource}, not verified infrastructure.</p>
      <div ref={stageRef} className={stageClass} data-mode={activeMode} data-reduced-motion={reducedMotion ? 'true' : 'false'}>
        <div className="topology-renderer-note">{activeMode === '3d' ? 'THREE.JS · WEBGL' : rendererNote}{edges.length > DEFAULT_VISIBLE_EDGE_LIMIT && <span className="topology-edge-scope">{edgeScope}</span>}</div>
        {graphNodes.length === 0 ? <div className="topology-empty" role="status">{emptyState}</div> : activeMode === '3d' ? <Suspense fallback={<div className="topology-empty" role="status">Loading 3D service view…</div>}>
          <Topology3D nodes={graphNodes} edges={depthEdges} selectedId={activeSelected} incidentServiceIds={incidentServiceIds} focusToken={focusToken} fitToken={fitToken} resetToken={resetToken} reducedMotion={reducedMotion} graphKind={graphKind} onSelect={select} />
        </Suspense> : <>
          <svg className="topology-svg" viewBox={viewBox} role="img" aria-labelledby={`${titleId}-visual ${descriptionId}-visual`} aria-describedby={`${descriptionId}-visual`} focusable="false" onWheel={onWheel} onPointerDown={beginPan} onPointerMove={movePan} onPointerUp={endPan} onPointerCancel={endPan}>
            <title id={`${titleId}-visual`}>{title}</title>
            <desc id={`${descriptionId}-visual`}>{stageDescription} Selectable service nodes. Edge thickness and static markers encode {weightSource}, not verified infrastructure.</desc>
            <defs>
              <marker id={markerId} viewBox="0 0 10 10" refX="9" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
                <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--text-muted)" />
              </marker>
            </defs>
            <g className="topology-edges" aria-hidden="true">
              {displayedEdges.map((edge, index) => {
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
                const markers = weight > 0 ? Math.min(8, Math.max(1, Math.ceil(normalized * 7))) : 0;
                const pathData = edge.source === edge.target
                  ? `M ${from.x} ${from.y} C ${from.x + 40} ${from.y - 74} ${from.x - 40} ${from.y - 74} ${from.x} ${from.y}`
                  : `M ${from.x} ${from.y} Q ${control.x} ${control.y} ${to.x} ${to.y}`;
                return (
                  <g key={`${edge.source}-${edge.target}-${index}`} data-edge-weight={weight} data-marker-count={markers}>
                    <path d={pathData} fill="none" stroke="var(--text-muted)" strokeWidth={width} strokeLinecap="round" opacity={0.3 + normalized * 0.55} markerEnd={`url(#${markerId})`} />
                    {Array.from({ length: markers }, (_, markerIndex) => {
                      const point = pointOnPath(from, control, to, (markerIndex + 1) / (markers + 1));
                      return <circle key={markerIndex} className="topology-weight-marker" cx={point.x} cy={point.y} r={1.5 + normalized * 1.2} fill="var(--accent-strong)" />;
                    })}
                  </g>
                );
              })}
            </g>
            <g className="topology-nodes">
              {graphNodes.map((node) => {
                const point = positions.get(node.id);
                if (!point) return null;
                const normalized = Math.max(0, node.events) / maxEvents;
                const radius = 12 + Math.sqrt(normalized) * 22;
                const selected = activeSelected === node.id;
                const incidentRelated = incidentServices.has(node.id);
                const health = healthBand(node);
                const handleKey = (event: KeyboardEvent<SVGGElement>) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    select(node.id);
                  }
                };
                  return (
                  <g key={node.id} className={`topology-node topology-node--${health}${selected ? ' topology-node--selected' : ''}${incidentRelated ? ' topology-node--incident' : ''}`} role="button" tabIndex={0} aria-pressed={selected} aria-label={`${node.id}, ${formatNumber(node.events)} ${eventsUnit}, ${rateText(node)}, ${healthText(node)}${incidentRelated ? ', named in the incident detector window' : ''}`} onClick={() => select(node.id)} onKeyDown={handleKey} data-node-id={node.id} data-events={node.events} data-health={health} data-radius={radius} data-incident-related={incidentRelated ? 'true' : 'false'}>
                    {incidentRelated && <circle className="topology-incident-ring" cx={point.x} cy={point.y} r={radius + 8} />}
                    <circle cx={point.x} cy={point.y} r={radius} fill="var(--bg-elevated)" stroke={selected ? 'var(--accent-strong)' : 'var(--accent)'} strokeWidth={selected ? 3 : 2} />
                    <circle className="topology-health-dot" cx={point.x - radius + 7} cy={point.y - radius + 8} r={3.5} fill={`var(--${health === 'unknown' ? 'text-muted' : health === 'healthy' ? 'ok' : health === 'watch' ? 'warn' : 'danger'})`} />
                    <text x={point.x} y={point.y + 4} textAnchor="middle" className="node-label">{shortLabel(node.id)}</text>
                    <text x={point.x} y={point.y + radius + 15} textAnchor="middle" className="node-count">{formatNumber(node.events)} {graphKind === 'declared' ? 'events' : 'dataset'} · {shortRateText(node)}</text>
                  </g>
                );
              })}
            </g>
          </svg>
        </>}
      </div>
      <div className="topology-accessible-list">
        <div className="topology-list-heading">Accessible service list · {eventsUnit} and selected-window error rate</div>
        <ul aria-label="Accessible service list">
          {graphNodes.map((node) => <li key={node.id}><button type="button" className={`topology-service-button${activeSelected === node.id ? ' topology-service-button--selected' : ''}${incidentServices.has(node.id) ? ' topology-service-button--incident' : ''}`} aria-label={`${node.id}, ${formatNumber(node.events)} ${eventsUnit}, ${rateText(node)}, ${healthText(node)}${incidentServices.has(node.id) ? ', named in the incident detector window' : ''}`} aria-pressed={activeSelected === node.id} onClick={() => select(node.id)}><span>{node.id}</span><span>{formatNumber(node.events)} {eventsUnit}</span><span>{rateText(node)}</span><span>{incidentServices.has(node.id) ? 'Incident window' : healthText(node)}</span></button></li>)}
        </ul>
        {graphEdges.length > 0 && <><div className="topology-list-heading topology-list-heading--edges">{adjacencyHeading}</div><ul aria-label={`${adjacencyHeading} edges`}>{graphEdges.map((edge, index) => <li className="topology-edge-item" key={`${edge.source}-${edge.target}-${index}`}>{edge.source} → {edge.target} <span>{formatNumber(edge.weight)} {edgeWeightUnit}</span></li>)}</ul></>}
      </div>
    </section>
  );
}

export default TopologyPanel;
