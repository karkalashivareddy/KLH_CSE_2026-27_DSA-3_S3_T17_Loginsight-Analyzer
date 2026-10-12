import { useState } from 'react';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TopologyPanel, type TopologyMode } from './TopologyPanel';

const nodes = [
  { id: 'api', events: 100, errorRate: 6.25, health: 'watch' as const },
  { id: 'auth', events: 40 }
];
const edges = [{ source: 'api', target: 'auth', weight: 8 }];

describe('TopologyPanel', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('describes a declared simulation graph without dataset wording', () => {
    const { container } = render(<TopologyPanel nodes={nodes} edges={edges} graphKind="declared" />);

    expect(container.querySelector('.topology-renderer-note')?.textContent).toContain('DECLARED DEPENDENCIES');
    expect(screen.getByText('Declared dependency adjacency')).toBeInTheDocument();
    expect(screen.getByText('Accessible service list · events in window and selected-window error rate')).toBeInTheDocument();
    expect(container.querySelector('[data-node-id="api"]')).toHaveAttribute(
      'aria-label',
      'api, 100 events in window, 6.3% window error rate, watch',
    );
    expect(container.querySelector('.node-count')?.textContent).toContain('100 events ·');
    expect(container.textContent).not.toContain('dataset');
    expect(container.textContent).not.toContain('Observed');
  });

  it('exposes an accessible service list and selectable data-driven nodes', () => {
    const onSelect = vi.fn();
    const { container } = render(<TopologyPanel nodes={nodes} edges={edges} onSelect={onSelect} />);

    const list = screen.getByRole('list', { name: 'Accessible service list' });
    fireEvent.click(within(list).getByRole('button', { name: /api, 100 dataset events, 6\.3% window error rate, watch/i }));

    expect(onSelect).toHaveBeenCalledWith('api');
    expect(screen.getByText('Observed request-trail adjacency')).toBeInTheDocument();
    expect(screen.getByText(/api → auth/)).toHaveTextContent('8 observed weight');
    expect(container.querySelector('[data-node-id="api"]')).toHaveAttribute('data-events', '100');
    expect(container.querySelector('[data-edge-weight="8"]')).toHaveAttribute('data-marker-count', '7');
    expect(Number(container.querySelector('[data-node-id="api"]')?.getAttribute('data-radius'))).toBeGreaterThan(Number(container.querySelector('[data-node-id="auth"]')?.getAttribute('data-radius')));
  });

  it('separates dataset-wide event counts from selected-window error rates', () => {
    const { container } = render(<TopologyPanel nodes={nodes} edges={edges} />);

    const apiNode = container.querySelector('[data-node-id="api"]');
    expect(apiNode).toHaveAttribute('aria-label', 'api, 100 dataset events, 6.3% window error rate, watch');
    expect(apiNode).toHaveClass('topology-node--watch');
    expect(apiNode?.querySelector('.node-count')).toHaveTextContent('100 dataset \u00b7 6.3% window');

    const authNode = container.querySelector('[data-node-id="auth"]');
    expect(authNode).toHaveAttribute('aria-label', 'auth, 40 dataset events, window error rate unavailable, health unavailable');
    expect(authNode).toHaveClass('topology-node--unknown');
    expect(authNode?.querySelector('.node-count')).toHaveTextContent('40 dataset \u00b7 rate unavailable');
    expect(screen.getByText('Accessible service list \u00b7 dataset events and selected-window error rate')).toBeInTheDocument();
    expect(screen.getByText('100 dataset events')).toBeInTheDocument();
    expect(screen.getByText('6.3% window error rate')).toBeInTheDocument();
  });

  it('marks service names returned in an incident window without claiming causality', () => {
    const { container } = render(<TopologyPanel nodes={nodes} edges={edges} incidentServiceIds={['api']} incidentLabel="Named by incident #7" />);

    const apiNode = container.querySelector('[data-node-id="api"]');
    expect(apiNode).toHaveAttribute('data-incident-related', 'true');
    expect(apiNode).toHaveClass('topology-node--incident');
    expect(apiNode).toHaveAttribute('aria-label', expect.stringContaining('named in the incident detector window'));
    expect(container.querySelector('[data-node-id="auth"]')).toHaveAttribute('data-incident-related', 'false');
    expect(screen.getByText(/Named by incident #7/)).toHaveTextContent('not a causal path');
  });

  it('keeps dense graphs legible while retaining every returned edge for inspection', () => {
    const denseNodes = [{ id: 'source', events: 100 }, ...Array.from({ length: 22 }, (_, index) => ({ id: `service-${index}`, events: 20 + index }))];
    const denseEdges = denseNodes.slice(1).map((node, index) => ({ source: 'source', target: node.id, weight: index + 1 }));
    const { container } = render(<TopologyPanel nodes={denseNodes} edges={denseEdges} />);

    expect(container.querySelectorAll('[data-edge-weight]')).toHaveLength(18);
    expect(screen.getByText(/18 of 22 observed dependencies shown/)).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Observed request-trail adjacency edges' }).querySelectorAll('li')).toHaveLength(22);

    fireEvent.click(screen.getByRole('button', { name: 'Show all 22 edges' }));
    expect(container.querySelectorAll('[data-edge-weight]')).toHaveLength(22);
    expect(screen.getByRole('button', { name: 'Show strongest 18' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('loads an actual WebGL view on demand and reports the 2D fallback when WebGL is unavailable', async () => {
    function Harness() {
      const [mode, setMode] = useState<TopologyMode>('2d');
      return <TopologyPanel nodes={nodes} edges={edges} mode={mode} onModeChange={setMode} />;
    }

    const { container } = render(<Harness />);
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
    fireEvent.click(screen.getByRole('button', { name: '3D WebGL' }));

    expect(container.querySelector('.topology-stage')).toHaveAttribute('data-mode', '3d');
    expect(screen.getByRole('button', { name: '3D WebGL' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText('THREE.JS · WEBGL')).toBeInTheDocument();
    expect(await screen.findByText('3D visualization unavailable on this device.')).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Accessible service list' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '2D' }));
    expect(screen.getByRole('button', { name: '2D' })).toHaveAttribute('aria-pressed', 'true');
    expect(container.querySelector('.topology-svg')).toHaveAttribute('viewBox', '0 0 760 440');
  });

  it('keeps weighted edge markers static for a dataset rather than implying ongoing traffic', () => {
    const original = window.matchMedia;
    Object.defineProperty(window, 'matchMedia', { configurable: true, value: vi.fn().mockReturnValue({ matches: true, addEventListener: vi.fn(), removeEventListener: vi.fn() }) });

    const { container } = render(<TopologyPanel nodes={nodes} edges={edges} />);

    expect(container.querySelector('.topology-stage')).toHaveAttribute('data-reduced-motion', 'true');
    expect(container.querySelectorAll('.topology-weight-marker')).toHaveLength(7);
    expect(container.querySelector('animateMotion')).not.toBeInTheDocument();

    Object.defineProperty(window, 'matchMedia', { configurable: true, value: original });
  });

  it('zooms and resets the 2D canvas with bounded view extents', () => {
    const { container } = render(<TopologyPanel nodes={nodes} edges={edges} />);
    const svg = container.querySelector('.topology-svg')!;
    expect(svg).toHaveAttribute('viewBox', '0 0 760 440');

    fireEvent.click(screen.getByRole('button', { name: 'Zoom in' }));
    const zoomed = svg.getAttribute('viewBox')!;
    expect(zoomed).not.toBe('0 0 760 440');
    expect(Number(zoomed.split(' ')[2])).toBeGreaterThanOrEqual(230);

    fireEvent.click(screen.getByRole('button', { name: 'Fit to view' }));
    expect(svg).toHaveAttribute('viewBox', '0 0 760 440');
  });

  it('drops malformed and dangling graph entries while coalescing duplicate directed edges', () => {
    const { container } = render(<TopologyPanel
      nodes={[{ id: 'api', events: 10 }, { id: 'api', events: 99 }, { id: 'broken', events: Number.NaN }, { id: 'auth', events: 3 }]}
      edges={[{ source: 'api', target: 'auth', weight: 2 }, { source: 'api', target: 'auth', weight: 4 }, { source: 'missing', target: 'api', weight: 20 }, { source: 'api', target: 'auth', weight: Number.NaN }]}
    />);

    expect(container.querySelectorAll('[data-node-id]')).toHaveLength(2);
    expect(container.querySelectorAll('[data-edge-weight]')).toHaveLength(1);
    expect(container.querySelector('[data-edge-weight]')).toHaveAttribute('data-edge-weight', '6');
    expect(screen.getByRole('list', { name: 'Observed request-trail adjacency edges' })).toHaveTextContent('6 observed weight');
  });

  it('caps excessive graph rendering and reports the omitted input records', () => {
    const manyNodes = Array.from({ length: 252 }, (_, index) => ({ id: `service-${index}`, events: index }));
    const manyEdges = Array.from({ length: 2_501 }, (_, index) => ({
      source: `service-${Math.floor(index / 249)}`,
      target: `service-${1 + (index % 249)}`,
      weight: index + 1
    }));
    const { container } = render(<TopologyPanel nodes={manyNodes} edges={manyEdges} />);

    expect(container.querySelectorAll('[data-node-id]')).toHaveLength(250);
    expect(container.querySelectorAll('.topology-service-button')).toHaveLength(250);
    expect(screen.getByRole('status')).toHaveTextContent('2 additional node records omitted');
    expect(screen.getByRole('status')).toHaveTextContent('additional edge records omitted');
    expect(container.querySelectorAll('.topology-edge-item')).toHaveLength(2_500);
  });

  it('supports selecting a node with the keyboard and clearing the selection', () => {
    const onSelect = vi.fn();
    const { container } = render(<TopologyPanel nodes={nodes} edges={edges} onSelect={onSelect} />);
    const node = container.querySelector('[data-node-id="api"]')!;

    fireEvent.keyDown(node, { key: 'Enter' });
    expect(onSelect).toHaveBeenCalledWith('api');
    fireEvent.click(screen.getByRole('button', { name: 'Clear selection' }));
    expect(onSelect).toHaveBeenLastCalledWith(null);
  });
});
