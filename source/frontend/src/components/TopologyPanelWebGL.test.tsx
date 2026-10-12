import { useState } from 'react';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TopologyPanel, type TopologyEdge, type TopologyMode, type TopologyNode } from './TopologyPanel';

/**
 * Regression coverage for the live WebGL lifecycle.
 *
 * The original suite mocked `getContext` to `null`, so `new WebGLRenderer`
 * always threw and no renderer was ever constructed. That left this whole path
 * untested — including the defect where an unstable prop identity rebuilt the
 * entire scene, and therefore the WebGL context, on every single render.
 *
 * jsdom has no WebGL, so `three`'s renderer and orbit controls are replaced with
 * minimal stand-ins that record construction and disposal. The intent is not to
 * test three.js; it is to prove this component builds one scene per *real*
 * input change and releases the context on teardown.
 */
const rendererState = { created: 0, disposed: 0, contextsLost: 0, failContext: false };
let activeAnimationLoops = 0;

vi.mock('three', async () => {
  const actual = await vi.importActual<typeof import('three')>('three');
  class FakeRenderer {
    domElement = document.createElement('canvas');
    setPixelRatio = () => {};
    setClearColor = () => {};
    setSize = () => {};
    setAnimationLoop = (fn: unknown) => { activeAnimationLoops = fn ? activeAnimationLoops + 1 : 0; };
    render = () => {};
    dispose = () => { rendererState.disposed += 1; };
    forceContextLoss = () => { rendererState.contextsLost += 1; };
  }
  class FakeOrbitControls {
    enableDamping = false; dampingFactor = 0; minDistance = 0; maxDistance = 0;
    maxPolarAngle = 0; autoRotate = false;
    target = { set: () => {}, copy: () => {}, setScalar: () => {} };
    position = { set: () => {} };
    update = () => {};
    addEventListener = () => {};
    removeEventListener = () => {};
    dispose = () => {};
  }
  return {
    ...actual,
    WebGLRenderer: class extends FakeRenderer {
      constructor() {
        super();
        // Mirrors three's own behaviour when no WebGL context can be created.
        if (rendererState.failContext) throw new Error('Error creating WebGL context.');
        rendererState.created += 1;
      }
    },
    OrbitControls: FakeOrbitControls
  };
});

const nodes: TopologyNode[] = [
  { id: 'api', events: 100, errorRate: 4, health: 'healthy' },
  { id: 'auth', events: 60, errorRate: 9, health: 'watch' },
  { id: 'db', events: 30, errorRate: 18, health: 'elevated' }
];
const edges: TopologyEdge[] = [
  { source: 'api', target: 'auth', weight: 8 },
  { source: 'auth', target: 'db', weight: 4 }
];

function Harness({ incidentServiceIds }: { incidentServiceIds?: string[] }) {
  const [mode, setMode] = useState<TopologyMode>('2d');
  const [selected, setSelected] = useState<string | null>(null);
  return (
    <TopologyPanel
      nodes={nodes}
      edges={edges}
      mode={mode}
      onModeChange={setMode}
      selectedId={selected}
      onSelect={setSelected}
      {...(incidentServiceIds ? { incidentServiceIds } : {})}
    />
  );
}

/**
 * Reproduces the shape used by every streaming call site: the parent re-creates
 * the node and edge arrays on each tick with identical contents, the way
 * `frame.health ?? []` and `blastRadiusServices(...)` do.
 */
function StreamingHarness({ tick }: { tick: number }) {
  const [mode, setMode] = useState<TopologyMode>('2d');
  const freshNodes = nodes.map((node) => ({ ...node }));
  const freshEdges = edges.map((edge) => ({ ...edge }));
  void tick;
  return <TopologyPanel nodes={freshNodes} edges={freshEdges} mode={mode} onModeChange={setMode} />;
}

/** Enters 3D mode and waits for the lazily loaded scene to mount. */
async function enter3d() {
  fireEvent.click(screen.getByRole('button', { name: '3D WebGL' }));
  await screen.findByText('THREE.JS · WEBGL');
  await waitFor(() => expect(rendererState.created).toBeGreaterThan(0));
}

beforeEach(() => {
  rendererState.created = 0;
  rendererState.disposed = 0;
  rendererState.contextsLost = 0;
  rendererState.failContext = false;
  activeAnimationLoops = 0;
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} });
  vi.stubGlobal('IntersectionObserver', class {
    constructor(private cb: (entries: unknown[]) => void) {}
    observe(el: Element) { this.cb([{ isIntersecting: true, target: el }]); }
    unobserve() {} disconnect() {}
  });
});

afterEach(() => {
  // This suite counts constructor calls, so every render must be unmounted
  // before the counters are reset or results leak between tests.
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('TopologyPanel WebGL lifecycle', () => {
  it('does not rebuild the scene when a caller passes an equivalent array', async () => {
    const { rerender } = render(<Harness />);
    await enter3d();
    expect(rendererState.created).toBe(1);

    // A freshly filtered array with identical contents must not tear down the
    // WebGL context. This is the regression guard for the churn defect.
    rerender(<Harness incidentServiceIds={['db']} />);
    rerender(<Harness incidentServiceIds={['db']} />);
    await waitFor(() => expect(rendererState.created).toBe(2));
    rerender(<Harness incidentServiceIds={['db']} />);

    expect(rendererState.created).toBe(2);
  });

  it('does not rebuild when a streaming parent re-creates identical node and edge arrays', async () => {
    // This is the shape every live call site produces: new array objects with
    // the same contents on every tick. An identity-keyed effect would rebuild
    // the WebGL context several times a second and exhaust the browser's
    // live-context budget.
    const { rerender } = render(<StreamingHarness tick={0} />);
    await enter3d();
    expect(rendererState.created).toBe(1);

    for (let tick = 1; tick <= 12; tick += 1) {
      rerender(<StreamingHarness tick={tick} />);
    }
    await new Promise((resolve) => setTimeout(resolve, 60));

    expect(rendererState.created).toBe(1);
    expect(rendererState.contextsLost).toBe(0);
  });

  it('rebuilds when the graph content genuinely changes', async () => {
    function GrowingHarness({ extra }: { extra: boolean }) {
      const [mode, setMode] = useState<TopologyMode>('2d');
      const list = extra ? [...nodes, { id: 'cache', events: 10, health: 'healthy' as const }] : nodes;
      return <TopologyPanel nodes={list} edges={edges} mode={mode} onModeChange={setMode} />;
    }
    const { rerender } = render(<GrowingHarness extra={false} />);
    await enter3d();
    const baseline = rendererState.created;

    rerender(<GrowingHarness extra />);
    await waitFor(() => expect(rendererState.created).toBe(baseline + 1));
    expect(rendererState.created).toBe(baseline + 1);
  });

  it('rebuilds once when the incident set genuinely changes', async () => {
    const { rerender } = render(<Harness incidentServiceIds={['db']} />);
    await enter3d();
    const baseline = rendererState.created;

    rerender(<Harness incidentServiceIds={['api', 'auth']} />);
    await waitFor(() => expect(rendererState.created).toBe(baseline + 1));
    expect(rendererState.created).toBe(baseline + 1);
  });

  it('releases the WebGL context on unmount', async () => {
    const { unmount } = render(<Harness />);
    await enter3d();

    unmount();

    expect(rendererState.disposed).toBe(1);
    // dispose() alone does not release the browser context; forceContextLoss does.
    expect(rendererState.contextsLost).toBe(1);
    expect(activeAnimationLoops).toBe(0);
  });

  it('never accumulates canvases across repeated mode switches', async () => {
    const { container } = render(<Harness />);
    for (let i = 0; i < 4; i += 1) {
      await enter3d();
      fireEvent.click(screen.getByRole('button', { name: '2D' }));
    }
    expect(container.querySelectorAll('canvas').length).toBeLessThanOrEqual(1);
    // Every mount that was torn down released its context.
    expect(rendererState.contextsLost).toBe(rendererState.disposed);
  });

  it('keeps selection and the accessible service list while WebGL is active', async () => {
    render(<Harness />);
    await enter3d();

    const listButton = screen.getByRole('list', { name: 'Accessible service list' })
      .querySelectorAll('button')[0];
    fireEvent.click(listButton);
    expect(listButton.getAttribute('aria-pressed')).toBe('true');

    // Switching back to 2D keeps the same selection.
    fireEvent.click(screen.getByRole('button', { name: '2D' }));
    expect(document.querySelector('.topology-node--selected')).not.toBeNull();
  });

  it('falls back to 2D and keeps the data reachable when WebGL is unavailable', async () => {
    rendererState.failContext = true;
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    try {
      render(<Harness />);
      fireEvent.click(screen.getByRole('button', { name: '3D WebGL' }));

      await screen.findByText('3D visualization unavailable on this device.');
      expect(rendererState.created).toBe(0);
      // The underlying services remain inspectable without WebGL.
      expect(screen.getByRole('list', { name: 'Accessible service list' })).toBeInTheDocument();

      // Retrying is offered rather than a dead end.
      expect(screen.getByRole('button', { name: 'Retry WebGL' })).toBeInTheDocument();
    } finally {
      rendererState.failContext = false;
      errorSpy.mockRestore();
    }
  });
});