import { useEffect, useMemo, useRef, useState } from 'react';
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import type { TopologyEdge, TopologyNode } from './TopologyPanel';

interface Topology3DProps {
  nodes: TopologyNode[];
  edges: TopologyEdge[];
  selectedId: string | null;
  incidentServiceIds: string[];
  focusToken: number;
  fitToken: number;
  resetToken: number;
  reducedMotion: boolean;
  graphKind?: 'observed' | 'declared';
  onSelect: (id: string | null) => void;
}

type SceneNode = {
  group: THREE.Group;
  core: THREE.Mesh<THREE.SphereGeometry, THREE.MeshStandardMaterial>;
  halo: THREE.Mesh<THREE.TorusGeometry, THREE.MeshBasicMaterial>;
  incidentRing?: THREE.Mesh<THREE.TorusGeometry, THREE.MeshBasicMaterial>;
  position: THREE.Vector3;
  health: NonNullable<TopologyNode['health']>;
  /** Resting emphasis for this node's health band, before any focus boost. */
  baseEmissive: number;
  baseHaloOpacity: number;
};

const HEALTH_COLORS: Record<NonNullable<TopologyNode['health']>, number> = {
  healthy: 0x54ddb5,
  watch: 0xf4bd55,
  elevated: 0xfb6577,
  unknown: 0x82969c
};

function graphPositions(nodes: TopologyNode[]): Map<string, THREE.Vector3> {
  const positions = new Map<string, THREE.Vector3>();
  const count = Math.max(nodes.length, 1);
  const radius = Math.max(3.8, Math.min(7.1, 1.6 + count * 0.68));
  nodes.forEach((node, index) => {
    const angle = (Math.PI * 2 * index) / count - Math.PI / 2;
    const y = count <= 2 ? (index === 0 ? 0.3 : -0.25) : Math.sin(index * 1.7) * 1.1;
    positions.set(node.id, new THREE.Vector3(Math.cos(angle) * radius, y, Math.sin(angle) * radius * 0.66));
  });
  return positions;
}

export default function Topology3D({ nodes, edges, selectedId, incidentServiceIds, focusToken, fitToken, resetToken, reducedMotion, graphKind = 'observed', onSelect }: Topology3DProps) {
  const stageRef = useRef<HTMLDivElement>(null);
  const latestSelectionRef = useRef(selectedId);
  const onSelectRef = useRef(onSelect);
  const hoveredIdRef = useRef<string | null>(null);
  const [retryToken, setRetryToken] = useState(0);
  const [rendererError, setRendererError] = useState(false);
  const [hovered, setHovered] = useState<{ id: string; x: number; y: number } | null>(null);

  useEffect(() => { latestSelectionRef.current = selectedId; }, [selectedId]);
  useEffect(() => { onSelectRef.current = onSelect; }, [onSelect]);

  /**
   * The scene-building effect must depend on the *content* of its inputs, never
   * on their array identity. Callers routinely pass freshly-filtered arrays
   * (`graph.health ?? []`, `.filter(...)` at a call site), and an identity dep
   * would tear down and reallocate the whole WebGL context on every render —
   * exhausting the browser's live-context budget within seconds.
   *
   * `JSON.stringify` is used rather than a delimiter join because it cannot
   * collide when a service name contains the separator itself. The current
   * values are read through refs, so the effect re-runs only when a set actually
   * changes.
   */
  const incidentIdsRef = useRef(incidentServiceIds);
  const graphRef = useRef({ nodes, edges });
  const incidentKey = JSON.stringify(incidentServiceIds);
  /**
   * Keyed only on the fields the scene actually reads. `errorRate` drives the
   * 2D map and the accessible list, not the 3D geometry, so including it would
   * rebuild the whole WebGL context whenever a value it ignores moved.
   */
  const graphKey = useMemo(
    () => JSON.stringify([
      nodes.map((node) => [node.id, node.events, node.health ?? null]),
      edges.map((edge) => [edge.source, edge.target, edge.weight])
    ]),
    [nodes, edges]
  );
  useEffect(() => { incidentIdsRef.current = incidentServiceIds; }, [incidentKey]);
  useEffect(() => { graphRef.current = { nodes, edges }; }, [graphKey]);

  useEffect(() => {
    const stage = stageRef.current;
    // Read through the ref so the effect body never closes over a stale array.
    const { nodes: currentNodes, edges: currentEdges } = graphRef.current;
    if (!stage || currentNodes.length === 0) return;

    let renderer: THREE.WebGLRenderer;
    try {
      renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'low-power' });
    } catch {
      setRendererError(true);
      return;
    }

    setRendererError(false);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.6));
    renderer.setClearColor(0x071014, 0);
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.domElement.className = 'topology-webgl-canvas';
    renderer.domElement.setAttribute('aria-hidden', 'true');
    stage.prepend(renderer.domElement);

    const scene = new THREE.Scene();
    scene.fog = new THREE.Fog(0x091216, 13, 30);
    const camera = new THREE.PerspectiveCamera(42, 1, 0.1, 60);
    camera.position.set(0, 8.4, 20.5);
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = false;
    controls.dampingFactor = 0.08;
    controls.minDistance = 6;
    controls.maxDistance = 30;
    controls.maxPolarAngle = Math.PI * 0.82;
    controls.target.set(0, 0, 0);
    controls.update();

    scene.add(new THREE.AmbientLight(0x94b9be, 1.25));
    const keyLight = new THREE.PointLight(0x57dcca, 22, 24, 2);
    keyLight.position.set(-5, 7, 4);
    scene.add(keyLight);
    const fillLight = new THREE.PointLight(0x648fe5, 12, 20, 2);
    fillLight.position.set(5, 2, -5);
    scene.add(fillLight);

    const grid = new THREE.GridHelper(20, 20, 0x35656a, 0x29434a);
    grid.position.y = -2.5;
    const gridMaterials = Array.isArray(grid.material) ? grid.material : [grid.material];
    gridMaterials.forEach((material) => {
      material.transparent = true;
      material.opacity = 0.2;
    });
    scene.add(grid);

    const positions = graphPositions(currentNodes);
    const incidentServices = new Set(incidentIdsRef.current);
    const maxEvents = Math.max(1, ...currentNodes.map((node) => Math.max(0, node.events)));
    const maxWeight = Math.max(1, ...currentEdges.map((edge) => Math.max(0, edge.weight)));
    const nodeObjects = new Map<string, SceneNode>();
    const hitAreaMeshes: THREE.Mesh[] = [];
    currentNodes.forEach((node) => {
      const position = positions.get(node.id);
      if (!position) return;
      const scale = 0.3 + Math.sqrt(Math.max(0, node.events) / maxEvents) * 0.48;
      const health = node.health ?? 'unknown';
      const color = HEALTH_COLORS[health];
      const group = new THREE.Group();
      group.position.copy(position);

      const coreMaterial = new THREE.MeshStandardMaterial({
        color: 0x0c1b20,
        emissive: color,
        emissiveIntensity: health === 'elevated' ? 0.58 : 0.34,
        metalness: 0.36,
        roughness: 0.38
      });
      const core = new THREE.Mesh(new THREE.SphereGeometry(scale, 28, 20), coreMaterial);
      core.userData.serviceId = node.id;
      group.add(core);

      const shellMaterial = new THREE.MeshBasicMaterial({ color, wireframe: true, transparent: true, opacity: 0.14 });
      const shell = new THREE.Mesh(new THREE.IcosahedronGeometry(scale * 1.42, 1), shellMaterial);
      shell.userData.serviceId = node.id;
      group.add(shell);

      const haloMaterial = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.62 });
      const halo = new THREE.Mesh(new THREE.TorusGeometry(scale * 1.28, 0.014, 6, 56), haloMaterial);
      halo.rotation.x = Math.PI / 2.5;
      group.add(halo);

      let incidentRing: THREE.Mesh<THREE.TorusGeometry, THREE.MeshBasicMaterial> | undefined;
      if (incidentServices.has(node.id)) {
        incidentRing = new THREE.Mesh(
          new THREE.TorusGeometry(scale * 1.78, 0.012, 5, 56),
          new THREE.MeshBasicMaterial({ color: 0xa78bfa, transparent: true, opacity: 0.72 })
        );
        incidentRing.rotation.x = Math.PI / 2.5;
        group.add(incidentRing);
      }

      const hitArea = new THREE.Mesh(
        new THREE.SphereGeometry(scale * 1.9, 12, 10),
        new THREE.MeshBasicMaterial({ transparent: true, opacity: 0, depthWrite: false })
      );
      hitArea.userData.serviceId = node.id;
      // Hidden from the render pass but still raycastable: three's `intersect()`
      // tests `layers`, not `visible`, so this removes one draw call per node
      // without affecting picking.
      hitArea.visible = false;
      group.add(hitArea);
      // Picking runs against these alone. They are one mesh per node and sit
      // outside the visible shells, so every click that could land on a node
      // lands on its hit area — at a third of the ray-test work of testing
      // core, shell and hit area together.
      hitAreaMeshes.push(hitArea);
      scene.add(group);
      nodeObjects.set(node.id, {
      group,
      core,
      halo,
      incidentRing,
      position: position.clone(),
      health,
      // Elevated nodes stay visibly brighter at rest. Without carrying the
      // resting value on the entry, the highlight pass overwrote it with the
      // same constant for every node and the band was invisible in 3D.
      baseEmissive: health === 'elevated' ? 0.58 : 0.34,
      baseHaloOpacity: health === 'elevated' ? 0.62 : 0.4
    });
    });

    currentEdges.forEach((edge) => {
      const from = positions.get(edge.source);
      const to = positions.get(edge.target);
      if (!from || !to) return;
      const midpoint = from.clone().add(to).multiplyScalar(0.5);
      midpoint.y += 0.62 + (Math.max(0, edge.weight) / maxWeight) * 0.42;
      const curve = new THREE.QuadraticBezierCurve3(from.clone(), midpoint, to.clone());
      const normalized = Math.max(0, edge.weight) / maxWeight;
      const tube = new THREE.Mesh(
        new THREE.TubeGeometry(curve, 32, 0.007 + normalized * 0.014, 5, false),
        new THREE.MeshStandardMaterial({ color: 0x72a8c7, emissive: 0x367ca3, emissiveIntensity: 0.12, transparent: true, opacity: 0.3 + normalized * 0.35 })
      );
      scene.add(tube);
    });

    const raycaster = new THREE.Raycaster();
    const pointer = new THREE.Vector2();
    const bounds = new THREE.Sphere(new THREE.Vector3(), 1);
    const worldUp = new THREE.Vector3(0, 1, 0);
    const cameraGoal = { position: camera.position.clone(), target: controls.target.clone(), active: false };
    let width = 1;
    let height = 1;
    let visible = true;
    let pageVisible = !document.hidden;
    let pointerPosition = { x: 0, y: 0 };

    const updateHighlights = () => {
      const selection = latestSelectionRef.current;
      nodeObjects.forEach((entry, id) => {
        const focused = selection === id || hoveredIdRef.current === id;
entry.core.material.emissiveIntensity = focused ? entry.baseEmissive + 0.38 : entry.baseEmissive;
      entry.halo.material.opacity = focused ? 0.95 : entry.baseHaloOpacity;
        entry.halo.scale.setScalar(focused ? 1.13 : 1);
      });
    };
    const renderOnce = () => {
      controls.update();
      updateHighlights();
      renderer.render(scene, camera);
    };
    const animationFrame = () => {
      if (!visible || !pageVisible || reducedMotion || !cameraGoal.active) {
        renderer.setAnimationLoop(null);
        renderOnce();
        return;
      }
      controls.update();
      if (cameraGoal.active) {
        camera.position.lerp(cameraGoal.position, 0.09);
        controls.target.lerp(cameraGoal.target, 0.09);
        if (camera.position.distanceTo(cameraGoal.position) < 0.035) cameraGoal.active = false;
      }
      updateHighlights();
      renderer.render(scene, camera);
      if (!cameraGoal.active) renderer.setAnimationLoop(null);
    };

    const startLoop = () => {
      if (visible && pageVisible && !reducedMotion && cameraGoal.active) renderer.setAnimationLoop(animationFrame);
      else {
        renderer.setAnimationLoop(null);
        renderOnce();
      }
    };
    const resize = () => {
      const rect = stage.getBoundingClientRect();
      width = Math.max(1, rect.width);
      height = Math.max(1, rect.height);
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      renderer.setSize(width, height, false);
      renderOnce();
    };
    const onVisibility = () => {
      pageVisible = !document.hidden;
      startLoop();
    };
    const setCameraGoal = (id: string | null) => {
      if (id && nodeObjects.has(id)) {
        const target = nodeObjects.get(id)!.position;
        const direction = target.clone().sub(controls.target).normalize();
        if (direction.lengthSq() === 0) direction.set(0, 0, 1);
        cameraGoal.target.copy(target);
        cameraGoal.position.copy(target.clone().add(direction.multiplyScalar(8)).add(worldUp.clone().multiplyScalar(3.2)));
      } else {
        bounds.setFromPoints([...positions.values()]);
        bounds.radius += 1.5;
        const target = bounds.center;
        const distance = Math.max(13, bounds.radius * 2.7);
        cameraGoal.target.copy(target);
        cameraGoal.position.set(target.x, target.y + distance * 0.47, target.z + distance);
      }
      cameraGoal.active = !reducedMotion;
      if (reducedMotion) {
        camera.position.copy(cameraGoal.position);
        controls.target.copy(cameraGoal.target);
      }
      renderOnce();
      startLoop();
    };
    const onFocusRequest = () => setCameraGoal(latestSelectionRef.current);
    const onFitRequest = () => setCameraGoal(null);
    const onSelectionChange = () => renderOnce();
    const onResetRequest = () => {
      cameraGoal.position.set(0, 8.4, 20.5);
      cameraGoal.target.set(0, 0, 0);
      cameraGoal.active = !reducedMotion;
      if (reducedMotion) {
        camera.position.copy(cameraGoal.position);
        controls.target.copy(cameraGoal.target);
      }
      renderOnce();
      startLoop();
    };
    const onPointerLeave = () => {
      stage.classList.remove('topology-stage--node-hover');
      hoveredIdRef.current = null;
      setHovered(null);
    };
    const onPointerMove = (event: PointerEvent) => {
      const previousHover = hoveredIdRef.current;
      const rect = renderer.domElement.getBoundingClientRect();
      pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
      pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
      pointerPosition = { x: event.clientX - rect.left, y: event.clientY - rect.top };
      raycaster.setFromCamera(pointer, camera);
      const hit = raycaster.intersectObjects(hitAreaMeshes, false).find((item) => typeof item.object.userData.serviceId === 'string');
      const nextId = hit?.object.userData.serviceId as string | undefined;
      stage.classList.toggle('topology-stage--node-hover', Boolean(nextId));
      if (nextId) {
        hoveredIdRef.current = nextId;
        setHovered((current) => current?.id === nextId ? current : { id: nextId, ...pointerPosition });
      } else if (hoveredIdRef.current) {
        hoveredIdRef.current = null;
        setHovered(null);
      }
      if (previousHover !== nextId) renderOnce();
    };
    const onClick = (event: MouseEvent) => {
      const rect = renderer.domElement.getBoundingClientRect();
      pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
      pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
      raycaster.setFromCamera(pointer, camera);
      const hit = raycaster.intersectObjects(hitAreaMeshes, false).find((item) => typeof item.object.userData.serviceId === 'string');
      const id = hit?.object.userData.serviceId as string | undefined;
      if (id) onSelectRef.current(id);
    };
    const onContextLost = (event: Event) => {
      event.preventDefault();
      renderer.setAnimationLoop(null);
      setRendererError(true);
    };
    /**
     * A lost context is recoverable: the browser restores it because
     * `onContextLost` calls `preventDefault()`. Bumping `retryToken` rebuilds the
     * scene from scratch, which is the only correct way to reinitialise GL state
     * after a restore. Without this the user is stranded on the fallback.
     */
    const onContextRestored = () => {
      setRendererError(false);
      setRetryToken((value) => value + 1);
    };
    const onControlChange = () => renderOnce();

    renderer.domElement.addEventListener('pointermove', onPointerMove);
    renderer.domElement.addEventListener('pointerleave', onPointerLeave);
    renderer.domElement.addEventListener('click', onClick);
    renderer.domElement.addEventListener('webglcontextlost', onContextLost);
    renderer.domElement.addEventListener('webglcontextrestored', onContextRestored);
    stage.addEventListener('topology-focus-selected', onFocusRequest);
    stage.addEventListener('topology-fit-view', onFitRequest);
    stage.addEventListener('topology-reset-view', onResetRequest);
    stage.addEventListener('topology-selection-changed', onSelectionChange);
    controls.addEventListener('change', onControlChange);
    document.addEventListener('visibilitychange', onVisibility);
    const resizeObserver = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(resize) : null;
    resizeObserver?.observe(stage);
    const intersectionObserver = typeof IntersectionObserver !== 'undefined'
      ? new IntersectionObserver(([entry]) => { visible = Boolean(entry?.isIntersecting); startLoop(); }, { threshold: 0.05 })
      : null;
    intersectionObserver?.observe(stage);
    if (!intersectionObserver) visible = true;
    resize();
    startLoop();

    return () => {
      renderer.setAnimationLoop(null);
      // A stale hover card pointing at a node that no longer exists would
      // otherwise stay pinned over the freshly built scene.
      hoveredIdRef.current = null;
      setHovered(null);
      stage.classList.remove('topology-stage--node-hover');
      document.removeEventListener('visibilitychange', onVisibility);
      renderer.domElement.removeEventListener('pointermove', onPointerMove);
      renderer.domElement.removeEventListener('pointerleave', onPointerLeave);
      renderer.domElement.removeEventListener('click', onClick);
      renderer.domElement.removeEventListener('webglcontextlost', onContextLost);
      renderer.domElement.removeEventListener('webglcontextrestored', onContextRestored);
      stage.removeEventListener('topology-focus-selected', onFocusRequest);
      stage.removeEventListener('topology-fit-view', onFitRequest);
      stage.removeEventListener('topology-reset-view', onResetRequest);
      stage.removeEventListener('topology-selection-changed', onSelectionChange);
      controls.removeEventListener('change', onControlChange);
      controls.dispose();
      resizeObserver?.disconnect();
      intersectionObserver?.disconnect();
      scene.traverse((object) => {
        if (object instanceof THREE.Mesh || object instanceof THREE.Line) {
          object.geometry.dispose();
          const materials = Array.isArray(object.material) ? object.material : [object.material];
          materials.forEach((material) => material.dispose());
        }
      });
      renderer.dispose();
      // `dispose()` releases three's own resources but deliberately keeps the
      // underlying WebGL context alive. Browsers cap the number of live
      // contexts and silently drop the oldest, so release it explicitly.
      try {
        renderer.forceContextLoss();
      } catch {
        // `WEBGL_lose_context` is an optional extension; nothing to release.
      }
      renderer.domElement.remove();
    };
  }, [graphKey, incidentKey, reducedMotion, retryToken]);

  useEffect(() => {
    if (focusToken <= 0 || nodes.length === 0) return;
    stageRef.current?.dispatchEvent(new CustomEvent('topology-focus-selected'));
  }, [focusToken, nodes.length]);

  useEffect(() => {
    if (fitToken <= 0) return;
    stageRef.current?.dispatchEvent(new CustomEvent('topology-fit-view'));
  }, [fitToken]);

  useEffect(() => {
    stageRef.current?.dispatchEvent(new CustomEvent('topology-selection-changed'));
  }, [selectedId, nodes.length]);

  useEffect(() => {
    if (resetToken <= 0) return;
    stageRef.current?.dispatchEvent(new CustomEvent('topology-reset-view'));
  }, [resetToken]);

  return (
    <div className="topology-three-wrap">
      {/*
  Deliberately no `role="img"` on this element. An image role makes the whole
  subtree presentational, which would hide the WebGL fallback message and its
  "Retry WebGL" control from assistive technology. The stage is a plain
  container; the canvas inside it is already `aria-hidden`, and the real
  representation of this data is the accessible service list in TopologyPanel.
*/}
<div ref={stageRef} className={`topology-three-stage${rendererError ? ' topology-three-stage--fallback' : ''}`}>
        {rendererError ? (
          <div className="topology-webgl-fallback" role="status">
            <span className="topology-fallback-mark" aria-hidden="true">3D</span>
            <strong>3D visualization unavailable on this device.</strong>
            <span>Use the 2D topology to inspect the same observed services and dependencies.</span>
            <button className="btn btn-sm" type="button" onClick={() => { setRendererError(false); setRetryToken((value) => value + 1); }}>Retry WebGL</button>
          </div>
        ) : null}
        {!rendererError && hovered && <div className="topology-hover-card" style={{ left: Math.min(hovered.x + 16, 480), top: Math.max(hovered.y - 52, 54) }} aria-hidden="true">
          <span className={`health-band health-band--${nodes.find((node) => node.id === hovered.id)?.health ?? 'unknown'}`} />
          <strong>{hovered.id}</strong>
          <small>{nodes.find((node) => node.id === hovered.id)?.events.toLocaleString() ?? '—'} dataset events · {nodes.find((node) => node.id === hovered.id)?.health ?? 'health unavailable'}</small>
        </div>}
      </div>
      {!rendererError && <div className="topology-three-tools" role="group" aria-label="3D camera controls">
        <button className="btn btn-sm" type="button" onClick={() => stageRef.current?.dispatchEvent(new CustomEvent('topology-fit-view'))}>Fit graph</button>
        <span>Drag to orbit · scroll to zoom</span>
      </div>}
      <p className="topology-three-note">{graphKind === 'declared' ? 'Node size represents simulated events in the selected window. Static edge paths represent declared scenario dependencies and their returned weights.' : 'Node size represents dataset event volume. Static edge paths represent observed request-trail associations and returned weights, not verified infrastructure or causality.'}</p>
    </div>
  );
}
