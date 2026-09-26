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

  it('exposes an accessible service list and selectable data-driven nodes', () => {
    const onSelect = vi.fn();
    const { container } = render(<TopologyPanel nodes={nodes} edges={edges} onSelect={onSelect} />);

    const list = screen.getByRole('list', { name: 'Accessible service list' });
    fireEvent.click(within(list).getByRole('button', { name: /api, 100 dataset events, 6\.3% window error rate, watch/i }));

    expect(onSelect).toHaveBeenCalledWith('api');
    expect(screen.getByText('Observed request-trail adjacency')).toBeInTheDocument();
    expect(screen.getByText(/api → auth/)).toHaveTextContent('8 observed weight');
    expect(container.querySelector('[data-node-id="api"]')).toHaveAttribute('data-events', '100');
    expect(container.querySelector('[data-edge-weight="8"]')).toHaveAttribute('data-particle-count', '7');
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

  it('supports controlled depth mode and reset controls', () => {
    function Harness() {
      const [mode, setMode] = useState<TopologyMode>('2d');
      return <TopologyPanel nodes={nodes} edges={edges} mode={mode} onModeChange={setMode} />;
    }

    const { container } = render(<Harness />);
    fireEvent.click(screen.getByRole('button', { name: '3D / depth' }));

    expect(container.querySelector('.topology-stage')).toHaveAttribute('data-mode', '3d');
    expect(screen.getByRole('button', { name: '3D / depth' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText('2.5D / SVG perspective · not WebGL')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Reset view' }));
    expect(screen.getByRole('button', { name: '2D' })).toHaveAttribute('aria-pressed', 'false');
    expect(container.querySelector('.topology-svg')).toHaveAttribute('viewBox', '0 0 760 440');
  });

  it('marks static particles when reduced motion is requested', () => {
    const original = window.matchMedia;
    Object.defineProperty(window, 'matchMedia', { configurable: true, value: vi.fn().mockReturnValue({ matches: true, addEventListener: vi.fn(), removeEventListener: vi.fn() }) });

    const { container } = render(<TopologyPanel nodes={nodes} edges={edges} />);

    expect(container.querySelector('.topology-stage')).toHaveAttribute('data-reduced-motion', 'true');
    expect(container.querySelectorAll('.topology-particle--static')).toHaveLength(7);

    Object.defineProperty(window, 'matchMedia', { configurable: true, value: original });
  });
});
