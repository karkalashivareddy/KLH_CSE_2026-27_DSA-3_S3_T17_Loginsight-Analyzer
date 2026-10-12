import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode
} from 'react';
import { api } from '../api/client';
import type {
  LogEvent,
  Scenario,
  ServiceHealth,
  SimulationCompleteEvent,
  SimulationEvidence,
  SimulationFrame,
  SimulationIncident,
  SimulationLifecycleStatus,
  SimulationSignal,
  SimulationStartEvent,
  SimulationStatus,
  SimulationTopologyPayload
} from '../api/types';

export type TelemetryState = 'idle' | 'starting' | 'streaming' | 'paused' | 'complete' | 'stopped' | 'error';

export interface TelemetryStartOptions {
  scenarioId?: string;
  seed?: number;
  speed?: number;
}

/** One point on the live chart series, retained for the session and never sent to the server. */
export interface TelemetrySample {
  tick: number;
  at: string;
  errorRate: number;
  eventsPerSecond: number;
  p95LatencyMs: number;
  averageLatencyMs: number;
  intensity: number;
  phase: SimulationFrame['phase'];
  openIncidents: number;
  matchedSignatures: number;
}

export interface TelemetryContextValue {
  state: TelemetryState;
  statusLabel: string;
  error: Error | null;
  isRunning: boolean;

  scenarios: Scenario[];
  scenariosLoading: boolean;
  scenariosError: Error | null;
  simulationStatus: SimulationStatus | null;

  scenario: Scenario | null;
  scenarioId: string | null;
  seed: number | null;
  speed: number;

  startEvent: SimulationStartEvent | null;
  sessionId: string | null;
  completeEvent: SimulationCompleteEvent | null;

  frame: SimulationFrame | null;
  samples: TelemetrySample[];
  events: LogEvent[];
  signals: SimulationSignal[];
  evidence: SimulationEvidence[];
  health: ServiceHealth[];
  incidents: SimulationIncident[];
  topology: SimulationTopologyPayload | null;
  openIncidentCount: number;

  selectScenario: (scenarioId: string) => void;
  setSpeed: (speed: number) => void;
  loadPreview: () => void;
  start: (options?: TelemetryStartOptions) => void;
  stop: () => void;
  restart: () => void;
  reseed: () => void;
  advanceIncident: (incidentId: number) => Promise<void>;
  setIncidentStatus: (incidentId: number, status: SimulationLifecycleStatus) => Promise<void>;
  refreshIncidents: () => Promise<void>;
  reloadScenarios: () => void;
}

const MAX_SAMPLES = 180;
const MAX_EVENTS = 300;
const DEFAULT_SCENARIO_ID = 'checkout-5xx-cascade';
const MIN_SPEED = 0.25;
const MAX_SPEED = 8;

const TelemetryContext = createContext<TelemetryContextValue | null>(null);

const emptyTelemetryContext: TelemetryContextValue = {
  state: 'idle',
  statusLabel: 'Ready',
  error: null,
  isRunning: false,
  scenarios: [],
  scenariosLoading: false,
  scenariosError: null,
  simulationStatus: null,
  scenario: null,
  scenarioId: null,
  seed: null,
  speed: 1,
  startEvent: null,
  sessionId: null,
  completeEvent: null,
  frame: null,
  samples: [],
  events: [],
  signals: [],
  evidence: [],
  health: [],
  incidents: [],
  topology: null,
  openIncidentCount: 0,
  selectScenario: () => undefined,
  setSpeed: () => undefined,
  loadPreview: () => undefined,
  start: () => undefined,
  stop: () => undefined,
  restart: () => undefined,
  reseed: () => undefined,
  advanceIncident: async () => undefined,
  setIncidentStatus: async () => undefined,
  refreshIncidents: async () => undefined,
  reloadScenarios: () => undefined
};

function asError(value: unknown): Error {
  return value instanceof Error ? value : new Error(String(value));
}

function stateLabel(state: TelemetryState): string {
  switch (state) {
    case 'starting':
      return 'Connecting';
    case 'streaming':
      return 'Streaming';
    case 'paused':
      return 'Paused';
    case 'complete':
      return 'Run complete';
    case 'stopped':
      return 'Stopped';
    case 'error':
      return 'Error';
    default:
      return 'Ready';
  }
}

function clampSpeed(value: number): number {
  if (!Number.isFinite(value)) return 1;
  return Math.max(MIN_SPEED, Math.min(MAX_SPEED, value));
}

function readPersistedScenario(): string {
  try {
    const stored = window.localStorage.getItem('loginsight:simulation:scenario');
    return stored && stored.trim() ? stored : DEFAULT_SCENARIO_ID;
  } catch {
    return DEFAULT_SCENARIO_ID;
  }
}

function persistScenario(scenarioId: string): void {
  try {
    window.localStorage.setItem('loginsight:simulation:scenario', scenarioId);
  } catch {
    /* storage unavailable (private mode); the app still works, it just will not remember */
  }
}

function readPersistedSpeed(): number {
  try {
    const stored = window.localStorage.getItem('loginsight:simulation:speed');
    const parsed = stored ? Number(stored) : Number.NaN;
    return Number.isFinite(parsed) && parsed > 0 ? clampSpeed(parsed) : 1;
  } catch {
    return 1;
  }
}

function persistSpeed(speed: number): void {
  try {
    window.localStorage.setItem('loginsight:simulation:speed', String(speed));
  } catch {
    /* ignored */
  }
}

/**
 * Owns the deterministic simulation for the whole application.
 *
 * <p>One run, one source of truth: Overview, Live, Services, Incidents and the topology all read the
 * same frame from here instead of opening competing streams. That keeps the fleet numbers on screen
 * consistent with each other, and keeps generated traffic clearly separated from the dataset replay
 * owned by `ReplayContext`.</p>
 */
export function TelemetryProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<TelemetryState>('idle');
  const [error, setError] = useState<Error | null>(null);
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [scenariosLoading, setScenariosLoading] = useState(true);
  const [scenariosError, setScenariosError] = useState<Error | null>(null);
  const [simulationStatus, setSimulationStatus] = useState<SimulationStatus | null>(null);
  const [scenarioId, setScenarioId] = useState<string>(() => readPersistedScenario());
  const [seed, setSeed] = useState<number | null>(null);
  const [speed, setSpeedState] = useState<number>(() => readPersistedSpeed());
  const [startEvent, setStartEvent] = useState<SimulationStartEvent | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [completeEvent, setCompleteEvent] = useState<SimulationCompleteEvent | null>(null);
  const [frame, setFrame] = useState<SimulationFrame | null>(null);
  const [samples, setSamples] = useState<TelemetrySample[]>([]);
  const [events, setEvents] = useState<LogEvent[]>([]);
  const [overrideIncident, setOverrideIncident] = useState<SimulationIncident | null>(null);
  const [sessionIncidents, setSessionIncidents] = useState<SimulationIncident[]>([]);

  const unsubscribeRef = useRef<(() => void) | null>(null);
  const generationRef = useRef(0);
  const scenariosControllerRef = useRef<AbortController | null>(null);
  const previewControllerRef = useRef<AbortController | null>(null);

  const isRunning = state === 'starting' || state === 'streaming';

  const scenario = useMemo(
    () => scenarios.find((item) => item.id === scenarioId) ?? null,
    [scenarioId, scenarios]
  );

  const clearStream = useCallback(() => {
    generationRef.current += 1;
    const unsubscribe = unsubscribeRef.current;
    unsubscribeRef.current = null;
    unsubscribe?.();
  }, []);

  const reloadScenarios = useCallback(() => {
    scenariosControllerRef.current?.abort();
    const controller = new AbortController();
    scenariosControllerRef.current = controller;
    setScenariosLoading(true);
    setScenariosError(null);
    void api
      .scenarios({ signal: controller.signal })
      .then((next) => {
        if (controller.signal.aborted) return;
        setScenarios(next);
        setScenariosError(null);
      })
      .catch((reason: unknown) => {
        if (controller.signal.aborted) return;
        setScenariosError(asError(reason));
      })
      .finally(() => {
        if (controller.signal.aborted) return;
        setScenariosLoading(false);
      });
  }, []);

  useEffect(() => {
    reloadScenarios();
    void api
      .simulationStatus()
      .then(setSimulationStatus)
      .catch(() => setSimulationStatus(null));
    return () => {
      scenariosControllerRef.current?.abort();
      previewControllerRef.current?.abort();
      clearStream();
    };
  }, [clearStream, reloadScenarios]);

  const resetRunState = useCallback(() => {
    setError(null);
    setStartEvent(null);
    setSessionId(null);
    setCompleteEvent(null);
    setFrame(null);
    setSamples([]);
    setEvents([]);
    setOverrideIncident(null);
    setSessionIncidents([]);
  }, []);

  /**
   * Fetches one deterministic frame without opening a stream.
   *
   * <p>The lab must explain itself before anyone presses start, so the preview is a real server
   * computation on the real rolling window at the scenario's peak tick — not a fixture.</p>
   */
  const loadPreview = useCallback(() => {
    const target = scenarioId;
    const controller = new AbortController();
    previewControllerRef.current?.abort();
    previewControllerRef.current = controller;
    void api
      .simulationSample(target, scenario?.seed, 1, { signal: controller.signal })
      .then((next) => {
        if (controller.signal.aborted) return;
        setFrame(next);
        setSeed(next.seed);
        setSessionId(next.sessionId);
        setStartEvent(null);
        setCompleteEvent(null);
        setError(null);
      })
      .catch((reason: unknown) => {
        if (controller.signal.aborted) return;
        setError(asError(reason));
      });
  }, [scenario?.seed, scenarioId]);

  // Show a real frame as soon as the catalogue resolves, but never while a run owns the state.
  useEffect(() => {
    if (isRunning) return;
    if (frame) return;
    if (!scenario) return;
    loadPreview();
  }, [frame, isRunning, loadPreview, scenario]);

  const start = useCallback(
    (options: TelemetryStartOptions = {}) => {
      const resolvedScenario = options.scenarioId ?? scenarioId;
      const resolvedSeed = options.seed ?? scenario?.seed ?? undefined;
      const resolvedSpeed = clampSpeed(options.speed ?? speed);
      clearStream();
      resetRunState();
      const generation = generationRef.current;
      let settled = false;
      const isCurrent = () => generation === generationRef.current;
      setState('starting');
      setSpeedState(resolvedSpeed);
      persistSpeed(resolvedSpeed);
      unsubscribeRef.current = api.simulationStream(
        {
          onStart: (next: SimulationStartEvent) => {
            if (!isCurrent()) return;
            setStartEvent(next);
            setSessionId(next.sessionId);
            setSeed(next.seed);
            setState('streaming');
          },
          onFrame: (next: SimulationFrame) => {
            if (!isCurrent()) return;
            setFrame(next);
            setState('streaming');
            setEvents((previous) => [...next.events].reverse().concat(previous).slice(0, MAX_EVENTS));
            setSamples((previous) => {
              const point: TelemetrySample = {
                tick: next.tick,
                at: new Date().toISOString(),
                errorRate: next.errorRate,
                eventsPerSecond: next.eventsPerSecond,
                p95LatencyMs: next.p95LatencyMs,
                averageLatencyMs: next.averageLatencyMs,
                intensity: next.intensity,
                phase: next.phase,
                openIncidents: next.incidents.filter((incident) => incident.open).length,
                matchedSignatures: next.matchedSignatures.length
              };
              const appended = [...previous, point];
              return appended.length > MAX_SAMPLES ? appended.slice(appended.length - MAX_SAMPLES) : appended;
            });
          },
          onComplete: (complete: SimulationCompleteEvent) => {
            if (!isCurrent()) return;
            settled = true;
            setCompleteEvent(complete);
            setState('complete');
          },
          onError: (streamError: Error) => {
            if (!isCurrent()) return;
            settled = true;
            setError(asError(streamError));
            setState('error');
          },
          onClose: () => {
            if (!isCurrent() || settled) return;
            setError(new Error('The simulation stream closed before the server reported completion.'));
            setState('error');
          }
        },
        { scenario: resolvedScenario, seed: resolvedSeed, speed: resolvedSpeed, intervalMs: 250, maxFrames: 2_000 }
      );
    },
    [clearStream, resetRunState, scenario?.seed, scenarioId, speed]
  );

  const stop = useCallback(() => {
    clearStream();
    setState((current) => (current === 'starting' || current === 'streaming' ? 'stopped' : current));
  }, [clearStream]);

  const restart = useCallback(() => start(), [start]);

  const reseed = useCallback(() => {
    // A fresh seed changes the event progression, so the run must restart to be meaningful.
    const next = Math.floor(Math.random() * 2_147_483_647);
    setSeed(next);
    start({ seed: next });
  }, [start]);

  const selectScenario = useCallback(
    (nextId: string) => {
      setScenarioId(nextId);
      persistScenario(nextId);
      setState((current) => (current === 'idle' || current === 'complete' || current === 'stopped' ? current : 'stopped'));
    },
    []
  );

  const setSpeed = useCallback((next: number) => {
    const clamped = clampSpeed(next);
    setSpeedState(clamped);
    persistSpeed(clamped);
  }, []);

  const applyIncident = useCallback((updated: SimulationIncident) => {
    setFrame((current) => {
      if (!current) return current;
      return {
        ...current,
        incidents: current.incidents.map((incident) => (incident.id === updated.id ? updated : incident))
      };
    });
    setOverrideIncident(updated);
  }, []);

  const advanceIncident = useCallback(
    async (incidentId: number) => {
      if (!sessionId) return;
      try {
        applyIncident(await api.transitionIncident(incidentId, { sessionId, advance: true }));
      } catch (reason) {
        setError(asError(reason));
      }
    },
    [applyIncident, sessionId]
  );

  const setIncidentStatus = useCallback(
    async (incidentId: number, nextStatus: SimulationLifecycleStatus) => {
      if (!sessionId) return;
      try {
        applyIncident(await api.transitionIncident(incidentId, { sessionId, status: nextStatus }));
      } catch (reason) {
        setError(asError(reason));
      }
    },
    [applyIncident, sessionId]
  );

  const incidents = useMemo(() => {
    const merged = new Map((frame?.incidents ?? []).map((incident) => [incident.id, incident]));
    for (const incident of sessionIncidents) merged.set(incident.id, incident);
    if (overrideIncident) merged.set(overrideIncident.id, overrideIncident);
    return [...merged.values()];
  }, [frame?.incidents, overrideIncident, sessionIncidents]);

  /**
   * Re-reads a session's incidents from the server.
   *
   * <p>The stream carries the incident list in every frame, but an investigation surface must not
   * depend on a frame happening to be in memory: after a reload, after the stream ended, or when the
   * run was started from another screen, the frame is empty even though the server still holds the
   * session's incidents. `GET /api/simulation/incidents` exists for exactly that case, so the
   * workbench calls this on mount and gets the real lifecycle back instead of an empty state.</p>
   */
  const refreshIncidents = useCallback(async () => {
    if (!sessionId) return;
    const fetched = await api.simulationIncidents(sessionId);
    setSessionIncidents((previous) => {
      const merged = new Map(previous.map((incident) => [incident.id, incident]));
      for (const incident of fetched) merged.set(incident.id, incident);
      return [...merged.values()];
    });
  }, [sessionId]);

  const openIncidentCount = useMemo(
    () => incidents.filter((incident) => incident.open).length,
    [incidents]
  );

  const value = useMemo<TelemetryContextValue>(
    () => ({
      state,
      statusLabel: stateLabel(state),
      error,
      isRunning,
      scenarios,
      scenariosLoading,
      scenariosError,
      simulationStatus,
      scenario,
      scenarioId,
      seed,
      speed,
      startEvent,
      sessionId,
      completeEvent,
      frame,
      samples,
      events,
      signals: frame?.signals ?? [],
      evidence: frame?.evidence ?? [],
      health: frame?.health ?? [],
      incidents,
      topology: frame?.topology ?? null,
      openIncidentCount,
      selectScenario,
      setSpeed,
      loadPreview,
      start,
      stop,
      restart,
      reseed,
      advanceIncident,
      setIncidentStatus,
      reloadScenarios,
      refreshIncidents
    }),
    [
      advanceIncident,
      completeEvent,
      error,
      events,
      frame,
      incidents,
      isRunning,
      loadPreview,
      openIncidentCount,
      refreshIncidents,
      reloadScenarios,
      restart,
      reseed,
      samples,
      scenario,
      scenarioId,
      scenarios,
      scenariosError,
      scenariosLoading,
      seed,
      selectScenario,
      sessionId,
      setIncidentStatus,
      setSpeed,
      simulationStatus,
      speed,
      start,
      startEvent,
      state,
      stop
    ]
  );

  return <TelemetryContext.Provider value={value}>{children}</TelemetryContext.Provider>;
}

export function useTelemetry(): TelemetryContextValue {
  return useContext(TelemetryContext) ?? emptyTelemetryContext;
}
